package com.mastercook777.heimdall;

/** Keeps macro taps at one active execution with no waiting queue. */
final class MacroDispatchGate<T> {
    enum Decision {
        START,
        CANCEL,
        CANCEL_PENDING,
        IGNORE_REPEAT,
        BUSY
    }

    static final class Result<T> {
        final Decision decision;
        final long id;
        final T payload;

        Result(Decision decision, long id, T payload) {
            this.decision = decision;
            this.id = id;
            this.payload = payload;
        }
    }

    private long nextId;
    private Active<T> active;

    synchronized Result<T> onTap(Object key, boolean cancellable, T startPayload) {
        if (active == null) {
            active = new Active<>(++nextId, key, cancellable, startPayload);
            return new Result<>(Decision.START, active.id, active.payload);
        }
        if (active.key != key) {
            return new Result<>(Decision.BUSY, active.id, active.payload);
        }
        if (!active.cancellable) {
            return new Result<>(Decision.IGNORE_REPEAT, active.id, active.payload);
        }
        if (active.cancelRequested) {
            return new Result<>(Decision.CANCEL_PENDING, active.id, active.payload);
        }
        active.cancelRequested = true;
        return new Result<>(Decision.CANCEL, active.id, active.payload);
    }

    synchronized void finish(long id) {
        if (active != null && active.id == id) {
            active = null;
        }
    }

    synchronized boolean hasActive() {
        return active != null;
    }

    private static final class Active<T> {
        final long id;
        final Object key;
        final boolean cancellable;
        final T payload;
        boolean cancelRequested;

        Active(long id, Object key, boolean cancellable, T payload) {
            this.id = id;
            this.key = key;
            this.cancellable = cancellable;
            this.payload = payload;
        }
    }
}
