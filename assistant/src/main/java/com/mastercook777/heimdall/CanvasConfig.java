package com.mastercook777.heimdall;

import org.json.JSONException;
import org.json.JSONObject;

final class CanvasConfig {
    static final String SOURCE_LOCAL_IMAGE = "local_image";
    static final String SHAPE_RECTANGLE = "rectangle";
    static final String SHAPE_CIRCLE = "circle";
    static final float MIN_ZOOM = 1f;
    static final float MAX_GESTURE_ZOOM = 8f;
    // A Fill composition may legitimately exceed the ordinary gesture range when an
    // accepted 4096px source is placed in an extreme 12 x 1 Grid viewport. Keep a
    // bounded persisted range so that Fill can cover without trusting corrupt JSON.
    static final float MAX_COMPOSITION_ZOOM = 65536f;

    String sourceType = SOURCE_LOCAL_IMAGE;
    String assetId = "";
    float focusX = 0.5f;
    float focusY = 0.5f;
    float zoom = MIN_ZOOM;
    String shape = SHAPE_RECTANGLE;
    boolean animated;
    boolean video;

    CanvasConfig copy() {
        CanvasConfig copy = new CanvasConfig();
        copy.sourceType = sourceType;
        copy.assetId = assetId;
        copy.focusX = focusX;
        copy.focusY = focusY;
        copy.zoom = zoom;
        copy.shape = shape;
        copy.animated = animated;
        copy.video = video;
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
        zoom = normalizeZoom(zoom);
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
        object.put("animated", animated);
        object.put("video", video);
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
        config.animated = object.optBoolean("animated", false);
        config.video = object.optBoolean("video", false);
        config.normalize();
        return config;
    }

    private static String normalizeShape(String value) {
        return SHAPE_CIRCLE.equals(value) ? SHAPE_CIRCLE : SHAPE_RECTANGLE;
    }

    static float normalizeZoom(float value) {
        return clamp(value, MIN_ZOOM, MAX_COMPOSITION_ZOOM);
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
