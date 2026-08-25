package com.mastercook777.heimdall;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

        if (containsFrameMarkers(events)) {
            return replayFramed(events, emitter);
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

    private static String replayFramed(List<Event> events, EventEmitter emitter) {
        synchronized (REPLAY_LOCK) {
            long targetTimeNanos = System.nanoTime();
            List<Event> frame = new ArrayList<>();
            FramedState state = new FramedState();
            int emittedCount = 0;
            for (Event event : events) {
                targetTimeNanos += event.delayMs * 1_000_000L;
                frame.add(event);
                if (event.type != GamepadSequenceComposer.EV_SYN) {
                    continue;
                }
                if (!sleepUntil(targetTimeNanos)) {
                    releaseFramedStateBestEffort(state, emitter);
                    return "Physical controller replay failed: interrupted";
                }
                FramedState candidate = state.copy();
                candidate.apply(frame);
                String result = emitter.emit(asFrameSequence(frame));
                if (!NativeGamepadPath.operationSucceeded(result)) {
                    releaseFramedStateBestEffort(candidate, emitter);
                    return result;
                }
                state = candidate;
                emittedCount += frame.size();
                frame.clear();
            }
            if (!frame.isEmpty()) {
                if (!sleepUntil(targetTimeNanos)) {
                    releaseFramedStateBestEffort(state, emitter);
                    return "Physical controller replay failed: interrupted";
                }
                FramedState candidate = state.copy();
                candidate.apply(frame);
                String result = emitter.emit(asFrameSequence(frame));
                if (!NativeGamepadPath.operationSucceeded(result)) {
                    releaseFramedStateBestEffort(candidate, emitter);
                    return result;
                }
                state = candidate;
                emittedCount += frame.size();
            }
            return "evdev sequence ok events=" + emittedCount;
        }
    }

    private static boolean containsFrameMarkers(List<Event> events) {
        for (Event event : events) {
            if (event.type == GamepadSequenceComposer.EV_SYN) {
                return true;
            }
        }
        return false;
    }

    private static String asFrameSequence(List<Event> frame) {
        StringBuilder sequence = new StringBuilder("seq:");
        for (Event event : frame) {
            if (sequence.length() > 4) {
                sequence.append(';');
            }
            sequence.append(event.type).append(',').append(event.code).append(',')
                    .append(event.value).append(",0");
        }
        return sequence.toString();
    }

    private static void releaseFramedStateBestEffort(FramedState state,
            EventEmitter emitter) {
        String release = state.releaseSequence();
        if (release != null) {
            try {
                emitter.emit(release);
            } catch (Throwable ignored) {
            }
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

    private static final class FramedState {
        final Set<Integer> pressedKeys = new LinkedHashSet<>();
        int hatX;
        int hatY;

        FramedState copy() {
            FramedState copy = new FramedState();
            copy.pressedKeys.addAll(pressedKeys);
            copy.hatX = hatX;
            copy.hatY = hatY;
            return copy;
        }

        void apply(List<Event> events) {
            for (Event event : events) {
                if (event.type == GamepadSequenceComposer.EV_KEY) {
                    if (event.value == 0) {
                        pressedKeys.remove(event.code);
                    } else {
                        pressedKeys.add(event.code);
                    }
                } else if (event.type == GamepadSequenceComposer.EV_ABS) {
                    if (event.code == GamepadSequenceComposer.ABS_HAT0X) {
                        hatX = event.value;
                    } else if (event.code == GamepadSequenceComposer.ABS_HAT0Y) {
                        hatY = event.value;
                    }
                }
            }
        }

        String releaseSequence() {
            if (pressedKeys.isEmpty() && hatX == 0 && hatY == 0) {
                return null;
            }
            StringBuilder sequence = new StringBuilder("seq:");
            for (Integer code : pressedKeys) {
                append(sequence, GamepadSequenceComposer.EV_KEY, code, 0);
            }
            if (hatX != 0) {
                append(sequence, GamepadSequenceComposer.EV_ABS,
                        GamepadSequenceComposer.ABS_HAT0X, 0);
            }
            if (hatY != 0) {
                append(sequence, GamepadSequenceComposer.EV_ABS,
                        GamepadSequenceComposer.ABS_HAT0Y, 0);
            }
            append(sequence, GamepadSequenceComposer.EV_SYN, 0, 0);
            return sequence.toString();
        }

        private static void append(StringBuilder sequence, int type, int code, int value) {
            if (sequence.length() > 4) {
                sequence.append(';');
            }
            sequence.append(type).append(',').append(code).append(',')
                    .append(value).append(",0");
        }
    }
}
