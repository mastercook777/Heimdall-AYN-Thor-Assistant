package com.mastercook777.heimdall;

import android.view.Display;

/**
 * Allows one bounded confirmation pulse after the first Companion pulse.
 *
 * The first application-window observation can precede the emulator's usable input
 * surface. Confirmation requires either strict ACTIVE Game Context for that package or
 * a later stable observation of the same upper application window. It never retries
 * indefinitely and never supplies foreground or ROM identity.
 */
final class UpperDisplayCompanionConfirmationGate {
    static final long MIN_STABLE_WINDOW_MS = 750L;
    static final long MAX_CONFIRMATION_WINDOW_MS = 5000L;

    private boolean armed;
    private boolean scheduled;
    private String packageName = "";
    private long primaryPulseAtMs;

    void arm(ForegroundAppTracker.Snapshot snapshot, long nowMs) {
        cancel();
        if (snapshot == null || snapshot.packageName.length() == 0
                || snapshot.displayId != Display.DEFAULT_DISPLAY) {
            return;
        }
        armed = true;
        packageName = snapshot.packageName;
        primaryPulseAtMs = nowMs;
    }

    boolean scheduleFor(ForegroundAppTracker.Snapshot foreground,
            GameContextSnapshot context, long nowMs) {
        if (!armed || scheduled || foreground == null
                || foreground.displayId != Display.DEFAULT_DISPLAY
                || !packageName.equals(foreground.packageName)) {
            return false;
        }
        long ageMs = nowMs - primaryPulseAtMs;
        if (ageMs < 0L || ageMs > MAX_CONFIRMATION_WINDOW_MS) {
            cancel();
            return false;
        }
        boolean activeContext = context != null
                && context.state == GameContextSnapshot.State.ACTIVE
                && packageName.equals(context.packageName);
        boolean stableWindow = ageMs >= MIN_STABLE_WINDOW_MS
                && foreground.observedAtMs >= primaryPulseAtMs;
        if (!activeContext && !stableWindow) {
            return false;
        }
        scheduled = true;
        return true;
    }

    boolean consumeScheduled() {
        if (!armed || !scheduled) {
            return false;
        }
        cancel();
        return true;
    }

    void cancel() {
        armed = false;
        scheduled = false;
        packageName = "";
        primaryPulseAtMs = 0L;
    }

    boolean isArmed() {
        return armed;
    }
}
