package com.mastercook777.heimdall;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Serializes and coalesces the high-frequency Touchpad stream for the Shizuku uinput mouse. */
public final class VirtualMouseDispatcher {
    private static final int LINUX_BTN_LEFT = 272;
    private static final int LINUX_BTN_RIGHT = 273;
    private static final AtomicBoolean DEVICE_MAY_BE_OPEN = new AtomicBoolean();
    private static final ExecutorService DEVICE_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, "HeimdallVirtualMouse");
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
    private float residualX;
    private float residualY;
    private boolean drainQueued;
    private boolean closed;
    private boolean errorReported;

    public VirtualMouseDispatcher(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void start() {
        submit(() -> handleResult(ShizukuNativeController.openVirtualMouse(context)));
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
        DEVICE_EXECUTOR.execute(() -> destroyParkedDeviceNow(appContext));
    }

    private void shutdown(boolean destroyDevice) {
        synchronized (lock) {
            if (closed) return;
            closed = true;
            frames.clear();
            residualX = 0f;
            residualY = 0f;
        }
        DEVICE_EXECUTOR.execute(() -> releaseButtonsAndMaybeDestroy(context, destroyDevice));
    }

    private static void releaseButtonsAndMaybeDestroy(Context context, boolean destroyDevice) {
        if (!DEVICE_MAY_BE_OPEN.get()) {
            return;
        }
        // Every departure is all-up. Parking deliberately omits UI_DEV_DESTROY so a returning
        // Activity does not present Steam Link with a newly hot-plugged mouse.
        ShizukuNativeController.emitVirtualMouse(
                context, 0, 0, 0, LINUX_BTN_LEFT, 0);
        ShizukuNativeController.emitVirtualMouse(
                context, 0, 0, 0, LINUX_BTN_RIGHT, 0);
        if (destroyDevice && operationSucceeded(
                ShizukuNativeController.releaseVirtualMouse(context))) {
            DEVICE_MAY_BE_OPEN.set(false);
        }
    }

    private static void destroyParkedDeviceNow(Context context) {
        if (!DEVICE_MAY_BE_OPEN.get()) {
            return;
        }
        // Parking already sent all-up. Native release repeats its tracked-button fallback
        // without auto-opening a missing device after a Shizuku service restart.
        if (operationSucceeded(ShizukuNativeController.releaseVirtualMouse(context))) {
            DEVICE_MAY_BE_OPEN.set(false);
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
                if (closed) {
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
            if (dx == 0 && dy == 0 && frame.wheel == 0 && frame.button == 0) {
                continue;
            }
            handleResult(ShizukuNativeController.emitVirtualMouse(
                    context, dx, dy, frame.wheel, frame.button, frame.buttonValue));
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
            errorReported = false;
            return;
        }
        if (errorReported) return;
        errorReported = true;
        mainHandler.post(listener::onUnavailable);
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
