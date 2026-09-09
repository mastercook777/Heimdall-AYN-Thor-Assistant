package com.mastercook777.heimdall;

final class GameContextTracker {
    private static volatile GameContextSnapshot latest = GameContextSnapshot.UNKNOWN;

    private GameContextTracker() {
    }

    static GameContextSnapshot latest() {
        return latest;
    }

    static boolean publish(GameContextSnapshot snapshot) {
        GameContextSnapshot next = snapshot == null ? GameContextSnapshot.UNKNOWN : snapshot;
        GameContextSnapshot previous = latest;
        latest = next;
        return !previous.sameIdentity(next);
    }

    static void clear() {
        latest = GameContextSnapshot.UNKNOWN;
    }
}
