package com.mastercook777.heimdall;

import android.os.Handler;
import android.os.Looper;

public final class AssistantMainHandler {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private AssistantMainHandler() {
    }

    public static void post(Runnable runnable) {
        HANDLER.post(runnable);
    }

    public static void postDelayed(Runnable runnable, long delayMillis) {
        HANDLER.postDelayed(runnable, delayMillis);
    }
}
