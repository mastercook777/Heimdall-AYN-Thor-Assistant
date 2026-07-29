package com.mastercook777.heimdall;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class InputBackendDiagnostics {
    public static final class Snapshot {
        public final boolean accessibilityReady;
        public final boolean shizukuInstalled;
        public final String shizukuPackageName;
        public final boolean suFound;
        public final String suPath;
        public final boolean uinputExists;
        public final boolean uinputReadable;
        public final boolean uinputWritable;
        public final boolean uinputOpenProbeOk;
        public final String uinputOpenProbeMessage;
        public final String recommendedRoute;

        private Snapshot(boolean accessibilityReady, boolean shizukuInstalled, String shizukuPackageName,
                boolean suFound, String suPath, boolean uinputExists, boolean uinputReadable,
                boolean uinputWritable, boolean uinputOpenProbeOk, String uinputOpenProbeMessage,
                String recommendedRoute) {
            this.accessibilityReady = accessibilityReady;
            this.shizukuInstalled = shizukuInstalled;
            this.shizukuPackageName = shizukuPackageName;
            this.suFound = suFound;
            this.suPath = suPath;
            this.uinputExists = uinputExists;
            this.uinputReadable = uinputReadable;
            this.uinputWritable = uinputWritable;
            this.uinputOpenProbeOk = uinputOpenProbeOk;
            this.uinputOpenProbeMessage = uinputOpenProbeMessage;
            this.recommendedRoute = recommendedRoute;
        }
    }

    private static final String[] SHIZUKU_PACKAGES = {
            "moe.shizuku.privileged.api",
            "rikka.shizuku"
    };
    private static final String[] SU_PATHS = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/vendor/bin/su",
            "/data/adb/magisk/su"
    };

    private InputBackendDiagnostics() {
    }

    public static Snapshot inspect(Context context) {
        boolean accessibility = InputBridge.isReady(context);
        String shizukuPackage = findInstalledPackage(context, SHIZUKU_PACKAGES);
        String suPath = findExistingPath(SU_PATHS);
        File uinput = new File("/dev/uinput");
        boolean uinputExists = uinput.exists();
        boolean uinputReadable = uinputExists && uinput.canRead();
        boolean uinputWritable = uinputExists && uinput.canWrite();
        UinputOpenProbe probe = runUinputOpenProbe();
        String route = recommend(context, accessibility, ShizukuNativeController.isReady(), shizukuPackage != null,
                suPath != null, uinputExists, uinputWritable);
        String notDetected = context.getString(R.string.diagnostic_not_detected);
        return new Snapshot(accessibility, shizukuPackage != null, nonEmpty(shizukuPackage, notDetected),
                suPath != null, nonEmpty(suPath, notDetected), uinputExists, uinputReadable,
                uinputWritable, probe.ok, probe.message, route);
    }

    private static String recommend(Context context, boolean accessibility, boolean shizukuReady, boolean shizukuInstalled,
            boolean su, boolean uinputExists, boolean uinputWritable) {
        if (shizukuReady) {
            return context.getString(R.string.diagnostic_route_shizuku);
        }
        if (uinputWritable) {
            return context.getString(R.string.diagnostic_route_uinput);
        }
        if (su && uinputExists) {
            return uinputWritable
                    ? context.getString(R.string.diagnostic_route_root_ready)
                    : context.getString(R.string.diagnostic_route_root_permission);
        }
        if (shizukuInstalled) {
            return context.getString(R.string.diagnostic_route_start_shizuku);
        }
        if (accessibility) {
            return context.getString(R.string.diagnostic_route_accessibility);
        }
        return context.getString(R.string.diagnostic_route_enable_accessibility);
    }

    private static String findInstalledPackage(Context context, String[] packageNames) {
        PackageManager pm = context.getPackageManager();
        for (String packageName : packageNames) {
            try {
                pm.getPackageInfo(packageName, 0);
                return packageName;
            } catch (PackageManager.NameNotFoundException ignored) {
                // Try next known package name.
            }
        }
        return null;
    }

    private static String findExistingPath(String[] paths) {
        for (String path : paths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                return path;
            }
        }
        return null;
    }

    private static UinputOpenProbe runUinputOpenProbe() {
        File file = new File("/dev/uinput");
        if (!file.exists()) {
            return new UinputOpenProbe(false, "Not found");
        }
        try {
            RandomAccessFile handle = new RandomAccessFile(file, "rw");
            handle.close();
            return new UinputOpenProbe(true, "Open rw ok");
        } catch (IOException | SecurityException e) {
            return new UinputOpenProbe(false, e.getClass().getSimpleName() + ": " + nonEmpty(e.getMessage(), "open failed"));
        }
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }

    private static final class UinputOpenProbe {
        final boolean ok;
        final String message;

        UinputOpenProbe(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }
    }
}
