package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

@SuppressLint("ViewConstructor")
final class FullscreenImageViewer extends FrameLayout {
    interface Listener {
        void onClose();
        void onEditComposition();
    }

    private final String assetId;
    private final Listener listener;
    private final LinearLayout toolbar;
    private final ImageButton revealButton;
    private final TextView stateView;
    private final Runnable hideControls = () -> setControlsVisible(false);
    private CanvasImageLoader.Request loadRequest;
    private ZoomableMapView viewer;
    private Bitmap bitmap;

    FullscreenImageViewer(Context context, String assetId, Listener listener) {
        super(context);
        this.assetId = assetId;
        this.listener = listener;
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
        post(this::loadImage);
    }

    @Override
    protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }

    void release() {
        removeCallbacks(hideControls);
        if (loadRequest != null) {
            loadRequest.cancel();
            loadRequest = null;
        }
        if (viewer != null) {
            viewer.setImageDrawable(null);
            viewer = null;
        }
        CanvasImageLoader.recycle(bitmap);
        bitmap = null;
    }

    private void loadImage() {
        loadRequest = CanvasImageLoader.load(getContext(), assetId, 3000,
                new CanvasImageLoader.Callback() {
                    @Override
                    public void onLoaded(Bitmap loaded) {
                        loadRequest = null;
                        bitmap = loaded;
                        viewer = new ZoomableMapView(getContext());
                        viewer.setImageBitmap(bitmap);
                        viewer.setBackgroundColor(0xFF020407);
                        addView(viewer, 0, new LayoutParams(-1, -1));
                        stateView.setVisibility(GONE);
                        bringChildToFront(toolbar);
                        bringChildToFront(revealButton);
                        showControls();
                    }

                    @Override
                    public void onError(CanvasImageLoader.Error error) {
                        loadRequest = null;
                        stateView.setText(error == CanvasImageLoader.Error.MISSING
                                ? R.string.canvas_image_missing : R.string.canvas_decode_error);
                        stateView.setTextColor(HeimdallUi.COLOR_DANGER);
                        showControls();
                    }
                });
    }

    private LinearLayout createToolbar() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(3), dp(4), dp(3));
        row.setBackground(HeimdallUi.isPearl(getContext())
                ? HeimdallUi.cncFlush(getContext(), HeimdallUi.RADIUS_PANEL)
                : HeimdallUi.glass(getContext(), 0xDD111824, 0xEE080C12,
                        HeimdallUi.COLOR_SYSTEM_BORDER_TOP,
                        HeimdallUi.COLOR_SYSTEM_BORDER_BOTTOM,
                        HeimdallUi.RADIUS_PANEL, 2));
        TextView title = new TextView(getContext());
        title.setText(R.string.canvas_name);
        title.setTextColor(HeimdallUi.textColor(getContext()));
        title.setTextSize(13);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setSingleLine(true);
        row.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));
        row.addView(iconButton(R.drawable.ic_refresh, R.string.canvas_reset,
                () -> {
                    if (viewer != null) {
                        viewer.resetZoom();
                    }
                }), iconParams());
        row.addView(iconButton(R.drawable.ic_edit, R.string.canvas_edit_composition,
                listener::onEditComposition), iconParams());
        row.addView(iconButton(R.drawable.ic_fullscreen_exit,
                R.string.common_exit_fullscreen, listener::onClose), iconParams());
        return row;
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
        button.setBackground(HeimdallUi.isPearl(getContext())
                ? HeimdallUi.pearlMenuControl(getContext(),
                        HeimdallUi.RADIUS_BUTTON, false, false)
                : HeimdallUi.glass(getContext(), 0xB20F1622, 0xD0090E16,
                        0x665F7C9A, 0x33344150,
                        HeimdallUi.RADIUS_BUTTON, 1));
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
