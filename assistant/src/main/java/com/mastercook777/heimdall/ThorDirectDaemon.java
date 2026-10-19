package com.mastercook777.heimdall;

import android.net.Credentials;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.system.Os;
import android.system.OsConstants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Privileged Thor-only helper, launched from the installed APK through stock PServer. */
public final class ThorDirectDaemon {
    static final int MAGIC = 0x48444c31; // HDL1
    static final int VERSION = 1;
    static final int OP_PING = 1;
    static final int OP_CAPTURE = 2;
    static final int OP_REPLAY = 3;
    static final int OP_STOP = 4;
    static final int OP_STICK = 5;
    static final int OP_TOUCH = 6;
    static final int OP_RELEASE_TOUCH = 7;
    static final int OP_OPEN_MOUSE = 8;
    static final int OP_EMIT_MOUSE = 9;
    static final int OP_RELEASE_MOUSE = 10;
    static final int OP_OPEN_KEYBOARD = 11;
    static final int OP_EMIT_KEYBOARD = 12;
    static final int OP_RELEASE_KEYBOARD_KEYS = 13;
    static final int OP_RELEASE_KEYBOARD = 14;
    private static final int MAX_REQUEST_BYTES = 64 * 1024;
    private static final int MAX_RESPONSE_BYTES = 256 * 1024;
    private static final long STICK_LEASE_MS = 700L;
    private static final long TOUCH_LEASE_MS = 1200L;
    private static final Object STICK_LOCK = new Object();
    private static final Object TOUCH_LOCK = new Object();
    private static final Object MOUSE_LOCK = new Object();
    private static final Object KEYBOARD_LOCK = new Object();
    private static final String TAG = "HeimdallThorDirect";
    private static boolean stickActive;
    private static String stickPath;
    private static int stickAxisX;
    private static int stickAxisY;
    private static long stickDeadlineMs;
    private static int stickNeutralRetries;
    private static boolean touchActive;
    private static long touchDeadlineMs;
    private static boolean mouseOpen;
    private static boolean mouseButtonDown;
    private static long mouseButtonDeadlineMs;
    private static boolean keyboardOpen;
    private static final boolean[] keyboardKeysDown = new boolean[256];
    private static int keyboardHeldCount;
    private static long keyboardDeadlineMs;
    private static volatile boolean watchdogRunning;
    private static volatile int clientPid;

    private ThorDirectDaemon() {
    }

    static String socketName(int appUid, String nonce) {
        return "heimdall_thor_direct_v1_" + appUid + "_" + nonce;
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || Process.myUid() != 0
                || Build.VERSION.SDK_INT < 33 || !"AYN Thor".equalsIgnoreCase(Build.MODEL)) {
            Log.e(TAG, "Refusing unprivileged or malformed start");
            return;
        }
        final int appUid;
        try {
            appUid = Integer.parseInt(args[0]);
        } catch (NumberFormatException error) {
            Log.e(TAG, "Invalid app UID");
            return;
        }
        if (appUid < 10000 || !args[1].matches("[0-9a-f]{32}")) {
            Log.e(TAG, "Refusing privileged client UID");
            return;
        }
        try (LocalServerSocket server = new LocalServerSocket(socketName(appUid, args[1]))) {
            Log.i(TAG, "Helper ready for app UID " + appUid);
            watchdogRunning = true;
            Thread watchdog = new Thread(() -> watchInputLeases(server, appUid),
                    "thor-direct-input-leases");
            watchdog.setDaemon(true);
            watchdog.start();
            boolean running = true;
            while (running && watchdogRunning) {
                try (LocalSocket socket = server.accept()) {
                    socket.setSoTimeout(8000);
                    Credentials peer = socket.getPeerCredentials();
                    if (peer == null || peer.getUid() != appUid) {
                        Log.w(TAG, "Rejected peer UID");
                        continue;
                    }
                    clientPid = peer.getPid();
                    running = handle(socket);
                } catch (IOException error) {
                    Log.w(TAG, "Client disconnected", error);
                } catch (Throwable error) {
                    Log.e(TAG, "Client request failed", error);
                }
            }
        } catch (IOException error) {
            Log.e(TAG, "Helper socket unavailable", error);
        } finally {
            watchdogRunning = false;
            releaseTouch();
            neutralStick();
            releaseMouse();
            releaseKeyboard();
        }
    }

    private static boolean handle(LocalSocket socket) throws IOException {
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        if (input.readInt() != MAGIC || input.readInt() != VERSION) {
            writeResponse(output, "protocol mismatch");
            return true;
        }
        int operation = input.readInt();
        String result;
        switch (operation) {
            case OP_PING:
                result = readiness();
                break;
            case OP_CAPTURE:
                int durationMs = input.readInt();
                result = capture(durationMs);
                break;
            case OP_REPLAY:
                result = replay(readBoundedString(input));
                break;
            case OP_STICK:
                result = emitStick(input.readFloat(), input.readFloat());
                break;
            case OP_TOUCH:
                result = emitTouch(input.readInt(), input.readInt(), input.readInt(),
                        input.readInt(), input.readInt(), input.readInt(), input.readInt());
                break;
            case OP_RELEASE_TOUCH:
                result = releaseTouch();
                break;
            case OP_OPEN_MOUSE:
                result = openMouse();
                break;
            case OP_EMIT_MOUSE:
                result = emitMouse(input.readInt(), input.readInt(), input.readInt(),
                        input.readInt(), input.readInt());
                break;
            case OP_RELEASE_MOUSE:
                result = releaseMouse();
                break;
            case OP_OPEN_KEYBOARD:
                result = openKeyboard();
                break;
            case OP_EMIT_KEYBOARD:
                result = emitKeyboard(input.readInt(), input.readInt());
                break;
            case OP_RELEASE_KEYBOARD_KEYS:
                result = releaseKeyboardKeys();
                break;
            case OP_RELEASE_KEYBOARD:
                result = releaseKeyboard();
                break;
            case OP_STOP:
                releaseTouch();
                neutralStick();
                releaseMouse();
                releaseKeyboard();
                writeResponse(output, "stopped");
                return false;
            default:
                result = "unsupported operation";
        }
        writeResponse(output, result);
        return true;
    }

    private static String readiness() {
        try {
            Class.forName(UinputNativeProbe.class.getName());
            NativeGamepadPath.Device device = NativeGamepadPath.requireDevice();
            if (!device.readable || !device.writable) {
                return "thor direct v1 controller node unavailable";
            }
            // Stat permissions alone do not prove SELinux permits actual device access.
            FileDescriptor read = Os.open(device.path, OsConstants.O_RDONLY, 0);
            Os.close(read);
            FileDescriptor write = Os.open(device.path, OsConstants.O_WRONLY, 0);
            Os.close(write);
            return "thor direct v1 ready uid=" + Process.myUid();
        } catch (Throwable error) {
            return "thor direct v1 unavailable: " + error.getClass().getSimpleName();
        }
    }

    private static String capture(int durationMs) {
        if (durationMs < 100 || durationMs > 5000) {
            return "invalid capture duration";
        }
        try {
            NativeGamepadPath.Device device = NativeGamepadPath.requireDevice();
            if (!device.readable) {
                return "Physical controller is not readable";
            }
            return NativeGamepadPath.userFacingError(
                    UinputNativeProbe.captureEvdevSequence(device.path, durationMs));
        } catch (Throwable error) {
            return "Physical controller recording failed: " + error.getClass().getSimpleName();
        }
    }

    private static String replay(String sequence) {
        if (sequence == null || sequence.length() == 0 || sequence.length() > MAX_REQUEST_BYTES) {
            return "invalid controller sequence";
        }
        GamepadSequencePolicy.Inspection inspection = GamepadSequencePolicy.inspect(sequence);
        if (inspection.hasUnreleasedSystemNavigationKey || inspection.exceedsReplayLimits()) {
            return "unsafe controller sequence";
        }
        try {
            NativeGamepadPath.Device device = NativeGamepadPath.requireDevice();
            if (!device.writable) {
                return "Physical controller is not writable";
            }
            if (sequence.startsWith("seq:") && !inspection.containsSystemNavigationKey()) {
                return ShizukuGamepadSequenceReplay.replay(sequence,
                        event -> NativeGamepadPath.userFacingError(
                                UinputNativeProbe.emitEvdevCombo(device.path, event, 90)));
            }
            return NativeGamepadPath.userFacingError(
                    UinputNativeProbe.emitEvdevCombo(device.path, sequence, 90));
        } catch (Throwable error) {
            return "Physical controller replay failed: " + error.getClass().getSimpleName();
        }
    }

    private static String emitStick(float x, float y) {
        if (Float.isNaN(x) || Float.isInfinite(x) || Float.isNaN(y)
                || Float.isInfinite(y) || Math.abs(x) > 1f || Math.abs(y) > 1f) {
            return "invalid right stick values";
        }
        synchronized (STICK_LOCK) {
            try {
                NativeGamepadPath.Device device = NativeGamepadPath.requireDevice();
                if (!device.writable) return "Physical controller is not writable";
                if (stickActive && !device.path.equals(stickPath)) {
                    neutralStickLocked();
                    if (stickActive) return "Previous controller right stick did not return to center";
                }
                boolean nonNeutral = x != 0f || y != 0f;
                stickPath = device.path;
                stickAxisX = device.rightStickAxisX;
                stickAxisY = device.rightStickAxisY;
                if (nonNeutral) {
                    // Even a partial native write must have a recovery lease.
                    stickActive = true;
                    stickDeadlineMs = SystemClock.elapsedRealtime() + STICK_LEASE_MS;
                }
                String result = NativeGamepadPath.userFacingError(
                        UinputNativeProbe.emitNativeRightStick(device.path, x, y,
                                device.rightStickAxisX, device.rightStickAxisY));
                if (NativeGamepadPath.operationSucceeded(result)) {
                    stickActive = nonNeutral;
                    stickNeutralRetries = 0;
                    stickDeadlineMs = stickActive
                            ? SystemClock.elapsedRealtime() + STICK_LEASE_MS : 0L;
                } else if (nonNeutral) {
                    neutralStickLocked();
                }
                return result;
            } catch (Throwable error) {
                return "Physical controller right stick failed: "
                        + error.getClass().getSimpleName();
            }
        }
    }

    private static String emitTouch(int displayId, int action, int x, int y,
            int width, int height, int rotation) {
        if (displayId != Display.DEFAULT_DISPLAY || width < 2 || height < 2
                || width > 8192 || height > 8192 || x < 0 || y < 0
                || x >= width || y >= height || rotation < 0 || rotation > 3
                || (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE
                        && action != MotionEvent.ACTION_UP
                        && action != MotionEvent.ACTION_CANCEL)) {
            return "invalid mapped touch frame";
        }
        synchronized (TOUCH_LOCK) {
            try {
                if (action == MotionEvent.ACTION_DOWN) {
                    String releaseResult = releaseTouchLocked();
                    if (!"ok".equals(releaseResult)) return releaseResult;
                    // Recover even if the native down write is only partially completed.
                    touchActive = true;
                    touchDeadlineMs = SystemClock.elapsedRealtime() + TOUCH_LEASE_MS;
                } else if (!touchActive) {
                    return "Thor mapped touch is not active";
                }
                String result = UinputNativeProbe.emitThorMappedTouch(
                        action, x, y, width, height, rotation);
                if ("ok".equals(result)) {
                    touchActive = action == MotionEvent.ACTION_DOWN
                            || action == MotionEvent.ACTION_MOVE;
                    touchDeadlineMs = touchActive
                            ? SystemClock.elapsedRealtime() + TOUCH_LEASE_MS : 0L;
                } else {
                    releaseTouchLocked();
                }
                return result;
            } catch (Throwable error) {
                releaseTouchLocked();
                return "Thor mapped touch failed: " + error.getClass().getSimpleName();
            }
        }
    }

    private static void watchInputLeases(LocalServerSocket server, int appUid) {
        while (watchdogRunning) {
            SystemClock.sleep(100L);
            int pid = clientPid;
            if (pid > 0) {
                try {
                    if (Os.stat("/proc/" + pid).st_uid != appUid) {
                        throw new IOException("client process identity changed");
                    }
                } catch (Throwable error) {
                    // A force-stopped app cannot send a final all-up or device close.
                    watchdogRunning = false;
                    releaseTouch();
                    neutralStick();
                    releaseMouse();
                    releaseKeyboard();
                    try {
                        server.close();
                    } catch (IOException ignored) {
                    }
                    return;
                }
            }
            synchronized (STICK_LOCK) {
                if (stickActive && SystemClock.elapsedRealtime() >= stickDeadlineMs) {
                    neutralStickLocked();
                }
            }
            synchronized (TOUCH_LOCK) {
                if (touchActive && SystemClock.elapsedRealtime() >= touchDeadlineMs) {
                    releaseTouchLocked();
                }
            }
            synchronized (MOUSE_LOCK) {
                if (mouseButtonDown
                        && SystemClock.elapsedRealtime() >= mouseButtonDeadlineMs) {
                    releaseMouseLocked();
                }
            }
            synchronized (KEYBOARD_LOCK) {
                if (keyboardHeldCount > 0
                        && SystemClock.elapsedRealtime() >= keyboardDeadlineMs) {
                    releaseKeyboardKeysLocked();
                }
            }
        }
    }

    private static String releaseTouch() {
        synchronized (TOUCH_LOCK) {
            return releaseTouchLocked();
        }
    }

    private static String releaseTouchLocked() {
        try {
            UinputNativeProbe.releaseThorMappedTouch();
            touchActive = false;
            touchDeadlineMs = 0L;
            return "ok";
        } catch (Throwable error) {
            Log.w(TAG, "Thor mapped touch release failed", error);
            touchDeadlineMs = SystemClock.elapsedRealtime() + 100L;
            return "Thor mapped touch release failed";
        }
    }

    private static String openMouse() {
        synchronized (MOUSE_LOCK) {
            try {
                String result = UinputNativeProbe.openVirtualMouse();
                mouseOpen = result != null && result.startsWith("ok");
                return result;
            } catch (Throwable error) {
                releaseMouseLocked();
                return "Thor direct virtual mouse open failed";
            }
        }
    }

    private static String emitMouse(int dx, int dy, int wheel,
            int button, int buttonValue) {
        if ((button != 0 && button != 272 && button != 273 && button != 274)
                || (buttonValue != 0 && buttonValue != 1)
                || (button == 0 && buttonValue != 0)) {
            return "invalid virtual mouse frame";
        }
        synchronized (MOUSE_LOCK) {
            if (!mouseOpen) return "Thor direct virtual mouse is not open";
            try {
                String result = UinputNativeProbe.emitVirtualMouseFrame(
                        Math.max(-2048, Math.min(dx, 2048)),
                        Math.max(-2048, Math.min(dy, 2048)),
                        Math.max(-32, Math.min(wheel, 32)), button, buttonValue);
                if (result == null || !result.startsWith("ok")) {
                    releaseMouseLocked();
                    return result;
                }
                if (button != 0) mouseButtonDown = buttonValue == 1;
                if (mouseButtonDown) {
                    mouseButtonDeadlineMs = SystemClock.elapsedRealtime() + 1200L;
                }
                return result;
            } catch (Throwable error) {
                releaseMouseLocked();
                return "Thor direct virtual mouse input failed";
            }
        }
    }

    private static String releaseMouse() {
        synchronized (MOUSE_LOCK) {
            return releaseMouseLocked();
        }
    }

    private static String releaseMouseLocked() {
        if (!mouseOpen) return "ok: virtual mouse closed";
        try {
            String result = UinputNativeProbe.releaseVirtualMouse();
            if (result != null && result.startsWith("ok")) {
                mouseOpen = false;
                mouseButtonDown = false;
                mouseButtonDeadlineMs = 0L;
            }
            return result;
        } catch (Throwable error) {
            return "Thor direct virtual mouse release failed";
        }
    }

    private static String openKeyboard() {
        synchronized (KEYBOARD_LOCK) {
            try {
                String result = UinputNativeProbe.openVirtualKeyboard();
                keyboardOpen = result != null && result.startsWith("ok");
                return result;
            } catch (Throwable error) {
                releaseKeyboardLocked();
                return "Thor direct virtual keyboard open failed";
            }
        }
    }

    private static String emitKeyboard(int keyCode, int keyValue) {
        if ((keyCode == 0 && keyValue != 0)
                || (keyCode != 0 && (keyCode < 1 || keyCode > 255
                || (keyValue != 0 && keyValue != 1)))) {
            return "invalid virtual keyboard frame";
        }
        synchronized (KEYBOARD_LOCK) {
            if (!keyboardOpen) return "Thor direct virtual keyboard is not open";
            if (keyCode == 0) {
                if (keyboardHeldCount == 0) return "virtual keyboard lease expired";
                keyboardDeadlineMs = SystemClock.elapsedRealtime() + 1200L;
                return "ok: virtual keyboard heartbeat";
            }
            try {
                String result = UinputNativeProbe.emitVirtualKeyboardKey(keyCode, keyValue);
                if (result == null || !result.startsWith("ok")) {
                    releaseKeyboardLocked();
                    return result;
                }
                boolean pressed = keyValue == 1;
                if (keyboardKeysDown[keyCode] != pressed) {
                    keyboardKeysDown[keyCode] = pressed;
                    keyboardHeldCount += pressed ? 1 : -1;
                }
                keyboardDeadlineMs = keyboardHeldCount > 0
                        ? SystemClock.elapsedRealtime() + 1200L : 0L;
                return result;
            } catch (Throwable error) {
                releaseKeyboardLocked();
                return "Thor direct virtual keyboard input failed";
            }
        }
    }

    private static String releaseKeyboardKeys() {
        synchronized (KEYBOARD_LOCK) {
            return releaseKeyboardKeysLocked();
        }
    }

    private static String releaseKeyboardKeysLocked() {
        if (!keyboardOpen) return "ok: virtual keyboard closed";
        try {
            String result = UinputNativeProbe.releaseVirtualKeyboardKeys();
            if (result == null || !result.startsWith("ok")) {
                releaseKeyboardLocked();
                return result;
            }
            Arrays.fill(keyboardKeysDown, false);
            keyboardHeldCount = 0;
            keyboardDeadlineMs = 0L;
            return result;
        } catch (Throwable error) {
            releaseKeyboardLocked();
            return "Thor direct virtual keyboard all-up failed";
        }
    }

    private static String releaseKeyboard() {
        synchronized (KEYBOARD_LOCK) {
            return releaseKeyboardLocked();
        }
    }

    private static String releaseKeyboardLocked() {
        if (!keyboardOpen) return "ok: virtual keyboard closed";
        try {
            String result = UinputNativeProbe.releaseVirtualKeyboard();
            if (result != null && result.startsWith("ok")) {
                keyboardOpen = false;
                Arrays.fill(keyboardKeysDown, false);
                keyboardHeldCount = 0;
                keyboardDeadlineMs = 0L;
            }
            return result;
        } catch (Throwable error) {
            keyboardDeadlineMs = SystemClock.elapsedRealtime() + 100L;
            return "Thor direct virtual keyboard release failed";
        }
    }

    private static void neutralStick() {
        synchronized (STICK_LOCK) {
            neutralStickLocked();
        }
    }

    private static void neutralStickLocked() {
        if (!stickActive || stickPath == null) return;
        boolean success = false;
        try {
            String result = UinputNativeProbe.emitNativeRightStick(
                    stickPath, 0f, 0f, stickAxisX, stickAxisY);
            success = NativeGamepadPath.operationSucceeded(result);
            if (!success) Log.w(TAG, "Right stick neutral failed: " + result);
        } catch (Throwable error) {
            Log.w(TAG, "Right stick neutral failed", error);
        }
        if (success || ++stickNeutralRetries >= 3) {
            stickActive = false;
            stickDeadlineMs = 0L;
            stickNeutralRetries = 0;
        } else {
            stickDeadlineMs = SystemClock.elapsedRealtime() + 100L;
        }
    }

    private static String readBoundedString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_REQUEST_BYTES) {
            throw new IOException("invalid request length");
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeResponse(DataOutputStream output, String response) throws IOException {
        byte[] bytes = (response == null ? "" : response).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_RESPONSE_BYTES) {
            throw new IOException("response too large");
        }
        output.writeInt(MAGIC);
        output.writeInt(VERSION);
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }
}
