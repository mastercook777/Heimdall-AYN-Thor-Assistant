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

/** Serializes and coalesces Touchpad input for the selected uinput mouse route. */
public final class VirtualMouseDispatcher {
    private static final int LINUX_BTN_LEFT = 272;
    private static final int LINUX_BTN_RIGHT = 273;
    private static final int LINUX_BTN_MIDDLE = 274;
    private static final AtomicBoolean DEVICE_MAY_BE_OPEN = new AtomicBoolean();
    // Accessed only on DEVICE_EXECUTOR; a parked device retains its route.
    private static InputBridge.VirtualMouseRoute deviceRoute =
            InputBridge.VirtualMouseRoute.UNAVAILABLE;
    private static final ExecutorService DEVICE_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "HeimdallVirtualMouse");
                thread.setDaemon(true);
                return thread;
            });
    private static final ScheduledExecutorService LEASE_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "HeimdallMouseLease");
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
    private final ArrayDeque<MouseFrame> frames = new ArrayDeque<>();
    private volatile InputBridge.VirtualMouseRoute route =
            InputBridge.VirtualMouseRoute.UNAVAILABLE;
    private final Runnable directStateListener;
    private final Runnable shizukuLossListener;
    private float residualX;
    private float residualY;
    private boolean drainQueued;
    private boolean closed;
    private boolean errorReported;
    private boolean heldButton;
    private final AtomicBoolean heartbeatQueued = new AtomicBoolean();
    private ScheduledFuture<?> heartbeat;

    public VirtualMouseDispatcher(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        directStateListener = () -> {
            if (route == InputBridge.VirtualMouseRoute.THOR_DIRECT
                    && !ThorDirectController.isReady(this.context)) {
                submit(this::reportUnavailable);
            }
        };
        shizukuLossListener = () -> {
            if (route == InputBridge.VirtualMouseRoute.SHIZUKU) {
                submit(this::reportUnavailable);
            }
        };
    }

    public void start() {
        ThorDirectController.addStateListener(directStateListener);
        ShizukuNativeController.addServiceLossListener(shizukuLossListener);
        submit(() -> {
            InputBridge.VirtualMouseRoute selected = InputBridge.virtualMouseRoute(context);
            if (selected == InputBridge.VirtualMouseRoute.UNAVAILABLE) {
                reportUnavailable();
                return;
            }
            if (DEVICE_MAY_BE_OPEN.get() && deviceRoute != selected) {
                releaseButtonsAndMaybeDestroy(context, true);
                if (DEVICE_MAY_BE_OPEN.get()) {
                    reportUnavailable();
                    return;
                }
            }
            if (!operationSucceeded(openMouse(context, selected))) {
                reportUnavailable();
                return;
            }
            DEVICE_MAY_BE_OPEN.set(true);
            deviceRoute = selected;
            route = selected;
        });
    }

    public void move(float dx, float dy) {
        synchronized (lock) {
            if (closed) return;
            residualX += dx;
            residualY += dy;
            int wholeX = wholePixels(residualX);
            int wholeY = wholePixels(residualY);
            residualX -= wholeX;
            residualY -= wholeY;
            if (wholeX == 0 && wholeY == 0) return;
            enqueueLocked(MouseFrame.move(wholeX, wholeY), true);
        }
    }

    public void wheel(int amount) {
        if (amount == 0) return;
        enqueue(MouseFrame.wheel(amount), false);
    }

    public void button(int linuxButtonCode, boolean pressed) {
        enqueue(MouseFrame.button(linuxButtonCode, pressed), false);
    }

    public void close() {
        shutdown(true);
    }

    /** Releases held buttons while keeping the uinput device available across Activity pauses. */
    public void park() {
        shutdown(false);
    }

    /** Destroys a device parked by an earlier Activity after Virtual Mouse becomes unused. */
    public static void destroyParkedDevice(Context context) {
        Context appContext = context.getApplicationContext();
        DEVICE_EXECUTOR.execute(() -> releaseButtonsAndMaybeDestroy(appContext, true));
    }

    private void shutdown(boolean destroyDevice) {
        synchronized (lock) {
            if (closed) return;
            closed = true;
            frames.clear();
            residualX = 0f;
            residualY = 0f;
        }
        ThorDirectController.removeStateListener(directStateListener);
        ShizukuNativeController.removeServiceLossListener(shizukuLossListener);
        DEVICE_EXECUTOR.execute(() -> {
            stopHeartbeat();
            route = InputBridge.VirtualMouseRoute.UNAVAILABLE;
            releaseButtonsAndMaybeDestroy(context, destroyDevice);
        });
    }

    private static void releaseButtonsAndMaybeDestroy(Context context, boolean destroyDevice) {
        if (!DEVICE_MAY_BE_OPEN.get()) {
            return;
        }
        // Every departure is all-up. Parking deliberately omits UI_DEV_DESTROY so a returning
        // Activity does not present Steam Link with a newly hot-plugged mouse.
        emitMouse(context, deviceRoute, 0, 0, 0, LINUX_BTN_LEFT, 0);
        emitMouse(context, deviceRoute, 0, 0, 0, LINUX_BTN_RIGHT, 0);
        emitMouse(context, deviceRoute, 0, 0, 0, LINUX_BTN_MIDDLE, 0);
        if (destroyDevice && operationSucceeded(releaseMouse(context, deviceRoute))) {
            DEVICE_MAY_BE_OPEN.set(false);
            deviceRoute = InputBridge.VirtualMouseRoute.UNAVAILABLE;
        }
    }

    private void enqueue(MouseFrame frame, boolean mergeAdjacentMove) {
        synchronized (lock) {
            if (closed) return;
            enqueueLocked(frame, mergeAdjacentMove);
        }
    }

    private void enqueueLocked(MouseFrame frame, boolean mergeAdjacentMove) {
        MouseFrame tail = frames.peekLast();
        if (mergeAdjacentMove && tail != null && tail.isMove()) {
            tail.dx += frame.dx;
            tail.dy += frame.dy;
        } else {
            frames.addLast(frame);
        }
        if (drainQueued) return;
        drainQueued = true;
        DEVICE_EXECUTOR.execute(this::drainFrames);
    }

    private void drainFrames() {
        while (true) {
            MouseFrame frame;
            synchronized (lock) {
                if (closed || errorReported) {
                    frames.clear();
                    drainQueued = false;
                    return;
                }
                frame = frames.pollFirst();
                if (frame == null) {
                    drainQueued = false;
                    return;
                }
            }
            int dx = Math.round(frame.dx);
            int dy = Math.round(frame.dy);
            if (route == InputBridge.VirtualMouseRoute.UNAVAILABLE) {
                reportUnavailable();
                return;
            }
            if (dx == 0 && dy == 0 && frame.wheel == 0 && frame.button == 0) {
                continue;
            }
            if (!operationSucceeded(emitMouse(context, route,
                    dx, dy, frame.wheel, frame.button, frame.buttonValue))) {
                reportUnavailable();
                return;
            }
            if (route == InputBridge.VirtualMouseRoute.THOR_DIRECT && frame.button != 0) {
                heldButton = frame.buttonValue == 1;
                if (heldButton) startHeartbeat();
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
                    if (!closed && heldButton
                            && route == InputBridge.VirtualMouseRoute.THOR_DIRECT
                            && !operationSucceeded(emitMouse(context, route,
                                    0, 0, 0, 0, 0))) {
                        reportUnavailable();
                    }
                } finally {
                    heartbeatQueued.set(false);
                }
            });
        }, 300L, 300L, TimeUnit.MILLISECONDS);
    }

    private void stopHeartbeat() {
        heldButton = false;
        if (heartbeat != null) {
            heartbeat.cancel(false);
            heartbeat = null;
        }
    }

    private void submit(Runnable work) {
        synchronized (lock) {
            if (closed) return;
        }
        DEVICE_EXECUTOR.execute(work);
    }

    private void reportUnavailable() {
        if (errorReported) return;
        errorReported = true;
        stopHeartbeat();
        mainHandler.post(listener::onUnavailable);
    }

    private static String openMouse(Context context, InputBridge.VirtualMouseRoute selected) {
        return selected == InputBridge.VirtualMouseRoute.THOR_DIRECT
                ? ThorDirectController.openVirtualMouse(context)
                : ShizukuNativeController.openVirtualMouse(context);
    }

    private static String emitMouse(Context context, InputBridge.VirtualMouseRoute selected,
            int dx, int dy, int wheel, int button, int value) {
        return selected == InputBridge.VirtualMouseRoute.THOR_DIRECT
                ? ThorDirectController.emitVirtualMouse(context, dx, dy, wheel, button, value)
                : ShizukuNativeController.emitVirtualMouse(context, dx, dy, wheel, button, value);
    }

    private static String releaseMouse(Context context, InputBridge.VirtualMouseRoute selected) {
        if (selected == InputBridge.VirtualMouseRoute.THOR_DIRECT) {
            String result = ThorDirectController.releaseVirtualMouse(context);
            // OP_STOP releases the mouse before acknowledging shutdown. Its socket may
            // already be gone when this queued cleanup reaches the device worker.
            return !operationSucceeded(result) && ThorDirectController.helperStopConfirmed()
                    ? "ok: helper already stopped" : result;
        }
        return ShizukuNativeController.releaseVirtualMouse(context);
    }

    private static boolean operationSucceeded(String result) {
        return result != null && result.startsWith("ok");
    }

    private static int wholePixels(float value) {
        return value >= 0f ? (int) Math.floor(value) : (int) Math.ceil(value);
    }

    private static final class MouseFrame {
        float dx;
        float dy;
        final int wheel;
        final int button;
        final int buttonValue;

        private MouseFrame(float dx, float dy, int wheel, int button, int buttonValue) {
            this.dx = dx;
            this.dy = dy;
            this.wheel = wheel;
            this.button = button;
            this.buttonValue = buttonValue;
        }

        static MouseFrame move(float dx, float dy) {
            return new MouseFrame(dx, dy, 0, 0, 0);
        }

        static MouseFrame wheel(int amount) {
            return new MouseFrame(0f, 0f, amount, 0, 0);
        }

        static MouseFrame button(int linuxButtonCode, boolean pressed) {
            return new MouseFrame(0f, 0f, 0, linuxButtonCode, pressed ? 1 : 0);
        }

        boolean isMove() {
            return wheel == 0 && button == 0;
        }
    }
}
