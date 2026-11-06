package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
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
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

@SuppressLint({"ViewConstructor", "ClickableViewAccessibility"})
final class CanvasWidgetView extends FrameLayout {
    private static final long SLIDE_PREFETCH_LEAD_MS = 1500L;
    interface Listener {
        void onChooseImage(WidgetLayout.Item item, int frameWidth, int frameHeight);
        void onFullscreen(WidgetLayout.Item item, int slideIndex,
                int frameWidth, int frameHeight);
        void onOptions(WidgetLayout.Item item, int frameWidth, int frameHeight);
        void onDraftInteractionBlocked();
    }

    private enum State {
        EMPTY,
        LOADING,
        READY,
        MISSING,
        PLATFORM_UNSUPPORTED,
        ERROR
    }

    private final WidgetLayout.Item item;
    private final Listener listener;
    private final boolean interactionEnabled;
    private final boolean circular;
    private final FrameLayout displayFrame;
    private final FrameLayout viewport;
    private CanvasImageView imageView;
    private CanvasImageView standbyImageView;
    private final CanvasVideoView videoView;
    private final TextView statusView;
    private final GestureDetector gestureDetector;
    private final Runnable clearPressedEdge = this::clearPressedEdge;
    private final Runnable triggerOptionsLongPress = this::triggerOptionsLongPress;
    private final Runnable advanceSlide = this::advanceSlide;
    private final Runnable prepareSlide = this::prepareSlide;
    private final int longPressTouchSlop;
    private CanvasImageLoader.Request loadRequest;
    private CanvasImageLoader.DecodedImage decodedImage;
    private CanvasImageLoader.DecodedImage pendingImage;
    private CanvasImageLoader.DecodedImage readyImage;
    private CanvasConfig readyConfig;
    private State state = State.EMPTY;
    private boolean optionsLongPressPending;
    private boolean optionsLongPressTriggered;
    private boolean playbackAllowed;
    private boolean aggregatedVisible;
    private boolean videoBound;
    private int currentSlideIndex;
    private int loadGeneration;
    private int pendingSlideIndex;
    private int readySlideIndex;
    private long nextSwitchUptimeMs;
    private boolean transitioning;
    private boolean released;
    private boolean clockJoined;

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
        displayFrame.setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncInputFrame(context, HeimdallUi.RADIUS_MODULE, circular)
                : circular
                        ? HeimdallUi.glassCircleSurface(context,
                                ThemeGlassColors.MEDIA_FRAME, HeimdallUi.STROKE_HAIRLINE)
                        : HeimdallUi.glassSurface(context, ThemeGlassColors.MEDIA_FRAME,
                                HeimdallUi.RADIUS_MODULE, HeimdallUi.STROKE_HAIRLINE));
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
                float radius = HeimdallUi.mediaFrameInnerRadiusDp(
                        getContext(), HeimdallUi.RADIUS_MODULE);
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(radius));
            }
        });
        int inset = dp(HeimdallUi.mediaFrameContentInsetDp(context));
        LayoutParams viewportParams = new LayoutParams(-1, -1);
        viewportParams.setMargins(inset, inset, inset, inset);
        displayFrame.addView(viewport, viewportParams);

        imageView = new CanvasImageView(context);
        imageView.setInteractive(false);
        imageView.setVisibility(GONE);
        viewport.addView(imageView, new LayoutParams(-1, -1));

        standbyImageView = new CanvasImageView(context);
        standbyImageView.setInteractive(false);
        standbyImageView.setVisibility(GONE);
        viewport.addView(standbyImageView, new LayoutParams(-1, -1));

        videoView = new CanvasVideoView(context);
        videoView.setInteractive(false);
        videoView.setVisibility(GONE);
        viewport.addView(videoView, new LayoutParams(-1, -1));

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
                        if (state == State.EMPTY || state == State.MISSING
                                || state == State.PLATFORM_UNSUPPORTED
                                || state == State.ERROR) {
                            performClick();
                        }
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent event) {
                        if (state == State.READY && interactionEnabled) {
                            finishTransition();
                            listener.onFullscreen(item, currentSlideIndex,
                                    getWidth(), getHeight());
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
            if (state == State.EMPTY || state == State.MISSING
                    || state == State.PLATFORM_UNSUPPORTED || state == State.ERROR) {
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
            if ((state == State.READY || state == State.EMPTY
                    || hasSlideshowToRepair())
                    && interactionEnabled && event.getPointerCount() == 1) {
                optionsLongPressPending = true;
                postDelayed(triggerOptionsLongPress,
                        HeimdallInteraction.EDIT_LONG_PRESS_TIMEOUT_MS);
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
        finishTransition();
        viewport.invalidateOutline();
        updateCompactPresentation();
        if (state == State.LOADING && loadRequest == null && !videoBound) {
            loadMedia();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        aggregatedVisible = isVisible;
        updatePlayback();
    }

    void setPlaybackAllowed(boolean allowed) {
        playbackAllowed = allowed;
        updatePlayback();
    }

    int currentSlideIndex() { return currentSlideIndex; }

    String itemId() { return item.itemId; }

    void release() {
        released = true;
        if (clockJoined) {
            CanvasSlideshowClock.leave();
            clockJoined = false;
        }
        loadGeneration++;
        removeCallbacks(advanceSlide);
        removeCallbacks(prepareSlide);
        removeCallbacks(clearPressedEdge);
        cancelOptionsLongPress();
        optionsLongPressTriggered = false;
        clearPressedEdge();
        if (loadRequest != null) {
            loadRequest.cancel();
            loadRequest = null;
        }
        transitioning = false;
        standbyImageView.animate().setListener(null);
        standbyImageView.animate().cancel();
        imageView.setImageDrawable(null);
        standbyImageView.setImageDrawable(null);
        videoView.release();
        videoBound = false;
        if (decodedImage != null) {
            decodedImage.release();
            decodedImage = null;
        }
        if (pendingImage != null) {
            pendingImage.release();
            pendingImage = null;
        }
        clearReadyImage();
        nextSwitchUptimeMs = 0L;
    }

    private void bind() {
        CanvasConfig config = item.canvasConfig == null
                ? new CanvasConfig() : item.canvasConfig;
        if (!config.hasAsset()) {
            showState(State.EMPTY);
            return;
        }
        showState(State.LOADING);
        post(this::loadMedia);
    }

    private void loadMedia() {
        if (released) return;
        CanvasConfig config = item.canvasConfig == null
                ? new CanvasConfig() : item.canvasConfig;
        if (config.video) {
            loadVideo(config);
            return;
        }
        if (!config.hasAsset() || loadRequest != null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        loadSlide(0, 0, 0L);
    }

    private void loadSlide(int index, int failures, long switchAtUptimeMs) {
        CanvasConfig owner = item.canvasConfig == null
                ? new CanvasConfig() : item.canvasConfig;
        CanvasConfig config = owner.slideConfig(index);
        if (released || getWidth() <= 0 || getHeight() <= 0) return;
        int generation = ++loadGeneration;
        int maxSide = CanvasImageLoader.runtimeDecodeMaxSide(
                getWidth(), getHeight(), config.zoom);
        loadRequest = CanvasImageLoader.load(getContext(), config.assetId, maxSide,
                new CanvasImageLoader.Callback() {
                    @Override
                    public void onLoaded(CanvasImageLoader.DecodedImage loaded) {
                        if (released || generation != loadGeneration) {
                            loaded.release();
                            return;
                        }
                        loadRequest = null;
                        if (switchAtUptimeMs > 0L
                                && SystemClock.uptimeMillis() < switchAtUptimeMs) {
                            readyImage = loaded;
                            readyConfig = config;
                            readySlideIndex = index;
                        } else {
                            nextSwitchUptimeMs = 0L;
                            removeCallbacks(advanceSlide);
                            showLoadedSlide(owner, config, loaded, index);
                        }
                    }

                    @Override
                    public void onError(CanvasImageLoader.Error error) {
                        if (released || generation != loadGeneration) return;
                        loadRequest = null;
                        int count = owner.isSlideshow() ? owner.slides.size() : 1;
                        if (owner.isSlideshow() && failures + 1 < count) {
                            loadSlide((index + 1) % count, failures + 1,
                                    switchAtUptimeMs);
                            return;
                        }
                        if (decodedImage != null) {
                            nextSwitchUptimeMs = 0L;
                            removeCallbacks(advanceSlide);
                            if (canRotate()) {
                                scheduleNextBoundary(Math.max(
                                        SystemClock.uptimeMillis(), switchAtUptimeMs));
                            }
                            return;
                        }
                        showState(error == CanvasImageLoader.Error.MISSING
                                ? State.MISSING
                                : error == CanvasImageLoader.Error.PLATFORM_UNSUPPORTED
                                        ? State.PLATFORM_UNSUPPORTED : State.ERROR);
                    }
                });
    }

    private void advanceSlide() {
        CanvasConfig owner = item.canvasConfig;
        if (owner == null || !owner.isSlideshow() || !canRotate()) return;
        long deadline = nextSwitchUptimeMs;
        if (readyImage != null) {
            CanvasImageLoader.DecodedImage loaded = readyImage;
            CanvasConfig config = readyConfig;
            int index = readySlideIndex;
            readyImage = null;
            readyConfig = null;
            nextSwitchUptimeMs = 0L;
            showLoadedSlide(owner, config, loaded, index);
        } else if (loadRequest == null) {
            nextSwitchUptimeMs = 0L;
            loadSlide((currentSlideIndex + 1) % owner.slides.size(), 0, deadline);
        }
    }

    private void prepareSlide() {
        CanvasConfig owner = item.canvasConfig;
        if (owner == null || !owner.isSlideshow() || !canRotate()
                || nextSwitchUptimeMs == 0L || loadRequest != null
                || readyImage != null) return;
        loadSlide((currentSlideIndex + 1) % owner.slides.size(), 0,
                nextSwitchUptimeMs);
    }

    private void scheduleNextBoundary(long afterUptimeMs) {
        CanvasConfig config = item.canvasConfig;
        if (config == null || !canRotate()) return;
        long intervalMs = config.slideSeconds * 1000L;
        nextSwitchUptimeMs = CanvasSlideshowClock.nextBoundary(
                afterUptimeMs, intervalMs);
        removeCallbacks(prepareSlide);
        removeCallbacks(advanceSlide);
        long nowUptimeMs = SystemClock.uptimeMillis();
        postDelayed(prepareSlide, Math.max(0L,
                nextSwitchUptimeMs - SLIDE_PREFETCH_LEAD_MS - nowUptimeMs));
        postDelayed(advanceSlide,
                Math.max(0L, nextSwitchUptimeMs - nowUptimeMs));
    }

    private void clearReadyImage() {
        if (readyImage != null) {
            readyImage.release();
            readyImage = null;
        }
        readyConfig = null;
    }

    private boolean canRotate() {
        CanvasConfig config = item.canvasConfig;
        return !released && config != null && config.isSlideshow() && config.autoRotate
                && playbackAllowed && aggregatedVisible && isAttachedToWindow()
                && state == State.READY && !transitioning;
    }

    private void showLoadedSlide(CanvasConfig owner, CanvasConfig config,
            CanvasImageLoader.DecodedImage loaded, int index) {
        boolean animate = decodedImage != null && owner.isSlideshow()
                && !CanvasConfig.TRANSITION_NONE.equals(owner.slideTransition)
                && playbackAllowed && aggregatedVisible && isAttachedToWindow()
                && ValueAnimator.areAnimatorsEnabled();
        if (!animate) {
            CanvasImageLoader.DecodedImage previous = decodedImage;
            decodedImage = loaded;
            setDecodedImage(imageView, loaded, config);
            currentSlideIndex = index;
            showState(State.READY);
            if (previous != null) previous.release();
            updatePlayback();
            return;
        }
        pendingImage = loaded;
        pendingSlideIndex = index;
        transitioning = true;
        setDecodedImage(standbyImageView, loaded, config);
        standbyImageView.setAlpha(CanvasConfig.TRANSITION_FADE.equals(
                owner.slideTransition) ? 0f : 1f);
        float offset = CanvasConfig.TRANSITION_FROM_LEFT.equals(owner.slideTransition)
                ? -viewport.getWidth() : CanvasConfig.TRANSITION_FROM_RIGHT.equals(
                        owner.slideTransition) ? viewport.getWidth() : 0f;
        standbyImageView.setTranslationX(offset);
        viewport.bringChildToFront(standbyImageView);
        standbyImageView.setVisibility(VISIBLE);
        standbyImageView.post(() -> {
            if (released || !transitioning || pendingImage != loaded) return;
            if (!playbackAllowed || !aggregatedVisible || !isAttachedToWindow()) {
                finishTransition();
                return;
            }
            standbyImageView.animate().alpha(1f).translationX(0f)
                    .setDuration(320L)
                    .setInterpolator(new DecelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override public void onAnimationEnd(Animator animation) {
                            if (transitioning && pendingImage == loaded) {
                                finishTransition();
                            }
                        }
                    }).start();
        });
    }

    private void setDecodedImage(CanvasImageView target,
            CanvasImageLoader.DecodedImage loaded, CanvasConfig config) {
        if (loaded.bitmap() != null) {
            target.setImageBitmap(loaded.bitmap());
        } else {
            target.setImageDrawable(loaded.drawable());
        }
        target.setComposition(config, false);
    }

    private void finishTransition() {
        if (!transitioning) return;
        transitioning = false;
        standbyImageView.animate().setListener(null);
        standbyImageView.animate().cancel();
        CanvasImageView previousView = imageView;
        imageView = standbyImageView;
        standbyImageView = previousView;
        standbyImageView.setVisibility(GONE);
        standbyImageView.setAlpha(1f);
        standbyImageView.setTranslationX(0f);
        standbyImageView.setImageDrawable(null);
        imageView.setAlpha(1f);
        imageView.setTranslationX(0f);
        CanvasImageLoader.DecodedImage previous = decodedImage;
        decodedImage = pendingImage;
        pendingImage = null;
        currentSlideIndex = pendingSlideIndex;
        if (previous != null) previous.release();
        updatePlayback();
    }

    private void loadVideo(CanvasConfig config) {
        if (!config.hasAsset() || videoBound) {
            return;
        }
        if (CanvasAssetStore.resolve(getContext(), config.assetId) == null) {
            showState(State.MISSING);
            return;
        }
        videoBound = true;
        videoView.setComposition(config, false);
        videoView.setSource(config, new CanvasVideoView.Listener() {
            @Override
            public void onReady() {
                videoView.setComposition(config, false);
                showState(State.READY);
                updatePlayback();
            }

            @Override
            public void onError() {
                showState(State.ERROR);
            }
        });
        updatePlayback();
    }

    private void showState(State next) {
        state = next;
        boolean ready = next == State.READY;
        CanvasConfig config = item.canvasConfig == null
                ? new CanvasConfig() : item.canvasConfig;
        imageView.setVisibility(ready && !config.video ? VISIBLE : GONE);
        videoView.setVisibility(config.video
                && (next == State.LOADING || ready) ? VISIBLE : GONE);
        statusView.setVisibility(ready ? GONE : VISIBLE);
        statusView.setCompoundDrawablesWithIntrinsicBounds(0,
                next == State.LOADING ? 0 : R.drawable.ic_add, 0, 0);
        statusView.setTextColor(next == State.ERROR || next == State.MISSING
                || next == State.PLATFORM_UNSUPPORTED
                ? HeimdallUi.COLOR_DANGER : HeimdallUi.mutedTextColor(getContext()));
        setContentDescription(getResources().getString(ready
                ? R.string.canvas_ready_content_description
                : R.string.canvas_recovery_content_description));
        updateCompactPresentation();
    }

    private void updatePlayback() {
        CanvasConfig config = item.canvasConfig;
        boolean clockEligible = !released && config != null && config.isSlideshow()
                && config.autoRotate && playbackAllowed && aggregatedVisible
                && isAttachedToWindow() && state == State.READY;
        if (clockEligible && !clockJoined) {
            CanvasSlideshowClock.join();
            clockJoined = true;
        } else if (!clockEligible && clockJoined) {
            CanvasSlideshowClock.leave();
            clockJoined = false;
        }
        if (!playbackAllowed || !aggregatedVisible || !isAttachedToWindow()
                || (config != null && config.isSlideshow() && !config.autoRotate)) {
            removeCallbacks(prepareSlide);
            removeCallbacks(advanceSlide);
            nextSwitchUptimeMs = 0L;
            clearReadyImage();
            if (loadRequest != null && decodedImage != null) {
                loadGeneration++;
                loadRequest.cancel();
                loadRequest = null;
            }
            if (transitioning) {
                finishTransition();
                return;
            }
        }
        if (canRotate() && loadRequest == null && readyImage == null
                && nextSwitchUptimeMs == 0L) {
            scheduleNextBoundary(SystemClock.uptimeMillis());
        }
        videoView.setPlaybackAllowed(playbackAllowed && aggregatedVisible
                && isAttachedToWindow()
                && (state == State.LOADING || state == State.READY));
        if (decodedImage == null || !decodedImage.isAnimated()) {
            return;
        }
        if (playbackAllowed && aggregatedVisible && isAttachedToWindow()
                && state == State.READY) {
            decodedImage.start();
        } else {
            decodedImage.stop();
        }
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
        } else if (state == State.PLATFORM_UNSUPPORTED) {
            statusView.setText(R.string.canvas_animation_platform_unsupported);
        } else {
            statusView.setText(R.string.canvas_decode_error);
        }
    }

    private void showPressedEdge() {
        int color = HeimdallUi.componentColors(getContext()).canvasPressedEdge;
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
        if (!optionsLongPressPending
                || (state != State.READY && state != State.EMPTY
                        && !hasSlideshowToRepair()) || !interactionEnabled
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

    private boolean hasSlideshowToRepair() {
        return item.canvasConfig != null && item.canvasConfig.isSlideshow()
                && item.canvasConfig.hasAsset();
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
