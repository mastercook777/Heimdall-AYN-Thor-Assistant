package com.mastercook777.heimdall;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Build;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Display;

/** Event-driven power boundary, independent of Thor's Activity focus policy. */
final class ScreenPowerObserver implements AutoCloseable {
    interface Listener {
        void onPowerChanged(boolean interactive, boolean visibleDisplayOn);
    }

    private final Activity activity;
    private final Listener listener;
    private final DisplayManager displays;
    private boolean interactive;
    private boolean visibleDisplayOn;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };
    private final DisplayManager.DisplayListener displayListener =
            new DisplayManager.DisplayListener() {
                @Override public void onDisplayAdded(int id) { refresh(); }
                @Override public void onDisplayRemoved(int id) { refresh(); }
                @Override public void onDisplayChanged(int id) { refresh(); }
            };

    ScreenPowerObserver(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        displays = activity.getSystemService(DisplayManager.class);
        interactive = isInteractive(activity);
        visibleDisplayOn = isDisplayOn();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        if (displays != null) {
            displays.registerDisplayListener(displayListener, new Handler(Looper.getMainLooper()));
        }
    }

    static boolean isInteractive(Context context) {
        PowerManager power = context.getSystemService(PowerManager.class);
        return power == null || power.isInteractive();
    }

    boolean isVisibleRuntimeAllowed() {
        return allowsVisibleRuntime(interactive, visibleDisplayOn);
    }

    static boolean allowsVisibleRuntime(boolean interactive, boolean displayOn) {
        return interactive && displayOn;
    }

    void refresh() {
        boolean nextInteractive = isInteractive(activity);
        boolean nextDisplayOn = isDisplayOn();
        if (nextInteractive == interactive && nextDisplayOn == visibleDisplayOn) return;
        interactive = nextInteractive;
        visibleDisplayOn = nextDisplayOn;
        listener.onPowerChanged(interactive, visibleDisplayOn);
    }

    private boolean isDisplayOn() {
        return isDisplayOn(activity);
    }

    static boolean isDisplayOn(Activity activity) {
        Display display = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? activity.getDisplay() : activity.getWindowManager().getDefaultDisplay();
        // Unknown state is not proof of sleep. OFF/DOZE are never live UI targets.
        return display == null || display.getState() == Display.STATE_ON
                || display.getState() == Display.STATE_UNKNOWN;
    }

    @Override public void close() {
        activity.unregisterReceiver(receiver);
        if (displays != null) displays.unregisterDisplayListener(displayListener);
    }
}
