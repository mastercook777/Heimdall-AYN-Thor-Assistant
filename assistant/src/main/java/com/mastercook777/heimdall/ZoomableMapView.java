package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

final class ZoomableMapView extends ImageView {

    private final Matrix imageMatrix = new Matrix();
    private final float[] matrixValues = new float[9];
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private float minimumScale = 1f;

    ZoomableMapView(Context context) {
        super(context);
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float current = currentScale();
                float factor = detector.getScaleFactor();
                float target = clamp(current * factor, minimumScale, minimumScale * 6f);
                imageMatrix.postScale(target / current, target / current,
                        detector.getFocusX(), detector.getFocusY());
                constrainMatrix();
                applyMatrix();
                return true;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent event) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent first, MotionEvent current, float distanceX, float distanceY) {
                if (scaleDetector.isInProgress()) {
                    return false;
                }
                imageMatrix.postTranslate(-distanceX, -distanceY);
                constrainMatrix();
                applyMatrix();
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent event) {
                resetZoom();
                return true;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        DebugPerformanceDiagnostics.countDraw("Map surface");
        super.onDraw(canvas);
    }

    @Override
    public void invalidate() {
        DebugPerformanceDiagnostics.countInvalidate("Map surface");
        super.invalidate();
    }

    @Override
    public void requestLayout() {
        DebugPerformanceDiagnostics.countRequestLayout("Map surface");
        super.requestLayout();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        resetZoom();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        post(this::resetZoom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            constrainMatrix();
            applyMatrix();
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }

    void resetZoom() {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() <= 0 || getHeight() <= 0
                || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            return;
        }
        float scaleX = getWidth() / (float) drawable.getIntrinsicWidth();
        float scaleY = getHeight() / (float) drawable.getIntrinsicHeight();
        minimumScale = Math.min(scaleX, scaleY);
        float dx = (getWidth() - drawable.getIntrinsicWidth() * minimumScale) * 0.5f;
        float dy = (getHeight() - drawable.getIntrinsicHeight() * minimumScale) * 0.5f;
        imageMatrix.reset();
        imageMatrix.postScale(minimumScale, minimumScale);
        imageMatrix.postTranslate(dx, dy);
        applyMatrix();
    }

    private void constrainMatrix() {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }
        RectF bounds = new RectF(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        imageMatrix.mapRect(bounds);
        float dx = 0f;
        float dy = 0f;
        if (bounds.width() <= getWidth()) {
            dx = getWidth() * 0.5f - bounds.centerX();
        } else if (bounds.left > 0) {
            dx = -bounds.left;
        } else if (bounds.right < getWidth()) {
            dx = getWidth() - bounds.right;
        }
        if (bounds.height() <= getHeight()) {
            dy = getHeight() * 0.5f - bounds.centerY();
        } else if (bounds.top > 0) {
            dy = -bounds.top;
        } else if (bounds.bottom < getHeight()) {
            dy = getHeight() - bounds.bottom;
        }
        imageMatrix.postTranslate(dx, dy);
    }

    private float currentScale() {
        imageMatrix.getValues(matrixValues);
        return Math.max(0.0001f, matrixValues[Matrix.MSCALE_X]);
    }

    private void applyMatrix() {
        setImageMatrix(imageMatrix);
        invalidate();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
