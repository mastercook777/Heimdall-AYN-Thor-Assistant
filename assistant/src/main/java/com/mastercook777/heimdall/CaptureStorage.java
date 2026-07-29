package com.mastercook777.heimdall;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class CaptureStorage {
    private CaptureStorage() {
    }

    static String profileFolder(String profileName) {
        String safe = profileName == null ? "Profile" : profileName.trim();
        safe = safe.replaceAll("[\\\\/:*?\"<>|]", "_");
        safe = safe.replaceAll("\\s+", " ");
        if (safe.length() == 0) {
            safe = "Profile";
        }
        return safe.length() > 48 ? safe.substring(0, 48) : safe;
    }

    static String timestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    static Uri createImage(ContentResolver resolver, String profileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Heimdall_" + timestamp() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/Heimdall/" + profileFolder(profileName));
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    static Uri createVideo(ContentResolver resolver, String profileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, "Heimdall_" + timestamp() + ".mp4");
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/Heimdall/" + profileFolder(profileName));
        values.put(MediaStore.Video.Media.IS_PENDING, 1);
        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }

    static void publish(ContentResolver resolver, Uri uri) {
        if (uri == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
    }

    static void discard(ContentResolver resolver, Uri uri) {
        if (uri != null) {
            resolver.delete(uri, null, null);
        }
    }
}
