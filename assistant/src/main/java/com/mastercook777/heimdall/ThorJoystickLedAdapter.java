package com.mastercook777.heimdall;

import android.graphics.Color;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Minimal best-effort adapter for Thor's firmware-owned joystick LEDs. */
final class ThorJoystickLedAdapter {
    static final String LEFT_ENABLE_NODE = "/sys/class/sn3112l/led/enable";
    static final String RIGHT_ENABLE_NODE = "/sys/class/sn3112r/led/enable";
    static final String LEFT_BRIGHTNESS_NODE = "/sys/class/sn3112l/led/brightness";
    static final String RIGHT_BRIGHTNESS_NODE = "/sys/class/sn3112r/led/brightness";

    private static final int MAX_WRITE_ATTEMPTS = 20;
    private static final long WRITE_RETRY_DELAY_MS = 50L;

    private final Object stateLock = new Object();
    private final Executor writerExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "heimdall-joystick-led");
        thread.setDaemon(true);
        return thread;
    });

    private PendingState pendingState;
    private boolean writerScheduled;

    void applyThemeColor(int color) {
        submitState(new PendingState(true, buildStaticColorPayload(color)));
    }

    void turnOff() {
        submitState(new PendingState(false, null));
    }

    static String buildStaticColorPayload(int color) {
        return "1-" + Color.red(color)
                + ":" + Color.green(color)
                + ":" + Color.blue(color);
    }

    private void submitState(PendingState state) {
        synchronized (stateLock) {
            pendingState = state;
            if (writerScheduled) {
                return;
            }
            writerScheduled = true;
        }
        try {
            writerExecutor.execute(this::drainPendingStates);
        } catch (Throwable ignored) {
            synchronized (stateLock) {
                writerScheduled = false;
            }
        }
    }

    private void drainPendingStates() {
        while (true) {
            PendingState state;
            synchronized (stateLock) {
                state = pendingState;
                pendingState = null;
                if (state == null) {
                    writerScheduled = false;
                    return;
                }
            }
            writeWithStockRetry(state);
        }
    }

    private static void writeWithStockRetry(PendingState state) {
        for (int attempt = 0; attempt < MAX_WRITE_ATTEMPTS; attempt++) {
            try {
                writeState(state);
                return;
            } catch (Exception ignored) {
                if (attempt + 1 >= MAX_WRITE_ATTEMPTS) {
                    return;
                }
                try {
                    Thread.sleep(WRITE_RETRY_DELAY_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static void writeState(PendingState state) throws IOException {
        // Match OdinSettings: select the stick by path and write without a newline.
        String enabledValue = state.enabled ? "1" : "0";
        writeNode(LEFT_ENABLE_NODE, enabledValue);
        writeNode(RIGHT_ENABLE_NODE, enabledValue);
        if (!state.enabled) {
            return;
        }
        writeNode(LEFT_BRIGHTNESS_NODE, state.colorPayload);
        writeNode(RIGHT_BRIGHTNESS_NODE, state.colorPayload);
    }

    private static void writeNode(String path, String value) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(value);
        }
    }

    private static final class PendingState {
        final boolean enabled;
        final String colorPayload;

        PendingState(boolean enabled, String colorPayload) {
            this.enabled = enabled;
            this.colorPayload = colorPayload;
        }
    }
}
