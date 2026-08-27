package com.mastercook777.heimdall;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;

/** Activity-scoped client for the separate Debug ROM probe UserService. */
final class RomContextProbeClient implements AutoCloseable {
    private static final long BIND_TIMEOUT_MS = 10_000L;

    private final Context appContext;
    private final Object lock = new Object();
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (lock) {
                binder = service;
                if (bindLatch != null) bindLatch.countDown();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (lock) {
                binder = null;
                boundRequested = false;
                if (bindLatch != null) bindLatch.countDown();
                bindLatch = null;
            }
        }
    };

    private Shizuku.UserServiceArgs serviceArgs;
    private IBinder binder;
    private CountDownLatch bindLatch;
    private boolean boundRequested;
    private boolean closed;

    RomContextProbeClient(Context context) {
        appContext = context.getApplicationContext();
    }

    boolean isAuthorized() {
        try {
            return Shizuku.pingBinder()
                    && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    String capture(int target, long sinceEpochMs) {
        if (!isAuthorized()) return "probe-error=shizuku-unavailable";
        IBinder service = awaitService();
        if (service == null) return "probe-error=user-service-unavailable";
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(RomContextProbeUserService.DESCRIPTOR);
            data.writeInt(target);
            data.writeLong(sinceEpochMs);
            if (!service.transact(RomContextProbeUserService.TRANSACTION_CAPTURE,
                    data, reply, 0)) {
                return "probe-error=transaction-rejected";
            }
            reply.readException();
            String result = reply.readString();
            return result == null ? "probe-error=empty-report" : result;
        } catch (RemoteException | RuntimeException error) {
            synchronized (lock) {
                binder = null;
            }
            return "probe-error=" + error.getClass().getSimpleName();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private IBinder awaitService() {
        CountDownLatch latch;
        synchronized (lock) {
            if (closed) return null;
            if (binder != null && binder.isBinderAlive()) return binder;
            if (!boundRequested) {
                serviceArgs = new Shizuku.UserServiceArgs(new ComponentName(
                        appContext.getPackageName(),
                        RomContextProbeUserService.class.getName()))
                        .daemon(false)
                        .debuggable(true)
                        .processNameSuffix("rom_context_probe_v1")
                        .tag("heimdall_rom_context_probe_v1")
                        .version(1);
                bindLatch = new CountDownLatch(1);
                boundRequested = true;
                try {
                    Shizuku.bindUserService(serviceArgs, connection);
                } catch (Throwable error) {
                    boundRequested = false;
                    bindLatch = null;
                    return null;
                }
            }
            latch = bindLatch;
        }
        if (latch != null) {
            try {
                latch.await(BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
        }
        synchronized (lock) {
            if (binder != null && binder.isBinderAlive()) return binder;
            binder = null;
            boundRequested = false;
            bindLatch = null;
            return null;
        }
    }

    @Override
    public void close() {
        Shizuku.UserServiceArgs args;
        synchronized (lock) {
            if (closed) return;
            closed = true;
            args = serviceArgs;
            binder = null;
            if (bindLatch != null) bindLatch.countDown();
            bindLatch = null;
        }
        if (args != null && boundRequested) {
            try {
                Shizuku.unbindUserService(args, connection, true);
            } catch (Throwable ignored) {
            }
        }
    }
}
