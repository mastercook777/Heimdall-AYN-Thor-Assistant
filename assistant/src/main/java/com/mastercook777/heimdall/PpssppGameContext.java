package com.mastercook777.heimdall;

import android.net.Uri;
import android.provider.DocumentsContract;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Positive-launch-only Game Context for official PPSSPP Android packages. */
final class PpssppGameContext {
    static final String PACKAGE_FREE = "org.ppsspp.ppsspp";
    static final String PACKAGE_GOLD = "org.ppsspp.ppssppgold";
    static final String DETECTOR_ID = "ppsspp_logcat_positive_launch_v1";

    private static final Pattern BOOT_LINE = Pattern.compile(
            "^\\s*\\d+\\.\\d+\\s+\\d+\\s+\\d+\\s+I\\s+PPSSPP\\s*:\\s*"
                    + "\\[BOOT\\]\\s+Booted\\s+(content://\\S+)\\s*$");

    private PpssppGameContext() {
    }

    static boolean supportsPackage(String packageName) {
        return PACKAGE_FREE.equals(packageName) || PACKAGE_GOLD.equals(packageName);
    }

    static String extractUri(String line) {
        if (line == null) return "";
        Matcher matcher = BOOT_LINE.matcher(line);
        if (!matcher.matches()) return "";
        String value = matcher.group(1);
        if (value.endsWith("...")) value = value.substring(0, value.length() - 3);
        return value.startsWith("content://") ? value : "";
    }

    static GameContextSnapshot snapshot(String packageName, int pid, String rawUri,
            long observedAt) {
        try {
            if (!supportsPackage(packageName) || pid <= 0) {
                return GameContextSnapshot.UNKNOWN;
            }
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
                    packageName, pid, GameContextBinding.KIND_SAF_DOCUMENT,
                    identity, label, observedAt, DETECTOR_ID);
        } catch (Throwable ignored) {
            return GameContextSnapshot.UNKNOWN;
        }
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte item : digest) {
            result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
        }
        return result.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
