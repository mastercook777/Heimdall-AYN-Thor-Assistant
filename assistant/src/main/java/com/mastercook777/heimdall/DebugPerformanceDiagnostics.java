package com.mastercook777.heimdall;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Debug-only A/B mode selection and deliberately low-frequency Logcat telemetry. */
final class DebugPerformanceDiagnostics {
    static final String EXTRA_MODE = "heimdall_diagnostic_mode";
    static final String TAG = "HeimdallPerf";
    private static final long REPORT_SECONDS = 5L;

    enum Mode {
        STATIC_UI("A"),
        FLAT_UI_NORMAL_LOGIC("B"),
        NORMAL("C"),
        COMPOSITION_PROBE("D");

        final String code;

        Mode(String code) {
            this.code = code;
        }
    }

    private static volatile Mode mode = Mode.NORMAL;
    private static volatile boolean debugEnabled;
    private static final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private static final Map<String, TaskState> tasks = new ConcurrentHashMap<>();
    private static final Set<String> activeAnimators = ConcurrentHashMap.newKeySet();
    private static final AtomicLong duplicateTaskRegistrations = new AtomicLong();
    private static ScheduledExecutorService reporter;
    private static long lastProcessCpuMs;
    private static long lastMainCpuMs;
    private static int mainTid;

    private DebugPerformanceDiagnostics() {
    }

    static void initialize(Activity activity) {
        debugEnabled = (activity.getApplicationInfo().flags
                & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (!debugEnabled) {
            mode = Mode.NORMAL;
            return;
        }
        String requested = activity.getIntent() == null
                ? null : activity.getIntent().getStringExtra(EXTRA_MODE);
        mode = parseMode(requested);
        counters.clear();
        tasks.clear();
        activeAnimators.clear();
        duplicateTaskRegistrations.set(0L);
        mainTid = Process.myTid();
        lastProcessCpuMs = Process.getElapsedCpuTime();
        lastMainCpuMs = readThreadCpuMs(mainTid);
        Log.i(TAG, "session mode=" + mode.code + " " + mode
                + " launch with: adb shell am force-stop " + BuildConfig.APPLICATION_ID + "; "
                + "adb shell am start -n " + BuildConfig.APPLICATION_ID + "/"
                + AssistantActivity.class.getName() + " --es "
                + EXTRA_MODE + " " + mode.code);
        startReporter();
    }

    static Mode mode() {
        return debugEnabled ? mode : Mode.NORMAL;
    }

    static boolean isDebugEnabled() {
        return debugEnabled;
    }

    static boolean isStaticUi() {
        return mode() == Mode.STATIC_UI;
    }

    static boolean isFlatUi() {
        return mode() == Mode.FLAT_UI_NORMAL_LOGIC;
    }

    static boolean isCompositionProbe() {
        return mode() == Mode.COMPOSITION_PROBE;
    }

    static void countDraw(String component) {
        count("draw", component);
    }

    static void countInvalidate(String component) {
        count("invalidate", component);
    }

    static void countPostInvalidate(String component) {
        count("postInvalidate", component);
    }

    static void countPostInvalidateOnAnimation(String component) {
        count("postInvalidateOnAnimation", component);
    }

    static void countRequestLayout(String component) {
        count("requestLayout", component);
    }

    static void countChoreographerCallback(String owner) {
        count("choreographer", owner);
    }

    static void countSurfaceFrame(String owner) {
        count("surfaceFrame", owner);
    }

    static void countAccessibilityWindowInspection() {
        count("accessibilityWindow", "getWindows");
    }

    static long beginTask(String name) {
        count("taskRun", name);
        TaskState state = tasks.computeIfAbsent(name, ignored -> new TaskState());
        long now = System.nanoTime();
        long previous = state.lastStartNanos;
        state.lastStartNanos = now;
        if (previous != 0L) {
            state.actualIntervalTotalMs.addAndGet((now - previous) / 1_000_000L);
            state.actualIntervalSamples.incrementAndGet();
        }
        return now;
    }

    static void endTask(String name, long startNanos) {
        long elapsedMicros = Math.max(0L, (System.nanoTime() - startNanos) / 1_000L);
        TaskState state = tasks.computeIfAbsent(name, ignored -> new TaskState());
        state.durationMicros.addAndGet(elapsedMicros);
        state.durationSamples.incrementAndGet();
    }

    static void registerRepeatingTask(String name, long intervalMs) {
        TaskState state = tasks.computeIfAbsent(name, ignored -> new TaskState());
        state.plannedIntervalMs = intervalMs;
        if (state.registered) {
            duplicateTaskRegistrations.incrementAndGet();
        }
        state.registered = true;
    }

    static void unregisterRepeatingTask(String name) {
        TaskState state = tasks.get(name);
        if (state != null) {
            state.registered = false;
        }
    }

    static void animatorStarted(String type, String creationSite, long durationMs, boolean infinite) {
        activeAnimators.add(type + "@" + creationSite + "/" + durationMs + "ms/infinite=" + infinite);
    }

    static void animatorStopped(String key) {
        activeAnimators.remove(key);
    }

    static void attachRootObservers(Activity activity) {
        if (!debugEnabled) {
            return;
        }
        View root = activity.getWindow().getDecorView();
        ViewTreeObserver observer = root.getViewTreeObserver();
        observer.addOnDrawListener(() -> countDraw("Window traversal"));
        observer.addOnGlobalLayoutListener(() -> count("layout", "Root layout"));
        root.post(() -> auditWindow(activity));
    }

    static void auditWindow(Activity activity) {
        if (!debugEnabled) {
            return;
        }
        Window window = activity.getWindow();
        View decor = window.getDecorView();
        WindowManager.LayoutParams attributes = window.getAttributes();
        Display display = decor.getDisplay();
        Display.Mode displayMode = display == null ? null : display.getMode();
        Drawable background = decor.getBackground();
        GraphicAudit audit = new GraphicAudit();
        auditViewTree(decor, decor.getWidth() * (long) decor.getHeight(), audit);
        String backgroundOpacity = background == null ? "none"
                : opacityName(background.getOpacity());
        Log.i(TAG, "window mode=" + mode.code
                + " format=" + attributes.format
                + " translucent=" + (attributes.format == PixelFormat.TRANSLUCENT)
                + " decorBackground=" + backgroundOpacity
                + " decorAlpha=" + decor.getAlpha()
                + " preferredRefreshRate=" + attributes.preferredRefreshRate
                + " preferredDisplayModeId=" + attributes.preferredDisplayModeId
                + " displayRefreshRate=" + (displayMode == null ? -1f : displayMode.getRefreshRate())
                + " softwareLayers=" + audit.softwareLayers
                + " elevatedViews=" + audit.elevatedViews
                + " alphaViews=" + audit.alphaViews
                + " largeNonOpaqueBackgrounds=" + audit.largeNonOpaqueBackgrounds
                + " renderEffects=" + audit.renderEffects
                + " appChoreographerLoops=0"
                + " sourceAudit=Animator:0 Choreographer:0 postInvalidateOnAnimation:0"
                + " saveLayer:0 BlurMaskFilter:0 softwareLayerCalls:0 RenderEffectCalls:0");
    }

    static void shutdown() {
        if (reporter != null) {
            reporter.shutdownNow();
            reporter = null;
        }
        tasks.clear();
        counters.clear();
        activeAnimators.clear();
    }

    private static void startReporter() {
        if (reporter != null) {
            reporter.shutdownNow();
        }
        reporter = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "heimdall-perf-reporter");
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
        reporter.scheduleAtFixedRate(DebugPerformanceDiagnostics::report,
                REPORT_SECONDS, REPORT_SECONDS, TimeUnit.SECONDS);
    }

    private static void report() {
        if (!debugEnabled) {
            return;
        }
        StringBuilder rates = new StringBuilder();
        boolean continuousFrameLoop = false;
        for (Map.Entry<String, AtomicLong> entry : counters.entrySet()) {
            long value = entry.getValue().getAndSet(0L);
            if (rates.length() > 0) {
                rates.append(' ');
            }
            rates.append(entry.getKey()).append('=')
                    .append(String.format(Locale.US, "%.2f/s", value / (double) REPORT_SECONDS));
            if (entry.getKey().startsWith("choreographer:")
                    && value >= REPORT_SECONDS) {
                continuousFrameLoop = true;
            }
        }
        long processCpu = Process.getElapsedCpuTime();
        long mainCpu = readThreadCpuMs(mainTid);
        long processDelta = Math.max(0L, processCpu - lastProcessCpuMs);
        long mainDelta = mainCpu < 0 || lastMainCpuMs < 0 ? -1L
                : Math.max(0L, mainCpu - lastMainCpuMs);
        lastProcessCpuMs = processCpu;
        lastMainCpuMs = mainCpu;
        int registeredTasks = 0;
        StringBuilder taskSummary = new StringBuilder();
        for (Map.Entry<String, TaskState> entry : tasks.entrySet()) {
            TaskState state = entry.getValue();
            if (state.registered) {
                registeredTasks++;
            }
            long durationSamples = state.durationSamples.getAndSet(0L);
            long durationMicros = state.durationMicros.getAndSet(0L);
            long intervalSamples = state.actualIntervalSamples.getAndSet(0L);
            long intervalTotalMs = state.actualIntervalTotalMs.getAndSet(0L);
            if (durationSamples > 0L || state.registered) {
                taskSummary.append(' ').append(entry.getKey())
                        .append("{registered=").append(state.registered)
                        .append(",plannedMs=").append(state.plannedIntervalMs)
                        .append(",actualMs=").append(intervalSamples == 0 ? -1 : intervalTotalMs / intervalSamples)
                        .append(",runMs=").append(durationSamples == 0 ? -1
                                : String.format(Locale.US, "%.3f", durationMicros / 1000d / durationSamples))
                        .append('}');
            }
        }
        Log.i(TAG, "sample mode=" + mode.code
                + " processCpuMsPerSec=" + String.format(Locale.US, "%.2f", processDelta / (double) REPORT_SECONDS)
                + " mainCpuMsPerSec=" + (mainDelta < 0 ? "n/a"
                        : String.format(Locale.US, "%.2f", mainDelta / (double) REPORT_SECONDS))
                + " activeTasks=" + registeredTasks
                + " duplicateTaskRegistrations=" + duplicateTaskRegistrations.get()
                + " activeAnimators=" + activeAnimators.size()
                + " continuousFrameLoop=" + continuousFrameLoop
                + (rates.length() == 0 ? " idleCounters=0" : " " + rates)
                + taskSummary);
    }

    private static void count(String event, String component) {
        if (!debugEnabled) {
            return;
        }
        counters.computeIfAbsent(event + ":" + component, ignored -> new AtomicLong())
                .incrementAndGet();
    }

    private static Mode parseMode(String value) {
        if (value == null) {
            return Mode.NORMAL;
        }
        String normalized = value.trim().toUpperCase(Locale.US);
        if ("A".equals(normalized) || "STATIC".equals(normalized)) {
            return Mode.STATIC_UI;
        }
        if ("B".equals(normalized) || "FLAT".equals(normalized)) {
            return Mode.FLAT_UI_NORMAL_LOGIC;
        }
        if ("D".equals(normalized) || "COMPOSITION".equals(normalized)
                || "PROBE".equals(normalized)) {
            return Mode.COMPOSITION_PROBE;
        }
        return Mode.NORMAL;
    }

    private static long readThreadCpuMs(int tid) {
        if (tid <= 0) {
            return -1L;
        }
        try (BufferedReader reader = new BufferedReader(
                new FileReader("/proc/self/task/" + tid + "/stat"))) {
            String stat = reader.readLine();
            int endName = stat == null ? -1 : stat.lastIndexOf(')');
            if (endName < 0 || endName + 2 >= stat.length()) {
                return -1L;
            }
            String[] fields = stat.substring(endName + 2).split("\\s+");
            long ticks = Long.parseLong(fields[11]) + Long.parseLong(fields[12]);
            long ticksPerSecond = Os.sysconf(OsConstants._SC_CLK_TCK);
            return ticksPerSecond <= 0L ? -1L : ticks * 1000L / ticksPerSecond;
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private static void auditViewTree(View view, long rootArea, GraphicAudit audit) {
        if (view.getLayerType() == View.LAYER_TYPE_SOFTWARE) {
            audit.softwareLayers++;
        }
        if (view.getElevation() > 0f) {
            audit.elevatedViews++;
        }
        if (view.getAlpha() < 1f) {
            audit.alphaViews++;
        }
        Drawable background = view.getBackground();
        long area = view.getWidth() * (long) view.getHeight();
        if (background != null && area > rootArea / 3L
                && background.getOpacity() != PixelFormat.OPAQUE) {
            audit.largeNonOpaqueBackgrounds++;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                auditViewTree(group.getChildAt(i), rootArea, audit);
            }
        }
    }

    private static String opacityName(int opacity) {
        if (opacity == PixelFormat.OPAQUE) return "opaque";
        if (opacity == PixelFormat.TRANSLUCENT) return "translucent";
        if (opacity == PixelFormat.TRANSPARENT) return "transparent";
        return String.valueOf(opacity);
    }

    private static final class TaskState {
        volatile boolean registered;
        volatile long plannedIntervalMs;
        volatile long lastStartNanos;
        final AtomicLong durationMicros = new AtomicLong();
        final AtomicLong durationSamples = new AtomicLong();
        final AtomicLong actualIntervalTotalMs = new AtomicLong();
        final AtomicLong actualIntervalSamples = new AtomicLong();
    }

    private static final class GraphicAudit {
        int softwareLayers;
        int elevatedViews;
        int alphaViews;
        int largeNonOpaqueBackgrounds;
        int renderEffects;
    }
}
