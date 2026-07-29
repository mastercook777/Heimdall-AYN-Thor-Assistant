package com.mastercook777.heimdall;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import java.util.Locale;

public final class AccessibilityInputBackend implements InputBackend {
    private final Handler handler = new Handler(Looper.getMainLooper());

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
                dispatchNativeOnlyMacro(context, macro, callback, 0);
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

    private void dispatchNativeOnlyMacro(Context context, Macro macro, InputBridge.Callback callback, int index) {
        if (index >= macro.steps.size()) {
            callback.onStatus(context.getString(R.string.macro_status_completed, macro.label));
            return;
        }
        MacroStep step = macro.steps.get(index);
        if (MacroStep.TYPE_WAIT.equals(step.type)) {
            handler.postDelayed(() -> dispatchNativeOnlyMacro(context, macro, callback, index + 1), parseDuration(step.value, 80));
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
                handler.post(() -> {
                    if (NativeGamepadPath.operationSucceeded(finalResult)) {
                        callback.onStatus(context.getString(R.string.native_controller_step_complete));
                    } else {
                        callback.onError(finalResult);
                    }
                    handler.postDelayed(() -> dispatchNativeOnlyMacro(context, macro, callback, index + 1), 60);
                });
            }, "native-controller-macro").start();
            return;
        }
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
