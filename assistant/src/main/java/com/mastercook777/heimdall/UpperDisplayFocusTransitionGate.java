package com.mastercook777.heimdall;

import android.view.Display;

/**
 * Allows one follow-up focus pulse when an emulator window reaches the upper display
 * after Heimdall's normal resume pulse has already run.
 */
final class UpperDisplayFocusTransitionGate {
    private boolean armed;
    private boolean primaryAttempted;
    private String baselineIdentity = "";
    private String baselinePackage = "";

    void arm(ForegroundAppTracker.Snapshot baseline) {
        armed = true;
        primaryAttempted = false;
        baselineIdentity = identityOf(baseline);
        baselinePackage = baseline == null ? "" : baseline.packageName;
    }

    void markPrimaryAttempted() {
        if (armed) {
            primaryAttempted = true;
        }
    }

    boolean shouldRecover(ForegroundAppTracker.Snapshot snapshot) {
        if (!armed || snapshot == null || !isUpperDisplay(snapshot)) {
            return false;
        }
        String identity = identityOf(snapshot);
        if (identity.length() == 0 || identity.equals(baselineIdentity)) {
            return false;
        }
        if (snapshot.packageName.equals(baselinePackage)
                && !ShizukuGameContextController.isSupportedPackage(snapshot.packageName)) {
            return false;
        }
        if (!primaryAttempted) {
            // The ordinary resume pulse will run after this already-observed transition.
            armed = false;
            return false;
        }
        armed = false;
        return true;
    }

    void cancel() {
        armed = false;
        primaryAttempted = false;
        baselineIdentity = "";
        baselinePackage = "";
    }

    private static boolean isUpperDisplay(ForegroundAppTracker.Snapshot snapshot) {
        return snapshot.displayId == Display.DEFAULT_DISPLAY
                || (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R
                && snapshot.displayId == Display.INVALID_DISPLAY);
    }

    private static String identityOf(ForegroundAppTracker.Snapshot snapshot) {
        if (snapshot == null) return "";
        return snapshot.packageName + '\n' + snapshot.className + '\n'
                + snapshot.windowTitle + '\n' + snapshot.displayId;
    }
}
