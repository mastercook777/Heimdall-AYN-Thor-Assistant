package com.mastercook777.heimdall;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

final class DockNavBar extends FrameLayout {
    private static final int TAB_COUNT = 3;
    private static final long INDICATOR_TRANSITION_MS = 160L;
    private static final PathInterpolator INDICATOR_EASE_OUT =
            new PathInterpolator(0.2f, 0f, 0f, 1f);

    private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF indicatorRect = new RectF();
    private float indicatorIndex;
    private ValueAnimator indicatorAnimator;

    DockNavBar(Context context, int initialIndex) {
        super(context);
        indicatorIndex = initialIndex;
        setWillNotDraw(false);
    }

    void setSelectedIndex(int selectedIndex, boolean animated) {
        float target = selectedIndex;
        if (indicatorAnimator != null) {
            indicatorAnimator.cancel();
            indicatorAnimator = null;
        }
        if (!animated || Math.abs(indicatorIndex - target) < 0.01f) {
            indicatorIndex = target;
            invalidate();
            return;
        }
        indicatorAnimator = ValueAnimator.ofFloat(indicatorIndex, target);
        indicatorAnimator.setDuration(INDICATOR_TRANSITION_MS);
        indicatorAnimator.setInterpolator(INDICATOR_EASE_OUT);
        indicatorAnimator.addUpdateListener(animation -> {
            indicatorIndex = (float) animation.getAnimatedValue();
            invalidate();
        });
        indicatorAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        DebugPerformanceDiagnostics.countDraw("Bottom Dock indicator");
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        float tabWidth = width / (float) TAB_COUNT;
        float indicatorWidth = Math.max(dp(42), tabWidth - dp(52));
        float indicatorHeight = dp(3);
        float centerX = tabWidth * (indicatorIndex + 0.5f);
        float bottom = height - dp(7);
        indicatorRect.set(centerX - indicatorWidth / 2f, bottom - indicatorHeight,
                centerX + indicatorWidth / 2f, bottom);
        indicatorPaint.setShader(null);
        indicatorPaint.setColor(HeimdallUi.isPearl(getContext())
                ? 0xE0F08A2A
                : 0xE04EA1FF);
        canvas.drawRoundRect(indicatorRect, indicatorHeight / 2f, indicatorHeight / 2f,
                indicatorPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (indicatorAnimator != null) {
            indicatorAnimator.cancel();
            indicatorAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
