package com.mastercook777.heimdall;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** Low-frequency, ordinary-app hardware sampling for the passive Grid monitor. */
final class HardwareMonitorSampler {
    static final String CPU_SENSOR_TYPE = "cpu-0-1";
    static final long SAMPLE_INTERVAL_MS = 2_000L;

    private static final File THERMAL_ROOT = new File("/sys/class/thermal");
    private static final long SENSOR_RETRY_MS = 30_000L;
    private static final float MIN_PLAUSIBLE_CELSIUS = 0f;
    private static final float MAX_PLAUSIBLE_CELSIUS = 150f;

    interface Listener {
        void onSnapshot(Snapshot snapshot);
    }

    static final class Snapshot {
        final Float cpuCelsius;
        final int thermalStatus;
        final long ramUsedBytes;
        final long ramTotalBytes;
        final boolean lowMemory;

        Snapshot(Float cpuCelsius, int thermalStatus, long ramUsedBytes,
                long ramTotalBytes, boolean lowMemory) {
            this.cpuCelsius = cpuCelsius;
            this.thermalStatus = thermalStatus;
            this.ramUsedBytes = ramUsedBytes;
            this.ramTotalBytes = ramTotalBytes;
            this.lowMemory = lowMemory;
        }

        boolean hasRam() {
            return ramTotalBytes > 0L && ramUsedBytes >= 0L;
        }

        int ramUsedPercent() {
            if (!hasRam()) return 0;
            return Math.max(0, Math.min(100,
                    Math.round(ramUsedBytes * 100f / ramTotalBytes)));
        }
    }

    private final Context appContext;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> samplingTask;
    private File cpuTemperatureFile;
    private long nextSensorResolveAtMs;
    private int generation;
    private boolean released;

    HardwareMonitorSampler(Context context, Listener listener) {
        appContext = context.getApplicationContext();
        this.listener = listener;
        executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "HeimdallHardwareMonitor");
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                return thread;
            }
        });
    }

    synchronized void start() {
        if (released || (samplingTask != null && !samplingTask.isDone())) return;
        final int token = ++generation;
        samplingTask = executor.scheduleWithFixedDelay(
                () -> sampleAndPost(token), 0L, SAMPLE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    synchronized void stop() {
        generation++;
        if (samplingTask != null) {
            samplingTask.cancel(false);
            samplingTask = null;
        }
    }

    synchronized void release() {
        if (released) return;
        released = true;
        stop();
        executor.shutdownNow();
    }

    private void sampleAndPost(int token) {
        Snapshot snapshot = sample();
        mainHandler.post(() -> {
            synchronized (HardwareMonitorSampler.this) {
                if (released || token != generation || samplingTask == null) return;
            }
            listener.onSnapshot(snapshot);
        });
    }

    private Snapshot sample() {
        Float cpuCelsius = readCpuCelsius();
        int thermalStatus = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PowerManager powerManager = (PowerManager) appContext.getSystemService(
                    Context.POWER_SERVICE);
            if (powerManager != null) {
                try {
                    thermalStatus = powerManager.getCurrentThermalStatus();
                } catch (RuntimeException ignored) {
                    // Thermal severity is advisory; keep metrics available if it fails.
                }
            }
        }

        long ramUsedBytes = -1L;
        long ramTotalBytes = -1L;
        boolean lowMemory = false;
        ActivityManager manager = (ActivityManager) appContext.getSystemService(
                Context.ACTIVITY_SERVICE);
        if (manager != null) {
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            try {
                manager.getMemoryInfo(info);
                if (info.totalMem > 0L && info.availMem >= 0L) {
                    ramTotalBytes = info.totalMem;
                    ramUsedBytes = Math.max(0L, info.totalMem - info.availMem);
                    lowMemory = info.lowMemory;
                }
            } catch (RuntimeException ignored) {
                // Public memory information should be available, but fail closed if it is not.
            }
        }
        return new Snapshot(cpuCelsius, thermalStatus,
                ramUsedBytes, ramTotalBytes, lowMemory);
    }

    private Float readCpuCelsius() {
        long now = android.os.SystemClock.elapsedRealtime();
        if (cpuTemperatureFile == null && now >= nextSensorResolveAtMs) {
            cpuTemperatureFile = findCpuTemperatureFile(THERMAL_ROOT);
            nextSensorResolveAtMs = now + SENSOR_RETRY_MS;
        }
        File source = cpuTemperatureFile;
        if (source == null) return null;
        Float value = readTemperatureCelsius(source);
        if (value == null) {
            cpuTemperatureFile = null;
            nextSensorResolveAtMs = now + SENSOR_RETRY_MS;
        }
        return value;
    }

    static File findCpuTemperatureFile(File thermalRoot) {
        if (thermalRoot == null) return null;
        File[] zones = thermalRoot.listFiles(file -> file != null && file.isDirectory()
                && file.getName().startsWith("thermal_zone"));
        if (zones == null) return null;
        Arrays.sort(zones, Comparator.comparing(File::getName));
        File match = null;
        for (File zone : zones) {
            String type = readSingleLine(new File(zone, "type"));
            if (!CPU_SENSOR_TYPE.equals(type)) continue;
            File temperature = new File(zone, "temp");
            if (match != null) return null;
            match = temperature;
        }
        return match;
    }

    static Float readTemperatureCelsius(File source) {
        return convertMilliCelsius(readSingleLine(source));
    }

    static Float convertMilliCelsius(String rawText) {
        if (rawText == null) return null;
        try {
            float value = Long.parseLong(rawText.trim()) / 1000f;
            if (!Float.isFinite(value)
                    || value < MIN_PLAUSIBLE_CELSIUS
                    || value > MAX_PLAUSIBLE_CELSIUS) {
                return null;
            }
            return value;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String readSingleLine(File source) {
        if (source == null) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(source), StandardCharsets.US_ASCII))) {
            String line = reader.readLine();
            return line == null ? null : line.trim();
        } catch (IOException | SecurityException ignored) {
            return null;
        }
    }
}
