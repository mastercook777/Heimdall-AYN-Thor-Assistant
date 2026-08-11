package com.mastercook777.heimdall;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.PathInterpolator;
import android.widget.Button;

final class MacroButtonView extends Button {
    private static final PathInterpolator PRESS_EASE_OUT =
            new PathInterpolator(0.2f, 0f, 0f, 1f);
    private static final long PRESS_IN_MS = 70L;
    private static final long PRESS_OUT_MS = 110L;
    static final long EDIT_LONG_PRESS_TIMEOUT_MS = 1800L;

    private final Paint macroLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint macroPressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String macroLabel = "";
    private boolean macroLabelVisible = true;
    private Drawable macroIcon;
    private boolean macroIconTintable = true;
    private boolean macroPressedVisual;
    private final int macroLongPressTouchSlop;
    private final Runnable triggerMacroLongPress = this::triggerMacroLongPress;
    private Runnable macroLongPressAction;
    private boolean macroLongPressPending;
    private boolean macroLongPressTriggered;
    private int macroIconColor = 0xFFE6EDF3;
    private int macroIconSize = dp(32);
    private String macroIconKey = "";

    MacroButtonView(Context context) {
        super(context);
        macroLongPressTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setLongClickable(false);
    }

    @Override
    public void invalidate() {
        DebugPerformanceDiagnostics.countInvalidate("Macro button");
        super.invalidate();
    }

    @Override
    public void requestLayout() {
        DebugPerformanceDiagnostics.countRequestLayout("Macro button");
        super.requestLayout();
    }

    void setMacroLabel(String label) {
        macroLabel = label == null ? "" : label;
        setContentDescription(macroLabel);
        invalidate();
    }

    void setMacroLabelVisible(boolean visible) {
        if (macroLabelVisible == visible) {
            return;
        }
        macroLabelVisible = visible;
        invalidate();
    }

    void setMacroIcon(MacroIconRepository.MacroIconOption option, int color, int sizePx) {
        String nextKey = option == null ? "" : option.key;
        if (nextKey.equals(macroIconKey)
                && macroIconColor == color && macroIconSize == sizePx) {
            return;
        }
        macroIconKey = nextKey;
        macroIconColor = color;
        macroIconSize = sizePx;
        macroIconTintable = option == null || option.tintable;
        Drawable drawable = option == null ? null : option.load(getContext());
        macroIcon = drawable == null ? null : drawable.mutate();
        invalidate();
    }

    void setMacroPressedVisual(boolean pressed) {
        if (macroPressedVisual == pressed) {
            return;
        }
        macroPressedVisual = pressed;
        animate().cancel();
        if (shouldAnimateUi()) {
            animate()
                    .scaleX(pressed ? 0.98f : 1f)
                    .scaleY(pressed ? 0.98f : 1f)
                    .setDuration(pressed ? PRESS_IN_MS : PRESS_OUT_MS)
                    .setInterpolator(PRESS_EASE_OUT)
                    .start();
        } else {
            setScaleX(pressed ? 0.98f : 1f);
            setScaleY(pressed ? 0.98f : 1f);
        }
        invalidate();
    }

    void setMacroLongPressAction(Runnable action) {
        cancelPendingMacroLongPress();
        macroLongPressAction = action;
    }

    void cancelPendingMacroLongPress() {
        if (macroLongPressPending) {
            removeCallbacks(triggerMacroLongPress);
            macroLongPressPending = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelPendingMacroLongPress();
        macroLongPressTriggered = false;
        animate().cancel();
        macroPressedVisual = false;
        setScaleX(1f);
        setScaleY(1f);
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            cancelPendingMacroLongPress();
            macroLongPressTriggered = false;
            setMacroPressedVisual(true);
            boolean handled = super.onTouchEvent(event);
            if (handled && macroLongPressAction != null && event.getPointerCount() == 1) {
                macroLongPressPending = true;
                postDelayed(triggerMacroLongPress, EDIT_LONG_PRESS_TIMEOUT_MS);
            }
            return handled;
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            cancelPendingMacroLongPress();
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() != 1 || !isInsideLongPressBounds(event)) {
                cancelPendingMacroLongPress();
            }
            if (macroLongPressTriggered) {
                return true;
            }
            boolean inside = event.getX() >= 0
                    && event.getY() >= 0
                    && event.getX() <= getWidth()
                    && event.getY() <= getHeight();
            setMacroPressedVisual(inside);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            boolean longPressTriggered = macroLongPressTriggered;
            cancelPendingMacroLongPress();
            setMacroPressedVisual(false);
            if (longPressTriggered) {
                macroLongPressTriggered = false;
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private void triggerMacroLongPress() {
        if (!macroLongPressPending || macroLongPressAction == null
                || !isAttachedToWindow() || !isShown() || !isEnabled()) {
            cancelPendingMacroLongPress();
            return;
        }
        macroLongPressPending = false;
        macroLongPressTriggered = true;
        cancelLongPress();
        setMacroPressedVisual(false);
        cancelSuperTouchGesture();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        macroLongPressAction.run();
    }

    private boolean isInsideLongPressBounds(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        return x >= -macroLongPressTouchSlop
                && y >= -macroLongPressTouchSlop
                && x <= getWidth() + macroLongPressTouchSlop
                && y <= getHeight() + macroLongPressTouchSlop;
    }

    private void cancelSuperTouchGesture() {
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
        super.onTouchEvent(cancel);
        cancel.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        DebugPerformanceDiagnostics.countDraw("Macro button");
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        float contentYOffset = macroPressedVisual ? dp(1) : 0f;
        if (macroPressedVisual && !DebugPerformanceDiagnostics.isFlatUi()) {
            macroPressPaint.setStyle(Paint.Style.STROKE);
            RectF outerGlow = new RectF(dp(1), dp(1), width - dp(1), height - dp(1));
            macroPressPaint.setStrokeWidth(dp(3) / 2f);
            macroPressPaint.setColor(HeimdallUi.isPearl(getContext())
                    ? 0xC8F08A2A : 0xEE70B7FF);
            canvas.drawRoundRect(outerGlow, dp(10), dp(10), macroPressPaint);

            float innerInset = dp(5) / 2f;
            RectF innerGlow = new RectF(innerInset, innerInset,
                    width - innerInset, height - innerInset);
            macroPressPaint.setStrokeWidth(dp(9) / 2f);
            macroPressPaint.setColor(HeimdallUi.isPearl(getContext())
                    ? 0x38F08A2A : 0x554EA1FF);
            canvas.drawRoundRect(innerGlow, dp(17) / 2f, dp(17) / 2f,
                    macroPressPaint);
        }

        float baseline = 0f;
        float labelTop = height;
        if (macroLabelVisible) {
            macroLabelPaint.setColor(getCurrentTextColor());
            macroLabelPaint.setTextSize(getTextSize());
            macroLabelPaint.setTypeface(getTypeface());
            macroLabelPaint.setTextAlign(Paint.Align.CENTER);
            macroLabelPaint.setAlpha(isEnabled() ? 255 : 140);
            Paint.FontMetrics metrics = macroLabelPaint.getFontMetrics();
            float labelCenter = height * 0.76f;
            baseline = labelCenter - (metrics.ascent + metrics.descent) / 2f
                    + contentYOffset;
            labelTop = baseline + metrics.ascent;
        }

        if (macroIcon != null) {
            int availableIconHeight = macroLabelVisible
                    ? Math.max(dp(18), Math.round(labelTop
                            - dp(HeimdallUi.MACRO_ICON_LABEL_GAP) - dp(8)))
                    : Math.max(dp(18), height - dp(16));
            int size = Math.min(macroIconSize, Math.min(width - dp(18), availableIconHeight));
            int intrinsicW = Math.max(1, macroIcon.getIntrinsicWidth());
            int intrinsicH = Math.max(1, macroIcon.getIntrinsicHeight());
            float scale = Math.min(size / (float) intrinsicW, size / (float) intrinsicH);
            int drawW = Math.max(1, Math.round(intrinsicW * scale));
            int drawH = Math.max(1, Math.round(intrinsicH * scale));
            int left = (width - drawW) / 2;
            int top = macroLabelVisible
                    ? Math.max(dp(8), Math.round(labelTop
                            - dp(HeimdallUi.MACRO_ICON_LABEL_GAP) - drawH))
                    : (height - drawH) / 2;
            top += Math.round(contentYOffset);
            if (macroIconTintable) {
                macroIcon.setTint(macroIconColor);
            } else {
                macroIcon.clearColorFilter();
            }
            macroIcon.setBounds(left, top, left + drawW, top + drawH);
            macroIcon.draw(canvas);
        }

        if (macroLabelVisible) {
            String label = fitTextToWidth(macroLabel, macroLabelPaint, width - dp(18));
            canvas.drawText(label, width / 2f, baseline, macroLabelPaint);
        }
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
