package com.mastercook777.heimdall;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replays upper-screen touch macros through Thor's mapping-compatible touch slot.
 *
 * The complete macro is validated before the first event so a mixed or malformed
 * macro never produces partial input. Timing stays in the App process; each Binder
 * call emits only one touch frame.
 */
final class ShizukuTouchMacroReplay {
    private static final int SWIPE_FRAME_MS = 8;
    private static final long TAP_DURATION_MS = 16L;

    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean cancelled = new AtomicBoolean();

    ShizukuTouchMacroReplay(ExecutorService executor) {
        this.executor = executor;
    }

    boolean isRunning() {
        return running.get();
    }

    void replay(Context context, Macro macro, boolean allowControllerSteps,
            InputBridge.Callback callback) {
        ReplayPlan plan = buildPlan(context, macro, allowControllerSteps, callback);
        if (plan == null) {
            return;
        }
        if (!running.compareAndSet(false, true)) {
            callback.onError(context.getString(R.string.macro_enhanced_touch_macro_busy));
            return;
        }
        cancelled.set(false);
        Context appContext = context.getApplicationContext();
        executor.execute(() -> execute(appContext, macro.label, plan, callback));
    }

    void cancel(Context context) {
        if (!running.get()) {
            return;
        }
        cancelled.set(true);
    }

    private void execute(Context context, String label, ReplayPlan plan,
            InputBridge.Callback callback) {
        boolean touchDown = false;
        String error = null;
        try {
            if (plan.hasGamepad) {
                requireGamepadRoute(context);
            }
            if (plan.hasTouch && !ShizukuNativeController.warmUp(context, 1500)) {
                throw new ReplayFailure(context.getString(R.string.native_touch_connection_failed));
            }
            for (PlannedStep step : plan.steps) {
                ensureNotCancelled();
                if (step.waitMs >= 0L) {
                    sleepResponsive(step.waitMs);
                    continue;
                }
                if (step.gamepadSequence != null) {
                    String result = NativeGamepadPath.userFacingError(context,
                            InputBridge.replayNativeGamepadSequence(
                                    context, step.gamepadSequence));
                    if (!NativeGamepadPath.operationSucceeded(result)) {
                        throw new ReplayFailure(result);
                    }
                    continue;
                }
                touchDown = true;
                requireFrame(context, step, MotionEvent.ACTION_DOWN,
                        step.startX, step.startY);
                if (step.startX == step.endX && step.startY == step.endY) {
                    sleepResponsive(step.durationMs);
                } else {
                    replaySwipe(context, step);
                }
                ensureNotCancelled();
                requireFrame(context, step, MotionEvent.ACTION_UP,
                        step.endX, step.endY);
                touchDown = false;
            }
        } catch (ReplayCancelled ignored) {
            // Activity/page teardown owns cancellation feedback; keep it quiet.
        } catch (ReplayFailure failure) {
            error = failure.getMessage();
        } catch (Throwable ignored) {
            error = context.getString(R.string.native_touch_injection_failed);
        } finally {
            if (touchDown || cancelled.get()) {
                ShizukuNativeController.releaseMappedTouch(context);
            }
            boolean wasCancelled = cancelled.get();
            cancelled.set(false);
            running.set(false);
            if (error != null) {
                String finalError = error;
                AssistantMainHandler.post(() -> callback.onError(finalError));
            } else if (!wasCancelled) {
                AssistantMainHandler.post(() -> callback.onStatus(
                        context.getString(R.string.macro_status_completed, label)));
            }
        }
    }

    private void requireGamepadRoute(Context context) throws ReplayFailure {
        NativeGamepadPath.Device device = NativeGamepadPath.resolveDevice();
        if (device == null) {
            throw new ReplayFailure(context.getString(
                    R.string.native_controller_replay_failed));
        }
        if (InputBridge.BACKEND_SHIZUKU.equals(
                InputBridge.selectedBackendId(context))) {
            if (!ShizukuNativeController.isReady()) {
                throw new ReplayFailure(context.getString(
                        R.string.controller_enhancement_unavailable));
            }
            return;
        }
        if (!device.writable) {
            throw new ReplayFailure(context.getString(
                    R.string.native_controller_permission_denied));
        }
    }

    private void replaySwipe(Context context, PlannedStep step) throws ReplayFailure, ReplayCancelled {
        long durationMs = Math.max(1L, step.durationMs);
        int frames = Math.max(1, (int) Math.ceil(durationMs / (double) SWIPE_FRAME_MS));
        long started = SystemClock.elapsedRealtime();
        for (int frame = 1; frame <= frames; frame++) {
            long targetElapsed = Math.round(durationMs * frame / (double) frames);
            sleepUntil(started + targetElapsed);
            ensureNotCancelled();
            float progress = frame / (float) frames;
            float x = step.startX + (step.endX - step.startX) * progress;
            float y = step.startY + (step.endY - step.startY) * progress;
            requireFrame(context, step, MotionEvent.ACTION_MOVE, x, y);
        }
    }

    private void requireFrame(Context context, PlannedStep step, int action,
            float x, float y) throws ReplayFailure {
        String result = ShizukuNativeController.injectMappedTouchEvent(context,
                Display.DEFAULT_DISPLAY, action, Math.round(x), Math.round(y),
                step.display.width, step.display.height, step.display.rotation);
        if ("ok".equals(result)) {
            return;
        }
        if (ShizukuNativeController.THOR_TOUCH_UNSUPPORTED.equals(result)) {
            throw new ReplayFailure(context.getString(
                    R.string.macro_enhanced_touch_route_unavailable));
        }
        throw new ReplayFailure(context.getString(R.string.native_touch_injection_failed));
    }

    private ReplayPlan buildPlan(Context context, Macro macro,
            boolean allowControllerSteps, InputBridge.Callback callback) {
        if (macro == null || macro.steps.isEmpty()) {
            String label = macro == null ? context.getString(R.string.common_macro_fallback) : macro.label;
            callback.onError(context.getString(R.string.macro_status_no_steps, label));
            return null;
        }
        List<PlannedStep> steps = new ArrayList<>();
        boolean hasTouch = false;
        boolean hasGamepad = false;
        DisplaySpec display = null;
        for (MacroStep step : macro.steps) {
            if (MacroStep.TYPE_GAMEPAD.equals(step.type)) {
                if (!allowControllerSteps) {
                    callback.onError(context.getString(
                            R.string.macro_enhanced_touch_controller_blocked));
                    return null;
                }
                if (step.value == null || step.value.trim().length() == 0) {
                    callback.onError(context.getString(
                            R.string.native_controller_replay_failed));
                    return null;
                }
                steps.add(PlannedStep.gamepadStep(step.value));
                hasGamepad = true;
                continue;
            }
            if (MacroStep.TYPE_WAIT.equals(step.type)) {
                steps.add(PlannedStep.waitStep(parseDuration(step.value, 80L)));
                continue;
            }
            TouchTarget target;
            if (MacroStep.TYPE_TAP.equals(step.type)) {
                target = parseTapTarget(step.value);
                if (target == null) {
                    callback.onError(context.getString(R.string.macro_status_invalid_tap, step.value));
                    return null;
                }
            } else if (MacroStep.TYPE_HOLD.equals(step.type)) {
                target = parseHoldTarget(step.value);
                if (target == null) {
                    callback.onError(context.getString(R.string.macro_status_invalid_hold, step.value));
                    return null;
                }
            } else if (MacroStep.TYPE_SWIPE.equals(step.type)) {
                target = parseSwipeTarget(step.value);
                if (target == null) {
                    callback.onError(context.getString(R.string.macro_status_invalid_swipe, step.value));
                    return null;
                }
            } else {
                callback.onError(context.getString(R.string.macro_status_unknown_step, step.toString()));
                return null;
            }
            if (target.displayId != Display.DEFAULT_DISPLAY) {
                callback.onError(context.getString(R.string.macro_enhanced_touch_upper_only));
                return null;
            }
            if (display == null) {
                display = resolveDisplay(context, Display.DEFAULT_DISPLAY);
                if (display == null) {
                    callback.onError(context.getString(R.string.native_touch_missing_dimensions));
                    return null;
                }
            }
            hasTouch = true;
            steps.add(PlannedStep.touchStep(display, target));
        }
        return new ReplayPlan(steps, hasTouch, hasGamepad);
    }

    private static DisplaySpec resolveDisplay(Context context, int displayId) {
        DisplayManager manager = context.getSystemService(DisplayManager.class);
        Display display = manager == null ? null : manager.getDisplay(displayId);
        if (display == null || !display.isValid()) {
            return null;
        }
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        try {
            Display.Mode mode = display.getMode();
            if (mode != null && mode.getPhysicalWidth() > 0 && mode.getPhysicalHeight() > 0) {
                int physicalWidth = mode.getPhysicalWidth();
                int physicalHeight = mode.getPhysicalHeight();
                boolean metricsLandscape = width >= height;
                boolean modeLandscape = physicalWidth >= physicalHeight;
                if (metricsLandscape != modeLandscape) {
                    int swap = physicalWidth;
                    physicalWidth = physicalHeight;
                    physicalHeight = swap;
                }
                width = physicalWidth;
                height = physicalHeight;
            }
        } catch (Throwable ignored) {
        }
        return width > 0 && height > 0
                ? new DisplaySpec(width, height, display.getRotation()) : null;
    }

    private void sleepResponsive(long durationMs) throws ReplayCancelled {
        sleepUntil(SystemClock.elapsedRealtime() + Math.max(0L, durationMs));
    }

    private void sleepUntil(long deadlineMs) throws ReplayCancelled {
        while (true) {
            ensureNotCancelled();
            long remaining = deadlineMs - SystemClock.elapsedRealtime();
            if (remaining <= 0L) {
                return;
            }
            try {
                Thread.sleep(Math.min(remaining, 16L));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                throw new ReplayCancelled();
            }
        }
    }

    private void ensureNotCancelled() throws ReplayCancelled {
        if (cancelled.get()) {
            throw new ReplayCancelled();
        }
    }

    private static TouchTarget parseTapTarget(String value) {
        float[] args = parseNumbers(value);
        if (args == null || (args.length != 2 && args.length != 3)) {
            return null;
        }
        if (args.length == 2) {
            return new TouchTarget(Display.DEFAULT_DISPLAY,
                    args[0], args[1], args[0], args[1], TAP_DURATION_MS);
        }
        return new TouchTarget((int) args[0],
                args[1], args[2], args[1], args[2], TAP_DURATION_MS);
    }

    private static TouchTarget parseHoldTarget(String value) {
        float[] args = parseNumbers(value);
        if (args == null || (args.length != 3 && args.length != 4)) {
            return null;
        }
        if (args.length == 3) {
            return new TouchTarget(Display.DEFAULT_DISPLAY,
                    args[0], args[1], args[0], args[1], Math.max(80L, (long) args[2]));
        }
        return new TouchTarget((int) args[0],
                args[1], args[2], args[1], args[2], Math.max(80L, (long) args[3]));
    }

    private static TouchTarget parseSwipeTarget(String value) {
        float[] args = parseNumbers(value);
        if (args == null || (args.length != 5 && args.length != 6)) {
            return null;
        }
        if (args.length == 5) {
            return new TouchTarget(Display.DEFAULT_DISPLAY,
                    args[0], args[1], args[2], args[3], Math.max(1L, (long) args[4]));
        }
        return new TouchTarget((int) args[0],
                args[1], args[2], args[3], args[4], Math.max(1L, (long) args[5]));
    }

    private static float[] parseNumbers(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.replace('\uff0c', ',').split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Float.parseFloat(parts[i].trim());
                if (Float.isNaN(result[i]) || Float.isInfinite(result[i])) {
                    return null;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return result;
    }

    private static long parseDuration(String value, long fallbackValue) {
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

    private static final class ReplayPlan {
        final List<PlannedStep> steps;
        final boolean hasTouch;
        final boolean hasGamepad;

        ReplayPlan(List<PlannedStep> steps, boolean hasTouch,
                boolean hasGamepad) {
            this.steps = steps;
            this.hasTouch = hasTouch;
            this.hasGamepad = hasGamepad;
        }
    }

    private static final class PlannedStep {
        final long waitMs;
        final String gamepadSequence;
        final DisplaySpec display;
        final float startX;
        final float startY;
        final float endX;
        final float endY;
        final long durationMs;

        private PlannedStep(long waitMs, String gamepadSequence, DisplaySpec display,
                float startX, float startY, float endX, float endY, long durationMs) {
            this.waitMs = waitMs;
            this.gamepadSequence = gamepadSequence;
            this.display = display;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.durationMs = durationMs;
        }

        static PlannedStep waitStep(long waitMs) {
            return new PlannedStep(waitMs, null, null, 0f, 0f, 0f, 0f, 0L);
        }

        static PlannedStep gamepadStep(String sequence) {
            return new PlannedStep(-1L, sequence, null,
                    0f, 0f, 0f, 0f, 0L);
        }

        static PlannedStep touchStep(DisplaySpec display, TouchTarget target) {
            return new PlannedStep(-1L, null, display, target.startX, target.startY,
                    target.endX, target.endY, target.durationMs);
        }
    }

    private static final class DisplaySpec {
        final int width;
        final int height;
        final int rotation;

        DisplaySpec(int width, int height, int rotation) {
            this.width = width;
            this.height = height;
            this.rotation = rotation;
        }
    }

    private static final class TouchTarget {
        final int displayId;
        final float startX;
        final float startY;
        final float endX;
        final float endY;
        final long durationMs;

        TouchTarget(int displayId, float startX, float startY,
                float endX, float endY, long durationMs) {
            this.displayId = displayId;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.durationMs = durationMs;
        }
    }

    private static final class ReplayFailure extends Exception {
        ReplayFailure(String message) {
            super(message);
        }
    }

    private static final class ReplayCancelled extends Exception {
    }
}
