package com.mastercook777.heimdall;

import android.annotation.TargetApi;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ThorAccessibilityService extends AccessibilityService {
    private static ThorAccessibilityService instance;
    private static volatile boolean diagnosticScanningSuspended;
    private static String lastExternalPackageName = "";
    private static final List<String> recentPackageNames = new ArrayList<>();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable foregroundRefresh = this::refreshForegroundApp;
    private GestureDescription.StrokeDescription activeTouchpadStroke;
    private InputBridge.Callback activeTouchpadCallback;
    private boolean touchpadActive;
    private boolean touchpadDispatching;
    private boolean touchpadFinishRequested;
    private int touchpadDisplayId = Display.DEFAULT_DISPLAY;
    private int touchpadWidth;
    private int touchpadHeight;
    private float touchpadX;
    private float touchpadY;
    private float pendingTouchpadDx;
    private float pendingTouchpadDy;
    private int macroGeneration;
    private InputBridge.Callback activeMacroCallback;
    private Thread activeMacroGamepadThread;
    private String activeMacroLabel = "";
    private boolean activeMacroCancelRequested;

    public static ThorAccessibilityService getInstance() {
        return instance;
    }

    public static boolean isReady() {
        return instance != null;
    }

    public static void setDiagnosticScanningSuspended(boolean suspended) {
        if (!DebugPerformanceDiagnostics.isDebugEnabled()) {
            return;
        }
        diagnosticScanningSuspended = suspended;
        ThorAccessibilityService service = instance;
        if (service != null && suspended) {
            service.handler.removeCallbacks(service.foregroundRefresh);
        }
    }

    public static String getLastExternalPackageName() {
        return lastExternalPackageName;
    }

    public static List<String> getRecentPackageNames() {
        return new ArrayList<>(recentPackageNames);
    }

    public interface ScreenshotCallback {
        void onCaptured(Bitmap bitmap);
        void onError(String message);
    }

    public static void captureDisplay(Context context, int displayId,
            ScreenshotCallback callback) {
        ThorAccessibilityService service = instance;
        if (service == null) {
            callback.onError(context.getString(R.string.accessibility_enable_required));
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            callback.onError(service.getString(R.string.accessibility_screenshot_unsupported));
            return;
        }
        captureDisplayR(service, displayId, callback);
    }

    @TargetApi(Build.VERSION_CODES.R)
    private static void captureDisplayR(ThorAccessibilityService service, int displayId,
            ScreenshotCallback callback) {
        service.takeScreenshot(displayId, service.getMainExecutor(),
                new AccessibilityService.TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(ScreenshotResult screenshot) {
                        HardwareBuffer buffer = screenshot.getHardwareBuffer();
                        Bitmap wrapped = Bitmap.wrapHardwareBuffer(buffer, screenshot.getColorSpace());
                        Bitmap copy = wrapped == null ? null : wrapped.copy(Bitmap.Config.ARGB_8888, false);
                        buffer.close();
                        if (copy == null) {
                            callback.onError(service.getString(
                                    R.string.accessibility_screenshot_image_failed));
                        } else {
                            callback.onCaptured(copy);
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        callback.onError(service.getString(
                                R.string.accessibility_screenshot_error_code, errorCode));
                    }
                });
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        if (!diagnosticScanningSuspended && ForegroundAppTracker.isEnabled(this)) {
            handler.postDelayed(foregroundRefresh, 120L);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(foregroundRefresh);
        cancelMacro();
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (diagnosticScanningSuspended || !ForegroundAppTracker.isEnabled(this)) {
            return;
        }
        CharSequence packageName = event.getPackageName();
        if (packageName == null) {
            return;
        }
        String value = packageName.toString();
        if (!isTrackablePackage(value)) {
            if (isGameAssistantPackage(value)) {
                handler.removeCallbacks(foregroundRefresh);
                handler.postDelayed(foregroundRefresh, 180L);
            }
            return;
        }
        rememberPackage(value);
        lastExternalPackageName = value;
        EventWindow window = resolveEventWindow(event.getWindowId());
        ForegroundAppTracker.publish(new ForegroundAppTracker.Snapshot(
                value,
                event.getClassName() == null ? "" : event.getClassName().toString(),
                window.title,
                window.displayId,
                System.currentTimeMillis()));
    }

    @Override
    public void onInterrupt() {
    }

    private static void rememberPackage(String packageName) {
        recentPackageNames.remove(packageName);
        recentPackageNames.add(0, packageName);
        while (recentPackageNames.size() > 12) {
            recentPackageNames.remove(recentPackageNames.size() - 1);
        }
    }

    public void refreshForegroundApp() {
        if (diagnosticScanningSuspended || !ForegroundAppTracker.isEnabled(this)) {
            return;
        }
        long started = DebugPerformanceDiagnostics.beginTask(
                "Accessibility getWindows scan");
        int windowCount = 0;
        AccessibilityWindowInfo best = null;
        int bestScore = Integer.MIN_VALUE;
        for (AccessibilityWindowInfo window : getWindows()) {
            windowCount++;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && window.getDisplayId() != Display.DEFAULT_DISPLAY) {
                continue;
            }
            if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) {
                continue;
            }
            AccessibilityNodeInfo root = window.getRoot();
            CharSequence packageName = root == null ? null : root.getPackageName();
            if (packageName == null || !isTrackablePackage(packageName.toString())) {
                continue;
            }
            int score = window.getLayer();
            if (window.isActive()) {
                score += 10000;
            }
            if (window.isFocused()) {
                score += 20000;
            }
            if (best == null || score > bestScore) {
                best = window;
                bestScore = score;
            }
        }
        for (int i = 0; i < windowCount; i++) {
            DebugPerformanceDiagnostics.countAccessibilityWindowInspection();
        }
        if (best == null) {
            DebugPerformanceDiagnostics.endTask(
                    "Accessibility getWindows scan", started);
            return;
        }
        AccessibilityNodeInfo root = best.getRoot();
        if (root == null || root.getPackageName() == null) {
            DebugPerformanceDiagnostics.endTask(
                    "Accessibility getWindows scan", started);
            return;
        }
        String packageName = root.getPackageName().toString();
        rememberPackage(packageName);
        lastExternalPackageName = packageName;
        ForegroundAppTracker.publish(new ForegroundAppTracker.Snapshot(
                packageName,
                root.getClassName() == null ? "" : root.getClassName().toString(),
                best.getTitle() == null ? "" : best.getTitle().toString(),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ? best.getDisplayId() : Display.INVALID_DISPLAY,
                System.currentTimeMillis()));
        DebugPerformanceDiagnostics.endTask(
                "Accessibility getWindows scan", started);
    }

    private boolean isTrackablePackage(String packageName) {
        return packageName != null
                && packageName.length() > 0
                && !packageName.equals(getPackageName())
                && !packageName.equals("android")
                && !packageName.equals("com.android.systemui")
                && !packageName.equals("com.android.settings")
                && !packageName.contains("permissioncontroller")
                && !packageName.contains("launcher")
                && !isGameAssistantPackage(packageName);
    }

    private static boolean isGameAssistantPackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        String normalized = packageName.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("gameassistant")
                || normalized.contains("game_assistant")
                || normalized.contains("game-assistant");
    }

    private EventWindow resolveEventWindow(int windowId) {
        long started = DebugPerformanceDiagnostics.beginTask(
                "Accessibility event window lookup");
        for (AccessibilityWindowInfo window : getWindows()) {
            DebugPerformanceDiagnostics.countAccessibilityWindowInspection();
            if (window.getId() == windowId) {
                EventWindow result = new EventWindow(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                ? window.getDisplayId() : Display.INVALID_DISPLAY,
                        window.getTitle() == null ? "" : window.getTitle().toString().trim());
                DebugPerformanceDiagnostics.endTask(
                        "Accessibility event window lookup", started);
                return result;
            }
        }
        DebugPerformanceDiagnostics.endTask(
                "Accessibility event window lookup", started);
        return new EventWindow(Display.INVALID_DISPLAY, "");
    }

    private static final class EventWindow {
        final int displayId;
        final String title;

        EventWindow(int displayId, String title) {
            this.displayId = displayId;
            this.title = title;
        }
    }

    public void dispatchMacro(Macro macro, InputBridge.Callback callback) {
        if (macro.steps.isEmpty()) {
            callback.onError(getString(R.string.macro_status_no_steps, macro.label));
            return;
        }
        int generation;
        synchronized (this) {
            macroGeneration++;
            generation = macroGeneration;
            activeMacroCallback = callback;
            activeMacroGamepadThread = null;
            activeMacroLabel = macro.label == null ? "" : macro.label;
            activeMacroCancelRequested = false;
        }
        callback.onStatus(getString(R.string.macro_status_started, macro.label));
        runStep(macro, callback, 0, generation);
    }

    public void cancelMacro() {
        InputBridge.Callback callback;
        Thread worker;
        String label;
        boolean finishImmediately;
        synchronized (this) {
            callback = activeMacroCallback;
            worker = activeMacroGamepadThread;
            label = activeMacroLabel;
            finishImmediately = callback != null && worker == null;
            if (finishImmediately) {
                macroGeneration++;
                activeMacroCallback = null;
                activeMacroLabel = "";
            } else if (callback != null) {
                activeMacroCancelRequested = true;
            }
        }
        if (worker != null) {
            worker.interrupt();
        }
        if (finishImmediately) {
            callback.onStatus(getString(R.string.macro_status_cancelled,
                    label.length() == 0
                            ? getString(R.string.common_macro_fallback) : label));
            callback.onMacroFinished(true);
        }
    }

    private synchronized boolean isMacroActive(
            int generation, InputBridge.Callback callback) {
        return generation == macroGeneration && activeMacroCallback == callback;
    }

    private synchronized void finishMacro(
            int generation, InputBridge.Callback callback) {
        if (generation == macroGeneration && activeMacroCallback == callback) {
            activeMacroCallback = null;
            activeMacroGamepadThread = null;
            activeMacroLabel = "";
            activeMacroCancelRequested = false;
        }
    }

    private synchronized boolean isMacroCancellationRequested(
            int generation, InputBridge.Callback callback) {
        return generation == macroGeneration
                && activeMacroCallback == callback
                && activeMacroCancelRequested;
    }

    public void dispatchTouchMove(int displayId, int width, int height, float dx, float dy, InputBridge.Callback callback) {
        if (width <= 0 || height <= 0) {
            callback.onError(getString(R.string.touchpad_status_missing_display_size));
            return;
        }
        float startX = width / 2f;
        float startY = height / 2f;
        float endX = clamp(startX + dx, 1f, width - 1f);
        float endY = clamp(startY + dy, 1f, height - 1f);
        dispatchPath(null, callback, -1, 0,
                displayId, startX, startY, endX, endY, 55L);
    }

    public void startTouchpadDrag(int displayId, int width, int height, float anchorX, float anchorY,
            InputBridge.Callback callback) {
        if (width <= 0 || height <= 0) {
            callback.onError(getString(R.string.touchpad_status_missing_display_size));
            return;
        }
        if (displayId != Display.DEFAULT_DISPLAY && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            callback.onError(getString(R.string.touchpad_status_multidisplay_unsupported));
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            callback.onError(getString(R.string.touchpad_status_continuous_unsupported));
            return;
        }
        touchpadDisplayId = displayId;
        touchpadWidth = width;
        touchpadHeight = height;
        touchpadX = clamp(width * anchorX, 1f, width - 1f);
        touchpadY = clamp(height * anchorY, 1f, height - 1f);
        pendingTouchpadDx = 0f;
        pendingTouchpadDy = 0f;
        activeTouchpadStroke = null;
        activeTouchpadCallback = callback;
        touchpadActive = true;
        touchpadDispatching = false;
        touchpadFinishRequested = false;
    }

    public void moveTouchpadDrag(float dx, float dy, int strokeMs, InputBridge.Callback callback) {
        if (!touchpadActive) {
            callback.onError(getString(R.string.touchpad_status_not_pressed));
            return;
        }
        activeTouchpadCallback = callback;
        pendingTouchpadDx += dx;
        pendingTouchpadDy += dy;
        if (!touchpadDispatching) {
            dispatchNextTouchpadStroke(false, strokeMs);
        }
    }

    public void endTouchpadDrag(int strokeMs, InputBridge.Callback callback) {
        if (!touchpadActive) {
            return;
        }
        activeTouchpadCallback = callback;
        touchpadFinishRequested = true;
        if (!touchpadDispatching) {
            dispatchNextTouchpadStroke(true, strokeMs);
        }
    }

    private void dispatchNextTouchpadStroke(boolean finishNow, int strokeMs) {
        if (!touchpadActive || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            resetTouchpadGesture();
            return;
        }

        boolean hasMove = Math.abs(pendingTouchpadDx) > 0.01f || Math.abs(pendingTouchpadDy) > 0.01f;
        if (!hasMove && !finishNow && !touchpadFinishRequested) {
            return;
        }
        if (!hasMove && activeTouchpadStroke == null && (finishNow || touchpadFinishRequested)) {
            resetTouchpadGesture();
            return;
        }

        float startX = touchpadX;
        float startY = touchpadY;
        float endX = hasMove ? clamp(startX + pendingTouchpadDx, 1f, touchpadWidth - 1f) : startX;
        float endY = hasMove ? clamp(startY + pendingTouchpadDy, 1f, touchpadHeight - 1f) : startY;
        pendingTouchpadDx = 0f;
        pendingTouchpadDy = 0f;

        boolean willContinue = !(finishNow || touchpadFinishRequested);
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);

        long duration = Math.max(1L, strokeMs);
        GestureDescription.StrokeDescription stroke;
        if (activeTouchpadStroke == null) {
            stroke = new GestureDescription.StrokeDescription(path, 0, duration, willContinue);
        } else {
            stroke = activeTouchpadStroke.continueStroke(path, 0, duration, willContinue);
        }

        GestureDescription.Builder builder = new GestureDescription.Builder().addStroke(stroke);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setDisplayId(touchpadDisplayId);
        }

        touchpadDispatching = true;
        boolean accepted = dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                touchpadX = endX;
                touchpadY = endY;
                activeTouchpadStroke = willContinue ? stroke : null;
                touchpadDispatching = false;
                if (!touchpadActive) {
                    return;
                }
                boolean hasPending = Math.abs(pendingTouchpadDx) > 0.01f || Math.abs(pendingTouchpadDy) > 0.01f;
                if (touchpadFinishRequested) {
                    dispatchNextTouchpadStroke(true, strokeMs);
                } else if (hasPending) {
                    dispatchNextTouchpadStroke(false, strokeMs);
                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                InputBridge.Callback callback = activeTouchpadCallback;
                resetTouchpadGesture();
                if (callback != null) {
                    callback.onGestureCancelled();
                }
            }
        }, handler);

        if (!accepted) {
            InputBridge.Callback callback = activeTouchpadCallback;
            resetTouchpadGesture();
            if (callback != null) {
                callback.onError(getString(R.string.touchpad_status_gesture_submit_failed));
            }
        }
    }

    private void resetTouchpadGesture() {
        activeTouchpadStroke = null;
        activeTouchpadCallback = null;
        touchpadActive = false;
        touchpadDispatching = false;
        touchpadFinishRequested = false;
        pendingTouchpadDx = 0f;
        pendingTouchpadDy = 0f;
    }

    private void runStep(Macro macro, InputBridge.Callback callback,
            int index, int generation) {
        if (!isMacroActive(generation, callback)) {
            return;
        }
        if (index >= macro.steps.size()) {
            callback.onStatus(getString(R.string.macro_status_completed, macro.label));
            finishMacro(generation, callback);
            callback.onMacroFinished(false);
            return;
        }

        MacroStep step = macro.steps.get(index);
        if (MacroStep.TYPE_WAIT.equals(step.type)) {
            handler.postDelayed(() -> runStep(
                    macro, callback, index + 1, generation),
                    parseDuration(step.value, 80));
            return;
        }

        if (MacroStep.TYPE_TAP.equals(step.type)) {
            GestureTarget target = parseTapTarget(step.value);
            if (target == null) {
                finishMacro(generation, callback);
                callback.onError(getString(R.string.macro_status_invalid_tap, step.value));
                return;
            }
            dispatchPath(macro, callback, index, generation,
                    target.displayId, target.startX, target.startY,
                    target.endX, target.endY, 1L);
            return;
        }

        if (MacroStep.TYPE_HOLD.equals(step.type)) {
            GestureTarget target = parseHoldTarget(step.value);
            if (target == null) {
                finishMacro(generation, callback);
                callback.onError(getString(R.string.macro_status_invalid_hold, step.value));
                return;
            }
            dispatchPath(macro, callback, index, generation,
                    target.displayId, target.startX, target.startY,
                    target.endX, target.endY, target.durationMs);
            return;
        }

        if (MacroStep.TYPE_SWIPE.equals(step.type)) {
            GestureTarget target = parseSwipeTarget(step.value);
            if (target == null) {
                finishMacro(generation, callback);
                callback.onError(getString(R.string.macro_status_invalid_swipe, step.value));
                return;
            }
            dispatchPath(macro, callback, index, generation,
                    target.displayId, target.startX, target.startY,
                    target.endX, target.endY, target.durationMs);
            return;
        }

        if (MacroStep.TYPE_GAMEPAD.equals(step.type)) {
            Thread worker = new Thread(() -> {
                String result;
                try {
                    result = NativeGamepadPath.userFacingError(this,
                            InputBridge.replayNativeGamepadSequence(this, step.value));
                } catch (Throwable t) {
                    result = t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "native controller replay failed" : t.getMessage());
                }
                String finalResult = result;
                handler.post(() -> {
                    if (!isMacroActive(generation, callback)) {
                        return;
                    }
                    synchronized (ThorAccessibilityService.this) {
                        if (generation == macroGeneration
                                && activeMacroCallback == callback) {
                            activeMacroGamepadThread = null;
                        }
                    }
                    if (isMacroCancellationRequested(generation, callback)) {
                        finishMacro(generation, callback);
                        callback.onStatus(getString(
                                R.string.macro_status_cancelled, macro.label));
                        callback.onMacroFinished(true);
                        return;
                    }
                    if (NativeGamepadPath.operationSucceeded(finalResult)) {
                        callback.onStatus(getString(R.string.native_controller_step_complete));
                        handler.postDelayed(() -> runStep(
                                macro, callback, index + 1, generation), 60);
                    } else {
                        finishMacro(generation, callback);
                        callback.onError(finalResult);
                    }
                });
            }, "macro-gamepad-combo");
            synchronized (this) {
                if (!isMacroActive(generation, callback)) {
                    return;
                }
                activeMacroGamepadThread = worker;
            }
            worker.start();
            return;
        }

        finishMacro(generation, callback);
        callback.onError(getString(R.string.macro_status_unknown_step, step.toString()));
    }

    private void dispatchPath(Macro macro, InputBridge.Callback callback,
            int index, int generation,
            int displayId, float startX, float startY, float endX, float endY, long durationMs) {
        if (displayId != Display.DEFAULT_DISPLAY && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (macro != null) {
                finishMacro(generation, callback);
            }
            callback.onError(getString(R.string.touchpad_status_multidisplay_unsupported));
            return;
        }

        Path path = new Path();
        path.moveTo(startX, startY);
        if (startX != endX || startY != endY) {
            path.lineTo(endX, endY);
        }

        GestureDescription.Builder builder = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, durationMs));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setDisplayId(displayId);
        }
        GestureDescription gesture = builder.build();

        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                if (macro != null && isMacroActive(generation, callback)) {
                    runStep(macro, callback, index + 1, generation);
                }
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                if (macro != null && index >= 0
                        && isMacroActive(generation, callback)) {
                    finishMacro(generation, callback);
                    callback.onError(getString(R.string.macro_status_gesture_cancelled,
                            macro.steps.get(index).toString()));
                }
            }
        }, handler);

        if (!accepted) {
            if (macro != null && index >= 0) {
                finishMacro(generation, callback);
                callback.onError(getString(R.string.macro_status_gesture_submit_failed,
                        macro.steps.get(index).toString()));
            } else {
                callback.onError(getString(R.string.touchpad_status_gesture_submit_failed));
            }
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static GestureTarget parseTapTarget(String value) {
        float[] args = parseNumbers(value);
        if (args == null || (args.length != 2 && args.length != 3)) {
            return null;
        }
        if (args.length == 2) {
            return new GestureTarget(Display.DEFAULT_DISPLAY, args[0], args[1], args[0], args[1], 1L);
        }
        return new GestureTarget((int) args[0], args[1], args[2], args[1], args[2], 1L);
    }

    private static GestureTarget parseSwipeTarget(String value) {
        float[] args = parseNumbers(value);
        if (args == null || (args.length != 5 && args.length != 6)) {
            return null;
        }
        if (args.length == 5) {
            return new GestureTarget(Display.DEFAULT_DISPLAY, args[0], args[1], args[2], args[3], Math.max(1L, (long) args[4]));
        }
        return new GestureTarget((int) args[0], args[1], args[2], args[3], args[4], Math.max(1L, (long) args[5]));
    }

    private static GestureTarget parseHoldTarget(String value) {
        float[] args = parseNumbers(value);
        if (args == null || (args.length != 3 && args.length != 4)) {
            return null;
        }
        if (args.length == 3) {
            return new GestureTarget(Display.DEFAULT_DISPLAY, args[0], args[1], args[0], args[1], Math.max(80L, (long) args[2]));
        }
        return new GestureTarget((int) args[0], args[1], args[2], args[1], args[2], Math.max(80L, (long) args[3]));
    }

    private static long parseDuration(String value, long fallback) {
        if (value == null) {
            return fallback;
        }
        String cleaned = value.trim().toLowerCase(Locale.ROOT).replace("ms", "");
        try {
            return Math.max(0L, Long.parseLong(cleaned));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float[] parseNumbers(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace('\uff0c', ',').trim();
        String[] parts = normalized.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Float.parseFloat(parts[i].trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return result;
    }

    private static final class GestureTarget {
        final int displayId;
        final float startX;
        final float startY;
        final float endX;
        final float endY;
        final long durationMs;

        GestureTarget(int displayId, float startX, float startY, float endX, float endY, long durationMs) {
            this.displayId = displayId;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.durationMs = durationMs;
        }
    }
}
