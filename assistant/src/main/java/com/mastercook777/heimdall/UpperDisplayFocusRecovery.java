package com.mastercook777.heimdall;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;

import java.util.Locale;

/** Restores upper-display input focus without moving Heimdall between displays. */
final class UpperDisplayFocusRecovery {
    private static final String TAG = "HeimdallGameFocus";
    private static final long FOCUS_PULSE_DURATION_MS = 1L;

    enum Result {
        PULSE_ACCEPTED,
        UPPER_FRONTEND_SKIPPED,
        UPPER_IDENTITY_UNAVAILABLE,
        INPUT_BUSY,
        ACCESSIBILITY_UNAVAILABLE,
        DISPLAY_UNAVAILABLE,
        PULSE_REJECTED,
        FAILED
    }

    private UpperDisplayFocusRecovery() {}

    static Result attempt(Activity source) {
        return attempt(source, true);
    }

    static Result attemptCompanionPulse(Activity source) {
        return attempt(source, false);
    }

    private static Result attempt(Activity source, boolean requireUpperIdentity) {
        if (source == null || source.isFinishing() || source.isDestroyed()) {
            return Result.FAILED;
        }

        View decor = source.getWindow().getDecorView();
        Display sourceDisplay = decor == null ? null : decor.getDisplay();
        if (sourceDisplay == null || sourceDisplay.getDisplayId() == Display.DEFAULT_DISPLAY) {
            Log.w(TAG, "upper focus recovery skipped; Heimdall lower display unresolved");
            record(source, "recovery skipped lower-display-unresolved");
            return Result.FAILED;
        }

        DisplayManager displayManager = source.getSystemService(DisplayManager.class);
        Display upperDisplay = displayManager == null
                ? null : displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        Point pulsePoint = resolvePulsePoint(upperDisplay);
        ThorAccessibilityService service = ThorAccessibilityService.getInstance();
        if (pulsePoint == null) {
            Log.w(TAG, "upper focus recovery skipped; default display geometry unavailable");
            record(source, "recovery display-unavailable");
            return Result.DISPLAY_UNAVAILABLE;
        }
        if (requireUpperIdentity) {
            ForegroundAppTracker.Snapshot foreground = ForegroundAppTracker.latest();
            if (foreground == null
                    || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    ? foreground.displayId != Display.DEFAULT_DISPLAY
                    : !foreground.isUpperOrUnknownDisplay())) {
                record(source, "recovery skipped upper-identity-unavailable");
                return Result.UPPER_IDENTITY_UNAVAILABLE;
            }
            if (isCurrentUpperFrontend(source, foreground)) {
                record(source, "recovery skipped upper-frontend package="
                        + foreground.packageName);
                return Result.UPPER_FRONTEND_SKIPPED;
            }
        }
        if (service == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.w(TAG, "upper focus pulse unavailable service=" + (service != null)
                    + " api=" + Build.VERSION.SDK_INT);
            record(source, "recovery accessibility-unavailable service=" + (service != null)
                    + " api=" + Build.VERSION.SDK_INT);
            return Result.ACCESSIBILITY_UNAVAILABLE;
        }

        ThorAccessibilityService.FocusPulseDispatchResult dispatchResult =
                service.dispatchFocusPulse(
                        Display.DEFAULT_DISPLAY,
                        pulsePoint.x,
                        pulsePoint.y,
                        FOCUS_PULSE_DURATION_MS);
        if (dispatchResult
                == ThorAccessibilityService.FocusPulseDispatchResult.ACCEPTED) {
            Log.i(TAG, "upper focus pulse accepted display=0 x=" + pulsePoint.x
                    + " y=" + pulsePoint.y + " durationMs=" + FOCUS_PULSE_DURATION_MS);
            record(source, "recovery pulse-accepted display=0 x=" + pulsePoint.x
                    + " y=" + pulsePoint.y);
            return Result.PULSE_ACCEPTED;
        }
        if (dispatchResult == ThorAccessibilityService.FocusPulseDispatchResult.BUSY) {
            Log.w(TAG, "upper focus pulse skipped; Heimdall input is active");
            record(source, "recovery input-busy");
            return Result.INPUT_BUSY;
        }

        Log.w(TAG, "upper focus pulse rejected; preserving task surfaces");
        record(source, "recovery pulse-rejected");
        return Result.PULSE_REJECTED;
    }

    private static void record(Activity source, String message) {
        HeimdallStabilityDiagnostics.recordFocusDiagnostic(source, message);
    }

    static Point resolveMirroredLowerRightPoint(int width, int height) {
        if (width <= 2 || height <= 1) {
            return null;
        }
        // The Thor-verified lower-left point is one pixel inside the horizontal
        // boundary: x=1, not x=0. Its exact horizontal mirror is therefore
        // width-2; width-1 would be the unverified absolute right edge.
        return new Point(width - 2, height - 1);
    }

    private static Point resolvePulsePoint(Display display) {
        if (display == null || !display.isValid()) {
            return null;
        }
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        try {
            Display.Mode mode = display.getMode();
            if (mode != null && mode.getPhysicalWidth() > 0 && mode.getPhysicalHeight() > 0) {
                int physicalWidth = mode.getPhysicalWidth();
                int physicalHeight = mode.getPhysicalHeight();
                boolean metricsLandscape = metrics.widthPixels >= metrics.heightPixels;
                boolean modeLandscape = physicalWidth >= physicalHeight;
                if (metricsLandscape != modeLandscape) {
                    int swap = physicalWidth;
                    physicalWidth = physicalHeight;
                    physicalHeight = swap;
                }
                width = physicalWidth;
                height = physicalHeight;
            }
        } catch (Throwable ignored) {
        }
        return resolveMirroredLowerRightPoint(width, height);
    }

    static boolean isCurrentUpperFrontend(Activity source,
            ForegroundAppTracker.Snapshot foreground) {
        if (foreground == null || foreground.packageName.length() == 0
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && foreground.displayId != Display.DEFAULT_DISPLAY)) {
            return false;
        }
        if (ThorAccessibilityService.isPairedDisplayFrontendPackage(
                foreground.packageName)) {
            return true;
        }
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo resolved = source.getPackageManager().resolveActivity(
                    homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolved != null && resolved.activityInfo != null
                    && foreground.packageName.equals(resolved.activityInfo.packageName)) {
                return true;
            }
            CharSequence label = source.getPackageManager().getApplicationLabel(
                    source.getPackageManager().getApplicationInfo(
                            foreground.packageName, 0));
            return label != null && label.toString().toLowerCase(Locale.ROOT)
                    .contains("cocoon");
        } catch (Throwable ignored) {
            return false;
        }
    }
}
