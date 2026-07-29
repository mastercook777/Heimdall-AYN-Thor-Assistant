package com.mastercook777.heimdall;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ProfileStore {
    private static final String PREFS = "thor_assistant_profiles";
    private static final String KEY_PROFILES = "profiles";
    private static final String KEY_SELECTED = "selected_profile";

    private ProfileStore() {
    }

    public static List<GameProfile> loadProfiles(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_PROFILES, "");
        if (raw == null || raw.length() == 0) {
            List<GameProfile> defaults = defaultProfiles();
            saveProfiles(context, defaults);
            return defaults;
        }

        try {
            JSONArray array = new JSONArray(raw);
            List<GameProfile> result = new ArrayList<>();
            TouchpadSettings legacyTouchpadSettings = TouchpadSettings.load(context);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                result.add(GameProfile.fromJson(object,
                        object.has("touchpadSettings") ? null : legacyTouchpadSettings));
            }
            if (!result.isEmpty()) {
                if (sanitizeProfiles(result)) {
                    int selectedIndex = prefs.getInt(KEY_SELECTED, 0);
                    if (ProfileSnapshotStore.createRawSnapshot(context, raw, selectedIndex,
                            ProfileSnapshotStore.REASON_SANITIZE)) {
                        saveProfiles(context, result);
                    }
                }
                return result;
            }
        } catch (JSONException ignored) {
        }

        List<GameProfile> defaults = defaultProfiles();
        int selectedIndex = prefs.getInt(KEY_SELECTED, 0);
        if (ProfileSnapshotStore.createRawSnapshot(context, raw, selectedIndex,
                ProfileSnapshotStore.REASON_RECOVERY)) {
            saveProfiles(context, defaults);
        }
        return defaults;
    }

    public static void saveProfiles(Context context, List<GameProfile> profiles) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PROFILES, profilesToJson(profiles))
                .apply();
    }

    public static String profilesToJson(List<GameProfile> profiles) {
        JSONArray array = new JSONArray();
        for (GameProfile profile : profiles) {
            try {
                array.put(profile.toJson());
            } catch (JSONException ignored) {
            }
        }
        return array.toString();
    }

    static String rawProfilesJson(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PROFILES, "");
    }

    public static String profilesToExportJson(List<GameProfile> profiles, String exportedAt) {
        JSONObject object = new JSONObject();
        try {
            object.put("app", "Heimdall");
            object.put("type", "profiles");
            object.put("version", 1);
            object.put("exportedAt", exportedAt == null ? "" : exportedAt);
            object.put("profiles", new JSONArray(profilesToJson(profiles)));
        } catch (JSONException ignored) {
            return profilesToJson(profiles);
        }
        return object.toString();
    }

    public static List<GameProfile> profilesFromJson(String raw) throws JSONException {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.length() == 0) {
            throw new JSONException("Empty profile data");
        }
        JSONArray array;
        if (trimmed.startsWith("[")) {
            array = new JSONArray(trimmed);
        } else {
            JSONObject object = new JSONObject(trimmed);
            JSONArray wrappedProfiles = object.optJSONArray("profiles");
            if (wrappedProfiles != null) {
                array = wrappedProfiles;
            } else {
                array = new JSONArray();
                array.put(object);
            }
        }
        List<GameProfile> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.optJSONObject(i);
            if (object != null) {
                result.add(GameProfile.fromJson(object));
            }
        }
        if (result.isEmpty()) {
            throw new JSONException("No valid profiles");
        }
        sanitizeProfiles(result);
        return result;
    }

    public static int loadSelectedIndex(Context context, int size) {
        int index = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_SELECTED, 0);
        if (index < 0 || index >= size) {
            return 0;
        }
        return index;
    }

    public static void saveSelectedIndex(Context context, int index) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SELECTED, index)
                .apply();
    }

    static List<MacroStep> steps(String... rawSteps) {
        List<MacroStep> result = new ArrayList<>();
        for (String rawStep : rawSteps) {
            result.add(MacroStep.parse(rawStep));
        }
        return result;
    }

    private static boolean sanitizeProfiles(List<GameProfile> profiles) {
        boolean changed = false;
        for (int i = profiles.size() - 1; i >= 0; i--) {
            if (isLegacyDemoProfile(profiles.get(i))) {
                profiles.remove(i);
                changed = true;
            }
        }
        if (profiles.isEmpty()) {
            profiles.add(defaultProfile());
            changed = true;
        }
        for (GameProfile profile : profiles) {
            if (profile.touchpadSettings == null) {
                profile.touchpadSettings = new TouchpadSettings();
                changed = true;
            }
            if (profile.widgetLayout == null) {
                profile.widgetLayout = WidgetLayout.defaultLayout();
                changed = true;
            }
            profile.widgetLayout.sanitize();
            int widgetMacroCount = requiredMacroCount(profile.widgetLayout);
            int desiredCount = Math.max(1, Math.min(24, profile.macroCount));
            desiredCount = Math.max(desiredCount, widgetMacroCount);
            if (profile.macroCount != desiredCount) {
                profile.macroCount = desiredCount;
                changed = true;
            }
            int oldColumns = profile.macroColumns;
            int oldRows = profile.macroRows;
            profile.normalizeLayout();
            if (profile.macroColumns != oldColumns || profile.macroRows != oldRows) {
                changed = true;
            }
            while (profile.macros.size() < profile.macroCount) {
                profile.macros.add(new Macro("Macro " + (profile.macros.size() + 1), steps("wait:80ms")));
                changed = true;
            }
            for (Macro macro : profile.macros) {
                for (int i = 0; i < macro.steps.size(); i++) {
                    MacroStep step = macro.steps.get(i);
                    if (!step.isValidForStorage()) {
                        macro.steps.set(i, new MacroStep(MacroStep.TYPE_WAIT, "80ms"));
                        changed = true;
                    }
                }
                if (macro.steps.isEmpty()) {
                    macro.steps.add(new MacroStep(MacroStep.TYPE_WAIT, "80ms"));
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static List<GameProfile> defaultProfiles() {
        List<GameProfile> result = new ArrayList<>();
        result.add(defaultProfile());
        return result;
    }

    private static int requiredMacroCount(WidgetLayout layout) {
        int required = 1;
        if (layout == null) {
            return required;
        }
        for (WidgetLayout.Item item : layout.items) {
            if (WidgetLayout.TYPE_MACRO_GROUP.equals(item.type)) {
                required = Math.max(required, item.macroStart + item.macroCount);
            }
        }
        return Math.min(24, required);
    }

    private static GameProfile defaultProfile() {
        GameProfile profile = new GameProfile("\u9ed8\u8ba4 Profile", "\u901a\u7528", "", 4,
                macros("Skill 1", "Skill 2", "Skill 3", "Skill 4"));
        profile.macroColumns = 2;
        profile.macroRows = 2;
        profile.rightHandPriority = true;
        profile.touchpadSettings = new TouchpadSettings();
        profile.widgetLayout = WidgetLayout.defaultLayout();
        profile.normalizeLayout();
        return profile;
    }

    private static boolean isLegacyDemoProfile(GameProfile profile) {
        if (profile == null || profile.macroCount != 12 || profile.macros.size() < 12) {
            return false;
        }
        String name = profile.name == null ? "" : profile.name;
        boolean knownName = "Android Action".equals(name) || "Emulator".equals(name) || "MOBA".equals(name);
        if (!knownName) {
            return false;
        }
        if ("Android Action".equals(name) && !"com.game.example".equals(profile.packageHint)) {
            return false;
        }
        if ("Emulator".equals(name) && !"org.emulator.example".equals(profile.packageHint)) {
            return false;
        }
        if ("MOBA".equals(name) && !"com.moba.example".equals(profile.packageHint)) {
            return false;
        }
        for (Macro macro : profile.macros) {
            if (macro.steps.size() != 1) {
                return false;
            }
            MacroStep step = macro.steps.get(0);
            if (!MacroStep.TYPE_WAIT.equals(step.type) || !"80ms".equals(step.value)) {
                return false;
            }
        }
        return labelsMatch(profile, legacyLabels(name));
    }

    private static boolean labelsMatch(GameProfile profile, String[] labels) {
        if (labels.length > profile.macros.size()) {
            return false;
        }
        for (int i = 0; i < labels.length; i++) {
            if (!labels[i].equals(profile.macros.get(i).label)) {
                return false;
            }
        }
        return true;
    }

    private static String[] legacyLabels(String name) {
        if ("Emulator".equals(name)) {
            return new String[]{"Save", "Load", "Turbo", "Menu", "A+B", "L+R", "Fast", "Slow", "Guide", "Map", "Touch", "Keyboard"};
        }
        if ("MOBA".equals(name)) {
            return new String[]{"Skill 1", "Skill 2", "Skill 3", "Ult", "Buy", "Ping", "Ward", "Recall", "Map", "Build", "Timer", "Chat"};
        }
        return new String[]{"Skill 1", "Skill 2", "Skill 3", "Dodge", "Combo A", "Combo B", "Burst", "Heal", "Map", "Quest", "Chat", "Record"};
    }

    private static List<Macro> macros(String... labels) {
        List<Macro> macros = new ArrayList<>();
        for (String label : labels) {
            macros.add(new Macro(label, steps("wait:80ms")));
        }
        return macros;
    }
}
