package com.mastercook777.heimdall;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
    static final int MAX_SLIDES = 20;
    static final int DEFAULT_SLIDE_SECONDS = 10;
    static final String TRANSITION_NONE = "none";
    static final String TRANSITION_FADE = "fade";
    static final String TRANSITION_FROM_LEFT = "from_left";
    static final String TRANSITION_FROM_RIGHT = "from_right";

    String sourceType = SOURCE_LOCAL_IMAGE;
    String assetId = "";
    float focusX = 0.5f;
    float focusY = 0.5f;
    float zoom = MIN_ZOOM;
    String shape = SHAPE_RECTANGLE;
    boolean animated;
    boolean video;
    final List<Slide> slides = new ArrayList<>();
    int slideSeconds = DEFAULT_SLIDE_SECONDS;
    boolean autoRotate = true;
    String slideTransition = TRANSITION_NONE;

    static final class Slide {
        String assetId;
        float focusX;
        float focusY;
        float zoom;

        Slide(String assetId, float focusX, float focusY, float zoom) {
            this.assetId = assetId == null ? "" : assetId.trim();
            this.focusX = focusX;
            this.focusY = focusY;
            this.zoom = zoom;
            normalize();
        }

        Slide copy() { return new Slide(assetId, focusX, focusY, zoom); }

        void normalize() {
            assetId = assetId == null ? "" : assetId.trim();
            focusX = clamp(focusX, 0f, 1f);
            focusY = clamp(focusY, 0f, 1f);
            zoom = normalizeZoom(zoom);
        }

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("assetId", assetId);
            object.put("focusX", focusX);
            object.put("focusY", focusY);
            object.put("zoom", zoom);
            return object;
        }

        static Slide fromJson(JSONObject object) {
            return new Slide(object.optString("assetId", ""),
                    (float) object.optDouble("focusX", 0.5d),
                    (float) object.optDouble("focusY", 0.5d),
                    (float) object.optDouble("zoom", MIN_ZOOM));
        }
    }

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
        copy.slideSeconds = slideSeconds;
        copy.autoRotate = autoRotate;
        copy.slideTransition = slideTransition;
        for (Slide slide : slides) copy.slides.add(slide.copy());
        return copy;
    }

    boolean isSlideshow() { return slides.size() >= 2; }

    void ensureSlides() {
        if (slides.isEmpty() && hasAsset() && !animated && !video) {
            slides.add(new Slide(assetId, focusX, focusY, zoom));
        }
    }

    CanvasConfig slideConfig(int index) {
        CanvasConfig result = copy();
        result.slides.clear();
        if (!slides.isEmpty()) {
            Slide slide = slides.get(Math.max(0, Math.min(index, slides.size() - 1)));
            result.assetId = slide.assetId;
            result.focusX = slide.focusX;
            result.focusY = slide.focusY;
            result.zoom = slide.zoom;
            result.animated = false;
            result.video = false;
        }
        return result;
    }

    void syncFirstSlide() {
        if (slides.isEmpty()) return;
        Slide first = slides.get(0);
        assetId = first.assetId;
        focusX = first.focusX;
        focusY = first.focusY;
        zoom = first.zoom;
        animated = false;
        video = false;
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
        slideSeconds = slideSeconds <= 5 ? 5 : slideSeconds <= 10 ? 10
                : slideSeconds <= 20 ? 20 : 30;
        slideTransition = normalizeTransition(slideTransition);
        for (Slide slide : slides) slide.normalize();
        while (slides.size() > MAX_SLIDES) slides.remove(slides.size() - 1);
        syncFirstSlide();
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
        if (!slides.isEmpty()) {
            JSONArray items = new JSONArray();
            for (Slide slide : slides) items.put(slide.toJson());
            object.put("slides", items);
            object.put("slideSeconds", slideSeconds);
            object.put("autoRotate", autoRotate);
            object.put("slideTransition", slideTransition);
        }
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
        JSONArray slides = object.optJSONArray("slides");
        if (slides != null && !config.animated && !config.video) {
            for (int i = 0; i < Math.min(slides.length(), MAX_SLIDES); i++) {
                JSONObject slide = slides.optJSONObject(i);
                if (slide != null) config.slides.add(Slide.fromJson(slide));
            }
        }
        config.slideSeconds = object.optInt("slideSeconds", DEFAULT_SLIDE_SECONDS);
        config.autoRotate = object.optBoolean("autoRotate", true);
        config.slideTransition = object.optString("slideTransition", TRANSITION_NONE);
        config.normalize();
        return config;
    }

    private static String normalizeTransition(String value) {
        return TRANSITION_FADE.equals(value)
                || TRANSITION_FROM_LEFT.equals(value)
                || TRANSITION_FROM_RIGHT.equals(value)
                ? value : TRANSITION_NONE;
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
