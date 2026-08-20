package com.mastercook777.heimdall;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Serializes key transitions for the Shizuku uinput keyboard. */
public final class VirtualKeyboardDispatcher {
    static final int MIN_LINUX_KEY_CODE = 1;
    static final int MAX_LINUX_KEY_CODE = 255;

    private static final AtomicBoolean DEVICE_MAY_BE_OPEN = new AtomicBoolean();
    private static final ExecutorService DEVICE_EXECUTOR =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "HeimdallVirtualKeyboard");
                thread.setDaemon(true);
                return thread;
            });

    public interface Listener {
        void onUnavailable();
    }

    private final Context context;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();
    private final ArrayDeque<KeyTransition> transitions = new ArrayDeque<>();
    private boolean drainQueued;
    private boolean closed;
    private final AtomicBoolean errorReported = new AtomicBoolean();

    public VirtualKeyboardDispatcher(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void start() {
        submit(() -> handleResult(ShizukuNativeController.openVirtualKeyboard(context)));
    }

    public void key(int linuxKeyCode, boolean pressed) {
        if (!isSupportedKeyCode(linuxKeyCode)) {
            return;
        }
        synchronized (lock) {
            if (closed) return;
            transitions.addLast(new KeyTransition(linuxKeyCode, pressed));
            if (drainQueued) return;
            drainQueued = true;
            DEVICE_EXECUTOR.execute(this::drainTransitions);
        }
    }

    /** Sends all-up while preserving the enumerated device across internal page changes. */
    public void park() {
        shutdown(false);
    }

    /** Sends all-up and destroys the device when keyboard mapping is no longer in use. */
    public void close() {
        shutdown(true);
    }

    /** Destroys a device parked by an earlier Activity after keyboard mapping becomes unused. */
    public static void destroyParkedDevice(Context context) {
        Context appContext = context.getApplicationContext();
        DEVICE_EXECUTOR.execute(() -> {
            if (!DEVICE_MAY_BE_OPEN.get()) return;
            if (operationSucceeded(
                    ShizukuNativeController.releaseVirtualKeyboard(appContext))) {
                DEVICE_MAY_BE_OPEN.set(false);
            }
        });
    }

    static boolean isSupportedKeyCode(int linuxKeyCode) {
        return linuxKeyCode >= MIN_LINUX_KEY_CODE && linuxKeyCode <= MAX_LINUX_KEY_CODE;
    }

    private void shutdown(boolean destroyDevice) {
        synchronized (lock) {
            if (closed) return;
            closed = true;
            transitions.clear();
        }
        DEVICE_EXECUTOR.execute(() -> releaseKeysAndMaybeDestroy(destroyDevice));
    }

    private void releaseKeysAndMaybeDestroy(boolean destroyDevice) {
        if (!DEVICE_MAY_BE_OPEN.get()) return;
        String allUp = ShizukuNativeController.releaseVirtualKeyboardKeys(context);
        if (!operationSucceeded(allUp)) {
            DEVICE_MAY_BE_OPEN.set(false);
            return;
        }
        if (destroyDevice && operationSucceeded(
                ShizukuNativeController.releaseVirtualKeyboard(context))) {
            DEVICE_MAY_BE_OPEN.set(false);
        }
    }

    private void drainTransitions() {
        while (true) {
            KeyTransition transition;
            synchronized (lock) {
                if (closed) {
                    transitions.clear();
                    drainQueued = false;
                    return;
                }
                transition = transitions.pollFirst();
                if (transition == null) {
                    drainQueued = false;
                    return;
                }
            }
            handleResult(ShizukuNativeController.emitVirtualKeyboard(
                    context, transition.linuxKeyCode, transition.pressed ? 1 : 0));
        }
    }

    private void submit(Runnable work) {
        synchronized (lock) {
            if (closed) return;
        }
        DEVICE_EXECUTOR.execute(work);
    }

    private void handleResult(String result) {
        if (operationSucceeded(result)) {
            DEVICE_MAY_BE_OPEN.set(true);
            errorReported.set(false);
            return;
        }
        DEVICE_MAY_BE_OPEN.set(false);
        reportUnavailable();
    }

    private void reportUnavailable() {
        if (!errorReported.compareAndSet(false, true)) return;
        mainHandler.post(listener::onUnavailable);
    }

    private static boolean operationSucceeded(String result) {
        return result != null && result.startsWith("ok");
    }

    private static final class KeyTransition {
        final int linuxKeyCode;
        final boolean pressed;

        KeyTransition(int linuxKeyCode, boolean pressed) {
            this.linuxKeyCode = linuxKeyCode;
            this.pressed = pressed;
        }
    }
}
