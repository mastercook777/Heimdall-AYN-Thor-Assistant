package com.mastercook777.heimdall;

import org.json.JSONException;
import org.json.JSONObject;

public final class MapEntry {
    public String title;
    public String uri;

    public MapEntry(String title, String uri) {
        this.title = title == null ? "" : title;
        this.uri = uri == null ? "" : uri;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("title", title);
        object.put("uri", uri);
        return object;
    }

    public static MapEntry fromJson(JSONObject object) {
        return new MapEntry(object.optString("title", ""), object.optString("uri", ""));
    }
}
