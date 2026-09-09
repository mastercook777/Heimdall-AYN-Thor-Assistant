package com.mastercook777.heimdall;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Performs the bounded startup capability probe without prompting for new permissions.
 * Presentation is deliberately kept outside this class so a branded startup animation can
 * replace the first-pass loading surface without changing the readiness contract.
 */
final class StartupReadinessCoordinator implements AutoCloseable {
    private static final long USER_SERVICE_WAIT_MS = 4_000L;
    private static final long ACCESSIBILITY_POST_WARM_WAIT_MS = 750L;

    enum Status {
        CHECKING,
        READY,
        OPTIONAL,
        ACTION_REQUIRED,
        UNAVAILABLE
    }

    static final class Snapshot {
        final Status accessibility;
        final Status shizuku;
        final Status keyboard;
        final boolean terminal;

        Snapshot(Status accessibility, Status shizuku, Status keyboard, boolean terminal) {
            this.accessibility = accessibility;
            this.shizuku = shizuku;
            this.keyboard = keyboard;
            this.terminal = terminal;
        }

        boolean allReady() {
            return accessibility == Status.READY
                    && shizuku == Status.READY
                    && keyboard == Status.READY;
        }
    }

    interface Listener {
        void onSnapshot(Snapshot snapshot);
    }

    private final Context context;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService probeExecutor =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "HeimdallStartupReadiness");
                thread.setDaemon(true);
                return thread;
            });
    private volatile boolean closed;
    private VirtualKeyboardDispatcher prewarmDispatcher;

    StartupReadinessCoordinator(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    void start() {
        publish(new Snapshot(Status.CHECKING, Status.CHECKING, Status.CHECKING, false));
        probeExecutor.execute(this::probe);
    }

    private void probe() {
        boolean accessibilityReady = ThorAccessibilityService.isReady();
        Status accessibility = accessibilityReady ? Status.READY : Status.OPTIONAL;
        publish(new Snapshot(accessibility, Status.CHECKING, Status.CHECKING, false));

        if (!ShizukuNativeController.isBinderAlive()) {
            publish(new Snapshot(accessibility, Status.ACTION_REQUIRED,
                    Status.UNAVAILABLE, true));
            return;
        }
        if (!ShizukuNativeController.isPermissionGranted()) {
            publish(new Snapshot(accessibility, Status.ACTION_REQUIRED,
                    Status.UNAVAILABLE, true));
            return;
        }

        publish(new Snapshot(accessibility, Status.CHECKING, Status.CHECKING, false));
        if (!ShizukuNativeController.warmUp(context, USER_SERVICE_WAIT_MS)) {
            publish(new Snapshot(accessibility, Status.UNAVAILABLE,
                    Status.UNAVAILABLE, true));
            return;
        }

        Status confirmedAccessibility = awaitAccessibilityAfterServiceWarm(accessibility);
        publish(new Snapshot(confirmedAccessibility, Status.READY, Status.CHECKING, false));
        mainHandler.post(() -> startKeyboardPrewarm(confirmedAccessibility));
    }

    private Status awaitAccessibilityAfterServiceWarm(Status initial) {
        if (initial == Status.READY) {
            return initial;
        }
        long deadline = SystemClock.uptimeMillis() + ACCESSIBILITY_POST_WARM_WAIT_MS;
        while (!closed && SystemClock.uptimeMillis() < deadline) {
            if (ThorAccessibilityService.isReady()) {
                return Status.READY;
            }
            SystemClock.sleep(50L);
        }
        return ThorAccessibilityService.isReady() ? Status.READY : Status.OPTIONAL;
    }

    private void startKeyboardPrewarm(Status accessibility) {
        if (closed) return;
        VirtualKeyboardDispatcher dispatcher = new VirtualKeyboardDispatcher(context,
                new VirtualKeyboardDispatcher.Listener() {
                    @Override
                    public void onReady() {
                        VirtualKeyboardDispatcher active = prewarmDispatcher;
                        if (closed || active == null) return;
                        active.park();
                        prewarmDispatcher = null;
                        publish(new Snapshot(accessibility, Status.READY,
                                Status.READY, true));
                    }

                    @Override
                    public void onUnavailable() {
                        VirtualKeyboardDispatcher active = prewarmDispatcher;
                        if (active != null) {
                            active.park();
                            prewarmDispatcher = null;
                        }
                        publish(new Snapshot(accessibility, Status.READY,
                                Status.UNAVAILABLE, true));
                    }
                });
        prewarmDispatcher = dispatcher;
        dispatcher.start();
    }

    private void publish(Snapshot snapshot) {
        mainHandler.post(() -> {
            if (!closed) listener.onSnapshot(snapshot);
        });
    }

    @Override
    public void close() {
        closed = true;
        probeExecutor.shutdownNow();
        VirtualKeyboardDispatcher dispatcher = prewarmDispatcher;
        prewarmDispatcher = null;
        if (dispatcher != null) dispatcher.park();
    }
}
