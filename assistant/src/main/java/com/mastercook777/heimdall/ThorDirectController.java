package com.mastercook777.heimdall;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Explicit, Thor-only connection to Heimdall's constrained controller helper. */
final class ThorDirectController {
    enum State { UNSUPPORTED, DISABLED, STARTING, READY, UNAVAILABLE }

    private static final String TAG = "HeimdallThorDirect";
    private static final String PREFS = "thor_direct_controls";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_NONCE = "socket_nonce";
    private static final long START_TIMEOUT_MS = 6000;
    private static final int MAX_MESSAGE_BYTES = 256 * 1024;
    private static final ExecutorService START_EXECUTOR =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "thor-direct-start");
                thread.setDaemon(true);
                return thread;
            });
    private static final CopyOnWriteArraySet<Runnable> STATE_LISTENERS =
            new CopyOnWriteArraySet<>();
    private static final Object LOCK = new Object();
    private static volatile State state = State.DISABLED;
    private static volatile boolean helperStopConfirmed;
    private static int generation;

    private ThorDirectController() {
    }

    static boolean isSupported() {
        return Build.VERSION.SDK_INT >= 33 && "AYN Thor".equalsIgnoreCase(Build.MODEL);
    }

    static boolean isEnabled(Context context) {
        return context != null && isSupported() && preferences(context).getBoolean(KEY_ENABLED, false);
    }

    static boolean isReady(Context context) {
        return isEnabled(context) && state == State.READY;
    }

    /** Call from a worker thread; the helper validates the physical display pair. */
    static String readLinkedBrightness(Context context) {
        if (!isReady(context)) return "unavailable:Thor Direct";
        try {
            return call(context, ThorDirectDaemon.OP_READ_LINKED_BRIGHTNESS, 0, null);
        } catch (IOException error) {
            connectionLost();
            return "unavailable:connection";
        }
    }

    /** Preview is temporary; commit saves both display values on touch release. */
    static String writeLinkedBrightness(Context context, float target, boolean commit) {
        if (!isReady(context)) return "unavailable:Thor Direct";
        try {
            return call(context, commit ? ThorDirectDaemon.OP_COMMIT_LINKED_BRIGHTNESS
                    : ThorDirectDaemon.OP_PREVIEW_LINKED_BRIGHTNESS, 0, null,
                    target, 0f);
        } catch (IOException error) {
            connectionLost();
            return "unavailable:connection";
        }
    }

    static boolean helperStopConfirmed() {
        return helperStopConfirmed;
    }

    static State state(Context context) {
        if (!isSupported()) return State.UNSUPPORTED;
        if (!isEnabled(context)) return State.DISABLED;
        return state;
    }

    static void addStateListener(Runnable listener) {
        if (listener != null) STATE_LISTENERS.add(listener);
    }

    static void removeStateListener(Runnable listener) {
        if (listener != null) STATE_LISTENERS.remove(listener);
    }

    static void autoStart(Context context) {
        if (isEnabled(context) && state != State.READY) requestStart(context);
    }

    static void enable(Context context) {
        if (context == null || !isSupported()) return;
        helperStopConfirmed = false;
        if (!preferences(context).edit().putBoolean(KEY_ENABLED, true).commit()) {
            Log.w(TAG, "Could not save Thor Direct opt-in");
            return;
        }
        requestStart(context);
    }

    static void disable(Context context) {
        if (context == null) return;
        helperStopConfirmed = false;
        Context appContext = context.getApplicationContext();
        if (!preferences(context).edit().putBoolean(KEY_ENABLED, false).commit()) {
            Log.w(TAG, "Could not save Thor Direct opt-out");
        }
        int stopGeneration;
        synchronized (LOCK) {
            stopGeneration = ++generation;
            state = State.DISABLED;
        }
        notifyStateChanged();
        START_EXECUTOR.execute(() -> {
            synchronized (LOCK) {
                if (generation != stopGeneration) return;
            }
            try {
                helperStopConfirmed = "stopped".equals(
                        call(appContext, ThorDirectDaemon.OP_STOP, 0, null));
            } catch (IOException ignored) {
            }
        });
    }

    static void requestStart(Context context) {
        if (context == null || !isEnabled(context)) return;
        helperStopConfirmed = false;
        Context appContext = context.getApplicationContext();
        int requestGeneration;
        synchronized (LOCK) {
            if (state == State.STARTING) return;
            state = State.STARTING;
            requestGeneration = ++generation;
        }
        notifyStateChanged();
        START_EXECUTOR.execute(() -> start(appContext, requestGeneration));
    }

    static String captureSequence(Context context, int durationMs) {
        if (!isReady(context)) return "Thor direct controller is not ready";
        try {
            return call(context, ThorDirectDaemon.OP_CAPTURE, durationMs, null);
        } catch (IOException error) {
            connectionLost();
            return "Thor direct controller recording failed: " + error.getClass().getSimpleName();
        }
    }

    static String replaySequence(Context context, String sequence) {
        if (!isReady(context)) return "Thor direct controller is not ready";
        try {
            return call(context, ThorDirectDaemon.OP_REPLAY, 0, sequence);
        } catch (IOException error) {
            connectionLost();
            return "Thor direct controller replay failed: " + error.getClass().getSimpleName();
        }
    }

    static String emitRightStick(Context context, float x, float y) {
        boolean neutral = x == 0f && y == 0f;
        if (!isEnabled(context) || (!neutral && !isReady(context))) {
            return "Thor direct controller is not ready";
        }
        try {
            return call(context, ThorDirectDaemon.OP_STICK, 0, null, x, y);
        } catch (IOException error) {
            connectionLost();
            return "Thor direct right stick failed: " + error.getClass().getSimpleName();
        }
    }

    static String emitMappedTouch(Context context, int displayId, int action,
            int x, int y, int width, int height, int rotation) {
        boolean release = action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL;
        if (!isSupported() || (!release && !isReady(context))) {
            return "Thor direct mapped touch is not ready";
        }
        try {
            return call(context, ThorDirectDaemon.OP_TOUCH, 0, null, 0f, 0f,
                    new int[] {displayId, action, x, y, width, height, rotation});
        } catch (IOException error) {
            connectionLost();
            return "Thor direct mapped touch failed: " + error.getClass().getSimpleName();
        }
    }

    static String releaseMappedTouch(Context context) {
        if (!isSupported()) return "Thor direct mapped touch is not supported";
        try {
            return call(context, ThorDirectDaemon.OP_RELEASE_TOUCH, 0, null);
        } catch (IOException error) {
            connectionLost();
            return "Thor direct mapped touch release failed: "
                    + error.getClass().getSimpleName();
        }
    }

    static String openVirtualMouse(Context context) {
        if (!isReady(context)) return "Thor direct virtual mouse is not ready";
        try {
            return call(context, ThorDirectDaemon.OP_OPEN_MOUSE, 0, null);
        } catch (IOException error) {
            connectionLost();
            return "Thor direct virtual mouse open failed";
        }
    }

    static String emitVirtualMouse(Context context, int dx, int dy, int wheel,
            int button, int buttonValue) {
        if (!isReady(context)) return "Thor direct virtual mouse is not ready";
        try {
            return call(context, ThorDirectDaemon.OP_EMIT_MOUSE, 0, null,
                    0f, 0f, new int[] {dx, dy, wheel, button, buttonValue});
        } catch (IOException error) {
            connectionLost();
            return "Thor direct virtual mouse input failed";
        }
    }

    static String releaseVirtualMouse(Context context) {
        if (!isSupported()) return "Thor direct virtual mouse is not supported";
        try {
            return call(context, ThorDirectDaemon.OP_RELEASE_MOUSE, 0, null);
        } catch (IOException error) {
            connectionLost();
            return "Thor direct virtual mouse release failed";
        }
    }

    static String openVirtualKeyboard(Context context) {
        if (!isReady(context)) return "Thor direct virtual keyboard is not ready";
        try {
            return call(context, ThorDirectDaemon.OP_OPEN_KEYBOARD, 0, null);
        } catch (IOException error) {
            connectionLost();
            return "Thor direct virtual keyboard open failed";
        }
    }

    static String emitVirtualKeyboard(Context context, int keyCode, int keyValue) {
        if (!isReady(context)) return "Thor direct virtual keyboard is not ready";
        try {
            return call(context, ThorDirectDaemon.OP_EMIT_KEYBOARD, 0, null,
                    0f, 0f, new int[] {keyCode, keyValue});
        } catch (IOException error) {
            connectionLost();
            return "Thor direct virtual keyboard input failed";
        }
    }

    static String releaseVirtualKeyboardKeys(Context context) {
        if (!isSupported()) return "Thor direct virtual keyboard is not supported";
        try {
            return call(context, ThorDirectDaemon.OP_RELEASE_KEYBOARD_KEYS, 0, null);
        } catch (IOException error) {
            connectionLost();
            return "Thor direct virtual keyboard all-up failed";
        }
    }

    static String releaseVirtualKeyboard(Context context) {
        if (!isSupported()) return "Thor direct virtual keyboard is not supported";
        try {
            return call(context, ThorDirectDaemon.OP_RELEASE_KEYBOARD, 0, null);
        } catch (IOException error) {
            connectionLost();
            return "Thor direct virtual keyboard release failed";
        }
    }

    private static void start(Context context, int requestGeneration) {
        try {
            if (!isCurrent(context, requestGeneration)) return;
            String initialResponse = pingResponse(context);
            // A new app process may follow an APK update; never reuse code loaded
            // into a helper by a previous installation of the package.
            if (initialResponse != null) {
                Log.i(TAG, "Replacing existing helper");
                call(context, ThorDirectDaemon.OP_STOP, 0, null);
                long stopDeadline = SystemClock.elapsedRealtime() + 1200;
                while (pingResponse(context) != null
                        && SystemClock.elapsedRealtime() < stopDeadline) {
                    SystemClock.sleep(50);
                }
                if (pingResponse(context) != null) {
                    throw new IOException("previous helper did not stop");
                }
            }
            if (!isCurrent(context, requestGeneration)) return;
            launchThroughPServer(context);
            long deadline = SystemClock.elapsedRealtime() + START_TIMEOUT_MS;
            while (SystemClock.elapsedRealtime() < deadline) {
                if (!isCurrent(context, requestGeneration)) return;
                if (isReadyResponse(pingResponse(context))) break;
                SystemClock.sleep(120);
            }
            String finalResponse = pingResponse(context);
            if (!isReadyResponse(finalResponse)) {
                Log.w(TAG, "Helper not ready: " + finalResponse);
            }
            boolean ready = isReadyResponse(finalResponse);
            setStateIfCurrent(context, requestGeneration,
                    ready ? State.READY : State.UNAVAILABLE);
            if (ready && BuildConfig.DEBUG) {
                try {
                    Log.i(TAG, "Read-only brightness probe: "
                            + call(context, ThorDirectDaemon.OP_PROBE_DISPLAY_BRIGHTNESS, 0, null));
                } catch (IOException error) {
                    Log.w(TAG, "Read-only brightness probe unavailable", error);
                }
            }
        } catch (Throwable error) {
            Log.w(TAG, "Helper start failed", error);
            setStateIfCurrent(context, requestGeneration, State.UNAVAILABLE);
        }
    }

    private static boolean isCurrent(Context context, int requestGeneration) {
        synchronized (LOCK) {
            return generation == requestGeneration && isEnabled(context);
        }
    }

    private static void setStateIfCurrent(Context context, int requestGeneration, State next) {
        synchronized (LOCK) {
            if (generation != requestGeneration || !isEnabled(context)) return;
            state = next;
        }
        notifyStateChanged();
    }

    private static void connectionLost() {
        synchronized (LOCK) {
            if (state != State.READY) return;
            state = State.UNAVAILABLE;
        }
        notifyStateChanged();
    }

    static void invalidate() {
        connectionLost();
    }

    private static void notifyStateChanged() {
        for (Runnable listener : STATE_LISTENERS) {
            AssistantMainHandler.post(listener);
        }
    }

    private static boolean isReadyResponse(String response) {
        return "thor direct v3 ready uid=0".equals(response);
    }

    private static String pingResponse(Context context) {
        try {
            return call(context, ThorDirectDaemon.OP_PING, 0, null);
        } catch (IOException error) {
            return null;
        }
    }

    private static String call(Context context, int operation, int value, String sequence)
            throws IOException {
        return call(context, operation, value, sequence, 0f, 0f);
    }

    private static String call(Context context, int operation, int value, String sequence,
            float stickX, float stickY) throws IOException {
        return call(context, operation, value, sequence, stickX, stickY, null);
    }

    private static String call(Context context, int operation, int value, String sequence,
            float stickX, float stickY, int[] intFrame) throws IOException {
        String socketName = ThorDirectDaemon.socketName(Process.myUid(), socketNonce(context));
        try (LocalSocket socket = new LocalSocket()) {
            socket.connect(new LocalSocketAddress(
                    socketName, LocalSocketAddress.Namespace.ABSTRACT));
            socket.setSoTimeout(operation == ThorDirectDaemon.OP_REPLAY ? 16000
                    : operation == ThorDirectDaemon.OP_STICK
                            || operation == ThorDirectDaemon.OP_TOUCH
                            || operation == ThorDirectDaemon.OP_RELEASE_TOUCH
                            || operation == ThorDirectDaemon.OP_EMIT_MOUSE
                            || operation == ThorDirectDaemon.OP_RELEASE_MOUSE
                            || operation == ThorDirectDaemon.OP_EMIT_KEYBOARD
                            || operation == ThorDirectDaemon.OP_RELEASE_KEYBOARD_KEYS
                            || operation == ThorDirectDaemon.OP_RELEASE_KEYBOARD ? 750 : 8000);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            output.writeInt(ThorDirectDaemon.MAGIC);
            output.writeInt(ThorDirectDaemon.VERSION);
            output.writeInt(operation);
            if (operation == ThorDirectDaemon.OP_CAPTURE) {
                output.writeInt(value);
            } else if (operation == ThorDirectDaemon.OP_REPLAY) {
                byte[] bytes = (sequence == null ? "" : sequence)
                        .getBytes(StandardCharsets.UTF_8);
                if (bytes.length > 64 * 1024) throw new IOException("sequence too large");
                output.writeInt(bytes.length);
                output.write(bytes);
            } else if (operation == ThorDirectDaemon.OP_STICK
                    || operation == ThorDirectDaemon.OP_PREVIEW_LINKED_BRIGHTNESS
                    || operation == ThorDirectDaemon.OP_COMMIT_LINKED_BRIGHTNESS) {
                output.writeFloat(stickX);
                if (operation == ThorDirectDaemon.OP_STICK) output.writeFloat(stickY);
            } else if (operation == ThorDirectDaemon.OP_TOUCH) {
                if (intFrame == null || intFrame.length != 7) {
                    throw new IOException("invalid touch frame");
                }
                for (int field : intFrame) output.writeInt(field);
            } else if (operation == ThorDirectDaemon.OP_EMIT_MOUSE) {
                if (intFrame == null || intFrame.length != 5) {
                    throw new IOException("invalid virtual mouse frame");
                }
                for (int field : intFrame) output.writeInt(field);
            } else if (operation == ThorDirectDaemon.OP_EMIT_KEYBOARD) {
                if (intFrame == null || intFrame.length != 2) {
                    throw new IOException("invalid virtual keyboard frame");
                }
                for (int field : intFrame) output.writeInt(field);
            }
            output.flush();
            DataInputStream input = new DataInputStream(socket.getInputStream());
            if (input.readInt() != ThorDirectDaemon.MAGIC
                    || input.readInt() != ThorDirectDaemon.VERSION) {
                throw new IOException("helper protocol mismatch");
            }
            int length = input.readInt();
            if (length < 0 || length > MAX_MESSAGE_BYTES) {
                throw new IOException("invalid helper response length");
            }
            byte[] bytes = new byte[length];
            input.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static void launchThroughPServer(Context context) throws Exception {
        launchThroughPServer(context, ThorDirectDaemon.class.getName(), "");
    }

    static void launchGameContextThroughPServer(Context context) throws Exception {
        launchThroughPServer(context, ThorGcDaemon.class.getName(),
                " " + Process.myPid());
    }

    static String gameContextSocketNonce(Context context) {
        return socketNonce(context);
    }

    private static void launchThroughPServer(Context context, String className,
            String extraArguments) throws Exception {
        String apk = safeShellAtom(context.getApplicationInfo().sourceDir);
        String nonce = socketNonce(context);
        String command = "CLASSPATH=" + apk + " app_process /system/bin "
                + className + " " + Process.myUid() + " "
                + nonce + extraArguments + " >/dev/null 2>&1 &";
        // Thor's PServer silently discards longer commands (211 bytes worked,
        // 311 bytes did not on the owner's firmware). Readiness is checked by
        // the helper socket after this request, not by shell output.
        if (command.length() >= 250) {
            throw new IOException("PServer launch command exceeds Thor limit");
        }
        Class<?> manager = Class.forName("android.os.ServiceManager");
        Method getService = manager.getDeclaredMethod("getService", String.class);
        IBinder binder = (IBinder) getService.invoke(null, "PServerBinder");
        if (binder == null || !binder.isBinderAlive()) {
            throw new IOException("PServerBinder unavailable");
        }
        Parcel request = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            request.writeStringArray(new String[] {command, "1"});
            if (!binder.transact(0, request, reply, 0)) {
                throw new IOException("PServer launch not delivered");
            }
            // Backgrounded launch normally returns an empty byte array.
            // The subsequent authenticated PING is the success gate.
        } finally {
            request.recycle();
            reply.recycle();
        }
    }

    private static String safeShellAtom(String path) throws IOException {
        if (path == null || !path.matches("[A-Za-z0-9_./:=+~\\-]+")) {
            throw new IOException("unsafe installed app path");
        }
        return path;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static synchronized String socketNonce(Context context) {
        SharedPreferences prefs = preferences(context);
        String existing = prefs.getString(KEY_NONCE, null);
        if (existing != null && existing.matches("[0-9a-f]{32}")) return existing;
        byte[] random = new byte[16];
        new SecureRandom().nextBytes(random);
        char[] hex = new char[random.length * 2];
        final char[] alphabet = "0123456789abcdef".toCharArray();
        for (int i = 0; i < random.length; i++) {
            hex[i * 2] = alphabet[(random[i] >>> 4) & 15];
            hex[i * 2 + 1] = alphabet[random[i] & 15];
        }
        String nonce = new String(hex);
        prefs.edit().putString(KEY_NONCE, nonce).commit();
        return nonce;
    }
}
