package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("ExifInterface")
final class CanvasImageLoader {
    private static final int RUNTIME_MIN_DECODE_SIDE = 256;
    private static final int RUNTIME_MAX_DECODE_SIDE = 2048;
    private static final float RUNTIME_SAMPLE_HEADROOM = 2f;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService DECODE_EXECUTOR = Executors.newSingleThreadExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "heimdall-canvas-decode");
                thread.setDaemon(true);
                return thread;
            });

    enum Error {
        MISSING,
        PLATFORM_UNSUPPORTED,
        DECODE
    }

    interface Callback {
        void onLoaded(DecodedImage image);
        void onError(Error error);
    }

    static final class DecodedImage {
        private final Bitmap bitmap;
        private final Drawable drawable;

        private DecodedImage(Bitmap bitmap, Drawable drawable) {
            this.bitmap = bitmap;
            this.drawable = drawable;
        }

        static DecodedImage still(Bitmap bitmap) {
            return new DecodedImage(bitmap, null);
        }

        static DecodedImage drawable(Drawable drawable) {
            return new DecodedImage(null, drawable);
        }

        Bitmap bitmap() {
            return bitmap;
        }

        Drawable drawable() {
            return drawable;
        }

        boolean isAnimated() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    && drawable instanceof AnimatedImageDrawable;
        }

        void start() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    && drawable instanceof AnimatedImageDrawable) {
                ((AnimatedImageDrawable) drawable).start();
            }
        }

        void stop() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    && drawable instanceof AnimatedImageDrawable) {
                ((AnimatedImageDrawable) drawable).stop();
            }
        }

        void release() {
            stop();
            recycle(bitmap);
        }
    }

    static final class Request {
        private final AtomicBoolean cancelled = new AtomicBoolean();

        void cancel() {
            cancelled.set(true);
        }

        boolean isCancelled() {
            return cancelled.get();
        }
    }

    private CanvasImageLoader() {
    }

    static int runtimeDecodeMaxSide(int viewportWidth, int viewportHeight, float savedZoom) {
        int viewportSide = Math.max(1, Math.max(viewportWidth, viewportHeight));
        float normalizedZoom = savedZoom;
        if (Float.isNaN(normalizedZoom) || Float.isInfinite(normalizedZoom)) {
            normalizedZoom = CanvasConfig.MIN_ZOOM;
        }
        normalizedZoom = Math.max(CanvasConfig.MIN_ZOOM,
                Math.min(CanvasConfig.MAX_ZOOM, normalizedZoom));
        // BitmapFactory samples in coarse steps. Preserve the existing 2x headroom and
        // apply the saved crop zoom on top so the retained Bitmap can cover the visible
        // source region without being enlarged again by CanvasImageView.
        double requested = Math.ceil(viewportSide
                * normalizedZoom * RUNTIME_SAMPLE_HEADROOM);
        return (int) Math.max(RUNTIME_MIN_DECODE_SIDE,
                Math.min(RUNTIME_MAX_DECODE_SIDE, requested));
    }

    static Request load(Context context, String assetId, int maxSide, Callback callback) {
        Request request = new Request();
        Context appContext = context.getApplicationContext();
        int boundedMaxSide = Math.max(64, Math.min(4096, maxSide));
        DECODE_EXECUTOR.execute(() -> {
            File source = CanvasAssetStore.resolve(appContext, assetId);
            if (source == null) {
                deliverError(request, callback, Error.MISSING);
                return;
            }
            CanvasAssetStore.AssetInfo info;
            try {
                info = CanvasAssetStore.inspectStoredAsset(source);
            } catch (IOException ex) {
                deliverError(request, callback, Error.DECODE);
                return;
            }
            if (info.video) {
                deliverError(request, callback, Error.DECODE);
                return;
            }
            if (info.animated && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                deliverError(request, callback, Error.PLATFORM_UNSUPPORTED);
                return;
            }
            DecodedImage image = info.animated
                    ? decodeAnimated(source, info, Math.min(RUNTIME_MAX_DECODE_SIDE,
                            boundedMaxSide))
                    : decodeStill(source, boundedMaxSide);
            if (image == null) {
                deliverError(request, callback, Error.DECODE);
                return;
            }
            MAIN.post(() -> {
                if (request.isCancelled()) {
                    image.release();
                    return;
                }
                callback.onLoaded(image);
            });
        });
        return request;
    }

    static void recycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private static void deliverError(Request request, Callback callback, Error error) {
        MAIN.post(() -> {
            if (!request.isCancelled()) {
                callback.onError(error);
            }
        });
    }

    private static DecodedImage decodeStill(File source, int maxSide) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            options.inSampleSize = 1;
            int largest = Math.max(bounds.outWidth, bounds.outHeight);
            while (largest / options.inSampleSize > maxSide) {
                options.inSampleSize *= 2;
            }
            Bitmap decoded = BitmapFactory.decodeFile(source.getAbsolutePath(), options);
            if (decoded == null) {
                return null;
            }
            Bitmap oriented = applyExifOrientation(source, decoded);
            return oriented == null ? null : DecodedImage.still(oriented);
        } catch (OutOfMemoryError | RuntimeException ex) {
            return null;
        }
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.P)
    private static DecodedImage decodeAnimated(File source, CanvasAssetStore.AssetInfo info,
            int maxSide) {
        try {
            int largest = Math.max(info.width, info.height);
            float scale = Math.min(1f, maxSide / (float) Math.max(1, largest));
            int targetWidth = Math.max(1, Math.round(info.width * scale));
            int targetHeight = Math.max(1, Math.round(info.height * scale));
            Drawable drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(source),
                    (decoder, imageInfo, imageSource) ->
                            decoder.setTargetSize(targetWidth, targetHeight));
            return DecodedImage.drawable(drawable);
        } catch (IOException | OutOfMemoryError | RuntimeException ex) {
            return null;
        }
    }

    private static Bitmap applyExifOrientation(File source, Bitmap bitmap) {
        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            ExifInterface exif = new ExifInterface(source.getAbsolutePath());
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException ignored) {
        }
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1f, -1f);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90f);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90f);
                matrix.postScale(-1f, 1f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90f);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap oriented = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (oriented != bitmap) {
                recycle(bitmap);
            }
            return oriented;
        } catch (OutOfMemoryError | RuntimeException ex) {
            recycle(bitmap);
            return null;
        }
    }
}
