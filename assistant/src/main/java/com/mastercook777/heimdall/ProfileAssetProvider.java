package com.mastercook777.heimdall;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class ProfileAssetProvider extends ContentProvider {
    static Uri uriFor(Context context, String storedName, String displayName) {
        String visibleName = ProfileAssetStore.sanitizeDisplayName(displayName, storedName);
        return new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".profile-assets")
                .appendPath(storedName)
                .appendPath(visibleName)
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        String extension = ProfileAssetStore.extensionOf(storedName(uri));
        if ("jpg".equals(extension)) {
            return "image/jpeg";
        }
        if ("png".equals(extension)) {
            return "image/png";
        }
        if ("webp".equals(extension)) {
            return "image/webp";
        }
        if ("gif".equals(extension)) {
            return "image/gif";
        }
        if ("pdf".equals(extension)) {
            return "application/pdf";
        }
        if ("html".equals(extension) || "htm".equals(extension)) {
            return "text/html";
        }
        if ("md".equals(extension)) {
            return "text/markdown";
        }
        return "text/plain";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        File file;
        try {
            file = requireFile(uri);
        } catch (FileNotFoundException ex) {
            return null;
        }
        String[] requested = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                : projection;
        MatrixCursor cursor = new MatrixCursor(requested, 1);
        MatrixCursor.RowBuilder row = cursor.newRow();
        for (String column : requested) {
            if (OpenableColumns.DISPLAY_NAME.equals(column)) {
                row.add(displayName(uri, file.getName()));
            } else if (OpenableColumns.SIZE.equals(column)) {
                row.add(file.length());
            } else {
                row.add(null);
            }
        }
        return cursor;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Profile assets are read-only");
        }
        return ParcelFileDescriptor.open(requireFile(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Profile assets are read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }

    private File requireFile(Uri uri) throws FileNotFoundException {
        Context context = getContext();
        if (context == null || !authority(context).equals(uri == null ? null : uri.getAuthority())) {
            throw new FileNotFoundException("Invalid Profile asset authority");
        }
        try {
            return ProfileAssetStore.resolve(context, storedName(uri));
        } catch (IOException ex) {
            FileNotFoundException failure = new FileNotFoundException(ex.getMessage());
            failure.initCause(ex);
            throw failure;
        }
    }

    private static String authority(Context context) {
        return context.getPackageName() + ".profile-assets";
    }

    private static String storedName(Uri uri) {
        List<String> segments = uri == null ? null : uri.getPathSegments();
        return segments == null || segments.isEmpty()
                ? "" : segments.get(0).toLowerCase(Locale.US);
    }

    private static String displayName(Uri uri, String fallback) {
        List<String> segments = uri == null ? null : uri.getPathSegments();
        return segments == null || segments.size() < 2
                ? fallback : ProfileAssetStore.sanitizeDisplayName(segments.get(1), fallback);
    }
}
