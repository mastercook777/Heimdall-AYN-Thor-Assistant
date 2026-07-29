package com.mastercook777.heimdall;

import org.json.JSONException;
import org.json.JSONObject;

public final class GuideEntry {
    public static final String TYPE_NOTE = "note";
    public static final String TYPE_LINK = "link";
    public static final String TYPE_FILE = "file";

    public String title;
    public String type;
    public String content;

    public GuideEntry(String title, String type, String content) {
        this.title = title;
        this.type = normalizeType(type);
        this.content = content == null ? "" : content;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("title", title);
        object.put("type", type);
        object.put("content", content);
        return object;
    }

    public static GuideEntry fromJson(JSONObject object) {
        return new GuideEntry(
                object.optString("title", "\u653b\u7565"),
                object.optString("type", TYPE_NOTE),
                object.optString("content", ""));
    }

    public static String normalizeType(String value) {
        if (TYPE_LINK.equals(value) || TYPE_FILE.equals(value)) {
            return value;
        }
        return TYPE_NOTE;
    }
}
