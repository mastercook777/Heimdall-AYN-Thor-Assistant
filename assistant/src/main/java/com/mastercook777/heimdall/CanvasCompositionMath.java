package com.mastercook777.heimdall;

final class CanvasCompositionMath {
    private static final float POST_FILL_ZOOM_MULTIPLIER = 2f;

    private CanvasCompositionMath() {
    }

    static float coverZoom(int viewportWidth, int viewportHeight,
            int sourceWidth, int sourceHeight) {
        if (viewportWidth <= 0 || viewportHeight <= 0
                || sourceWidth <= 0 || sourceHeight <= 0) {
            return CanvasConfig.MIN_ZOOM;
        }
        double widthScale = viewportWidth / (double) sourceWidth;
        double heightScale = viewportHeight / (double) sourceHeight;
        double fitScale = Math.min(widthScale, heightScale);
        double fillScale = Math.max(widthScale, heightScale);
        if (!(fitScale > 0d) || !Double.isFinite(fitScale)
                || !Double.isFinite(fillScale)) {
            return CanvasConfig.MIN_ZOOM;
        }
        return CanvasConfig.normalizeZoom((float) (fillScale / fitScale));
    }

    static float gestureMaximumZoom(int viewportWidth, int viewportHeight,
            int sourceWidth, int sourceHeight) {
        float coverZoom = coverZoom(
                viewportWidth, viewportHeight, sourceWidth, sourceHeight);
        float postFillZoom = CanvasConfig.normalizeZoom(
                coverZoom * POST_FILL_ZOOM_MULTIPLIER);
        return Math.max(CanvasConfig.MAX_GESTURE_ZOOM, postFillZoom);
    }
}
