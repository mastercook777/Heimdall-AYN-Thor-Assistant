package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

final class CanvasAssetStore {
    private static final String DIRECTORY = "canvas_assets";
    private static final long MAX_SOURCE_BYTES = 50L * 1024L * 1024L;
    private static final int MAX_SOURCE_SIDE = 4096;
    private static final int MAX_VIDEO_SIDE = 2048;
    private static final Pattern ASSET_ID = Pattern.compile(
            "^[0-9a-f]{64}\\.(jpg|png|webp|gif|mp4)$");
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IMPORT_EXECUTOR = Executors.newSingleThreadExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "heimdall-canvas-import");
                thread.setDaemon(true);
                return thread;
            });

    enum ImportError {
        UNAVAILABLE,
        TOO_LARGE,
        UNSUPPORTED,
        APNG_UNSUPPORTED,
        PLATFORM_UNSUPPORTED,
        DECODE,
        STORAGE
    }

    interface ImportCallback {
        void onImported(ImportedAsset asset);
        void onError(ImportError error);
    }

    static final class ImportedAsset {
        final String assetId;
        final boolean animated;
        final boolean video;

        ImportedAsset(String assetId, boolean animated, boolean video) {
            this.assetId = assetId;
            this.animated = animated;
            this.video = video;
        }
    }

    static final class AssetInfo {
        final String extension;
        final boolean animated;
        final boolean video;
        final int width;
        final int height;

        AssetInfo(String extension, boolean animated, boolean video, int width, int height) {
            this.extension = extension;
            this.animated = animated;
            this.video = video;
            this.width = width;
            this.height = height;
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

    private CanvasAssetStore() {
    }

    static Request importAsync(Context context, Uri uri, ImportCallback callback) {
        Request request = new Request();
        Context appContext = context.getApplicationContext();
        IMPORT_EXECUTOR.execute(() -> {
            ImportedAsset importedAsset = null;
            ImportError error = null;
            try {
                importedAsset = importMedia(appContext, uri);
            } catch (ImportException ex) {
                error = ex.error;
            } catch (Exception ex) {
                error = ImportError.STORAGE;
            }
            ImportedAsset finalAsset = importedAsset;
            ImportError finalError = error;
            MAIN.post(() -> {
                if (request.isCancelled()) {
                    return;
                }
                if (finalAsset != null) {
                    callback.onImported(finalAsset);
                } else {
                    callback.onError(finalError == null ? ImportError.STORAGE : finalError);
                }
            });
        });
        return request;
    }

    static File resolve(Context context, String assetId) {
        String normalized = assetId == null ? "" : assetId.trim().toLowerCase(Locale.US);
        if (!ASSET_ID.matcher(normalized).matches()) {
            return null;
        }
        File directory = assetDirectory(context);
        File file = new File(directory, normalized);
        try {
            if (!file.getCanonicalFile().getParentFile().equals(directory.getCanonicalFile())) {
                return null;
            }
        } catch (IOException ex) {
            return null;
        }
        return file.isFile() ? file : null;
    }

    static synchronized String installBundledAsset(Context context, File source,
            String expectedSha256, String expectedExtension) throws IOException {
        validateBundledAsset(source, expectedExtension);
        if (context == null) {
            throw new IOException("Missing Canvas storage context");
        }
        String digest = expectedSha256 == null
                ? "" : expectedSha256.trim().toLowerCase(Locale.US);
        if (!digest.matches("^[0-9a-f]{64}$")
                || !digest.equals(ProfileAssetStore.sha256(source))) {
            throw new IOException("Bundled Canvas checksum mismatch");
        }
        String extension = expectedExtension.trim().toLowerCase(Locale.US);

        File directory = assetDirectory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create Canvas storage");
        }
        String assetId = digest + "." + extension;
        File destination = new File(directory, assetId);
        if (destination.isFile()) {
            if (!digest.equals(ProfileAssetStore.sha256(destination))) {
                throw new IOException("Existing Canvas checksum mismatch");
            }
            return assetId;
        }
        File temporary = new File(directory, ".bundle-" + UUID.randomUUID() + ".tmp");
        try {
            copyFile(source, temporary);
            if (!digest.equals(ProfileAssetStore.sha256(temporary))
                    || !temporary.renameTo(destination)) {
                throw new IOException("Unable to install bundled Canvas asset");
            }
        } finally {
            temporary.delete();
        }
        return assetId;
    }

    static void validateBundledAsset(File source, String expectedExtension) throws IOException {
        inspectBundledAsset(source, expectedExtension);
    }

    static AssetInfo inspectStoredAsset(File source) throws IOException {
        if (source == null || !source.isFile() || source.length() <= 0L) {
            throw new IOException("Missing Canvas asset");
        }
        try {
            return inspect(source, false);
        } catch (ImportException ex) {
            throw new IOException("Invalid Canvas asset", ex);
        }
    }

    static AssetInfo inspectBundledAsset(File source, String expectedExtension)
            throws IOException {
        if (source == null || !source.isFile() || source.length() <= 0L) {
            throw new IOException("Invalid bundled Canvas asset");
        }
        String requestedExtension = expectedExtension == null
                ? "" : expectedExtension.trim().toLowerCase(Locale.US);
        try {
            AssetInfo info = inspect(source, true);
            if (!info.extension.equals(requestedExtension)) {
                throw new IOException("Bundled Canvas format mismatch");
            }
            return info;
        } catch (ImportException ex) {
            throw new IOException("Unsupported bundled Canvas asset", ex);
        }
    }

    private static ImportedAsset importMedia(Context context, Uri uri) throws ImportException {
        if (uri == null) {
            throw new ImportException(ImportError.UNAVAILABLE);
        }
        File directory = assetDirectory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new ImportException(ImportError.STORAGE);
        }
        File temporary = new File(directory, ".import-" + UUID.randomUUID() + ".tmp");
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new ImportException(ImportError.STORAGE);
        }

        long total = 0L;
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(temporary)) {
            if (input == null) {
                throw new ImportException(ImportError.UNAVAILABLE);
            }
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_SOURCE_BYTES) {
                    throw new ImportException(ImportError.TOO_LARGE);
                }
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
            output.flush();
            output.getFD().sync();
        } catch (ImportException ex) {
            temporary.delete();
            throw ex;
        } catch (IOException | SecurityException ex) {
            temporary.delete();
            throw new ImportException(ImportError.UNAVAILABLE);
        }

        if (total == 0L) {
            temporary.delete();
            throw new ImportException(ImportError.UNAVAILABLE);
        }

        AssetInfo info;
        try {
            info = inspect(temporary, true);
        } catch (ImportException ex) {
            temporary.delete();
            throw ex;
        }

        String assetId = hex(digest.digest()) + "." + info.extension;
        File destination = new File(directory, assetId);
        if (destination.isFile()) {
            temporary.delete();
            return new ImportedAsset(assetId, info.animated, info.video);
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete();
            throw new ImportException(ImportError.STORAGE);
        }
        return new ImportedAsset(assetId, info.animated, info.video);
    }

    private static AssetInfo inspect(File file, boolean enforceImportLimits)
            throws ImportException {
        Format format = detectFormat(file);
        int[] bounds = format.video ? readVideoBounds(file) : readImageBounds(file);
        if (enforceImportLimits && (file.length() > MAX_SOURCE_BYTES
                || (format.video && Math.max(bounds[0], bounds[1]) > MAX_VIDEO_SIDE)
                || (!format.video
                        && (bounds[0] > MAX_SOURCE_SIDE || bounds[1] > MAX_SOURCE_SIDE)))) {
            throw new ImportException(ImportError.TOO_LARGE);
        }
        if (enforceImportLimits && format.animated && !format.video
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            throw new ImportException(ImportError.PLATFORM_UNSUPPORTED);
        }
        return new AssetInfo(format.extension, format.animated, format.video,
                bounds[0], bounds[1]);
    }

    private static Format detectFormat(File file) throws ImportException {
        byte[] header = new byte[12];
        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            input.readFully(header);
        } catch (EOFException ex) {
            throw new ImportException(ImportError.UNSUPPORTED);
        } catch (IOException ex) {
            throw new ImportException(ImportError.UNAVAILABLE);
        }
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return new Format("jpg", false);
        }
        if (isPngHeader(header)) {
            if (containsPngAnimation(file)) {
                throw new ImportException(ImportError.APNG_UNSUPPORTED);
            }
            return new Format("png", false);
        }
        if (asciiEquals(header, 0, "RIFF") && asciiEquals(header, 8, "WEBP")) {
            return new Format("webp", containsWebpAnimation(file));
        }
        if (asciiEquals(header, 0, "GIF8")) {
            return new Format("gif", true, false);
        }
        if (asciiEquals(header, 4, "ftyp")) {
            return new Format("mp4", true, true);
        }
        throw new ImportException(ImportError.UNSUPPORTED);
    }

    private static boolean containsPngAnimation(File file) throws ImportException {
        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            input.skipBytes(8);
            while (true) {
                long length = Integer.toUnsignedLong(input.readInt());
                byte[] typeBytes = new byte[4];
                input.readFully(typeBytes);
                String type = new String(typeBytes, java.nio.charset.StandardCharsets.US_ASCII);
                if ("acTL".equals(type)) {
                    return true;
                }
                if ("IDAT".equals(type) || "IEND".equals(type)) {
                    return false;
                }
                skipFully(input, length + 4L);
            }
        } catch (EOFException ex) {
            throw new ImportException(ImportError.DECODE);
        } catch (IOException ex) {
            throw new ImportException(ImportError.UNAVAILABLE);
        }
    }

    private static boolean containsWebpAnimation(File file) throws ImportException {
        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            skipFully(input, 12L);
            while (true) {
                byte[] typeBytes = new byte[4];
                input.readFully(typeBytes);
                String type = new String(typeBytes, java.nio.charset.StandardCharsets.US_ASCII);
                long length = readUnsignedLittleEndianInt(input);
                if ("ANIM".equals(type) || "ANMF".equals(type)) {
                    return true;
                }
                if ("VP8X".equals(type) && length >= 1L) {
                    int flags = input.readUnsignedByte();
                    if ((flags & 0x02) != 0) {
                        return true;
                    }
                    skipFully(input, length - 1L + (length & 1L));
                } else {
                    skipFully(input, length + (length & 1L));
                }
            }
        } catch (EOFException ex) {
            return false;
        } catch (IOException ex) {
            throw new ImportException(ImportError.UNAVAILABLE);
        }
    }

    private static int[] readImageBounds(File file) throws ImportException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw new ImportException(ImportError.DECODE);
        }
        return new int[]{options.outWidth, options.outHeight};
    }

    private static int[] readVideoBounds(File file) throws ImportException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            int width = parsePositiveInt(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height = parsePositiveInt(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            String mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            if (width <= 0 || height <= 0 || mime == null
                    || !mime.toLowerCase(Locale.US).startsWith("video/")) {
                throw new ImportException(ImportError.DECODE);
            }
            int rotation = parsePositiveInt(retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
            if (rotation == 90 || rotation == 270) {
                int swap = width;
                width = height;
                height = swap;
            }
            return new int[]{width, height};
        } catch (RuntimeException ex) {
            throw new ImportException(ImportError.DECODE);
        } finally {
            try {
                retriever.release();
            } catch (IOException | RuntimeException ignored) {
            }
        }
    }

    private static int parsePositiveInt(String value) {
        try {
            return Math.max(0, Integer.parseInt(value == null ? "" : value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static File assetDirectory(Context context) {
        return new File(context.getApplicationContext().getFilesDir(), DIRECTORY);
    }

    private static void copyFile(File source, File destination) throws IOException {
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
            output.getFD().sync();
        }
    }

    private static boolean isPngHeader(byte[] value) {
        int[] png = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        for (int i = 0; i < png.length; i++) {
            if ((value[i] & 0xFF) != png[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean asciiEquals(byte[] value, int offset, String expected) {
        for (int i = 0; i < expected.length(); i++) {
            if (offset + i >= value.length || value[offset + i] != (byte) expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static long readUnsignedLittleEndianInt(DataInputStream input) throws IOException {
        return (long) input.readUnsignedByte()
                | ((long) input.readUnsignedByte() << 8)
                | ((long) input.readUnsignedByte() << 16)
                | ((long) input.readUnsignedByte() << 24);
    }

    private static void skipFully(DataInputStream input, long count) throws IOException {
        if (count < 0L || count > MAX_SOURCE_BYTES + 16L) {
            throw new EOFException();
        }
        long remaining = count;
        while (remaining > 0L) {
            long skipped = input.skip(remaining);
            if (skipped <= 0L) {
                if (input.read() == -1) {
                    throw new EOFException();
                }
                skipped = 1L;
            }
            remaining -= skipped;
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format(Locale.US, "%02x", value & 0xFF));
        }
        return builder.toString();
    }

    private static final class ImportException extends Exception {
        final ImportError error;

        ImportException(ImportError error) {
            this.error = error;
        }
    }

    private static final class Format {
        final String extension;
        final boolean animated;
        final boolean video;

        Format(String extension, boolean animated) {
            this(extension, animated, false);
        }

        Format(String extension, boolean animated, boolean video) {
            this.extension = extension;
            this.animated = animated;
            this.video = video;
        }
    }
}
