package com.mastercook777.heimdall;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only, user-facing interpretation of the native controller sequence payload.
 * The stored seq: value remains the source of truth for replay.
 */
public final class GamepadSequenceSummary {
    private static final int EV_KEY = 1;
    private static final int EV_ABS = 3;
    private static final int MAX_CAPTURED_EVENTS = 1024;

    private static final int ABS_X = 0;
    private static final int ABS_Y = 1;
    private static final int ABS_Z = 2;
    private static final int ABS_RZ = 5;
    private static final int ABS_GAS = 9;
    private static final int ABS_BRAKE = 10;
    private static final int ABS_HAT0X = 16;
    private static final int ABS_HAT0Y = 17;

    public final boolean valid;
    public final String title;
    public final String subtitle;
    public final int eventCount;
    public final long replayDurationMs;
    public final boolean containsAnalog;
    public final boolean hitEventLimit;

    private GamepadSequenceSummary(boolean valid, String title, String subtitle,
            int eventCount, long replayDurationMs, boolean containsAnalog,
            boolean hitEventLimit) {
        this.valid = valid;
        this.title = title;
        this.subtitle = subtitle;
        this.eventCount = eventCount;
        this.replayDurationMs = replayDurationMs;
        this.containsAnalog = containsAnalog;
        this.hitEventLimit = hitEventLimit;
    }

    public static GamepadSequenceSummary summarize(Context context, String sequence,
            int rightStickAxisX, int rightStickAxisY) {
        String raw = sequence == null ? "" : sequence.trim();
        if (!raw.startsWith("seq:") || raw.length() <= 4) {
            return empty(context);
        }

        String[] tokens = raw.substring(4).split(";");
        List<List<String>> groups = new ArrayList<>();
        Map<Integer, String> activeButtons = new HashMap<>();
        Set<String> seenAnalogInputs = new LinkedHashSet<>();
        List<String> activeGroup = null;
        long replayDurationMs = 0L;
        int eventCount = 0;
        boolean containsAnalog = false;

        for (String token : tokens) {
            int[] item = parseItem(token);
            if (item == null) {
                continue;
            }
            int type = item[0];
            int code = item[1];
            int value = item[2];
            int delayMs = item[3];
            eventCount++;
            replayDurationMs += Math.max(0, delayMs);

            if (type == EV_KEY) {
                String label = keyLabel(context, code);
                if (value == 1) {
                    if (activeGroup == null || activeButtons.isEmpty()) {
                        activeGroup = new ArrayList<>();
                        groups.add(activeGroup);
                    }
                    addOnce(activeGroup, label);
                    activeButtons.put(code, label);
                } else if (value == 0) {
                    activeButtons.remove(code);
                    if (activeButtons.isEmpty()) {
                        activeGroup = null;
                    }
                }
                continue;
            }

            if (type != EV_ABS) {
                continue;
            }

            String dpad = dpadLabel(code, value);
            if (dpad != null) {
                activeGroup = appendAction(groups, activeGroup, activeButtons, dpad);
                continue;
            }

            String analog = analogLabel(context, code, rightStickAxisX, rightStickAxisY);
            if (analog == null || value == 0 || !seenAnalogInputs.add(analog)) {
                continue;
            }
            containsAnalog = true;
            activeGroup = appendAction(groups, activeGroup, activeButtons, analog);
        }

        if (eventCount == 0) {
            return empty(context);
        }

        String title = renderGroups(context, groups);
        if (title.length() == 0) {
            title = context.getString(R.string.gamepad_summary_generic);
        }
        String kind = sequenceKind(context, groups, containsAnalog);
        String duration = replayDurationLabel(context, replayDurationMs);
        String events = context.getResources().getQuantityString(
                R.plurals.gamepad_summary_event_count, eventCount, eventCount);
        boolean hitLimit = eventCount >= MAX_CAPTURED_EVENTS;
        String subtitle = context.getString(hitLimit
                ? R.string.gamepad_summary_subtitle_many
                : R.string.gamepad_summary_subtitle, kind, duration, events);
        return new GamepadSequenceSummary(true, title, subtitle, eventCount,
                replayDurationMs, containsAnalog, hitLimit);
    }

    private static GamepadSequenceSummary empty(Context context) {
        return new GamepadSequenceSummary(false,
                context.getString(R.string.gamepad_summary_empty), "", 0, 0L,
                false, false);
    }

    private static int[] parseItem(String token) {
        String[] parts = token == null ? new String[0] : token.trim().split(",");
        if (parts.length != 4) {
            return null;
        }
        int[] item = new int[4];
        try {
            for (int i = 0; i < item.length; i++) {
                item[i] = Integer.parseInt(parts[i].trim());
            }
            return item;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static List<String> appendAction(List<List<String>> groups, List<String> activeGroup,
            Map<Integer, String> activeButtons, String label) {
        if (activeGroup == null || activeButtons.isEmpty()) {
            activeGroup = new ArrayList<>();
            groups.add(activeGroup);
        }
        addOnce(activeGroup, label);
        return activeButtons.isEmpty() ? null : activeGroup;
    }

    private static void addOnce(List<String> group, String label) {
        if (label != null && label.length() > 0 && !group.contains(label)) {
            group.add(label);
        }
    }

    private static String renderGroups(Context context, List<List<String>> groups) {
        List<String> rendered = new ArrayList<>();
        for (List<String> group : groups) {
            if (group.isEmpty()) {
                continue;
            }
            rendered.add(join(group, " + "));
        }
        if (rendered.isEmpty()) {
            return "";
        }

        List<String> compressed = new ArrayList<>();
        int index = 0;
        while (index < rendered.size()) {
            String label = rendered.get(index);
            int count = 1;
            while (index + count < rendered.size()
                    && label.equals(rendered.get(index + count))) {
                count++;
            }
            compressed.add(count > 1 ? label + " \u00d7 " + count : label);
            index += count;
        }
        String sequence = join(compressed, " \u00b7 ");
        return compressed.size() > 1
                ? context.getString(R.string.gamepad_summary_sequence, sequence) : sequence;
    }

    private static String sequenceKind(Context context, List<List<String>> groups,
            boolean containsAnalog) {
        int nonEmptyGroups = 0;
        int firstGroupSize = 0;
        for (List<String> group : groups) {
            if (!group.isEmpty()) {
                nonEmptyGroups++;
                if (firstGroupSize == 0) {
                    firstGroupSize = group.size();
                }
            }
        }
        if (nonEmptyGroups > 1) {
            return context.getString(R.string.gamepad_summary_kind_sequence);
        }
        if (firstGroupSize > 1) {
            return context.getString(R.string.gamepad_summary_kind_chord);
        }
        if (containsAnalog) {
            return context.getString(R.string.gamepad_summary_kind_analog);
        }
        return context.getString(R.string.gamepad_summary_kind_single);
    }

    private static String replayDurationLabel(Context context, long durationMs) {
        if (durationMs < 100L) {
            return context.getString(R.string.gamepad_summary_duration_short);
        }
        if (durationMs < 1000L) {
            return context.getString(R.string.gamepad_summary_duration_ms, durationMs);
        }
        return context.getString(R.string.gamepad_summary_duration_seconds,
                durationMs / 1000f);
    }

    private static String keyLabel(Context context, int code) {
        switch (code) {
            case 304: return "A";
            case 305: return "B";
            case 307: return "X";
            case 308: return "Y";
            case 310: return "L1";
            case 311: return "R1";
            case 312: return "L2";
            case 313: return "R2";
            case 314: return "SELECT";
            case 315: return "START";
            case 316: return "HOME";
            case 317: return "L3";
            case 318: return "R3";
            case 544: return "\u2191";
            case 545: return "\u2193";
            case 546: return "\u2190";
            case 547: return "\u2192";
            default: return context.getString(R.string.gamepad_summary_button_unknown, code);
        }
    }

    private static String dpadLabel(int code, int value) {
        if (value == 0) {
            return null;
        }
        if (code == ABS_HAT0X) {
            return value < 0 ? "\u2190" : "\u2192";
        }
        if (code == ABS_HAT0Y) {
            return value < 0 ? "\u2191" : "\u2193";
        }
        return null;
    }

    private static String analogLabel(Context context, int code,
            int rightStickAxisX, int rightStickAxisY) {
        if (code == rightStickAxisX || code == rightStickAxisY) {
            return context.getString(R.string.gamepad_summary_right_stick);
        }
        if (code == ABS_X || code == ABS_Y) {
            return context.getString(R.string.gamepad_summary_left_stick);
        }
        if (code == ABS_Z && code != rightStickAxisX && code != rightStickAxisY) {
            return rightStickAxisX >= 0 && rightStickAxisY >= 0 ? "L2"
                    : context.getString(R.string.gamepad_summary_analog_input);
        }
        if (code == ABS_RZ && code != rightStickAxisX && code != rightStickAxisY) {
            return rightStickAxisX >= 0 && rightStickAxisY >= 0 ? "R2"
                    : context.getString(R.string.gamepad_summary_analog_input);
        }
        if (code == ABS_GAS) {
            return "R2";
        }
        if (code == ABS_BRAKE) {
            return "L2";
        }
        return context.getString(R.string.gamepad_summary_analog_input);
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
}
