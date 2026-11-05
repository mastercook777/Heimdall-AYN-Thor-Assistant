package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded, recycled-cell-safe thumbnail loading for Gallery media. */
final class GalleryThumbnailLoader {
    interface Callback {
        void onFinished(boolean available);
    }

    private static final int MAX_SIDE = 512;
    private static final int CACHE_KIB = 16 * 1024;
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(CACHE_KIB) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return Math.max(1, value.getByteCount() / 1024);
        }
    };

    private GalleryThumbnailLoader() {
    }

    static void load(Context context, Uri uri, ImageView target, Callback callback) {
        target.setImageDrawable(null);
        String key = uri == null ? "" : uri.toString();
        target.setTag(key);
        if (key.isEmpty()) {
            callback.onFinished(false);
            return;
        }
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            target.setImageBitmap(cached);
            callback.onFinished(true);
            return;
        }
        Context app = context.getApplicationContext();
        WORKERS.execute(() -> {
            Bitmap decoded = decode(app, uri, MAX_SIDE);
            if (decoded == null) {
                MAIN.post(() -> {
                    if (key.equals(target.getTag())) {
                        callback.onFinished(false);
                    }
                });
                return;
            }
            MAIN.post(() -> {
                Bitmap retained = CACHE.get(key);
                if (retained == null) {
                    CACHE.put(key, decoded);
                    retained = decoded;
                } else {
                    decoded.recycle();
                }
                if (key.equals(target.getTag())) {
                    target.setImageBitmap(retained);
                    callback.onFinished(true);
                }
            });
        });
    }

    static Bitmap decode(Context context, Uri uri, int maxSide) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) return null;
                BitmapFactory.decodeStream(input, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            BitmapFactory.Options sampled = new BitmapFactory.Options();
            sampled.inSampleSize = 1;
            while (Math.max(bounds.outWidth / sampled.inSampleSize,
                    bounds.outHeight / sampled.inSampleSize) > maxSide) {
                sampled.inSampleSize *= 2;
            }
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                return input == null ? null : BitmapFactory.decodeStream(input, null, sampled);
            }
        } catch (Exception ignored) {
            return null;
        }
    }
}
