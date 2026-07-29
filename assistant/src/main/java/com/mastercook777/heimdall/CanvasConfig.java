package com.mastercook777.heimdall;

import org.json.JSONException;
import org.json.JSONObject;

final class CanvasConfig {
    static final String SOURCE_LOCAL_IMAGE = "local_image";
    static final float MIN_ZOOM = 1f;
    static final float MAX_ZOOM = 8f;

    String sourceType = SOURCE_LOCAL_IMAGE;
    String assetId = "";
    float focusX = 0.5f;
    float focusY = 0.5f;
    float zoom = MIN_ZOOM;

    CanvasConfig copy() {
        CanvasConfig copy = new CanvasConfig();
        copy.sourceType = sourceType;
        copy.assetId = assetId;
        copy.focusX = focusX;
        copy.focusY = focusY;
        copy.zoom = zoom;
        return copy;
    }

    boolean hasAsset() {
        return assetId != null && assetId.trim().length() > 0;
    }

    void normalize() {
        sourceType = SOURCE_LOCAL_IMAGE;
        assetId = assetId == null ? "" : assetId.trim();
        focusX = clamp(focusX, 0f, 1f);
        focusY = clamp(focusY, 0f, 1f);
        zoom = clamp(zoom, MIN_ZOOM, MAX_ZOOM);
    }

    JSONObject toJson() throws JSONException {
        normalize();
        JSONObject object = new JSONObject();
        object.put("sourceType", sourceType);
        object.put("assetId", assetId);
        object.put("focusX", focusX);
        object.put("focusY", focusY);
        object.put("zoom", zoom);
        return object;
    }

    static CanvasConfig fromJson(JSONObject object) {
        CanvasConfig config = new CanvasConfig();
        if (object == null) {
            return config;
        }
        config.sourceType = object.optString("sourceType", SOURCE_LOCAL_IMAGE);
        config.assetId = object.optString("assetId", "");
        config.focusX = (float) object.optDouble("focusX", 0.5d);
        config.focusY = (float) object.optDouble("focusY", 0.5d);
        config.zoom = (float) object.optDouble("zoom", MIN_ZOOM);
        config.normalize();
        return config;
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
