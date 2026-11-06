package com.mastercook777.heimdall;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Serializes key transitions for the selected uinput keyboard route. */
public final class VirtualKeyboardDispatcher {
    static final int MIN_LINUX_KEY_CODE = 1;
    static final int MAX_LINUX_KEY_CODE = 255;

    private static final AtomicBoolean DEVICE_MAY_BE_OPEN = new AtomicBoolean();
    // Accessed only on DEVICE_EXECUTOR. The resident device retains its owning route.
    private static InputBridge.KeyboardRoute deviceRoute =
            InputBridge.KeyboardRoute.UNAVAILABLE;
    private static final ExecutorService DEVICE_EXECUTOR =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "HeimdallVirtualKeyboard");
                thread.setDaemon(true);
                return thread;
            });
    private static final ScheduledExecutorService LEASE_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "HeimdallKeyboardLease");
                thread.setDaemon(true);
                return thread;
            });

    public interface Listener {
        void onReady();
        void onUnavailable();
    }

    private final Context context;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object lock = new Object();
    private final ArrayDeque<KeyTransition> transitions = new ArrayDeque<>();
    private volatile InputBridge.KeyboardRoute route =
            InputBridge.KeyboardRoute.UNAVAILABLE;
    private final Runnable serviceLossListener;
    private final Runnable directStateListener;
    private boolean drainQueued;
    private volatile boolean closed;
    private final boolean[] heldKeys = new boolean[MAX_LINUX_KEY_CODE + 1];
    private int heldCount;
    private ScheduledFuture<?> heartbeat;
    private final AtomicBoolean heartbeatQueued = new AtomicBoolean();
    private final AtomicBoolean errorReported = new AtomicBoolean();
    private final AtomicBoolean readyReported = new AtomicBoolean();

    public VirtualKeyboardDispatcher(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        serviceLossListener = () -> {
            if (route == InputBridge.KeyboardRoute.SHIZUKU) submit(this::reportUnavailable);
        };
        directStateListener = () -> {
            if (route == InputBridge.KeyboardRoute.THOR_DIRECT
                    && !ThorDirectController.isReady(this.context)) {
                submit(this::reportUnavailable);
            }
        };
    }

    public void start() {
        ShizukuNativeController.addServiceLossListener(serviceLossListener);
        ThorDirectController.addStateListener(directStateListener);
        submit(() -> {
            InputBridge.KeyboardRoute selected = InputBridge.keyboardRoute(context);
            if (selected == InputBridge.KeyboardRoute.UNAVAILABLE) {
                reportUnavailable();
                return;
            }
            boolean priorDeviceTracked = DEVICE_MAY_BE_OPEN.get();
            if (DEVICE_MAY_BE_OPEN.get() && deviceRoute != selected) {
                destroyDeviceNow(context);
                if (DEVICE_MAY_BE_OPEN.get()) {
                    reportUnavailable();
                    return;
                }
            }
            // The Shizuku UserService can outlive a previous app process. Its
            // resident keyboard is not represented by this process's static flag.
            if (!priorDeviceTracked
                    && selected == InputBridge.KeyboardRoute.THOR_DIRECT
                    && ShizukuNativeController.isReady()
                    && !operationSucceeded(
                            ShizukuNativeController.releaseVirtualKeyboard(context))) {
                reportUnavailable();
                return;
            }
            if (!operationSucceeded(openKeyboard(context, selected))) {
                reportUnavailable();
                return;
            }
            DEVICE_MAY_BE_OPEN.set(true);
            deviceRoute = selected;
            route = selected;
            if (readyReported.compareAndSet(false, true)) {
                mainHandler.post(() -> {
                    if (!closed && !errorReported.get()) listener.onReady();
                });
            }
        });
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
        shutdown();
    }

    /** Sends all-up without closing this dispatcher or destroying the uinput device. */
    public void releaseAll() {
        synchronized (lock) {
            if (closed) return;
            transitions.clear();
        }
        DEVICE_EXECUTOR.execute(() -> {
            stopHeartbeat();
            if (route != InputBridge.KeyboardRoute.UNAVAILABLE
                    && !operationSucceeded(releaseKeyboardKeys(context, route))) {
                reportUnavailable();
            }
        });
    }

    static boolean isSupportedKeyCode(int linuxKeyCode) {
        return linuxKeyCode >= MIN_LINUX_KEY_CODE && linuxKeyCode <= MAX_LINUX_KEY_CODE;
    }

    private void shutdown() {
        synchronized (lock) {
            if (closed) return;
            closed = true;
            transitions.clear();
        }
        ShizukuNativeController.removeServiceLossListener(serviceLossListener);
        ThorDirectController.removeStateListener(directStateListener);
        DEVICE_EXECUTOR.execute(this::releaseKeys);
    }

    private void releaseKeys() {
        stopHeartbeat();
        if (!DEVICE_MAY_BE_OPEN.get()) return;
        String allUp = releaseKeyboardKeys(context, deviceRoute);
        if (!operationSucceeded(allUp)) {
            destroyDeviceNow(context);
        }
        route = InputBridge.KeyboardRoute.UNAVAILABLE;
    }

    private void drainTransitions() {
        while (true) {
            KeyTransition transition;
            synchronized (lock) {
                if (closed || errorReported.get()) {
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
            if (route == InputBridge.KeyboardRoute.UNAVAILABLE
                    || !operationSucceeded(emitKeyboard(context, route,
                            transition.linuxKeyCode, transition.pressed ? 1 : 0))) {
                reportUnavailable();
                return;
            }
            if (route == InputBridge.KeyboardRoute.THOR_DIRECT) {
                int code = transition.linuxKeyCode;
                if (heldKeys[code] != transition.pressed) {
                    heldKeys[code] = transition.pressed;
                    heldCount += transition.pressed ? 1 : -1;
                }
                if (heldCount > 0) startHeartbeat();
                else stopHeartbeat();
            }
        }
    }

    private void startHeartbeat() {
        if (heartbeat != null) return;
        heartbeat = LEASE_EXECUTOR.scheduleAtFixedRate(() -> {
            if (!heartbeatQueued.compareAndSet(false, true)) return;
            DEVICE_EXECUTOR.execute(() -> {
                try {
                    if (!closed && heldCount > 0
                            && route == InputBridge.KeyboardRoute.THOR_DIRECT
                            && !operationSucceeded(emitKeyboard(context, route, 0, 0))) {
                        reportUnavailable();
                    }
                } finally {
                    heartbeatQueued.set(false);
                }
            });
        }, 300L, 300L, TimeUnit.MILLISECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeat != null) {
            heartbeat.cancel(false);
            heartbeat = null;
        }
        java.util.Arrays.fill(heldKeys, false);
        heldCount = 0;
    }

    private void submit(Runnable work) {
        synchronized (lock) {
            if (closed) return;
        }
        DEVICE_EXECUTOR.execute(work);
    }

    private void reportUnavailable() {
        if (closed) return;
        stopHeartbeat();
        readyReported.set(false);
        if (!errorReported.compareAndSet(false, true)) return;
        mainHandler.post(() -> {
            if (!closed) listener.onUnavailable();
        });
    }

    private static void destroyDeviceNow(Context context) {
        if (!DEVICE_MAY_BE_OPEN.get()) return;
        releaseKeyboardKeys(context, deviceRoute);
        if (operationSucceeded(releaseKeyboard(context, deviceRoute))) {
            DEVICE_MAY_BE_OPEN.set(false);
            deviceRoute = InputBridge.KeyboardRoute.UNAVAILABLE;
        }
    }

    private static String openKeyboard(Context context, InputBridge.KeyboardRoute selected) {
        return selected == InputBridge.KeyboardRoute.THOR_DIRECT
                ? ThorDirectController.openVirtualKeyboard(context)
                : ShizukuNativeController.openVirtualKeyboard(context);
    }

    private static String emitKeyboard(Context context, InputBridge.KeyboardRoute selected,
            int code, int value) {
        return selected == InputBridge.KeyboardRoute.THOR_DIRECT
                ? ThorDirectController.emitVirtualKeyboard(context, code, value)
                : ShizukuNativeController.emitVirtualKeyboard(context, code, value);
    }

    private static String releaseKeyboardKeys(Context context,
            InputBridge.KeyboardRoute selected) {
        return selected == InputBridge.KeyboardRoute.THOR_DIRECT
                ? ThorDirectController.releaseVirtualKeyboardKeys(context)
                : ShizukuNativeController.releaseVirtualKeyboardKeys(context);
    }

    private static String releaseKeyboard(Context context,
            InputBridge.KeyboardRoute selected) {
        if (selected == InputBridge.KeyboardRoute.THOR_DIRECT) {
            String result = ThorDirectController.releaseVirtualKeyboard(context);
            // OP_STOP releases every key and destroys the device before acknowledging.
            return !operationSucceeded(result) && ThorDirectController.helperStopConfirmed()
                    ? "ok: helper already stopped" : result;
        }
        return ShizukuNativeController.releaseVirtualKeyboard(context);
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
