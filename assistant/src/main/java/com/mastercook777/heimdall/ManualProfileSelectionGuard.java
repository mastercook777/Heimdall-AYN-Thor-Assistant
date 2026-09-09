package com.mastercook777.heimdall;

import java.util.Locale;

/**
 * Keeps an explicit emulator Profile selection stable until a new launch is observed.
 * This is intentionally memory-only and never changes Profile persistence or detector state.
 */
final class ManualProfileSelectionGuard {
    private boolean armed;
    private String packageName = "";
    private boolean selectedDuringActiveLaunch;
    private int pid = -1;
    private String kind = "";
    private String identityKey = "";
    private long observedAt;
    private String detectorId = "";

    void record(ForegroundAppTracker.Snapshot foreground, GameContextSnapshot context) {
        if (foreground == null
                || !ShizukuGameContextController.isSupportedPackage(foreground.packageName)) {
            clear();
            return;
        }
        armed = true;
        packageName = normalize(foreground.packageName);
        selectedDuringActiveLaunch = isActiveForPackage(context, packageName);
        if (selectedDuringActiveLaunch) {
            pid = context.pid;
            kind = context.kind;
            identityKey = context.identityKey;
            observedAt = context.observedAt;
            detectorId = context.detectorId;
        } else {
            clearLaunch();
        }
    }

    boolean shouldSuppress(ForegroundAppTracker.Snapshot foreground,
            GameContextSnapshot context) {
        if (!armed) return false;
        if (foreground == null || !packageName.equals(normalize(foreground.packageName))) {
            clear();
            return false;
        }

        if (!isActiveForPackage(context, packageName)) {
            // UNKNOWN is absence of evidence, while NONE/menu is not a new game launch.
            return true;
        }
        if (!selectedDuringActiveLaunch) {
            clear();
            return false;
        }
        if (pid == context.pid
                && kind.equals(context.kind)
                && identityKey.equals(context.identityKey)
                && observedAt == context.observedAt
                && detectorId.equals(context.detectorId)) {
            return true;
        }

        clear();
        return false;
    }

    void clear() {
        armed = false;
        packageName = "";
        clearLaunch();
    }

    private void clearLaunch() {
        selectedDuringActiveLaunch = false;
        pid = -1;
        kind = "";
        identityKey = "";
        observedAt = 0L;
        detectorId = "";
    }

    private static boolean isActiveForPackage(GameContextSnapshot context,
            String normalizedPackage) {
        return context != null
                && context.state == GameContextSnapshot.State.ACTIVE
                && normalizedPackage.equals(normalize(context.packageName));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
