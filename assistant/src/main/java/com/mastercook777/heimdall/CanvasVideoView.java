package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;

@SuppressLint({"ViewConstructor", "ClickableViewAccessibility"})
final class CanvasVideoView extends FrameLayout implements CanvasCompositionSurface,
        TextureView.SurfaceTextureListener {
    interface Listener {
        void onReady();
        void onError();
    }

    private final TextureView textureView;
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector panDetector;
    private CanvasConfig composition = new CanvasConfig();
    private File sourceFile;
    private Listener listener;
    private MediaPlayer player;
    private Surface surface;
    private boolean interactive;
    private boolean playbackAllowed;
    private boolean aggregatedVisible;
    private boolean prepared;
    private boolean terminalError;
    private boolean readyNotified;
    private boolean resetToFillWhenReady;
    private int videoWidth;
    private int videoHeight;
    private float fitScale = 1f;
    private float currentScale = 1f;
    private float contentX;
    private float contentY;

    CanvasVideoView(Context context) {
        super(context);
        setClipChildren(true);
        setClipToPadding(true);
        textureView = new TextureView(context);
        textureView.setOpaque(false);
        textureView.setSurfaceTextureListener(this);
        textureView.setPivotX(0f);
        textureView.setPivotY(0f);
        addView(textureView, new LayoutParams(1, 1));

        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        if (!isMediaReady()) {
                            return false;
                        }
                        float minimum = Math.max(0.0001f,
                                fitScale * CanvasConfig.MIN_ZOOM);
                        float maximum = Math.max(minimum,
                                fitScale * CanvasConfig.MAX_ZOOM);
                        float target = clamp(currentScale * detector.getScaleFactor(),
                                minimum, maximum);
                        float factor = target / Math.max(0.0001f, currentScale);
                        float focusX = clamp(detector.getFocusX(), 0f, getWidth());
                        float focusY = clamp(detector.getFocusY(), 0f, getHeight());
                        contentX = focusX - (focusX - contentX) * factor;
                        contentY = focusY - (focusY - contentY) * factor;
                        currentScale = target;
                        constrainTransform();
                        updateCompositionFromTransform();
                        applyTransform();
                        return true;
                    }
                });
        panDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent event) {
                        return true;
                    }

                    @Override
                    public boolean onScroll(MotionEvent first, MotionEvent current,
                            float distanceX, float distanceY) {
                        if (scaleDetector.isInProgress() || !isMediaReady()) {
                            return false;
                        }
                        contentX -= distanceX;
                        contentY -= distanceY;
                        constrainTransform();
                        updateCompositionFromTransform();
                        applyTransform();
                        return true;
                    }
                });
    }

    void setSource(CanvasConfig value, Listener valueListener) {
        releasePlayer();
        listener = valueListener;
        composition = value == null ? new CanvasConfig() : value.copy();
        composition.normalize();
        sourceFile = CanvasAssetStore.resolve(getContext(), composition.assetId);
        terminalError = sourceFile == null;
        readyNotified = false;
        prepared = false;
        videoWidth = 0;
        videoHeight = 0;
        if (terminalError) {
            notifyError();
            return;
        }
        updatePlayer();
    }

    @Override
    public void setComposition(CanvasConfig value, boolean resetToFill) {
        composition = value == null ? new CanvasConfig() : value.copy();
        composition.normalize();
        resetToFillWhenReady = resetToFill;
        post(this::applyStoredComposition);
    }

    @Override
    public CanvasConfig composition() {
        updateCompositionFromTransform();
        return composition.copy();
    }

    @Override
    public void setInteractive(boolean value) {
        interactive = value;
    }

    @Override
    public void fitImage() {
        setCenteredZoom(CanvasConfig.MIN_ZOOM);
    }

    @Override
    public void fillImage() {
        if (!isMediaReady()) {
            return;
        }
        float fillScale = Math.max(getWidth() / (float) videoWidth,
                getHeight() / (float) videoHeight);
        setCenteredZoom(clamp(fillScale / Math.max(0.0001f, fitScale),
                CanvasConfig.MIN_ZOOM, CanvasConfig.MAX_ZOOM));
    }

    @Override
    public void resetImage() {
        fillImage();
    }

    void setPlaybackAllowed(boolean allowed) {
        playbackAllowed = allowed;
        updatePlayer();
    }

    void release() {
        playbackAllowed = false;
        releasePlayer();
        listener = null;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updatePlayer();
    }

    @Override
    protected void onDetachedFromWindow() {
        releasePlayer();
        super.onDetachedFromWindow();
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        aggregatedVisible = isVisible;
        updatePlayer();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        if (oldWidth > 0 && oldHeight > 0) {
            updateCompositionFromTransform();
        }
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        applyStoredComposition();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!interactive || !isMediaReady()) {
            return false;
        }
        getParent().requestDisallowInterceptTouchEvent(true);
        scaleDetector.onTouchEvent(event);
        panDetector.onTouchEvent(event);
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            constrainTransform();
            updateCompositionFromTransform();
            applyTransform();
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
            int width, int height) {
        replaceSurface(surfaceTexture);
        updatePlayer();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
            int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        releasePlayer();
        releaseSurface();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    private void updatePlayer() {
        if (shouldPlay()) {
            ensurePlayer();
            if (prepared && player != null && !player.isPlaying()) {
                try {
                    player.start();
                } catch (IllegalStateException ex) {
                    reportError();
                }
            }
        } else {
            // Releasing, rather than merely pausing, makes the one-owner rule explicit:
            // hidden runtime, editor, and fullscreen surfaces cannot decode concurrently.
            releasePlayer();
        }
    }

    private boolean shouldPlay() {
        return playbackAllowed && aggregatedVisible && isAttachedToWindow()
                && sourceFile != null && surface != null && !terminalError;
    }

    private void ensurePlayer() {
        if (player != null || sourceFile == null || surface == null) {
            return;
        }
        MediaPlayer candidate = new MediaPlayer();
        player = candidate;
        prepared = false;
        try {
            candidate.setDataSource(sourceFile.getAbsolutePath());
            candidate.setSurface(surface);
            candidate.setVolume(0f, 0f);
            candidate.setLooping(true);
            candidate.setScreenOnWhilePlaying(false);
            candidate.setOnVideoSizeChangedListener((mediaPlayer, width, height) -> {
                if (player == candidate && width > 0 && height > 0) {
                    setVideoSize(width, height);
                }
            });
            candidate.setOnPreparedListener(mediaPlayer -> {
                if (player != candidate) {
                    return;
                }
                prepared = true;
                int width = mediaPlayer.getVideoWidth();
                int height = mediaPlayer.getVideoHeight();
                if (width <= 0 || height <= 0) {
                    reportError();
                    return;
                }
                setVideoSize(width, height);
                notifyReady();
                if (shouldPlay()) {
                    try {
                        mediaPlayer.start();
                    } catch (IllegalStateException ex) {
                        reportError();
                    }
                }
            });
            candidate.setOnErrorListener((mediaPlayer, what, extra) -> {
                if (player == candidate) {
                    reportError();
                }
                return true;
            });
            candidate.prepareAsync();
        } catch (IOException | RuntimeException ex) {
            reportError();
        }
    }

    private void setVideoSize(int width, int height) {
        if (width == videoWidth && height == videoHeight) {
            return;
        }
        videoWidth = width;
        videoHeight = height;
        LayoutParams params = (LayoutParams) textureView.getLayoutParams();
        params.width = Math.max(1, width);
        params.height = Math.max(1, height);
        textureView.setLayoutParams(params);
        post(this::applyStoredComposition);
    }

    private void applyStoredComposition() {
        if (!isMediaReady()) {
            return;
        }
        fitScale = Math.min(getWidth() / (float) videoWidth,
                getHeight() / (float) videoHeight);
        if (resetToFillWhenReady) {
            resetToFillWhenReady = false;
            fillImage();
            return;
        }
        currentScale = fitScale * clamp(composition.zoom,
                CanvasConfig.MIN_ZOOM, CanvasConfig.MAX_ZOOM);
        contentX = getWidth() * 0.5f
                - composition.focusX * videoWidth * currentScale;
        contentY = getHeight() * 0.5f
                - composition.focusY * videoHeight * currentScale;
        constrainTransform();
        updateCompositionFromTransform();
        applyTransform();
    }

    private void setCenteredZoom(float zoom) {
        if (!isMediaReady()) {
            return;
        }
        composition.focusX = 0.5f;
        composition.focusY = 0.5f;
        composition.zoom = clamp(zoom, CanvasConfig.MIN_ZOOM, CanvasConfig.MAX_ZOOM);
        applyStoredComposition();
    }

    private void constrainTransform() {
        if (!isMediaReady()) {
            return;
        }
        float width = videoWidth * currentScale;
        float height = videoHeight * currentScale;
        if (width <= getWidth()) {
            contentX = (getWidth() - width) * 0.5f;
        } else {
            contentX = clamp(contentX, getWidth() - width, 0f);
        }
        if (height <= getHeight()) {
            contentY = (getHeight() - height) * 0.5f;
        } else {
            contentY = clamp(contentY, getHeight() - height, 0f);
        }
    }

    private void updateCompositionFromTransform() {
        if (!isMediaReady() || fitScale <= 0f || currentScale <= 0f) {
            return;
        }
        composition.focusX = clamp((getWidth() * 0.5f - contentX)
                / (videoWidth * currentScale), 0f, 1f);
        composition.focusY = clamp((getHeight() * 0.5f - contentY)
                / (videoHeight * currentScale), 0f, 1f);
        composition.zoom = clamp(currentScale / fitScale,
                CanvasConfig.MIN_ZOOM, CanvasConfig.MAX_ZOOM);
    }

    private void applyTransform() {
        textureView.setScaleX(currentScale);
        textureView.setScaleY(currentScale);
        textureView.setTranslationX(contentX);
        textureView.setTranslationY(contentY);
        invalidate();
    }

    private boolean isMediaReady() {
        return videoWidth > 0 && videoHeight > 0 && getWidth() > 0 && getHeight() > 0;
    }

    private void notifyReady() {
        if (!readyNotified && listener != null) {
            readyNotified = true;
            listener.onReady();
        }
    }

    private void notifyError() {
        if (listener != null) {
            listener.onError();
        }
    }

    private void reportError() {
        terminalError = true;
        releasePlayer();
        notifyError();
    }

    private void replaceSurface(SurfaceTexture surfaceTexture) {
        releaseSurface();
        surface = new Surface(surfaceTexture);
    }

    private void releasePlayer() {
        MediaPlayer current = player;
        player = null;
        prepared = false;
        if (current == null) {
            return;
        }
        current.setOnPreparedListener(null);
        current.setOnVideoSizeChangedListener(null);
        current.setOnErrorListener(null);
        try {
            current.release();
        } catch (RuntimeException ignored) {
        }
    }

    private void releaseSurface() {
        if (surface != null) {
            surface.release();
            surface = null;
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
