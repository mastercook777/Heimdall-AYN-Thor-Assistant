package com.mastercook777.heimdall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Profile-owned configuration for one Keyboard Pad module. */
public final class KeyboardPad {
    public static final String BEHAVIOR_PRESS = "press";
    public static final String BEHAVIOR_WHILE_HELD = "while_held";
    public static final String LAYOUT_HORIZONTAL = "horizontal";
    public static final String LAYOUT_VERTICAL = "vertical";
    public static final String ACTION_KEYBOARD_BINDING = "keyboard_binding";
    public static final String ACTION_HEIMDALL = "heimdall_action";

    public static final int MIN_KEY_COUNT = 1;
    public static final int MAX_KEY_COUNT = 12;

    private static final int DEFAULT_COLUMNS = 2;
    private static final int DEFAULT_ROWS = 2;

    public int columns = DEFAULT_COLUMNS;
    public int rows = DEFAULT_ROWS;
    public String layoutMode = LAYOUT_HORIZONTAL;
    public final List<Key> keys = new ArrayList<>();

    public KeyboardPad copy() {
        KeyboardPad pad = new KeyboardPad();
        pad.columns = columns;
        pad.rows = rows;
        pad.layoutMode = normalizeLayoutMode(layoutMode);
        for (Key key : keys) {
            pad.keys.add(key.copy());
        }
        pad.sanitize();
        return pad;
    }

    public JSONObject toJson() throws JSONException {
        sanitize();
        JSONObject object = new JSONObject();
        object.put("columns", columns);
        object.put("rows", rows);
        object.put("layoutMode", normalizeLayoutMode(layoutMode));
        JSONArray array = new JSONArray();
        for (Key key : keys) {
            array.put(key.toJson());
        }
        object.put("keys", array);
        return object;
    }

    public static KeyboardPad fromJson(JSONObject object) {
        if (object == null) {
            return defaultPad();
        }
        KeyboardPad pad = new KeyboardPad();
        pad.columns = object.optInt("columns", DEFAULT_COLUMNS);
        pad.rows = object.optInt("rows", DEFAULT_ROWS);
        pad.layoutMode = normalizeLayoutMode(
                object.optString("layoutMode", LAYOUT_HORIZONTAL));
        JSONArray array = object.optJSONArray("keys");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject key = array.optJSONObject(i);
                if (key != null) {
                    pad.keys.add(Key.fromJson(key));
                }
            }
        }
        if (pad.keys.isEmpty()) {
            return defaultPad();
        }
        pad.sanitize();
        if (pad.isPrototypeWasdDefault()) {
            return defaultPad();
        }
        return pad;
    }

    public static KeyboardPad defaultPad() {
        KeyboardPad pad = new KeyboardPad();
        pad.resizeKeyCount(4);
        return pad;
    }

    /** Rebuilds a small physical-keypad grid after an explicit key-count change. */
    public void resizeKeyCount(int requestedCount) {
        int count = clamp(requestedCount, MIN_KEY_COUNT, MAX_KEY_COUNT);
        while (keys.size() > count) {
            keys.remove(keys.size() - 1);
        }
        while (keys.size() < count) {
            keys.add(defaultShortcutKey(keys.size()));
        }
        applyCompactLayout();
    }

    /** Keeps configured bindings/display while packing keys into a compact keypad footprint. */
    public void applyCompactLayout() {
        int count = clamp(keys.size(), MIN_KEY_COUNT, MAX_KEY_COUNT);
        layoutMode = normalizeLayoutMode(layoutMode);
        if (LAYOUT_VERTICAL.equals(layoutMode)) {
            columns = 1;
            rows = count;
        } else if (count <= 3) {
            columns = count;
            rows = 1;
        } else if (count == 4) {
            columns = 2;
            rows = 2;
        } else if (count <= 6) {
            columns = 3;
            rows = 2;
        } else if (count <= 9) {
            columns = 3;
            rows = 3;
        } else {
            columns = 4;
            rows = 3;
        }
        for (int index = 0; index < keys.size(); index++) {
            Geometry geometry = keys.get(index).geometry;
            geometry.x = index % columns;
            geometry.y = index / columns;
            geometry.w = 1;
            geometry.h = 1;
        }
        sanitize();
    }

    /** Explicit editor action: change packing direction and repack the current keys. */
    public void setLayoutMode(String requestedMode) {
        String normalized = normalizeLayoutMode(requestedMode);
        if (normalized.equals(layoutMode)) {
            return;
        }
        layoutMode = normalized;
        applyCompactLayout();
    }

    public void sanitize() {
        layoutMode = normalizeLayoutMode(layoutMode);
        columns = clamp(columns, 1, 8);
        rows = clamp(rows, 1, MAX_KEY_COUNT);
        for (Key key : keys) {
            key.sanitize(columns, rows);
        }
    }

    private boolean isPrototypeWasdDefault() {
        if (columns != 4 || rows != 3 || keys.size() != 6) {
            return false;
        }
        int[] codes = {KeyboardKeyCatalog.KEY_W, KeyboardKeyCatalog.KEY_A,
                KeyboardKeyCatalog.KEY_S, KeyboardKeyCatalog.KEY_D,
                KeyboardKeyCatalog.KEY_LEFTSHIFT, KeyboardKeyCatalog.KEY_SPACE};
        int[][] geometry = {{1, 0, 1, 1}, {0, 1, 1, 1}, {1, 1, 1, 1},
                {2, 1, 1, 1}, {0, 2, 2, 1}, {2, 2, 2, 1}};
        for (int index = 0; index < keys.size(); index++) {
            Key key = keys.get(index);
            Geometry keyGeometry = key.geometry;
            if (key.binding.linuxKeyCode != codes[index]
                    || !ACTION_KEYBOARD_BINDING.equals(normalizeActionType(key.actionType))
                    || key.binding.ctrl || key.binding.shift
                    || key.binding.alt || key.binding.win
                    || !key.display.isEmpty()
                    || !BEHAVIOR_WHILE_HELD.equals(normalizeBehavior(key.behavior))
                    || keyGeometry.x != geometry[index][0]
                    || keyGeometry.y != geometry[index][1]
                    || keyGeometry.w != geometry[index][2]
                    || keyGeometry.h != geometry[index][3]) {
                return false;
            }
        }
        return true;
    }

    private static Key defaultShortcutKey(int index) {
        Key key = new Key();
        int shortcutIndex = Math.min(Math.max(index, 0), MAX_KEY_COUNT - 1);
        key.binding.linuxKeyCode = shortcutIndex < 10
                ? KeyboardKeyCatalog.KEY_F1 + shortcutIndex
                : 87 + shortcutIndex - 10;
        key.behavior = BEHAVIOR_WHILE_HELD;
        return key;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Key {
        public Binding binding = new Binding();
        public Display display = new Display();
        public Geometry geometry = new Geometry();
        public String behavior = BEHAVIOR_WHILE_HELD;
        public String actionType = ACTION_KEYBOARD_BINDING;
        public String heimdallAction = "";

        public Key copy() {
            Key key = new Key();
            key.binding = binding.copy();
            key.display = display.copy();
            key.geometry = geometry.copy();
            key.behavior = normalizeBehavior(behavior);
            key.actionType = normalizeActionType(actionType);
            key.heimdallAction = normalizeHeimdallAction(heimdallAction);
            return key;
        }

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("binding", binding.toJson());
            object.put("display", display.toJson());
            object.put("geometry", geometry.toJson());
            object.put("behavior", normalizeBehavior(behavior));
            object.put("actionType", normalizeActionType(actionType));
            if (ACTION_HEIMDALL.equals(normalizeActionType(actionType))) {
                object.put("heimdallAction", normalizeHeimdallAction(heimdallAction));
            }
            return object;
        }

        static Key fromJson(JSONObject object) {
            Key key = new Key();
            key.binding = Binding.fromJson(object.optJSONObject("binding"));
            key.display = Display.fromJson(object.optJSONObject("display"));
            key.geometry = Geometry.fromJson(object.optJSONObject("geometry"));
            key.behavior = normalizeBehavior(
                    object.optString("behavior", BEHAVIOR_WHILE_HELD));
            key.actionType = normalizeActionType(
                    object.optString("actionType", ACTION_KEYBOARD_BINDING));
            key.heimdallAction = normalizeHeimdallAction(
                    object.optString("heimdallAction", ""));
            if (ACTION_HEIMDALL.equals(key.actionType) && key.heimdallAction.isEmpty()) {
                key.actionType = ACTION_KEYBOARD_BINDING;
            }
            return key;
        }

        void sanitize(int columns, int rows) {
            binding.sanitize();
            display.sanitize();
            geometry.x = clamp(geometry.x, 0, columns - 1);
            geometry.y = clamp(geometry.y, 0, rows - 1);
            geometry.w = clamp(geometry.w, 1, columns - geometry.x);
            geometry.h = clamp(geometry.h, 1, rows - geometry.y);
            behavior = normalizeBehavior(behavior);
            actionType = normalizeActionType(actionType);
            heimdallAction = normalizeHeimdallAction(heimdallAction);
            if (ACTION_HEIMDALL.equals(actionType) && heimdallAction.isEmpty()) {
                actionType = ACTION_KEYBOARD_BINDING;
            }
        }

        public boolean isWhileHeld() {
            return BEHAVIOR_WHILE_HELD.equals(normalizeBehavior(behavior));
        }

        public boolean isHeimdallAction() {
            return ACTION_HEIMDALL.equals(normalizeActionType(actionType))
                    && !normalizeHeimdallAction(heimdallAction).isEmpty();
        }
    }

    public static final class Binding {
        public int linuxKeyCode = KeyboardKeyCatalog.KEY_A;
        public boolean ctrl;
        public boolean shift;
        public boolean alt;
        public boolean win;

        public Binding copy() {
            Binding binding = new Binding();
            binding.linuxKeyCode = linuxKeyCode;
            binding.ctrl = ctrl;
            binding.shift = shift;
            binding.alt = alt;
            binding.win = win;
            return binding;
        }

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("linuxKeyCode", linuxKeyCode);
            object.put("ctrl", ctrl);
            object.put("shift", shift);
            object.put("alt", alt);
            object.put("win", win);
            return object;
        }

        static Binding fromJson(JSONObject object) {
            Binding binding = new Binding();
            if (object == null) {
                return binding;
            }
            binding.linuxKeyCode = object.optInt(
                    "linuxKeyCode", KeyboardKeyCatalog.KEY_A);
            binding.ctrl = object.optBoolean("ctrl", false);
            binding.shift = object.optBoolean("shift", false);
            binding.alt = object.optBoolean("alt", false);
            binding.win = object.optBoolean("win", false);
            binding.sanitize();
            return binding;
        }

        void sanitize() {
            if (!VirtualKeyboardDispatcher.isSupportedKeyCode(linuxKeyCode)) {
                linuxKeyCode = KeyboardKeyCatalog.KEY_A;
            }
        }
    }

    public static final class Display {
        public String label = "";
        public String iconKey = "";

        public Display copy() {
            Display display = new Display();
            display.label = label;
            display.iconKey = iconKey;
            return display;
        }

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            if (!label.isEmpty()) {
                object.put("label", label);
            }
            if (!iconKey.isEmpty()) {
                object.put("iconKey", iconKey);
            }
            return object;
        }

        static Display fromJson(JSONObject object) {
            Display display = new Display();
            if (object != null) {
                display.label = object.optString("label", "");
                display.iconKey = object.optString("iconKey", "");
            }
            display.sanitize();
            return display;
        }

        void sanitize() {
            label = label == null ? "" : label.trim();
            iconKey = iconKey == null ? "" : iconKey.trim();
        }

        public boolean isEmpty() {
            return label.isEmpty() && iconKey.isEmpty();
        }
    }

    public static final class Geometry {
        public int x;
        public int y;
        public int w = 1;
        public int h = 1;

        public Geometry copy() {
            Geometry geometry = new Geometry();
            geometry.x = x;
            geometry.y = y;
            geometry.w = w;
            geometry.h = h;
            return geometry;
        }

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("x", x);
            object.put("y", y);
            object.put("w", w);
            object.put("h", h);
            return object;
        }

        static Geometry fromJson(JSONObject object) {
            Geometry geometry = new Geometry();
            if (object != null) {
                geometry.x = object.optInt("x", 0);
                geometry.y = object.optInt("y", 0);
                geometry.w = object.optInt("w", 1);
                geometry.h = object.optInt("h", 1);
            }
            return geometry;
        }
    }

    public static String normalizeBehavior(String value) {
        return BEHAVIOR_PRESS.equals(value) ? BEHAVIOR_PRESS : BEHAVIOR_WHILE_HELD;
    }

    public static String normalizeActionType(String value) {
        return ACTION_HEIMDALL.equals(value) ? ACTION_HEIMDALL : ACTION_KEYBOARD_BINDING;
    }

    public static String normalizeHeimdallAction(String value) {
        return HeimdallActionCatalog.ACTION_OPEN_VIRTUAL_KEYBOARD.equals(value)
                ? HeimdallActionCatalog.ACTION_OPEN_VIRTUAL_KEYBOARD : "";
    }

    public static String normalizeLayoutMode(String value) {
        return LAYOUT_VERTICAL.equals(value) ? LAYOUT_VERTICAL : LAYOUT_HORIZONTAL;
    }
}
