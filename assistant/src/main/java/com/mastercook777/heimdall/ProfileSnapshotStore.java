package com.mastercook777.heimdall;

import android.content.Context;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProfileSnapshotStore {
    public static final String REASON_DELETE = "delete";
    public static final String REASON_REPLACE_ALL = "replace_all";
    public static final String REASON_RESTORE = "restore";
    public static final String REASON_SANITIZE = "sanitize";
    public static final String REASON_RECOVERY = "recovery";
    public static final int MAX_SNAPSHOTS = 5;

    private static final String DIRECTORY = "profile_snapshots";
    private static final String PREFIX = "profile-snapshot-";
    private static final Pattern FILE_PATTERN = Pattern.compile(
            "^profile-snapshot-(\\d+)-([a-z_]+)-(\\d+)\\.json$");

    private ProfileSnapshotStore() {
    }

    public static boolean createSnapshot(Context context, List<GameProfile> fallbackProfiles,
            int selectedIndex, String reason) {
        String raw = ProfileStore.rawProfilesJson(context);
        if ((raw == null || raw.length() == 0) && fallbackProfiles != null) {
            raw = ProfileStore.profilesToJson(fallbackProfiles);
        }
        return createRawSnapshot(context, raw, selectedIndex, reason);
    }

    static synchronized boolean createRawSnapshot(Context context, String raw, int selectedIndex,
            String reason) {
        if (context == null || raw == null || raw.length() == 0) {
            return false;
        }
        File directory = snapshotDirectory(context);
        if (!directory.exists() && !directory.mkdirs()) {
            return false;
        }
        deleteTemporaryFiles(directory);

        String safeReason = safeReason(reason);
        long createdAt = Math.max(1L, System.currentTimeMillis());
        File snapshot;
        do {
            snapshot = new File(directory, fileName(createdAt, safeReason, selectedIndex));
            createdAt++;
        } while (snapshot.exists());
        File temporary = new File(directory, snapshot.getName() + ".tmp");

        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(raw.getBytes(StandardCharsets.UTF_8));
            output.flush();
            output.getFD().sync();
        } catch (IOException ex) {
            temporary.delete();
            return false;
        }

        if (!temporary.renameTo(snapshot)) {
            temporary.delete();
            return false;
        }
        if (pruneToBound(directory)) {
            return true;
        }
        snapshot.delete();
        pruneToBound(directory);
        return false;
    }

    public static synchronized List<SnapshotInfo> listSnapshots(Context context) {
        File directory = snapshotDirectory(context);
        pruneToBound(directory);
        List<File> files = snapshotFiles(directory);
        List<SnapshotInfo> snapshots = new ArrayList<>();
        for (File file : files) {
            ParsedName parsed = parseName(file.getName());
            if (parsed == null) {
                continue;
            }
            int profileCount = 0;
            boolean readable = false;
            try {
                profileCount = ProfileStore.profilesFromJson(readText(file)).size();
                readable = profileCount > 0;
            } catch (Exception ignored) {
            }
            snapshots.add(new SnapshotInfo(file.getName(), parsed.createdAt, parsed.reason,
                    parsed.selectedIndex, profileCount, readable));
        }
        return snapshots;
    }

    public static List<GameProfile> readSnapshot(Context context, SnapshotInfo snapshot)
            throws IOException, JSONException {
        if (snapshot == null || parseName(snapshot.fileName) == null) {
            throw new IOException("Invalid snapshot");
        }
        File file = new File(snapshotDirectory(context), snapshot.fileName);
        if (!file.isFile()) {
            throw new IOException("Snapshot not found");
        }
        return ProfileStore.profilesFromJson(readText(file));
    }

    private static boolean pruneToBound(File directory) {
        List<File> files = snapshotFiles(directory);
        for (int i = MAX_SNAPSHOTS; i < files.size(); i++) {
            files.get(i).delete();
        }
        return snapshotFiles(directory).size() <= MAX_SNAPSHOTS;
    }

    private static void deleteTemporaryFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String name = file.getName();
            if (file.isFile() && name.startsWith(PREFIX) && name.endsWith(".json.tmp")) {
                file.delete();
            }
        }
    }

    private static List<File> snapshotFiles(File directory) {
        List<File> files = new ArrayList<>();
        File[] candidates = directory.listFiles();
        if (candidates == null) {
            return files;
        }
        for (File candidate : candidates) {
            if (candidate.isFile() && parseName(candidate.getName()) != null) {
                files.add(candidate);
            }
        }
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File left, File right) {
                ParsedName leftName = parseName(left.getName());
                ParsedName rightName = parseName(right.getName());
                long leftTime = leftName == null ? 0L : leftName.createdAt;
                long rightTime = rightName == null ? 0L : rightName.createdAt;
                return Long.compare(rightTime, leftTime);
            }
        });
        return files;
    }

    private static File snapshotDirectory(Context context) {
        return new File(context.getApplicationContext().getFilesDir(), DIRECTORY);
    }

    private static String fileName(long createdAt, String reason, int selectedIndex) {
        return PREFIX + createdAt + "-" + reason + "-" + Math.max(0, selectedIndex) + ".json";
    }

    private static ParsedName parseName(String name) {
        Matcher matcher = FILE_PATTERN.matcher(name == null ? "" : name);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new ParsedName(Long.parseLong(matcher.group(1)), matcher.group(2),
                    Integer.parseInt(matcher.group(3)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String safeReason(String reason) {
        if (REASON_DELETE.equals(reason)
                || REASON_REPLACE_ALL.equals(reason)
                || REASON_RESTORE.equals(reason)
                || REASON_SANITIZE.equals(reason)
                || REASON_RECOVERY.equals(reason)) {
            return reason;
        }
        return REASON_RECOVERY;
    }

    private static String readText(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public static final class SnapshotInfo {
        public final String fileName;
        public final long createdAt;
        public final String reason;
        public final int selectedIndex;
        public final int profileCount;
        public final boolean readable;

        private SnapshotInfo(String fileName, long createdAt, String reason, int selectedIndex,
                int profileCount, boolean readable) {
            this.fileName = fileName;
            this.createdAt = createdAt;
            this.reason = reason;
            this.selectedIndex = selectedIndex;
            this.profileCount = profileCount;
            this.readable = readable;
        }
    }

    private static final class ParsedName {
        final long createdAt;
        final String reason;
        final int selectedIndex;

        ParsedName(long createdAt, String reason, int selectedIndex) {
            this.createdAt = createdAt;
            this.reason = reason;
            this.selectedIndex = selectedIndex;
        }
    }
}
