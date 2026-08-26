package com.mastercook777.heimdall;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Records process-exit evidence and sheds optional compositor work under system pressure. */
final class HeimdallStabilityDiagnostics {
    static final String TAG = "HeimdallStability";
    private static final String JOURNAL_FILE = "heimdall-stability.log";
    private static final long MAX_JOURNAL_BYTES = 128L * 1024L;
    private static final int MAX_EXIT_TRACE_BYTES = 64 * 1024;
    private static boolean crashHandlerInstalled;

    private final ThorPerformanceCompatibility performanceCompatibility;
    private Object thermalListener;
    private PowerManager powerManager;
    private Context appContext;

    HeimdallStabilityDiagnostics(ThorPerformanceCompatibility performanceCompatibility) {
        this.performanceCompatibility = performanceCompatibility;
    }

    static synchronized void installCrashHandler(Context context) {
        if (crashHandlerInstalled) {
            return;
        }
        Context appContext = context.getApplicationContext();
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            record(appContext, "uncaughtException thread=" + safeToken(thread.getName())
                    + " type=" + error.getClass().getName() + "\n"
                    + privacySafeStack(error));
            if (previous != null) {
                previous.uncaughtException(thread, error);
            }
        });
        crashHandlerInstalled = true;
    }

    static void reportPreviousExit(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return;
        }
        try {
            List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(
                    context.getPackageName(), 0, 1);
            if (exits == null || exits.isEmpty()) {
                Log.i(TAG, "previousExit unavailable");
                return;
            }
            ApplicationExitInfo exit = exits.get(0);
            Log.i(TAG, "previousExit reason=" + exitReasonName(exit.getReason())
                    + " status=" + exit.getStatus()
                    + " importance=" + exit.getImportance()
                    + " timestampMs=" + exit.getTimestamp()
                    + " pssKb=" + exit.getPss()
                    + " rssKb=" + exit.getRss());
        } catch (RuntimeException error) {
            Log.w(TAG, "previousExit read failed", error);
        }
    }

    void start(Activity activity) {
        appContext = activity.getApplicationContext();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || thermalListener != null) {
            return;
        }
        powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            return;
        }
        thermalListener = ThermalApi.register(powerManager, activity,
                this::onThermalStatusChanged);
        onThermalStatusChanged(ThermalApi.currentStatus(powerManager));
    }

    void stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && powerManager != null && thermalListener != null) {
            ThermalApi.unregister(powerManager, thermalListener);
        }
        thermalListener = null;
        powerManager = null;
    }

    void onTrimMemory(Activity activity, int level) {
        logMemoryState(activity, "trimMemory", level);
        if (level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
                || level == android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
                || level == android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
                || level == android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE
                || level == android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            record(activity, "resourceGuard event=trimMemory level=" + level
                    + " action=suspend-performance-compatibility");
            performanceCompatibility.suspendForResourcePressure("trim-memory-" + level);
        }
    }

    void onLowMemory(Activity activity) {
        logMemoryState(activity, "lowMemory", -1);
        record(activity, "resourceGuard event=lowMemory"
                + " action=suspend-performance-compatibility");
        performanceCompatibility.suspendForResourcePressure("low-memory");
    }

    String buildReport(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        int memoryClass = -1;
        if (manager != null) {
            manager.getMemoryInfo(memoryInfo);
            memoryClass = manager.getMemoryClass();
        }
        Runtime runtime = Runtime.getRuntime();
        StringBuilder report = new StringBuilder();
        report.append("Heimdall diagnostic report\n")
                .append("createdUtc=").append(utcNow()).append('\n')
                .append("package=").append(BuildConfig.APPLICATION_ID).append('\n')
                .append("version=").append(BuildConfig.VERSION_NAME)
                .append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
                .append("debug=").append(BuildConfig.DEBUG).append('\n')
                .append("device=").append(safeToken(Build.MANUFACTURER)).append(' ')
                .append(safeToken(Build.MODEL)).append('\n')
                .append("androidSdk=").append(Build.VERSION.SDK_INT).append('\n')
                .append("memoryClassMb=").append(memoryClass).append('\n')
                .append("systemAvailMb=").append(mb(memoryInfo.availMem)).append('\n')
                .append("systemThresholdMb=").append(mb(memoryInfo.threshold)).append('\n')
                .append("systemLowMemory=").append(memoryInfo.lowMemory).append('\n')
                .append("javaUsedMb=")
                .append(mb(runtime.totalMemory() - runtime.freeMemory())).append('\n')
                .append("javaMaxMb=").append(mb(runtime.maxMemory())).append('\n')
                .append("performanceCompatibilityEnabled=")
                .append(ThorPerformanceCompatibility.isEnabled(context)).append('\n')
                .append("performanceCompatibilityActive=")
                .append(performanceCompatibility.isActive()).append('\n')
                .append("performanceCompatibilityPressureSuspended=")
                .append(performanceCompatibility.isSuspendedForResourcePressure()).append('\n')
                .append("magnifierProjectionActive=")
                .append(UpperScreenProjectionService.isActiveOrStarting()).append('\n')
                .append("screenRecordingActive=")
                .append(ScreenRecordingService.isRecording()).append('\n')
                .append("previousExit=").append(previousExitSummary(context)).append('\n')
                .append("\nPrevious exit trace\n")
                .append(previousExitTrace(context))
                .append("\nPrivate stability journal\n")
                .append(readJournal(context));
        return report.toString();
    }

    private void onThermalStatusChanged(int status) {
        Log.i(TAG, "thermalStatus=" + status);
        if (appContext != null) {
            record(appContext, "thermalStatus=" + status);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && status >= PowerManager.THERMAL_STATUS_SEVERE) {
            if (appContext != null) {
                record(appContext, "resourceGuard event=thermal status=" + status
                        + " action=suspend-performance-compatibility");
            }
            performanceCompatibility.suspendForResourcePressure("thermal-" + status);
        }
    }

    private static void logMemoryState(Activity activity, String event, int level) {
        ActivityManager manager = (ActivityManager) activity.getSystemService(
                Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        if (manager != null) {
            manager.getMemoryInfo(memoryInfo);
        }
        Runtime runtime = Runtime.getRuntime();
        Log.w(TAG, event + " level=" + level
                + " systemAvailMb=" + mb(memoryInfo.availMem)
                + " systemThresholdMb=" + mb(memoryInfo.threshold)
                + " systemLow=" + memoryInfo.lowMemory
                + " javaUsedMb=" + mb(runtime.totalMemory() - runtime.freeMemory())
                + " javaMaxMb=" + mb(runtime.maxMemory()));
    }

    static String previousExitSummary(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "unsupported";
        }
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return "unavailable";
        }
        try {
            List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(
                    context.getPackageName(), 0, 1);
            if (exits == null || exits.isEmpty()) {
                return "none";
            }
            ApplicationExitInfo exit = exits.get(0);
            return "reason=" + exitReasonName(exit.getReason())
                    + ",status=" + exit.getStatus()
                    + ",importance=" + exit.getImportance()
                    + ",timestampMs=" + exit.getTimestamp()
                    + ",pssKb=" + exit.getPss()
                    + ",rssKb=" + exit.getRss();
        } catch (RuntimeException error) {
            return "read-failed:" + error.getClass().getSimpleName();
        }
    }

    private static String previousExitTrace(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return "unsupported\n";
        }
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return "unavailable\n";
        }
        try {
            List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(
                    context.getPackageName(), 0, 1);
            if (exits == null || exits.isEmpty()) {
                return "none\n";
            }
            try (InputStream input = exits.get(0).getTraceInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (input == null) {
                    return "none\n";
                }
                byte[] buffer = new byte[4096];
                int read;
                while (output.size() < MAX_EXIT_TRACE_BYTES
                        && (read = input.read(buffer, 0,
                        Math.min(buffer.length, MAX_EXIT_TRACE_BYTES - output.size()))) != -1) {
                    output.write(buffer, 0, read);
                }
                if (output.size() == 0) {
                    return "none\n";
                }
                return output.toString(StandardCharsets.UTF_8.name()) + "\n";
            }
        } catch (Exception error) {
            return "read-failed:" + error.getClass().getSimpleName() + "\n";
        }
    }

    private static synchronized void record(Context context, String message) {
        if (context == null || message == null) {
            return;
        }
        File journal = new File(context.getFilesDir(), JOURNAL_FILE);
        try {
            if (journal.length() > MAX_JOURNAL_BYTES) {
                journal.delete();
            }
            try (FileOutputStream output = new FileOutputStream(journal, true)) {
                String line = utcNow() + " " + message + "\n";
                output.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception error) {
            Log.w(TAG, "journal write failed", error);
        }
    }

    private static String readJournal(Context context) {
        File journal = new File(context.getFilesDir(), JOURNAL_FILE);
        if (!journal.isFile()) {
            return "none\n";
        }
        try (FileInputStream input = new FileInputStream(journal);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1
                    && output.size() <= MAX_JOURNAL_BYTES) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (Exception error) {
            return "read-failed:" + error.getClass().getSimpleName() + "\n";
        }
    }

    private static String privacySafeStack(Throwable error) {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        Throwable current = error;
        int causes = 0;
        while (current != null && causes < 4) {
            writer.println("exception=" + current.getClass().getName());
            StackTraceElement[] trace = current.getStackTrace();
            for (int i = 0; i < trace.length && i < 64; i++) {
                writer.println("  at " + trace[i]);
            }
            current = current.getCause();
            causes++;
        }
        writer.flush();
        return buffer.toString();
    }

    private static String safeToken(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String utcNow() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
    }

    private static String mb(long bytes) {
        return String.format(Locale.US, "%.1f", bytes / 1048576d);
    }

    private static String exitReasonName(int reason) {
        switch (reason) {
            case ApplicationExitInfo.REASON_CRASH:
                return "CRASH";
            case ApplicationExitInfo.REASON_CRASH_NATIVE:
                return "CRASH_NATIVE";
            case ApplicationExitInfo.REASON_ANR:
                return "ANR";
            case ApplicationExitInfo.REASON_LOW_MEMORY:
                return "LOW_MEMORY";
            case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE:
                return "EXCESSIVE_RESOURCE_USAGE";
            case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE:
                return "INITIALIZATION_FAILURE";
            case ApplicationExitInfo.REASON_PERMISSION_CHANGE:
                return "PERMISSION_CHANGE";
            case ApplicationExitInfo.REASON_USER_REQUESTED:
                return "USER_REQUESTED";
            case ApplicationExitInfo.REASON_USER_STOPPED:
                return "USER_STOPPED";
            case ApplicationExitInfo.REASON_SIGNALED:
                return "SIGNALED";
            case ApplicationExitInfo.REASON_DEPENDENCY_DIED:
                return "DEPENDENCY_DIED";
            case ApplicationExitInfo.REASON_OTHER:
                return "OTHER";
            case ApplicationExitInfo.REASON_UNKNOWN:
            default:
                return "UNKNOWN_" + reason;
        }
    }

    private interface ThermalStatusCallback {
        void onStatusChanged(int status);
    }

    /** Isolates API 29 thermal types so API 26-28 never resolve them while loading this class. */
    @android.annotation.TargetApi(Build.VERSION_CODES.Q)
    private static final class ThermalApi {
        private ThermalApi() {
        }

        static Object register(PowerManager manager, Activity activity,
                ThermalStatusCallback callback) {
            PowerManager.OnThermalStatusChangedListener listener = callback::onStatusChanged;
            manager.addThermalStatusListener(activity.getMainExecutor(), listener);
            return listener;
        }

        static void unregister(PowerManager manager, Object listener) {
            manager.removeThermalStatusListener(
                    (PowerManager.OnThermalStatusChangedListener) listener);
        }

        static int currentStatus(PowerManager manager) {
            return manager.getCurrentThermalStatus();
        }
    }
}
