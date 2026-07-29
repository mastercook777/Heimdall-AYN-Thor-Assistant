package com.mastercook777.heimdall;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/** Grants Heimdall window focus only for an explicit lower-screen text-input session. */
final class ThorTextInputFocusLease {
    private static final String TAG = "HeimdallTextFocus";
    private static final int RELEASE_MAX_FRAMES = 6;

    private Activity activity;
    private EditText activeInput;
    private Runnable onReleased;
    private Runnable pendingAfterRelease;
    private boolean holdingWindowFocus;
    private boolean releasingWindowFocus;
    private int releaseFramesRemaining;

    private final Runnable releaseCheck = new Runnable() {
        @Override
        public void run() {
            if (!releasingWindowFocus) {
                return;
            }
            View decor = activity == null ? null : activity.getWindow().getDecorView();
            boolean windowFocusReleased = decor == null || !decor.hasWindowFocus();
            if (windowFocusReleased || releaseFramesRemaining-- <= 0) {
                finishRelease(windowFocusReleased);
                return;
            }
            View target = activeInput != null ? activeInput : decor;
            if (target == null) {
                finishRelease(false);
                return;
            }
            target.postOnAnimation(this);
        }
    };

    void acquire(Activity activity, ThorGameFocusProtection focusProtection, EditText input) {
        if (activity == null || focusProtection == null || input == null
                || releasingWindowFocus) {
            return;
        }
        this.activity = activity;
        activeInput = input;
        holdingWindowFocus = true;
        focusProtection.apply(activity, false);
        requestInputFocus();
        Log.i(TAG, "text-input focus acquired");
    }

    void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && holdingWindowFocus && !releasingWindowFocus) {
            requestInputFocus();
        }
    }

    boolean release(
            Activity activity,
            ThorGameFocusProtection focusProtection,
            Runnable onReleased,
            Runnable afterRelease) {
        if (releasingWindowFocus) {
            if (pendingAfterRelease == null && afterRelease != null) {
                pendingAfterRelease = afterRelease;
            }
            return true;
        }
        if (!holdingWindowFocus) {
            return false;
        }

        this.activity = activity;
        this.onReleased = onReleased;
        pendingAfterRelease = afterRelease;
        holdingWindowFocus = false;
        releasingWindowFocus = true;
        releaseFramesRemaining = RELEASE_MAX_FRAMES;

        hideKeyboard();
        focusProtection.apply(activity, true);
        View target = activeInput != null
                ? activeInput
                : activity.getWindow().getDecorView();
        if (target == null) {
            finishRelease(false);
        } else {
            target.postOnAnimation(releaseCheck);
        }
        Log.i(TAG, "text-input focus release requested");
        return true;
    }

    boolean isHoldingWindowFocus() {
        return holdingWindowFocus;
    }

    boolean isActiveInputInside(View root) {
        if (root == null || activeInput == null) {
            return false;
        }
        if (activeInput == root) {
            return true;
        }
        ViewParent current = activeInput.getParent();
        while (current != null) {
            if (current == root) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    void cancel() {
        View target = activeInput;
        if (target != null) {
            target.removeCallbacks(releaseCheck);
            target.clearFocus();
        }
        activity = null;
        activeInput = null;
        onReleased = null;
        pendingAfterRelease = null;
        holdingWindowFocus = false;
        releasingWindowFocus = false;
    }

    private void requestInputFocus() {
        EditText input = activeInput;
        if (input == null) {
            return;
        }
        input.post(() -> {
            if (!holdingWindowFocus || releasingWindowFocus || input != activeInput
                    || input.getWindowToken() == null) {
                return;
            }
            input.requestFocus();
            InputMethodManager manager = (InputMethodManager)
                    input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager != null) {
                manager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void hideKeyboard() {
        EditText input = activeInput;
        if (input == null) {
            return;
        }
        InputMethodManager manager = (InputMethodManager)
                input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null && input.getWindowToken() != null) {
            manager.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
    }

    private void finishRelease(boolean windowFocusReleased) {
        EditText input = activeInput;
        if (input != null) {
            input.removeCallbacks(releaseCheck);
            input.clearFocus();
        }
        Runnable released = onReleased;
        Runnable after = pendingAfterRelease;
        activity = null;
        activeInput = null;
        onReleased = null;
        pendingAfterRelease = null;
        holdingWindowFocus = false;
        releasingWindowFocus = false;
        Log.i(TAG, "text-input focus released windowFocusReleased=" + windowFocusReleased);
        if (released != null) {
            released.run();
        }
        if (after != null) {
            after.run();
        }
    }
}
