package com.mastercook777.heimdall;

import java.util.HashSet;
import java.util.Set;

final class GamepadSequencePolicy {
    static final int EV_KEY = 1;

    static final int KEY_RECENT_APPS = 120;
    static final int KEY_BACK = 158;
    static final int KEY_HOME = 172;
    static final int KEY_APPSELECT = 580;

    static final int MAX_REPLAY_EVENTS = 1024;
    static final long MAX_REPLAY_DURATION_MS = 10_000L;

    private static final long MIN_REPLAY_TIMEOUT_MS = 3_000L;
    private static final long REPLAY_TIMEOUT_MARGIN_MS = 3_000L;
    private static final long REPLAY_EVENT_BUDGET_MS = 5L;
    private static final long MAX_REPLAY_TIMEOUT_MS = 15_000L;
    private static final int MAX_EVENT_DELAY_MS = 500;

    private GamepadSequencePolicy() {
    }

    static Inspection inspect(String sequence) {
        String raw = sequence == null ? "" : sequence.trim();
        if (!raw.startsWith("seq:") || raw.length() <= 4) {
            return new Inspection(0, 0L, -1, false);
        }

        int eventCount = 0;
        long replayDurationMs = 0L;
        int systemNavigationScanCode = -1;
        Set<Integer> pressedSystemNavigationKeys = new HashSet<>();
        for (String token : raw.substring(4).split(";", -1)) {
            int[] item = parseItem(token);
            if (item == null) {
                continue;
            }
            eventCount++;
            replayDurationMs += Math.max(0, Math.min(MAX_EVENT_DELAY_MS, item[3]));
            if (systemNavigationScanCode < 0 && item[0] == EV_KEY
                    && isSystemNavigationScanCode(item[1])) {
                systemNavigationScanCode = item[1];
            }
            if (item[0] == EV_KEY && isSystemNavigationScanCode(item[1])) {
                if (item[2] == 0) {
                    pressedSystemNavigationKeys.remove(item[1]);
                } else {
                    pressedSystemNavigationKeys.add(item[1]);
                }
            }
        }
        return new Inspection(eventCount, replayDurationMs, systemNavigationScanCode,
                !pressedSystemNavigationKeys.isEmpty());
    }

    static boolean isSystemNavigationScanCode(int scanCode) {
        return scanCode == KEY_BACK
                || scanCode == KEY_HOME
                || scanCode == KEY_RECENT_APPS
                || scanCode == KEY_APPSELECT;
    }

    private static int[] parseItem(String token) {
        String[] values = token == null ? new String[0] : token.trim().split(",", -1);
        if (values.length != 4) {
            return null;
        }
        int[] item = new int[4];
        try {
            for (int i = 0; i < item.length; i++) {
                item[i] = Integer.parseInt(values[i].trim());
            }
            return item;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static final class Inspection {
        final int eventCount;
        final long replayDurationMs;
        final int systemNavigationScanCode;
        final boolean hasUnreleasedSystemNavigationKey;

        Inspection(int eventCount, long replayDurationMs, int systemNavigationScanCode,
                boolean hasUnreleasedSystemNavigationKey) {
            this.eventCount = eventCount;
            this.replayDurationMs = replayDurationMs;
            this.systemNavigationScanCode = systemNavigationScanCode;
            this.hasUnreleasedSystemNavigationKey = hasUnreleasedSystemNavigationKey;
        }

        boolean containsSystemNavigationKey() {
            return systemNavigationScanCode >= 0;
        }

        boolean exceedsReplayLimits() {
            return eventCount > MAX_REPLAY_EVENTS
                    || replayDurationMs > MAX_REPLAY_DURATION_MS;
        }

        long replayTimeoutMs() {
            long estimated = replayDurationMs
                    + eventCount * REPLAY_EVENT_BUDGET_MS
                    + REPLAY_TIMEOUT_MARGIN_MS;
            return Math.min(MAX_REPLAY_TIMEOUT_MS,
                    Math.max(MIN_REPLAY_TIMEOUT_MS, estimated));
        }
    }
}
