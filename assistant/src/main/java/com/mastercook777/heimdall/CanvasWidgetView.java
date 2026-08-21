package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.TextView;

@SuppressLint({"ViewConstructor", "ClickableViewAccessibility"})
final class CanvasWidgetView extends FrameLayout {
    interface Listener {
        void onChooseImage(WidgetLayout.Item item, int frameWidth, int frameHeight);
        void onFullscreen(WidgetLayout.Item item, int frameWidth, int frameHeight);
        void onOptions(WidgetLayout.Item item, int frameWidth, int frameHeight);
        void onDraftInteractionBlocked();
    }

    private enum State {
        EMPTY,
        LOADING,
        READY,
        MISSING,
        ERROR
    }

    private final WidgetLayout.Item item;
    private final Listener listener;
    private final boolean interactionEnabled;
    private final boolean circular;
    private final FrameLayout displayFrame;
    private final FrameLayout viewport;
    private final CanvasImageView imageView;
    private final TextView statusView;
    private final GestureDetector gestureDetector;
    private final Runnable clearPressedEdge = this::clearPressedEdge;
    private final Runnable triggerOptionsLongPress = this::triggerOptionsLongPress;
    private final int longPressTouchSlop;
    private CanvasImageLoader.Request loadRequest;
    private Bitmap bitmap;
    private State state = State.EMPTY;
    private boolean optionsLongPressPending;
    private boolean optionsLongPressTriggered;

    CanvasWidgetView(Context context, WidgetLayout.Item item, boolean interactionEnabled,
            Listener listener) {
        super(context);
        this.item = item;
        this.listener = listener;
        this.interactionEnabled = interactionEnabled;
        CanvasConfig config = item.canvasConfig == null
                ? new CanvasConfig() : item.canvasConfig;
        circular = config.isCircular();
        setClickable(true);
        setLongClickable(false);
        setFocusable(true);
        longPressTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        displayFrame = new FrameLayout(context);
        displayFrame.setBackground(HeimdallUi.isPearl(context)
                ? HeimdallUi.cncInputFrame(context, HeimdallUi.RADIUS_MODULE, circular)
                : circular
                        ? HeimdallUi.glassCircle(context, 0xB20C131D, 0xD2070B11,
                                0x555F7C9A, 0x33344150, HeimdallUi.STROKE_HAIRLINE)
                        : HeimdallUi.glass(context, 0xB20C131D, 0xD2070B11,
                                0x555F7C9A, 0x33344150, HeimdallUi.RADIUS_MODULE,
                                HeimdallUi.STROKE_HAIRLINE));
        addView(displayFrame, new LayoutParams(-1, -1));

        viewport = new FrameLayout(context);
        viewport.setBackground(new ColorDrawable(0xFF020407));
        viewport.setClipToOutline(true);
        viewport.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (circular) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    return;
                }
                int insetDp = HeimdallUi.isPearl(getContext()) ? 6 : 1;
                float radius = HeimdallUi.isPearl(getContext())
                        ? HeimdallUi.concentricInnerRadiusDp(
                                HeimdallUi.RADIUS_MODULE, insetDp)
                        : Math.max(0f, HeimdallUi.RADIUS_MODULE - insetDp);
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(radius));
            }
        });
        int inset = dp(HeimdallUi.isPearl(context) ? 6 : 1);
        LayoutParams viewportParams = new LayoutParams(-1, -1);
        viewportParams.setMargins(inset, inset, inset, inset);
        displayFrame.addView(viewport, viewportParams);

        imageView = new CanvasImageView(context);
        imageView.setInteractive(false);
        imageView.setVisibility(GONE);
        viewport.addView(imageView, new LayoutParams(-1, -1));

        statusView = new TextView(context);
        statusView.setGravity(Gravity.CENTER);
        statusView.setTextSize(12);
        statusView.setTextColor(HeimdallUi.mutedTextColor(context));
        statusView.setPadding(dp(8), dp(6), dp(8), dp(6));
        statusView.setCompoundDrawablePadding(dp(5));
        viewport.addView(statusView, new LayoutParams(-1, -1));

        gestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent event) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent event) {
                        if (state == State.EMPTY || state == State.MISSING || state == State.ERROR) {
                            performClick();
                        }
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent event) {
                        if (state == State.READY && interactionEnabled) {
                            listener.onFullscreen(item, getWidth(), getHeight());
                            return true;
                        }
                        return false;
                    }
                });
        gestureDetector.setIsLongpressEnabled(false);
        setOnClickListener(view -> {
            if (!interactionEnabled) {
                listener.onDraftInteractionBlocked();
                return;
            }
            if (state == State.EMPTY || state == State.MISSING || state == State.ERROR) {
                listener.onChooseImage(item, getWidth(), getHeight());
            }
        });
        bind();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            cancelOptionsLongPress();
            optionsLongPressTriggered = false;
            removeCallbacks(clearPressedEdge);
            showPressedEdge();
            gestureDetector.onTouchEvent(event);
            if (state == State.READY && interactionEnabled && event.getPointerCount() == 1) {
                optionsLongPressPending = true;
                postDelayed(triggerOptionsLongPress,
                        MacroButtonView.EDIT_LONG_PRESS_TIMEOUT_MS);
            }
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            cancelOptionsLongPress();
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() != 1 || !isInsideLongPressBounds(event)) {
                cancelOptionsLongPress();
            }
            if (optionsLongPressTriggered) {
                return true;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            boolean longPressTriggered = optionsLongPressTriggered;
            cancelOptionsLongPress();
            removeCallbacks(clearPressedEdge);
            postDelayed(clearPressedEdge, 100L);
            if (longPressTriggered) {
                optionsLongPressTriggered = false;
                return true;
            }
        }
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (circular) {
            int diameter = Math.min(getMeasuredWidth(), getMeasuredHeight());
            int exactDiameter = MeasureSpec.makeMeasureSpec(diameter, MeasureSpec.EXACTLY);
            displayFrame.measure(exactDiameter, exactDiameter);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        if (circular) {
            int frameWidth = displayFrame.getMeasuredWidth();
            int frameHeight = displayFrame.getMeasuredHeight();
            int frameLeft = (width - frameWidth) / 2;
            int frameTop = (height - frameHeight) / 2;
            displayFrame.layout(frameLeft, frameTop,
                    frameLeft + frameWidth, frameTop + frameHeight);
        } else {
            displayFrame.layout(0, 0, width, height);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        viewport.invalidateOutline();
        updateCompactPresentation();
        if (state == State.LOADING && loadRequest == null) {
            loadImage();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }

    void release() {
        removeCallbacks(clearPressedEdge);
        cancelOptionsLongPress();
        optionsLongPressTriggered = false;
        clearPressedEdge();
        if (loadRequest != null) {
            loadRequest.cancel();
            loadRequest = null;
        }
        imageView.setImageDrawable(null);
        CanvasImageLoader.recycle(bitmap);
        bitmap = null;
    }

    private void bind() {
        CanvasConfig config = item.canvasConfig == null
                ? new CanvasConfig() : item.canvasConfig;
        if (!config.hasAsset()) {
            showState(State.EMPTY);
            return;
        }
        showState(State.LOADING);
        post(this::loadImage);
    }

    private void loadImage() {
        CanvasConfig config = item.canvasConfig == null
                ? new CanvasConfig() : item.canvasConfig;
        if (!config.hasAsset() || loadRequest != null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        int maxSide = CanvasImageLoader.runtimeDecodeMaxSide(
                getWidth(), getHeight(), config.zoom);
        loadRequest = CanvasImageLoader.load(getContext(), config.assetId, maxSide,
                new CanvasImageLoader.Callback() {
                    @Override
                    public void onLoaded(Bitmap loaded) {
                        loadRequest = null;
                        CanvasImageLoader.recycle(bitmap);
                        bitmap = loaded;
                        imageView.setImageBitmap(bitmap);
                        imageView.setComposition(config, false);
                        showState(State.READY);
                    }

                    @Override
                    public void onError(CanvasImageLoader.Error error) {
                        loadRequest = null;
                        showState(error == CanvasImageLoader.Error.MISSING
                                ? State.MISSING : State.ERROR);
                    }
                });
    }

    private void showState(State next) {
        state = next;
        boolean ready = next == State.READY;
        imageView.setVisibility(ready ? VISIBLE : GONE);
        statusView.setVisibility(ready ? GONE : VISIBLE);
        statusView.setCompoundDrawablesWithIntrinsicBounds(0,
                next == State.LOADING ? 0 : R.drawable.ic_add, 0, 0);
        statusView.setTextColor(next == State.ERROR || next == State.MISSING
                ? HeimdallUi.COLOR_DANGER : HeimdallUi.mutedTextColor(getContext()));
        setContentDescription(getResources().getString(ready
                ? R.string.canvas_ready_content_description
                : R.string.canvas_recovery_content_description));
        updateCompactPresentation();
    }

    private void updateCompactPresentation() {
        if (statusView == null || state == State.READY) {
            return;
        }
        boolean compact = getWidth() > 0 && getHeight() > 0
                && (getWidth() < dp(112) || getHeight() < dp(72));
        if (compact) {
            statusView.setText("");
            return;
        }
        if (!interactionEnabled) {
            statusView.setText(R.string.canvas_save_layout_first);
        } else if (state == State.EMPTY) {
            statusView.setText(R.string.canvas_add_image);
        } else if (state == State.LOADING) {
            statusView.setText(R.string.canvas_loading);
        } else if (state == State.MISSING) {
            statusView.setText(R.string.canvas_image_missing);
        } else {
            statusView.setText(R.string.canvas_decode_error);
        }
    }

    private void showPressedEdge() {
        int color = HeimdallUi.isPearl(getContext()) ? 0xC8F08A2A : 0xAA70B7FF;
        if (circular) {
            GradientDrawable edge = new GradientDrawable();
            edge.setShape(GradientDrawable.OVAL);
            edge.setColor(0x00000000);
            edge.setStroke(dp(2), color);
            displayFrame.setForeground(edge);
        } else {
            displayFrame.setForeground(HeimdallUi.rounded(getContext(), 0x00000000, color,
                    HeimdallUi.RADIUS_MODULE, 2));
        }
    }

    private void clearPressedEdge() {
        displayFrame.setForeground(null);
    }

    private void triggerOptionsLongPress() {
        if (!optionsLongPressPending || state != State.READY || !interactionEnabled
                || !isAttachedToWindow() || !isShown() || !isEnabled()) {
            cancelOptionsLongPress();
            return;
        }
        optionsLongPressPending = false;
        optionsLongPressTriggered = true;
        clearPressedEdge();
        cancelGestureDetector();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        listener.onOptions(item, getWidth(), getHeight());
    }

    private void cancelOptionsLongPress() {
        if (optionsLongPressPending) {
            removeCallbacks(triggerOptionsLongPress);
            optionsLongPressPending = false;
        }
    }

    private boolean isInsideLongPressBounds(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        return x >= -longPressTouchSlop
                && y >= -longPressTouchSlop
                && x <= getWidth() + longPressTouchSlop
                && y <= getHeight() + longPressTouchSlop;
    }

    private void cancelGestureDetector() {
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now,
                MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
        gestureDetector.onTouchEvent(cancel);
        cancel.recycle();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
