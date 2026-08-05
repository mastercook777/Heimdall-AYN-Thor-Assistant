package com.mastercook777.heimdall;

import android.content.ContentProvider;
import android.content.ContentValues;
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

public final class ProfileBundleTestProvider extends ContentProvider {
    static final String AUTHORITY = "com.mastercook777.heimdall.debug.test.profile-bundle";

    static Uri uri(String token, String displayName) {
        return new Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendPath(token).appendPath(displayName).build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        String name = displayName(uri).toLowerCase(Locale.US);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".md")) return "text/markdown";
        if (name.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        File file;
        try {
            file = file(uri);
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
                row.add(displayName(uri));
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
        File file = file(uri);
        if (mode != null && mode.startsWith("w")) {
            File parent = file.getParentFile();
            if (parent == null || (!parent.exists() && !parent.mkdirs())) {
                throw new FileNotFoundException("Unable to create test storage");
            }
            return ParcelFileDescriptor.open(file,
                    ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE
                            | ParcelFileDescriptor.MODE_WRITE_ONLY);
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        try {
            return file(uri).delete() ? 1 : 0;
        } catch (FileNotFoundException ex) {
            return 0;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }

    private File file(Uri uri) throws FileNotFoundException {
        if (getContext() == null || uri == null || !AUTHORITY.equals(uri.getAuthority())) {
            throw new FileNotFoundException("Invalid test URI");
        }
        List<String> segments = uri.getPathSegments();
        String token = segments.isEmpty() ? "" : segments.get(0);
        if (!token.matches("^[a-z0-9_-]{1,80}$")) {
            throw new FileNotFoundException("Invalid test token");
        }
        File directory = new File(getContext().getFilesDir(), "profile_bundle_test");
        File candidate = new File(directory, token);
        try {
            if (!directory.getCanonicalFile().equals(candidate.getCanonicalFile().getParentFile())) {
                throw new FileNotFoundException("Escaped test storage");
            }
        } catch (IOException ex) {
            throw new FileNotFoundException(ex.getMessage());
        }
        return candidate;
    }

    private static String displayName(Uri uri) {
        List<String> segments = uri == null ? null : uri.getPathSegments();
        return segments == null || segments.size() < 2 ? "fixture.bin" : segments.get(1);
    }
}
