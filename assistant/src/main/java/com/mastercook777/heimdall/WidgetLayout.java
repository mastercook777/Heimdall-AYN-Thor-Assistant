package com.mastercook777.heimdall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class WidgetLayout {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final String PRESET_DEFAULT = "default";
    public static final String PRESET_FPS = "fps";
    public static final String PRESET_MACRO_FOCUS = "macro_focus";
    public static final String PRESET_CUSTOM = "custom";
    public static final String TYPE_MACRO_GROUP = "macro_group";
    public static final String TYPE_KEYBOARD_PAD = "keyboard_pad";
    public static final String TYPE_TOUCHPAD = "touchpad";
    public static final String TYPE_STATUS = "status";
    public static final String TYPE_CANVAS = "canvas";
    public static final String TYPE_QUICK_ACTIONS = "quick_actions";
    public static final String TYPE_MAGNIFIER = "upper_screen_magnifier";
    public static final String MAGNIFIER_SCALE_FILL = "fill";
    public static final String MAGNIFIER_SCALE_FIT = "fit";
    public static final String MAGNIFIER_SHAPE_RECTANGLE = "rectangle";
    public static final String MAGNIFIER_SHAPE_CIRCLE = "circle";

    private static final int DEFAULT_COLUMNS = 12;
    private static final int DEFAULT_ROWS = 8;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public String preset = PRESET_DEFAULT;
    public int columns = DEFAULT_COLUMNS;
    public int rows = DEFAULT_ROWS;
    public final List<Item> items = new ArrayList<>();

    public WidgetLayout copy() {
        WidgetLayout layout = new WidgetLayout();
        layout.schemaVersion = schemaVersion;
        layout.preset = preset;
        layout.columns = columns;
        layout.rows = rows;
        layout.items.clear();
        for (Item item : items) {
            layout.items.add(item.copy());
        }
        return layout;
    }

    public JSONObject toJson() throws JSONException {
        return toJson(true);
    }

    private JSONObject toJson(boolean includeIdentity) throws JSONException {
        JSONObject object = new JSONObject();
        if (includeIdentity) {
            object.put("schemaVersion", CURRENT_SCHEMA_VERSION);
        }
        object.put("preset", preset);
        object.put("columns", columns);
        object.put("rows", rows);
        JSONArray array = new JSONArray();
        for (Item item : items) {
            array.put(item.toJson(includeIdentity));
        }
        object.put("items", array);
        return object;
    }

    public static WidgetLayout fromJson(JSONObject object) {
        WidgetLayout layout = defaultLayout();
        if (object == null) {
            return layout;
        }
        layout.preset = object.optString("preset", PRESET_DEFAULT);
        layout.columns = clamp(object.optInt("columns", 6), 4, DEFAULT_COLUMNS);
        layout.rows = clamp(object.optInt("rows", DEFAULT_ROWS), 4, 10);
        layout.items.clear();
        JSONArray array = object.optJSONArray("items");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    layout.items.add(Item.fromJson(item));
                }
            }
        }
        layout.sanitize();
        return layout;
    }

    public boolean hasSameContent(WidgetLayout other) {
        if (other == null) {
            return false;
        }
        try {
            return toJson(false).toString().equals(other.toJson(false).toString());
        } catch (JSONException ignored) {
            return false;
        }
    }

    public boolean adoptItemIdsFromEquivalent(WidgetLayout source) {
        if (!hasSameContent(source) || items.size() != source.items.size()) {
            return false;
        }
        for (int i = 0; i < items.size(); i++) {
            items.get(i).itemId = source.items.get(i).itemId;
        }
        schemaVersion = CURRENT_SCHEMA_VERSION;
        return true;
    }

    public static WidgetLayout defaultLayout() {
        WidgetLayout layout = new WidgetLayout();
        layout.items.add(macroItem(6, 5, 6, 3, 0, 3, 3, 1, true));
        layout.items.add(new Item(TYPE_TOUCHPAD, 6, 0, 6, 5));
        layout.items.add(new Item(TYPE_MAGNIFIER, 0, 0, 6, 5));
        layout.items.add(new Item(TYPE_QUICK_ACTIONS, 0, 5, 6, 3));
        return layout;
    }

    public static WidgetLayout fpsLayout() {
        WidgetLayout layout = new WidgetLayout();
        layout.preset = PRESET_FPS;
        layout.items.add(macroItem(6, 6, 6, 2, 0, 2, 2, 1, true));
        layout.items.add(new Item(TYPE_TOUCHPAD, 0, 0, 12, 6));
        layout.items.add(new Item(TYPE_QUICK_ACTIONS, 0, 6, 6, 2));
        return layout;
    }

    public static WidgetLayout macroFocusLayout() {
        WidgetLayout layout = new WidgetLayout();
        layout.preset = PRESET_MACRO_FOCUS;
        layout.items.add(macroItem(0, 4, 4, 4, 0, 2, 1, 2, true));
        layout.items.add(new Item(TYPE_TOUCHPAD, 8, 0, 4, 4));
        layout.items.add(macroItem(8, 4, 4, 4, 2, 2, 1, 2, true));
        layout.items.add(new Item(TYPE_QUICK_ACTIONS, 4, 4, 4, 4));
        layout.items.add(macroItem(0, 0, 8, 4, 4, 4, 2, 2, true));
        return layout;
    }

    public static WidgetLayout customLayout(int macroColumns, int touchpadRows) {
        WidgetLayout layout = new WidgetLayout();
        layout.preset = PRESET_CUSTOM;
        int macroGridColumns = clamp(macroColumns, 1, 4);
        int macroW = macroGridColumns * 2;
        int touchH = clamp(touchpadRows, 4, DEFAULT_ROWS - 1);
        int rightW = DEFAULT_COLUMNS - macroW;
        layout.items.add(macroItem(0, 0, macroW, DEFAULT_ROWS,
                0, 4, macroGridColumns, 0, true));
        layout.items.add(new Item(TYPE_TOUCHPAD, macroW, 0, rightW, touchH));
        layout.items.add(new Item(TYPE_CANVAS, macroW, touchH, rightW, DEFAULT_ROWS - touchH));
        return layout;
    }

    private static Item macroItem(int x, int y, int w, int h, int macroStart, int macroCount,
                                  int macroColumns, int macroRows, boolean rightHandPriority) {
        Item item = new Item(TYPE_MACRO_GROUP, x, y, w, h);
        item.macroStart = macroStart;
        item.macroCount = macroCount;
        item.macroColumns = macroColumns;
        item.macroRows = macroRows;
        item.macroRightHandPriority = rightHandPriority;
        return item;
    }

    public Item findItem(String type) {
        for (Item item : items) {
            if (type.equals(item.type)) {
                return item;
            }
        }
        return null;
    }

    public Item findItemById(String itemId) {
        String target = normalizeItemId(itemId);
        if (target.length() == 0) {
            return null;
        }
        for (Item item : items) {
            if (target.equals(normalizeItemId(item.itemId))) {
                return item;
            }
        }
        return null;
    }

    public void sanitize() {
        schemaVersion = CURRENT_SCHEMA_VERSION;
        migrateToCurrentGrid();
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            if (!isKnownType(item.type)) {
                items.remove(i);
                continue;
            }
            item.x = clamp(item.x, 0, columns - 1);
            item.y = clamp(item.y, 0, rows - 1);
            item.w = clamp(item.w, 1, columns - item.x);
            item.h = clamp(item.h, 1, rows - item.y);
            if (TYPE_MACRO_GROUP.equals(item.type)) {
                item.macroStart = clamp(item.macroStart, 0, 23);
                item.macroCount = clamp(item.macroCount, 1, 24 - item.macroStart);
                item.macroColumns = clamp(item.macroColumns, 1, 4);
                item.macroRows = clamp(item.macroRows, 0, 6);
            }
            if (TYPE_KEYBOARD_PAD.equals(item.type)) {
                item.safeKeyboardPad().sanitize();
            }
            if (TYPE_QUICK_ACTIONS.equals(item.type)) {
                item.safeQuickActions().sanitize();
            }
            if (TYPE_MAGNIFIER.equals(item.type)) {
                item.magnifierLeft = clamp(item.magnifierLeft, 0f, 0.98f);
                item.magnifierTop = clamp(item.magnifierTop, 0f, 0.98f);
                item.magnifierRight = clamp(item.magnifierRight,
                        item.magnifierLeft + 0.02f, 1f);
                item.magnifierBottom = clamp(item.magnifierBottom,
                        item.magnifierTop + 0.02f, 1f);
                item.magnifierAspectRatio = clamp(item.magnifierAspectRatio, 0.2f, 5f);
                item.magnifierFps = normalizeMagnifierFps(item.magnifierFps);
                item.magnifierScaleMode = normalizeMagnifierScaleMode(item.magnifierScaleMode);
                item.magnifierShape = normalizeMagnifierShape(item.magnifierShape);
                item.magnifierZoom = normalizeMagnifierZoom(item.magnifierZoom);
            }
            if (TYPE_CANVAS.equals(item.type)) {
                if (item.canvasConfig == null) {
                    item.canvasConfig = new CanvasConfig();
                }
                item.canvasConfig.normalize();
            }
        }
        Set<String> usedItemIds = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            item.itemId = uniqueItemId(item.itemId, i, usedItemIds);
        }
    }

    private void migrateToCurrentGrid() {
        int sourceColumns = clamp(columns, 4, DEFAULT_COLUMNS);
        int sourceRows = clamp(rows, 4, 10);
        if (sourceColumns == DEFAULT_COLUMNS && sourceRows == DEFAULT_ROWS) {
            columns = DEFAULT_COLUMNS;
            rows = DEFAULT_ROWS;
            return;
        }
        for (Item item : items) {
            int right = scaleBoundary(item.x + item.w, sourceColumns, DEFAULT_COLUMNS);
            int bottom = scaleBoundary(item.y + item.h, sourceRows, DEFAULT_ROWS);
            item.x = scaleBoundary(item.x, sourceColumns, DEFAULT_COLUMNS);
            item.y = scaleBoundary(item.y, sourceRows, DEFAULT_ROWS);
            item.w = Math.max(1, right - item.x);
            item.h = Math.max(1, bottom - item.y);
        }
        columns = DEFAULT_COLUMNS;
        rows = DEFAULT_ROWS;
    }

    private static int scaleBoundary(int value, int sourceSize, int targetSize) {
        return Math.round(value * targetSize / (float) sourceSize);
    }

    private static String uniqueItemId(String value, int index, Set<String> usedItemIds) {
        String candidate = normalizeItemId(value);
        if (candidate.length() > 0 && usedItemIds.add(candidate)) {
            return candidate;
        }
        candidate = "legacy-item-" + index;
        int suffix = 2;
        while (!usedItemIds.add(candidate)) {
            candidate = "legacy-item-" + index + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private static String normalizeItemId(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isKnownType(String type) {
        return TYPE_MACRO_GROUP.equals(type)
                || TYPE_KEYBOARD_PAD.equals(type)
                || TYPE_TOUCHPAD.equals(type)
                || TYPE_STATUS.equals(type)
                || TYPE_CANVAS.equals(type)
                || TYPE_QUICK_ACTIONS.equals(type)
                || TYPE_MAGNIFIER.equals(type);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int normalizeMagnifierFps(int value) {
        if (value >= 45) {
            return 60;
        }
        if (value >= 23) {
            return 30;
        }
        return 15;
    }

    public static String normalizeMagnifierScaleMode(String value) {
        return MAGNIFIER_SCALE_FIT.equals(value) ? MAGNIFIER_SCALE_FIT : MAGNIFIER_SCALE_FILL;
    }

    public static String normalizeMagnifierShape(String value) {
        return MAGNIFIER_SHAPE_CIRCLE.equals(value)
                ? MAGNIFIER_SHAPE_CIRCLE : MAGNIFIER_SHAPE_RECTANGLE;
    }

    public static boolean isCircularMagnifier(Item item) {
        return item != null
                && MAGNIFIER_SHAPE_CIRCLE.equals(normalizeMagnifierShape(item.magnifierShape));
    }

    public static float magnifierTargetAspectRatio(Item item) {
        if (isCircularMagnifier(item)) {
            return 1f;
        }
        return item == null ? 1f : clamp(item.magnifierAspectRatio, 0.2f, 5f);
    }

    public static float normalizeMagnifierZoom(float value) {
        if (value >= 1.75f) {
            return 2f;
        }
        if (value >= 1.35f) {
            return 1.5f;
        }
        if (value >= 1.1f) {
            return 1.2f;
        }
        return 1f;
    }

    public static final class Item {
        public String itemId;
        public String type;
        public int x;
        public int y;
        public int w;
        public int h;
        public int macroStart = 0;
        public int macroCount = 4;
        public int macroColumns = 2;
        public int macroRows = 0;
        public boolean macroRightHandPriority = true;
        public boolean macroIconOnly = false;
        public boolean hasMacroConfig = true;
        public KeyboardPad keyboardPad = KeyboardPad.defaultPad();
        public QuickActionsConfig quickActions = QuickActionsConfig.newModuleDefault();
        public float magnifierLeft = 0.25f;
        public float magnifierTop = 0.25f;
        public float magnifierRight = 0.75f;
        public float magnifierBottom = 0.75f;
        public float magnifierAspectRatio = 1f;
        public int magnifierFps = 30;
        public String magnifierScaleMode = MAGNIFIER_SCALE_FILL;
        public String magnifierShape = MAGNIFIER_SHAPE_RECTANGLE;
        public float magnifierZoom = 1f;
        public CanvasConfig canvasConfig = new CanvasConfig();

        public Item(String type, int x, int y, int w, int h) {
            this.itemId = UUID.randomUUID().toString();
            this.type = type;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public Item copy() {
            Item item = new Item(type, x, y, w, h);
            item.itemId = itemId;
            item.macroStart = macroStart;
            item.macroCount = macroCount;
            item.macroColumns = macroColumns;
            item.macroRows = macroRows;
            item.macroRightHandPriority = macroRightHandPriority;
            item.macroIconOnly = macroIconOnly;
            item.hasMacroConfig = hasMacroConfig;
            item.keyboardPad = safeKeyboardPad().copy();
            item.quickActions = safeQuickActions().copy();
            item.magnifierLeft = magnifierLeft;
            item.magnifierTop = magnifierTop;
            item.magnifierRight = magnifierRight;
            item.magnifierBottom = magnifierBottom;
            item.magnifierAspectRatio = magnifierAspectRatio;
            item.magnifierFps = magnifierFps;
            item.magnifierScaleMode = magnifierScaleMode;
            item.magnifierShape = magnifierShape;
            item.magnifierZoom = magnifierZoom;
            item.canvasConfig = canvasConfig == null
                    ? new CanvasConfig() : canvasConfig.copy();
            return item;
        }

        public JSONObject toJson() throws JSONException {
            return toJson(true);
        }

        private JSONObject toJson(boolean includeIdentity) throws JSONException {
            JSONObject object = new JSONObject();
            if (includeIdentity) {
                object.put("itemId", itemId);
            }
            object.put("type", type);
            object.put("x", x);
            object.put("y", y);
            object.put("w", w);
            object.put("h", h);
            if (TYPE_MACRO_GROUP.equals(type)) {
                object.put("macroStart", macroStart);
                object.put("macroCount", macroCount);
                object.put("macroColumns", macroColumns);
                object.put("macroRows", macroRows);
                object.put("macroRightHandPriority", macroRightHandPriority);
                object.put("macroIconOnly", macroIconOnly);
            }
            if (TYPE_KEYBOARD_PAD.equals(type)) {
                object.put("keyboardPad", safeKeyboardPad().toJson());
            }
            if (TYPE_QUICK_ACTIONS.equals(type)) {
                object.put("quickActions", safeQuickActions().toJson());
            }
            if (TYPE_MAGNIFIER.equals(type)) {
                object.put("magnifierLeft", magnifierLeft);
                object.put("magnifierTop", magnifierTop);
                object.put("magnifierRight", magnifierRight);
                object.put("magnifierBottom", magnifierBottom);
                object.put("magnifierAspectRatio", magnifierAspectRatio);
                object.put("magnifierFps", magnifierFps);
                object.put("magnifierScaleMode", magnifierScaleMode);
                object.put("magnifierShape", normalizeMagnifierShape(magnifierShape));
                object.put("magnifierZoom", magnifierZoom);
            }
            if (TYPE_CANVAS.equals(type)) {
                object.put("canvas", (canvasConfig == null
                        ? new CanvasConfig() : canvasConfig).toJson());
            }
            return object;
        }

        public static Item fromJson(JSONObject object) {
            Item item = new Item(
                    object.optString("type", TYPE_STATUS),
                    object.optInt("x", 0),
                    object.optInt("y", 0),
                    object.optInt("w", 1),
                    object.optInt("h", 1));
            item.itemId = normalizeItemId(object.optString("itemId", ""));
            item.macroStart = object.optInt("macroStart", 0);
            item.macroCount = object.optInt("macroCount", 4);
            item.macroColumns = object.optInt("macroColumns", 2);
            item.macroRows = object.optInt("macroRows", 0);
            item.macroRightHandPriority = object.optBoolean("macroRightHandPriority", true);
            item.macroIconOnly = object.optBoolean("macroIconOnly", false);
            item.hasMacroConfig = object.has("macroStart")
                    || object.has("macroCount")
                    || object.has("macroColumns")
                    || object.has("macroRows")
                    || object.has("macroRightHandPriority")
                    || object.has("macroIconOnly");
            item.keyboardPad = KeyboardPad.fromJson(object.optJSONObject("keyboardPad"));
            item.quickActions = QuickActionsConfig.fromJson(object.optJSONObject("quickActions"));
            item.magnifierLeft = (float) object.optDouble("magnifierLeft", 0.25d);
            item.magnifierTop = (float) object.optDouble("magnifierTop", 0.25d);
            item.magnifierRight = (float) object.optDouble("magnifierRight", 0.75d);
            item.magnifierBottom = (float) object.optDouble("magnifierBottom", 0.75d);
            item.magnifierAspectRatio = (float) object.optDouble("magnifierAspectRatio", 1d);
            item.magnifierFps = object.optInt("magnifierFps", 30);
            item.magnifierScaleMode = object.optString(
                    "magnifierScaleMode", MAGNIFIER_SCALE_FILL);
            item.magnifierShape = object.optString(
                    "magnifierShape", MAGNIFIER_SHAPE_RECTANGLE);
            item.magnifierZoom = (float) object.optDouble("magnifierZoom", 1d);
            item.canvasConfig = CanvasConfig.fromJson(object.optJSONObject("canvas"));
            return item;
        }

        public KeyboardPad safeKeyboardPad() {
            if (keyboardPad == null) {
                keyboardPad = KeyboardPad.defaultPad();
            }
            keyboardPad.sanitize();
            return keyboardPad;
        }

        public QuickActionsConfig safeQuickActions() {
            if (quickActions == null) {
                quickActions = QuickActionsConfig.legacyDefault();
            }
            quickActions.sanitize();
            return quickActions;
        }
    }
}
