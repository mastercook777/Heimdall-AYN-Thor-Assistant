package com.mastercook777.heimdall;

import android.app.Activity;
import android.os.Process;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/** Event-only focus telemetry for Thor lifecycle investigations. */
final class ThorFocusDiagnostics {
    private static final String TAG = "HeimdallFocusTrace";
    private static int sequence;

    private ThorFocusDiagnostics() {}

    static synchronized void record(
            Activity activity,
            String event,
            boolean textInputLease,
            boolean startupReadinessVisible) {
        if (activity == null) return;

        Window window = activity.getWindow();
        View decor = window == null ? null : window.getDecorView();
        Display display = decor == null ? null : decor.getDisplay();
        int displayId = display == null ? Display.INVALID_DISPLAY : display.getDisplayId();
        int flags = window == null ? 0 : window.getAttributes().flags;
        boolean notFocusable =
                (flags & WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0;
        boolean altFocusableIme =
                (flags & WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM) != 0;

        String message = "seq=" + (++sequence)
                        + " pid=" + Process.myPid()
                        + " instance=" + Integer.toHexString(
                                System.identityHashCode(activity))
                        + " event=" + event
                        + " display=" + displayId
                        + " task=" + activity.getTaskId()
                        + " taskRoot=" + activity.isTaskRoot()
                        + " attached=" + (decor != null && decor.isAttachedToWindow())
                        + " windowFocus=" + (decor != null && decor.hasWindowFocus())
                        + " viewFocus=" + (decor != null && decor.hasFocus())
                        + " notFocusable=" + notFocusable
                        + " altFocusableIme=" + altFocusableIme
                        + " textLease=" + textInputLease
                        + " readiness=" + startupReadinessVisible
                        + " finishing=" + activity.isFinishing();
        Log.i(TAG, message);
        HeimdallStabilityDiagnostics.recordFocusDiagnostic(activity, message);
    }
}
