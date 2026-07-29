package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

@SuppressLint("ClickableViewAccessibility")
final class CanvasImageView extends ImageView {
    private final Matrix compositionMatrix = new Matrix();
    private final float[] matrixValues = new float[9];
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector panDetector;
    private CanvasConfig composition = new CanvasConfig();
    private boolean interactive;
    private boolean resetToFillWhenReady;
    private float fitScale = 1f;

    CanvasImageView(Context context) {
        super(context);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float current = currentScale();
                        float minimum = Math.max(0.0001f, fitScale * CanvasConfig.MIN_ZOOM);
                        float maximum = Math.max(minimum, fitScale * CanvasConfig.MAX_ZOOM);
                        float target = clamp(current * detector.getScaleFactor(), minimum, maximum);
                        float factor = target / Math.max(0.0001f, current);
                        compositionMatrix.postScale(factor, factor,
                                clamp(detector.getFocusX(), 0f, getWidth()),
                                clamp(detector.getFocusY(), 0f, getHeight()));
                        constrainMatrix();
                        updateCompositionFromMatrix();
                        applyMatrix();
                        return true;
                    }
                });
        panDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent event) {
                        return true;
                    }

                    @Override
                    public boolean onScroll(MotionEvent first, MotionEvent current,
                            float distanceX, float distanceY) {
                        if (scaleDetector.isInProgress()) {
                            return false;
                        }
                        compositionMatrix.postTranslate(-distanceX, -distanceY);
                        constrainMatrix();
                        updateCompositionFromMatrix();
                        applyMatrix();
                        return true;
                    }
                });
    }

    void setComposition(CanvasConfig value, boolean resetToFill) {
        composition = value == null ? new CanvasConfig() : value.copy();
        composition.normalize();
        resetToFillWhenReady = resetToFill;
        post(this::applyStoredComposition);
    }

    CanvasConfig composition() {
        updateCompositionFromMatrix();
        return composition.copy();
    }

    void setInteractive(boolean value) {
        interactive = value;
    }

    void fitImage() {
        setCenteredZoom(CanvasConfig.MIN_ZOOM);
    }

    void fillImage() {
        Drawable drawable = getDrawable();
        if (!isReady(drawable)) {
            return;
        }
        float fillScale = Math.max(getWidth() / (float) drawable.getIntrinsicWidth(),
                getHeight() / (float) drawable.getIntrinsicHeight());
        setCenteredZoom(clamp(fillScale / Math.max(0.0001f, fitScale),
                CanvasConfig.MIN_ZOOM, CanvasConfig.MAX_ZOOM));
    }

    void resetImage() {
        fillImage();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        DebugPerformanceDiagnostics.countDraw("Canvas image surface");
        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        if (oldWidth > 0 && oldHeight > 0) {
            updateCompositionFromMatrix();
        }
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        applyStoredComposition();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        post(this::applyStoredComposition);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!interactive || getDrawable() == null) {
            return false;
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        scaleDetector.onTouchEvent(event);
        panDetector.onTouchEvent(event);
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            constrainMatrix();
            updateCompositionFromMatrix();
            applyMatrix();
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }

    private void applyStoredComposition() {
        Drawable drawable = getDrawable();
        if (!isReady(drawable)) {
            return;
        }
        fitScale = Math.min(getWidth() / (float) drawable.getIntrinsicWidth(),
                getHeight() / (float) drawable.getIntrinsicHeight());
        if (resetToFillWhenReady) {
            resetToFillWhenReady = false;
            fillImage();
            return;
        }
        float scale = fitScale * clamp(composition.zoom,
                CanvasConfig.MIN_ZOOM, CanvasConfig.MAX_ZOOM);
        float tx = getWidth() * 0.5f
                - composition.focusX * drawable.getIntrinsicWidth() * scale;
        float ty = getHeight() * 0.5f
                - composition.focusY * drawable.getIntrinsicHeight() * scale;
        compositionMatrix.reset();
        compositionMatrix.postScale(scale, scale);
        compositionMatrix.postTranslate(tx, ty);
        constrainMatrix();
        updateCompositionFromMatrix();
        applyMatrix();
    }

    private void setCenteredZoom(float zoom) {
        Drawable drawable = getDrawable();
        if (!isReady(drawable)) {
            return;
        }
        composition.focusX = 0.5f;
        composition.focusY = 0.5f;
        composition.zoom = clamp(zoom, CanvasConfig.MIN_ZOOM, CanvasConfig.MAX_ZOOM);
        applyStoredComposition();
    }

    private void constrainMatrix() {
        Drawable drawable = getDrawable();
        if (!isReady(drawable)) {
            return;
        }
        RectF bounds = new RectF(0f, 0f,
                drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        compositionMatrix.mapRect(bounds);
        float dx = 0f;
        float dy = 0f;
        if (bounds.width() <= getWidth()) {
            dx = getWidth() * 0.5f - bounds.centerX();
        } else if (bounds.left > 0f) {
            dx = -bounds.left;
        } else if (bounds.right < getWidth()) {
            dx = getWidth() - bounds.right;
        }
        if (bounds.height() <= getHeight()) {
            dy = getHeight() * 0.5f - bounds.centerY();
        } else if (bounds.top > 0f) {
            dy = -bounds.top;
        } else if (bounds.bottom < getHeight()) {
            dy = getHeight() - bounds.bottom;
        }
        compositionMatrix.postTranslate(dx, dy);
    }

    private void updateCompositionFromMatrix() {
        Drawable drawable = getDrawable();
        if (!isReady(drawable) || fitScale <= 0f) {
            return;
        }
        Matrix inverse = new Matrix();
        if (!compositionMatrix.invert(inverse)) {
            return;
        }
        float[] center = {getWidth() * 0.5f, getHeight() * 0.5f};
        inverse.mapPoints(center);
        composition.focusX = clamp(center[0] / drawable.getIntrinsicWidth(), 0f, 1f);
        composition.focusY = clamp(center[1] / drawable.getIntrinsicHeight(), 0f, 1f);
        composition.zoom = clamp(currentScale() / fitScale,
                CanvasConfig.MIN_ZOOM, CanvasConfig.MAX_ZOOM);
    }

    private float currentScale() {
        compositionMatrix.getValues(matrixValues);
        return Math.max(0.0001f, Math.abs(matrixValues[Matrix.MSCALE_X]));
    }

    private void applyMatrix() {
        setImageMatrix(compositionMatrix);
        invalidate();
    }

    private boolean isReady(Drawable drawable) {
        return drawable != null && getWidth() > 0 && getHeight() > 0
                && drawable.getIntrinsicWidth() > 0 && drawable.getIntrinsicHeight() > 0;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
