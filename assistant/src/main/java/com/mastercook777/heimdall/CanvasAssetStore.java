package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
    private static final long MAX_SOURCE_BYTES = 64L * 1024L * 1024L;
    private static final long MAX_SOURCE_PIXELS = 100_000_000L;
    private static final int MAX_SOURCE_SIDE = 32_768;
    private static final Pattern ASSET_ID = Pattern.compile(
            "^[0-9a-f]{64}\\.(jpg|png|webp)$");
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
        ANIMATED,
        DECODE,
        STORAGE
    }

    interface ImportCallback {
        void onImported(String assetId);
        void onError(ImportError error);
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
            String assetId = null;
            ImportError error = null;
            try {
                assetId = importImage(appContext, uri);
            } catch (ImportException ex) {
                error = ex.error;
            } catch (Exception ex) {
                error = ImportError.STORAGE;
            }
            String finalAssetId = assetId;
            ImportError finalError = error;
            MAIN.post(() -> {
                if (request.isCancelled()) {
                    return;
                }
                if (finalAssetId != null) {
                    callback.onImported(finalAssetId);
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
        if (source == null || !source.isFile()
                || source.length() <= 0L || source.length() > MAX_SOURCE_BYTES) {
            throw new IOException("Invalid bundled Canvas asset");
        }
        String requestedExtension = expectedExtension == null
                ? "" : expectedExtension.trim().toLowerCase(Locale.US);
        try {
            String detectedExtension = detectStaticFormat(source);
            validateBounds(source);
            if (!detectedExtension.equals(requestedExtension)) {
                throw new IOException("Bundled Canvas format mismatch");
            }
        } catch (ImportException ex) {
            throw new IOException("Unsupported bundled Canvas asset", ex);
        }
    }

    private static String importImage(Context context, Uri uri) throws ImportException {
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

        String extension;
        try {
            extension = detectStaticFormat(temporary);
            validateBounds(temporary);
        } catch (ImportException ex) {
            temporary.delete();
            throw ex;
        }

        String assetId = hex(digest.digest()) + "." + extension;
        File destination = new File(directory, assetId);
        if (destination.isFile()) {
            temporary.delete();
            return assetId;
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete();
            throw new ImportException(ImportError.STORAGE);
        }
        return assetId;
    }

    private static String detectStaticFormat(File file) throws ImportException {
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
            return "jpg";
        }
        if (isPngHeader(header)) {
            if (containsPngAnimation(file)) {
                throw new ImportException(ImportError.ANIMATED);
            }
            return "png";
        }
        if (asciiEquals(header, 0, "RIFF") && asciiEquals(header, 8, "WEBP")) {
            if (containsWebpAnimation(file)) {
                throw new ImportException(ImportError.ANIMATED);
            }
            return "webp";
        }
        if (asciiEquals(header, 0, "GIF8")) {
            throw new ImportException(ImportError.ANIMATED);
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

    private static void validateBounds(File file) throws ImportException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        long pixels = (long) options.outWidth * (long) options.outHeight;
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw new ImportException(ImportError.DECODE);
        }
        if (options.outWidth > MAX_SOURCE_SIDE || options.outHeight > MAX_SOURCE_SIDE
                || pixels > MAX_SOURCE_PIXELS) {
            throw new ImportException(ImportError.TOO_LARGE);
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
}
