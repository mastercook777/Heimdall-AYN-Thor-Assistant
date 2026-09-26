package com.mastercook777.heimdall;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict current-state Game Context for the official GameNative package. */
final class GameNativeGameContext {
    static final String PACKAGE_NAME = "app.gamenative";
    static final String DETECTOR_ID = "gamenative_logcat_process_v3";
    static final int STATE_UNKNOWN = 0;
    static final int STATE_NONE = 1;
    static final int STATE_ACTIVE = 2;

    private static final String ID_PATTERN = "(STEAM|CUSTOM_GAME|GOG|EPIC)_([1-9][0-9]*)";
    private static final String EPOCH_PREFIX =
            "^\\s*[0-9]+\\.[0-9]+\\s+[0-9]+\\s+[0-9]+\\s+I\\s+";
    private static final Pattern LAUNCH_LINE = Pattern.compile(EPOCH_PREFIX
            + "app\\.gamenative\\s*:\\s*I:\\s*ID:\\s*" + ID_PATTERN + "\\s*$");
    private static final Pattern EXIT_LINE = Pattern.compile(EPOCH_PREFIX
            + "Exit\\s*:\\s*I:\\s*Exiting, getting feedback for appId:\\s*"
            + ID_PATTERN + "\\s*$");
    private static final Pattern GAME_LABEL_LINE = Pattern.compile(EPOCH_PREFIX
            + "XServerScreen\\s*:\\s*I:\\s*Initiated CPU pinning for:\\s*(.+?)\\s*$");
    private static final Pattern CANONICAL_ID = Pattern.compile("^" + ID_PATTERN + "$");

    private GameNativeGameContext() {
    }

    static boolean supportsPackage(String packageName) {
        return PACKAGE_NAME.equals(packageName);
    }

    static String extractLaunchId(String line) {
        return extractId(LAUNCH_LINE, line);
    }

    static String extractExitId(String line) {
        return extractId(EXIT_LINE, line);
    }

    static String extractGameLabel(String line) {
        if (line == null) return "";
        Matcher matcher = GAME_LABEL_LINE.matcher(line);
        if (!matcher.matches()) return "";
        String value = safeLabel(matcher.group(1));
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < value.length()) value = value.substring(slash + 1);
        if (value.toLowerCase(Locale.ROOT).endsWith(".exe")) {
            value = value.substring(0, value.length() - 4);
        }
        return safeLabel(value);
    }

    static boolean isBaselineSameUidCommand(String packageName, String command) {
        if (!supportsPackage(packageName) || command == null) return false;
        String trimmed = command.trim();
        if (trimmed.length() == 0) return false;
        if (trimmed.equals(packageName) || trimmed.startsWith(packageName + ":")) {
            return true;
        }
        int separator = trimmed.indexOf(' ');
        String executable = separator < 0 ? trimmed : trimmed.substring(0, separator);
        return "logcat".equals(executable) || executable.endsWith("/logcat");
    }

    static GameContextSnapshot snapshot(int state, int pid, String rawId,
            String rawLabel, long observedAt) {
        if (pid <= 0) return GameContextSnapshot.UNKNOWN;
        if (state == STATE_NONE) {
            return GameContextSnapshot.none(PACKAGE_NAME, pid, DETECTOR_ID);
        }
        if (state != STATE_ACTIVE) return GameContextSnapshot.UNKNOWN;
        try {
            Matcher matcher = CANONICAL_ID.matcher(rawId == null ? "" : rawId.trim());
            if (!matcher.matches()) return GameContextSnapshot.UNKNOWN;
            String namespace = matcher.group(1);
            String numericId = matcher.group(2);
            String identity = sha256("pc-game-v1\n" + PACKAGE_NAME + "\n"
                    + namespace + "\n" + numericId);
            String label = safeLabel(rawLabel);
            if (label.length() == 0) {
                if ("STEAM".equals(namespace)) label = "Steam game";
                else if ("GOG".equals(namespace)) label = "GOG game";
                else if ("EPIC".equals(namespace)) label = "Epic game";
                else label = "Custom game";
            }
            return new GameContextSnapshot(GameContextSnapshot.State.ACTIVE,
                    PACKAGE_NAME, pid, GameContextBinding.KIND_PC_GAME,
                    identity, label, observedAt, DETECTOR_ID);
        } catch (Throwable ignored) {
            return GameContextSnapshot.UNKNOWN;
        }
    }

    private static String extractId(Pattern pattern, String line) {
        if (line == null) return "";
        Matcher matcher = pattern.matcher(line);
        if (!matcher.matches()) return "";
        return matcher.group(1) + "_" + matcher.group(2);
    }

    private static String safeLabel(String value) {
        if (value == null) return "";
        StringBuilder clean = new StringBuilder(Math.min(value.length(), 120));
        for (int i = 0; i < value.length() && clean.length() < 120; i++) {
            char item = value.charAt(i);
            if (!Character.isISOControl(item)) clean.append(item);
        }
        return clean.toString().trim();
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
