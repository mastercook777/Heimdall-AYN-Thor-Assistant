package com.mastercook777.heimdall;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.system.Os;
import android.system.OsConstants;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Separate read-only Shizuku service for fixed, allowlisted emulator detectors. */
public final class ShizukuGameContextUserService extends Binder {
    static final String DESCRIPTOR = BuildConfig.APPLICATION_ID + ".IGameContext";
    static final int TRANSACTION_QUERY_CONTEXT = IBinder.FIRST_CALL_TRANSACTION;

    private static final long EDEN_LABEL_PAIR_WINDOW_MS = 15_000L;
    private static final long STREAM_PID_CHECK_INTERVAL_MS = 750L;
    private static final long RETROARCH_POLL_INTERVAL_MS = 1_000L;
    private static final int MAX_RETROARCH_HISTORY_BYTES = 256 * 1024;

    private final Object lock = new Object();
    private volatile boolean destroyed;
    private Process logcatProcess;
    private String requestedPackage = "";
    private String monitoredPackage = "";
    private int monitoredPid = -1;
    private String activeValue = "";
    private String activeLabel = "";
    private String activePlatformValue = "";
    private String activePlatformLabel = "";
    private boolean activePlatformAffectsContentIdentity;
    private long observedAt;
    private long minimumAcceptedAt;
    private String pendingEdenLabel = "";
    private long pendingEdenLabelAt;
    private String cachedActivityPackage = "";
    private int cachedActivityState = AetherSx2GameContext.ACTIVITY_UNKNOWN;
    private long cachedActivityCheckedAt;
    private final Thread monitorThread;

    public ShizukuGameContextUserService() {
        this(null);
    }

    public ShizukuGameContextUserService(Context context) {
        attachInterface(null, DESCRIPTOR);
        monitorThread = new Thread(this::monitorLoop, "heimdall-game-context-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        data.enforceInterface(DESCRIPTOR);
        if (code == TRANSACTION_QUERY_CONTEXT) {
            String packageName = safe(data.readString());
            if (!ShizukuGameContextController.isSupportedPackage(packageName)) {
                writeReply(reply, "", AetherSx2GameContext.ACTIVITY_UNKNOWN,
                        -1, "", "", 0L, "", "", false);
                return true;
            }
            selectTarget(packageName);
            int activityState = isPositiveLaunchPackage(packageName)
                    ? AetherSx2GameContext.ACTIVITY_UNKNOWN
                    : currentActivityState(packageName);
            synchronized (lock) {
                boolean ready = packageName.equals(monitoredPackage);
                if (ready && shouldClearForPackage(packageName, activityState,
                        cachedActivityCheckedAt, observedAt)) {
                    clearIdentityLocked(cachedActivityCheckedAt);
                }
                writeReply(reply, packageName, activityState,
                        ready ? monitoredPid : -1,
                        ready ? activeValue : "",
                        ready ? activeLabel : "",
                        ready ? observedAt : 0L,
                        ready ? activePlatformValue : "",
                        ready ? activePlatformLabel : "",
                        ready && activePlatformAffectsContentIdentity);
            }
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    public void destroy() {
        destroyed = true;
        stopLogcat();
        monitorThread.interrupt();
        System.exit(0);
    }

    private static void writeReply(Parcel reply, String packageName, int activityState,
            int pid, String value, String label, long valueObservedAt,
            String platformValue, String platformLabel,
            boolean platformAffectsContentIdentity) {
        reply.writeNoException();
        reply.writeString(packageName);
        reply.writeInt(activityState);
        reply.writeInt(pid);
        reply.writeString(value);
        reply.writeString(label);
        reply.writeLong(valueObservedAt);
        reply.writeString(platformValue);
        reply.writeString(platformLabel);
        reply.writeInt(platformAffectsContentIdentity ? 1 : 0);
    }

    private void selectTarget(String packageName) {
        boolean changed;
        synchronized (lock) {
            changed = !packageName.equals(requestedPackage);
            if (changed) {
                requestedPackage = packageName;
                cachedActivityPackage = "";
                cachedActivityState = AetherSx2GameContext.ACTIVITY_UNKNOWN;
                cachedActivityCheckedAt = 0L;
            }
        }
        if (changed) stopLogcat();
    }

    private void monitorLoop() {
        while (!destroyed) {
            String packageName = currentRequestedPackage();
            if (packageName.length() == 0) {
                pause(500L);
                continue;
            }
            long streamFrom = System.currentTimeMillis();
            int pid = resolvePid(packageName);
            if (pid <= 0) {
                resetForTarget(packageName, -1);
                pause(1000L);
                continue;
            }
            if (!isCurrentTarget(packageName, pid)) {
                resetForTarget(packageName, pid);
                if (!RetroArchGameContext.supportsPackage(packageName)) {
                    bootstrap(packageName, pid);
                }
            }
            if (RetroArchGameContext.supportsPackage(packageName)) {
                monitorRetroArch(packageName, pid);
                pause(350L);
                continue;
            }
            stream(packageName, pid, streamFrom);
            pause(350L);
        }
    }

    private void monitorRetroArch(String packageName, int pid) {
        long processStartedAt = processStartEpochMillis(pid);
        while (!destroyed && isCurrentTarget(packageName, pid)
                && streamProcessIsCurrent(packageName, pid)) {
            pollRetroArchHistory(packageName, pid, processStartedAt);
            pause(RETROARCH_POLL_INTERVAL_MS);
            if (Thread.currentThread().isInterrupted()) return;
        }
    }

    private void pollRetroArchHistory(String packageName, int pid, long processStartedAt) {
        if (processStartedAt <= 0L) return;
        RetroArchGameContext.LaunchRecord best = null;
        for (String path : RetroArchGameContext.historyPathsForPackage(packageName)) {
            File file = new File(path);
            long modifiedAt = file.lastModified();
            if (!RetroArchGameContext.isLaunchEvidenceFresh(modifiedAt, processStartedAt)) {
                continue;
            }
            synchronized (lock) {
                if (!isCurrentTargetLocked(packageName, pid)
                        || modifiedAt <= observedAt) continue;
            }
            String json = readBoundedUtf8(file, MAX_RETROARCH_HISTORY_BYTES);
            RetroArchGameContext.LaunchRecord candidate =
                    RetroArchGameContext.parseHistory(json, modifiedAt);
            if (candidate != null && candidate.isValid()
                    && (best == null || candidate.observedAt > best.observedAt)) {
                best = candidate;
            }
        }
        if (best == null) return;
        synchronized (lock) {
            if (!isCurrentTargetLocked(packageName, pid)
                    || best.observedAt <= observedAt) return;
            activeValue = best.contentPath;
            activeLabel = best.contentLabel;
            activePlatformValue = best.platformCode;
            activePlatformLabel = best.platformLabel;
            activePlatformAffectsContentIdentity = best.platformAffectsContentIdentity;
            observedAt = best.observedAt;
        }
    }

    private void bootstrap(String packageName, int pid) {
        Process process = null;
        try {
            process = new ProcessBuilder(logcatDumpCommand(packageName, pid))
                    .redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    handleLogLine(packageName, pid, line);
                }
            }
            process.waitFor(3, TimeUnit.SECONDS);
        } catch (Throwable ignored) {
        } finally {
            if (process != null) process.destroy();
        }
    }

    private void stream(String packageName, int pid, long replayFrom) {
        Process process = null;
        Thread pidWatcher = null;
        try {
            String start = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
                    .format(new Date(replayFrom));
            process = new ProcessBuilder(logcatStreamCommand(packageName, pid, start))
                    .redirectErrorStream(true).start();
            synchronized (lock) {
                if (destroyed || !isCurrentTargetLocked(packageName, pid)) {
                    process.destroy();
                    return;
                }
                logcatProcess = process;
            }
            Process watchedProcess = process;
            pidWatcher = new Thread(() -> watchStreamPid(
                    packageName, pid, watchedProcess), "heimdall-game-context-pid");
            pidWatcher.setDaemon(true);
            pidWatcher.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (!destroyed && isCurrentTarget(packageName, pid)
                        && (line = reader.readLine()) != null) {
                    handleLogLine(packageName, pid, line);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (pidWatcher != null) pidWatcher.interrupt();
            synchronized (lock) {
                if (logcatProcess == process) logcatProcess = null;
            }
            if (process != null) process.destroy();
        }
    }

    private void watchStreamPid(String packageName, int pid, Process process) {
        while (!destroyed && isCurrentTarget(packageName, pid) && process.isAlive()) {
            pause(STREAM_PID_CHECK_INTERVAL_MS);
            if (Thread.currentThread().isInterrupted()) return;
            if (!streamProcessIsCurrent(packageName, pid)) {
                process.destroy();
                return;
            }
        }
    }

    private static boolean streamProcessIsCurrent(String packageName, int pid) {
        if (pid <= 0 || packageName == null || packageName.length() == 0) return false;
        File processDirectory = new File("/proc/" + pid);
        if (!processDirectory.isDirectory()) return false;
        File cmdlineFile = new File(processDirectory, "cmdline");
        try (FileInputStream input = new FileInputStream(cmdlineFile)) {
            byte[] buffer = new byte[256];
            int count = input.read(buffer);
            if (count <= 0) return processDirectory.isDirectory();
            int end = 0;
            while (end < count && buffer[end] != 0) end++;
            String command = new String(buffer, 0, end, StandardCharsets.UTF_8);
            return processCommandMatchesPackage(packageName, command);
        } catch (Throwable ignored) {
            // Some Android procfs policies hide cmdline while still exposing process liveness.
            // Treat an existing PID as alive to avoid a destructive logcat restart loop.
            return processDirectory.isDirectory();
        }
    }

    static boolean processCommandMatchesPackage(String packageName, String command) {
        if (packageName == null || command == null) return false;
        return packageName.equals(command) || command.startsWith(packageName + ":");
    }

    private static String readBoundedUtf8(File file, int maximumBytes) {
        if (file == null || !file.isFile() || maximumBytes <= 0
                || file.length() <= 0L || file.length() > maximumBytes) return "";
        try (FileInputStream input = new FileInputStream(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream(
                        (int) Math.min(file.length(), 16 * 1024L))) {
            byte[] buffer = new byte[4096];
            int count;
            int total = 0;
            while ((count = input.read(buffer)) >= 0) {
                total += count;
                if (total > maximumBytes) return "";
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static long processStartEpochMillis(int pid) {
        if (pid <= 0) return 0L;
        try {
            long startTicks = parseProcessStartTicks(readSmallUtf8(
                    new File("/proc/" + pid + "/stat"), 4096));
            long bootEpochSeconds = parseBootEpochSeconds(readSmallUtf8(
                    new File("/proc/stat"), 64 * 1024));
            long ticksPerSecond = Os.sysconf(OsConstants._SC_CLK_TCK);
            if (startTicks <= 0L || bootEpochSeconds <= 0L || ticksPerSecond <= 0L) {
                return 0L;
            }
            return bootEpochSeconds * 1000L
                    + (startTicks * 1000L / ticksPerSecond);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    static long parseProcessStartTicks(String statLine) {
        if (statLine == null) return 0L;
        int processNameEnd = statLine.lastIndexOf(") ");
        if (processNameEnd < 0 || processNameEnd + 2 >= statLine.length()) return 0L;
        String[] fields = statLine.substring(processNameEnd + 2).trim().split("\\s+");
        if (fields.length <= 19) return 0L;
        try {
            return Long.parseLong(fields[19]);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    static long parseBootEpochSeconds(String procStat) {
        if (procStat == null) return 0L;
        for (String line : procStat.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("btime ")) continue;
            try {
                return Long.parseLong(trimmed.substring(6).trim());
            } catch (Throwable ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static String readSmallUtf8(File file, int maximumBytes) {
        if (file == null || !file.isFile() || file.length() > maximumBytes) return "";
        try (FileInputStream input = new FileInputStream(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[2048];
            int count;
            int total = 0;
            while ((count = input.read(buffer)) >= 0) {
                total += count;
                if (total > maximumBytes) return "";
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (Throwable ignored) {
            return "";
        }
    }

    private void handleLogLine(String packageName, int pid, String line) {
        long timestamp = AetherSx2GameContext.extractObservedAt(line);
        if (timestamp <= 0L) return;
        if (EdenGameContext.supportsPackage(packageName)) {
            String label = EdenGameContext.extractTitleLabel(line);
            if (label.length() > 0) rememberEdenLabel(packageName, pid, label, timestamp);
            String titleId = EdenGameContext.extractTitleId(line);
            if (titleId.length() > 0) accept(packageName, pid, titleId, timestamp);
            return;
        }
        if (PpssppGameContext.supportsPackage(packageName)) {
            String uri = PpssppGameContext.extractUri(line);
            if (uri.length() > 0) accept(packageName, pid, uri, timestamp);
            return;
        }
        String uri = AetherSx2GameContext.extractUri(line);
        if (uri.length() > 0) accept(packageName, pid, uri, timestamp);
    }

    private void rememberEdenLabel(String packageName, int pid, String label, long timestamp) {
        synchronized (lock) {
            if (!isCurrentTargetLocked(packageName, pid)
                    || timestamp < minimumAcceptedAt) return;
            pendingEdenLabel = label;
            pendingEdenLabelAt = timestamp;
        }
    }

    private void accept(String packageName, int pid, String value, long timestamp) {
        synchronized (lock) {
            if (!isCurrentTargetLocked(packageName, pid)
                    || timestamp < minimumAcceptedAt) return;
            activeValue = value;
            if (EdenGameContext.supportsPackage(packageName)
                    && pendingEdenLabelAt > 0L
                    && pendingEdenLabelAt <= timestamp
                    && timestamp - pendingEdenLabelAt <= EDEN_LABEL_PAIR_WINDOW_MS) {
                activeLabel = pendingEdenLabel;
            } else {
                activeLabel = "";
            }
            observedAt = timestamp;
        }
    }

    private void resetForTarget(String packageName, int pid) {
        stopLogcat();
        synchronized (lock) {
            monitoredPackage = packageName;
            monitoredPid = pid;
            activeValue = "";
            activeLabel = "";
            activePlatformValue = "";
            activePlatformLabel = "";
            activePlatformAffectsContentIdentity = false;
            observedAt = 0L;
            minimumAcceptedAt = 0L;
            pendingEdenLabel = "";
            pendingEdenLabelAt = 0L;
        }
    }

    private void clearIdentityLocked(long cutoff) {
        activeValue = "";
        activeLabel = "";
        activePlatformValue = "";
        activePlatformLabel = "";
        activePlatformAffectsContentIdentity = false;
        observedAt = 0L;
        pendingEdenLabel = "";
        pendingEdenLabelAt = 0L;
        minimumAcceptedAt = Math.max(minimumAcceptedAt, cutoff);
    }

    private String currentRequestedPackage() {
        synchronized (lock) {
            return requestedPackage;
        }
    }

    private boolean isCurrentTarget(String packageName, int pid) {
        synchronized (lock) {
            return isCurrentTargetLocked(packageName, pid);
        }
    }

    private boolean isCurrentTargetLocked(String packageName, int pid) {
        return packageName.equals(requestedPackage)
                && packageName.equals(monitoredPackage)
                && pid == monitoredPid;
    }

    private void stopLogcat() {
        Process process;
        synchronized (lock) {
            process = logcatProcess;
            logcatProcess = null;
        }
        if (process != null) process.destroy();
    }

    private static String[] logcatDumpCommand(String packageName, int pid) {
        return new String[] {"logcat", "-d", "-v", "epoch", "--pid=" + pid,
                detectorLogTag(packageName) + ":I", "*:S"};
    }

    private static String[] logcatStreamCommand(String packageName, int pid, String start) {
        return new String[] {"logcat", "-v", "epoch", "-T", start, "--pid=" + pid,
                detectorLogTag(packageName) + ":I", "*:S"};
    }

    static String detectorLogTag(String packageName) {
        if (EdenGameContext.supportsPackage(packageName)) return "YuzuNative";
        if (PpssppGameContext.supportsPackage(packageName)) return "PPSSPP";
        if (RetroArchGameContext.supportsPackage(packageName)) return "RetroArch";
        return "EmulationThread";
    }

    private static int resolvePid(String packageName) {
        Process process = null;
        try {
            process = new ProcessBuilder("pidof", packageName).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String value = reader.readLine();
                if (value == null) return -1;
                String[] tokens = value.trim().split("\\s+");
                return tokens.length == 0 ? -1 : Integer.parseInt(tokens[0]);
            }
        } catch (Throwable ignored) {
            return -1;
        } finally {
            if (process != null) process.destroy();
        }
    }

    private int currentActivityState(String packageName) {
        long now = System.currentTimeMillis();
        synchronized (lock) {
            if (packageName.equals(cachedActivityPackage)
                    && now - cachedActivityCheckedAt
                    < activityCacheDurationMillis(cachedActivityState)) {
                return cachedActivityState;
            }
        }
        int resolved = resolveActivityState(packageName);
        synchronized (lock) {
            cachedActivityPackage = packageName;
            cachedActivityState = resolved;
            cachedActivityCheckedAt = System.currentTimeMillis();
            return cachedActivityState;
        }
    }

    static long activityCacheDurationMillis(int activityState) {
        return activityState == AetherSx2GameContext.ACTIVITY_EMULATION ? 3000L : 500L;
    }

    static boolean shouldClearForActivity(int activityState,
            long activityCheckedAt, long identityObservedAt) {
        return activityState == AetherSx2GameContext.ACTIVITY_MAIN
                && identityObservedAt <= activityCheckedAt;
    }

    static boolean shouldClearForPackage(String packageName, int activityState,
            long activityCheckedAt, long identityObservedAt) {
        return !isPositiveLaunchPackage(packageName)
                && shouldClearForActivity(activityState,
                activityCheckedAt, identityObservedAt);
    }

    private static boolean isPositiveLaunchPackage(String packageName) {
        return PpssppGameContext.supportsPackage(packageName)
                || RetroArchGameContext.supportsPackage(packageName);
    }

    private static int resolveActivityState(String packageName) {
        Process process = null;
        try {
            process = new ProcessBuilder("dumpsys", "activity", "activities")
                    .redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int state = classifyActivityLine(packageName, line);
                    if (state != AetherSx2GameContext.ACTIVITY_UNKNOWN) return state;
                }
            }
            process.waitFor(3, TimeUnit.SECONDS);
        } catch (Throwable ignored) {
        } finally {
            if (process != null) process.destroy();
        }
        return AetherSx2GameContext.ACTIVITY_UNKNOWN;
    }

    static int classifyActivityLine(String packageName, String line) {
        if (AetherSx2GameContext.PACKAGE_NAME.equals(packageName)) {
            return AetherSx2GameContext.classifyResumedActivityLine(line);
        }
        if (PpssppGameContext.supportsPackage(packageName)) {
            return AetherSx2GameContext.ACTIVITY_UNKNOWN;
        }
        if (RetroArchGameContext.supportsPackage(packageName)) {
            return AetherSx2GameContext.ACTIVITY_UNKNOWN;
        }
        return EdenGameContext.classifyResumedActivityLine(packageName, line);
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
