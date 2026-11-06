package com.mastercook777.heimdall;

import android.service.notification.NotificationListenerService;
import android.os.Handler;
import android.os.Looper;

/** Grants access to active MediaSessions; notification content is never read. */
public final class QuickMediaSessionListenerService extends NotificationListenerService {
    private static volatile Runnable connectionListener;

    static void setConnectionListener(Runnable listener) {
        connectionListener = listener;
    }

    @Override
    public void onListenerConnected() {
        dispatchConnectionChanged();
    }

    @Override
    public void onListenerDisconnected() {
        dispatchConnectionChanged();
    }

    private void dispatchConnectionChanged() {
        Runnable listener = connectionListener;
        if (listener != null) new Handler(Looper.getMainLooper()).post(listener);
    }
}
