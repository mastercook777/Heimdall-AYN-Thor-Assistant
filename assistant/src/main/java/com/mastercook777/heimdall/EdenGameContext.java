package com.mastercook777.heimdall;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Strict Eden identity parser backed only by process-local startup logs and Activity state. */
final class EdenGameContext {
    static final String PACKAGE_MAIN = "dev.eden.eden_emulator";
    static final String PACKAGE_NIGHTLY = "dev.eden.eden_emulator.nightly";
    static final String PACKAGE_REL_WITH_DEB_INFO =
            "dev.eden.eden_emulator.relWithDebInfo";
    static final String PACKAGE_LEGACY = "dev.legacy.eden_emulator";
    static final String DETECTOR_ID = "eden_title_id_logcat_activity_v1";
    static final int ACTIVITY_UNKNOWN = 0;
    static final int ACTIVITY_MAIN = 1;
    static final int ACTIVITY_EMULATION = 2;

    private static final String[] PACKAGES = {
            PACKAGE_MAIN, PACKAGE_NIGHTLY, PACKAGE_REL_WITH_DEB_INFO, PACKAGE_LEGACY
    };
    private static final String PROGRAM_ID_MARKER = "ProgramID 0x";
    private static final String TITLE_ID_MARKER = "title ID ";
    private static final String TITLE_LABEL_MARKER =
            "[EmulationFragment] Starting view setup for game: ";

    private EdenGameContext() {
    }

    static boolean supportsPackage(String packageName) {
        if (packageName == null) return false;
        for (String candidate : PACKAGES) {
            if (candidate.equals(packageName)) return true;
        }
        return false;
    }

    static String extractTitleId(String line) {
        if (line == null || !line.contains(" YuzuNative:")) return "";
        int marker;
        if (line.contains(" Loader <Info>")) {
            marker = line.indexOf(PROGRAM_ID_MARKER);
            if (marker >= 0) marker += PROGRAM_ID_MARKER.length();
        } else if (line.contains(" Core <Info>")) {
            marker = line.indexOf(TITLE_ID_MARKER);
            if (marker >= 0) marker += TITLE_ID_MARKER.length();
        } else {
            return "";
        }
        if (marker < 0 || marker + 16 > line.length()) return "";
        String value = line.substring(marker, marker + 16);
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) return "";
        }
        if (marker + 16 < line.length()
                && Character.digit(line.charAt(marker + 16), 16) >= 0) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    static String extractTitleLabel(String line) {
        if (line == null || !line.contains(" YuzuNative:")
                || !line.contains(" Frontend <Info>")) {
            return "";
        }
        int marker = line.indexOf(TITLE_LABEL_MARKER);
        if (marker < 0) return "";
        String value = line.substring(marker + TITLE_LABEL_MARKER.length()).trim();
        StringBuilder clean = new StringBuilder(Math.min(value.length(), 120));
        for (int i = 0; i < value.length() && clean.length() < 120; i++) {
            char item = value.charAt(i);
            if (!Character.isISOControl(item)) clean.append(item);
        }
        return clean.toString().trim();
    }

    static int classifyResumedActivityLine(String packageName, String line) {
        if (!supportsPackage(packageName) || line == null) return ACTIVITY_UNKNOWN;
        String trimmed = line.trim();
        if (!trimmed.startsWith("topResumedActivity=")
                && !trimmed.startsWith("mResumedActivity:")
                && !trimmed.startsWith("Resumed:")) {
            return ACTIVITY_UNKNOWN;
        }
        if (trimmed.contains(packageName
                + "/org.yuzu.yuzu_emu.activities.EmulationActivity")) {
            return ACTIVITY_EMULATION;
        }
        if (trimmed.contains(packageName
                + "/org.yuzu.yuzu_emu.ui.main.MainActivity")) {
            return ACTIVITY_MAIN;
        }
        return ACTIVITY_UNKNOWN;
    }

    static GameContextSnapshot snapshot(String packageName, int pid, String titleId,
            String titleLabel, long observedAt) {
        try {
            if (!supportsPackage(packageName) || pid <= 0) {
                return GameContextSnapshot.UNKNOWN;
            }
            String normalized = titleId == null
                    ? "" : titleId.trim().toLowerCase(Locale.ROOT);
            if (!normalized.matches("[0-9a-f]{16}")) {
                return GameContextSnapshot.UNKNOWN;
            }
            String identity = sha256("eden-title-id\n" + normalized);
            String label = titleLabel == null ? "" : titleLabel.trim();
            if (label.length() == 0) label = normalized.toUpperCase(Locale.ROOT);
            return new GameContextSnapshot(GameContextSnapshot.State.ACTIVE,
                    packageName, pid, GameContextBinding.KIND_EMULATOR_TITLE_ID,
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
}
