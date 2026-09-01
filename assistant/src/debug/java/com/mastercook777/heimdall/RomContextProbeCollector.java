package com.mastercook777.heimdall;

import android.os.Build;
import android.system.Os;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** Fixed, read-only evidence collector executed inside a Debug Shizuku UserService. */
public final class RomContextProbeCollector {
    public static final int TARGET_RETROARCH = 1;
    public static final int TARGET_PPSSPP = 2;
    public static final int TARGET_EDEN = 3;

    private static final int MAX_COMMAND_BYTES = 256 * 1024;
    private static final int MAX_FILE_BYTES = 64 * 1024;
    private static final int MAX_FD_ENTRIES = 512;
    private static final long MAX_SESSION_AGE_MS = 15L * 60L * 1000L;
    private static final String COCOON_PACKAGE = "rip.moth.cocoonshell";
    private static final String DAIJISHO_PACKAGE = "com.magneticchen.daijishou";

    private static final String[] RETROARCH_PACKAGES = {
            "com.retroarch", "com.retroarch.aarch64", "com.retroarch.ra32",
            "org.retroarch", "org.retroarch.aarch64", "org.retroarch.ra32"
    };
    private static final String[] PPSSPP_PACKAGES = {
            "org.ppsspp.ppsspp", "org.ppsspp.ppssppgold"
    };
    private static final String[] EDEN_PACKAGES = {
            "dev.eden.eden_emulator", "dev.eden.eden_emulator.nightly",
            "dev.eden.eden_emulator.relWithDebInfo", "dev.legacy.eden_emulator"
    };
    private static final String[] RETROARCH_FILES = {
            "/storage/emulated/0/RetroArch/playlists/content_history.lpl",
            "/storage/emulated/0/RetroArch/playlists/builtin/content_history.lpl",
            "/storage/emulated/0/Android/data/com.retroarch.aarch64/files/playlists/content_history.lpl",
            "/storage/emulated/0/Android/data/com.retroarch/files/playlists/content_history.lpl"
    };
    private static final String[] ROM_PATH_MARKERS = {
            ".iso", ".chd", ".cso", ".bin", ".cue", ".m3u", ".pbp", ".elf",
            ".rvz", ".gcz", ".wbfs", ".nes", ".sfc", ".smc", ".gba", ".gbc",
            ".nds", ".3ds", ".cia", ".n64", ".z64", ".v64", ".nsp", ".nsz",
            ".xci", ".nca", ".nro", ".zip", ".7z"
    };
    private static final String[] LOG_IDENTITY_MARKERS = {
            "bootpath", "content://", "/storage/", "/mnt/media_rw/", "document/",
            "rom path", "game path", "disc path", "amstart", "launch", "serial",
            "booting", "disc id", "disc_id", "game id", "title id", "program id",
            "programid", "emulationfragment", "emulationactivity", "rom swap",
            "loading content", "load content", "booted content", "request_game_boot",
            "request_game_stop", "psp_shutdown", ".iso", ".chd", ".cso", ".pbp",
            ".cue", ".m3u", ".rvz", ".wbfs", ".nsp", ".nsz", ".xci", ".nca",
            ".nro"
    };

    private RomContextProbeCollector() {
    }

    public static String capture(int target, long requestedSinceEpochMs) {
        long capturedAt = System.currentTimeMillis();
        long sinceEpochMs = normalizeSince(requestedSinceEpochMs, capturedAt);
        String[] packages = packagesForTarget(target);
        StringBuilder report = new StringBuilder(32 * 1024);
        report.append("Heimdall ROM Context Probe\n")
                .append("schema=4\n")
                .append("probePhase=P3-ppsspp-lifecycle-evidence\n")
                .append("buildDebug=").append(BuildConfig.DEBUG).append('\n')
                .append("readOnly=true\n")
                .append("resolverConnected=false\n")
                .append("activityControllerInstalled=false\n")
                .append("target=").append(targetName(target)).append('\n')
                .append("packages=").append(String.join(",", packages)).append('\n')
                .append("frontendHint=").append(COCOON_PACKAGE).append('\n')
                .append("sessionStartedUtc=").append(utc(sinceEpochMs)).append('\n')
                .append("capturedUtc=").append(utc(capturedAt)).append('\n')
                .append("androidSdk=").append(Build.VERSION.SDK_INT).append('\n')
                .append("device=").append(cleanToken(Build.MANUFACTURER)).append(' ')
                .append(cleanToken(Build.MODEL)).append("\n\n");

        appendRaw(report, "shell_identity", runCommand("id"));
        appendRaw(report, "selinux_identity", runCommand("id", "-Z"));
        appendRaw(report, "selinux_mode", runCommand("getenforce"));

        String logcatSince = logcatTime(sinceEpochMs);
        appendFiltered(report, "activity_events",
                runCommand("logcat", "-b", "events", "-d", "-v", "epoch",
                        "-T", logcatSince, "wm_create_activity:I", "wm_new_intent:I", "*:S"),
                packages);
        appendFiltered(report, "activity_manager_log",
                runCommand("logcat", "-d", "-v", "epoch", "-T", logcatSince,
                        "ActivityTaskManager:I", "ActivityManager:I", "*:S"),
                packages);
        appendFiltered(report, "activity_records",
                runCommand("dumpsys", "activity", "activities"), packages);
        appendFiltered(report, "recent_tasks",
                runCommand("dumpsys", "activity", "recents"), packages);
        appendFiltered(report, "pending_intents",
                runCommand("dumpsys", "activity", "intents"), packages);
        appendFiltered(report, "uri_grants",
                runCommand("dumpsys", "uri_grants"), packages);

        appendFrontendProcessEvidence(report, sinceEpochMs);
        appendPackageMetadata(report, packages);
        appendProcessEvidence(report, packages, sinceEpochMs);
        if (target == TARGET_RETROARCH) {
            appendFiles(report, RETROARCH_FILES);
        } else if (target == TARGET_PPSSPP) {
            report.append("\n## emulator_native_state\n")
                    .append("PPSSPP WebSocket was not queried. This read-only probe does not "
                            + "enable or modify RemoteDebugger settings.\n");
        } else {
            report.append("\n## emulator_native_state\n")
                    .append("Eden internal state was not modified or queried through private "
                            + "app APIs. Activity, PID log, and open-FD evidence only.\n");
        }
        report.append("\n## interpretation_guard\n")
                .append("History files, recent-game files, task records, and URI grants may be "
                        + "stale. Do not use this report to switch Profiles. Compare ROM A, ROM B, "
                        + "ROM A, and no-game captures before proposing an identity source.\n");
        return report.toString();
    }

    static String targetName(int target) {
        if (target == TARGET_RETROARCH) return "retroarch";
        if (target == TARGET_PPSSPP) return "ppsspp";
        if (target == TARGET_EDEN) return "eden";
        return "invalid";
    }

    private static String[] packagesForTarget(int target) {
        if (target == TARGET_RETROARCH) return RETROARCH_PACKAGES.clone();
        if (target == TARGET_PPSSPP) return PPSSPP_PACKAGES.clone();
        if (target == TARGET_EDEN) return EDEN_PACKAGES.clone();
        throw new IllegalArgumentException("Unknown target " + target);
    }

    private static long normalizeSince(long requested, long now) {
        if (requested <= 0L || requested > now) {
            return now - 60_000L;
        }
        return Math.max(requested, now - MAX_SESSION_AGE_MS);
    }

    private static void appendProcessEvidence(StringBuilder report, String[] packages,
            long sinceEpochMs) {
        report.append("\n## process_evidence\n");
        for (String packageName : packages) {
            String pidOutput = runCommand("pidof", packageName).trim();
            report.append("package=").append(packageName)
                    .append(" pidof=").append(cleanToken(pidOutput)).append('\n');
            for (String token : pidOutput.split("\\s+")) {
                if (!token.matches("[0-9]+")) continue;
                File cmdline = new File("/proc/" + token + "/cmdline");
                report.append("  pid=").append(token).append(" cmdline=")
                        .append(readSmallFile(cmdline, 4096).replace('\u0000', ' ').trim())
                        .append('\n');
                appendFdEvidence(report, token);
                appendTargetProcessLog(report, token, sinceEpochMs);
            }
        }
    }

    private static void appendPackageMetadata(StringBuilder report, String[] packages) {
        report.append("\n## package_metadata\n");
        for (String packageName : packages) {
            String raw = runCommand("dumpsys", "package", packageName);
            report.append("package=").append(packageName).append('\n');
            boolean found = false;
            for (String line : raw.split("\\r?\\n")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("versionCode=")
                        || trimmed.startsWith("versionName=")
                        || trimmed.startsWith("firstInstallTime=")
                        || trimmed.startsWith("lastUpdateTime=")) {
                    report.append("  ").append(trimmed).append('\n');
                    found = true;
                }
            }
            if (!found) report.append("  unavailable\n");
        }
    }

    private static void appendFrontendProcessEvidence(StringBuilder report, long sinceEpochMs) {
        report.append("\n## frontend_process_evidence\n");
        String pidOutput = runCommand("pidof", COCOON_PACKAGE).trim();
        report.append("package=").append(COCOON_PACKAGE)
                .append(" pidof=").append(cleanToken(pidOutput)).append('\n');
        for (String token : pidOutput.split("\\s+")) {
            if (!token.matches("[0-9]+")) continue;
            File cmdline = new File("/proc/" + token + "/cmdline");
            report.append("  pid=").append(token).append(" cmdline=")
                    .append(readSmallFile(cmdline, 4096).replace('\u0000', ' ').trim())
                    .append('\n');
            appendFdEvidence(report, token);
            appendTargetProcessLog(report, token, sinceEpochMs);
        }
    }

    private static void appendFdEvidence(StringBuilder report, String pid) {
        File directory = new File("/proc/" + pid + "/fd");
        String[] entries = directory.list();
        report.append("  fdProbe=");
        if (entries == null) {
            report.append("unavailable\n");
            return;
        }
        Arrays.sort(entries);
        int inspected = 0;
        int readable = 0;
        int denied = 0;
        Set<String> candidates = new LinkedHashSet<>();
        for (String entry : entries) {
            if (inspected >= MAX_FD_ENTRIES || !entry.matches("[0-9]+")) continue;
            inspected++;
            try {
                String target = Os.readlink(new File(directory, entry).getAbsolutePath());
                readable++;
                if (isRomPathCandidate(target)) candidates.add(target);
            } catch (Exception ignored) {
                denied++;
            }
        }
        report.append("available inspected=").append(inspected)
                .append(" readable=").append(readable)
                .append(" denied=").append(denied).append('\n');
        if (candidates.isEmpty()) {
            report.append("    romFdCandidates=none\n");
        } else {
            for (String candidate : candidates) {
                report.append("    romFdCandidate=").append(candidate).append('\n');
            }
        }
    }

    private static void appendTargetProcessLog(StringBuilder report, String pid,
            long sinceEpochMs) {
        String raw = runCommand("logcat", "-d", "-v", "epoch", "-T",
                logcatTime(sinceEpochMs), "--pid=" + pid);
        report.append("  targetPidLogIdentityCandidates=\n")
                .append(indent(filterIdentityCandidates(raw), "    "));
    }

    static String filterIdentityCandidates(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "none\n";
        StringBuilder filtered = new StringBuilder();
        for (String line : raw.split("\\r?\\n")) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("[error=") || lower.startsWith("[exit=")
                    || lower.startsWith("[timeout]")) {
                filtered.append(line).append('\n');
                continue;
            }
            for (String marker : LOG_IDENTITY_MARKERS) {
                if (lower.contains(marker)) {
                    filtered.append(line).append('\n');
                    break;
                }
            }
        }
        return filtered.length() == 0 ? "none\n" : filtered.toString();
    }

    private static boolean isRomPathCandidate(String value) {
        if (value == null) return false;
        String lower = value.toLowerCase(Locale.ROOT);
        for (String marker : ROM_PATH_MARKERS) {
            if (lower.contains(marker)) return true;
        }
        return false;
    }

    private static String indent(String value, String prefix) {
        StringBuilder result = new StringBuilder();
        for (String line : value.split("\\r?\\n", -1)) {
            if (line.isEmpty()) continue;
            result.append(prefix).append(line).append('\n');
        }
        return result.length() == 0 ? prefix + "none\n" : result.toString();
    }

    private static void appendFiles(StringBuilder report, String[] paths) {
        report.append("\n## emulator_history_files\n");
        for (String path : paths) {
            File file = new File(path);
            report.append("path=").append(path)
                    .append(" exists=").append(file.isFile());
            if (!file.isFile()) {
                report.append('\n');
                continue;
            }
            report.append(" bytes=").append(file.length())
                    .append(" modifiedUtc=").append(utc(file.lastModified()))
                    .append('\n')
                    .append(readTail(file, MAX_FILE_BYTES)).append('\n');
        }
    }

    private static void appendRaw(StringBuilder report, String label, String value) {
        report.append("\n## ").append(label).append("\n")
                .append(value == null || value.trim().isEmpty() ? "none\n" : value.trim() + "\n");
    }

    private static void appendFiltered(StringBuilder report, String label, String raw,
            String[] packages) {
        report.append("\n## ").append(label).append("\n")
                .append(filterRelated(raw, packages));
    }

    static String filterRelated(String raw, String[] packages) {
        if (raw == null || raw.trim().isEmpty()) return "none\n";
        String[] lines = raw.split("\\r?\\n");
        boolean[] include = new boolean[lines.length];
        Set<String> markers = new LinkedHashSet<>(Arrays.asList(packages));
        markers.add(COCOON_PACKAGE);
        markers.add(DAIJISHO_PACKAGE);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean matched = false;
            for (String marker : markers) {
                if (line.contains(marker)) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                for (int j = Math.max(0, i - 2); j <= Math.min(lines.length - 1, i + 4); j++) {
                    include[j] = true;
                }
            }
        }
        StringBuilder filtered = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (include[i]) filtered.append(lines[i]).append('\n');
        }
        return filtered.length() == 0 ? "none\n" : filtered.toString();
    }

    private static String runCommand(String... command) {
        Process process = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            Process running = process;
            Thread reader = new Thread(() -> copyLimited(running, output),
                    "rom-context-probe-reader");
            reader.setDaemon(true);
            reader.start();
            boolean finished = process.waitFor(8, TimeUnit.SECONDS);
            if (!finished) process.destroyForcibly();
            reader.join(1500L);
            String value = new String(output.toByteArray(), StandardCharsets.UTF_8);
            if (!finished) return value + "\n[timeout]";
            if (process.exitValue() != 0) {
                return value + "\n[exit=" + process.exitValue() + "]";
            }
            return value;
        } catch (Exception error) {
            if (process != null) process.destroyForcibly();
            return "[error=" + error.getClass().getSimpleName() + ":"
                    + cleanToken(error.getMessage()) + "]";
        }
    }

    private static void copyLimited(Process process, ByteArrayOutputStream output) {
        try (InputStream input = process.getInputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                int remaining = MAX_COMMAND_BYTES - output.size();
                if (remaining <= 0) {
                    process.destroyForcibly();
                    break;
                }
                output.write(buffer, 0, Math.min(read, remaining));
            }
        } catch (Exception ignored) {
        }
    }

    private static String readSmallFile(File file, int limit) {
        if (!file.isFile()) return "unavailable";
        try (InputStream input = new FileInputStream(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[Math.min(4096, limit)];
            while (output.size() < limit) {
                int read = input.read(buffer, 0, Math.min(buffer.length, limit - output.size()));
                if (read == -1) break;
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    private static String readTail(File file, int limit) {
        try (RandomAccessFile input = new RandomAccessFile(file, "r")) {
            long start = Math.max(0L, input.length() - limit);
            input.seek(start);
            byte[] bytes = new byte[(int) (input.length() - start)];
            input.readFully(bytes);
            return (start > 0L ? "[tail truncated]\n" : "")
                    + new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception error) {
            return "[read-error=" + error.getClass().getSimpleName() + "]";
        }
    }

    private static String logcatTime(long epochMs) {
        return new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
                .format(new Date(epochMs));
    }

    private static String utc(long epochMs) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(epochMs));
    }

    private static String cleanToken(String value) {
        if (value == null) return "unknown";
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
