package com.mastercook777.heimdall;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Parcel;
import android.os.Process;
import android.os.SystemClock;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Transport for the independent, read-only Thor Game Context process. */
final class ThorGameContextController {
    private static final int MAX_REPLY_BYTES = 64 * 1024;
    private static boolean ready;
    private static boolean launched;

    private ThorGameContextController() {
    }

    static synchronized Parcel query(Context context, String packageName) throws IOException {
        if (!ShizukuGameContextController.isSupportedPackage(packageName)) {
            throw new IOException("unsupported Game Context package");
        }
        ensureReady(context);
        try {
            byte[] data = call(context, ThorGcDaemon.OP_QUERY, packageName);
            if (data.length == 0) throw new IOException("empty Game Context reply");
            Parcel reply = Parcel.obtain();
            reply.unmarshall(data, 0, data.length);
            reply.setDataPosition(0);
            return reply;
        } catch (IOException error) {
            ready = false;
            throw error;
        }
    }

    static synchronized void pause(Context context) throws IOException {
        if (!ready) return;
        try {
            call(context, ThorGcDaemon.OP_PAUSE, null);
        } catch (IOException error) {
            ready = false;
            throw error;
        }
    }

    static synchronized boolean stop(Context context) {
        if (!launched) return true;
        try {
            call(context, ThorGcDaemon.OP_STOP, null);
        } catch (IOException ignored) {
            // A broken or already exited socket is also a confirmed stop.
        }
        ready = false;
        long deadline = SystemClock.elapsedRealtime() + 1200L;
        while (ping(context) && SystemClock.elapsedRealtime() < deadline) {
            SystemClock.sleep(50L);
        }
        if (!ping(context)) {
            launched = false;
            return true;
        }
        return false;
    }

    private static void ensureReady(Context context) throws IOException {
        if (!ThorDirectController.isReady(context)) {
            throw new IOException("Thor Direct unavailable");
        }
        if (ready && ping(context)) return;
        ready = false;
        // A prior app process may still be retiring after an APK replacement.
        if (ping(context)) {
            ready = true;
            launched = true;
            return;
        }
        try {
            launched = true;
            ThorDirectController.launchGameContextThroughPServer(context);
        } catch (Exception error) {
            throw new IOException("Game Context launch failed", error);
        }
        long deadline = SystemClock.elapsedRealtime() + 6000L;
        while (SystemClock.elapsedRealtime() < deadline) {
            if (ping(context)) {
                ready = true;
                return;
            }
            SystemClock.sleep(120L);
        }
        throw new IOException("Game Context helper not ready");
    }

    private static boolean ping(Context context) {
        try {
            call(context, ThorGcDaemon.OP_PING, null);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static byte[] call(Context context, int operation, String packageName)
            throws IOException {
        String socketName = ThorGcDaemon.socketName(Process.myUid(),
                ThorDirectController.gameContextSocketNonce(context));
        try (LocalSocket socket = new LocalSocket()) {
            socket.connect(new LocalSocketAddress(socketName,
                    LocalSocketAddress.Namespace.ABSTRACT));
            socket.setSoTimeout(8000);
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            output.writeInt(ThorGcDaemon.MAGIC);
            output.writeInt(ThorGcDaemon.VERSION);
            output.writeInt(operation);
            if (operation == ThorGcDaemon.OP_QUERY) {
                byte[] bytes = packageName.getBytes(StandardCharsets.UTF_8);
                if (bytes.length == 0 || bytes.length > 256) {
                    throw new IOException("invalid Game Context package");
                }
                output.writeInt(bytes.length);
                output.write(bytes);
            }
            output.flush();
            DataInputStream input = new DataInputStream(socket.getInputStream());
            if (input.readInt() != ThorGcDaemon.MAGIC
                    || input.readInt() != ThorGcDaemon.VERSION
                    || input.readInt() != 1) {
                throw new IOException("Game Context helper rejected request");
            }
            int length = input.readInt();
            if (length < 0 || length > MAX_REPLY_BYTES) {
                throw new IOException("invalid Game Context reply length");
            }
            byte[] bytes = new byte[length];
            input.readFully(bytes);
            return bytes;
        }
    }
}
