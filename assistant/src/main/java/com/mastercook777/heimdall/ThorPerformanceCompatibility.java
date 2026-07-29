package com.mastercook777.heimdall;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/** Opt-in workaround for Thor's lower-display composition performance policy. */
final class ThorPerformanceCompatibility {
    private static final String PREFS = "heimdall_performance";
    private static final String KEY_ENABLED = "thor_client_composition_compatibility";

    private Dialog dialog;
    private boolean pausedForTextInput;
    private boolean lastForceForDiagnostics;

    static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    static boolean isEnabled(Context context) {
        return isSupported() && context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled && isSupported())
                .apply();
    }

    void apply(Activity activity, boolean forceForDiagnostics) {
        lastForceForDiagnostics = forceForDiagnostics;
        boolean enabled = isSupported() && (forceForDiagnostics || isEnabled(activity));
        if (pausedForTextInput) {
            release();
            return;
        }
        if (!enabled) {
            release();
            return;
        }
        if (dialog != null || activity.isFinishing()) {
            return;
        }
        Dialog next = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
        View layer = new View(activity);
        layer.setBackgroundColor(Color.argb(1, 0, 0, 0));
        next.setContentView(layer);
        Window window = next.getWindow();
        if (window == null) {
            return;
        }
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setBackgroundBlurRadius(1);
        window.setDimAmount(0f);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.width = WindowManager.LayoutParams.MATCH_PARENT;
        attributes.height = WindowManager.LayoutParams.MATCH_PARENT;
        attributes.gravity = Gravity.FILL;
        attributes.format = PixelFormat.TRANSLUCENT;
        window.setAttributes(attributes);
        next.setOnDismissListener(ignored -> {
            if (dialog == next) {
                dialog = null;
            }
        });
        next.show();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        dialog = next;
        Log.i(DebugPerformanceDiagnostics.TAG,
                "thorPerformanceCompatibility enabled forceForDiagnostics="
                        + forceForDiagnostics);
    }

    void setPausedForTextInput(Activity activity, boolean paused) {
        if (pausedForTextInput == paused) {
            return;
        }
        pausedForTextInput = paused;
        if (paused) {
            release();
            Log.i(DebugPerformanceDiagnostics.TAG,
                    "thorPerformanceCompatibility pausedForTextInput=true");
        } else {
            Log.i(DebugPerformanceDiagnostics.TAG,
                    "thorPerformanceCompatibility pausedForTextInput=false");
            apply(activity, lastForceForDiagnostics);
        }
    }

    void release() {
        Dialog current = dialog;
        dialog = null;
        if (current != null && current.isShowing()) {
            current.dismiss();
        }
    }
}
