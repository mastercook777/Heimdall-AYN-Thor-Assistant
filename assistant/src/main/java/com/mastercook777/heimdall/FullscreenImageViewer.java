package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ViewConstructor")
final class FullscreenImageViewer extends FrameLayout {
    interface Listener {
        void onClose();
        void onEditComposition();
    }

    private final CanvasConfig config;
    private final Listener listener;
    private final boolean showEditAction;
    private final LinearLayout toolbar;
    private final ImageButton revealButton;
    private final TextView stateView;
    private TextView slideTitle;
    private final Runnable hideControls = () -> setControlsVisible(false);
    private CanvasImageLoader.Request loadRequest;
    private ZoomableMapView viewer;
    private CanvasImageLoader.DecodedImage decodedImage;
    private CanvasVideoView videoViewer;
    private boolean playbackAllowed;
    private boolean aggregatedVisible;
    private int currentSlideIndex;
    private int displayedSlideIndex;
    private int loadGeneration;

    FullscreenImageViewer(Context context, CanvasConfig config, Listener listener) {
        this(context, config, 0, listener, true);
    }

    FullscreenImageViewer(Context context, CanvasConfig config, Listener listener,
            boolean showEditAction) {
        this(context, config, 0, listener, showEditAction);
    }

    FullscreenImageViewer(Context context, CanvasConfig config, int slideIndex,
            Listener listener) {
        this(context, config, slideIndex, listener, true);
    }

    private FullscreenImageViewer(Context context, CanvasConfig config, int slideIndex,
            Listener listener, boolean showEditAction) {
        super(context);
        this.config = config == null ? new CanvasConfig() : config.copy();
        currentSlideIndex = this.config.isSlideshow()
                ? Math.max(0, Math.min(slideIndex, this.config.slides.size() - 1)) : 0;
        displayedSlideIndex = currentSlideIndex;
        this.listener = listener;
        this.showEditAction = showEditAction;
        setBackgroundColor(0xFF020407);

        stateView = new TextView(context);
        stateView.setText(R.string.canvas_loading);
        stateView.setTextColor(HeimdallUi.mutedTextColor(context));
        stateView.setTextSize(14);
        stateView.setGravity(Gravity.CENTER);
        addView(stateView, new LayoutParams(-1, -1));

        toolbar = createToolbar();
        LayoutParams toolbarParams = new LayoutParams(-1, dp(50));
        toolbarParams.gravity = Gravity.TOP;
        toolbarParams.setMargins(dp(8), dp(8), dp(8), 0);
        addView(toolbar, toolbarParams);

        revealButton = iconButton(R.drawable.ic_toolbar_reveal,
                R.string.map_show_navigation, this::showControls);
        toolbar.setElevation(dp(2));
        LayoutParams revealParams = new LayoutParams(dp(52), dp(34));
        revealParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        revealParams.setMargins(0, dp(6), 0, 0);
        revealButton.setElevation(dp(3));
        addView(revealButton, revealParams);
        showControls();
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
        loadGeneration++;
        removeCallbacks(hideControls);
        if (loadRequest != null) {
            loadRequest.cancel();
            loadRequest = null;
        }
        if (viewer != null) {
            viewer.setImageDrawable(null);
            viewer = null;
        }
        if (decodedImage != null) {
            decodedImage.release();
            decodedImage = null;
        }
        if (videoViewer != null) {
            videoViewer.release();
            videoViewer = null;
        }
    }

    private void loadMedia() {
        if (config.video) {
            loadVideo();
            return;
        }
        CanvasConfig current = config.slideConfig(currentSlideIndex);
        int generation = ++loadGeneration;
        loadRequest = CanvasImageLoader.load(getContext(), current.assetId, 3000,
                new CanvasImageLoader.Callback() {
                    @Override
                    public void onLoaded(CanvasImageLoader.DecodedImage loaded) {
                        if (generation != loadGeneration) {
                            loaded.release();
                            return;
                        }
                        loadRequest = null;
                        displayedSlideIndex = currentSlideIndex;
                        ZoomableMapView previousViewer = viewer;
                        CanvasImageLoader.DecodedImage previousImage = decodedImage;
                        decodedImage = loaded;
                        viewer = new ZoomableMapView(getContext());
                        if (loaded.bitmap() != null) {
                            viewer.setImageBitmap(loaded.bitmap());
                        } else {
                            viewer.setImageDrawable(loaded.drawable());
                        }
                        viewer.setBackgroundColor(0xFF020407);
                        addView(viewer, 0, new LayoutParams(-1, -1));
                        if (previousViewer != null) {
                            previousViewer.setImageDrawable(null);
                            removeView(previousViewer);
                        }
                        if (previousImage != null) previousImage.release();
                        stateView.setVisibility(GONE);
                        bringChildToFront(toolbar);
                        bringChildToFront(revealButton);
                        showControls();
                        updatePlayback();
                    }

                    @Override
                    public void onError(CanvasImageLoader.Error error) {
                        if (generation != loadGeneration) return;
                        loadRequest = null;
                        if (viewer != null) {
                            currentSlideIndex = displayedSlideIndex;
                            updateSlideTitle();
                            Toast.makeText(getContext(), R.string.canvas_image_missing,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        stateView.setText(error == CanvasImageLoader.Error.MISSING
                                ? R.string.canvas_image_missing
                                : error == CanvasImageLoader.Error.PLATFORM_UNSUPPORTED
                                        ? R.string.canvas_animation_platform_unsupported
                                        : R.string.canvas_decode_error);
                        stateView.setTextColor(HeimdallUi.semanticColor(
                                getContext(), HeimdallUi.SEMANTIC_ERROR));
                        showControls();
                    }
                });
    }

    private void loadVideo() {
        videoViewer = new CanvasVideoView(getContext());
        videoViewer.setInteractive(true);
        CanvasConfig fullscreenConfig = config.copy();
        fullscreenConfig.focusX = 0.5f;
        fullscreenConfig.focusY = 0.5f;
        fullscreenConfig.zoom = CanvasConfig.MIN_ZOOM;
        videoViewer.setComposition(fullscreenConfig, false);
        addView(videoViewer, 0, new LayoutParams(-1, -1));
        videoViewer.setSource(fullscreenConfig, new CanvasVideoView.Listener() {
            @Override
            public void onReady() {
                stateView.setVisibility(GONE);
                bringChildToFront(toolbar);
                bringChildToFront(revealButton);
                showControls();
            }

            @Override
            public void onError() {
                stateView.setText(CanvasAssetStore.resolve(getContext(), config.assetId) == null
                        ? R.string.canvas_image_missing : R.string.canvas_decode_error);
                stateView.setTextColor(HeimdallUi.semanticColor(
                        getContext(), HeimdallUi.SEMANTIC_ERROR));
                stateView.setVisibility(VISIBLE);
                showControls();
            }
        });
        updatePlayback();
    }

    private LinearLayout createToolbar() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(3), dp(4), dp(3));
        row.setBackground(HeimdallUi.fullscreenToolbarPanel(
                getContext(), HeimdallUi.RADIUS_PANEL));
        slideTitle = new TextView(getContext());
        slideTitle.setTextColor(HeimdallUi.textColor(getContext()));
        slideTitle.setTextSize(13);
        slideTitle.setGravity(Gravity.CENTER_VERTICAL);
        slideTitle.setSingleLine(true);
        updateSlideTitle();
        row.addView(slideTitle, new LinearLayout.LayoutParams(0, -1, 1f));
        if (config.isSlideshow()) {
            row.addView(iconButton(R.drawable.ic_macro_previous,
                    R.string.canvas_slideshow_previous, () -> changeSlide(-1)), iconParams());
            row.addView(iconButton(R.drawable.ic_macro_next,
                    R.string.canvas_slideshow_next, () -> changeSlide(1)), iconParams());
        }
        row.addView(iconButton(R.drawable.ic_refresh, R.string.canvas_reset,
                () -> {
                    if (viewer != null) {
                        viewer.resetZoom();
                    }
                    if (videoViewer != null) {
                        videoViewer.fitImage();
                    }
                }), iconParams());
        if (showEditAction) {
            row.addView(iconButton(R.drawable.ic_edit, R.string.canvas_edit_composition,
                    listener::onEditComposition), iconParams());
        }
        row.addView(iconButton(R.drawable.ic_fullscreen_exit,
                R.string.common_exit_fullscreen, listener::onClose), iconParams());
        return row;
    }

    private void changeSlide(int delta) {
        if (!config.isSlideshow()) return;
        currentSlideIndex = (currentSlideIndex + delta + config.slides.size())
                % config.slides.size();
        if (loadRequest != null) {
            loadRequest.cancel();
            loadRequest = null;
        }
        updateSlideTitle();
        loadMedia();
    }

    private void updateSlideTitle() {
        if (slideTitle == null) return;
        slideTitle.setText(config.isSlideshow()
                ? getResources().getString(R.string.canvas_slideshow_fullscreen_count,
                        currentSlideIndex + 1, config.slides.size())
                : getResources().getString(R.string.canvas_name));
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                && (event.getY() <= dp(34)
                || event.getY() >= getHeight() - dp(34))) {
            showControls();
        }
        return false;
    }

    private ImageButton iconButton(int iconRes, int descriptionRes, Runnable action) {
        ImageButton button = new ImageButton(getContext());
        button.setImageResource(iconRes);
        button.setColorFilter(HeimdallUi.textColor(getContext()));
        button.setContentDescription(getResources().getString(descriptionRes));
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setBackground(HeimdallUi.fullscreenToolbarControl(
                getContext(), HeimdallUi.RADIUS_BUTTON));
        button.setOnClickListener(view -> {
            action.run();
            showControls();
        });
        return button;
    }

    private LinearLayout.LayoutParams iconParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(42));
        params.setMargins(dp(2), 0, dp(2), 0);
        return params;
    }

    private void showControls() {
        setControlsVisible(true);
        removeCallbacks(hideControls);
        postDelayed(hideControls, 3000L);
    }

    private void setControlsVisible(boolean visible) {
        toolbar.setVisibility(visible ? VISIBLE : GONE);
        revealButton.setVisibility(visible ? GONE : VISIBLE);
    }

    private void updatePlayback() {
        if (videoViewer != null) {
            videoViewer.setPlaybackAllowed(
                    playbackAllowed && aggregatedVisible && isAttachedToWindow());
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
