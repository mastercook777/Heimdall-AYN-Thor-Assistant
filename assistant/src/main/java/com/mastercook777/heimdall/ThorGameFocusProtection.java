package com.mastercook777.heimdall;

import android.app.Activity;
import android.util.Log;
import android.view.WindowManager;

/** Keeps Heimdall touchable without taking keyboard focus away from the upper-screen game. */
final class ThorGameFocusProtection {
    private static final String TAG = "HeimdallGameFocus";
    private static final int PROTECTED_WINDOW_FLAGS =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;

    private boolean protectedWindow;

    void apply(Activity activity, boolean protectGameFocus) {
        apply(activity, protectGameFocus, false);
    }

    void reapply(Activity activity, boolean protectGameFocus) {
        apply(activity, protectGameFocus, true);
    }

    private void apply(Activity activity, boolean protectGameFocus, boolean force) {
        if (activity == null) {
            return;
        }
        int currentFlags = activity.getWindow().getAttributes().flags;
        if ((currentFlags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            Log.i(TAG, "cleared stale not-touchable flag");
        }
        int activeProtectedFlags = currentFlags & PROTECTED_WINDOW_FLAGS;
        boolean flagsMatch = protectGameFocus
                ? activeProtectedFlags == PROTECTED_WINDOW_FLAGS
                : activeProtectedFlags == 0;
        if (!force && protectedWindow == protectGameFocus && flagsMatch) {
            return;
        }
        if (protectGameFocus) {
            activity.getWindow().addFlags(PROTECTED_WINDOW_FLAGS);
        } else {
            activity.getWindow().clearFlags(PROTECTED_WINDOW_FLAGS);
        }
        protectedWindow = protectGameFocus;
        Log.i(TAG, "game focus protection=" + protectGameFocus);
    }
}
