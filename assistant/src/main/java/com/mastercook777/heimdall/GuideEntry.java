package com.mastercook777.heimdall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GuideEntry {
    public static final String TYPE_NOTE = "note";
    public static final String TYPE_LINK = "link";
    public static final String TYPE_FILE = "file";
    public static final String READER_MODE_READING = "reading";
    public static final String READER_MODE_ORIGINAL = "original";
    public static final int MAX_BOOKMARKS = 100;

    public static final class Bookmark {
        public final String label;
        public final int anchor;
        public final int anchorTop;
        public final int horizontalColumn;
        public final int viewportWidth;
        public final String fingerprint;

        Bookmark(String label, int anchor, int anchorTop, int horizontalColumn,
                int viewportWidth, String fingerprint) {
            this.label = normalizeBookmarkLabel(label);
            this.anchor = clampPosition(anchor);
            this.anchorTop = clampAnchorTop(anchorTop);
            this.horizontalColumn = clampPosition(horizontalColumn);
            this.viewportWidth = clampViewportWidth(viewportWidth);
            this.fingerprint = normalizeFingerprint(fingerprint);
        }

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("label", label);
            object.put("anchor", anchor);
            object.put("anchorTop", anchorTop);
            object.put("horizontalColumn", horizontalColumn);
            object.put("viewportWidth", viewportWidth);
            object.put("fingerprint", fingerprint);
            return object;
        }

        static Bookmark fromJson(JSONObject object) {
            if (object == null) {
                return null;
            }
            Bookmark bookmark = new Bookmark(
                    object.optString("label", ""),
                    object.optInt("anchor", 0),
                    object.optInt("anchorTop", 0),
                    object.optInt("horizontalColumn", 0),
                    object.optInt("viewportWidth", 0),
                    object.optString("fingerprint", ""));
            return bookmark.fingerprint.length() == 0 ? null : bookmark;
        }

        Bookmark copy() {
            return new Bookmark(label, anchor, anchorTop, horizontalColumn,
                    viewportWidth, fingerprint);
        }

        boolean belongsTo(String currentFingerprint) {
            return fingerprint.equals(normalizeFingerprint(currentFingerprint));
        }
    }

    public String title;
    public String type;
    public String content;
    public int readerScrollY;
    public int readerAnchor;
    public int readerAnchorTop;
    public int readerHorizontalColumn;
    public int readerViewportWidth;
    public String readerFingerprint = "";
    public String readerMode = READER_MODE_READING;
    public final List<Bookmark> bookmarks = new ArrayList<>();

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
        if (!READER_MODE_READING.equals(readerMode)) {
            object.put("readerMode", readerMode);
        }
        if (readerFingerprint.length() > 0) {
            object.put("readerScrollY", readerScrollY);
            object.put("readerAnchor", readerAnchor);
            object.put("readerAnchorTop", readerAnchorTop);
            object.put("readerHorizontalColumn", readerHorizontalColumn);
            object.put("readerViewportWidth", readerViewportWidth);
            object.put("readerFingerprint", readerFingerprint);
        }
        if (!bookmarks.isEmpty()) {
            JSONArray bookmarkArray = new JSONArray();
            for (Bookmark bookmark : bookmarks) {
                bookmarkArray.put(bookmark.toJson());
            }
            object.put("bookmarks", bookmarkArray);
        }
        return object;
    }

    public static GuideEntry fromJson(JSONObject object) {
        GuideEntry entry = new GuideEntry(
                object.optString("title", "\u653b\u7565"),
                object.optString("type", TYPE_NOTE),
                object.optString("content", ""));
        String fingerprint = normalizeFingerprint(object.optString("readerFingerprint", ""));
        entry.readerMode = normalizeReaderMode(object.optString(
                "readerMode", READER_MODE_READING));
        if (fingerprint.length() > 0) {
            entry.readerScrollY = clampPosition(object.optInt("readerScrollY", 0));
            entry.readerAnchor = clampPosition(object.optInt("readerAnchor", 0));
            entry.readerAnchorTop = clampAnchorTop(object.optInt("readerAnchorTop", 0));
            entry.readerHorizontalColumn = clampPosition(
                    object.optInt("readerHorizontalColumn", 0));
            entry.readerViewportWidth = clampViewportWidth(
                    object.optInt("readerViewportWidth", 0));
            entry.readerFingerprint = fingerprint;
        }
        JSONArray bookmarkArray = object.optJSONArray("bookmarks");
        if (bookmarkArray != null) {
            int count = Math.min(bookmarkArray.length(), MAX_BOOKMARKS);
            for (int index = 0; index < count; index++) {
                Bookmark bookmark = Bookmark.fromJson(bookmarkArray.optJSONObject(index));
                if (bookmark != null) {
                    entry.bookmarks.add(bookmark);
                }
            }
        }
        return entry;
    }

    public GuideEntry copy() {
        GuideEntry copy = new GuideEntry(title, type, content);
        copy.readerScrollY = readerScrollY;
        copy.readerAnchor = readerAnchor;
        copy.readerAnchorTop = readerAnchorTop;
        copy.readerHorizontalColumn = readerHorizontalColumn;
        copy.readerViewportWidth = readerViewportWidth;
        copy.readerFingerprint = readerFingerprint;
        copy.readerMode = readerMode;
        for (Bookmark bookmark : bookmarks) {
            copy.bookmarks.add(bookmark.copy());
        }
        return copy;
    }

    public boolean hasBookmarkAt(String fingerprint, int anchor) {
        String normalizedFingerprint = normalizeFingerprint(fingerprint);
        int normalizedAnchor = clampPosition(anchor);
        for (Bookmark bookmark : bookmarks) {
            if (bookmark.fingerprint.equals(normalizedFingerprint)
                    && bookmark.anchor == normalizedAnchor) {
                return true;
            }
        }
        return false;
    }

    public Bookmark addBookmark(String label, int anchor, int anchorTop,
            int horizontalColumn, int viewportWidth, String fingerprint) {
        String normalizedFingerprint = normalizeFingerprint(fingerprint);
        int normalizedAnchor = clampPosition(anchor);
        if (normalizedFingerprint.length() == 0 || bookmarks.size() >= MAX_BOOKMARKS
                || hasBookmarkAt(normalizedFingerprint, normalizedAnchor)) {
            return null;
        }
        Bookmark bookmark = new Bookmark(label, normalizedAnchor, anchorTop,
                horizontalColumn, viewportWidth, normalizedFingerprint);
        bookmarks.add(bookmark);
        return bookmark;
    }

    public boolean removeBookmark(Bookmark bookmark) {
        return bookmark != null && bookmarks.remove(bookmark);
    }

    public boolean hasReadingPositionFor(String fingerprint) {
        return readerFingerprint.length() > 0 && readerFingerprint.equals(fingerprint);
    }

    public boolean updateReadingPosition(int scrollY, int anchor, int viewportWidth,
            String fingerprint) {
        return updateReadingPosition(scrollY, anchor, 0, 0, viewportWidth, fingerprint);
    }

    public boolean updateReadingPosition(int scrollY, int anchor, int anchorTop,
            int horizontalColumn, int viewportWidth, String fingerprint) {
        String normalizedFingerprint = normalizeFingerprint(fingerprint);
        int normalizedScrollY = clampPosition(scrollY);
        int normalizedAnchor = clampPosition(anchor);
        int normalizedAnchorTop = clampAnchorTop(anchorTop);
        int normalizedHorizontalColumn = clampPosition(horizontalColumn);
        int normalizedViewportWidth = clampViewportWidth(viewportWidth);
        boolean changed = readerScrollY != normalizedScrollY
                || readerAnchor != normalizedAnchor
                || readerAnchorTop != normalizedAnchorTop
                || readerHorizontalColumn != normalizedHorizontalColumn
                || readerViewportWidth != normalizedViewportWidth
                || !readerFingerprint.equals(normalizedFingerprint);
        readerScrollY = normalizedScrollY;
        readerAnchor = normalizedAnchor;
        readerAnchorTop = normalizedAnchorTop;
        readerHorizontalColumn = normalizedHorizontalColumn;
        readerViewportWidth = normalizedViewportWidth;
        readerFingerprint = normalizedFingerprint;
        return changed;
    }

    public boolean clearReadingPosition() {
        return updateReadingPosition(0, 0, 0, 0, 0, "");
    }

    public boolean updateReaderPresentation(String mode) {
        String normalizedMode = normalizeReaderMode(mode);
        boolean changed = !readerMode.equals(normalizedMode);
        readerMode = normalizedMode;
        return changed;
    }

    public static String readingFingerprint(String renderedText) {
        return readingFingerprint("", renderedText);
    }

    public static String readingFingerprint(String sourceIdentity, String renderedText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = (sourceIdentity == null ? "" : sourceIdentity)
                    + "\n" + (renderedText == null ? "" : renderedText);
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format(Locale.US, "%02x", value & 0xFF));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static String normalizeType(String value) {
        if (TYPE_LINK.equals(value) || TYPE_FILE.equals(value)) {
            return value;
        }
        return TYPE_NOTE;
    }

    public static String normalizeReaderMode(String value) {
        return READER_MODE_ORIGINAL.equals(value)
                ? READER_MODE_ORIGINAL : READER_MODE_READING;
    }

    private static int clampPosition(int value) {
        return Math.max(0, Math.min(10_000_000, value));
    }

    private static int clampViewportWidth(int value) {
        return Math.max(0, Math.min(10_000, value));
    }

    private static int clampAnchorTop(int value) {
        return Math.max(-10_000, Math.min(10_000, value));
    }

    private static String normalizeFingerprint(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.US);
        return normalized.matches("[0-9a-f]{64}") ? normalized : "";
    }

    private static String normalizeBookmarkLabel(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80).trim();
    }
}
