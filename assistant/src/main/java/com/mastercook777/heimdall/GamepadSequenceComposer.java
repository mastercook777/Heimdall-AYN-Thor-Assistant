package com.mastercook777.heimdall;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Builds a bounded native-controller seq: payload from structured digital actions. */
final class GamepadSequenceComposer {
    static final int DPAD_KEYS = 0;
    static final int DPAD_HAT = 1;

    static final int EV_SYN = 0;
    static final int EV_KEY = 1;
    static final int EV_ABS = 3;
    static final int SYN_REPORT = 0;
    static final int ABS_HAT0X = 16;
    static final int ABS_HAT0Y = 17;

    static final int BTN_A = 304;
    static final int BTN_B = 305;
    static final int BTN_X = 307;
    static final int BTN_Y = 308;
    static final int BTN_L1 = 310;
    static final int BTN_R1 = 311;
    static final int BTN_L2 = 312;
    static final int BTN_R2 = 313;
    static final int BTN_SELECT = 314;
    static final int BTN_START = 315;
    static final int BTN_L3 = 317;
    static final int BTN_R3 = 318;
    static final int BTN_DPAD_UP = 544;
    static final int BTN_DPAD_DOWN = 545;
    static final int BTN_DPAD_LEFT = 546;
    static final int BTN_DPAD_RIGHT = 547;

    static final int MAX_FRAMES = 24;
    static final int MIN_TIMING_MS = 8;
    static final int MAX_TIMING_MS = 500;
    static final int MAX_HOLD_MS = 5_000;
    static final int DEFAULT_FRAME_INTERVAL_MS = 40;
    static final int DEFAULT_FINAL_HOLD_MS = 60;
    static final int DEFAULT_LONG_HOLD_MS = 500;

    static final ButtonSpec[] DIGITAL_BUTTONS = {
            new ButtonSpec(BTN_A, "A"), new ButtonSpec(BTN_B, "B"),
            new ButtonSpec(BTN_X, "X"), new ButtonSpec(BTN_Y, "Y"),
            new ButtonSpec(BTN_L1, "L1"), new ButtonSpec(BTN_R1, "R1"),
            new ButtonSpec(BTN_L2, "L2"), new ButtonSpec(BTN_R2, "R2"),
            new ButtonSpec(BTN_SELECT, "SELECT"), new ButtonSpec(BTN_START, "START"),
            new ButtonSpec(BTN_L3, "L3"), new ButtonSpec(BTN_R3, "R3")
    };

    private GamepadSequenceComposer() {
    }

    static final class ButtonSpec {
        final int code;
        final String label;

        ButtonSpec(int code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    static final class Frame {
        final int directionX;
        final int directionY;
        final List<Integer> buttons;
        final int holdOverrideMs;

        Frame(int directionX, int directionY, List<Integer> buttons) {
            this(directionX, directionY, buttons, 0);
        }

        Frame(int directionX, int directionY, List<Integer> buttons, int holdOverrideMs) {
            this.directionX = normalizeDirection(directionX);
            this.directionY = normalizeDirection(directionY);
            Set<Integer> unique = new LinkedHashSet<>();
            if (buttons != null) {
                unique.addAll(buttons);
            }
            this.buttons = new ArrayList<>(unique);
            this.holdOverrideMs = holdOverrideMs <= 0 ? 0 : clampHold(holdOverrideMs);
        }

        boolean isEmpty() {
            return directionX == 0 && directionY == 0 && buttons.isEmpty();
        }

        String displayLabel() {
            List<String> labels = new ArrayList<>();
            String direction = directionLabel(directionX, directionY);
            if (direction.length() > 0) {
                labels.add(direction);
            }
            for (Integer code : buttons) {
                String label = buttonLabel(code == null ? -1 : code);
                if (label.length() > 0) {
                    labels.add(label);
                }
            }
            return join(labels, " + ");
        }
    }

    static final class EditableSequence {
        final List<Frame> frames;
        final int frameIntervalMs;
        final int finalHoldMs;

        EditableSequence(List<Frame> frames, int frameIntervalMs, int finalHoldMs) {
            this.frames = frames;
            this.frameIntervalMs = clampTiming(frameIntervalMs);
            this.finalHoldMs = clampTiming(finalHoldMs);
        }
    }

    static String build(List<Frame> frames, int dpadEncoding,
            int frameIntervalMs, int finalHoldMs) {
        if (frames == null || frames.isEmpty() || frames.size() > MAX_FRAMES) {
            return "";
        }
        int interval = clampTiming(frameIntervalMs);
        int hold = clampTiming(finalHoldMs);
        List<Event> events = new ArrayList<>();
        int currentX = 0;
        int currentY = 0;
        Set<Integer> currentButtons = new LinkedHashSet<>();

        for (int index = 0; index < frames.size(); index++) {
            Frame frame = frames.get(index);
            if (frame == null || frame.isEmpty()) {
                return "";
            }
            Frame previous = index == 0 ? null : frames.get(index - 1);
            int delay = previous == null ? 0
                    : previous.holdOverrideMs > 0 ? 0 : interval;
            List<Event> transition = new ArrayList<>();
            Set<Integer> targetButtons = new LinkedHashSet<>(frame.buttons);

            for (Integer code : currentButtons) {
                if (!targetButtons.contains(code)) {
                    transition.add(new Event(EV_KEY, code, 0, 0));
                }
            }
            for (Integer code : targetButtons) {
                if (!currentButtons.contains(code)) {
                    transition.add(new Event(EV_KEY, code, 1, 0));
                }
            }
            addDirectionTransition(transition, dpadEncoding,
                    currentX, currentY, frame.directionX, frame.directionY);
            appendFrame(events, transition, delay);
            if (frame.holdOverrideMs > 0) {
                appendHoldMarkers(events, frame.holdOverrideMs);
            }
            currentButtons = targetButtons;
            currentX = frame.directionX;
            currentY = frame.directionY;
        }

        List<Event> release = new ArrayList<>();
        for (Integer code : currentButtons) {
            release.add(new Event(EV_KEY, code, 0, 0));
        }
        addDirectionTransition(release, dpadEncoding, currentX, currentY, 0, 0);
        Frame lastFrame = frames.get(frames.size() - 1);
        appendFrame(events, release, lastFrame.holdOverrideMs > 0 ? 0 : hold);
        return serialize(events);
    }

    static EditableSequence parseEditable(String sequence) {
        if (!hasFrameMarkers(sequence)) {
            return null;
        }
        List<ParsedGroup> groups = new ArrayList<>();
        List<Event> pending = new ArrayList<>();
        String body = sequence.substring(4);
        for (String token : body.split(";", -1)) {
            String[] values = token.split(",", -1);
            if (values.length != 4) {
                return null;
            }
            int type;
            int code;
            int value;
            int delay;
            try {
                type = Integer.parseInt(values[0].trim());
                code = Integer.parseInt(values[1].trim());
                value = Integer.parseInt(values[2].trim());
                delay = Integer.parseInt(values[3].trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
            if (delay < 0) {
                return null;
            }
            if (type == EV_SYN && code == SYN_REPORT && value == 0) {
                int groupDelay = pending.isEmpty() ? delay : pending.get(0).delayMs;
                groups.add(new ParsedGroup(new ArrayList<>(pending), groupDelay));
                pending.clear();
            } else {
                pending.add(new Event(type, code, value, delay));
            }
        }
        if (!pending.isEmpty() || groups.size() < 2) {
            return null;
        }

        Set<Integer> heldButtons = new LinkedHashSet<>();
        boolean dpadLeft = false;
        boolean dpadRight = false;
        boolean dpadUp = false;
        boolean dpadDown = false;
        int directionX = 0;
        int directionY = 0;
        List<Frame> snapshots = new ArrayList<>();
        for (ParsedGroup group : groups) {
            for (Event event : group.events) {
                if (event.type == EV_KEY) {
                    boolean pressed = event.value != 0;
                    if (event.code == BTN_DPAD_LEFT) {
                        dpadLeft = pressed;
                    } else if (event.code == BTN_DPAD_RIGHT) {
                        dpadRight = pressed;
                    } else if (event.code == BTN_DPAD_UP) {
                        dpadUp = pressed;
                    } else if (event.code == BTN_DPAD_DOWN) {
                        dpadDown = pressed;
                    } else if (buttonLabel(event.code).length() > 0) {
                        if (pressed) {
                            heldButtons.add(event.code);
                        } else {
                            heldButtons.remove(event.code);
                        }
                    } else {
                        return null;
                    }
                    directionX = directionFromKeys(dpadLeft, dpadRight);
                    directionY = directionFromKeys(dpadUp, dpadDown);
                } else if (event.type == EV_ABS && event.code == ABS_HAT0X) {
                    directionX = normalizeDirection(event.value);
                } else if (event.type == EV_ABS && event.code == ABS_HAT0Y) {
                    directionY = normalizeDirection(event.value);
                } else {
                    return null;
                }
            }
            snapshots.add(new Frame(directionX, directionY,
                    orderedButtons(heldButtons)));
        }

        Frame released = snapshots.get(snapshots.size() - 1);
        if (!released.isEmpty()) {
            return null;
        }
        int releaseGroupIndex = groups.size() - 1;
        List<Frame> actionFrames = new ArrayList<>();
        List<Integer> actionGroupIndices = new ArrayList<>();
        List<Integer> holdOverrides = new ArrayList<>();
        for (int groupIndex = 0; groupIndex < releaseGroupIndex; groupIndex++) {
            Frame snapshot = snapshots.get(groupIndex);
            ParsedGroup group = groups.get(groupIndex);
            if (group.events.isEmpty()) {
                if (actionFrames.isEmpty()
                        || !sameFrameState(snapshot, actionFrames.get(actionFrames.size() - 1))
                        || group.delayMs <= 0) {
                    return null;
                }
                int holdIndex = holdOverrides.size() - 1;
                holdOverrides.set(holdIndex, clampHold(
                        holdOverrides.get(holdIndex) + Math.min(MAX_TIMING_MS, group.delayMs)));
                continue;
            }
            if (snapshot.isEmpty()) {
                return null;
            }
            actionFrames.add(snapshot);
            actionGroupIndices.add(groupIndex);
            holdOverrides.add(0);
        }
        if (actionFrames.isEmpty() || actionFrames.size() > MAX_FRAMES) {
            return null;
        }

        int actionCount = actionFrames.size();
        int interval = DEFAULT_FRAME_INTERVAL_MS;
        if (actionCount > 1) {
            List<Integer> nonFinalDurations = new ArrayList<>();
            for (int index = 0; index < actionCount - 1; index++) {
                if (holdOverrides.get(index) == 0) {
                    int nextGroup = actionGroupIndices.get(index) + 1;
                    nonFinalDurations.add(clampTiming(groups.get(nextGroup).delayMs));
                }
            }
            interval = mostCommonTiming(nonFinalDurations, DEFAULT_FRAME_INTERVAL_MS);
        }
        int lastIndex = actionCount - 1;
        int finalHold = holdOverrides.get(lastIndex) > 0
                ? DEFAULT_FINAL_HOLD_MS
                : clampTiming(groups.get(actionGroupIndices.get(lastIndex) + 1).delayMs);
        List<Frame> restored = new ArrayList<>();
        for (int index = 0; index < actionCount; index++) {
            Frame frame = actionFrames.get(index);
            restored.add(new Frame(frame.directionX, frame.directionY,
                    frame.buttons, holdOverrides.get(index)));
        }
        return new EditableSequence(restored, interval, finalHold);
    }

    static List<Frame> mirrorHorizontally(List<Frame> frames) {
        List<Frame> mirrored = new ArrayList<>();
        if (frames == null) {
            return mirrored;
        }
        for (Frame frame : frames) {
            if (frame == null) {
                continue;
            }
            mirrored.add(new Frame(-frame.directionX, frame.directionY,
                    frame.buttons, frame.holdOverrideMs));
        }
        return mirrored;
    }

    static long durationMs(List<Frame> frames, int frameIntervalMs, int finalHoldMs) {
        if (frames == null || frames.isEmpty()) {
            return 0L;
        }
        int interval = clampTiming(frameIntervalMs);
        int finalHold = clampTiming(finalHoldMs);
        long duration = 0L;
        for (int index = 0; index < frames.size(); index++) {
            Frame frame = frames.get(index);
            if (frame == null || frame.isEmpty()) {
                continue;
            }
            if (frame.holdOverrideMs > 0) {
                duration += frame.holdOverrideMs;
            } else if (index == frames.size() - 1) {
                duration += finalHold;
            } else {
                duration += interval;
            }
        }
        return duration;
    }

    static int clampTiming(int value) {
        return Math.max(MIN_TIMING_MS, Math.min(MAX_TIMING_MS, value));
    }

    static int clampHold(int value) {
        return Math.max(MIN_TIMING_MS, Math.min(MAX_HOLD_MS, value));
    }

    private static boolean sameFrameState(Frame first, Frame second) {
        return first != null && second != null
                && first.directionX == second.directionX
                && first.directionY == second.directionY
                && first.buttons.equals(second.buttons);
    }

    private static int directionFromKeys(boolean negative, boolean positive) {
        if (negative == positive) {
            return 0;
        }
        return negative ? -1 : 1;
    }

    private static List<Integer> orderedButtons(Set<Integer> heldButtons) {
        List<Integer> ordered = new ArrayList<>();
        for (ButtonSpec spec : DIGITAL_BUTTONS) {
            if (heldButtons.contains(spec.code)) {
                ordered.add(spec.code);
            }
        }
        return ordered;
    }

    private static int mostCommonTiming(List<Integer> values, int fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        int bestValue = values.get(0);
        int bestCount = 0;
        for (Integer candidate : values) {
            int count = 0;
            for (Integer value : values) {
                if (candidate.equals(value)) {
                    count++;
                }
            }
            if (count > bestCount) {
                bestValue = candidate;
                bestCount = count;
            }
        }
        return clampTiming(bestValue);
    }

    static boolean isComposedSequence(String sequence) {
        return parseEditable(sequence) != null;
    }

    private static boolean hasFrameMarkers(String sequence) {
        if (sequence == null || !sequence.startsWith("seq:")) {
            return false;
        }
        for (String token : sequence.substring(4).split(";", -1)) {
            String[] values = token.split(",", -1);
            if (values.length == 4 && "0".equals(values[0].trim())
                    && "0".equals(values[1].trim()) && "0".equals(values[2].trim())) {
                return true;
            }
        }
        return false;
    }

    private static void addDirectionTransition(List<Event> events, int encoding,
            int oldX, int oldY, int newX, int newY) {
        if (encoding == DPAD_HAT) {
            if (oldX != newX) {
                events.add(new Event(EV_ABS, ABS_HAT0X, newX, 0));
            }
            if (oldY != newY) {
                events.add(new Event(EV_ABS, ABS_HAT0Y, newY, 0));
            }
            return;
        }

        Set<Integer> oldKeys = directionKeys(oldX, oldY);
        Set<Integer> newKeys = directionKeys(newX, newY);
        for (Integer code : oldKeys) {
            if (!newKeys.contains(code)) {
                events.add(new Event(EV_KEY, code, 0, 0));
            }
        }
        for (Integer code : newKeys) {
            if (!oldKeys.contains(code)) {
                events.add(new Event(EV_KEY, code, 1, 0));
            }
        }
    }

    private static Set<Integer> directionKeys(int x, int y) {
        Set<Integer> keys = new LinkedHashSet<>();
        if (x < 0) keys.add(BTN_DPAD_LEFT);
        if (x > 0) keys.add(BTN_DPAD_RIGHT);
        if (y < 0) keys.add(BTN_DPAD_UP);
        if (y > 0) keys.add(BTN_DPAD_DOWN);
        return keys;
    }

    private static void appendFrame(List<Event> target, List<Event> transition, int delay) {
        if (transition.isEmpty()) {
            target.add(new Event(EV_SYN, SYN_REPORT, 0, delay));
            return;
        }
        transition.get(0).delayMs = delay;
        target.addAll(transition);
        target.add(new Event(EV_SYN, SYN_REPORT, 0, 0));
    }

    private static void appendHoldMarkers(List<Event> target, int durationMs) {
        int remaining = clampHold(durationMs);
        while (remaining > 0) {
            int chunk = Math.min(MAX_TIMING_MS, remaining);
            appendFrame(target, new ArrayList<>(), chunk);
            remaining -= chunk;
        }
    }

    private static String serialize(List<Event> events) {
        if (events.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("seq:");
        for (Event event : events) {
            if (builder.length() > 4) {
                builder.append(';');
            }
            builder.append(event.type).append(',').append(event.code).append(',')
                    .append(event.value).append(',').append(event.delayMs);
        }
        return builder.toString();
    }

    static String directionLabel(int x, int y) {
        if (x < 0 && y < 0) return "\u2196";
        if (x > 0 && y < 0) return "\u2197";
        if (x < 0 && y > 0) return "\u2199";
        if (x > 0 && y > 0) return "\u2198";
        if (x < 0) return "\u2190";
        if (x > 0) return "\u2192";
        if (y < 0) return "\u2191";
        if (y > 0) return "\u2193";
        return "";
    }

    static String buttonLabel(int code) {
        for (ButtonSpec spec : DIGITAL_BUTTONS) {
            if (spec.code == code) {
                return spec.label;
            }
        }
        return "";
    }

    private static int normalizeDirection(int value) {
        return Integer.compare(value, 0);
    }

    private static String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static final class Event {
        final int type;
        final int code;
        final int value;
        int delayMs;

        Event(int type, int code, int value, int delayMs) {
            this.type = type;
            this.code = code;
            this.value = value;
            this.delayMs = delayMs;
        }
    }

    private static final class ParsedGroup {
        final List<Event> events;
        final int delayMs;

        ParsedGroup(List<Event> events, int delayMs) {
            this.events = events;
            this.delayMs = delayMs;
        }
    }
}
