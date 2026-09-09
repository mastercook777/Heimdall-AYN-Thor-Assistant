package com.mastercook777.heimdall;

/**
 * One-shot claim for an upper-display handoff during one Started lifecycle.
 *
 * <p>The generation makes a callback posted by an earlier lifecycle harmless after
 * {@code onStop()}. Ordinary pause/resume within the same Started lifecycle does not rearm.</p>
 */
final class UpperDisplayStartedLifecycleHandoff {
    static final int NO_CLAIM = -1;

    private int generation;
    private boolean claimed;

    int claim() {
        if (claimed) {
            return NO_CLAIM;
        }
        claimed = true;
        return generation;
    }

    boolean isCurrent(int candidateGeneration) {
        return claimed && candidateGeneration == generation;
    }

    void rearmAfterStop() {
        claimed = false;
        generation++;
    }

    int generation() {
        return generation;
    }
}
