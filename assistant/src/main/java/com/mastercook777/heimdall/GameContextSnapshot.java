package com.mastercook777.heimdall;

final class GameContextSnapshot {
    enum State { ACTIVE, NONE, UNKNOWN }

    static final GameContextSnapshot UNKNOWN = new GameContextSnapshot(
            State.UNKNOWN, "", -1, "", "", "", 0L, "");

    final State state;
    final String packageName;
    final int pid;
    final String kind;
    final String identityKey;
    final String label;
    final long observedAt;
    final String detectorId;

    GameContextSnapshot(State state, String packageName, int pid, String kind,
            String identityKey, String label, long observedAt, String detectorId) {
        this.state = state == null ? State.UNKNOWN : state;
        this.packageName = safe(packageName);
        this.pid = pid;
        this.kind = safe(kind);
        this.identityKey = safe(identityKey);
        this.label = safe(label);
        this.observedAt = observedAt;
        this.detectorId = safe(detectorId);
    }

    static GameContextSnapshot none(String packageName, int pid, String detectorId) {
        return new GameContextSnapshot(State.NONE, packageName, pid,
                "", "", "", System.currentTimeMillis(), detectorId);
    }

    boolean sameIdentity(GameContextSnapshot other) {
        if (other == null || state != other.state || pid != other.pid) return false;
        return packageName.equals(other.packageName)
                && kind.equals(other.kind)
                && identityKey.equals(other.identityKey)
                && label.equals(other.label)
                && detectorId.equals(other.detectorId);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
