package com.mastercook777.heimdall;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Compact three-lamp status for the constrained Interactive Map browser. */
final class InteractiveMapLoadIndicator extends View {
    private static final int STATE_LOADING = 0;
    private static final int STATE_LOADED = 1;
    private static final int STATE_ERROR = 2;
    private static final long LOADING_STEP_MS = 260L;

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Runnable advanceLoading = new Runnable() {
        @Override
        public void run() {
            if (state != STATE_LOADING || !isAttachedToWindow()) {
                return;
            }
            illuminatedCount = illuminatedCount >= 3 ? 1 : illuminatedCount + 1;
            invalidate();
            postDelayed(this, LOADING_STEP_MS);
        }
    };

    private int state = STATE_LOADING;
    private int illuminatedCount = 1;
    private int statusDescriptionRes = R.string.map_loading;
    private boolean revealsControls;

    InteractiveMapLoadIndicator(Context context) {
        super(context);
        setFocusable(false);
        setClickable(false);
        setLoading();
    }

    void setLoading() {
        boolean stateChanged = state != STATE_LOADING;
        if (stateChanged) {
            state = STATE_LOADING;
            illuminatedCount = 1;
        }
        statusDescriptionRes = R.string.map_loading;
        updateContentDescription();
        if (stateChanged) {
            restartLoadingAnimation();
        }
        invalidate();
    }

    void setLoaded() {
        state = STATE_LOADED;
        illuminatedCount = 3;
        removeCallbacks(advanceLoading);
        statusDescriptionRes = R.string.map_loaded;
        updateContentDescription();
        invalidate();
    }

    void setError(int descriptionRes) {
        state = STATE_ERROR;
        illuminatedCount = 3;
        removeCallbacks(advanceLoading);
        statusDescriptionRes = descriptionRes;
        updateContentDescription();
        invalidate();
    }

    void setRevealControlsAction(Runnable action) {
        revealsControls = action != null;
        setOnClickListener(action == null ? null : view -> action.run());
        setClickable(revealsControls);
        updateContentDescription();
    }

    private void updateContentDescription() {
        String status = getContext().getString(statusDescriptionRes);
        setContentDescription(revealsControls
                ? getContext().getString(R.string.map_status_control_description,
                        status, getContext().getString(R.string.map_show_navigation))
                : status);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        restartLoadingAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(advanceLoading);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boolean pearl = HeimdallUi.isPearl(getContext());
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        int activeColor = state == STATE_LOADED
                ? HeimdallUi.COLOR_SUCCESS
                : state == STATE_ERROR
                        ? HeimdallUi.COLOR_DANGER
                        : HeimdallUi.accent(getContext());
        for (int index = 0; index < 3; index++) {
            float x = centerX + dp((index - 1) * 12);
            dotPaint.setColor(pearl ? 0x66717A82 : 0x88414A53);
            canvas.drawCircle(x, centerY, dp(3), dotPaint);
            dotPaint.setColor(index < illuminatedCount
                    ? activeColor
                    : HeimdallUi.mutedTextColor(getContext()));
            dotPaint.setAlpha(index < illuminatedCount ? 255 : 92);
            canvas.drawCircle(x, centerY, dp(2), dotPaint);
        }
        dotPaint.setAlpha(255);
    }

    private void restartLoadingAnimation() {
        removeCallbacks(advanceLoading);
        if (state != STATE_LOADING || !isAttachedToWindow()) {
            return;
        }
        if (!ValueAnimator.areAnimatorsEnabled()) {
            illuminatedCount = 3;
            invalidate();
            return;
        }
        postDelayed(advanceLoading, LOADING_STEP_MS);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
