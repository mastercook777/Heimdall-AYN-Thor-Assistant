package com.mastercook777.heimdall;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.animation.PathInterpolator;
import android.widget.Button;

final class DockNavButton extends Button {
    private static final PathInterpolator PRESS_EASE_OUT =
            new PathInterpolator(0.2f, 0f, 0f, 1f);
    private static final long PRESS_IN_MS = 70L;
    private static final long PRESS_OUT_MS = 110L;

    private final Paint navLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Drawable navIcon;
    private boolean navSelected;
    private boolean navPressedVisual;
    private int navIconSize = dp(22);
    private int navScreen = -1;
    private int navIconRes;

    DockNavButton(Context context) {
        super(context);
        setMinHeight(0);
        setMinWidth(0);
        setIncludeFontPadding(false);
        setGravity(Gravity.CENTER);
        setPadding(0, 0, 0, 0);
    }

    void setNavDestination(int screen, int iconRes) {
        navScreen = screen;
        navIconRes = iconRes;
    }

    int navScreen() {
        return navScreen;
    }

    int navIconRes() {
        return navIconRes;
    }

    void setNavIcon(int iconRes, boolean selected) {
        navSelected = selected;
        navIconSize = dp(selected ? 24 : 22);
        Drawable icon = getContext().getDrawable(iconRes);
        if (icon == null) {
            navIcon = null;
            return;
        }
        icon = icon.mutate();
        icon.setTint(HeimdallUi.isPearl(getContext())
                ? (selected ? 0xFF2D3C4E : 0xCC657386)
                : (selected ? 0xFFFFFFFF : 0xD0D8E6F2));
        navIcon = icon;
        invalidate();
    }

    private void setNavPressedVisual(boolean pressed) {
        if (navPressedVisual == pressed) {
            return;
        }
        navPressedVisual = pressed;
        animate().cancel();
        if (shouldAnimateUi()) {
            animate()
                    .scaleX(pressed ? 0.96f : 1f)
                    .scaleY(pressed ? 0.96f : 1f)
                    .setDuration(pressed ? PRESS_IN_MS : PRESS_OUT_MS)
                    .setInterpolator(PRESS_EASE_OUT)
                    .start();
        } else {
            setScaleX(pressed ? 0.96f : 1f);
            setScaleY(pressed ? 0.96f : 1f);
        }
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            setNavPressedVisual(true);
        } else if (action == MotionEvent.ACTION_MOVE) {
            boolean inside = event.getX() >= 0f && event.getY() >= 0f
                    && event.getX() <= getWidth() && event.getY() <= getHeight();
            setNavPressedVisual(inside);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            setNavPressedVisual(false);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        DebugPerformanceDiagnostics.countDraw("Bottom Dock item");
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        navLabelPaint.setColor(navPressedVisual
                ? HeimdallUi.accent(getContext())
                : getCurrentTextColor());
        navLabelPaint.setTextSize(getTextSize());
        navLabelPaint.setTypeface(navSelected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        navLabelPaint.setTextAlign(Paint.Align.LEFT);
        navLabelPaint.setAlpha(isEnabled() ? 255 : 140);
        String label = fitTextToWidth(String.valueOf(getText()), navLabelPaint,
                Math.max(1, width - dp(34)));
        float textWidth = navLabelPaint.measureText(label);
        int gap = dp(3);
        float groupWidth = (navIcon == null ? 0 : navIconSize + gap) + textWidth;
        float start = Math.max(dp(2), (width - groupWidth) / 2f);
        int iconTop = Math.round((height - navIconSize) / 2f - dp(1));
        if (navIcon != null) {
            navIcon.setTint(HeimdallUi.isPearl(getContext())
                    ? (navPressedVisual ? 0xFFF08A2A
                            : (navSelected ? 0xFF2D3C4E : 0xCC657386))
                    : (navPressedVisual ? HeimdallUi.accent(getContext())
                            : (navSelected ? 0xFFFFFFFF : 0xD0D8E6F2)));
            int iconLeft = Math.round(start);
            navIcon.setBounds(iconLeft, iconTop, iconLeft + navIconSize, iconTop + navIconSize);
            navIcon.draw(canvas);
            start += navIconSize + gap;
        }

        Paint.FontMetrics metrics = navLabelPaint.getFontMetrics();
        float baseline = height / 2f - (metrics.ascent + metrics.descent) / 2f - dp(1);
        canvas.drawText(label, start, baseline, navLabelPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        animate().cancel();
        navPressedVisual = false;
        setScaleX(1f);
        setScaleY(1f);
        super.onDetachedFromWindow();
    }

    private boolean shouldAnimateUi() {
        return !DebugPerformanceDiagnostics.isFlatUi() && ValueAnimator.areAnimatorsEnabled();
    }

    private static String fitTextToWidth(String text, Paint paint, int maxWidth) {
        if (text == null || text.isEmpty() || paint.measureText(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        String ellipsis = "...";
        int end = text.length();
        while (end > 1
                && paint.measureText(text, 0, end) + paint.measureText(ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
