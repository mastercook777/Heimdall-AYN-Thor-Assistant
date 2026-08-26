package com.mastercook777.heimdall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Closed catalog of in-app actions exposed by configurable Heimdall controls. */
final class HeimdallActionCatalog {
    static final String ACTION_NONE = "none";
    static final String ACTION_SCREENSHOT = "screenshot";
    static final String ACTION_SCREEN_RECORDING = "screen_recording";
    static final String ACTION_OPEN_VIRTUAL_KEYBOARD = "open_virtual_keyboard";

    static final class Entry {
        final String id;
        final int labelRes;
        final int iconRes;

        Entry(String id, int labelRes, int iconRes) {
            this.id = id;
            this.labelRes = labelRes;
            this.iconRes = iconRes;
        }
    }

    private static final List<Entry> QUICK_ACTION_ENTRIES = createQuickActionEntries();

    static List<Entry> quickActionEntries() {
        return QUICK_ACTION_ENTRIES;
    }

    static String normalizeQuickAction(String value) {
        if (ACTION_SCREENSHOT.equals(value)
                || ACTION_SCREEN_RECORDING.equals(value)
                || ACTION_OPEN_VIRTUAL_KEYBOARD.equals(value)) {
            return value;
        }
        return ACTION_NONE;
    }

    static Entry findQuickAction(String id) {
        String normalized = normalizeQuickAction(id);
        for (Entry entry : QUICK_ACTION_ENTRIES) {
            if (entry.id.equals(normalized)) {
                return entry;
            }
        }
        return QUICK_ACTION_ENTRIES.get(0);
    }

    private static List<Entry> createQuickActionEntries() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(ACTION_NONE, R.string.quick_action_none,
                R.drawable.ic_arrow_back));
        entries.add(new Entry(ACTION_SCREENSHOT, R.string.quick_action_screenshot,
                R.drawable.ic_camera));
        entries.add(new Entry(ACTION_SCREEN_RECORDING, R.string.quick_action_recording,
                R.drawable.ic_video));
        entries.add(new Entry(ACTION_OPEN_VIRTUAL_KEYBOARD,
                R.string.quick_action_open_virtual_keyboard,
                R.drawable.ic_keyboard_full));
        return Collections.unmodifiableList(entries);
    }

    private HeimdallActionCatalog() {
    }
}
