package com.mastercook777.heimdall;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

/** Establishes one lower-display input connection without showing a keyboard. */
final class ThorImeSessionInitializer {
    private static final String TAG = "HeimdallImeInit";
    private static final long SESSION_SETTLE_MS = 350L;
    private static final long SESSION_TIMEOUT_MS = 1200L;
    private static final int FOCUS_RELEASE_MAX_FRAMES = 6;

    interface Completion {
        void onComplete(boolean initialized);
    }

    private Activity activity;
    private PrimingEditText input;
    private Completion completion;
    private int previousSoftInputMode;
    private boolean running;
    private boolean releasing;
    private boolean initialized;
    private int focusReleaseFramesRemaining;

    private final Runnable settle = () -> finish(true);
    private final Runnable timeout = () -> finish(false);
    private final Runnable releaseCheck = new Runnable() {
        @Override
        public void run() {
            PrimingEditText currentInput = input;
            if (!releasing || currentInput == null) {
                return;
            }
            if (!currentInput.hasWindowFocus() || focusReleaseFramesRemaining-- <= 0) {
                cleanup(true);
                return;
            }
            currentInput.postOnAnimation(this);
        }
    };

    boolean isHoldingWindowFocus() {
        return running;
    }

    void initialize(
            Activity activity,
            FrameLayout host,
            Runnable requestWindowFocus,
            Completion completion) {
        if (initialized || running || releasing || activity == null || host == null
                || requestWindowFocus == null || completion == null) {
            return;
        }

        running = true;
        this.activity = activity;
        this.completion = completion;

        Window window = activity.getWindow();
        previousSoftInputMode = window.getAttributes().softInputMode;
        int hiddenSoftInputMode =
                (previousSoftInputMode & ~WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE)
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
        window.setSoftInputMode(hiddenSoftInputMode);

        PrimingEditText nextInput = new PrimingEditText(activity);
        input = nextInput;
        nextInput.setOnInputConnectionCreated(() -> {
            if (!running || input != nextInput) {
                return;
            }
            nextInput.removeCallbacks(timeout);
            nextInput.postDelayed(settle, SESSION_SETTLE_MS);
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                1, 1, Gravity.TOP | Gravity.RIGHT);
        host.addView(nextInput, params);
        requestWindowFocus.run();
        nextInput.post(() -> {
            if (running && input == nextInput) {
                nextInput.requestFocus();
            }
        });
        nextInput.postDelayed(timeout, SESSION_TIMEOUT_MS);
        Log.i(TAG, "lower-display IME session initialization started");
    }

    void cancel() {
        if (!running && !releasing) {
            return;
        }
        cleanup(true);
        Log.i(TAG, "lower-display IME session initialization cancelled");
    }

    private void finish(boolean success) {
        if (!running) {
            return;
        }
        initialized = success;
        Completion callback = completion;
        completion = null;
        running = false;
        releasing = true;
        PrimingEditText currentInput = input;
        if (currentInput != null) {
            currentInput.removeCallbacks(settle);
            currentInput.removeCallbacks(timeout);
        }
        Log.i(TAG, "lower-display IME session initialized=" + success
                + "; releasing window focus before detaching input");
        if (callback != null) {
            callback.onComplete(success);
        }
        if (currentInput == null) {
            cleanup(true);
            return;
        }
        focusReleaseFramesRemaining = FOCUS_RELEASE_MAX_FRAMES;
        currentInput.postOnAnimation(releaseCheck);
    }

    private void cleanup(boolean restoreSoftInputMode) {
        PrimingEditText currentInput = input;
        input = null;
        running = false;
        releasing = false;
        if (currentInput != null) {
            currentInput.removeCallbacks(settle);
            currentInput.removeCallbacks(timeout);
            currentInput.removeCallbacks(releaseCheck);
            boolean windowFocusReleased = !currentInput.hasWindowFocus();
            currentInput.clearFocus();
            if (currentInput.getParent() instanceof ViewGroup) {
                ((ViewGroup) currentInput.getParent()).removeView(currentInput);
            }
            boolean inputStillActive = false;
            if (activity != null) {
                InputMethodManager inputMethodManager = (InputMethodManager)
                        activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null) {
                    // isActive() also flushes the public focus check after the served view detaches.
                    inputStillActive = inputMethodManager.isActive(currentInput);
                }
            }
            Log.i(TAG, "lower-display IME input detached windowFocusReleased="
                    + windowFocusReleased + " inputStillActive=" + inputStillActive);
        }
        if (restoreSoftInputMode && activity != null) {
            activity.getWindow().setSoftInputMode(previousSoftInputMode);
        }
        activity = null;
        completion = null;
    }

    private static final class PrimingEditText extends EditText {
        private Runnable onInputConnectionCreated;
        private boolean connectionReported;

        PrimingEditText(Activity activity) {
            super(activity);
            setAlpha(0f);
            setBackgroundColor(Color.TRANSPARENT);
            setCursorVisible(false);
            setFocusable(true);
            setFocusableInTouchMode(true);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
            setInputType(InputType.TYPE_CLASS_TEXT);
            setSaveEnabled(false);
            setShowSoftInputOnFocus(false);
            setSingleLine(true);
        }

        void setOnInputConnectionCreated(Runnable callback) {
            onInputConnectionCreated = callback;
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            InputConnection connection = super.onCreateInputConnection(outAttrs);
            if (connection != null && !connectionReported) {
                connectionReported = true;
                Runnable callback = onInputConnectionCreated;
                if (callback != null) {
                    post(callback);
                }
            }
            return connection;
        }
    }
}
