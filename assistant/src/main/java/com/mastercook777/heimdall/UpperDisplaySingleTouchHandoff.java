package com.mastercook777.heimdall;

import android.app.Activity;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;

/**
 * Returns system-navigation focus to Thor's upper display after a known lower-focus lease ends.
 *
 * <p>This primitive submits exactly one Accessibility touch. It has no lifecycle inference,
 * frontend detection, delay, retry, confirmation, rearm, or Activity fallback.</p>
 */
final class UpperDisplaySingleTouchHandoff {
    private static final String TAG = "HeimdallGameFocus";
    private static final long TOUCH_DURATION_MS = 1L;

    enum Result {
        ACCEPTED,
        INPUT_BUSY,
        ACCESSIBILITY_UNAVAILABLE,
        DISPLAY_UNAVAILABLE,
        REJECTED,
        FAILED
    }

    private UpperDisplaySingleTouchHandoff() {}

    static Result attempt(Activity source) {
        if (source == null || source.isFinishing() || source.isDestroyed()) {
            return Result.FAILED;
        }

        View decor = source.getWindow().getDecorView();
        Display sourceDisplay = decor == null ? null : decor.getDisplay();
        if (sourceDisplay == null || sourceDisplay.getDisplayId() == Display.DEFAULT_DISPLAY) {
            record(source, "deterministic handoff skipped lower-display-unresolved");
            return Result.FAILED;
        }

        DisplayManager displayManager = source.getSystemService(DisplayManager.class);
        Display upperDisplay = displayManager == null
                ? null : displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        Point touchPoint = resolveTouchPoint(upperDisplay);
        if (touchPoint == null) {
            record(source, "deterministic handoff display-unavailable");
            return Result.DISPLAY_UNAVAILABLE;
        }

        ThorAccessibilityService service = ThorAccessibilityService.getInstance();
        if (service == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            record(source, "deterministic handoff accessibility-unavailable service="
                    + (service != null) + " api=" + Build.VERSION.SDK_INT);
            return Result.ACCESSIBILITY_UNAVAILABLE;
        }

        ThorAccessibilityService.SingleTouchHandoffDispatchResult dispatchResult =
                service.dispatchUpperDisplaySingleTouchHandoff(
                        Display.DEFAULT_DISPLAY,
                        touchPoint.x,
                        touchPoint.y,
                        TOUCH_DURATION_MS);
        if (dispatchResult
                == ThorAccessibilityService.SingleTouchHandoffDispatchResult.ACCEPTED) {
            Log.i(TAG, "deterministic upper handoff accepted display=0 x=" + touchPoint.x
                    + " y=" + touchPoint.y + " durationMs=" + TOUCH_DURATION_MS);
            record(source, "deterministic handoff accepted display=0 x=" + touchPoint.x
                    + " y=" + touchPoint.y);
            return Result.ACCEPTED;
        }
        if (dispatchResult
                == ThorAccessibilityService.SingleTouchHandoffDispatchResult.BUSY) {
            record(source, "deterministic handoff input-busy");
            return Result.INPUT_BUSY;
        }

        record(source, "deterministic handoff rejected");
        return Result.REJECTED;
    }

    static Point resolveMirroredLowerRightPoint(int width, int height) {
        if (width <= 2 || height <= 1) {
            return null;
        }
        // Thor verified x=1 at the lower-left edge. Its exact horizontal mirror is width-2.
        return new Point(width - 2, height - 1);
    }

    private static Point resolveTouchPoint(Display display) {
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

    private static void record(Activity source, String message) {
        HeimdallStabilityDiagnostics.recordFocusDiagnostic(source, message);
    }
}
