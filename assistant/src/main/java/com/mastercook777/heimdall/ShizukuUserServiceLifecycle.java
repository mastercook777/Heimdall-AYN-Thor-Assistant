package com.mastercook777.heimdall;

/** Handles the reserved destroy transaction sent by Shizuku's UserService manager. */
final class ShizukuUserServiceLifecycle {
    // Shizuku 13.x sends this one-way transaction before removing a non-daemon
    // UserService record. Manual Binder implementations must consume it before
    // enforcing their application descriptor so their public destroy() runs.
    private static final int TRANSACTION_DESTROY = 16_777_115;

    private ShizukuUserServiceLifecycle() {
    }

    static boolean isDestroyTransaction(int code) {
        return code == TRANSACTION_DESTROY;
    }
}
