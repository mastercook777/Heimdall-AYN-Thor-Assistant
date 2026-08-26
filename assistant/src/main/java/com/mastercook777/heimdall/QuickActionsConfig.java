package com.mastercook777.heimdall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Versioned, per-widget structured Quick Actions configuration. */
public final class QuickActionsConfig {
    public static final int VERSION = 1;
    public static final int MAX_SLOTS = 3;

    public int version = VERSION;
    public boolean mediaVolume = true;
    public final List<String> actions = new ArrayList<>();

    /** Missing legacy data keeps the accepted Screenshot + Recording + Volume surface. */
    public static QuickActionsConfig legacyDefault() {
        QuickActionsConfig config = new QuickActionsConfig();
        config.actions.add(HeimdallActionCatalog.ACTION_SCREENSHOT);
        config.actions.add(HeimdallActionCatalog.ACTION_SCREEN_RECORDING);
        return config;
    }

    /** Newly added modules expose the full keyboard without changing old Profiles. */
    public static QuickActionsConfig newModuleDefault() {
        QuickActionsConfig config = legacyDefault();
        config.actions.add(HeimdallActionCatalog.ACTION_OPEN_VIRTUAL_KEYBOARD);
        return config;
    }

    public QuickActionsConfig copy() {
        QuickActionsConfig copy = new QuickActionsConfig();
        copy.version = version;
        copy.mediaVolume = mediaVolume;
        copy.actions.addAll(actions);
        copy.sanitize();
        return copy;
    }

    public JSONObject toJson() throws JSONException {
        sanitize();
        JSONObject object = new JSONObject();
        object.put("version", VERSION);
        object.put("mediaVolume", mediaVolume);
        JSONArray array = new JSONArray();
        for (String action : actions) {
            array.put(action);
        }
        object.put("actions", array);
        return object;
    }

    public static QuickActionsConfig fromJson(JSONObject object) {
        if (object == null) {
            return legacyDefault();
        }
        QuickActionsConfig config = new QuickActionsConfig();
        config.version = object.optInt("version", VERSION);
        config.mediaVolume = object.optBoolean("mediaVolume", true);
        JSONArray array = object.optJSONArray("actions");
        if (array != null) {
            for (int index = 0; index < array.length() && index < MAX_SLOTS; index++) {
                config.actions.add(array.optString(index, HeimdallActionCatalog.ACTION_NONE));
            }
        }
        config.sanitize();
        return config;
    }

    public void sanitize() {
        version = VERSION;
        while (actions.size() > MAX_SLOTS) {
            actions.remove(actions.size() - 1);
        }
        for (int index = 0; index < actions.size(); index++) {
            actions.set(index, HeimdallActionCatalog.normalizeQuickAction(actions.get(index)));
        }
        while (!actions.isEmpty()
                && HeimdallActionCatalog.ACTION_NONE.equals(actions.get(actions.size() - 1))) {
            actions.remove(actions.size() - 1);
        }
    }

    public String actionAt(int index) {
        if (index < 0 || index >= actions.size()) {
            return HeimdallActionCatalog.ACTION_NONE;
        }
        return HeimdallActionCatalog.normalizeQuickAction(actions.get(index));
    }

    public void setActionAt(int index, String action) {
        if (index < 0 || index >= MAX_SLOTS) {
            return;
        }
        while (actions.size() <= index) {
            actions.add(HeimdallActionCatalog.ACTION_NONE);
        }
        actions.set(index, HeimdallActionCatalog.normalizeQuickAction(action));
        sanitize();
    }
}
