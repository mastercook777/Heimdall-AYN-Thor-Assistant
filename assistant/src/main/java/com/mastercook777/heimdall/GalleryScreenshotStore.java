package com.mastercook777.heimdall;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/** Local ownership index. Screenshot bytes stay in MediaStore and never enter Profile bundles. */
final class GalleryScreenshotStore extends SQLiteOpenHelper {
    private static final String DATABASE = "gallery_screenshots.db";

    static final class Entry {
        final Uri uri;
        final long createdAt;

        Entry(Uri uri, long createdAt) {
            this.uri = uri;
            this.createdAt = createdAt;
        }
    }

    GalleryScreenshotStore(Context context) {
        this(context, DATABASE);
    }

    GalleryScreenshotStore(Context context, String databaseName) {
        super(context.getApplicationContext(), databaseName, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE screenshots (uri TEXT PRIMARY KEY NOT NULL, "
                + "profile_id TEXT NOT NULL, created_at INTEGER NOT NULL)");
        database.execSQL("CREATE INDEX screenshots_profile_date "
                + "ON screenshots(profile_id, created_at DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // Version 1 has no legacy schema.
    }

    boolean add(String profileId, Uri uri, long createdAt) {
        if (profileId == null || profileId.trim().isEmpty() || uri == null
                || !"content".equals(uri.getScheme())) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("uri", uri.toString());
        values.put("profile_id", profileId.trim());
        values.put("created_at", createdAt);
        return getWritableDatabase().insertWithOnConflict("screenshots", null, values,
                SQLiteDatabase.CONFLICT_REPLACE) != -1L;
    }

    List<Entry> list(String profileId) {
        List<Entry> entries = new ArrayList<>();
        if (profileId == null || profileId.trim().isEmpty()) {
            return entries;
        }
        try (Cursor cursor = getReadableDatabase().query("screenshots",
                new String[]{"uri", "created_at"}, "profile_id=?",
                new String[]{profileId.trim()}, null, null, "created_at DESC, uri DESC")) {
            while (cursor.moveToNext()) {
                entries.add(new Entry(Uri.parse(cursor.getString(0)), cursor.getLong(1)));
            }
        }
        return entries;
    }

    void remove(String profileId, Uri uri) {
        if (profileId == null || uri == null) {
            return;
        }
        getWritableDatabase().delete("screenshots", "profile_id=? AND uri=?",
                new String[]{profileId.trim(), uri.toString()});
    }
}
