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
    static final String SERVICE_PROCESS_SUFFIX = "game_context_v12";
    static final String SERVICE_TAG = "heimdall_game_context_v12";
    static final int SERVICE_VERSION = 11;
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
                if (closed || !boundRequested) return;
                binder = service;
                if (bindLatch != null) bindLatch.countDown();
            }
            recordDiagnostic("service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (lock) {
                binder = null;
                boundRequested = false;
                if (bindLatch != null) bindLatch.countDown();
                bindLatch = null;
            }
            recordDiagnostic("service disconnected");
            publish(GameContextSnapshot.UNKNOWN);
        }
    };

    private Shizuku.UserServiceArgs serviceArgs;
    private IBinder binder;
    private CountDownLatch bindLatch;
    private boolean boundRequested;
    private volatile boolean enabled;
    private volatile boolean closed;
    private String lastDiagnostic = "";

    ShizukuGameContextController(Context context, Listener listener) {
        appContext = context.getApplicationContext();
        this.listener = listener;
    }

    void setEnabled(boolean enabled) {
        boolean changed = this.enabled != enabled;
        this.enabled = enabled;
        if (changed) recordDiagnostic("controller enabled=" + enabled);
        if (!enabled) {
            publish(GameContextSnapshot.UNKNOWN);
            releaseService();
        }
    }

    /** Pauses UI-driven queries across a temporary lower-Activity stop without
     * destroying the detector that still observes the unchanged upper game. */
    void suspend() {
        if (!enabled) return;
        enabled = false;
        recordDiagnostic("controller suspended; service retained");
    }

    void refresh(ForegroundAppTracker.Snapshot foreground) {
        if (!enabled || closed) {
            publish(GameContextSnapshot.UNKNOWN);
            return;
        }
        if (foreground == null || !isSupportedPackage(foreground.packageName)) {
            releaseService();
            publish(GameContextSnapshot.UNKNOWN);
            return;
        }
        submit(foreground.packageName);
    }

    private void submit(String packageName) {
        if (!requestInFlight.compareAndSet(false, true)) {
            recordDiagnostic("request coalesced package=" + packageName);
            return;
        }
        executor.execute(() -> {
            try {
                IBinder service = awaitService();
                if (service == null) {
                    recordDiagnostic("query unavailable package=" + packageName);
                    publish(GameContextSnapshot.UNKNOWN);
                    return;
                }
                queryRemote(service, packageName);
            } finally {
                requestInFlight.set(false);
            }
        });
    }

    private void queryRemote(IBinder service, String packageName) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuGameContextUserService.DESCRIPTOR);
            data.writeString(packageName);
            if (!service.transact(ShizukuGameContextUserService.TRANSACTION_QUERY_CONTEXT,
                    data, reply, 0)) throw new IllegalStateException("transaction rejected");
            reply.readException();
            String resolvedPackage = reply.readString();
            int activityState = reply.readInt();
            int pid = reply.readInt();
            String identityValue = reply.readString();
            String identityLabel = reply.readString();
            long observedAt = reply.readLong();
            String platformValue = reply.readString();
            String platformLabel = reply.readString();
            boolean platformAffectsContentIdentity = reply.readInt() != 0;
            GameContextSnapshot snapshot;
            if (!packageName.equals(resolvedPackage)) {
                snapshot = GameContextSnapshot.UNKNOWN;
            } else if (AetherSx2GameContext.PACKAGE_NAME.equals(packageName)) {
                if (activityState == AetherSx2GameContext.ACTIVITY_MAIN) {
                    snapshot = GameContextSnapshot.none(packageName, pid,
                            AetherSx2GameContext.DETECTOR_ID);
                } else if (activityState == AetherSx2GameContext.ACTIVITY_EMULATION) {
                    snapshot = AetherSx2GameContext.snapshot(pid,
                            identityValue == null ? "" : identityValue, observedAt);
                } else {
                    snapshot = GameContextSnapshot.UNKNOWN;
                }
            } else if (RetroArchGameContext.supportsPackage(packageName)) {
                RetroArchGameContext.LaunchRecord record =
                        new RetroArchGameContext.LaunchRecord(
                                identityValue == null ? "" : identityValue,
                                identityLabel == null ? "" : identityLabel,
                                platformValue == null ? "" : platformValue,
                                platformLabel == null ? "" : platformLabel,
                                platformAffectsContentIdentity,
                                observedAt);
                snapshot = RetroArchGameContext.snapshot(packageName, pid, record);
            } else if (PpssppGameContext.supportsPackage(packageName)) {
                snapshot = PpssppGameContext.snapshot(packageName, pid,
                        identityValue == null ? "" : identityValue, observedAt);
            } else if (EdenGameContext.supportsPackage(packageName)) {
                if (activityState == EdenGameContext.ACTIVITY_MAIN) {
                    snapshot = GameContextSnapshot.none(packageName, pid,
                            EdenGameContext.DETECTOR_ID);
                } else if (activityState == EdenGameContext.ACTIVITY_EMULATION) {
                    snapshot = EdenGameContext.snapshot(packageName, pid,
                            identityValue == null ? "" : identityValue,
                            identityLabel == null ? "" : identityLabel, observedAt);
                } else {
                    snapshot = GameContextSnapshot.UNKNOWN;
                }
            } else {
                snapshot = GameContextSnapshot.UNKNOWN;
            }
            publish(snapshot);
        } catch (Throwable error) {
            recordDiagnostic("query failed type=" + error.getClass().getSimpleName());
            invalidateBinder();
            publish(GameContextSnapshot.UNKNOWN);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private IBinder awaitService() {
        if (!isAuthorized()) {
            recordDiagnostic("authorization unavailable");
            return null;
        }
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
                        .processNameSuffix(SERVICE_PROCESS_SUFFIX)
                        .tag(SERVICE_TAG)
                        .version(SERVICE_VERSION);
                bindLatch = new CountDownLatch(1);
                boundRequested = true;
                try {
                    recordDiagnostic("service bind requested");
                    Shizuku.bindUserService(serviceArgs, connection);
                } catch (Throwable error) {
                    recordDiagnostic("service bind failed type="
                            + error.getClass().getSimpleName());
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
            recordDiagnostic("service bind timeout");
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

    static boolean isSupportedPackage(String packageName) {
        return AetherSx2GameContext.PACKAGE_NAME.equals(packageName)
                || RetroArchGameContext.supportsPackage(packageName)
                || PpssppGameContext.supportsPackage(packageName)
                || EdenGameContext.supportsPackage(packageName);
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
                recordDiagnostic("service released");
            } catch (Throwable ignored) {
            }
        }
    }

    private void publish(GameContextSnapshot snapshot) {
        if (!GameContextTracker.publish(snapshot)) return;
        recordDiagnostic("snapshot state=" + snapshot.state
                + " package=" + snapshot.packageName
                + " pid=" + snapshot.pid
                + " detector=" + snapshot.detectorId
                + " kind=" + snapshot.kind
                + " platform=" + snapshot.hasPlatformIdentity());
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

    private void recordDiagnostic(String message) {
        synchronized (lock) {
            if (message.equals(lastDiagnostic)) return;
            lastDiagnostic = message;
        }
        HeimdallStabilityDiagnostics.recordGameContextDiagnostic(appContext, message);
    }
}
