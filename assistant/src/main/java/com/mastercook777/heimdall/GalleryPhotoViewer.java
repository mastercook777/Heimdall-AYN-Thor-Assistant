package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Read-only, in-app viewer for a MediaStore screenshot. */
@SuppressLint("ViewConstructor")
final class GalleryPhotoViewer extends FrameLayout {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private final Uri uri;
    private final Runnable close;
    private ZoomableMapView image;
    private Bitmap bitmap;
    private boolean released;

    GalleryPhotoViewer(Context context, Uri uri, Runnable close) {
        super(context);
        this.uri = uri;
        this.close = close;
        setBackgroundColor(0xFF020407);

        TextView loading = new TextView(context);
        loading.setText(R.string.gallery_loading);
        loading.setTextColor(HeimdallUi.mutedTextColor(context));
        loading.setTextSize(13);
        loading.setGravity(Gravity.CENTER);
        addView(loading, new LayoutParams(-1, -1));

        LinearLayout toolbar = new LinearLayout(context);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(3), dp(4), dp(3));
        toolbar.setBackground(HeimdallUi.fullscreenToolbarPanel(context,
                HeimdallUi.RADIUS_PANEL));
        TextView title = new TextView(context);
        title.setText(R.string.gallery_screenshots);
        title.setTextColor(HeimdallUi.textColor(context));
        title.setTextSize(13);
        title.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, -1, 1));
        toolbar.addView(tool(R.drawable.ic_refresh, R.string.map_reset_zoom,
                () -> { if (image != null) image.resetZoom(); }));
        toolbar.addView(tool(R.drawable.ic_fullscreen_exit,
                R.string.common_exit_fullscreen, close));
        LayoutParams toolbarParams = new LayoutParams(-1, dp(50), Gravity.TOP);
        toolbarParams.setMargins(dp(8), dp(8), dp(8), 0);
        addView(toolbar, toolbarParams);
        post(() -> load(loading, toolbar));
    }

    private ImageButton tool(int icon, int label, Runnable action) {
        ImageButton button = new ImageButton(getContext());
        button.setImageResource(icon);
        button.setColorFilter(HeimdallUi.textColor(getContext()));
        button.setContentDescription(getResources().getString(label));
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setBackground(HeimdallUi.fullscreenToolbarControl(getContext(),
                HeimdallUi.RADIUS_BUTTON));
        button.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(42), dp(42));
        params.setMargins(dp(2), 0, dp(2), 0);
        button.setLayoutParams(params);
        return button;
    }

    private void load(TextView loading, LinearLayout toolbar) {
        Context app = getContext().getApplicationContext();
        new Thread(() -> {
            Bitmap decoded = GalleryThumbnailLoader.decode(app, uri, 3000);
            MAIN.post(() -> {
                if (released || !isAttachedToWindow()) {
                    if (decoded != null) decoded.recycle();
                    return;
                }
                if (decoded == null) {
                    loading.setText(R.string.gallery_image_unavailable);
                    return;
                }
                bitmap = decoded;
                image = new ZoomableMapView(getContext());
                image.setImageBitmap(decoded);
                addView(image, 1, new LayoutParams(-1, -1));
                toolbar.bringToFront();
                loading.setVisibility(GONE);
            });
        }, "heimdall-gallery-photo").start();
    }

    void release() {
        released = true;
        if (image != null) {
            image.setImageDrawable(null);
            image = null;
        }
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
