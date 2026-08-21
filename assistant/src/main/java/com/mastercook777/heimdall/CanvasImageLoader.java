package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
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
        DECODE
    }

    interface Callback {
        void onLoaded(Bitmap bitmap);
        void onError(Error error);
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
            Bitmap bitmap = decode(source, boundedMaxSide);
            if (bitmap == null) {
                deliverError(request, callback, Error.DECODE);
                return;
            }
            MAIN.post(() -> {
                if (request.isCancelled()) {
                    recycle(bitmap);
                    return;
                }
                callback.onLoaded(bitmap);
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

    private static Bitmap decode(File source, int maxSide) {
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
            return applyExifOrientation(source, decoded);
        } catch (OutOfMemoryError | RuntimeException ex) {
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
