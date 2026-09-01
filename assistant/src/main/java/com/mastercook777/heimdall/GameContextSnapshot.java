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
    final String platformKind;
    final String platformIdentityKey;
    final String platformLabel;

    GameContextSnapshot(State state, String packageName, int pid, String kind,
            String identityKey, String label, long observedAt, String detectorId) {
        this(state, packageName, pid, kind, identityKey, label, observedAt, detectorId,
                "", "", "");
    }

    GameContextSnapshot(State state, String packageName, int pid, String kind,
            String identityKey, String label, long observedAt, String detectorId,
            String platformKind, String platformIdentityKey, String platformLabel) {
        this.state = state == null ? State.UNKNOWN : state;
        this.packageName = safe(packageName);
        this.pid = pid;
        this.kind = safe(kind);
        this.identityKey = safe(identityKey);
        this.label = safe(label);
        this.observedAt = observedAt;
        this.detectorId = safe(detectorId);
        this.platformKind = safe(platformKind);
        this.platformIdentityKey = safe(platformIdentityKey);
        this.platformLabel = safe(platformLabel);
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
                && detectorId.equals(other.detectorId)
                && platformKind.equals(other.platformKind)
                && platformIdentityKey.equals(other.platformIdentityKey)
                && platformLabel.equals(other.platformLabel)
                && (state != State.ACTIVE || observedAt == other.observedAt);
    }

    boolean hasPlatformIdentity() {
        return state == State.ACTIVE
                && platformKind.length() > 0
                && platformIdentityKey.length() > 0;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
