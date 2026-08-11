package com.mastercook777.heimdall;

import org.json.JSONException;
import org.json.JSONObject;

final class CanvasConfig {
    static final String SOURCE_LOCAL_IMAGE = "local_image";
    static final String SHAPE_RECTANGLE = "rectangle";
    static final String SHAPE_CIRCLE = "circle";
    static final float MIN_ZOOM = 1f;
    static final float MAX_ZOOM = 8f;

    String sourceType = SOURCE_LOCAL_IMAGE;
    String assetId = "";
    float focusX = 0.5f;
    float focusY = 0.5f;
    float zoom = MIN_ZOOM;
    String shape = SHAPE_RECTANGLE;

    CanvasConfig copy() {
        CanvasConfig copy = new CanvasConfig();
        copy.sourceType = sourceType;
        copy.assetId = assetId;
        copy.focusX = focusX;
        copy.focusY = focusY;
        copy.zoom = zoom;
        copy.shape = shape;
        return copy;
    }

    boolean hasAsset() {
        return assetId != null && assetId.trim().length() > 0;
    }

    boolean isCircular() {
        return SHAPE_CIRCLE.equals(normalizeShape(shape));
    }

    void normalize() {
        sourceType = SOURCE_LOCAL_IMAGE;
        assetId = assetId == null ? "" : assetId.trim();
        focusX = clamp(focusX, 0f, 1f);
        focusY = clamp(focusY, 0f, 1f);
        zoom = clamp(zoom, MIN_ZOOM, MAX_ZOOM);
        shape = normalizeShape(shape);
    }

    JSONObject toJson() throws JSONException {
        normalize();
        JSONObject object = new JSONObject();
        object.put("sourceType", sourceType);
        object.put("assetId", assetId);
        object.put("focusX", focusX);
        object.put("focusY", focusY);
        object.put("zoom", zoom);
        object.put("shape", shape);
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
        config.shape = object.optString("shape", SHAPE_RECTANGLE);
        config.normalize();
        return config;
    }

    private static String normalizeShape(String value) {
        return SHAPE_CIRCLE.equals(value) ? SHAPE_CIRCLE : SHAPE_RECTANGLE;
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
