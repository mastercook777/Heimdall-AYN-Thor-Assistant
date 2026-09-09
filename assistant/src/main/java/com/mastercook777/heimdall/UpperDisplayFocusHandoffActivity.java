package com.mastercook777.heimdall;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.View;

/**
 * Briefly owns focus on Thor's upper display, then returns it to the existing upper task.
 *
 * <p>This intentionally uses a real, transparent display-0 Activity. It is the owner-selected
 * compromise for reliable Back ownership during active play; it does not inject or intercept
 * Back and it does not depend on Accessibility, Shizuku, or Game Context.</p>
 */
public final class UpperDisplayFocusHandoffActivity extends Activity {
    private static final String TAG = "HeimdallGameFocus";
    private static final long FINISH_TIMEOUT_MS = 500L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finishTimeout = this::finishHandoff;

    static boolean launch(Activity source) {
        if (source == null || source.isFinishing() || source.isDestroyed()) {
            return false;
        }
        View decor = source.getWindow().getDecorView();
        Display sourceDisplay = decor == null ? null : decor.getDisplay();
        if (sourceDisplay == null || sourceDisplay.getDisplayId() == Display.DEFAULT_DISPLAY) {
            record(source, "explicit-handoff skipped-lower-display-unresolved");
            return false;
        }
        DisplayManager displayManager = source.getSystemService(DisplayManager.class);
        Display upperDisplay = displayManager == null
                ? null : displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (upperDisplay == null || !upperDisplay.isValid()) {
            record(source, "explicit-handoff skipped-default-display-unavailable");
            return false;
        }

        Intent intent = new Intent(source, UpperDisplayFocusHandoffActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(Display.DEFAULT_DISPLAY);
        try {
            source.startActivity(intent, options.toBundle());
            record(source, "explicit-handoff requested sourceDisplay="
                    + sourceDisplay.getDisplayId());
            return true;
        } catch (RuntimeException error) {
            Log.e(TAG, "upper focus handoff launch failed", error);
            record(source, "explicit-handoff failed type="
                    + error.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new View(this));
        overridePendingTransition(0, 0);
        handler.postDelayed(finishTimeout, FINISH_TIMEOUT_MS);
        Display display = getWindow().getDecorView().getDisplay();
        int displayId = display == null ? Display.INVALID_DISPLAY : display.getDisplayId();
        record(this, "explicit-handoff attached display=" + displayId);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            record(this, "explicit-handoff window-focus=true");
            getWindow().getDecorView().post(this::finishHandoff);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(finishTimeout);
        super.onDestroy();
    }

    private void finishHandoff() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        handler.removeCallbacks(finishTimeout);
        record(this, "explicit-handoff complete");
        finishAndRemoveTask();
        overridePendingTransition(0, 0);
    }

    private static void record(Activity activity, String message) {
        Log.i(TAG, message);
        HeimdallStabilityDiagnostics.recordFocusDiagnostic(activity, message);
    }
}
