package com.mastercook777.heimdall;

import android.net.Uri;
import android.provider.DocumentsContract;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

final class AetherSx2GameContext {
    static final String PACKAGE_NAME = "xyz.aethersx2.android";
    static final String DETECTOR_ID = "aethersx2_logcat_v1";
    private static final String LOG_MARKER =
            "EmulationThread: Starting emulation thread (";

    private AetherSx2GameContext() {
    }

    static String extractUri(String line) {
        if (line == null) return "";
        int marker = line.indexOf(LOG_MARKER);
        if (marker < 0) return "";
        int start = marker + LOG_MARKER.length();
        int end = line.indexOf(')', start);
        if (end <= start) return "";
        String value = line.substring(start, end).trim();
        return value.startsWith("content://") ? value : "";
    }

    static GameContextSnapshot snapshot(int pid, String rawUri, long observedAt) {
        try {
            if (pid <= 0) return GameContextSnapshot.UNKNOWN;
            Uri uri = Uri.parse(rawUri);
            String authority = safe(uri.getAuthority());
            if (!"content".equals(uri.getScheme()) || authority.length() == 0) {
                return GameContextSnapshot.UNKNOWN;
            }
            String documentId = DocumentsContract.getDocumentId(uri);
            if (documentId == null || documentId.trim().length() == 0) {
                return GameContextSnapshot.UNKNOWN;
            }
            String identity = sha256(authority + "\n" + documentId);
            String label = documentId;
            int slash = label.lastIndexOf('/');
            if (slash >= 0 && slash + 1 < label.length()) label = label.substring(slash + 1);
            int colon = label.lastIndexOf(':');
            if (colon >= 0 && colon + 1 < label.length()) label = label.substring(colon + 1);
            return new GameContextSnapshot(GameContextSnapshot.State.ACTIVE,
                    PACKAGE_NAME, pid, GameContextBinding.KIND_SAF_DOCUMENT,
                    identity, label, observedAt, DETECTOR_ID);
        } catch (Throwable ignored) {
            return GameContextSnapshot.UNKNOWN;
        }
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte item : digest) result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
        return result.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
