package com.mastercook777.heimdall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class Macro {
    public static final String ROLE_PRIMARY = "primary";
    public static final String ROLE_SECONDARY = "secondary";
    public static final String ROLE_UTILITY = "utility";

    public String label;
    public boolean highlighted;
    public String role = ROLE_SECONDARY;
    public String iconKey;
    public final List<MacroStep> steps = new ArrayList<>();

    public Macro(String label, List<MacroStep> steps) {
        this.label = label;
        this.steps.addAll(steps);
    }

    public void overwriteFrom(Macro source) {
        if (source == null || source == this) {
            return;
        }
        label = source.label;
        role = normalizeRole(source.role);
        highlighted = ROLE_PRIMARY.equals(role);
        iconKey = source.iconKey;
        steps.clear();
        for (MacroStep step : source.steps) {
            if (step != null) {
                steps.add(new MacroStep(step.type, step.value));
            }
        }
        if (steps.isEmpty()) {
            steps.add(new MacroStep(MacroStep.TYPE_WAIT, "80ms"));
        }
    }

    public boolean hasCancellableControllerHold() {
        for (MacroStep step : steps) {
            if (!MacroStep.TYPE_GAMEPAD.equals(step.type)) {
                continue;
            }
            GamepadSequenceComposer.EditableSequence sequence =
                    GamepadSequenceComposer.parseEditable(step.value);
            if (sequence == null) {
                continue;
            }
            for (GamepadSequenceComposer.Frame frame : sequence.frames) {
                if (frame.holdOverrideMs > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public String stepsAsText() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(steps.get(i).toString());
        }
        return builder.toString();
    }

    public void replaceStepsFromText(String text) {
        steps.clear();
        String[] lines = text == null ? new String[0] : text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 0) {
                steps.add(MacroStep.parse(trimmed));
            }
        }
        if (steps.isEmpty()) {
            steps.add(new MacroStep(MacroStep.TYPE_WAIT, "80ms"));
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("label", label);
        role = normalizeRole(role);
        highlighted = ROLE_PRIMARY.equals(role);
        object.put("role", role);
        object.put("highlighted", highlighted);
        if (iconKey != null && iconKey.trim().length() > 0) {
            object.put("iconKey", iconKey.trim());
        }
        JSONArray array = new JSONArray();
        for (MacroStep step : steps) {
            array.put(step.toJson());
        }
        object.put("steps", array);
        return object;
    }

    public static Macro fromJson(JSONObject object) {
        List<MacroStep> parsedSteps = new ArrayList<>();
        JSONArray array = object.optJSONArray("steps");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject step = array.optJSONObject(i);
                if (step != null) {
                    parsedSteps.add(MacroStep.fromJson(step));
                }
            }
        }
        if (parsedSteps.isEmpty()) {
            parsedSteps.add(new MacroStep(MacroStep.TYPE_WAIT, "80ms"));
        }
        Macro macro = new Macro(object.optString("label", "Macro"), parsedSteps);
        macro.highlighted = object.optBoolean("highlighted", false);
        macro.role = object.has("role")
                ? normalizeRole(object.optString("role", ROLE_SECONDARY))
                : (macro.highlighted ? ROLE_PRIMARY : ROLE_SECONDARY);
        macro.highlighted = ROLE_PRIMARY.equals(macro.role);
        String iconKey = object.optString("iconKey", "");
        macro.iconKey = iconKey.trim().length() == 0 ? null : iconKey.trim();
        return macro;
    }

    public String normalizedRole() {
        role = normalizeRole(role);
        return role;
    }

    public static String normalizeRole(String value) {
        if (ROLE_PRIMARY.equals(value) || ROLE_UTILITY.equals(value)) {
            return value;
        }
        return ROLE_SECONDARY;
    }
}
