package com.mastercook777.heimdall;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Display;

final class ForegroundAppTracker {
    private static final String PREFS = "heimdall_app_awareness";
    private static final String KEY_ENABLED = "auto_profile_switch_enabled";

    interface Listener {
        void onForegroundAppChanged(Snapshot snapshot);
    }

    static final class Snapshot {
        final String packageName;
        final String className;
        final String windowTitle;
        final int displayId;
        final long observedAtMs;

        Snapshot(String packageName, String className, String windowTitle,
                int displayId, long observedAtMs) {
            this.packageName = clean(packageName);
            this.className = clean(className);
            this.windowTitle = clean(windowTitle);
            this.displayId = displayId;
            this.observedAtMs = observedAtMs;
        }

        String contextText() {
            return (windowTitle + " " + className).trim();
        }

        boolean isUpperOrUnknownDisplay() {
            return displayId == Display.DEFAULT_DISPLAY || displayId == Display.INVALID_DISPLAY;
        }
    }

    private static volatile Snapshot latest;
    private static volatile Listener listener;

    private ForegroundAppTracker() {
    }

    static boolean isEnabled(Context context) {
        return preferences(context).getBoolean(KEY_ENABLED, false);
    }

    static void setEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    static Snapshot latest() {
        return latest;
    }

    static void setListener(Listener value) {
        listener = value;
    }

    static void clearListener(Listener value) {
        if (listener == value) {
            listener = null;
        }
    }

    static void publish(Snapshot snapshot) {
        if (snapshot == null || snapshot.packageName.length() == 0) {
            return;
        }
        Snapshot previous = latest;
        if (previous != null
                && previous.packageName.equals(snapshot.packageName)
                && previous.className.equals(snapshot.className)
                && previous.windowTitle.equals(snapshot.windowTitle)
                && previous.displayId == snapshot.displayId) {
            latest = snapshot;
            return;
        }
        latest = snapshot;
        Listener current = listener;
        if (current != null) {
            current.onForegroundAppChanged(snapshot);
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
