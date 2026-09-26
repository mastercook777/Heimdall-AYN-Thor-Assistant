package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class HardwareMonitorProgressView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private float fraction;
    private int fillColor;

    HardwareMonitorProgressView(Context context) {
        super(context);
        fillColor = HeimdallUi.accent(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setProgress(int percent, boolean warning) {
        float nextFraction = Math.max(0f, Math.min(1f, percent / 100f));
        int nextColor = warning
                ? HeimdallUi.semanticColor(getContext(), HeimdallUi.SEMANTIC_WARNING)
                : HeimdallUi.accent(getContext());
        if (Math.abs(fraction - nextFraction) < 0.0001f && fillColor == nextColor) return;
        fraction = nextFraction;
        fillColor = nextColor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = getHeight() / 2f;
        bounds.set(0f, 0f, getWidth(), getHeight());
        paint.setColor(HeimdallUi.componentColors(getContext()).sliderTrack);
        canvas.drawRoundRect(bounds, radius, radius, paint);
        if (fraction <= 0f) return;
        bounds.right = Math.max(getHeight(), getWidth() * fraction);
        paint.setColor(fillColor);
        canvas.drawRoundRect(bounds, radius, radius, paint);
    }
}
