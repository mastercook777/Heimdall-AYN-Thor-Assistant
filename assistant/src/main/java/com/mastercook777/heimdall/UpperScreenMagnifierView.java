package com.mastercook777.heimdall;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

final class UpperScreenMagnifierView extends FrameLayout
        implements UpperScreenProjectionService.Listener, TextureView.SurfaceTextureListener {
    private static final int DARK_CONTENT_INSET_DP = 1;
    private static final int PEARL_CONTENT_INSET_DP = 6;
    private static final long MULTI_TAP_TIMEOUT_MS = ViewConfiguration.getDoubleTapTimeout();
    private static final long TRIPLE_TAP_TOTAL_TIMEOUT_MS = MULTI_TAP_TIMEOUT_MS * 2L;

    interface ActionListener {
        void onProjectionRequested(WidgetLayout.Item item);
        void onRegionRequested(WidgetLayout.Item item, float targetAspectRatio);
        void onStopRequested();
    }

    private final WidgetLayout.Item item;
    private final ActionListener actionListener;
    private final FrameLayout viewport;
    private final SourceSurfaceViewport sourceViewport;
    private final TextureView textureView;
    private final TextView statusView;
    private final ImageView frozenStopControl;
    private final int touchSlopSquared;
    private final int multiTapSlopSquared;
    private final Runnable resetTapSequenceRunnable = this::resetTapSequence;
    private final Runnable clearSingleTapSuppressionRunnable =
            () -> suppressNextConfirmedTap = false;

    private Surface outputSurface;
    private SurfaceTexture outputSurfaceTexture;
    private boolean resumed;
    private boolean tapCandidate;
    private boolean longPressTriggered;
    private boolean suppressNextConfirmedTap;
    private float tapDownX;
    private float tapDownY;
    private float firstTapX;
    private float firstTapY;
    private long firstTapTimeMs;
    private long lastTapTimeMs;
    private int tapCount;

    UpperScreenMagnifierView(Context context, WidgetLayout.Item item, ActionListener listener) {
        super(context);
        this.item = item;
        this.actionListener = listener;
        ViewConfiguration configuration = ViewConfiguration.get(context);
        int touchSlop = configuration.getScaledTouchSlop();
        int multiTapSlop = configuration.getScaledDoubleTapSlop();
        touchSlopSquared = touchSlop * touchSlop;
        multiTapSlopSquared = multiTapSlop * multiTapSlop;
        setContentDescription(context.getString(R.string.magnifier_live_content_description));
        setBackground(HeimdallUi.isPearl(context)
                ? HeimdallUi.cncInputFrame(context, HeimdallUi.RADIUS_MODULE)
                : HeimdallUi.glass(context,
                        0xB20C131D, 0xD2070B11, 0x7770B7FF, 0x33445A72,
                        HeimdallUi.RADIUS_MODULE, HeimdallUi.STROKE_HAIRLINE));

        viewport = new FrameLayout(context);
        viewport.setBackground(new ColorDrawable(0xFF020407));
        viewport.setClipToOutline(true);
        viewport.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int insetDp = HeimdallUi.isPearl(getContext())
                        ? PEARL_CONTENT_INSET_DP : DARK_CONTENT_INSET_DP;
                float radius = dp(HeimdallUi.isPearl(getContext())
                        ? HeimdallUi.concentricInnerRadiusDp(HeimdallUi.RADIUS_MODULE, insetDp)
                        : Math.max(0f, HeimdallUi.RADIUS_MODULE - insetDp));
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        int inset = contentInset();
        LayoutParams viewportParams = new LayoutParams(-1, -1);
        viewportParams.setMargins(inset, inset, inset, inset);
        addView(viewport, viewportParams);

        sourceViewport = new SourceSurfaceViewport(context);
        viewport.addView(sourceViewport, new FrameLayout.LayoutParams(-1, -1));

        textureView = sourceViewport.textureView();
        textureView.setOpaque(true);
        textureView.setSurfaceTextureListener(this);
        textureView.setVisibility(INVISIBLE);

        statusView = new TextView(context);
        statusView.setText(R.string.magnifier_enable_hint);
        statusView.setTextColor(HeimdallUi.isPearl(context) ? 0xFF5E6D7E : 0xFF8EA4BA);
        statusView.setTextSize(12);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(dp(8), dp(8), dp(8), dp(8));
        viewport.addView(statusView, new FrameLayout.LayoutParams(-1, -1));

        frozenStopControl = createFrozenStopControl(context);
        frozenStopControl.setVisibility(GONE);
        frozenStopControl.setOnClickListener(view -> {
            if (!UpperScreenProjectionService.isActiveOrStarting()) {
                return;
            }
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            actionListener.onStopRequested();
        });
        FrameLayout.LayoutParams frozenParams = new FrameLayout.LayoutParams(dp(48), dp(48));
        frozenParams.gravity = Gravity.TOP | Gravity.RIGHT;
        frozenParams.setMargins(0, dp(7), dp(7), 0);
        viewport.addView(frozenStopControl, frozenParams);

        setOnClickListener(view -> {
            if (!UpperScreenProjectionService.isActiveOrStarting()) {
                actionListener.onProjectionRequested(item);
            } else if (UpperScreenProjectionService.isFrozen()) {
                UpperScreenProjectionService.setFrozen(false);
            }
        });
        setOnLongClickListener(view -> {
            float contentWidth = Math.max(1f, viewport.getWidth());
            float contentHeight = Math.max(1f, viewport.getHeight());
            actionListener.onRegionRequested(item, contentWidth / contentHeight);
            return true;
        });
        GestureDetector gestures = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent event) {
                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent event) {
                        resetTapSequence();
                        if (suppressNextConfirmedTap) {
                            suppressNextConfirmedTap = false;
                            removeCallbacks(clearSingleTapSuppressionRunnable);
                            return true;
                        }
                        return performClick();
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent event) {
                        if (UpperScreenProjectionService.isRunning()
                                && !UpperScreenProjectionService.isFrozen()) {
                            UpperScreenProjectionService.setFrozen(true);
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void onLongPress(MotionEvent event) {
                        longPressTriggered = true;
                        tapCandidate = false;
                        resetTapSequence();
                        performLongClick();
                    }
                });
        setOnTouchListener((view, event) -> {
            boolean gestureHandled = gestures.onTouchEvent(event);
            boolean tripleTapHandled = trackTripleTap(event);
            return gestureHandled || tripleTapHandled;
        });
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        DebugPerformanceDiagnostics.countDraw("Magnifier module");
        super.dispatchDraw(canvas);
    }

    @Override
    public void invalidate() {
        DebugPerformanceDiagnostics.countInvalidate("Magnifier module");
        super.invalidate();
    }

    @Override
    public void requestLayout() {
        DebugPerformanceDiagnostics.countRequestLayout("Magnifier module");
        super.requestLayout();
    }

    void resume() {
        resumed = true;
        UpperScreenProjectionService.setListener(this);
        UpperScreenProjectionService.setRegion(item.magnifierLeft, item.magnifierTop,
                item.magnifierRight, item.magnifierBottom);
        UpperScreenProjectionService.setTuning(
                item.magnifierAspectRatio, item.magnifierFps, item.magnifierZoom);
        if (UpperScreenProjectionService.isRunning()) {
            textureView.setVisibility(VISIBLE);
            statusView.setText(R.string.magnifier_connecting);
        }
        ensureOutputSurfaceIfAvailable();
        updateFrozenIndicator();
        applyCropTransform();
    }

    void pause() {
        resumed = false;
    }

    void release() {
        pause();
        resetTapSequence();
        removeCallbacks(clearSingleTapSuppressionRunnable);
        suppressNextConfirmedTap = false;
        UpperScreenProjectionService.clearListener(this);
        detachSurface();
    }

    @Override
    protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (UpperScreenProjectionService.isRunning()) {
            textureView.setVisibility(VISIBLE);
            post(this::ensureOutputSurfaceIfAvailable);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        viewport.invalidateOutline();
        applyCropTransform();
    }

    @Override
    public void onProjectionStatus(String message) {
        String status = message == null ? "" : message;
        boolean active = UpperScreenProjectionService.isRunning();
        textureView.setVisibility(active ? VISIBLE : INVISIBLE);
        statusView.setText(status);
        statusView.setVisibility(VISIBLE);
        if (active) {
            post(this::ensureOutputSurfaceIfAvailable);
        }
        updateFrozenIndicator();
    }

    @Override
    public void onProjectionConfigurationChanged() {
        post(() -> {
            configureSurfaceBuffer();
            applyCropTransform();
            updateFrozenIndicator();
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        bindOutputSurface(surfaceTexture);
        applyCropTransform();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        applyCropTransform();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        detachSurface();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        DebugPerformanceDiagnostics.countSurfaceFrame("Magnifier TextureView");
        if (statusView.getVisibility() != GONE) {
            statusView.setVisibility(GONE);
        }
    }

    private void attachSurfaceIfReady() {
        if (outputSurface != null && outputSurface.isValid()) {
            UpperScreenProjectionService.attachOutputSurface(outputSurface);
        }
    }

    private void ensureOutputSurfaceIfAvailable() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        if (!textureView.isAvailable() || surfaceTexture == null) {
            return;
        }
        if (outputSurface != null && outputSurface.isValid()
                && outputSurfaceTexture == surfaceTexture) {
            attachSurfaceIfReady();
            return;
        }
        bindOutputSurface(surfaceTexture);
    }

    private void bindOutputSurface(SurfaceTexture surfaceTexture) {
        if (surfaceTexture == null) {
            return;
        }
        if (outputSurface != null && outputSurface.isValid()
                && outputSurfaceTexture == surfaceTexture) {
            attachSurfaceIfReady();
            return;
        }
        detachSurface();
        surfaceTexture.setDefaultBufferSize(
                UpperScreenProjectionService.sourceWidth(),
                UpperScreenProjectionService.sourceHeight());
        outputSurfaceTexture = surfaceTexture;
        outputSurface = new Surface(surfaceTexture);
        attachSurfaceIfReady();
    }

    private void configureSurfaceBuffer() {
        sourceViewport.setSourceSize(
                UpperScreenProjectionService.sourceWidth(),
                UpperScreenProjectionService.sourceHeight());
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        if (surfaceTexture != null) {
            surfaceTexture.setDefaultBufferSize(
                    UpperScreenProjectionService.sourceWidth(),
                    UpperScreenProjectionService.sourceHeight());
        }
    }

    private void detachSurface() {
        Surface surface = outputSurface;
        if (surface == null) {
            return;
        }
        UpperScreenProjectionService.detachOutputSurface(surface);
        outputSurface = null;
        outputSurfaceTexture = null;
        surface.release();
    }

    private void applyCropTransform() {
        int width = sourceViewport.getWidth();
        int height = sourceViewport.getHeight();
        int sourceWidth = UpperScreenProjectionService.sourceWidth();
        int sourceHeight = UpperScreenProjectionService.sourceHeight();
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }

        float centerX = (UpperScreenProjectionService.regionLeft()
                + UpperScreenProjectionService.regionRight()) * 0.5f * sourceWidth;
        float centerY = (UpperScreenProjectionService.regionTop()
                + UpperScreenProjectionService.regionBottom()) * 0.5f * sourceHeight;
        float cropWidth = Math.max(2f, (UpperScreenProjectionService.regionRight()
                - UpperScreenProjectionService.regionLeft()) * sourceWidth);
        float cropHeight = Math.max(2f, (UpperScreenProjectionService.regionBottom()
                - UpperScreenProjectionService.regionTop()) * sourceHeight);
        float aspect = clamp(UpperScreenProjectionService.targetAspectRatio(), 0.2f, 5f);
        if (cropWidth / cropHeight > aspect) {
            cropWidth = cropHeight * aspect;
        } else {
            cropHeight = cropWidth / aspect;
        }
        float zoom = WidgetLayout.normalizeMagnifierZoom(
                UpperScreenProjectionService.targetZoom());
        cropWidth /= zoom;
        cropHeight /= zoom;
        float fitScale = Math.min(1f,
                Math.min(sourceWidth / cropWidth, sourceHeight / cropHeight));
        cropWidth *= fitScale;
        cropHeight *= fitScale;

        float left = clamp(centerX - cropWidth / 2f, 0f, sourceWidth - cropWidth);
        float top = clamp(centerY - cropHeight / 2f, 0f, sourceHeight - cropHeight);
        sourceViewport.setSourceSize(sourceWidth, sourceHeight);
        sourceViewport.setCrop(left, top, cropWidth, cropHeight);
    }

    private void updateFrozenIndicator() {
        boolean frozen = UpperScreenProjectionService.isFrozen();
        frozenStopControl.setVisibility(frozen ? VISIBLE : GONE);
        if (frozen) {
            setContentDescription(getResources().getString(
                    R.string.magnifier_paused_content_description));
        } else if (UpperScreenProjectionService.isActiveOrStarting()) {
            setContentDescription(getResources().getString(
                    R.string.magnifier_active_content_description));
        } else {
            setContentDescription(getResources().getString(
                    R.string.magnifier_stopped_content_description));
        }
    }

    private ImageView createFrozenStopControl(Context context) {
        ImageView control = new ImageView(context);
        control.setScaleType(ImageView.ScaleType.FIT_CENTER);
        control.setPadding(dp(8), dp(8), dp(8), dp(8));
        control.setClickable(true);
        control.setFocusable(true);
        control.setContentDescription(context.getString(
                R.string.magnifier_frozen_stop_content_description));
        control.setImageResource(R.drawable.ic_stop);
        control.setImageTintList(ColorStateList.valueOf(0xFFD6DCE2));
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_pressed},
                HeimdallUi.rounded(context, 0xE0222B35, 0xCCD6DCE2, 10, 1));
        background.addState(new int[]{},
                HeimdallUi.rounded(context, 0xB20C1117, 0x997B8188, 10, 1));
        control.setBackground(background);
        return control;
    }

    private boolean trackTripleTap(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            tapCandidate = event.getPointerCount() == 1;
            longPressTriggered = false;
            tapDownX = event.getX();
            tapDownY = event.getY();
            return false;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            tapCandidate = false;
            resetTapSequence();
            return false;
        }
        if (action == MotionEvent.ACTION_MOVE && tapCandidate) {
            float dx = event.getX() - tapDownX;
            float dy = event.getY() - tapDownY;
            if (dx * dx + dy * dy > touchSlopSquared) {
                tapCandidate = false;
                resetTapSequence();
            }
            return false;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            tapCandidate = false;
            resetTapSequence();
            return false;
        }
        if (action != MotionEvent.ACTION_UP) {
            return false;
        }
        boolean eligible = tapCandidate && !longPressTriggered;
        tapCandidate = false;
        return eligible && registerTap(event);
    }

    private boolean registerTap(MotionEvent event) {
        long eventTime = event.getEventTime();
        float x = event.getX();
        float y = event.getY();
        float dx = x - firstTapX;
        float dy = y - firstTapY;
        boolean continuesSequence = tapCount > 0
                && eventTime - lastTapTimeMs <= MULTI_TAP_TIMEOUT_MS
                && eventTime - firstTapTimeMs <= TRIPLE_TAP_TOTAL_TIMEOUT_MS
                && dx * dx + dy * dy <= multiTapSlopSquared;
        if (!continuesSequence) {
            tapCount = 1;
            firstTapTimeMs = eventTime;
            firstTapX = x;
            firstTapY = y;
        } else {
            tapCount++;
        }
        lastTapTimeMs = eventTime;
        removeCallbacks(resetTapSequenceRunnable);
        postDelayed(resetTapSequenceRunnable, MULTI_TAP_TIMEOUT_MS);
        if (tapCount < 3) {
            return false;
        }
        resetTapSequence();
        if (!UpperScreenProjectionService.isActiveOrStarting()) {
            return false;
        }
        suppressNextConfirmedTap = true;
        removeCallbacks(clearSingleTapSuppressionRunnable);
        postDelayed(clearSingleTapSuppressionRunnable, MULTI_TAP_TIMEOUT_MS + 50L);
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        actionListener.onStopRequested();
        return true;
    }

    private void resetTapSequence() {
        removeCallbacks(resetTapSequenceRunnable);
        tapCount = 0;
        firstTapTimeMs = 0L;
        lastTapTimeMs = 0L;
    }

    private int contentInset() {
        return dp(HeimdallUi.isPearl(getContext())
                ? PEARL_CONTENT_INSET_DP : DARK_CONTENT_INSET_DP);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Keeps the projection child in source-pixel space. The lower-screen widget only clips and
     * scales that child, so source crop coordinates never depend on TextureView's destination size.
     */
    private static final class SourceSurfaceViewport extends ViewGroup {
        private final TextureView textureView;
        private int sourceWidth = 1;
        private int sourceHeight = 1;
        private float cropLeft;
        private float cropTop;
        private float cropWidth = 1f;
        private float cropHeight = 1f;

        SourceSurfaceViewport(Context context) {
            super(context);
            setClipChildren(false);
            textureView = new TextureView(context);
            addView(textureView);
        }

        TextureView textureView() {
            return textureView;
        }

        void setSourceSize(int width, int height) {
            int normalizedWidth = Math.max(1, width);
            int normalizedHeight = Math.max(1, height);
            if (sourceWidth == normalizedWidth && sourceHeight == normalizedHeight) {
                return;
            }
            sourceWidth = normalizedWidth;
            sourceHeight = normalizedHeight;
            requestLayout();
        }

        void setCrop(float left, float top, float width, float height) {
            cropLeft = left;
            cropTop = top;
            cropWidth = Math.max(1f, width);
            cropHeight = Math.max(1f, height);
            invalidate();
        }

        @Override
        public void invalidate() {
            DebugPerformanceDiagnostics.countInvalidate("Magnifier source viewport");
            super.invalidate();
        }

        @Override
        public void requestLayout() {
            DebugPerformanceDiagnostics.countRequestLayout("Magnifier source viewport");
            super.requestLayout();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            setMeasuredDimension(width, height);
            textureView.measure(
                    MeasureSpec.makeMeasureSpec(sourceWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(sourceHeight, MeasureSpec.EXACTLY));
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            textureView.layout(0, 0, sourceWidth, sourceHeight);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            if (getWidth() <= 0 || getHeight() <= 0 || textureView.getVisibility() != VISIBLE) {
                return;
            }
            float scaleX = getWidth() / cropWidth;
            float scaleY = getHeight() / cropHeight;
            int save = canvas.save();
            canvas.clipRect(0f, 0f, getWidth(), getHeight());
            canvas.translate(-cropLeft * scaleX, -cropTop * scaleY);
            canvas.scale(scaleX, scaleY);
            drawChild(canvas, textureView, getDrawingTime());
            canvas.restoreToCount(save);
        }
    }
}
