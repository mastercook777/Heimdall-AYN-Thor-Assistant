package com.mastercook777.heimdall;

import java.util.ArrayList;
import java.util.List;

final class ShizukuGamepadSequenceReplay {
    interface EventEmitter {
        String emit(String eventSequence);
    }

    private static final Object REPLAY_LOCK = new Object();
    private static final int MAX_EVENT_DELAY_MS = 500;

    private ShizukuGamepadSequenceReplay() {
    }

    static String replay(String sequence, EventEmitter emitter) {
        List<Event> events = parse(sequence);
        if (events.isEmpty()) {
            return "no sequence events";
        }

        synchronized (REPLAY_LOCK) {
            long targetTimeNanos = System.nanoTime();
            int emittedCount = 0;
            for (Event event : events) {
                targetTimeNanos += event.delayMs * 1_000_000L;
                if (!sleepUntil(targetTimeNanos)) {
                    return "Physical controller replay failed: interrupted";
                }
                String result = emitter.emit(event.asSingleEventSequence());
                if (!NativeGamepadPath.operationSucceeded(result)) {
                    return result;
                }
                emittedCount++;
            }
            return "evdev sequence ok events=" + emittedCount;
        }
    }

    private static List<Event> parse(String sequence) {
        List<Event> events = new ArrayList<>();
        if (sequence == null || !sequence.startsWith("seq:")) {
            return events;
        }
        String raw = sequence.substring(4);
        for (String token : raw.split(";", -1)) {
            String[] values = token.split(",", -1);
            if (values.length != 4) {
                continue;
            }
            try {
                int type = Integer.parseInt(values[0].trim());
                int code = Integer.parseInt(values[1].trim());
                int value = Integer.parseInt(values[2].trim());
                int delayMs = Integer.parseInt(values[3].trim());
                events.add(new Event(type, code, value,
                        Math.max(0, Math.min(MAX_EVENT_DELAY_MS, delayMs))));
            } catch (NumberFormatException ignored) {
                // Match the native seq: parser: malformed events are skipped.
            }
        }
        return events;
    }

    private static boolean sleepUntil(long targetTimeNanos) {
        while (true) {
            long remainingNanos = targetTimeNanos - System.nanoTime();
            if (remainingNanos <= 0L) {
                return true;
            }
            long millis = remainingNanos / 1_000_000L;
            int nanos = (int) (remainingNanos % 1_000_000L);
            try {
                Thread.sleep(millis, nanos);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private static final class Event {
        final int type;
        final int code;
        final int value;
        final int delayMs;

        Event(int type, int code, int value, int delayMs) {
            this.type = type;
            this.code = code;
            this.value = value;
            this.delayMs = delayMs;
        }

        String asSingleEventSequence() {
            return "seq:" + type + "," + code + "," + value + ",0";
        }
    }
}
