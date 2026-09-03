package com.mastercook777.heimdall;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ClipData;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;

/**
 * Transparent, non-focusable package entry that places the persistent assistant on a
 * verified secondary display. It never creates an upper focus-handoff surface.
 */
public final class HeimdallLaunchActivity extends Activity {
    private static final int FORWARDED_GRANT_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        super.onCreate(savedInstanceState);

        Intent sourceIntent = getIntent();
        int sourceFlags = sourceIntent == null ? 0 : sourceIntent.getFlags();
        int sourceDisplayId = resolveSourceDisplayId();
        int targetDisplayId = resolveTargetDisplayId(sourceDisplayId);
        HeimdallStabilityDiagnostics.recordFocusDiagnostic(this,
                "launch-router sourceDisplay=" + sourceDisplayId
                        + " targetDisplay=" + targetDisplayId
                        + " launchTask=" + getTaskId()
                        + " sourceFlags=0x" + Integer.toHexString(sourceFlags));
        if (targetDisplayId == Display.INVALID_DISPLAY
                || targetDisplayId == Display.DEFAULT_DISPLAY) {
            HeimdallStabilityDiagnostics.recordFocusDiagnostic(this,
                    "launch-router failed-no-secondary-display");
            finishWithoutAnimation();
            return;
        }

        try {
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(targetDisplayId);
            startActivity(buildTargetIntent(sourceIntent), options.toBundle());
            HeimdallStabilityDiagnostics.recordFocusDiagnostic(this,
                    "launch-router dispatched targetDisplay=" + targetDisplayId);
        } catch (RuntimeException error) {
            HeimdallStabilityDiagnostics.recordFocusDiagnostic(this,
                    "launch-router failed type=" + error.getClass().getSimpleName());
        }
        finishWithoutAnimation();
    }

    private Intent buildTargetIntent(Intent source) {
        Intent target = new Intent(this, AssistantActivity.class);
        if (source != null) {
            target.setAction(source.getAction());
            if (source.getData() != null || source.getType() != null) {
                target.setDataAndType(source.getData(), source.getType());
            }
            Bundle extras = source.getExtras();
            if (extras != null) {
                target.putExtras(extras);
            }
            ClipData clipData = source.getClipData();
            if (clipData != null) {
                target.setClipData(clipData);
            }
            target.addFlags(source.getFlags() & FORWARDED_GRANT_FLAGS);
        }
        target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return target;
    }

    private int resolveSourceDisplayId() {
        Display display = getWindowManager().getDefaultDisplay();
        return display == null ? Display.INVALID_DISPLAY : display.getDisplayId();
    }

    private int resolveTargetDisplayId(int sourceDisplayId) {
        DisplayManager manager = getSystemService(DisplayManager.class);
        Display[] displays = manager == null ? new Display[0] : manager.getDisplays();
        int[] ids = new int[displays.length];
        int[] states = new int[displays.length];
        for (int index = 0; index < displays.length; index++) {
            ids[index] = displays[index].getDisplayId();
            states[index] = displays[index].getState();
        }
        return HeimdallLaunchRouter.selectTargetDisplayId(
                sourceDisplayId, ids, states);
    }

    private void finishWithoutAnimation() {
        finish();
        overridePendingTransition(0, 0);
    }
}
