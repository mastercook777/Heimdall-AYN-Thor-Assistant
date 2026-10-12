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
    private static final long MULTI_TAP_TIMEOUT_MS = ViewConfiguration.getDoubleTapTimeout();
    private static final long TRIPLE_TAP_TOTAL_TIMEOUT_MS = MULTI_TAP_TIMEOUT_MS * 2L;

    interface ActionListener {
        void onProjectionRequested(WidgetLayout.Item item);
        void onRegionRequested(WidgetLayout.Item item, float targetAspectRatio);
        void onStopRequested();
        void onInteractivePreparationRequested();
        boolean isInteractiveReady();
        boolean onInteractiveTouchStart(float sourceX, float sourceY,
                int sourceWidth, int sourceHeight);
        boolean onInteractiveTouchMove(float sourceX, float sourceY);
        boolean onInteractiveTouchEnd(float sourceX, float sourceY);
        void onInteractiveTouchCancel();
        void onInteractiveUnavailable();
    }

    private final WidgetLayout.Item item;
    private final ActionListener actionListener;
    private final boolean interactive;
    private final boolean circular;
    private final ShapeFrameLayout displayFrame;
    private final FrameLayout viewport;
    private final SourceSurfaceViewport sourceViewport;
    private final TextureView textureView;
    private final TextView statusView;
    private final ImageView frozenStopControl;
    private final int touchSlopSquared;
    private final int multiTapSlopSquared;
    private final Runnable triggerEditLongPress = this::triggerEditLongPress;
    private final Runnable resetTapSequenceRunnable = this::resetTapSequence;
    private final Runnable clearSingleTapSuppressionRunnable =
            () -> suppressNextConfirmedTap = false;

    private Surface outputSurface;
    private SurfaceTexture outputSurfaceTexture;
    private boolean resumed;
    private boolean tapCandidate;
    private boolean longPressTriggered;
    private boolean editLongPressPending;
    private boolean suppressNextConfirmedTap;
    private float tapDownX;
    private float tapDownY;
    private float firstTapX;
    private float firstTapY;
    private long firstTapTimeMs;
    private long lastTapTimeMs;
    private int tapCount;
    private boolean interactiveTouchActive;
    private boolean interactiveGestureBlocked;
    private int interactivePointerId = MotionEvent.INVALID_POINTER_ID;
    private final float[] mappedTouchPoint = new float[2];

    UpperScreenMagnifierView(Context context, WidgetLayout.Item item, ActionListener listener) {
        super(context);
        this.item = item;
        this.actionListener = listener;
        interactive = WidgetLayout.isInteractiveMagnifier(item);
        circular = WidgetLayout.isCircularMagnifier(item);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        int touchSlop = configuration.getScaledTouchSlop();
        int multiTapSlop = configuration.getScaledDoubleTapSlop();
        touchSlopSquared = touchSlop * touchSlop;
        multiTapSlopSquared = multiTapSlop * multiTapSlop;
        displayFrame = new ShapeFrameLayout(context, circular);
        displayFrame.setContentDescription(context.getString(interactive
                ? R.string.portal_interactive_content_description
                : R.string.magnifier_live_content_description));
        displayFrame.setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncInputFrame(context, HeimdallUi.RADIUS_MODULE, circular)
                : circular
                        ? HeimdallUi.glassCircleSurface(context,
                                ThemeGlassColors.MAGNIFIER_FRAME, HeimdallUi.STROKE_HAIRLINE)
                        : HeimdallUi.glassSurface(context, ThemeGlassColors.MAGNIFIER_FRAME,
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
                } else {
                    float radius = dp(HeimdallUi.mediaFrameInnerRadiusDp(
                            getContext(), HeimdallUi.RADIUS_MODULE));
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            }
        });
        int inset = contentInset();
        LayoutParams viewportParams = new LayoutParams(-1, -1);
        viewportParams.setMargins(inset, inset, inset, inset);
        displayFrame.addView(viewport, viewportParams);

        sourceViewport = new SourceSurfaceViewport(context);
        viewport.addView(sourceViewport, new FrameLayout.LayoutParams(-1, -1));

        textureView = sourceViewport.textureView();
        textureView.setOpaque(true);
        textureView.setSurfaceTextureListener(this);
        textureView.setVisibility(INVISIBLE);

        statusView = new TextView(context);
        statusView.setText(R.string.magnifier_enable_hint);
        statusView.setTextColor(HeimdallUi.componentColors(context).mediaHintText);
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

        displayFrame.setOnClickListener(view -> {
            if (!UpperScreenProjectionService.isActiveOrStarting()) {
                actionListener.onProjectionRequested(item);
            } else if (!interactive && isThisPortalFrozen()) {
                setThisPortalFrozen(false);
            }
        });
        displayFrame.setOnLongClickListener(view -> {
            if (interactive) {
                return false;
            }
            float contentWidth = Math.max(1f, viewport.getWidth());
            float contentHeight = Math.max(1f, viewport.getHeight());
            actionListener.onRegionRequested(item,
                    circular ? 1f : contentWidth / contentHeight);
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
                        return displayFrame.performClick();
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent event) {
                        if (circular && UpperScreenProjectionService.isActiveOrStarting()) {
                            return true;
                        }
                        if (UpperScreenProjectionService.isRunning()
                                && !isThisPortalFrozen()) {
                            setThisPortalFrozen(true);
                            return true;
                        }
                        return false;
                    }

                });
        gestures.setIsLongpressEnabled(false);
        if (interactive) {
            displayFrame.setLongClickable(false);
            displayFrame.setOnTouchListener((view, event) -> handleInteractiveTouch(event));
        } else {
            displayFrame.setOnTouchListener((view, event) -> {
                boolean tripleTapHandled = trackTripleTap(event);
                boolean editLongPressHandled = trackEditLongPress(gestures, event);
                boolean gestureHandled = !editLongPressHandled && gestures.onTouchEvent(event);
                return gestureHandled || tripleTapHandled || editLongPressHandled;
            });
        }
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
        if (interactive) {
            actionListener.onInteractivePreparationRequested();
            if (isThisPortalFrozen()) {
                setThisPortalFrozen(false);
            }
        }
        UpperScreenProjectionService.setListener(this);
        if (!UpperScreenProjectionService.isMultiPortalMode()) {
            UpperScreenProjectionService.setRegion(item.magnifierLeft, item.magnifierTop,
                    item.magnifierRight, item.magnifierBottom);
            UpperScreenProjectionService.setTuning(
                    WidgetLayout.magnifierTargetAspectRatio(item),
                    item.magnifierFps, item.magnifierZoom);
        }
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
        cancelInteractiveTouch();
    }

    String itemId() {
        return item.itemId == null ? "" : item.itemId;
    }

    float renderedContentAspectRatio() {
        int width = viewport.getWidth();
        int height = viewport.getHeight();
        if (width <= 0 || height <= 0) {
            return 0f;
        }
        return width / (float) height;
    }

    void release() {
        pause();
        cancelEditLongPress();
        longPressTriggered = false;
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
        } else {
            cancelInteractiveTouch();
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
            if (UpperScreenProjectionService.isMultiPortalMode()) {
                UpperScreenProjectionService.attachPortalOutput(itemId(), outputSurface,
                        UpperScreenProjectionService.sourceWidth(),
                        UpperScreenProjectionService.sourceHeight(), item);
            } else {
                UpperScreenProjectionService.attachOutputSurface(outputSurface);
            }
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
        if (UpperScreenProjectionService.isMultiPortalMode()) {
            UpperScreenProjectionService.updatePortalTargetSize(itemId(),
                    UpperScreenProjectionService.sourceWidth(),
                    UpperScreenProjectionService.sourceHeight());
        }
    }

    private void detachSurface() {
        Surface surface = outputSurface;
        if (surface == null) {
            return;
        }
        if (UpperScreenProjectionService.isMultiPortalMode()) {
            UpperScreenProjectionService.detachPortalOutput(itemId());
        } else {
            UpperScreenProjectionService.detachOutputSurface(surface);
        }
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

        PortalSurfaceFanOut.Spec portalSpec = UpperScreenProjectionService.isMultiPortalMode()
                ? UpperScreenProjectionService.portalSpec(itemId(), item) : null;

        float regionLeft = portalSpec == null
                ? UpperScreenProjectionService.regionLeft() : portalSpec.left;
        float regionTop = portalSpec == null
                ? UpperScreenProjectionService.regionTop() : portalSpec.top;
        float regionRight = portalSpec == null
                ? UpperScreenProjectionService.regionRight() : portalSpec.right;
        float regionBottom = portalSpec == null
                ? UpperScreenProjectionService.regionBottom() : portalSpec.bottom;
        float centerX = (regionLeft + regionRight) * 0.5f * sourceWidth;
        float centerY = (regionTop + regionBottom) * 0.5f * sourceHeight;
        float cropWidth = Math.max(2f, (regionRight - regionLeft) * sourceWidth);
        float cropHeight = Math.max(2f, (regionBottom - regionTop) * sourceHeight);
        float aspect = circular ? 1f
                : clamp(portalSpec == null
                        ? UpperScreenProjectionService.targetAspectRatio()
                        : portalSpec.aspect, 0.2f, 5f);
        if (cropWidth / cropHeight > aspect) {
            cropWidth = cropHeight * aspect;
        } else {
            cropHeight = cropWidth / aspect;
        }
        float zoom = WidgetLayout.normalizeMagnifierZoom(
                portalSpec == null ? UpperScreenProjectionService.targetZoom() : portalSpec.zoom);
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
        boolean frozen = isThisPortalFrozen();
        frozenStopControl.setVisibility(frozen && !circular && !interactive ? VISIBLE : GONE);
        if (interactive) {
            displayFrame.setContentDescription(getResources().getString(
                    UpperScreenProjectionService.isActiveOrStarting()
                            ? R.string.portal_interactive_content_description
                            : R.string.portal_stopped_content_description));
            return;
        }
        if (frozen) {
            displayFrame.setContentDescription(getResources().getString(circular
                    ? R.string.magnifier_circle_paused_content_description
                    : R.string.magnifier_paused_content_description));
        } else if (UpperScreenProjectionService.isActiveOrStarting()) {
            displayFrame.setContentDescription(getResources().getString(circular
                    ? R.string.magnifier_circle_active_content_description
                    : R.string.magnifier_active_content_description));
        } else {
            displayFrame.setContentDescription(getResources().getString(
                    R.string.magnifier_stopped_content_description));
        }
    }

    private boolean isThisPortalFrozen() {
        return UpperScreenProjectionService.isPortalFrozen(itemId());
    }

    private void setThisPortalFrozen(boolean value) {
        UpperScreenProjectionService.setPortalFrozen(itemId(), value);
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

    private boolean handleInteractiveTouch(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            interactiveGestureBlocked = false;
            interactivePointerId = event.getPointerId(0);
            if (!isInsideViewport(event.getX(), event.getY())) {
                interactivePointerId = MotionEvent.INVALID_POINTER_ID;
                return false;
            }
            if (!UpperScreenProjectionService.isRunning()) {
                actionListener.onProjectionRequested(item);
                interactiveGestureBlocked = true;
                return true;
            }
            if (!actionListener.isInteractiveReady()) {
                actionListener.onInteractiveUnavailable();
                interactiveGestureBlocked = true;
                return true;
            }
            if (!mapEventPointToSource(event, 0, mappedTouchPoint)) {
                interactiveGestureBlocked = true;
                return true;
            }
            interactiveTouchActive = actionListener.onInteractiveTouchStart(
                    mappedTouchPoint[0], mappedTouchPoint[1],
                    UpperScreenProjectionService.sourceWidth(),
                    UpperScreenProjectionService.sourceHeight());
            interactiveGestureBlocked = !interactiveTouchActive;
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            cancelInteractiveTouch();
            interactiveGestureBlocked = true;
            return true;
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            cancelInteractiveTouch();
            interactiveGestureBlocked = false;
            interactivePointerId = MotionEvent.INVALID_POINTER_ID;
            return true;
        }
        if (interactiveGestureBlocked) {
            if (action == MotionEvent.ACTION_UP) {
                interactiveGestureBlocked = false;
                interactivePointerId = MotionEvent.INVALID_POINTER_ID;
            }
            return true;
        }
        int pointerIndex = event.findPointerIndex(interactivePointerId);
        if (pointerIndex < 0 || !mapEventPointToSource(event, pointerIndex, mappedTouchPoint)) {
            cancelInteractiveTouch();
            interactiveGestureBlocked = true;
            return true;
        }
        if (circular && !isInsideViewport(
                event.getX(pointerIndex), event.getY(pointerIndex))) {
            cancelInteractiveTouch();
            interactiveGestureBlocked = true;
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (interactiveTouchActive && !actionListener.onInteractiveTouchMove(
                    mappedTouchPoint[0], mappedTouchPoint[1])) {
                cancelInteractiveTouch();
                interactiveGestureBlocked = true;
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP) {
            if (interactiveTouchActive) {
                actionListener.onInteractiveTouchEnd(mappedTouchPoint[0], mappedTouchPoint[1]);
            }
            interactiveTouchActive = false;
            interactivePointerId = MotionEvent.INVALID_POINTER_ID;
            return true;
        }
        return true;
    }

    private boolean isInsideViewport(float x, float y) {
        if (x < viewport.getLeft() || x > viewport.getRight()
                || y < viewport.getTop() || y > viewport.getBottom()) {
            return false;
        }
        if (!circular) {
            return true;
        }
        float localX = x - viewport.getLeft();
        float localY = y - viewport.getTop();
        float radius = Math.min(viewport.getWidth(), viewport.getHeight()) * 0.5f;
        float dx = localX - viewport.getWidth() * 0.5f;
        float dy = localY - viewport.getHeight() * 0.5f;
        return dx * dx + dy * dy <= radius * radius;
    }

    private boolean mapEventPointToSource(MotionEvent event, int pointerIndex, float[] out) {
        float localX = event.getX(pointerIndex) - viewport.getLeft();
        float localY = event.getY(pointerIndex) - viewport.getTop();
        return sourceViewport.mapRenderedPointToSource(localX, localY, out);
    }

    private void cancelInteractiveTouch() {
        if (interactiveTouchActive) {
            actionListener.onInteractiveTouchCancel();
        }
        interactiveTouchActive = false;
        interactivePointerId = MotionEvent.INVALID_POINTER_ID;
    }

    static boolean mapRenderedPointToSource(float viewX, float viewY,
            float viewportWidth, float viewportHeight,
            float cropLeft, float cropTop, float cropWidth, float cropHeight,
            int sourceWidth, int sourceHeight, float[] out) {
        if (out == null || out.length < 2 || viewportWidth <= 0f || viewportHeight <= 0f
                || cropWidth <= 0f || cropHeight <= 0f
                || sourceWidth <= 0 || sourceHeight <= 0) {
            return false;
        }
        float scaleX = viewportWidth / cropWidth;
        float scaleY = viewportHeight / cropHeight;
        float clampedX = clamp(viewX, 0f, viewportWidth);
        float clampedY = clamp(viewY, 0f, viewportHeight);
        out[0] = clamp(cropLeft + clampedX / scaleX, 0f, sourceWidth - 1f);
        out[1] = clamp(cropTop + clampedY / scaleY, 0f, sourceHeight - 1f);
        return true;
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

    private boolean trackEditLongPress(GestureDetector gestures, MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            cancelEditLongPress();
            if (event.getPointerCount() == 1) {
                editLongPressPending = true;
                postDelayed(triggerEditLongPress,
                        HeimdallInteraction.EDIT_LONG_PRESS_TIMEOUT_MS);
            }
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            cancelEditLongPress();
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = event.getX() - tapDownX;
            float dy = event.getY() - tapDownY;
            if (event.getPointerCount() != 1 || dx * dx + dy * dy > touchSlopSquared) {
                cancelEditLongPress();
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            boolean consume = longPressTriggered;
            cancelEditLongPress();
            if (consume) {
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                gestures.onTouchEvent(cancel);
                cancel.recycle();
                longPressTriggered = false;
                return true;
            }
        }
        return longPressTriggered;
    }

    private void triggerEditLongPress() {
        if (!editLongPressPending || !isAttachedToWindow()
                || !displayFrame.isShown() || !displayFrame.isEnabled()) {
            cancelEditLongPress();
            return;
        }
        editLongPressPending = false;
        longPressTriggered = true;
        tapCandidate = false;
        resetTapSequence();
        displayFrame.performLongClick();
    }

    private void cancelEditLongPress() {
        if (editLongPressPending) removeCallbacks(triggerEditLongPress);
        editLongPressPending = false;
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
        return dp(HeimdallUi.mediaFrameContentInsetDp(getContext()));
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

    private static final class ShapeFrameLayout extends FrameLayout {
        private final boolean circular;

        ShapeFrameLayout(Context context, boolean circular) {
            super(context);
            this.circular = circular;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            if (circular && event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                float radius = Math.min(getWidth(), getHeight()) * 0.5f;
                float dx = event.getX() - getWidth() * 0.5f;
                float dy = event.getY() - getHeight() * 0.5f;
                if (dx * dx + dy * dy > radius * radius) {
                    return false;
                }
            }
            return super.dispatchTouchEvent(event);
        }
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

        boolean mapRenderedPointToSource(float x, float y, float[] out) {
            return UpperScreenMagnifierView.mapRenderedPointToSource(
                    x, y, getWidth(), getHeight(),
                    cropLeft, cropTop, cropWidth, cropHeight,
                    sourceWidth, sourceHeight, out);
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
