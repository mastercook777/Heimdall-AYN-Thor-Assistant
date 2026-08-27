package com.mastercook777.heimdall;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Parcel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rikka.shizuku.Shizuku;

final class ShizukuGameContextController implements AutoCloseable {
    interface Listener {
        void onGameContextChanged(GameContextSnapshot snapshot);
    }

    private static final long BIND_TIMEOUT_MS = 8_000L;
    private final Context appContext;
    private final Listener listener;
    private final Object lock = new Object();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "heimdall-game-context-client");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean requestInFlight = new AtomicBoolean();
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            synchronized (lock) {
                if (closed || !enabled) return;
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
            publish(GameContextSnapshot.UNKNOWN);
        }
    };

    private Shizuku.UserServiceArgs serviceArgs;
    private IBinder binder;
    private CountDownLatch bindLatch;
    private boolean boundRequested;
    private volatile boolean enabled;
    private volatile boolean closed;
    private boolean clearedForMain;
    private volatile boolean pendingClear;

    ShizukuGameContextController(Context context, Listener listener) {
        appContext = context.getApplicationContext();
        this.listener = listener;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clearedForMain = false;
            publish(GameContextSnapshot.UNKNOWN);
            releaseService();
        }
    }

    void refresh(ForegroundAppTracker.Snapshot foreground) {
        if (!enabled || closed) {
            clearedForMain = false;
            publish(GameContextSnapshot.UNKNOWN);
            return;
        }
        if (foreground == null
                || !AetherSx2GameContext.PACKAGE_NAME.equals(foreground.packageName)) {
            clearedForMain = false;
            releaseService();
            publish(GameContextSnapshot.UNKNOWN);
            return;
        }
        String className = foreground.className == null ? "" : foreground.className;
        if (className.endsWith(".MainActivity")) {
            publish(GameContextSnapshot.none(AetherSx2GameContext.PACKAGE_NAME,
                    -1, AetherSx2GameContext.DETECTOR_ID));
            if (!clearedForMain) {
                clearedForMain = true;
                submit(true);
            }
            return;
        }
        if (!className.endsWith(".EmulationActivity")) {
            publish(GameContextSnapshot.UNKNOWN);
            return;
        }
        clearedForMain = false;
        submit(false);
    }

    private void submit(boolean clear) {
        if (clear) pendingClear = true;
        if (!requestInFlight.compareAndSet(false, true)) return;
        executor.execute(() -> {
            try {
                IBinder service = awaitService();
                if (service == null) {
                    publish(GameContextSnapshot.UNKNOWN);
                    return;
                }
                boolean shouldClear = clear || pendingClear;
                if (shouldClear) pendingClear = false;
                if (shouldClear) clearRemote(service);
                else queryRemote(service);
            } finally {
                requestInFlight.set(false);
                if (pendingClear && enabled && !closed) submit(true);
            }
        });
    }

    private void queryRemote(IBinder service) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuGameContextUserService.DESCRIPTOR);
            if (!service.transact(ShizukuGameContextUserService.TRANSACTION_QUERY_AETHER,
                    data, reply, 0)) throw new IllegalStateException("transaction rejected");
            reply.readException();
            int pid = reply.readInt();
            String uri = reply.readString();
            long observedAt = reply.readLong();
            GameContextSnapshot snapshot = AetherSx2GameContext.snapshot(
                    pid, uri == null ? "" : uri, observedAt);
            publish(snapshot);
        } catch (Throwable error) {
            invalidateBinder();
            publish(GameContextSnapshot.UNKNOWN);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private void clearRemote(IBinder service) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuGameContextUserService.DESCRIPTOR);
            service.transact(ShizukuGameContextUserService.TRANSACTION_CLEAR_AETHER,
                    data, reply, 0);
            reply.readException();
        } catch (Throwable error) {
            invalidateBinder();
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private IBinder awaitService() {
        if (!isAuthorized()) return null;
        CountDownLatch latch;
        synchronized (lock) {
            if (closed) return null;
            if (binder != null && binder.isBinderAlive()) return binder;
            if (!boundRequested) {
                serviceArgs = new Shizuku.UserServiceArgs(new ComponentName(
                        appContext.getPackageName(),
                        ShizukuGameContextUserService.class.getName()))
                        .daemon(false)
                        .debuggable(BuildConfig.DEBUG)
                        .processNameSuffix("game_context_v1")
                        .tag("heimdall_game_context_v1")
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
        try {
            if (latch != null) latch.await(BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
        synchronized (lock) {
            if (binder != null && binder.isBinderAlive()) return binder;
            binder = null;
            boundRequested = false;
            bindLatch = null;
            return null;
        }
    }

    private boolean isAuthorized() {
        try {
            return Shizuku.pingBinder()
                    && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void invalidateBinder() {
        synchronized (lock) {
            binder = null;
            boundRequested = false;
            if (bindLatch != null) bindLatch.countDown();
            bindLatch = null;
        }
    }

    private void releaseService() {
        Shizuku.UserServiceArgs args;
        boolean unbind;
        synchronized (lock) {
            args = serviceArgs;
            unbind = boundRequested;
            serviceArgs = null;
            binder = null;
            boundRequested = false;
            if (bindLatch != null) bindLatch.countDown();
            bindLatch = null;
        }
        if (args != null && unbind) {
            try {
                Shizuku.unbindUserService(args, connection, true);
            } catch (Throwable ignored) {
            }
        }
    }

    private void publish(GameContextSnapshot snapshot) {
        if (!GameContextTracker.publish(snapshot)) return;
        if (listener != null) {
            AssistantMainHandler.post(() -> listener.onGameContextChanged(
                    GameContextTracker.latest()));
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (closed) return;
            closed = true;
        }
        executor.shutdownNow();
        releaseService();
        GameContextTracker.clear();
    }
}
