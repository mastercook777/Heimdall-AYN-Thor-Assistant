package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint({"ViewConstructor", "ClickableViewAccessibility"})
final class CanvasCompositionEditor extends LinearLayout {
    interface Listener {
        void onDone(CanvasConfig config);
        void onCancel();
    }

    private final CanvasConfig initialConfig;
    private final boolean initialFill;
    private final Listener listener;
    private final CanvasCompositionSurface compositionSurface;
    private final View compositionView;
    private final CanvasImageView imageView;
    private final CanvasVideoView videoView;
    private final TextView stateView;
    private final Button fitButton;
    private final Button fillButton;
    private final Button resetButton;
    private final Button doneButton;
    private CanvasImageLoader.Request loadRequest;
    private CanvasImageLoader.DecodedImage decodedImage;
    private boolean playbackAllowed;
    private boolean aggregatedVisible;

    CanvasCompositionEditor(Context context, CanvasConfig config, boolean initialFill,
            int targetFrameWidth, int targetFrameHeight, Listener listener) {
        super(context);
        this.initialConfig = config == null ? new CanvasConfig() : config.copy();
        this.initialFill = initialFill;
        this.listener = listener;
        boolean circular = this.initialConfig.isCircular();
        setOrientation(VERTICAL);
        setPadding(dp(12), dp(10), dp(12), dp(10));
        setBackground(HeimdallUi.isPearl(context)
                ? HeimdallUi.cncFlush(context, HeimdallUi.RADIUS_PANEL)
                : HeimdallUi.glass(context, 0xFF0B111B, 0xFF070A10,
                        0x886A829C, 0x44344150, HeimdallUi.RADIUS_PANEL, 2));

        TextView title = new TextView(context);
        title.setText(R.string.canvas_edit_composition);
        title.setTextColor(HeimdallUi.textColor(context));
        title.setTextSize(HeimdallUi.TYPE_EDITOR_TITLE);
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        addView(title, new LayoutParams(-1, dp(36)));

        GestureStage previewStage = new GestureStage(context);
        previewStage.setPadding(dp(8), dp(8), dp(8), dp(8));
        previewStage.setClipChildren(false);
        previewStage.setClipToPadding(false);
        LayoutParams stageParams = new LayoutParams(-1, 0, 1f);
        stageParams.setMargins(0, dp(4), 0, dp(8));
        addView(previewStage, stageParams);

        ReferenceFrame referenceFrame = new ReferenceFrame(context,
                targetFrameWidth, targetFrameHeight, circular);
        referenceFrame.setBackground(HeimdallUi.isPearl(context)
                ? HeimdallUi.cncInputFrame(context, HeimdallUi.RADIUS_MODULE, circular)
                : circular
                        ? HeimdallUi.glassCircle(context, 0xB20C131D, 0xD2070B11,
                                0x555F7C9A, 0x33344150, HeimdallUi.STROKE_HAIRLINE)
                        : HeimdallUi.glass(context, 0xB20C131D, 0xD2070B11,
                                0x555F7C9A, 0x33344150, HeimdallUi.RADIUS_MODULE,
                                HeimdallUi.STROKE_HAIRLINE));
        FrameLayout.LayoutParams referenceParams = new FrameLayout.LayoutParams(
                -2, -2, Gravity.CENTER);
        previewStage.addView(referenceFrame, referenceParams);

        FrameLayout viewport = new FrameLayout(context);
        viewport.setBackgroundColor(0xFF020407);
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
        int viewportInset = dp(HeimdallUi.isPearl(context) ? 6 : 1);
        FrameLayout.LayoutParams viewportParams = new FrameLayout.LayoutParams(-1, -1);
        viewportParams.setMargins(viewportInset, viewportInset, viewportInset, viewportInset);
        referenceFrame.addView(viewport, viewportParams);

        if (this.initialConfig.video) {
            videoView = new CanvasVideoView(context);
            imageView = null;
            compositionSurface = videoView;
            compositionView = videoView;
        } else {
            imageView = new CanvasImageView(context);
            videoView = null;
            compositionSurface = imageView;
            compositionView = imageView;
        }
        compositionSurface.setInteractive(true);
        viewport.addView(compositionView, new FrameLayout.LayoutParams(-1, -1));
        previewStage.setGestureTarget(compositionView);

        stateView = new TextView(context);
        stateView.setText(R.string.canvas_loading);
        stateView.setTextColor(HeimdallUi.mutedTextColor(context));
        stateView.setTextSize(13);
        stateView.setGravity(Gravity.CENTER);
        viewport.addView(stateView, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout compositionActions = new LinearLayout(context);
        compositionActions.setOrientation(HORIZONTAL);
        addView(compositionActions, new LayoutParams(-1, dp(44)));
        fitButton = addAction(compositionActions, R.string.canvas_fit,
                compositionSurface::fitImage, false, false);
        fillButton = addAction(compositionActions, R.string.canvas_fill,
                compositionSurface::fillImage, false, false);
        resetButton = addAction(compositionActions, R.string.canvas_reset,
                compositionSurface::resetImage, false, false);

        LinearLayout commitActions = new LinearLayout(context);
        commitActions.setOrientation(HORIZONTAL);
        LayoutParams commitParams = new LayoutParams(-1, dp(48));
        commitParams.setMargins(0, dp(5), 0, 0);
        addView(commitActions, commitParams);
        addAction(commitActions, R.string.common_cancel, listener::onCancel,
                false, true);
        doneButton = addAction(commitActions, R.string.canvas_done, () -> {
            CanvasConfig result = compositionSurface.composition();
            result.assetId = initialConfig.assetId;
            result.sourceType = CanvasConfig.SOURCE_LOCAL_IMAGE;
            result.animated = initialConfig.animated;
            result.video = initialConfig.video;
            result.normalize();
            listener.onDone(result);
        }, true, true);

        setEditingEnabled(false);
        post(this::loadMedia);
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

    void release() {
        if (loadRequest != null) {
            loadRequest.cancel();
            loadRequest = null;
        }
        if (imageView != null) {
            imageView.setImageDrawable(null);
        }
        if (videoView != null) {
            videoView.release();
        }
        if (decodedImage != null) {
            decodedImage.release();
            decodedImage = null;
        }
    }

    private void loadMedia() {
        if (initialConfig.video) {
            loadVideo();
            return;
        }
        if (loadRequest != null || !initialConfig.hasAsset()) {
            showError(CanvasImageLoader.Error.DECODE);
            return;
        }
        loadRequest = CanvasImageLoader.load(getContext(), initialConfig.assetId, 2400,
                new CanvasImageLoader.Callback() {
                    @Override
                    public void onLoaded(CanvasImageLoader.DecodedImage loaded) {
                        loadRequest = null;
                        decodedImage = loaded;
                        if (loaded.bitmap() != null) {
                            imageView.setImageBitmap(loaded.bitmap());
                        } else {
                            imageView.setImageDrawable(loaded.drawable());
                        }
                        imageView.setComposition(initialConfig, initialFill);
                        stateView.setVisibility(View.GONE);
                        setEditingEnabled(true);
                        updatePlayback();
                    }

                    @Override
                    public void onError(CanvasImageLoader.Error error) {
                        loadRequest = null;
                        showError(error);
                    }
                });
    }

    private void loadVideo() {
        if (videoView == null || !initialConfig.hasAsset()) {
            showError(CanvasImageLoader.Error.DECODE);
            return;
        }
        videoView.setSource(initialConfig, new CanvasVideoView.Listener() {
            @Override
            public void onReady() {
                videoView.setComposition(initialConfig, initialFill);
                stateView.setVisibility(View.GONE);
                setEditingEnabled(true);
                updatePlayback();
            }

            @Override
            public void onError() {
                showError(CanvasImageLoader.Error.DECODE);
            }
        });
        videoView.setComposition(initialConfig, initialFill);
        updatePlayback();
    }

    private void showError(CanvasImageLoader.Error error) {
        stateView.setText(error == CanvasImageLoader.Error.PLATFORM_UNSUPPORTED
                ? R.string.canvas_animation_platform_unsupported
                : R.string.canvas_decode_error);
        stateView.setTextColor(HeimdallUi.COLOR_DANGER);
        stateView.setVisibility(View.VISIBLE);
        setEditingEnabled(false);
    }

    private void updatePlayback() {
        if (videoView != null) {
            videoView.setPlaybackAllowed(
                    playbackAllowed && aggregatedVisible && isAttachedToWindow());
            return;
        }
        if (decodedImage == null || !decodedImage.isAnimated()) {
            return;
        }
        if (playbackAllowed && aggregatedVisible && isAttachedToWindow()) {
            decodedImage.start();
        } else {
            decodedImage.stop();
        }
    }

    private void setEditingEnabled(boolean enabled) {
        compositionSurface.setInteractive(enabled);
        fitButton.setEnabled(enabled);
        fillButton.setEnabled(enabled);
        resetButton.setEnabled(enabled);
        doneButton.setEnabled(enabled);
        fitButton.setAlpha(enabled ? 1f : 0.45f);
        fillButton.setAlpha(enabled ? 1f : 0.45f);
        resetButton.setAlpha(enabled ? 1f : 0.45f);
        doneButton.setAlpha(enabled ? 1f : 0.45f);
    }

    private Button addAction(LinearLayout parent, int labelRes, Runnable action,
            boolean primary, boolean wide) {
        Button button = new Button(getContext());
        button.setAllCaps(false);
        button.setText(labelRes);
        button.setTextSize(HeimdallUi.TYPE_BUTTON_COMPACT);
        button.setTextColor(HeimdallUi.textColor(getContext()));
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(6), 0, dp(6), 0);
        button.setOnClickListener(view -> action.run());
        if (primary) {
            HeimdallUi.applyPrimaryActionButton(getContext(), button);
        } else {
            HeimdallUi.applySecondaryButton(getContext(), button);
        }
        LayoutParams params = new LayoutParams(0, -1, 1f);
        params.setMargins(dp(wide ? 4 : 3), dp(2), dp(wide ? 4 : 3), dp(2));
        parent.addView(button, params);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class ReferenceFrame extends FrameLayout {
        private final int targetWidth;
        private final int targetHeight;

        ReferenceFrame(Context context, int targetWidth, int targetHeight, boolean circular) {
            super(context);
            int normalizedWidth = Math.max(1, targetWidth);
            int normalizedHeight = Math.max(1, targetHeight);
            int diameter = Math.min(normalizedWidth, normalizedHeight);
            this.targetWidth = circular ? diameter : normalizedWidth;
            this.targetHeight = circular ? diameter : normalizedHeight;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int availableWidth = Math.max(1, MeasureSpec.getSize(widthMeasureSpec));
            int availableHeight = Math.max(1, MeasureSpec.getSize(heightMeasureSpec));
            float scale = Math.min(1f, Math.min(
                    availableWidth / (float) targetWidth,
                    availableHeight / (float) targetHeight));
            int measuredWidth = Math.max(1, Math.round(targetWidth * scale));
            int measuredHeight = Math.max(1, Math.round(targetHeight * scale));
            super.onMeasure(MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY));
        }
    }

    private static final class GestureStage extends FrameLayout {
        private final Rect targetBounds = new Rect();
        private View gestureTarget;

        GestureStage(Context context) {
            super(context);
        }

        void setGestureTarget(View target) {
            gestureTarget = target;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            return gestureTarget != null;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (gestureTarget == null) {
                return false;
            }
            targetBounds.set(0, 0, gestureTarget.getWidth(), gestureTarget.getHeight());
            offsetDescendantRectToMyCoords(gestureTarget, targetBounds);
            MotionEvent forwarded = MotionEvent.obtain(event);
            forwarded.offsetLocation(-targetBounds.left, -targetBounds.top);
            boolean handled = gestureTarget.onTouchEvent(forwarded);
            forwarded.recycle();
            return handled;
        }
    }
}
