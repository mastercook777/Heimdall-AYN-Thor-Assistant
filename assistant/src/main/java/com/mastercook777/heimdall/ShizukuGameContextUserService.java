package com.mastercook777.heimdall;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Separate read-only Shizuku service for fixed, allowlisted emulator detectors. */
public final class ShizukuGameContextUserService extends Binder {
    static final String DESCRIPTOR = BuildConfig.APPLICATION_ID + ".IGameContext";
    static final int TRANSACTION_QUERY_AETHER = IBinder.FIRST_CALL_TRANSACTION;
    static final int TRANSACTION_CLEAR_AETHER = IBinder.FIRST_CALL_TRANSACTION + 1;

    private final Object lock = new Object();
    private volatile boolean destroyed;
    private Process logcatProcess;
    private int monitoredPid = -1;
    private String activeUri = "";
    private long observedAt;
    private long minimumAcceptedAt;
    private final Thread monitorThread;

    public ShizukuGameContextUserService() {
        this(null);
    }

    public ShizukuGameContextUserService(Context context) {
        attachInterface(null, DESCRIPTOR);
        monitorThread = new Thread(this::monitorLoop, "heimdall-game-context-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        data.enforceInterface(DESCRIPTOR);
        if (code == TRANSACTION_QUERY_AETHER) {
            synchronized (lock) {
                reply.writeNoException();
                reply.writeInt(monitoredPid);
                reply.writeString(activeUri);
                reply.writeLong(observedAt);
            }
            return true;
        }
        if (code == TRANSACTION_CLEAR_AETHER) {
            synchronized (lock) {
                activeUri = "";
                observedAt = 0L;
                minimumAcceptedAt = System.currentTimeMillis();
            }
            stopLogcat();
            reply.writeNoException();
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    public void destroy() {
        destroyed = true;
        stopLogcat();
        monitorThread.interrupt();
        System.exit(0);
    }

    private void monitorLoop() {
        while (!destroyed) {
            int pid = resolvePid(AetherSx2GameContext.PACKAGE_NAME);
            if (pid <= 0) {
                resetForPid(-1);
                pause(1000L);
                continue;
            }
            if (pid != currentPid()) {
                resetForPid(pid);
                bootstrap(pid);
            }
            stream(pid);
            pause(350L);
        }
    }

    private void bootstrap(int pid) {
        Process process = null;
        try {
            process = new ProcessBuilder("logcat", "-d", "-v", "epoch",
                    "--pid=" + pid, "EmulationThread:I", "*:S")
                    .redirectErrorStream(true).start();
            String newest = "";
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String candidate = AetherSx2GameContext.extractUri(line);
                    if (candidate.length() > 0) newest = candidate;
                }
            }
            process.waitFor(3, TimeUnit.SECONDS);
            if (newest.length() > 0) accept(pid, newest, System.currentTimeMillis());
        } catch (Throwable ignored) {
        } finally {
            if (process != null) process.destroy();
        }
    }

    private void stream(int pid) {
        Process process = null;
        try {
            String start = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
                    .format(new Date());
            process = new ProcessBuilder("logcat", "-v", "epoch", "-T", start,
                    "--pid=" + pid, "EmulationThread:I", "*:S")
                    .redirectErrorStream(true).start();
            synchronized (lock) {
                if (destroyed || pid != monitoredPid) {
                    process.destroy();
                    return;
                }
                logcatProcess = process;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (!destroyed && pid == currentPid() && (line = reader.readLine()) != null) {
                    String candidate = AetherSx2GameContext.extractUri(line);
                    if (candidate.length() > 0) {
                        accept(pid, candidate, System.currentTimeMillis());
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            synchronized (lock) {
                if (logcatProcess == process) logcatProcess = null;
            }
            if (process != null) process.destroy();
        }
    }

    private void accept(int pid, String uri, long timestamp) {
        synchronized (lock) {
            if (pid != monitoredPid || timestamp < minimumAcceptedAt) return;
            activeUri = uri;
            observedAt = timestamp;
        }
    }

    private void resetForPid(int pid) {
        stopLogcat();
        synchronized (lock) {
            monitoredPid = pid;
            activeUri = "";
            observedAt = 0L;
            minimumAcceptedAt = 0L;
        }
    }

    private int currentPid() {
        synchronized (lock) {
            return monitoredPid;
        }
    }

    private void stopLogcat() {
        Process process;
        synchronized (lock) {
            process = logcatProcess;
            logcatProcess = null;
        }
        if (process != null) process.destroy();
    }

    private static int resolvePid(String packageName) {
        Process process = null;
        try {
            process = new ProcessBuilder("pidof", packageName).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String value = reader.readLine();
                if (value == null) return -1;
                String[] tokens = value.trim().split("\\s+");
                return tokens.length == 0 ? -1 : Integer.parseInt(tokens[0]);
            }
        } catch (Throwable ignored) {
            return -1;
        } finally {
            if (process != null) process.destroy();
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
