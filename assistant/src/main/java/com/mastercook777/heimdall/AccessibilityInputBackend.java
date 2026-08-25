package com.mastercook777.heimdall;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import java.util.Locale;

public final class AccessibilityInputBackend implements InputBackend {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Object macroLock = new Object();
    private int nativeMacroGeneration;
    private Thread nativeMacroThread;
    private InputBridge.Callback nativeMacroCallback;
    private String nativeMacroLabel = "";
    private boolean nativeMacroCancelRequested;

    @Override
    public String name() {
        return "Accessibility";
    }

    @Override
    public boolean isReady() {
        return ThorAccessibilityService.isReady();
    }

    @Override
    public void openSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void dispatchMacro(Context context, Macro macro, InputBridge.Callback callback) {
        ThorAccessibilityService service = ThorAccessibilityService.getInstance();
        if (service == null) {
            if (isNativeOnlyMacro(macro)) {
                int generation = beginNativeMacro(macro, callback);
                dispatchNativeOnlyMacro(context, macro, callback, 0, generation);
                return;
            }
            callback.onError(context.getString(R.string.accessibility_macro_touch_required));
            return;
        }
        service.dispatchMacro(macro, callback);
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

    @Override
    public void cancelMacro(Context context) {
        ThorAccessibilityService service = ThorAccessibilityService.getInstance();
        if (service != null) {
            service.cancelMacro();
        }
        InputBridge.Callback callback;
        Thread worker;
        String label;
        boolean finishImmediately;
        synchronized (macroLock) {
            callback = nativeMacroCallback;
            worker = nativeMacroThread;
            label = nativeMacroLabel;
            finishImmediately = callback != null && worker == null;
            if (finishImmediately) {
                nativeMacroGeneration++;
                nativeMacroCallback = null;
                nativeMacroLabel = "";
            } else if (callback != null) {
                nativeMacroCancelRequested = true;
            }
        }
        if (worker != null) {
            worker.interrupt();
        }
        if (finishImmediately) {
            callback.onStatus(context.getString(R.string.macro_status_cancelled,
                    label.length() == 0
                            ? context.getString(R.string.common_macro_fallback) : label));
            callback.onMacroFinished(true);
        }
    }

    private int beginNativeMacro(Macro macro, InputBridge.Callback callback) {
        synchronized (macroLock) {
            nativeMacroGeneration++;
            nativeMacroCallback = callback;
            nativeMacroThread = null;
            nativeMacroLabel = macro == null || macro.label == null ? "" : macro.label;
            nativeMacroCancelRequested = false;
            return nativeMacroGeneration;
        }
    }

    private boolean isNativeMacroActive(int generation, InputBridge.Callback callback) {
        synchronized (macroLock) {
            return generation == nativeMacroGeneration && nativeMacroCallback == callback;
        }
    }

    private void finishNativeMacro(int generation, InputBridge.Callback callback) {
        synchronized (macroLock) {
            if (generation == nativeMacroGeneration && nativeMacroCallback == callback) {
                nativeMacroCallback = null;
                nativeMacroThread = null;
                nativeMacroLabel = "";
                nativeMacroCancelRequested = false;
            }
        }
    }

    private boolean isNativeMacroCancellationRequested(
            int generation, InputBridge.Callback callback) {
        synchronized (macroLock) {
            return generation == nativeMacroGeneration
                    && nativeMacroCallback == callback
                    && nativeMacroCancelRequested;
        }
    }

    private void dispatchNativeOnlyMacro(Context context, Macro macro,
            InputBridge.Callback callback, int index, int generation) {
        if (!isNativeMacroActive(generation, callback)) {
            return;
        }
        if (index >= macro.steps.size()) {
            callback.onStatus(context.getString(R.string.macro_status_completed, macro.label));
            finishNativeMacro(generation, callback);
            callback.onMacroFinished(false);
            return;
        }
        MacroStep step = macro.steps.get(index);
        if (MacroStep.TYPE_WAIT.equals(step.type)) {
            handler.postDelayed(() -> dispatchNativeOnlyMacro(
                    context, macro, callback, index + 1, generation),
                    parseDuration(step.value, 80));
            return;
        }
        if (MacroStep.TYPE_GAMEPAD.equals(step.type)) {
            Thread worker = new Thread(() -> {
                String result;
                try {
                    result = NativeGamepadPath.userFacingError(context,
                            InputBridge.replayNativeGamepadSequence(context, step.value));
                } catch (Throwable t) {
                    result = context.getString(R.string.native_controller_replay_failed);
                }
                String finalResult = result;
                handler.post(() -> {
                    if (!isNativeMacroActive(generation, callback)) {
                        return;
                    }
                    synchronized (macroLock) {
                        if (generation == nativeMacroGeneration
                                && nativeMacroCallback == callback) {
                            nativeMacroThread = null;
                        }
                    }
                    if (isNativeMacroCancellationRequested(generation, callback)) {
                        finishNativeMacro(generation, callback);
                        callback.onStatus(context.getString(
                                R.string.macro_status_cancelled, macro.label));
                        callback.onMacroFinished(true);
                        return;
                    }
                    if (NativeGamepadPath.operationSucceeded(finalResult)) {
                        callback.onStatus(context.getString(R.string.native_controller_step_complete));
                        handler.postDelayed(() -> dispatchNativeOnlyMacro(
                                context, macro, callback, index + 1, generation), 60);
                    } else {
                        finishNativeMacro(generation, callback);
                        callback.onError(finalResult);
                    }
                });
            }, "native-controller-macro");
            synchronized (macroLock) {
                if (generation != nativeMacroGeneration || nativeMacroCallback != callback) {
                    return;
                }
                nativeMacroThread = worker;
            }
            worker.start();
            return;
        }
        finishNativeMacro(generation, callback);
        callback.onError(context.getString(R.string.macro_status_unknown_step, step.toString()));
    }

    private long parseDuration(String value, long fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace("ms", "");
        try {
            return Math.max(0L, Long.parseLong(normalized));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    public boolean dispatchTouchMove(Context context, int displayId, int width, int height,
            float dx, float dy, InputBridge.Callback callback) {
        ThorAccessibilityService service = ThorAccessibilityService.getInstance();
        if (service == null) {
            callback.onError(context.getString(R.string.accessibility_enable_required));
            return false;
        }
        service.dispatchTouchMove(displayId, width, height, dx, dy, callback);
        return true;
    }

    @Override
    public boolean startTouchpadDrag(Context context, int displayId, int width, int height,
            float anchorX, float anchorY, InputBridge.Callback callback) {
        ThorAccessibilityService service = ThorAccessibilityService.getInstance();
        if (service == null) {
            callback.onError(context.getString(R.string.accessibility_enable_required));
            return false;
        }
        service.startTouchpadDrag(displayId, width, height, anchorX, anchorY, callback);
        return true;
    }

    @Override
    public boolean moveTouchpadDrag(Context context, float dx, float dy, int strokeMs,
            InputBridge.Callback callback) {
        ThorAccessibilityService service = ThorAccessibilityService.getInstance();
        if (service == null) {
            callback.onError(context.getString(R.string.accessibility_enable_required));
            return false;
        }
        service.moveTouchpadDrag(dx, dy, strokeMs, callback);
        return true;
    }

    @Override
    public boolean endTouchpadDrag(Context context, int strokeMs, InputBridge.Callback callback) {
        ThorAccessibilityService service = ThorAccessibilityService.getInstance();
        if (service == null) {
            callback.onError(context.getString(R.string.accessibility_enable_required));
            return false;
        }
        service.endTouchpadDrag(strokeMs, callback);
        return true;
    }

    @Override
    public boolean supportsMouseMode() {
        return false;
    }

    @Override
    public boolean supportsRelativeMove() {
        return false;
    }
}
