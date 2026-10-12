package com.mastercook777.heimdall;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Surface;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public final class UpperScreenProjectionService extends Service {
    static final String ACTION_START =
            BuildConfig.APPLICATION_ID + ".START_MAGNIFIER_PROJECTION";
    static final String ACTION_STOP =
            BuildConfig.APPLICATION_ID + ".STOP_MAGNIFIER_PROJECTION";
    static final String EXTRA_RESULT_CODE = "result_code";
    static final String EXTRA_RESULT_DATA = "result_data";
    static final String EXTRA_WIDTH = "width";
    static final String EXTRA_HEIGHT = "height";
    static final String EXTRA_DENSITY = "density";

    interface Listener {
        void onProjectionStatus(String message);
        void onProjectionConfigurationChanged();
    }

    private static final String CHANNEL_ID = "heimdall_live_magnifier";
    private static final int NOTIFICATION_ID = 1402;
    private static final Object SURFACE_LOCK = new Object();

    private static volatile UpperScreenProjectionService activeService;
    private static volatile boolean running;
    private static volatile boolean starting;
    private static volatile boolean frozen;
    private static final CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<>();
    private static final Map<String, Boolean> portalFrozen = new ConcurrentHashMap<>();
    private static final Map<String, PortalSurfaceFanOut.Spec> portalSpecs =
            new ConcurrentHashMap<>();
    private static volatile Surface outputSurface;
    private static volatile int portalViewCount = 1;
    private static volatile float regionLeft = 0.25f;
    private static volatile float regionTop = 0.25f;
    private static volatile float regionRight = 0.75f;
    private static volatile float regionBottom = 0.75f;
    private static volatile float targetAspectRatio = 1f;
    private static volatile int targetFps = 30;
    private static volatile float targetZoom = 1f;
    private static volatile int sourceWidth = 1920;
    private static volatile int sourceHeight = 1080;

    private final Handler mainHandler = new Handler(android.os.Looper.getMainLooper());
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private PortalSurfaceFanOut portalFanOut;
    private int densityDpi = 320;
    private boolean stopping;

    static boolean isRunning() {
        return running;
    }

    static boolean isActiveOrStarting() {
        return running || starting;
    }

    static boolean isFrozen() {
        return running && frozen;
    }

    static boolean isMultiPortalMode() {
        return portalViewCount > 1;
    }

    static boolean isPortalFrozen(String itemId) {
        return isMultiPortalMode()
                ? running && Boolean.TRUE.equals(portalFrozen.get(itemId))
                : isFrozen();
    }

    static void setPortalViewCount(int count) {
        int normalized = count > 1 ? 2 : 1;
        if (portalViewCount == normalized) return;
        portalViewCount = normalized;
        frozen = false;
        portalFrozen.clear();
        portalSpecs.clear();
        UpperScreenProjectionService service = activeService;
        if (service != null) service.mainHandler.post(service::reconfigureOutputMode);
        notifyConfigurationChanged();
    }

    static void markStarting() {
        starting = true;
    }

    static void cancelStarting() {
        starting = false;
    }

    static void setListener(Listener value) {
        if (value != null) {
            listeners.add(value);
            value.onProjectionConfigurationChanged();
        }
    }

    static void clearListener(Listener value) {
        listeners.remove(value);
    }

    static void attachOutputSurface(Surface surface) {
        synchronized (SURFACE_LOCK) {
            outputSurface = surface;
        }
        UpperScreenProjectionService service = activeService;
        if (service != null) {
            service.requestOutputSurfaceUpdate();
        }
    }

    static void detachOutputSurface(Surface surface) {
        synchronized (SURFACE_LOCK) {
            if (outputSurface == surface) {
                outputSurface = null;
            }
        }
        UpperScreenProjectionService service = activeService;
        if (service != null) {
            service.requestOutputSurfaceUpdate();
        }
    }

    static void attachPortalOutput(String itemId, Surface surface, int width, int height,
            WidgetLayout.Item item) {
        portalSpecs.put(itemId, specFromItem(item));
        UpperScreenProjectionService service = activeService;
        if (service != null) {
            service.mainHandler.post(() -> service.attachPortalOutputInternal(
                    itemId, surface, width, height, specFromItem(item)));
        }
    }

    static void updatePortalSpec(String itemId, WidgetLayout.Item item) {
        portalSpecs.put(itemId, specFromItem(item));
        UpperScreenProjectionService service = activeService;
        if (service != null) {
            service.mainHandler.post(() -> {
                PortalSurfaceFanOut fanOut = service.portalFanOut;
                if (fanOut != null) fanOut.updateTargetSpec(itemId, specFromItem(item));
            });
        }
        notifyConfigurationChanged();
    }

    static void updatePortalFps(String itemId, int fps) {
        int normalizedFps = WidgetLayout.normalizeMagnifierFps(fps);
        PortalSurfaceFanOut.Spec current = portalSpecs.get(itemId);
        if (current != null) {
            portalSpecs.put(itemId, current.withFps(normalizedFps));
        }
        UpperScreenProjectionService service = activeService;
        if (service != null) {
            service.mainHandler.post(() -> {
                PortalSurfaceFanOut fanOut = service.portalFanOut;
                if (fanOut != null) fanOut.updateTargetFps(itemId, normalizedFps);
            });
        }
    }

    static void updatePortalTargetSize(String itemId, int width, int height) {
        UpperScreenProjectionService service = activeService;
        if (service != null) {
            service.mainHandler.post(() -> {
                PortalSurfaceFanOut fanOut = service.portalFanOut;
                if (fanOut != null) fanOut.updateTargetSize(itemId, width, height);
            });
        }
    }

    static PortalSurfaceFanOut.Spec portalSpec(String itemId, WidgetLayout.Item fallback) {
        PortalSurfaceFanOut.Spec spec = portalSpecs.get(itemId);
        return spec == null ? specFromItem(fallback) : spec;
    }

    static void detachPortalOutput(String itemId) {
        UpperScreenProjectionService service = activeService;
        if (service != null) {
            PortalSurfaceFanOut fanOut = service.portalFanOut;
            if (fanOut != null) fanOut.removeTargetAndWait(itemId);
        }
    }

    static void setPortalFrozen(String itemId, boolean value) {
        if (!isMultiPortalMode()) {
            setFrozen(value);
            return;
        }
        portalFrozen.put(itemId, value);
        UpperScreenProjectionService service = activeService;
        if (service != null) {
            service.mainHandler.post(() -> {
                PortalSurfaceFanOut fanOut = service.portalFanOut;
                if (fanOut != null) fanOut.setFrozen(itemId, value);
            });
            service.requestOutputSurfaceUpdate();
        }
        notifyConfigurationChanged();
    }

    static void setFrozen(boolean value) {
        frozen = value;
        UpperScreenProjectionService service = activeService;
        if (service != null) {
            service.requestOutputSurfaceUpdate();
        }
        notifyConfigurationChanged();
    }

    static void setRegion(float left, float top, float right, float bottom) {
        regionLeft = clamp(left, 0f, 0.98f);
        regionTop = clamp(top, 0f, 0.98f);
        regionRight = clamp(right, regionLeft + 0.02f, 1f);
        regionBottom = clamp(bottom, regionTop + 0.02f, 1f);
        notifyConfigurationChanged();
    }

    static void setTuning(float aspectRatio, int fps, float zoom) {
        targetAspectRatio = clamp(aspectRatio, 0.2f, 5f);
        targetFps = WidgetLayout.normalizeMagnifierFps(fps);
        targetZoom = WidgetLayout.normalizeMagnifierZoom(zoom);
        UpperScreenProjectionService service = activeService;
        if (service != null) {
            service.mainHandler.post(service::applyFrameRate);
        }
        notifyConfigurationChanged();
    }

    static void setFrameRate(int fps) {
        targetFps = WidgetLayout.normalizeMagnifierFps(fps);
        UpperScreenProjectionService service = activeService;
        if (service != null) {
            service.mainHandler.post(service::applyFrameRate);
        }
    }

    static float regionLeft() { return regionLeft; }
    static float regionTop() { return regionTop; }
    static float regionRight() { return regionRight; }
    static float regionBottom() { return regionBottom; }
    static float targetAspectRatio() { return targetAspectRatio; }
    static float targetZoom() { return targetZoom; }
    static int sourceWidth() { return sourceWidth; }
    static int sourceHeight() { return sourceHeight; }

    private static void notifyConfigurationChanged() {
        for (Listener current : listeners) {
            current.onProjectionConfigurationChanged();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        activeService = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        if (ACTION_STOP.equals(intent.getAction())) {
            stopProjection(getString(R.string.magnifier_service_status_stopped));
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(intent.getAction()) && !running) {
            starting = true;
            startForeground(NOTIFICATION_ID, createNotification());
            startProjection(intent);
        }
        return START_NOT_STICKY;
    }

    private void startProjection(Intent intent) {
        try {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
            sourceWidth = Math.max(640, intent.getIntExtra(EXTRA_WIDTH, 1920));
            sourceHeight = Math.max(360, intent.getIntExtra(EXTRA_HEIGHT, 1080));
            densityDpi = Math.max(1, intent.getIntExtra(EXTRA_DENSITY, 320));

            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            projection = manager == null ? null : manager.getMediaProjection(resultCode, resultData);
            if (projection == null) {
                throw new IllegalStateException("media projection unavailable");
            }
            projection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    stopProjection(getString(R.string.magnifier_status_permission_ended));
                }

                @Override
                public void onCapturedContentResize(int width, int height) {
                    applyCapturedContentSize(width, height);
                }
            }, mainHandler);

            running = true;
            starting = false;
            notifyConfigurationChanged();
            updateOutputSurface();
            dispatchStatus(getString(R.string.magnifier_status_connected));
        } catch (Throwable ignored) {
            stopProjection(getString(R.string.magnifier_status_start_failed));
        }
    }

    private void updateOutputSurface() {
        if (projection == null || !running) {
            return;
        }
        Surface activeTarget;
        if (isMultiPortalMode()) {
            Surface fanOutSurface = ensurePortalFanOut();
            activeTarget = allPortalsFrozen() ? null : fanOutSurface;
        } else {
            Surface target;
            synchronized (SURFACE_LOCK) {
                target = outputSurface;
            }
            activeTarget = !frozen && target != null && target.isValid() ? target : null;
        }
        if (virtualDisplay == null) {
            if (activeTarget == null) {
                return;
            }
            virtualDisplay = projection.createVirtualDisplay(
                    "HeimdallLiveMagnifierSurface",
                    sourceWidth,
                    sourceHeight,
                    densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    activeTarget,
                    null,
                    mainHandler);
            if (virtualDisplay == null) {
                stopProjection(getString(R.string.magnifier_status_surface_failed));
                return;
            }
        } else {
            virtualDisplay.setSurface(activeTarget);
        }
        applyFrameRate();
    }

    private void attachPortalOutputInternal(String itemId, Surface surface, int width, int height,
            PortalSurfaceFanOut.Spec spec) {
        if (!isMultiPortalMode() || itemId == null || itemId.isEmpty()) return;
        PortalSurfaceFanOut fanOut = portalFanOut;
        if (fanOut == null) {
            requestOutputSurfaceUpdate();
            fanOut = portalFanOut;
        }
        if (fanOut != null) {
            fanOut.putTarget(itemId, surface, width, height, spec);
            fanOut.setFrozen(itemId, Boolean.TRUE.equals(portalFrozen.get(itemId)));
        }
    }

    private Surface ensurePortalFanOut() {
        if (portalFanOut == null) {
            portalFanOut = new PortalSurfaceFanOut(sourceWidth, sourceHeight);
            Surface surface = portalFanOut.start();
            if (surface == null || !surface.isValid()) {
                portalFanOut.release();
                portalFanOut = null;
                stopProjection(getString(R.string.magnifier_status_surface_failed));
                return null;
            }
        }
        return portalFanOut.inputSurface();
    }

    private void reconfigureOutputMode() {
        if (virtualDisplay != null) {
            try {
                virtualDisplay.setSurface(null);
            } catch (Throwable ignored) {
            }
        }
        if (!isMultiPortalMode()) releasePortalFanOut();
        updateOutputSurface();
    }

    private void releasePortalFanOut() {
        PortalSurfaceFanOut fanOut = portalFanOut;
        portalFanOut = null;
        if (fanOut != null) fanOut.release();
    }

    private static boolean allPortalsFrozen() {
        if (!isMultiPortalMode()) return false;
        int frozenCount = 0;
        for (Boolean value : portalFrozen.values()) {
            if (Boolean.TRUE.equals(value)) frozenCount++;
        }
        return frozenCount >= portalViewCount;
    }

    private void requestOutputSurfaceUpdate() {
        if (Looper.myLooper() == mainHandler.getLooper()) {
            // TextureView releases its Surface immediately after detachOutputSurface returns.
            // Detach the VirtualDisplay first so it can never retain that released producer.
            updateOutputSurface();
        } else {
            mainHandler.post(this::updateOutputSurface);
        }
    }

    private void applyFrameRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        Surface target = isMultiPortalMode() && portalFanOut != null
                ? portalFanOut.inputSurface() : null;
        if (!isMultiPortalMode()) {
            synchronized (SURFACE_LOCK) {
                target = outputSurface;
            }
        }
        if (target != null && target.isValid()) {
            try {
                target.setFrameRate(isMultiPortalMode() ? 60 : targetFps,
                        Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
            } catch (Throwable ignored) {
            }
        }
    }

    private void applyCapturedContentSize(int width, int height) {
        if (!running || width <= 0 || height <= 0
                || (sourceWidth == width && sourceHeight == height)) {
            return;
        }
        sourceWidth = width;
        sourceHeight = height;
        if (portalFanOut != null) portalFanOut.setSourceSize(width, height);
        notifyConfigurationChanged();
        mainHandler.post(() -> {
            if (virtualDisplay != null && running) {
                try {
                    virtualDisplay.resize(sourceWidth, sourceHeight, densityDpi);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private synchronized void stopProjection(String status) {
        if (stopping) {
            return;
        }
        stopping = true;
        running = false;
        starting = false;
        frozen = false;
        portalFrozen.clear();
        portalSpecs.clear();
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        releasePortalFanOut();
        if (projection != null) {
            MediaProjection active = projection;
            projection = null;
            try {
                active.stop();
            } catch (Throwable ignored) {
            }
        }
        dispatchStatus(status);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        stopping = false;
    }

    private void dispatchStatus(String status) {
        mainHandler.post(() -> {
            for (Listener current : listeners) current.onProjectionStatus(status);
        });
    }

    private Notification createNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, getString(R.string.magnifier_notification_channel),
                    NotificationManager.IMPORTANCE_LOW));
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(R.drawable.ic_camera)
                .setContentTitle(getString(R.string.magnifier_notification_title))
                .setContentText(getString(R.string.magnifier_notification_text))
                .setOngoing(true)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (activeService == this) {
            activeService = null;
        }
        if (running || projection != null) {
            stopProjection(getString(R.string.magnifier_service_status_stopped));
        }
        super.onDestroy();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static PortalSurfaceFanOut.Spec specFromItem(WidgetLayout.Item item) {
        if (item == null) {
            return new PortalSurfaceFanOut.Spec(0.25f, 0.25f, 0.75f, 0.75f, 1f, 1f, 30);
        }
        return new PortalSurfaceFanOut.Spec(
                item.magnifierLeft, item.magnifierTop,
                item.magnifierRight, item.magnifierBottom,
                WidgetLayout.magnifierTargetAspectRatio(item),
                item.magnifierZoom, item.magnifierFps);
    }
}
