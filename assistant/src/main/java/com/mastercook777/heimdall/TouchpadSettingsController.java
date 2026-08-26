package com.mastercook777.heimdall;

import android.content.res.ColorStateList;
import android.os.Build;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

final class TouchpadSettingsController {
    interface Host {
        TouchpadSettings draft();
        String mode();
        void setMode(String mode);
        boolean advancedVisible();
        void setAdvancedVisible(boolean visible);
        boolean modeAvailable(String mode);
        boolean relativeMouseBackendAvailable();
        void refresh();
        void showError(String message);
    }

    private final AssistantActivity activity;
    private final Host host;
    private SliderInput sensitivityInput;
    private SliderInput intervalInput;
    private SliderInput minDeltaInput;
    private SliderInput anchorXInput;
    private SliderInput anchorYInput;
    private SliderInput strokeInput;
    private SliderInput shizukuSensitivityXInput;
    private SliderInput shizukuSensitivityYInput;
    private SliderInput shizukuFrameInput;
    private SliderInput shizukuMinDeltaInput;
    private SliderInput shizukuCurveInput;
    private SliderInput shizukuSmoothingInput;
    private SliderInput rightStickSensitivityInput;
    private SliderInput rightStickDeadzoneInput;
    private SliderInput rightStickCurveInput;
    private SliderInput rightStickMaxInput;
    private SliderInput rightStickRadiusInput;
    private SliderInput rightStickRecenterInput;
    private String rightStickCenterMode;
    private SliderInput relativeMouseSensitivityInput;
    private SliderInput relativeMouseMaxOutputInput;
    private SliderInput relativeMousePulseInput;
    private CheckBox relativeMouseInvertYInput;
    private String relativeMouseAcceleration;
    private SliderInput virtualMouseSensitivityInput;
    private SliderInput virtualMouseScrollInput;
    private CheckBox virtualMouseInvertYInput;
    private CheckBox virtualMouseFullGestureAreaInput;

    TouchpadSettingsController(AssistantActivity activity, Host host) {
        this.activity = activity;
        this.host = host;
    }

    void populate(LinearLayout content) {
        clearInputs();
        TouchpadSettings draft = host.draft();
        String mode = TouchpadSettings.normalizeMode(draft.mode);
        host.setMode(mode);

        LinearLayout row = actionRow(content);
        row.addView(modeButton(activity.getString(R.string.touch_mode_compatible),
                TouchpadSettings.MODE_TOUCH_DRAG));
        row.addView(modeButton(activity.getString(R.string.touch_mode_precision_aim),
                TouchpadSettings.MODE_RELATIVE_MOUSE));
        row = actionRow(content);
        row.addView(modeButton(activity.getString(R.string.touch_mode_virtual_right_stick),
                TouchpadSettings.MODE_RIGHT_STICK));
        row.addView(modeButton(activity.getString(R.string.touch_mode_enhanced),
                TouchpadSettings.MODE_SHIZUKU_TOUCH));
        row = actionRow(content);
        row.addView(modeButton(activity.getString(R.string.touch_mode_virtual_mouse),
                TouchpadSettings.MODE_VIRTUAL_MOUSE));

        if (!host.relativeMouseBackendAvailable()) {
            addHelp(content, activity.getString(R.string.touch_precision_requires_controller));
        }
        if (TouchpadSettings.MODE_VIRTUAL_MOUSE.equals(mode)) {
            populateVirtualMouse(content, draft);
        } else if (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(mode)) {
            populateRelativeMouse(content, draft);
        } else if (TouchpadSettings.MODE_RIGHT_STICK.equals(mode)) {
            populateRightStick(content, draft);
        } else if (TouchpadSettings.MODE_SHIZUKU_TOUCH.equals(mode)) {
            populateEnhancedTouch(content, draft);
        } else if (TouchpadSettings.MODE_TOUCH_DRAG.equals(mode)) {
            populateCompatibleTouch(content, draft);
        } else {
            addHelp(content, activity.getString(R.string.touch_no_extra_tuning,
                    localizedModeLabel(activity, mode)));
        }
    }

    private void populateVirtualMouse(LinearLayout content, TouchpadSettings draft) {
        virtualMouseSensitivityInput = slider(content,
                activity.getString(R.string.touch_virtual_mouse_sensitivity),
                0.2f, 4f, draft.virtualMouseSensitivity, 10, "x");
        virtualMouseScrollInput = slider(content,
                activity.getString(R.string.touch_virtual_mouse_scroll),
                16f, 96f, draft.virtualMouseScrollDistance, 1, "px");
        virtualMouseInvertYInput = checkBox(content,
                R.string.touch_invert_vertical, draft.virtualMouseInvertY);
        virtualMouseFullGestureAreaInput = checkBox(content,
                R.string.touch_virtual_mouse_full_gesture_area,
                draft.virtualMouseFullGestureArea);
        addHelp(content, activity.getString(R.string.touch_virtual_mouse_full_gesture_area_help));
        addHelp(content, activity.getString(R.string.touch_virtual_mouse_help));
    }

    private void populateRelativeMouse(LinearLayout content, TouchpadSettings draft) {
        relativeMouseSensitivityInput = slider(content,
                activity.getString(R.string.touch_aim_sensitivity),
                0.2f, 4f, draft.relativeMouseSensitivity, 10, "x");
        relativeMouseMaxOutputInput = slider(content,
                activity.getString(R.string.touch_max_output),
                10f, 100f, Math.round(draft.relativeMouseMaxOutputPercent * 100f), 1, "%");
        addLabel(content, activity.getString(R.string.touch_response_curve));
        relativeMouseAcceleration = TouchpadSettings.normalizeRelativeMouseAcceleration(
                draft.relativeMouseAcceleration);
        LinearLayout row = actionRow(content);
        row.addView(accelerationButton(activity.getString(R.string.touch_curve_off),
                TouchpadSettings.RELATIVE_MOUSE_ACCELERATION_OFF));
        row.addView(accelerationButton(activity.getString(R.string.touch_curve_gentle),
                TouchpadSettings.RELATIVE_MOUSE_ACCELERATION_LOW));
        row.addView(accelerationButton(activity.getString(R.string.touch_curve_balanced),
                TouchpadSettings.RELATIVE_MOUSE_ACCELERATION_MEDIUM));
        row.addView(accelerationButton(activity.getString(R.string.touch_curve_fast),
                TouchpadSettings.RELATIVE_MOUSE_ACCELERATION_HIGH));
        relativeMouseInvertYInput = checkBox(content,
                R.string.touch_invert_vertical, draft.relativeMouseInvertY);
        addAdvancedToggle(content);
        if (host.advancedVisible()) {
            relativeMousePulseInput = slider(content,
                    activity.getString(R.string.touch_response_duration),
                    8f, 24f, draft.relativeMousePulseDurationMs, 1, "ms");
        }
        addHelp(content, activity.getString(R.string.touch_precision_help));
    }

    private void populateRightStick(LinearLayout content, TouchpadSettings draft) {
        rightStickCenterMode = TouchpadSettings.normalizeRightStickCenterMode(
                draft.rightStickCenterMode);
        addLabel(content, activity.getString(R.string.touch_stick_center));
        LinearLayout row = actionRow(content);
        row.addView(centerModeButton(activity.getString(R.string.touch_center_dynamic),
                TouchpadSettings.RIGHT_STICK_CENTER_FLOAT));
        row.addView(centerModeButton(activity.getString(R.string.touch_center_fixed),
                TouchpadSettings.RIGHT_STICK_CENTER_STATIC));
        rightStickSensitivityInput = slider(content,
                activity.getString(R.string.touch_sensitivity),
                0.2f, 4f, draft.rightStickSensitivity, 10, "x");
        rightStickDeadzoneInput = slider(content,
                activity.getString(R.string.touch_deadzone),
                0f, 40f, Math.round(draft.rightStickDeadzone * 100f), 1, "%");
        rightStickCurveInput = slider(content,
                activity.getString(R.string.touch_feel_curve),
                0.5f, 3f, draft.rightStickCurve, 10, "");
        rightStickMaxInput = slider(content,
                activity.getString(R.string.touch_max_output),
                10f, 100f, Math.round(draft.rightStickMaxOutput * 100f), 1, "%");
        addAdvancedToggle(content);
        if (host.advancedVisible()) {
            rightStickRadiusInput = slider(content,
                    activity.getString(R.string.touch_operation_radius),
                    18f, 80f, Math.round(draft.rightStickRadius * 100f), 1, "%");
            rightStickRecenterInput = slider(content,
                    activity.getString(R.string.touch_recenter_strength),
                    1f, 8f, draft.rightStickRecenterBursts, 1, "");
            intervalInput = slider(content,
                    activity.getString(R.string.touch_response_interval),
                    0f, 80f, draft.intervalMs, 1, "ms");
        }
        addHelp(content, activity.getString(R.string.touch_right_stick_help));
    }

    private void populateEnhancedTouch(LinearLayout content, TouchpadSettings draft) {
        shizukuSensitivityXInput = slider(content,
                activity.getString(R.string.touch_horizontal_sensitivity),
                0.2f, 12f, draft.shizukuTouchSensitivityX, 10, "x");
        shizukuSensitivityYInput = slider(content,
                activity.getString(R.string.touch_vertical_sensitivity),
                0.2f, 12f, draft.shizukuTouchSensitivityY, 10, "x");
        shizukuSmoothingInput = slider(content,
                activity.getString(R.string.touch_smoothing),
                0f, 90f, Math.round(draft.shizukuTouchSmoothing * 100f), 1, "%");
        addAdvancedToggle(content);
        if (host.advancedVisible()) {
            shizukuFrameInput = slider(content,
                    activity.getString(R.string.touch_response_interval),
                    4f, 33f, draft.shizukuTouchFrameMs, 1, "ms");
            shizukuMinDeltaInput = slider(content,
                    activity.getString(R.string.touch_minimum_movement),
                    0f, 20f, draft.shizukuTouchMinDelta, 10, "px");
            shizukuCurveInput = slider(content,
                    activity.getString(R.string.touch_feel_curve),
                    0.5f, 3f, draft.shizukuTouchCurve, 10, "");
            anchorXInput = slider(content, activity.getString(R.string.touch_start_x),
                    5f, 95f, Math.round(draft.anchorX * 100f), 1, "%");
            anchorYInput = slider(content, activity.getString(R.string.touch_start_y),
                    5f, 95f, Math.round(draft.anchorY * 100f), 1, "%");
        }
        addHelp(content, activity.getString(R.string.touch_enhanced_help));
    }

    private void populateCompatibleTouch(LinearLayout content, TouchpadSettings draft) {
        sensitivityInput = slider(content, activity.getString(R.string.touch_sensitivity),
                0.2f, 12f, draft.sensitivity, 10, "x");
        addAdvancedToggle(content);
        if (host.advancedVisible()) {
            intervalInput = slider(content, activity.getString(R.string.touch_response_interval),
                    0f, 160f, draft.intervalMs, 1, "ms");
            minDeltaInput = slider(content, activity.getString(R.string.touch_minimum_movement),
                    0f, 20f, draft.minDelta, 10, "px");
            anchorXInput = slider(content, activity.getString(R.string.touch_start_x),
                    5f, 95f, Math.round(draft.anchorX * 100f), 1, "%");
            anchorYInput = slider(content, activity.getString(R.string.touch_start_y),
                    5f, 95f, Math.round(draft.anchorY * 100f), 1, "%");
            strokeInput = slider(content, activity.getString(R.string.touch_press_duration),
                    1f, 80f, draft.strokeMs, 1, "ms");
        }
        addHelp(content, activity.getString(R.string.touch_compatible_help));
    }

    void selectMode(String mode) {
        if (!host.modeAvailable(mode)) {
            int message = TouchpadSettings.MODE_RELATIVE_MOUSE.equals(mode)
                    ? R.string.action_controller_enhancement_required
                    : (TouchpadSettings.MODE_VIRTUAL_MOUSE.equals(mode)
                            ? R.string.virtual_mouse_unavailable
                            : R.string.action_connection_setup_required);
            host.showError(activity.getString(message));
            return;
        }
        applyInputs();
        host.draft().mode = mode;
        host.setMode(TouchpadSettings.normalizeMode(mode));
        host.refresh();
    }

    void applyInputs() {
        TouchpadSettings draft = host.draft();
        if (host.mode() != null) draft.mode = host.mode();
        if (rightStickCenterMode != null) draft.rightStickCenterMode = rightStickCenterMode;
        if (sensitivityInput != null) draft.sensitivity = sensitivityInput.floatValue();
        if (intervalInput != null) draft.intervalMs = intervalInput.intValue();
        if (minDeltaInput != null) draft.minDelta = minDeltaInput.floatValue();
        if (anchorXInput != null) draft.anchorX = anchorXInput.floatValue() / 100f;
        if (anchorYInput != null) draft.anchorY = anchorYInput.floatValue() / 100f;
        if (strokeInput != null) draft.strokeMs = strokeInput.intValue();
        if (shizukuSensitivityXInput != null) draft.shizukuTouchSensitivityX = shizukuSensitivityXInput.floatValue();
        if (shizukuSensitivityYInput != null) draft.shizukuTouchSensitivityY = shizukuSensitivityYInput.floatValue();
        if (shizukuFrameInput != null) draft.shizukuTouchFrameMs = shizukuFrameInput.intValue();
        if (shizukuMinDeltaInput != null) draft.shizukuTouchMinDelta = shizukuMinDeltaInput.floatValue();
        if (shizukuCurveInput != null) draft.shizukuTouchCurve = shizukuCurveInput.floatValue();
        if (shizukuSmoothingInput != null) draft.shizukuTouchSmoothing = shizukuSmoothingInput.floatValue() / 100f;
        if (rightStickSensitivityInput != null) draft.rightStickSensitivity = rightStickSensitivityInput.floatValue();
        if (rightStickDeadzoneInput != null) draft.rightStickDeadzone = rightStickDeadzoneInput.floatValue() / 100f;
        if (rightStickCurveInput != null) draft.rightStickCurve = rightStickCurveInput.floatValue();
        if (rightStickMaxInput != null) draft.rightStickMaxOutput = rightStickMaxInput.floatValue() / 100f;
        if (rightStickRadiusInput != null) draft.rightStickRadius = rightStickRadiusInput.floatValue() / 100f;
        if (rightStickRecenterInput != null) draft.rightStickRecenterBursts = rightStickRecenterInput.intValue();
        if (relativeMouseSensitivityInput != null) draft.relativeMouseSensitivity = relativeMouseSensitivityInput.floatValue();
        if (relativeMouseMaxOutputInput != null) draft.relativeMouseMaxOutputPercent = relativeMouseMaxOutputInput.floatValue() / 100f;
        if (relativeMousePulseInput != null) draft.relativeMousePulseDurationMs = relativeMousePulseInput.intValue();
        if (relativeMouseInvertYInput != null) draft.relativeMouseInvertY = relativeMouseInvertYInput.isChecked();
        if (relativeMouseAcceleration != null) draft.relativeMouseAcceleration = TouchpadSettings.normalizeRelativeMouseAcceleration(relativeMouseAcceleration);
        if (virtualMouseSensitivityInput != null) draft.virtualMouseSensitivity = virtualMouseSensitivityInput.floatValue();
        if (virtualMouseScrollInput != null) draft.virtualMouseScrollDistance = virtualMouseScrollInput.floatValue();
        if (virtualMouseInvertYInput != null) draft.virtualMouseInvertY = virtualMouseInvertYInput.isChecked();
        if (virtualMouseFullGestureAreaInput != null) draft.virtualMouseFullGestureArea = virtualMouseFullGestureAreaInput.isChecked();
    }

    void clearInputs() {
        sensitivityInput = intervalInput = minDeltaInput = anchorXInput = anchorYInput = strokeInput = null;
        shizukuSensitivityXInput = shizukuSensitivityYInput = shizukuFrameInput = null;
        shizukuMinDeltaInput = shizukuCurveInput = shizukuSmoothingInput = null;
        rightStickSensitivityInput = rightStickDeadzoneInput = rightStickCurveInput = null;
        rightStickMaxInput = rightStickRadiusInput = rightStickRecenterInput = null;
        rightStickCenterMode = null;
        relativeMouseSensitivityInput = relativeMouseMaxOutputInput = relativeMousePulseInput = null;
        relativeMouseInvertYInput = null;
        relativeMouseAcceleration = null;
        virtualMouseSensitivityInput = virtualMouseScrollInput = null;
        virtualMouseInvertYInput = virtualMouseFullGestureAreaInput = null;
    }

    static String localizedModeLabel(AssistantActivity activity, String mode) {
        String normalized = TouchpadSettings.normalizeMode(mode);
        if (TouchpadSettings.MODE_RELATIVE_MOVE.equals(normalized)) return activity.getString(R.string.touch_mode_relative_move);
        if (TouchpadSettings.MODE_MOUSE_POINTER.equals(normalized)) return activity.getString(R.string.touch_mode_mouse_pointer);
        if (TouchpadSettings.MODE_VIRTUAL_MOUSE.equals(normalized)) return activity.getString(R.string.touch_mode_virtual_mouse);
        if (TouchpadSettings.MODE_RIGHT_STICK.equals(normalized)) return activity.getString(R.string.touch_mode_virtual_right_stick);
        if (TouchpadSettings.MODE_SHIZUKU_TOUCH.equals(normalized)) return activity.getString(R.string.touch_mode_enhanced);
        if (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(normalized)) return activity.getString(R.string.touch_mode_precision_aim);
        return activity.getString(R.string.touch_mode_compatible);
    }

    private Button modeButton(String label, String mode) {
        Button button = editorButton(label, () -> selectMode(mode));
        HeimdallUi.applyChoiceButton(activity, button, mode.equals(host.mode()));
        if (TouchpadSettings.MODE_RELATIVE_MOUSE.equals(mode)
                && !host.relativeMouseBackendAvailable()) {
            disable(button, R.string.touch_precision_requires_controller_description);
        }
        if (TouchpadSettings.MODE_VIRTUAL_MOUSE.equals(mode)
                && !ShizukuNativeController.isReady()) {
            disable(button, R.string.virtual_mouse_unavailable);
        }
        return button;
    }

    private Button centerModeButton(String label, String mode) {
        Button button = editorButton(label, () -> {
            applyInputs();
            rightStickCenterMode = TouchpadSettings.normalizeRightStickCenterMode(mode);
            host.draft().rightStickCenterMode = rightStickCenterMode;
            host.refresh();
        });
        HeimdallUi.applyChoiceButton(activity, button, mode.equals(rightStickCenterMode));
        return button;
    }

    private Button accelerationButton(String label, String acceleration) {
        Button button = editorButton(label, () -> {
            applyInputs();
            relativeMouseAcceleration = TouchpadSettings.normalizeRelativeMouseAcceleration(acceleration);
            host.draft().relativeMouseAcceleration = relativeMouseAcceleration;
            host.refresh();
        });
        button.setTextSize(HeimdallUi.TYPE_BUTTON_COMPACT);
        button.setPadding(dp(HeimdallUi.SPACE_1), 0, dp(HeimdallUi.SPACE_1), 0);
        HeimdallUi.applyChoiceButton(activity, button,
                acceleration.equals(relativeMouseAcceleration));
        return button;
    }

    private void addAdvancedToggle(LinearLayout content) {
        LinearLayout row = actionRow(content);
        row.addView(editorButton(host.advancedVisible()
                ? activity.getString(R.string.touch_collapse_advanced_tuning)
                : activity.getString(R.string.touch_advanced_tuning), () -> {
            applyInputs();
            host.setAdvancedVisible(!host.advancedVisible());
            host.refresh();
        }));
    }

    private LinearLayout actionRow(LinearLayout content) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
        return row;
    }

    private void addLabel(LinearLayout content, String label) {
        TextView title = HeimdallUi.text(activity, label, 12, HeimdallUi.COLOR_TEXT_MUTED, true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(24));
        params.setMargins(0, dp(2), 0, 0);
        content.addView(title, params);
    }

    private void addHelp(LinearLayout content, String message) {
        TextView help = HeimdallUi.text(activity, message, 11, HeimdallUi.COLOR_TEXT_MUTED, false);
        help.setGravity(Gravity.TOP | Gravity.LEFT);
        help.setMinHeight(dp(34));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(4), 0, dp(8));
        content.addView(help, params);
    }

    private CheckBox checkBox(LinearLayout content, int labelRes, boolean checked) {
        CheckBox input = new CheckBox(activity);
        input.setText(labelRes);
        input.setTextSize(12);
        input.setTextColor(HeimdallUi.textColor(activity));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            input.setButtonTintList(new ColorStateList(
                    new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                    new int[]{HeimdallUi.accent(activity),
                            HeimdallUi.isPearl(activity) ? 0xFF788693 : 0xFF7F91A6}));
        }
        input.setChecked(checked);
        content.addView(input, new LinearLayout.LayoutParams(-1, dp(42)));
        return input;
    }

    private Button editorButton(String label, Runnable action) {
        Button button = HeimdallUi.baseButton(activity, label, action);
        HeimdallUi.applySecondaryButton(activity, button);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), dp(4), dp(3), dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private SliderInput slider(LinearLayout parent, String label,
            float min, float max, float value, int scale, String suffix) {
        SliderInput input = new SliderInput(min, max, value, scale, suffix);
        LinearLayout labelRow = new LinearLayout(activity);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(labelRow, new LinearLayout.LayoutParams(-1, dp(28)));
        TextView title = HeimdallUi.text(activity, label, 13, HeimdallUi.COLOR_TEXT, true);
        labelRow.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
        input.valueLabel = HeimdallUi.text(activity, "", 13, HeimdallUi.COLOR_TEXT_MUTED, true);
        input.valueLabel.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        labelRow.addView(input.valueLabel, new LinearLayout.LayoutParams(dp(88), -1));
        input.seekBar = new SeekBar(activity);
        input.seekBar.setMax(input.maxUnits - input.minUnits);
        input.seekBar.setProgress(input.currentUnits - input.minUnits);
        styleSeekBar(input.seekBar);
        input.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                input.currentUnits = input.minUnits + progress;
                input.updateLabel();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        input.updateLabel();
        parent.addView(input.seekBar, new LinearLayout.LayoutParams(-1, dp(42)));
        return input;
    }

    private void styleSeekBar(SeekBar seekBar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        int active = HeimdallUi.isPearl(activity) ? 0xFFF08A2A : HeimdallUi.accent(activity);
        int track = HeimdallUi.isPearl(activity) ? 0xFF909AA2 : 0xFF445A72;
        seekBar.setProgressTintList(ColorStateList.valueOf(active));
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(track));
        seekBar.setThumbTintList(ColorStateList.valueOf(active));
        seekBar.setSplitTrack(false);
    }

    private void disable(Button button, int descriptionRes) {
        button.setEnabled(false);
        button.setAlpha(0.45f);
        button.setContentDescription(activity.getString(descriptionRes));
    }

    private int dp(int value) {
        return HeimdallUi.dp(activity, value);
    }

    private static final class SliderInput {
        final int minUnits;
        final int maxUnits;
        final int scale;
        final String suffix;
        int currentUnits;
        SeekBar seekBar;
        TextView valueLabel;

        SliderInput(float min, float max, float value, int scale, String suffix) {
            this.scale = scale;
            this.suffix = suffix == null ? "" : suffix;
            minUnits = Math.round(min * scale);
            maxUnits = Math.round(max * scale);
            currentUnits = Math.max(minUnits, Math.min(maxUnits, Math.round(value * scale)));
        }

        int intValue() { return Math.round(floatValue()); }
        float floatValue() { return currentUnits / (float) scale; }

        void updateLabel() {
            if (valueLabel == null) return;
            String value = scale == 1 ? String.valueOf(currentUnits)
                    : String.format(Locale.US, "%.1f", floatValue());
            valueLabel.setText(value + suffix);
        }
    }
}
