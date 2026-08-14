package com.mastercook777.heimdall;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public final class TouchpadSettings {
    public static final String MODE_TOUCH_DRAG = "touch_drag";
    public static final String MODE_SHIZUKU_TOUCH = "shizuku_touch";
    public static final String MODE_RELATIVE_MOVE = "relative_move";
    public static final String MODE_MOUSE_POINTER = "mouse_pointer";
    public static final String MODE_VIRTUAL_MOUSE = "virtual_mouse";
    public static final String MODE_RIGHT_STICK = "right_stick";
    public static final String MODE_RELATIVE_MOUSE = "relative_mouse_experimental";
    @Deprecated
    public static final String MODE_RELATIVE_MOUSE_EXPERIMENTAL = MODE_RELATIVE_MOUSE;
    public static final String RIGHT_STICK_CENTER_FLOAT = "float";
    public static final String RIGHT_STICK_CENTER_STATIC = "static";
    public static final String RELATIVE_MOUSE_ACCELERATION_OFF = "off";
    public static final String RELATIVE_MOUSE_ACCELERATION_LOW = "low";
    public static final String RELATIVE_MOUSE_ACCELERATION_MEDIUM = "medium";
    public static final String RELATIVE_MOUSE_ACCELERATION_HIGH = "high";

    private static final String PREFS = "thor_assistant_touchpad";
    private static final String KEY_MODE = "mode";
    private static final String KEY_LEFT_WEIGHT = "left_weight";
    private static final String KEY_TOUCH_WEIGHT = "touch_weight";
    private static final String KEY_TOP_WEIGHT = "top_weight";
    private static final String KEY_MACRO_WEIGHT = "macro_weight";
    private static final String KEY_SENSITIVITY = "sensitivity";
    private static final String KEY_INTERVAL_MS = "interval_ms";
    private static final String KEY_MIN_DELTA = "min_delta";
    private static final String KEY_ANCHOR_X = "anchor_x";
    private static final String KEY_ANCHOR_Y = "anchor_y";
    private static final String KEY_STROKE_MS = "stroke_ms";
    private static final String KEY_SHIZUKU_TOUCH_SENSITIVITY_X = "shizuku_touch_sensitivity_x";
    private static final String KEY_SHIZUKU_TOUCH_SENSITIVITY_Y = "shizuku_touch_sensitivity_y";
    private static final String KEY_SHIZUKU_TOUCH_FRAME_MS = "shizuku_touch_frame_ms";
    private static final String KEY_SHIZUKU_TOUCH_MIN_DELTA = "shizuku_touch_min_delta";
    private static final String KEY_SHIZUKU_TOUCH_CURVE = "shizuku_touch_curve";
    private static final String KEY_SHIZUKU_TOUCH_SMOOTHING = "shizuku_touch_smoothing";
    private static final String KEY_RIGHT_STICK_SENSITIVITY = "right_stick_sensitivity";
    private static final String KEY_RIGHT_STICK_DEADZONE = "right_stick_deadzone";
    private static final String KEY_RIGHT_STICK_CURVE = "right_stick_curve";
    private static final String KEY_RIGHT_STICK_MAX_OUTPUT = "right_stick_max_output";
    private static final String KEY_RIGHT_STICK_RADIUS = "right_stick_radius";
    private static final String KEY_RIGHT_STICK_RECENTER = "right_stick_recenter";
    private static final String KEY_RIGHT_STICK_CENTER_MODE = "right_stick_center_mode";
    private static final String KEY_RELATIVE_MOUSE_SENSITIVITY = "relative_mouse_sensitivity";
    private static final String KEY_RELATIVE_MOUSE_MAX_OUTPUT = "relative_mouse_max_output";
    private static final String KEY_RELATIVE_MOUSE_INVERT_Y = "relative_mouse_invert_y";
    private static final String KEY_RELATIVE_MOUSE_ACCELERATION = "relative_mouse_acceleration";
    private static final String KEY_RELATIVE_MOUSE_PULSE_MS = "relative_mouse_pulse_ms";
    private static final String KEY_VIRTUAL_MOUSE_SENSITIVITY = "virtual_mouse_sensitivity";
    private static final String KEY_VIRTUAL_MOUSE_INVERT_Y = "virtual_mouse_invert_y";
    private static final String KEY_VIRTUAL_MOUSE_SCROLL_DISTANCE = "virtual_mouse_scroll_distance";
    private static final String KEY_VIRTUAL_MOUSE_FULL_GESTURE_AREA =
            "virtual_mouse_full_gesture_area";

    public String mode = MODE_TOUCH_DRAG;
    public int leftWeight = 5;
    public int touchWeight = 6;
    public int topWeight = 4;
    public int macroWeight = 5;
    public float sensitivity = 2.8f;
    public int intervalMs = 8;
    public float minDelta = 0f;
    public float anchorX = 0.78f;
    public float anchorY = 0.50f;
    public int strokeMs = 16;
    public float shizukuTouchSensitivityX = 2.8f;
    public float shizukuTouchSensitivityY = 2.8f;
    public int shizukuTouchFrameMs = 8;
    public float shizukuTouchMinDelta = 0f;
    public float shizukuTouchCurve = 1.15f;
    public float shizukuTouchSmoothing = 0.12f;
    public float rightStickSensitivity = 1.0f;
    public float rightStickDeadzone = 0.08f;
    public float rightStickCurve = 1.45f;
    public float rightStickMaxOutput = 0.85f;
    public float rightStickRadius = 0.42f;
    public int rightStickRecenterBursts = 4;
    public String rightStickCenterMode = RIGHT_STICK_CENTER_FLOAT;
    public float relativeMouseSensitivity = 1.0f;
    public float relativeMouseMaxOutputPercent = 0.70f;
    public boolean relativeMouseInvertY;
    public String relativeMouseAcceleration = RELATIVE_MOUSE_ACCELERATION_OFF;
    public int relativeMousePulseDurationMs = 10;
    public float virtualMouseSensitivity = 1.0f;
    public boolean virtualMouseInvertY;
    public float virtualMouseScrollDistance = 36f;
    public boolean virtualMouseFullGestureArea;

    public TouchpadSettings copy() {
        TouchpadSettings settings = new TouchpadSettings();
        settings.copyFrom(this);
        return settings;
    }

    public void copyFrom(TouchpadSettings source) {
        if (source == null) {
            return;
        }
        mode = normalizeMode(source.mode);
        leftWeight = clampInt(source.leftWeight, 2, 10);
        touchWeight = clampInt(source.touchWeight, 2, 12);
        topWeight = clampInt(source.topWeight, 2, 8);
        macroWeight = clampInt(source.macroWeight, 3, 10);
        sensitivity = clampFloat(source.sensitivity, 0.2f, 12f);
        intervalMs = clampInt(source.intervalMs, 0, 160);
        minDelta = clampFloat(source.minDelta, 0f, 20f);
        anchorX = clampFloat(source.anchorX, 0.05f, 0.95f);
        anchorY = clampFloat(source.anchorY, 0.05f, 0.95f);
        strokeMs = clampInt(source.strokeMs, 1, 80);
        shizukuTouchSensitivityX = clampFloat(source.shizukuTouchSensitivityX, 0.2f, 12f);
        shizukuTouchSensitivityY = clampFloat(source.shizukuTouchSensitivityY, 0.2f, 12f);
        shizukuTouchFrameMs = clampInt(source.shizukuTouchFrameMs, 4, 33);
        shizukuTouchMinDelta = clampFloat(source.shizukuTouchMinDelta, 0f, 20f);
        shizukuTouchCurve = clampFloat(source.shizukuTouchCurve, 0.5f, 3f);
        shizukuTouchSmoothing = clampFloat(source.shizukuTouchSmoothing, 0f, 0.9f);
        rightStickSensitivity = clampFloat(source.rightStickSensitivity, 0.2f, 4f);
        rightStickDeadzone = clampFloat(source.rightStickDeadzone, 0f, 0.4f);
        rightStickCurve = clampFloat(source.rightStickCurve, 0.5f, 3f);
        rightStickMaxOutput = clampFloat(source.rightStickMaxOutput, 0.1f, 1f);
        rightStickRadius = clampFloat(source.rightStickRadius, 0.18f, 0.8f);
        rightStickRecenterBursts = clampInt(source.rightStickRecenterBursts, 1, 8);
        rightStickCenterMode = normalizeRightStickCenterMode(source.rightStickCenterMode);
        relativeMouseSensitivity = clampFloat(source.relativeMouseSensitivity, 0.2f, 4f);
        relativeMouseMaxOutputPercent = clampFloat(source.relativeMouseMaxOutputPercent, 0.1f, 1f);
        relativeMouseInvertY = source.relativeMouseInvertY;
        relativeMouseAcceleration = normalizeRelativeMouseAcceleration(source.relativeMouseAcceleration);
        relativeMousePulseDurationMs = clampInt(source.relativeMousePulseDurationMs, 8, 30);
        virtualMouseSensitivity = clampFloat(source.virtualMouseSensitivity, 0.2f, 4f);
        virtualMouseInvertY = source.virtualMouseInvertY;
        virtualMouseScrollDistance = clampFloat(source.virtualMouseScrollDistance, 16f, 96f);
        virtualMouseFullGestureArea = source.virtualMouseFullGestureArea;
    }

    public static TouchpadSettings load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        TouchpadSettings settings = new TouchpadSettings();
        settings.mode = normalizeMode(prefs.getString(KEY_MODE, settings.mode));
        settings.leftWeight = clampInt(prefs.getInt(KEY_LEFT_WEIGHT, settings.leftWeight), 2, 10);
        settings.touchWeight = clampInt(prefs.getInt(KEY_TOUCH_WEIGHT, settings.touchWeight), 2, 12);
        settings.topWeight = clampInt(prefs.getInt(KEY_TOP_WEIGHT, settings.topWeight), 2, 8);
        settings.macroWeight = clampInt(prefs.getInt(KEY_MACRO_WEIGHT, settings.macroWeight), 3, 10);
        settings.sensitivity = clampFloat(prefs.getFloat(KEY_SENSITIVITY, settings.sensitivity), 0.2f, 12f);
        settings.intervalMs = clampInt(prefs.getInt(KEY_INTERVAL_MS, settings.intervalMs), 0, 160);
        settings.minDelta = clampFloat(prefs.getFloat(KEY_MIN_DELTA, settings.minDelta), 0f, 20f);
        settings.anchorX = clampFloat(prefs.getFloat(KEY_ANCHOR_X, settings.anchorX), 0.05f, 0.95f);
        settings.anchorY = clampFloat(prefs.getFloat(KEY_ANCHOR_Y, settings.anchorY), 0.05f, 0.95f);
        settings.strokeMs = clampInt(prefs.getInt(KEY_STROKE_MS, settings.strokeMs), 1, 80);
        settings.shizukuTouchSensitivityX = clampFloat(prefs.getFloat(KEY_SHIZUKU_TOUCH_SENSITIVITY_X, settings.shizukuTouchSensitivityX), 0.2f, 12f);
        settings.shizukuTouchSensitivityY = clampFloat(prefs.getFloat(KEY_SHIZUKU_TOUCH_SENSITIVITY_Y, settings.shizukuTouchSensitivityY), 0.2f, 12f);
        settings.shizukuTouchFrameMs = clampInt(prefs.getInt(KEY_SHIZUKU_TOUCH_FRAME_MS, settings.shizukuTouchFrameMs), 4, 33);
        settings.shizukuTouchMinDelta = clampFloat(prefs.getFloat(KEY_SHIZUKU_TOUCH_MIN_DELTA, settings.shizukuTouchMinDelta), 0f, 20f);
        settings.shizukuTouchCurve = clampFloat(prefs.getFloat(KEY_SHIZUKU_TOUCH_CURVE, settings.shizukuTouchCurve), 0.5f, 3f);
        settings.shizukuTouchSmoothing = clampFloat(prefs.getFloat(KEY_SHIZUKU_TOUCH_SMOOTHING, settings.shizukuTouchSmoothing), 0f, 0.9f);
        settings.rightStickSensitivity = clampFloat(prefs.getFloat(KEY_RIGHT_STICK_SENSITIVITY, settings.rightStickSensitivity), 0.2f, 4f);
        settings.rightStickDeadzone = clampFloat(prefs.getFloat(KEY_RIGHT_STICK_DEADZONE, settings.rightStickDeadzone), 0f, 0.4f);
        settings.rightStickCurve = clampFloat(prefs.getFloat(KEY_RIGHT_STICK_CURVE, settings.rightStickCurve), 0.5f, 3f);
        settings.rightStickMaxOutput = clampFloat(prefs.getFloat(KEY_RIGHT_STICK_MAX_OUTPUT, settings.rightStickMaxOutput), 0.1f, 1f);
        settings.rightStickRadius = clampFloat(prefs.getFloat(KEY_RIGHT_STICK_RADIUS, settings.rightStickRadius), 0.18f, 0.8f);
        settings.rightStickRecenterBursts = clampInt(prefs.getInt(KEY_RIGHT_STICK_RECENTER, settings.rightStickRecenterBursts), 1, 8);
        settings.rightStickCenterMode = normalizeRightStickCenterMode(prefs.getString(KEY_RIGHT_STICK_CENTER_MODE, settings.rightStickCenterMode));
        settings.relativeMouseSensitivity = clampFloat(prefs.getFloat(KEY_RELATIVE_MOUSE_SENSITIVITY, settings.relativeMouseSensitivity), 0.2f, 4f);
        settings.relativeMouseMaxOutputPercent = clampFloat(prefs.getFloat(KEY_RELATIVE_MOUSE_MAX_OUTPUT, settings.relativeMouseMaxOutputPercent), 0.1f, 1f);
        settings.relativeMouseInvertY = prefs.getBoolean(KEY_RELATIVE_MOUSE_INVERT_Y, settings.relativeMouseInvertY);
        settings.relativeMouseAcceleration = normalizeRelativeMouseAcceleration(
                prefs.getString(KEY_RELATIVE_MOUSE_ACCELERATION, settings.relativeMouseAcceleration));
        settings.relativeMousePulseDurationMs = clampInt(
                prefs.getInt(KEY_RELATIVE_MOUSE_PULSE_MS, settings.relativeMousePulseDurationMs), 8, 30);
        settings.virtualMouseSensitivity = clampFloat(
                prefs.getFloat(KEY_VIRTUAL_MOUSE_SENSITIVITY, settings.virtualMouseSensitivity), 0.2f, 4f);
        settings.virtualMouseInvertY = prefs.getBoolean(
                KEY_VIRTUAL_MOUSE_INVERT_Y, settings.virtualMouseInvertY);
        settings.virtualMouseScrollDistance = clampFloat(
                prefs.getFloat(KEY_VIRTUAL_MOUSE_SCROLL_DISTANCE, settings.virtualMouseScrollDistance), 16f, 96f);
        settings.virtualMouseFullGestureArea = prefs.getBoolean(
                KEY_VIRTUAL_MOUSE_FULL_GESTURE_AREA, settings.virtualMouseFullGestureArea);
        return settings;
    }

    public void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_MODE, normalizeMode(mode))
                .putInt(KEY_LEFT_WEIGHT, leftWeight)
                .putInt(KEY_TOUCH_WEIGHT, touchWeight)
                .putInt(KEY_TOP_WEIGHT, topWeight)
                .putInt(KEY_MACRO_WEIGHT, macroWeight)
                .putFloat(KEY_SENSITIVITY, sensitivity)
                .putInt(KEY_INTERVAL_MS, intervalMs)
                .putFloat(KEY_MIN_DELTA, minDelta)
                .putFloat(KEY_ANCHOR_X, anchorX)
                .putFloat(KEY_ANCHOR_Y, anchorY)
                .putInt(KEY_STROKE_MS, strokeMs)
                .putFloat(KEY_SHIZUKU_TOUCH_SENSITIVITY_X, shizukuTouchSensitivityX)
                .putFloat(KEY_SHIZUKU_TOUCH_SENSITIVITY_Y, shizukuTouchSensitivityY)
                .putInt(KEY_SHIZUKU_TOUCH_FRAME_MS, shizukuTouchFrameMs)
                .putFloat(KEY_SHIZUKU_TOUCH_MIN_DELTA, shizukuTouchMinDelta)
                .putFloat(KEY_SHIZUKU_TOUCH_CURVE, shizukuTouchCurve)
                .putFloat(KEY_SHIZUKU_TOUCH_SMOOTHING, shizukuTouchSmoothing)
                .putFloat(KEY_RIGHT_STICK_SENSITIVITY, rightStickSensitivity)
                .putFloat(KEY_RIGHT_STICK_DEADZONE, rightStickDeadzone)
                .putFloat(KEY_RIGHT_STICK_CURVE, rightStickCurve)
                .putFloat(KEY_RIGHT_STICK_MAX_OUTPUT, rightStickMaxOutput)
                .putFloat(KEY_RIGHT_STICK_RADIUS, rightStickRadius)
                .putInt(KEY_RIGHT_STICK_RECENTER, rightStickRecenterBursts)
                .putString(KEY_RIGHT_STICK_CENTER_MODE, normalizeRightStickCenterMode(rightStickCenterMode))
                .putFloat(KEY_RELATIVE_MOUSE_SENSITIVITY, relativeMouseSensitivity)
                .putFloat(KEY_RELATIVE_MOUSE_MAX_OUTPUT, relativeMouseMaxOutputPercent)
                .putBoolean(KEY_RELATIVE_MOUSE_INVERT_Y, relativeMouseInvertY)
                .putString(KEY_RELATIVE_MOUSE_ACCELERATION,
                        normalizeRelativeMouseAcceleration(relativeMouseAcceleration))
                .putInt(KEY_RELATIVE_MOUSE_PULSE_MS, relativeMousePulseDurationMs)
                .putFloat(KEY_VIRTUAL_MOUSE_SENSITIVITY, virtualMouseSensitivity)
                .putBoolean(KEY_VIRTUAL_MOUSE_INVERT_Y, virtualMouseInvertY)
                .putFloat(KEY_VIRTUAL_MOUSE_SCROLL_DISTANCE, virtualMouseScrollDistance)
                .putBoolean(KEY_VIRTUAL_MOUSE_FULL_GESTURE_AREA, virtualMouseFullGestureArea)
                .apply();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(KEY_MODE, normalizeMode(mode));
        object.put(KEY_LEFT_WEIGHT, leftWeight);
        object.put(KEY_TOUCH_WEIGHT, touchWeight);
        object.put(KEY_TOP_WEIGHT, topWeight);
        object.put(KEY_MACRO_WEIGHT, macroWeight);
        object.put(KEY_SENSITIVITY, sensitivity);
        object.put(KEY_INTERVAL_MS, intervalMs);
        object.put(KEY_MIN_DELTA, minDelta);
        object.put(KEY_ANCHOR_X, anchorX);
        object.put(KEY_ANCHOR_Y, anchorY);
        object.put(KEY_STROKE_MS, strokeMs);
        object.put(KEY_SHIZUKU_TOUCH_SENSITIVITY_X, shizukuTouchSensitivityX);
        object.put(KEY_SHIZUKU_TOUCH_SENSITIVITY_Y, shizukuTouchSensitivityY);
        object.put(KEY_SHIZUKU_TOUCH_FRAME_MS, shizukuTouchFrameMs);
        object.put(KEY_SHIZUKU_TOUCH_MIN_DELTA, shizukuTouchMinDelta);
        object.put(KEY_SHIZUKU_TOUCH_CURVE, shizukuTouchCurve);
        object.put(KEY_SHIZUKU_TOUCH_SMOOTHING, shizukuTouchSmoothing);
        object.put(KEY_RIGHT_STICK_SENSITIVITY, rightStickSensitivity);
        object.put(KEY_RIGHT_STICK_DEADZONE, rightStickDeadzone);
        object.put(KEY_RIGHT_STICK_CURVE, rightStickCurve);
        object.put(KEY_RIGHT_STICK_MAX_OUTPUT, rightStickMaxOutput);
        object.put(KEY_RIGHT_STICK_RADIUS, rightStickRadius);
        object.put(KEY_RIGHT_STICK_RECENTER, rightStickRecenterBursts);
        object.put(KEY_RIGHT_STICK_CENTER_MODE, normalizeRightStickCenterMode(rightStickCenterMode));
        object.put(KEY_RELATIVE_MOUSE_SENSITIVITY, relativeMouseSensitivity);
        object.put(KEY_RELATIVE_MOUSE_MAX_OUTPUT, relativeMouseMaxOutputPercent);
        object.put(KEY_RELATIVE_MOUSE_INVERT_Y, relativeMouseInvertY);
        object.put(KEY_RELATIVE_MOUSE_ACCELERATION,
                normalizeRelativeMouseAcceleration(relativeMouseAcceleration));
        object.put(KEY_RELATIVE_MOUSE_PULSE_MS, relativeMousePulseDurationMs);
        object.put(KEY_VIRTUAL_MOUSE_SENSITIVITY, virtualMouseSensitivity);
        object.put(KEY_VIRTUAL_MOUSE_INVERT_Y, virtualMouseInvertY);
        object.put(KEY_VIRTUAL_MOUSE_SCROLL_DISTANCE, virtualMouseScrollDistance);
        object.put(KEY_VIRTUAL_MOUSE_FULL_GESTURE_AREA, virtualMouseFullGestureArea);
        return object;
    }

    public static TouchpadSettings fromJson(JSONObject object, TouchpadSettings fallback) {
        TouchpadSettings settings = fallback == null ? new TouchpadSettings() : fallback.copy();
        if (object == null) {
            return settings;
        }
        settings.mode = normalizeMode(object.optString(KEY_MODE, settings.mode));
        settings.leftWeight = clampInt(object.optInt(KEY_LEFT_WEIGHT, settings.leftWeight), 2, 10);
        settings.touchWeight = clampInt(object.optInt(KEY_TOUCH_WEIGHT, settings.touchWeight), 2, 12);
        settings.topWeight = clampInt(object.optInt(KEY_TOP_WEIGHT, settings.topWeight), 2, 8);
        settings.macroWeight = clampInt(object.optInt(KEY_MACRO_WEIGHT, settings.macroWeight), 3, 10);
        settings.sensitivity = clampFloat((float) object.optDouble(KEY_SENSITIVITY, settings.sensitivity), 0.2f, 12f);
        settings.intervalMs = clampInt(object.optInt(KEY_INTERVAL_MS, settings.intervalMs), 0, 160);
        settings.minDelta = clampFloat((float) object.optDouble(KEY_MIN_DELTA, settings.minDelta), 0f, 20f);
        settings.anchorX = clampFloat((float) object.optDouble(KEY_ANCHOR_X, settings.anchorX), 0.05f, 0.95f);
        settings.anchorY = clampFloat((float) object.optDouble(KEY_ANCHOR_Y, settings.anchorY), 0.05f, 0.95f);
        settings.strokeMs = clampInt(object.optInt(KEY_STROKE_MS, settings.strokeMs), 1, 80);
        settings.shizukuTouchSensitivityX = clampFloat((float) object.optDouble(KEY_SHIZUKU_TOUCH_SENSITIVITY_X, settings.shizukuTouchSensitivityX), 0.2f, 12f);
        settings.shizukuTouchSensitivityY = clampFloat((float) object.optDouble(KEY_SHIZUKU_TOUCH_SENSITIVITY_Y, settings.shizukuTouchSensitivityY), 0.2f, 12f);
        settings.shizukuTouchFrameMs = clampInt(object.optInt(KEY_SHIZUKU_TOUCH_FRAME_MS, settings.shizukuTouchFrameMs), 4, 33);
        settings.shizukuTouchMinDelta = clampFloat((float) object.optDouble(KEY_SHIZUKU_TOUCH_MIN_DELTA, settings.shizukuTouchMinDelta), 0f, 20f);
        settings.shizukuTouchCurve = clampFloat((float) object.optDouble(KEY_SHIZUKU_TOUCH_CURVE, settings.shizukuTouchCurve), 0.5f, 3f);
        settings.shizukuTouchSmoothing = clampFloat((float) object.optDouble(KEY_SHIZUKU_TOUCH_SMOOTHING, settings.shizukuTouchSmoothing), 0f, 0.9f);
        settings.rightStickSensitivity = clampFloat((float) object.optDouble(KEY_RIGHT_STICK_SENSITIVITY, settings.rightStickSensitivity), 0.2f, 4f);
        settings.rightStickDeadzone = clampFloat((float) object.optDouble(KEY_RIGHT_STICK_DEADZONE, settings.rightStickDeadzone), 0f, 0.4f);
        settings.rightStickCurve = clampFloat((float) object.optDouble(KEY_RIGHT_STICK_CURVE, settings.rightStickCurve), 0.5f, 3f);
        settings.rightStickMaxOutput = clampFloat((float) object.optDouble(KEY_RIGHT_STICK_MAX_OUTPUT, settings.rightStickMaxOutput), 0.1f, 1f);
        settings.rightStickRadius = clampFloat((float) object.optDouble(KEY_RIGHT_STICK_RADIUS, settings.rightStickRadius), 0.18f, 0.8f);
        settings.rightStickRecenterBursts = clampInt(object.optInt(KEY_RIGHT_STICK_RECENTER, settings.rightStickRecenterBursts), 1, 8);
        settings.rightStickCenterMode = normalizeRightStickCenterMode(object.optString(KEY_RIGHT_STICK_CENTER_MODE, settings.rightStickCenterMode));
        settings.relativeMouseSensitivity = clampFloat(
                (float) object.optDouble(KEY_RELATIVE_MOUSE_SENSITIVITY, settings.relativeMouseSensitivity), 0.2f, 4f);
        settings.relativeMouseMaxOutputPercent = clampFloat(
                (float) object.optDouble(KEY_RELATIVE_MOUSE_MAX_OUTPUT, settings.relativeMouseMaxOutputPercent), 0.1f, 1f);
        settings.relativeMouseInvertY = object.optBoolean(
                KEY_RELATIVE_MOUSE_INVERT_Y, settings.relativeMouseInvertY);
        settings.relativeMouseAcceleration = normalizeRelativeMouseAcceleration(
                object.optString(KEY_RELATIVE_MOUSE_ACCELERATION, settings.relativeMouseAcceleration));
        settings.relativeMousePulseDurationMs = clampInt(
                object.optInt(KEY_RELATIVE_MOUSE_PULSE_MS, settings.relativeMousePulseDurationMs), 8, 30);
        settings.virtualMouseSensitivity = clampFloat(
                (float) object.optDouble(KEY_VIRTUAL_MOUSE_SENSITIVITY, settings.virtualMouseSensitivity), 0.2f, 4f);
        settings.virtualMouseInvertY = object.optBoolean(
                KEY_VIRTUAL_MOUSE_INVERT_Y, settings.virtualMouseInvertY);
        settings.virtualMouseScrollDistance = clampFloat(
                (float) object.optDouble(KEY_VIRTUAL_MOUSE_SCROLL_DISTANCE,
                        settings.virtualMouseScrollDistance), 16f, 96f);
        settings.virtualMouseFullGestureArea = object.optBoolean(
                KEY_VIRTUAL_MOUSE_FULL_GESTURE_AREA, settings.virtualMouseFullGestureArea);
        return settings;
    }

    public static String normalizeMode(String value) {
        if (MODE_RELATIVE_MOVE.equals(value)) {
            return MODE_RELATIVE_MOVE;
        }
        if (MODE_MOUSE_POINTER.equals(value)) {
            return MODE_MOUSE_POINTER;
        }
        if (MODE_VIRTUAL_MOUSE.equals(value)) {
            return MODE_VIRTUAL_MOUSE;
        }
        if (MODE_SHIZUKU_TOUCH.equals(value)) {
            return MODE_SHIZUKU_TOUCH;
        }
        if (MODE_RIGHT_STICK.equals(value)) {
            return MODE_RIGHT_STICK;
        }
        if (MODE_RELATIVE_MOUSE.equals(value)) {
            return MODE_RELATIVE_MOUSE;
        }
        return MODE_TOUCH_DRAG;
    }

    public static String normalizeRelativeMouseAcceleration(String value) {
        if (RELATIVE_MOUSE_ACCELERATION_LOW.equals(value)) {
            return RELATIVE_MOUSE_ACCELERATION_LOW;
        }
        if (RELATIVE_MOUSE_ACCELERATION_MEDIUM.equals(value)) {
            return RELATIVE_MOUSE_ACCELERATION_MEDIUM;
        }
        if (RELATIVE_MOUSE_ACCELERATION_HIGH.equals(value)) {
            return RELATIVE_MOUSE_ACCELERATION_HIGH;
        }
        return RELATIVE_MOUSE_ACCELERATION_OFF;
    }

    public static String relativeMouseAccelerationLabel(String value) {
        String normalized = normalizeRelativeMouseAcceleration(value);
        if (RELATIVE_MOUSE_ACCELERATION_LOW.equals(normalized)) {
            return "Low";
        }
        if (RELATIVE_MOUSE_ACCELERATION_MEDIUM.equals(normalized)) {
            return "Medium";
        }
        if (RELATIVE_MOUSE_ACCELERATION_HIGH.equals(normalized)) {
            return "High";
        }
        return "Off";
    }

    public static String normalizeRightStickCenterMode(String value) {
        if (RIGHT_STICK_CENTER_STATIC.equals(value)) {
            return RIGHT_STICK_CENTER_STATIC;
        }
        return RIGHT_STICK_CENTER_FLOAT;
    }

    public static String rightStickCenterModeLabel(String mode) {
        String normalized = normalizeRightStickCenterMode(mode);
        if (RIGHT_STICK_CENTER_STATIC.equals(normalized)) {
            return "Static";
        }
        return "Float";
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
