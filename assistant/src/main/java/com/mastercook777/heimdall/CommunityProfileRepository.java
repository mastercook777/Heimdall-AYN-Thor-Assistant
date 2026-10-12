package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

final class CommunityProfileRepository {
    private static final String DIRECTORY = "community_profiles";
    private static final String CATALOG_FILE = "catalog-v1.json";
    private static final long STALE_DOWNLOAD_MS = 24L * 60L * 60L * 1000L;
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 25_000;
    private static final int MAX_REDIRECTS = 5;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3, runnable -> {
        Thread thread = new Thread(runnable, "heimdall-community-profiles");
        thread.setDaemon(true);
        return thread;
    });

    interface CatalogCallback {
        void onCatalog(CommunityProfileCatalog catalog, boolean cached, long fetchedAt);
        void onFailure(boolean cachedAvailable, String detail);
    }

    interface DownloadCallback {
        void onProgress(long downloadedBytes, long totalBytes);
        void onDownloaded(File file, String source);
        void onFailure(String detail);
    }

    interface PreviewCallback {
        void onLoaded(Bitmap bitmap);
        void onFailure();
    }

    static final class Request {
        private final AtomicBoolean cancelled = new AtomicBoolean();

        void cancel() {
            cancelled.set(true);
        }

        boolean isCancelled() {
            return cancelled.get();
        }

        void check() throws IOException {
            if (isCancelled()) {
                throw new IOException("Cancelled");
            }
        }
    }

    private CommunityProfileRepository() {
    }

    static Request loadCatalogAsync(Context context, String catalogUrl,
            CatalogCallback callback) {
        Request request = new Request();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            File catalogFile = new File(new File(appContext.getNoBackupFilesDir(), DIRECTORY),
                    CATALOG_FILE);
            boolean cachedAvailable = false;
            try {
                ensureDirectory(catalogFile.getParentFile());
                if (catalogFile.isFile()) {
                    CommunityProfileCatalog cached = CommunityProfileCatalog.parse(
                            readUtf8(catalogFile, CommunityProfileCatalog.MAX_CATALOG_BYTES));
                    cachedAvailable = true;
                    postCatalog(request, callback, cached, true, catalogFile.lastModified());
                }
            } catch (Exception ignored) {
                catalogFile.delete();
            }
            try {
                request.check();
                byte[] bytes = downloadBytes(catalogUrl,
                        CommunityProfileCatalog.MAX_CATALOG_BYTES, request);
                CommunityProfileCatalog catalog = CommunityProfileCatalog.parse(
                        new String(bytes, StandardCharsets.UTF_8));
                writeAtomic(catalogFile, bytes);
                postCatalog(request, callback, catalog, false, System.currentTimeMillis());
            } catch (Exception ex) {
                boolean finalCachedAvailable = cachedAvailable;
                MAIN.post(() -> {
                    if (!request.isCancelled()) {
                        callback.onFailure(finalCachedAvailable, safeDetail(ex));
                    }
                });
            }
        });
        return request;
    }

    static Request downloadProfileAsync(Context context, CommunityProfileCatalog.Entry entry,
            DownloadCallback callback) {
        Request request = new Request();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            File downloads = new File(new File(appContext.getCacheDir(), DIRECTORY), "downloads");
            try {
                ensureDirectory(downloads);
            } catch (IOException ex) {
                MAIN.post(() -> {
                    if (!request.isCancelled()) {
                        callback.onFailure(safeDetail(ex));
                    }
                });
                return;
            }
            cleanupStale(downloads);
            File completed = new File(downloads, entry.id + "-"
                    + entry.sha256.substring(0, 16) + ProfileBundleStore.FILE_EXTENSION);
            try {
                if (completed.isFile()) {
                    verifyFile(completed, entry.size, entry.sha256);
                    postProgress(request, callback, entry.size, entry.size);
                    postDownloaded(request, callback, completed, "cache");
                    return;
                }
            } catch (Exception ignored) {
                completed.delete();
            }

            String lastFailure = "Download failed";
            for (CommunityProfileCatalog.Download mirror : entry.downloads) {
                File partial = new File(downloads, entry.id + "-" + UUID.randomUUID() + ".part");
                try {
                    postProgress(request, callback, 0L, entry.size);
                    downloadFile(mirror.url, partial, entry.size, entry.sha256,
                            request, callback);
                    if (completed.exists() && !completed.delete()) {
                        throw new IOException("Could not replace cached download");
                    }
                    if (!partial.renameTo(completed)) {
                        throw new IOException("Could not finalize download");
                    }
                    postDownloaded(request, callback, completed, mirror.source);
                    return;
                } catch (Exception ex) {
                    partial.delete();
                    lastFailure = mirror.source + ": " + safeDetail(ex);
                    if (request.isCancelled()) {
                        return;
                    }
                }
            }
            String failure = lastFailure;
            MAIN.post(() -> {
                if (!request.isCancelled()) {
                    callback.onFailure(failure);
                }
            });
        });
        return request;
    }

    static Request loadPreviewAsync(Context context, CommunityProfileCatalog.Entry entry,
            int targetWidth, int targetHeight, PreviewCallback callback) {
        return loadImageAsync(context, entry.previewUrl, entry.previewSha256,
                entry.previewSize, CommunityProfileCatalog.MAX_PREVIEW_BYTES,
                targetWidth, targetHeight, callback);
    }

    static Request loadIconAsync(Context context, CommunityProfileCatalog.Entry entry,
            int targetWidth, int targetHeight, PreviewCallback callback) {
        return loadImageAsync(context, entry.iconUrl, entry.iconSha256,
                entry.iconSize, CommunityProfileCatalog.MAX_ICON_BYTES,
                targetWidth, targetHeight, callback);
    }

    static Request loadScreenshotAsync(Context context, CommunityProfileCatalog.Entry entry,
            int targetWidth, int targetHeight, PreviewCallback callback) {
        return loadImageAsync(context, entry.screenshotUrl, entry.screenshotSha256,
                entry.screenshotSize, CommunityProfileCatalog.MAX_SCREENSHOT_BYTES,
                targetWidth, targetHeight, callback);
    }

    private static Request loadImageAsync(Context context, String url, String expectedSha256,
            long expectedSize, long maxBytes, int targetWidth, int targetHeight,
            PreviewCallback callback) {
        Request request = new Request();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                File previews = new File(cacheDirectory(appContext), "previews");
                ensureDirectory(previews);
                String key = expectedSha256.length() > 0
                        ? expectedSha256 : sha256(url.getBytes(StandardCharsets.UTF_8));
                File cached = new File(previews, key + ".image");
                if (cached.isFile()
                        && (expectedSize > 0L || expectedSha256.length() > 0)) {
                    try {
                        verifyFile(cached, expectedSize, expectedSha256);
                    } catch (IOException ignored) {
                        cached.delete();
                    }
                }
                if (!cached.isFile()) {
                    byte[] bytes = downloadBytes(url, maxBytes, request);
                    if (expectedSize > 0L && bytes.length != expectedSize) {
                        throw new IOException("Image size mismatch");
                    }
                    if (expectedSha256.length() > 0
                            && !expectedSha256.equals(sha256(bytes))) {
                        throw new IOException("Image checksum mismatch");
                    }
                    writeAtomic(cached, bytes);
                }
                request.check();
                Bitmap bitmap;
                try {
                    bitmap = decodeBounded(cached, targetWidth, targetHeight);
                } catch (IOException ex) {
                    cached.delete();
                    throw ex;
                }
                if (bitmap == null) {
                    throw new IOException("Image decode failed");
                }
                MAIN.post(() -> {
                    if (request.isCancelled()) {
                        bitmap.recycle();
                    } else {
                        callback.onLoaded(bitmap);
                    }
                });
            } catch (Exception ex) {
                MAIN.post(() -> {
                    if (!request.isCancelled()) {
                        callback.onFailure();
                    }
                });
            }
        });
        return request;
    }

    static void deleteDownloadedProfile(File file) {
        if (file != null && file.isFile()) {
            file.delete();
        }
    }

    static void verifyFile(File file, long expectedSize, String expectedSha256)
            throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("Missing file");
        }
        if (expectedSize > 0L && file.length() != expectedSize) {
            throw new IOException("Size mismatch");
        }
        if (expectedSha256 != null && expectedSha256.length() > 0) {
            try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
                MessageDigest digest = digest();
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
                if (!expectedSha256.equals(hex(digest.digest()))) {
                    throw new IOException("Checksum mismatch");
                }
            }
        }
    }

    private static void downloadFile(String url, File destination, long expectedSize,
            String expectedSha256, Request request, DownloadCallback callback)
            throws IOException {
        HttpURLConnection connection = open(url);
        try {
            long contentLength = connection.getContentLengthLong();
            if (contentLength > 0L && contentLength != expectedSize) {
                throw new IOException("Unexpected Content-Length");
            }
            MessageDigest digest = digest();
            long total = 0L;
            long progressStep = Math.max(64L * 1024L, expectedSize / 100L);
            long nextProgress = progressStep;
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                    FileOutputStream output = new FileOutputStream(destination)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    request.check();
                    total += read;
                    if (total > expectedSize || total > CommunityProfileCatalog.MAX_BUNDLE_BYTES) {
                        throw new IOException("Download too large");
                    }
                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                    if (total >= nextProgress) {
                        postProgress(request, callback, total, expectedSize);
                        nextProgress = total + progressStep;
                    }
                }
                output.getFD().sync();
            }
            if (total != expectedSize) {
                throw new IOException("Download size mismatch");
            }
            if (!expectedSha256.equals(hex(digest.digest()))) {
                throw new IOException("Download checksum mismatch");
            }
            postProgress(request, callback, expectedSize, expectedSize);
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] downloadBytes(String url, long maxBytes, Request request)
            throws IOException {
        HttpURLConnection connection = open(url);
        try {
            long contentLength = connection.getContentLengthLong();
            if (contentLength > maxBytes) {
                throw new IOException("Response too large");
            }
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                    ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[16 * 1024];
                int read;
                long total = 0L;
                while ((read = input.read(buffer)) != -1) {
                    request.check();
                    total += read;
                    if (total > maxBytes) {
                        throw new IOException("Response too large");
                    }
                    output.write(buffer, 0, read);
                }
                return output.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection open(String initialUrl) throws IOException {
        URL url = new URL(initialUrl);
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            if (!"https".equalsIgnoreCase(url.getProtocol())) {
                throw new IOException("HTTPS required");
            }
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("User-Agent", "Heimdall/" + BuildConfig.VERSION_NAME);
            int code = connection.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null || redirects == MAX_REDIRECTS) {
                    throw new IOException("Invalid redirect");
                }
                url = new URL(url, location);
                continue;
            }
            if (code < 200 || code >= 300) {
                connection.disconnect();
                throw new IOException("HTTP " + code);
            }
            return connection;
        }
        throw new IOException("Too many redirects");
    }

    private static Bitmap decodeBounded(File file, int targetWidth, int targetHeight)
            throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0
                || bounds.outWidth > 8192 || bounds.outHeight > 8192) {
            throw new IOException("Invalid preview dimensions");
        }
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= Math.max(1, targetWidth)
                && bounds.outHeight / (sample * 2) >= Math.max(1, targetHeight)) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    private static File cacheDirectory(Context context) throws IOException {
        File directory = new File(context.getCacheDir(), DIRECTORY);
        ensureDirectory(directory);
        return directory;
    }

    private static void ensureDirectory(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Could not create cache directory");
        }
    }

    private static void cleanupStale(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        long threshold = System.currentTimeMillis() - STALE_DOWNLOAD_MS;
        for (File file : files) {
            if (file.isFile() && file.lastModified() < threshold) {
                file.delete();
            }
        }
    }

    private static String readUtf8(File file, long maxBytes) throws IOException {
        if (file.length() <= 0L || file.length() > maxBytes) {
            throw new IOException("Invalid cached catalog size");
        }
        try (InputStream input = new FileInputStream(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (output.size() + read > maxBytes) {
                    throw new IOException("Cached catalog too large");
                }
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static void writeAtomic(File destination, byte[] bytes) throws IOException {
        ensureDirectory(destination.getParentFile());
        File temporary = new File(destination.getParentFile(),
                destination.getName() + "." + UUID.randomUUID() + ".tmp");
        try {
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(bytes);
                output.getFD().sync();
            }
            try {
                Files.move(temporary.toPath(), destination.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary.toPath(), destination.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (temporary.exists()) {
                temporary.delete();
            }
        }
    }

    private static void postCatalog(Request request, CatalogCallback callback,
            CommunityProfileCatalog catalog, boolean cached, long fetchedAt) {
        MAIN.post(() -> {
            if (!request.isCancelled()) {
                callback.onCatalog(catalog, cached, fetchedAt);
            }
        });
    }

    private static void postDownloaded(Request request, DownloadCallback callback,
            File file, String source) {
        MAIN.post(() -> {
            if (request.isCancelled()) {
                deleteDownloadedProfile(file);
            } else {
                callback.onDownloaded(file, source);
            }
        });
    }

    private static void postProgress(Request request, DownloadCallback callback,
            long downloadedBytes, long totalBytes) {
        MAIN.post(() -> {
            if (!request.isCancelled()) {
                callback.onProgress(downloadedBytes, totalBytes);
            }
        });
    }

    private static MessageDigest digest() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception ex) {
            throw new IOException("SHA-256 unavailable", ex);
        }
    }

    private static String sha256(byte[] bytes) throws IOException {
        return hex(digest().digest(bytes));
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte item : bytes) {
            value.append(String.format(Locale.US, "%02x", item & 0xff));
        }
        return value.toString();
    }

    private static String safeDetail(Exception exception) {
        String message = exception == null ? "" : exception.getMessage();
        return message == null || message.trim().length() == 0
                ? "Request failed" : message.trim();
    }
}
