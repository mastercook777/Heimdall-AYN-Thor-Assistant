package com.mastercook777.heimdall;

import android.net.Credentials;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Build;
import android.os.Parcel;
import android.os.Process;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Separate read-only Thor process for the existing allowlisted Game Context monitor. */
public final class ThorGcDaemon {
    static final int MAGIC = 0x48474331; // HGC1
    static final int VERSION = 1;
    static final int OP_PING = 1;
    static final int OP_QUERY = 2;
    static final int OP_PAUSE = 3;
    static final int OP_STOP = 4;
    private static final int MAX_PACKAGE_BYTES = 256;
    private static final int MAX_REPLY_BYTES = 64 * 1024;
    private static final String TAG = "HeimdallGameContext";

    private ThorGcDaemon() {
    }

    static String socketName(int appUid, String nonce) {
        return "heimdall_game_context_v1_" + appUid + "_" + nonce;
    }

    public static void main(String[] args) {
        if (args == null || args.length != 3 || Process.myUid() != 0
                || Build.VERSION.SDK_INT < 33
                || !"AYN Thor".equalsIgnoreCase(Build.MODEL)) return;
        final int appUid;
        final int appPid;
        try {
            appUid = Integer.parseInt(args[0]);
            appPid = Integer.parseInt(args[2]);
        } catch (NumberFormatException error) {
            return;
        }
        if (appUid < 10000 || appPid <= 0 || !args[1].matches("[0-9a-f]{32}")) return;
        ShizukuGameContextUserService monitor = new ShizukuGameContextUserService();
        try (LocalServerSocket server = new LocalServerSocket(socketName(appUid, args[1]))) {
            Thread clientWatcher = new Thread(() -> {
                while (new File("/proc/" + appPid).exists()) {
                    try { Thread.sleep(1000L); }
                    catch (InterruptedException ignored) { break; }
                }
                try { server.close(); } catch (IOException ignored) { }
            }, "thor-game-context-client-watch");
            clientWatcher.setDaemon(true);
            clientWatcher.start();
            boolean running = true;
            while (running) {
                try (LocalSocket socket = server.accept()) {
                    socket.setSoTimeout(8000);
                    Credentials peer = socket.getPeerCredentials();
                    if (peer == null || peer.getUid() != appUid
                            || peer.getPid() != appPid) continue;
                    running = handle(socket, monitor);
                } catch (IOException error) {
                    if (!new File("/proc/" + appPid).exists()) break;
                    Log.w(TAG, "Game Context client disconnected", error);
                } catch (Throwable error) {
                    Log.e(TAG, "Game Context request failed", error);
                }
            }
        } catch (IOException error) {
            Log.w(TAG, "Game Context socket unavailable", error);
        } finally {
            monitor.destroy();
        }
    }

    private static boolean handle(LocalSocket socket,
            ShizukuGameContextUserService monitor) throws IOException {
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        if (input.readInt() != MAGIC || input.readInt() != VERSION) {
            writeReply(output, false, new byte[0]);
            return true;
        }
        int operation = input.readInt();
        if (operation == OP_PING) {
            writeReply(output, true, new byte[0]);
            return true;
        }
        if (operation == OP_STOP) {
            writeReply(output, true, new byte[0]);
            return false;
        }
        Parcel request = Parcel.obtain();
        Parcel response = Parcel.obtain();
        try {
            request.writeInterfaceToken(ShizukuGameContextUserService.DESCRIPTOR);
            int transaction;
            if (operation == OP_PAUSE) {
                request.writeInt(1);
                transaction = ShizukuGameContextUserService.TRANSACTION_SET_MONITOR_PAUSED;
            } else if (operation == OP_QUERY) {
                int length = input.readInt();
                if (length <= 0 || length > MAX_PACKAGE_BYTES) {
                    writeReply(output, false, new byte[0]);
                    return true;
                }
                byte[] bytes = new byte[length];
                input.readFully(bytes);
                String packageName = new String(bytes, StandardCharsets.UTF_8);
                if (!ShizukuGameContextController.isSupportedPackage(packageName)) {
                    writeReply(output, false, new byte[0]);
                    return true;
                }
                request.writeString(packageName);
                transaction = ShizukuGameContextUserService.TRANSACTION_QUERY_CONTEXT;
            } else {
                writeReply(output, false, new byte[0]);
                return true;
            }
            if (!monitor.transact(transaction, request, response, 0)) {
                writeReply(output, false, new byte[0]);
                return true;
            }
            byte[] data = response.marshall();
            writeReply(output, data.length <= MAX_REPLY_BYTES, data);
            return true;
        } catch (Throwable error) {
            Log.w(TAG, "Read-only transaction failed", error);
            writeReply(output, false, new byte[0]);
            return true;
        } finally {
            request.recycle();
            response.recycle();
        }
    }

    private static void writeReply(DataOutputStream output, boolean success,
            byte[] data) throws IOException {
        output.writeInt(MAGIC);
        output.writeInt(VERSION);
        output.writeInt(success ? 1 : 0);
        output.writeInt(success ? data.length : 0);
        if (success) output.write(data);
        output.flush();
    }
}
