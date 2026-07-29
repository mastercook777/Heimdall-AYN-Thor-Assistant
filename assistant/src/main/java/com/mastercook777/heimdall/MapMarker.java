package com.mastercook777.heimdall;

import org.json.JSONException;
import org.json.JSONObject;

public final class MapMarker {
    public String title;
    public String note;
    public String position;

    public MapMarker(String title, String note, String position) {
        this.title = title == null ? "" : title;
        this.note = note == null ? "" : note;
        this.position = position == null ? "" : position;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("title", title);
        object.put("note", note);
        object.put("position", position);
        return object;
    }

    public static MapMarker fromJson(JSONObject object) {
        return new MapMarker(
                object.optString("title", "\u6807\u8bb0"),
                object.optString("note", ""),
                object.optString("position", ""));
    }
}
