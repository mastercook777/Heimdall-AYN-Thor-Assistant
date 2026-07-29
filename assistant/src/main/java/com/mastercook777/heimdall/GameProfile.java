package com.mastercook777.heimdall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class GameProfile {
    public String name;
    public String mode;
    public String packageHint;
    public String romContextHint = "";
    public boolean defaultForPackage;
    public String iconUri = "";
    public int macroCount;
    public int macroColumns = 4;
    public int macroRows = 0;
    public boolean rightHandPriority = true;
    public String mapTitle = "";
    public String mapUri = "";
    public String interactiveMapTitle = "";
    public String interactiveMapUrl = "";
    public TouchpadSettings touchpadSettings = new TouchpadSettings();
    public WidgetLayout widgetLayout = WidgetLayout.defaultLayout();
    public final List<Macro> macros = new ArrayList<>();
    public final List<GuideEntry> guides = new ArrayList<>();
    public final List<MapEntry> maps = new ArrayList<>();
    public final List<MapMarker> mapMarkers = new ArrayList<>();

    public GameProfile(String name, String mode, String packageHint, List<Macro> macros) {
        this.name = name;
        this.mode = mode;
        this.packageHint = packageHint;
        this.macros.addAll(macros);
        this.macroCount = clampMacroCount(macros.size());
        normalizeLayout();
    }

    public GameProfile(String name, String mode, String packageHint, int macroCount, List<Macro> macros) {
        this.name = name;
        this.mode = mode;
        this.packageHint = packageHint;
        this.macros.addAll(macros);
        this.macroCount = clampMacroCount(macroCount);
        normalizeLayout();
    }

    public JSONObject toJson() throws JSONException {
        ensureMapEntries();
        syncLegacyMapFields();
        JSONObject object = new JSONObject();
        object.put("name", name);
        object.put("mode", mode);
        object.put("packageHint", packageHint);
        object.put("romContextHint", romContextHint);
        object.put("defaultForPackage", defaultForPackage);
        object.put("iconUri", iconUri);
        object.put("macroCount", macroCount);
        object.put("macroColumns", macroColumns);
        object.put("macroRows", macroRows);
        object.put("rightHandPriority", rightHandPriority);
        object.put("mapTitle", mapTitle);
        object.put("mapUri", mapUri);
        object.put("interactiveMapTitle", interactiveMapTitle);
        object.put("interactiveMapUrl", interactiveMapUrl);
        object.put("touchpadSettings", safeTouchpadSettings().toJson());
        object.put("widgetLayout", safeWidgetLayout().toJson());
        JSONArray macroArray = new JSONArray();
        for (Macro macro : macros) {
            macroArray.put(macro.toJson());
        }
        object.put("macros", macroArray);
        JSONArray guideArray = new JSONArray();
        for (GuideEntry guide : guides) {
            guideArray.put(guide.toJson());
        }
        object.put("guides", guideArray);
        JSONArray mapArray = new JSONArray();
        for (MapEntry map : maps) {
            mapArray.put(map.toJson());
        }
        object.put("maps", mapArray);
        JSONArray markerArray = new JSONArray();
        for (MapMarker marker : mapMarkers) {
            markerArray.put(marker.toJson());
        }
        object.put("mapMarkers", markerArray);
        return object;
    }

    public static GameProfile fromJson(JSONObject object) {
        return fromJson(object, null);
    }

    public static GameProfile fromJson(JSONObject object, TouchpadSettings fallbackTouchpadSettings) {
        List<Macro> parsedMacros = new ArrayList<>();
        JSONArray macroArray = object.optJSONArray("macros");
        if (macroArray != null) {
            for (int i = 0; i < macroArray.length(); i++) {
                JSONObject macro = macroArray.optJSONObject(i);
                if (macro != null) {
                    parsedMacros.add(Macro.fromJson(macro));
                }
            }
        }
        int macroCount = object.optInt("macroCount", parsedMacros.size() == 0 ? 4 : parsedMacros.size());
        macroCount = clampMacroCount(macroCount);
        while (parsedMacros.size() < macroCount) {
            parsedMacros.add(new Macro("Macro " + (parsedMacros.size() + 1),
                    ProfileStore.steps("wait:80ms")));
        }
        GameProfile profile = new GameProfile(
                object.optString("name", "Profile"),
                object.optString("mode", "\u901a\u7528"),
                object.optString("packageHint", ""),
                macroCount,
                parsedMacros);
        profile.macroColumns = object.optInt("macroColumns", 4);
        profile.macroRows = object.optInt("macroRows", 0);
        profile.rightHandPriority = object.optBoolean("rightHandPriority", true);
        profile.romContextHint = object.optString("romContextHint", "");
        profile.defaultForPackage = object.optBoolean("defaultForPackage", false);
        profile.iconUri = object.optString("iconUri", "");
        profile.mapTitle = object.optString("mapTitle", "");
        profile.mapUri = object.optString("mapUri", "");
        profile.interactiveMapTitle = object.optString("interactiveMapTitle", "");
        profile.interactiveMapUrl = object.optString("interactiveMapUrl", "");
        JSONArray mapArray = object.optJSONArray("maps");
        if (mapArray != null) {
            for (int i = 0; i < mapArray.length(); i++) {
                JSONObject map = mapArray.optJSONObject(i);
                if (map != null) {
                    MapEntry entry = MapEntry.fromJson(map);
                    if (entry.uri.trim().length() > 0) {
                        profile.maps.add(entry);
                    }
                }
            }
        }
        profile.ensureMapEntries();
        profile.syncLegacyMapFields();
        profile.touchpadSettings = TouchpadSettings.fromJson(
                object.optJSONObject("touchpadSettings"), fallbackTouchpadSettings);
        profile.widgetLayout = WidgetLayout.fromJson(object.optJSONObject("widgetLayout"));
        profile.applyLegacyMacroModuleSettings();
        JSONArray guideArray = object.optJSONArray("guides");
        if (guideArray != null) {
            for (int i = 0; i < guideArray.length(); i++) {
                JSONObject guide = guideArray.optJSONObject(i);
                if (guide != null) {
                    profile.guides.add(GuideEntry.fromJson(guide));
                }
            }
        }
        JSONArray markerArray = object.optJSONArray("mapMarkers");
        if (markerArray != null) {
            for (int i = 0; i < markerArray.length(); i++) {
                JSONObject marker = markerArray.optJSONObject(i);
                if (marker != null) {
                    profile.mapMarkers.add(MapMarker.fromJson(marker));
                }
            }
        }
        profile.normalizeLayout();
        return profile;
    }

    public TouchpadSettings safeTouchpadSettings() {
        if (touchpadSettings == null) {
            touchpadSettings = new TouchpadSettings();
        }
        return touchpadSettings;
    }

    public WidgetLayout safeWidgetLayout() {
        if (widgetLayout == null) {
            widgetLayout = WidgetLayout.defaultLayout();
        }
        applyLegacyMacroModuleSettings();
        widgetLayout.sanitize();
        return widgetLayout;
    }

    public List<MapEntry> safeMaps() {
        ensureMapEntries();
        return maps;
    }

    public void syncLegacyMapFields() {
        if (maps.isEmpty()) {
            mapTitle = "";
            mapUri = "";
            return;
        }
        MapEntry first = maps.get(0);
        mapTitle = first.title;
        mapUri = first.uri;
    }

    private void ensureMapEntries() {
        if (maps.isEmpty() && mapUri != null && mapUri.trim().length() > 0) {
            maps.add(new MapEntry(mapTitle, mapUri));
        }
    }

    public void setMacroCount(int value) {
        macroCount = clampMacroCount(value);
        while (macros.size() < macroCount) {
            macros.add(new Macro("Macro " + (macros.size() + 1), ProfileStore.steps("wait:80ms")));
        }
    }

    public void normalizeLayout() {
        macroColumns = clamp(macroColumns, 1, 4);
        macroRows = clamp(macroRows, 0, 6);
    }

    private void applyLegacyMacroModuleSettings() {
        if (widgetLayout == null) {
            return;
        }
        for (WidgetLayout.Item item : widgetLayout.items) {
            if (WidgetLayout.TYPE_MACRO_GROUP.equals(item.type) && !item.hasMacroConfig) {
                item.macroStart = 0;
                item.macroCount = macroCount;
                item.macroColumns = macroColumns;
                item.macroRows = macroRows;
                item.macroRightHandPriority = rightHandPriority;
                item.hasMacroConfig = true;
            }
        }
    }

    private static int clampMacroCount(int value) {
        return Math.max(1, Math.min(24, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
