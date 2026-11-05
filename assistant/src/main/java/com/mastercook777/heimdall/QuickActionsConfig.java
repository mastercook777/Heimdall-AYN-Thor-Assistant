package com.mastercook777.heimdall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Versioned, per-widget structured Quick Actions configuration. */
public final class QuickActionsConfig {
    public static final int VERSION = 3;
    public static final int MAX_ACTIONS = 8;
    public static final String MEDIA_COMPACT = "compact";
    public static final String MEDIA_FULL = "full";

    public int version = VERSION;
    public boolean mediaVolume = true;
    public boolean mediaControls = false;
    public boolean linkedBrightness = false;
    public String mediaMode = MEDIA_COMPACT;
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
        copy.mediaControls = mediaControls;
        copy.linkedBrightness = linkedBrightness;
        copy.mediaMode = mediaMode;
        copy.actions.addAll(actions);
        copy.sanitize();
        return copy;
    }

    public JSONObject toJson() throws JSONException {
        sanitize();
        JSONObject object = new JSONObject();
        object.put("version", VERSION);
        object.put("mediaVolume", mediaVolume);
        object.put("mediaControls", mediaControls);
        object.put("linkedBrightness", linkedBrightness);
        object.put("mediaMode", mediaMode);
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
        config.mediaControls = object.optBoolean("mediaControls", false);
        config.linkedBrightness = object.optBoolean("linkedBrightness", false);
        config.mediaMode = object.optString("mediaMode", MEDIA_COMPACT);
        JSONArray array = object.optJSONArray("actions");
        if (array != null) {
            for (int index = 0; index < array.length() && index < MAX_ACTIONS; index++) {
                config.actions.add(array.optString(index, HeimdallActionCatalog.ACTION_NONE));
            }
        }
        config.sanitize();
        return config;
    }

    public void sanitize() {
        version = VERSION;
        mediaMode = MEDIA_FULL.equals(mediaMode) ? MEDIA_FULL : MEDIA_COMPACT;
        while (actions.size() > MAX_ACTIONS) {
            actions.remove(actions.size() - 1);
        }
        for (int index = actions.size() - 1; index >= 0; index--) {
            String normalized = HeimdallActionCatalog.normalizeQuickAction(actions.get(index));
            if (HeimdallActionCatalog.ACTION_NONE.equals(normalized)) {
                actions.remove(index);
            } else {
                actions.set(index, normalized);
            }
        }
    }

    public String actionAt(int index) {
        if (index < 0 || index >= actions.size()) {
            return HeimdallActionCatalog.ACTION_NONE;
        }
        return HeimdallActionCatalog.normalizeQuickAction(actions.get(index));
    }

    public void setActionAt(int index, String action) {
        if (index < 0 || index >= MAX_ACTIONS) {
            return;
        }
        if (HeimdallActionCatalog.ACTION_NONE.equals(
                HeimdallActionCatalog.normalizeQuickAction(action))) {
            removeActionAt(index);
            return;
        }
        while (actions.size() <= index) {
            actions.add(HeimdallActionCatalog.ACTION_SCREENSHOT);
        }
        actions.set(index, HeimdallActionCatalog.normalizeQuickAction(action));
        sanitize();
    }

    public void removeActionAt(int index) {
        if (index >= 0 && index < actions.size()) {
            actions.remove(index);
        }
    }

    public void moveAction(int from, int to) {
        if (from < 0 || from >= actions.size() || to < 0 || to >= actions.size()) {
            return;
        }
        actions.add(to, actions.remove(from));
    }
}
