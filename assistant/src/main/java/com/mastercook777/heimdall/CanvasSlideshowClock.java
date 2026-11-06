package com.mastercook777.heimdall;

import android.os.SystemClock;

/** One monotonic phase for every visible Canvas slideshow in this process. */
final class CanvasSlideshowClock {
    private static long epochUptimeMs;
    private static int activeViews;

    private CanvasSlideshowClock() { }

    static void join() {
        if (activeViews++ == 0) epochUptimeMs = SystemClock.uptimeMillis();
    }

    static void leave() {
        if (activeViews > 0 && --activeViews == 0) epochUptimeMs = 0L;
    }

    static long nextBoundary(long nowUptimeMs, long intervalMs) {
        return nextBoundary(nowUptimeMs, epochUptimeMs, intervalMs);
    }

    static long nextBoundary(long nowUptimeMs, long epochUptimeMs,
            long intervalMs) {
        long period = Math.max(1L, intervalMs);
        long elapsed = Math.max(0L, nowUptimeMs - epochUptimeMs);
        return epochUptimeMs + (elapsed / period + 1L) * period;
    }
}
