package com.mastercook777.heimdall;

import android.net.Uri;
import android.provider.DocumentsContract;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

final class AetherSx2GameContext {
    static final String PACKAGE_NAME = "xyz.aethersx2.android";
    static final String DETECTOR_ID = "aethersx2_logcat_activity_v4";
    static final int ACTIVITY_UNKNOWN = 0;
    static final int ACTIVITY_MAIN = 1;
    static final int ACTIVITY_EMULATION = 2;
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

    static long extractObservedAt(String line) {
        if (line == null) return 0L;
        String trimmed = line.trim();
        int tokenEnd = trimmed.indexOf(' ');
        if (tokenEnd <= 0) return 0L;
        String token = trimmed.substring(0, tokenEnd);
        int decimal = token.indexOf('.');
        if (decimal <= 0) return 0L;
        try {
            long seconds = Long.parseLong(token.substring(0, decimal));
            String fraction = token.substring(decimal + 1);
            if (fraction.length() == 0) return 0L;
            String millisText = (fraction + "000").substring(0, 3);
            return Math.addExact(Math.multiplyExact(seconds, 1000L),
                    Long.parseLong(millisText));
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    static int classifyResumedActivityLine(String line) {
        if (line == null) return ACTIVITY_UNKNOWN;
        String trimmed = line.trim();
        if (!trimmed.startsWith("topResumedActivity=")
                && !trimmed.startsWith("mResumedActivity:")
                && !trimmed.startsWith("Resumed:")) {
            return ACTIVITY_UNKNOWN;
        }
        if (containsActivity(trimmed, "EmulationActivity")) return ACTIVITY_EMULATION;
        if (containsActivity(trimmed, "MainActivity")) return ACTIVITY_MAIN;
        return ACTIVITY_UNKNOWN;
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

    private static boolean containsActivity(String line, String activity) {
        return line.contains(PACKAGE_NAME + "/." + activity)
                || line.contains(PACKAGE_NAME + "/" + PACKAGE_NAME + "." + activity);
    }
}
