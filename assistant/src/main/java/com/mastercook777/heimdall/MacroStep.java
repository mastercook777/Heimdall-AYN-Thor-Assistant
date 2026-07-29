package com.mastercook777.heimdall;

import org.json.JSONException;
import org.json.JSONObject;

public final class MacroStep {
    public static final String TYPE_TAP = "tap";
    public static final String TYPE_HOLD = "hold";
    public static final String TYPE_SWIPE = "swipe";
    public static final String TYPE_WAIT = "wait";
    public static final String TYPE_GAMEPAD = "gamepad";

    public final String type;
    public final String value;

    public MacroStep(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public static MacroStep parse(String raw) {
        String trimmed = raw == null ? "" : raw.trim()
                .replace('\uff1a', ':')
                .replace('\uff0c', ',');
        if (trimmed.length() == 0) {
            return new MacroStep(TYPE_WAIT, "80ms");
        }

        int split = trimmed.indexOf(':');
        if (split <= 0 || split == trimmed.length() - 1) {
            return new MacroStep(TYPE_TAP, trimmed);
        }

        return new MacroStep(trimmed.substring(0, split).trim(), trimmed.substring(split + 1).trim());
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("type", type);
        object.put("value", value);
        return object;
    }

    public static MacroStep fromJson(JSONObject object) {
        return new MacroStep(object.optString("type", TYPE_WAIT), object.optString("value", "80ms"));
    }

    public boolean isValidForStorage() {
        if (TYPE_WAIT.equals(type) || TYPE_GAMEPAD.equals(type)) {
            return true;
        }
        if (TYPE_TAP.equals(type)) {
            return hasNumberCount(value, 2) || hasNumberCount(value, 3);
        }
        if (TYPE_HOLD.equals(type)) {
            return hasNumberCount(value, 3) || hasNumberCount(value, 4);
        }
        if (TYPE_SWIPE.equals(type)) {
            return hasNumberCount(value, 5) || hasNumberCount(value, 6);
        }
        return false;
    }

    private static boolean hasNumberCount(String value, int expected) {
        if (value == null) {
            return false;
        }
        String[] parts = value.replace('\uff0c', ',').split(",");
        if (parts.length != expected) {
            return false;
        }
        for (String part : parts) {
            try {
                Float.parseFloat(part.trim());
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return type + ":" + value;
    }
}
