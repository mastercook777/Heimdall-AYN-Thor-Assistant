package com.mastercook777.heimdall;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;

final class TranslationRuntimeController {
    static final class Snapshot {
        final String sourceText;
        final String translatedText;

        Snapshot(String sourceText, String translatedText) {
            this.sourceText = TranslationTextStabilizer.normalize(sourceText);
            this.translatedText = translatedText == null ? "" : translatedText.trim();
        }

        boolean isEmpty() {
            return sourceText.length() == 0 && translatedText.length() == 0;
        }
    }

    enum Status {
        STARTING,
        SCANNING,
        TRANSLATING,
        PAUSED,
        NO_TEXT,
        ACCESSIBILITY_UNAVAILABLE,
        PROVIDER_UNCONFIGURED,
        NETWORK_UNAVAILABLE,
        OCR_ERROR,
        API_ERROR
    }

    interface Listener {
        void onTranslationState(String translatedText, Status status);
    }

    private static final long CAPTURE_INTERVAL_MS = 500L;
    private static final long STABILITY_CONFIRM_INTERVAL_MS = 200L;
    private static final long ERROR_RETRY_MS = 1800L;
    private static final int OCR_MAXIMUM_SIDE = 1600;
    private static final int OCR_ERROR_VISIBLE_AFTER_FAILURES = 3;

    private final Context context;
    private final String profileId;
    private final String widgetId;
    private final TranslationConfig config;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TranslationTextStabilizer stabilizer = new TranslationTextStabilizer();
    private final TranslationOcrEngine ocr = new TranslationOcrEngine();
    private final TranslationApiClient api = new TranslationApiClient();
    private final Runnable captureRunnable = this::capture;

    private int generation;
    private boolean running;
    private boolean capturePending;
    private String translatedText = "";
    private String lastTranslatedSourceText = "";
    private String activeSourceText = "";
    private String failedSourceText = "";
    private long failedSourceRetryAtMs;
    private int consecutiveOcrFailures;
    private TranslationApiClient.Request activeRequest;

    TranslationRuntimeController(Context context, String profileId, String widgetId,
            TranslationConfig config, Snapshot initialSnapshot, Listener listener) {
        this.context = context.getApplicationContext();
        this.profileId = profileId == null ? "" : profileId;
        this.widgetId = widgetId == null ? "" : widgetId;
        this.config = config.copy();
        this.listener = listener;
        if (initialSnapshot != null) {
            lastTranslatedSourceText = initialSnapshot.sourceText;
            translatedText = initialSnapshot.translatedText;
        }
    }

    void start() {
        if (running) return;
        running = true;
        generation++;
        stabilizer.reset();
        if (lastTranslatedSourceText.length() > 0) {
            stabilizer.markTranslated(lastTranslatedSourceText);
        }
        consecutiveOcrFailures = 0;
        DebugPerformanceDiagnostics.registerRepeatingTask(
                "Translation screenshot", CAPTURE_INTERVAL_MS);
        publish(Status.STARTING);
        handler.post(captureRunnable);
    }

    void stop() {
        running = false;
        generation++;
        capturePending = false;
        handler.removeCallbacks(captureRunnable);
        DebugPerformanceDiagnostics.unregisterRepeatingTask("Translation screenshot");
        cancelRequest();
        stabilizer.reset();
        consecutiveOcrFailures = 0;
    }

    Snapshot snapshot() {
        return new Snapshot(lastTranslatedSourceText, translatedText);
    }

    void release() {
        stop();
        ocr.close();
        api.shutdown();
    }

    private void capture() {
        if (!running || capturePending) return;
        final int sessionGeneration = generation;
        if (!ThorAccessibilityService.isReady()) {
            publish(Status.ACCESSIBILITY_UNAVAILABLE);
            schedule(ERROR_RETRY_MS, sessionGeneration);
            return;
        }
        capturePending = true;
        final long captureStarted = DebugPerformanceDiagnostics.beginTask(
                "Translation screenshot");
        ThorAccessibilityService.captureDisplayRegion(context, Display.DEFAULT_DISPLAY,
                config.regionLeft, config.regionTop, config.regionRight, config.regionBottom,
                OCR_MAXIMUM_SIDE,
                new ThorAccessibilityService.ScreenshotCallback() {
                    @Override
                    public void onCaptured(android.graphics.Bitmap bitmap) {
                        DebugPerformanceDiagnostics.endTask(
                                "Translation screenshot", captureStarted);
                        handler.post(() -> handleCapturedRegion(bitmap, sessionGeneration));
                    }

                    @Override
                    public void onError(String message) {
                        DebugPerformanceDiagnostics.endTask(
                                "Translation screenshot", captureStarted);
                        handler.post(() -> {
                            if (!isCurrent(sessionGeneration)) return;
                            capturePending = false;
                            handleOcrFailure(sessionGeneration);
                        });
                    }
                });
    }

    private void handleCapturedRegion(android.graphics.Bitmap bitmap, int sessionGeneration) {
        if (!isCurrent(sessionGeneration)) {
            bitmap.recycle();
            return;
        }
        final long ocrStarted = DebugPerformanceDiagnostics.beginTask("Translation OCR");
        ocr.recognize(bitmap, config, new TranslationOcrEngine.Callback() {
            @Override
            public void onSuccess(String text) {
                DebugPerformanceDiagnostics.endTask("Translation OCR", ocrStarted);
                if (!isCurrent(sessionGeneration)) return;
                capturePending = false;
                consecutiveOcrFailures = 0;
                boolean needsConfirmation = handleOcrText(text, sessionGeneration);
                schedule(needsConfirmation
                        ? STABILITY_CONFIRM_INTERVAL_MS : CAPTURE_INTERVAL_MS,
                        sessionGeneration);
            }

            @Override
            public void onError() {
                DebugPerformanceDiagnostics.endTask("Translation OCR", ocrStarted);
                if (!isCurrent(sessionGeneration)) return;
                capturePending = false;
                handleOcrFailure(sessionGeneration);
            }
        });
    }

    private void handleOcrFailure(int sessionGeneration) {
        if (!ThorAccessibilityService.isReady()) {
            consecutiveOcrFailures = 0;
            publish(Status.ACCESSIBILITY_UNAVAILABLE);
            schedule(ERROR_RETRY_MS, sessionGeneration);
            return;
        }
        consecutiveOcrFailures++;
        boolean sustainedFailure = shouldSurfaceOcrError(consecutiveOcrFailures);
        publish(sustainedFailure ? Status.OCR_ERROR : Status.SCANNING);
        schedule(sustainedFailure ? ERROR_RETRY_MS : CAPTURE_INTERVAL_MS,
                sessionGeneration);
    }

    static boolean shouldSurfaceOcrError(int consecutiveFailures) {
        return consecutiveFailures >= OCR_ERROR_VISIBLE_AFTER_FAILURES;
    }

    private boolean handleOcrText(String text, int sessionGeneration) {
        String normalized = TranslationTextStabilizer.normalize(text);
        if (normalized.length() < 2) {
            publish(translatedText.length() == 0 ? Status.NO_TEXT : Status.SCANNING);
            return false;
        }
        String stableText = stabilizer.accept(normalized);
        if (stableText == null) {
            // A single noisy OCR frame is not a confirmed subtitle change. Cancelling here can
            // continuously restart a healthy HTTP request before it has time to complete.
            if (activeRequest != null && shouldReplaceActiveRequest(
                    normalized, activeSourceText, false)) {
                cancelRequest();
            }
            publish(activeRequest == null ? Status.SCANNING : Status.TRANSLATING);
            return stabilizer.isAwaitingConfirmation() && activeRequest == null;
        }
        if (activeRequest != null) {
            if (!shouldReplaceActiveRequest(stableText, activeSourceText, true)) {
                publish(Status.TRANSLATING);
                return false;
            }
        }
        if (TranslationTextStabilizer.isTranslationDuplicate(stableText, failedSourceText)
                && android.os.SystemClock.uptimeMillis() < failedSourceRetryAtMs) {
            publish(Status.API_ERROR);
            return false;
        }
        TranslationProviderConfig provider = TranslationSettingsStore.load(context);
        if (!provider.isComplete()) {
            publish(Status.PROVIDER_UNCONFIGURED);
            return false;
        }
        if (!hasNetwork()) {
            publish(Status.NETWORK_UNAVAILABLE);
            return false;
        }
        cancelRequest();
        activeSourceText = stableText;
        publish(Status.TRANSLATING);
        activeRequest = api.translate(provider, stableText, config.targetLanguage,
                new TranslationApiClient.Callback() {
                    @Override
                    public void onSuccess(String result) {
                        if (!isCurrent(sessionGeneration)
                                || !stableText.equals(activeSourceText)) return;
                        activeRequest = null;
                        translatedText = result;
                        lastTranslatedSourceText = stableText;
                        failedSourceText = "";
                        failedSourceRetryAtMs = 0L;
                        stabilizer.markTranslated(stableText);
                        publish(Status.SCANNING);
                    }

                    @Override
                    public void onError(String message) {
                        if (!isCurrent(sessionGeneration)
                                || !stableText.equals(activeSourceText)) return;
                        activeRequest = null;
                        failedSourceText = stableText;
                        failedSourceRetryAtMs = android.os.SystemClock.uptimeMillis() + 8000L;
                        publish(Status.API_ERROR);
                    }
                });
        return false;
    }

    static boolean shouldReplaceActiveRequest(String observedSourceText,
            String activeSourceText, boolean observationConfirmed) {
        return observationConfirmed && !TranslationTextStabilizer.isTranslationDuplicate(
                observedSourceText, activeSourceText);
    }

    private boolean hasNetwork() {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        Network network = manager.getActiveNetwork();
        NetworkCapabilities capabilities = network == null
                ? null : manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void cancelRequest() {
        if (activeRequest != null) activeRequest.cancel();
        activeRequest = null;
        activeSourceText = "";
    }

    private boolean isCurrent(int sessionGeneration) {
        return running && generation == sessionGeneration
                && profileId.length() > 0 && widgetId.length() > 0;
    }

    private void schedule(long delayMs, int sessionGeneration) {
        if (isCurrent(sessionGeneration)) handler.postDelayed(captureRunnable, delayMs);
    }

    private void publish(Status status) {
        listener.onTranslationState(translatedText, status);
    }
}
