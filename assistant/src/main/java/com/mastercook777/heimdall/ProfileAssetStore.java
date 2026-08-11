package com.mastercook777.heimdall;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

final class ProfileAssetStore {
    static final String DIRECTORY = "profile_assets";
    private static final Pattern STORED_NAME = Pattern.compile(
            "^[0-9a-f]{64}\\.(jpg|png|webp|gif|pdf|txt|md|html|htm)$");
    private static final Pattern SUPPORTED_EXTENSION = Pattern.compile(
            "^(jpg|png|webp|gif|pdf|txt|md|html|htm)$");

    private ProfileAssetStore() {
    }

    static synchronized String install(Context context, File source, String expectedSha256,
            String extension, String displayName) throws IOException {
        if (context == null || source == null || !source.isFile()) {
            throw new IOException("Missing staged asset");
        }
        String digest = normalizeDigest(expectedSha256);
        String normalizedExtension = normalizeExtension(extension);
        if (!digest.equals(sha256(source))) {
            throw new IOException("Staged asset checksum mismatch");
        }

        File directory = directory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Unable to create Profile asset storage");
        }
        String storedName = digest + "." + normalizedExtension;
        File destination = new File(directory, storedName);
        assertDirectChild(directory, destination);
        if (destination.isFile()) {
            if (!digest.equals(sha256(destination))) {
                throw new IOException("Existing Profile asset checksum mismatch");
            }
            return ProfileAssetProvider.uriFor(context, storedName, displayName).toString();
        }

        File temporary = new File(directory, ".install-" + UUID.randomUUID() + ".tmp");
        try {
            copy(source, temporary);
            if (!digest.equals(sha256(temporary))) {
                throw new IOException("Installed Profile asset checksum mismatch");
            }
            if (!temporary.renameTo(destination)) {
                throw new IOException("Unable to finalize Profile asset");
            }
        } finally {
            temporary.delete();
        }
        return ProfileAssetProvider.uriFor(context, storedName, displayName).toString();
    }

    static File resolve(Context context, String storedName) throws IOException {
        String normalized = storedName == null
                ? "" : storedName.trim().toLowerCase(Locale.US);
        if (!STORED_NAME.matcher(normalized).matches()) {
            throw new IOException("Invalid Profile asset name");
        }
        File directory = directory(context);
        File file = new File(directory, normalized);
        assertDirectChild(directory, file);
        if (!file.isFile()) {
            throw new IOException("Profile asset not found");
        }
        return file;
    }

    static String sanitizeDisplayName(String value, String fallback) {
        String name = value == null ? "" : value.trim();
        name = name.replace('/', '_').replace('\\', '_');
        StringBuilder clean = new StringBuilder();
        for (int i = 0; i < name.length() && clean.length() < 128; i++) {
            char character = name.charAt(i);
            if (!Character.isISOControl(character)) {
                clean.append(character);
            }
        }
        String result = clean.toString().trim();
        return result.length() == 0 ? fallback : result;
    }

    static String extensionOf(String storedName) {
        int separator = storedName == null ? -1 : storedName.lastIndexOf('.');
        return separator < 0 ? "" : storedName.substring(separator + 1).toLowerCase(Locale.US);
    }

    private static File directory(Context context) {
        return new File(context.getApplicationContext().getFilesDir(), DIRECTORY);
    }

    private static void assertDirectChild(File directory, File file) throws IOException {
        File canonicalDirectory = directory.getCanonicalFile();
        File canonicalFile = file.getCanonicalFile();
        if (!canonicalDirectory.equals(canonicalFile.getParentFile())) {
            throw new IOException("Profile asset path escaped storage");
        }
    }

    private static String normalizeDigest(String value) throws IOException {
        String digest = value == null ? "" : value.trim().toLowerCase(Locale.US);
        if (!digest.matches("^[0-9a-f]{64}$")) {
            throw new IOException("Invalid Profile asset checksum");
        }
        return digest;
    }

    private static String normalizeExtension(String value) throws IOException {
        String extension = value == null ? "" : value.trim().toLowerCase(Locale.US);
        if (!SUPPORTED_EXTENSION.matcher(extension).matches()) {
            throw new IOException("Unsupported Profile asset extension");
        }
        return extension;
    }

    private static void copy(File source, File destination) throws IOException {
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

    static String sha256(File file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-256 unavailable", ex);
        }
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder value = new StringBuilder(64);
        for (byte item : digest.digest()) {
            value.append(String.format(Locale.US, "%02x", item & 0xFF));
        }
        return value.toString();
    }
}
