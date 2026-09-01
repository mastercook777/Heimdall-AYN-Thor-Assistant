package com.mastercook777.heimdall;

import org.json.JSONException;
import org.json.JSONObject;

public final class GameContextBinding {
    static final int VERSION = 1;
    static final String KIND_SAF_DOCUMENT = "saf_document";
    static final String KIND_EMULATOR_TITLE_ID = "emulator_title_id";

    public int version = VERSION;
    public String kind = "";
    public String identityKey = "";
    public String label = "";

    public boolean isBound() {
        return VERSION == version
                && (KIND_SAF_DOCUMENT.equals(kind)
                || KIND_EMULATOR_TITLE_ID.equals(kind)
                || RetroArchGameContext.KIND_CONTENT.equals(kind)
                || RetroArchGameContext.KIND_PLATFORM.equals(kind))
                && identityKey != null
                && identityKey.trim().length() > 0;
    }

    public boolean isPlatformBinding() {
        return isBound() && RetroArchGameContext.KIND_PLATFORM.equals(kind);
    }

    public GameContextBinding copy() {
        GameContextBinding copy = new GameContextBinding();
        copy.version = version;
        copy.kind = safe(kind);
        copy.identityKey = safe(identityKey);
        copy.label = safe(label);
        return copy;
    }

    JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("version", version);
        object.put("kind", safe(kind));
        object.put("identityKey", safe(identityKey));
        object.put("label", safe(label));
        return object;
    }

    static GameContextBinding fromJson(JSONObject object) {
        GameContextBinding binding = new GameContextBinding();
        if (object == null) return binding;
        binding.version = object.optInt("version", VERSION);
        binding.kind = object.optString("kind", "");
        binding.identityKey = object.optString("identityKey", "");
        binding.label = object.optString("label", "");
        return binding;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
