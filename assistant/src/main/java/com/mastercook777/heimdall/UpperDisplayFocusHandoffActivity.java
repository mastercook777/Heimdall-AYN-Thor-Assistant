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
 * <p>Heimdall's lower Activity must remain non-focusable for game performance. Launching that
 * Activity can nevertheless leave Android's global focused display on the lower panel, where no
 * focused window exists. A display-unspecified Back/Home event then has no dispatch target. This
 * transparent one-shot task uses the public multi-display Activity launch path to move focus to
 * display 0 without injecting input or relaunching the user's upper-screen application.</p>
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
            Log.w(TAG, "upper focus handoff skipped; Heimdall lower display unresolved");
            return false;
        }
        DisplayManager displayManager = source.getSystemService(DisplayManager.class);
        Display upperDisplay = displayManager == null
                ? null : displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        if (upperDisplay == null || !upperDisplay.isValid()) {
            Log.w(TAG, "upper focus handoff skipped; default display unavailable");
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
            Log.i(TAG, "upper focus handoff requested from display="
                    + sourceDisplay.getDisplayId());
            return true;
        } catch (Throwable throwable) {
            Log.e(TAG, "upper focus handoff launch failed", throwable);
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
        Log.i(TAG, "upper focus handoff attached display="
                + (display == null ? Display.INVALID_DISPLAY : display.getDisplayId()));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
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
        Log.i(TAG, "upper focus handoff complete");
        finishAndRemoveTask();
        overridePendingTransition(0, 0);
    }
}
