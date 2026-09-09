package com.mastercook777.heimdall;

import android.view.Display;

/** Pure, fail-closed target selection for the lower-display launcher entry. */
final class HeimdallLaunchRouter {
    private HeimdallLaunchRouter() {}

    static int selectTargetDisplayId(int sourceDisplayId,
            int[] displayIds, int[] displayStates) {
        if (displayIds == null || displayStates == null
                || displayIds.length != displayStates.length) {
            return Display.INVALID_DISPLAY;
        }

        if (sourceDisplayId != Display.DEFAULT_DISPLAY) {
            for (int index = 0; index < displayIds.length; index++) {
                if (displayIds[index] == sourceDisplayId
                        && displayStates[index] != Display.STATE_OFF) {
                    return sourceDisplayId;
                }
            }
        }

        int firstAvailableSecondary = Display.INVALID_DISPLAY;
        for (int index = 0; index < displayIds.length; index++) {
            int displayId = displayIds[index];
            if (displayId == Display.DEFAULT_DISPLAY
                    || displayStates[index] == Display.STATE_OFF) {
                continue;
            }
            if (firstAvailableSecondary == Display.INVALID_DISPLAY) {
                firstAvailableSecondary = displayId;
            }
            if (displayStates[index] == Display.STATE_ON) {
                return displayId;
            }
        }
        return firstAvailableSecondary;
    }
}
