package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.view.View;

import java.io.InputStream;
import java.util.Locale;

public final class ProfileIconView extends View {
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect source = new Rect();
    private final RectF destination = new RectF();
    private final Path clipPath = new Path();
    private Bitmap bitmap;
    private String fallback = "H";
    private String loadedUri = "";
    private boolean profileSet;

    public ProfileIconView(Context context) {
        super(context);
        setBackground(HeimdallUi.isPearl(context)
                ? HeimdallUi.cncFlush(context, HeimdallUi.RADIUS_CARD)
                : HeimdallUi.glass(context, 0xB2131B27, 0xD0080D14,
                        0xAA70B7FF, 0x55445A72, HeimdallUi.RADIUS_CARD, 1));
        textPaint.setColor(HeimdallUi.isPearl(context) ? 0xFF344457 : 0xFFD7EEFF);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setProfile(GameProfile profile) {
        String name = profile == null ? "" : profile.name;
        String trimmed = name == null ? "" : name.trim();
        String nextFallback = trimmed.length() == 0 ? "H" : trimmed.substring(0, 1).toUpperCase(Locale.US);
        String nextUri = profile == null || profile.iconUri == null ? "" : profile.iconUri.trim();
        if (profileSet && nextFallback.equals(fallback) && nextUri.equals(loadedUri)) {
            return;
        }
        profileSet = true;
        fallback = nextFallback;
        setContentDescription(getContext().getString(R.string.profile_icon_content_description,
                trimmed.length() == 0
                        ? getContext().getString(R.string.common_profile_fallback) : trimmed));
        load(nextUri);
    }

    @Override
    public void invalidate() {
        DebugPerformanceDiagnostics.countInvalidate("Profile icon");
        super.invalidate();
    }

    @Override
    public void requestLayout() {
        DebugPerformanceDiagnostics.countRequestLayout("Profile icon");
        super.requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        DebugPerformanceDiagnostics.countDraw("Profile icon");
        super.onDraw(canvas);
        boolean pearl = HeimdallUi.isPearl(getContext());
        float inset = dp(pearl ? 0 : 2);
        destination.set(inset, inset, getWidth() - inset, getHeight() - inset);
        float radius = dp(pearl ? HeimdallUi.RADIUS_CARD : 8);
        if (bitmap != null && !bitmap.isRecycled()) {
            cropSource(bitmap, destination.width() / Math.max(1f, destination.height()));
            if (DebugPerformanceDiagnostics.isFlatUi()) {
                canvas.drawBitmap(bitmap, source, destination, imagePaint);
            } else {
                int save = canvas.save();
                clipPath.reset();
                clipPath.addRoundRect(destination, radius, radius, Path.Direction.CW);
                canvas.clipPath(clipPath);
                canvas.drawBitmap(bitmap, source, destination, imagePaint);
                canvas.restoreToCount(save);
            }
        } else {
            textPaint.setTextSize(Math.max(dp(12), Math.min(getWidth(), getHeight()) * 0.42f));
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(fallback, getWidth() / 2f, baseline, textPaint);
        }
        if (pearl) {
            float stroke = dp(1f);
            RectF edge = new RectF(stroke / 2f, stroke / 2f,
                    getWidth() - stroke / 2f, getHeight() - stroke / 2f);
            edgePaint.setStyle(Paint.Style.STROKE);
            edgePaint.setStrokeWidth(stroke);
            edgePaint.setColor(0x7078858F);
            float edgeRadius = dp(HeimdallUi.RADIUS_CARD) - stroke / 2f;
            canvas.drawRoundRect(edge, edgeRadius, edgeRadius, edgePaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        recycleBitmap();
        super.onDetachedFromWindow();
    }

    private void load(String uriString) {
        loadedUri = uriString == null ? "" : uriString.trim();
        recycleBitmap();
        if (uriString == null || uriString.trim().length() == 0) {
            invalidate();
            return;
        }
        try {
            Uri uri = Uri.parse(uriString.trim());
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            InputStream boundsStream = getContext().getContentResolver().openInputStream(uri);
            if (boundsStream != null) {
                BitmapFactory.decodeStream(boundsStream, null, bounds);
                boundsStream.close();
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            int largest = Math.max(bounds.outWidth, bounds.outHeight);
            while (largest / options.inSampleSize > 256) {
                options.inSampleSize *= 2;
            }
            InputStream stream = getContext().getContentResolver().openInputStream(uri);
            if (stream != null) {
                bitmap = BitmapFactory.decodeStream(stream, null, options);
                stream.close();
            }
        } catch (Exception ignored) {
            bitmap = null;
        }
        invalidate();
    }

    private void cropSource(Bitmap image, float targetRatio) {
        int width = image.getWidth();
        int height = image.getHeight();
        float imageRatio = width / (float) Math.max(1, height);
        if (imageRatio > targetRatio) {
            int cropWidth = Math.max(1, Math.round(height * targetRatio));
            int left = (width - cropWidth) / 2;
            source.set(left, 0, left + cropWidth, height);
        } else {
            int cropHeight = Math.max(1, Math.round(width / Math.max(0.01f, targetRatio)));
            int top = (height - cropHeight) / 2;
            source.set(0, top, width, top + cropHeight);
        }
    }

    private void recycleBitmap() {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        bitmap = null;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
