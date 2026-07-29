package com.mastercook777.heimdall;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ShizukuInputBackend implements InputBackend {
    private final AccessibilityInputBackend fallback = new AccessibilityInputBackend();
    private final Object touchLock = new Object();
    private final ExecutorService touchExecutor = Executors.newSingleThreadExecutor();
    private final ShizukuTouchMacroReplay touchMacroReplay =
            new ShizukuTouchMacroReplay(touchExecutor);
    private boolean nativeTouchActive;
    private boolean nativeTouchFrameScheduled;
    private int nativeTouchDisplayId;
    private int nativeTouchWidth;
    private int nativeTouchHeight;
    private int nativeTouchRotation;
    private float nativeTouchX;
    private float nativeTouchY;
    private float pendingNativeTouchDx;
    private float pendingNativeTouchDy;
    private int pendingNativeTouchFrameMs = 8;
    private InputBridge.Callback nativeTouchCallback;

    @Override
    public String name() {
        return "Shizuku / Shell";
    }

    @Override
    public boolean isReady() {
        return ShizukuNativeController.isReady();
    }

    @Override
    public void openSettings(Context context) {
        ShizukuNativeController.requestPermission();
    }

    @Override
    public void dispatchMacro(Context context, Macro macro, InputBridge.Callback callback) {
        if (isNativeOnlyMacro(macro)) {
            dispatchNativeOnlyMacro(context, macro, callback, 0);
            return;
        }
        fallback.dispatchMacro(context, macro, callback);
    }

    public void dispatchMappedTouchMacro(Context context, Macro macro,
            InputBridge.Callback callback) {
        synchronized (touchLock) {
            if (nativeTouchActive) {
                callback.onError(context.getString(
                        R.string.macro_enhanced_touch_gesture_busy));
                return;
            }
            touchMacroReplay.replay(context, macro, callback);
        }
    }

    private boolean isNativeOnlyMacro(Macro macro) {
        if (macro == null || macro.steps.isEmpty()) {
            return false;
        }
        for (MacroStep step : macro.steps) {
            if (!MacroStep.TYPE_WAIT.equals(step.type) && !MacroStep.TYPE_GAMEPAD.equals(step.type)) {
                return false;
            }
        }
        return true;
    }

    private void dispatchNativeOnlyMacro(Context context, Macro macro, InputBridge.Callback callback, int index) {
        if (index >= macro.steps.size()) {
            callback.onStatus(context.getString(R.string.macro_status_completed, macro.label));
            return;
        }
        MacroStep step = macro.steps.get(index);
        if (MacroStep.TYPE_WAIT.equals(step.type)) {
            AssistantMainHandler.postDelayed(() -> dispatchNativeOnlyMacro(context, macro, callback, index + 1),
                    parseDuration(step.value, 80));
            return;
        }
        if (MacroStep.TYPE_GAMEPAD.equals(step.type)) {
            new Thread(() -> {
                String result;
                try {
                    result = NativeGamepadPath.userFacingError(context,
                            InputBridge.replayNativeGamepadSequence(context, step.value));
                } catch (Throwable t) {
                    result = context.getString(R.string.native_controller_replay_failed);
                }
                String finalResult = result;
                AssistantMainHandler.post(() -> {
                    if (NativeGamepadPath.operationSucceeded(finalResult)) {
                        callback.onStatus(context.getString(R.string.native_controller_step_complete));
                    } else {
                        callback.onError(finalResult);
                    }
                    AssistantMainHandler.postDelayed(() -> dispatchNativeOnlyMacro(context, macro, callback, index + 1), 60);
                });
            }, "shizuku-controller-macro").start();
            return;
        }
        callback.onError(context.getString(R.string.macro_status_unknown_step, step.toString()));
    }

    private long parseDuration(String value, long fallbackValue) {
        if (value == null) {
            return fallbackValue;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace("ms", "");
        try {
            return Math.max(0L, Long.parseLong(normalized));
        } catch (NumberFormatException ignored) {
            return fallbackValue;
        }
    }

    @Override
    public boolean dispatchTouchMove(Context context, int displayId, int width, int height,
            float dx, float dy, InputBridge.Callback callback) {
        return fallback.dispatchTouchMove(context, displayId, width, height, dx, dy, callback);
    }

    @Override
    public boolean startTouchpadDrag(Context context, int displayId, int width, int height,
            float anchorX, float anchorY, InputBridge.Callback callback) {
        return fallback.startTouchpadDrag(context, displayId, width, height, anchorX, anchorY, callback);
    }

    @Override
    public boolean moveTouchpadDrag(Context context, float dx, float dy, int strokeMs,
            InputBridge.Callback callback) {
        return fallback.moveTouchpadDrag(context, dx, dy, strokeMs, callback);
    }

    @Override
    public boolean endTouchpadDrag(Context context, int strokeMs, InputBridge.Callback callback) {
        return fallback.endTouchpadDrag(context, strokeMs, callback);
    }

    public boolean startNativeTouchDrag(Context context, int displayId, int width, int height,
            float anchorX, float anchorY, InputBridge.Callback callback) {
        if (!ShizukuNativeController.isReady()) {
            callback.onError(context.getString(R.string.native_touch_shizuku_required));
            ShizukuNativeController.requestPermission();
            return false;
        }
        if (width <= 0 || height <= 0) {
            callback.onError(context.getString(R.string.native_touch_missing_dimensions));
            return false;
        }
        if (!ShizukuNativeController.warmUp(context, 1500)) {
            callback.onError(context.getString(R.string.native_touch_connection_failed));
            return false;
        }
        synchronized (touchLock) {
            if (touchMacroReplay.isRunning()) {
                callback.onError(context.getString(
                        R.string.touchpad_enhanced_touch_macro_busy));
                return false;
            }
            nativeTouchDisplayId = displayId;
            nativeTouchWidth = width;
            nativeTouchHeight = height;
            nativeTouchRotation = resolveDisplayRotation(context, displayId);
            nativeTouchX = clamp(width * anchorX, 1f, width - 1f);
            nativeTouchY = clamp(height * anchorY, 1f, height - 1f);
            pendingNativeTouchDx = 0f;
            pendingNativeTouchDy = 0f;
            nativeTouchCallback = callback;
            nativeTouchActive = true;
            nativeTouchFrameScheduled = false;
        }
        enqueueNativeTouchEvent(context, MotionEvent.ACTION_DOWN, Math.round(width * anchorX), Math.round(height * anchorY), callback);
        return true;
    }

    public boolean moveNativeTouchDrag(Context context, float dx, float dy, int strokeMs,
            InputBridge.Callback callback) {
        synchronized (touchLock) {
            if (!nativeTouchActive) {
                callback.onError(context.getString(R.string.native_touch_not_pressed));
                return false;
            }
            nativeTouchCallback = callback;
            pendingNativeTouchDx += dx;
            pendingNativeTouchDy += dy;
            pendingNativeTouchFrameMs = Math.max(4, strokeMs);
            scheduleNativeTouchLocked(context);
        }
        return true;
    }

    public boolean endNativeTouchDrag(Context context, int strokeMs, InputBridge.Callback callback) {
        int x;
        int y;
        synchronized (touchLock) {
            nativeTouchCallback = callback;
            x = Math.round(nativeTouchX);
            y = Math.round(nativeTouchY);
            nativeTouchActive = false;
            pendingNativeTouchDx = 0f;
            pendingNativeTouchDy = 0f;
        }
        enqueueNativeTouchEvent(context, MotionEvent.ACTION_UP, x, y, callback);
        return true;
    }

    public void cancelNativeTouchDrag(Context context) {
        touchMacroReplay.cancel(context);
        int x;
        int y;
        InputBridge.Callback callback;
        synchronized (touchLock) {
            if (!nativeTouchActive) {
                return;
            }
            x = Math.round(nativeTouchX);
            y = Math.round(nativeTouchY);
            callback = nativeTouchCallback;
            nativeTouchActive = false;
            pendingNativeTouchDx = 0f;
            pendingNativeTouchDy = 0f;
        }
        enqueueNativeTouchEvent(context, MotionEvent.ACTION_CANCEL, x, y, callback);
    }

    private void scheduleNativeTouchLocked(Context context) {
        if (nativeTouchFrameScheduled) {
            return;
        }
        if (Math.abs(pendingNativeTouchDx) < 0.01f && Math.abs(pendingNativeTouchDy) < 0.01f) {
            return;
        }
        nativeTouchFrameScheduled = true;
        int frameMs = pendingNativeTouchFrameMs;
        touchExecutor.execute(() -> {
            sleepQuietly(frameMs);
            int displayId;
            int width;
            int height;
            int rotation;
            int x2;
            int y2;
            synchronized (touchLock) {
                if (Math.abs(pendingNativeTouchDx) < 0.01f && Math.abs(pendingNativeTouchDy) < 0.01f) {
                    nativeTouchFrameScheduled = false;
                    return;
                }
                float endX = clamp(nativeTouchX + pendingNativeTouchDx, 1f, nativeTouchWidth - 1f);
                float endY = clamp(nativeTouchY + pendingNativeTouchDy, 1f, nativeTouchHeight - 1f);
                pendingNativeTouchDx = 0f;
                pendingNativeTouchDy = 0f;
                nativeTouchX = endX;
                nativeTouchY = endY;
                displayId = nativeTouchDisplayId;
                width = nativeTouchWidth;
                height = nativeTouchHeight;
                rotation = nativeTouchRotation;
                x2 = Math.round(endX);
                y2 = Math.round(endY);
            }
            String result = ShizukuNativeController.injectTouchEvent(context, displayId,
                    MotionEvent.ACTION_MOVE, x2, y2, width, height, rotation);
            InputBridge.Callback callback;
            synchronized (touchLock) {
                nativeTouchFrameScheduled = false;
                callback = nativeTouchCallback;
                if (nativeTouchActive) {
                    scheduleNativeTouchLocked(context);
                }
            }
            if (result != null && !"ok".equals(result) && callback != null) {
                AssistantMainHandler.post(() -> callback.onError(
                        context.getString(R.string.native_touch_injection_failed)));
            }
        });
    }

    private void sleepQuietly(int frameMs) {
        try {
            Thread.sleep(Math.max(1L, frameMs));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void enqueueNativeTouchEvent(Context context, int action, int x, int y, InputBridge.Callback callback) {
        int displayId;
        int width;
        int height;
        int rotation;
        synchronized (touchLock) {
            displayId = nativeTouchDisplayId;
            width = nativeTouchWidth;
            height = nativeTouchHeight;
            rotation = nativeTouchRotation;
        }
        touchExecutor.execute(() -> {
            String result = ShizukuNativeController.injectTouchEvent(context, displayId,
                    action, x, y, width, height, rotation);
            if (result != null && !"ok".equals(result) && callback != null) {
                AssistantMainHandler.post(() -> callback.onError(
                        context.getString(R.string.native_touch_injection_failed)));
            }
        });
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int resolveDisplayRotation(Context context, int displayId) {
        DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        Display display = displayManager == null ? null : displayManager.getDisplay(displayId);
        return display == null ? Surface.ROTATION_0 : display.getRotation();
    }

    @Override
    public boolean supportsMouseMode() {
        return false;
    }

    @Override
    public boolean supportsRelativeMove() {
        return true;
    }
}
