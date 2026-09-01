package com.mastercook777.heimdall;

import android.view.Display;

/**
 * One-shot ownership for Companion focus handoff.
 *
 * The relaunch Intent is only an early signal. The pulse may be scheduled only after
 * Accessibility observes a fresh application window on the upper display.
 */
final class UpperDisplayCompanionHandoffGate {
    private boolean armed;
    private boolean scheduled;
    private boolean companionConfirmed;
    private long armedAtMs;

    void arm(long nowMs, boolean confirmed) {
        armed = true;
        scheduled = false;
        companionConfirmed = confirmed;
        armedAtMs = nowMs;
    }

    boolean observePairedFrontend(ForegroundAppTracker.Snapshot snapshot) {
        if (!armed || snapshot == null
                || snapshot.displayId != Display.DEFAULT_DISPLAY
                || snapshot.observedAtMs < armedAtMs
                || !ThorAccessibilityService.isPairedDisplayFrontendPackage(
                        snapshot.packageName)) {
            return false;
        }
        companionConfirmed = true;
        return true;
    }

    void cancel() {
        armed = false;
        scheduled = false;
        companionConfirmed = false;
        armedAtMs = 0L;
    }

    boolean scheduleFor(ForegroundAppTracker.Snapshot snapshot) {
        if (!armed || scheduled || !companionConfirmed || snapshot == null
                || snapshot.packageName.length() == 0
                || ThorAccessibilityService.isPairedDisplayFrontendPackage(
                        snapshot.packageName)
                || snapshot.displayId != Display.DEFAULT_DISPLAY
                || snapshot.observedAtMs < armedAtMs) {
            return false;
        }
        scheduled = true;
        return true;
    }

    void retryAfterInterruptedSchedule() {
        if (armed) {
            scheduled = false;
        }
    }

    boolean consumeScheduled() {
        if (!armed || !scheduled) {
            return false;
        }
        cancel();
        return true;
    }

    boolean isArmed() {
        return armed;
    }

    boolean isScheduled() {
        return scheduled;
    }

    boolean isCompanionConfirmed() {
        return companionConfirmed;
    }
}
