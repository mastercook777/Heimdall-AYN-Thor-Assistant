package com.mastercook777.heimdall;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

final class ProfileBundleStore {
    static final int SCHEMA_VERSION = 1;
    static final String FILE_EXTENSION = ".heimdall-profile";
    static final String MIME_TYPE = "application/zip";

    private static final String FORMAT = "heimdall-profile-bundle";
    private static final String MANIFEST_ENTRY = "manifest.json";
    private static final String PROFILES_ENTRY = "profiles.json";
    private static final String PLACEHOLDER_PREFIX = "heimdall-bundle:";
    private static final String JOB_DIRECTORY = "profile_bundle_jobs";
    private static final Pattern JOB_NAME = Pattern.compile(
            "^job-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final Pattern ASSET_PATH = Pattern.compile(
            "^assets/([0-9a-f]{64})\\.(jpg|png|webp|gif|pdf|txt|md|html|htm)$");
    private static final long MAX_ASSET_BYTES = 128L * 1024L * 1024L;
    private static final long MAX_TOTAL_ASSET_BYTES = 512L * 1024L * 1024L;
    private static final long MAX_BUNDLE_BYTES = 512L * 1024L * 1024L;
    private static final int MAX_ASSET_COUNT = 512;
    private static final int MAX_PROFILE_JSON_BYTES = 16 * 1024 * 1024;
    private static final int MAX_MANIFEST_BYTES = 1024 * 1024;
    private static final long STALE_JOB_MS = 24L * 60L * 60L * 1000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "heimdall-profile-bundle");
        thread.setDaemon(true);
        return thread;
    });

    enum ErrorCode {
        EMPTY,
        MISSING_ASSET,
        UNSUPPORTED_ASSET,
        TOO_LARGE,
        INVALID_FORMAT,
        UNSUPPORTED_VERSION,
        CORRUPT,
        UNSAFE_PATH,
        STORAGE
    }

    static final class Failure extends Exception {
        final ErrorCode code;
        final String detail;

        Failure(ErrorCode code, String detail) {
            super(detail);
            this.code = code;
            this.detail = detail == null ? "" : detail;
        }

        Failure(ErrorCode code, String detail, Throwable cause) {
            super(detail, cause);
            this.code = code;
            this.detail = detail == null ? "" : detail;
        }
    }

    interface ExportCallback {
        void onExported(int assetCount, long assetBytes);
        void onError(Failure failure);
    }

    interface ImportCallback {
        void onPrepared(PreparedImport preparedImport);
        void onError(Failure failure);
    }

    interface InstallCallback {
        void onInstalled(List<GameProfile> profiles);
        void onError(Failure failure);
    }

    static final class Request {
        private final AtomicBoolean cancelled = new AtomicBoolean();

        void cancel() {
            cancelled.set(true);
        }

        boolean isCancelled() {
            return cancelled.get();
        }

        void throwIfCancelled() throws Failure {
            if (isCancelled()) {
                throw new Failure(ErrorCode.STORAGE, "Operation cancelled");
            }
        }
    }

    static final class PreparedImport {
        final List<GameProfile> profiles;
        final int assetCount;
        final long assetBytes;
        final boolean legacyJson;
        private final File jobDirectory;
        private final List<InstallTask> installTasks;
        private boolean closed;

        PreparedImport(List<GameProfile> profiles, int assetCount, long assetBytes,
                boolean legacyJson, File jobDirectory, List<InstallTask> installTasks) {
            this.profiles = profiles;
            this.assetCount = assetCount;
            this.assetBytes = assetBytes;
            this.legacyJson = legacyJson;
            this.jobDirectory = jobDirectory;
            this.installTasks = installTasks;
        }

        synchronized void close() {
            if (closed) {
                return;
            }
            closed = true;
            deleteRecursively(jobDirectory);
        }

        synchronized boolean isClosed() {
            return closed;
        }
    }

    private enum AssetKind {
        PROFILE_ICON,
        MAP,
        GUIDE,
        MACRO_ICON,
        CANVAS
    }

    private enum InstallKind {
        PROFILE_ASSET,
        MACRO_ICON,
        CANVAS
    }

    private static final class AssetType {
        final String extension;
        final String mimeType;

        AssetType(String extension, String mimeType) {
            this.extension = extension;
            this.mimeType = mimeType;
        }
    }

    private static final class BundleAsset {
        final String sha256;
        final String extension;
        final String mimeType;
        final String displayName;
        final File file;
        final long size;

        BundleAsset(String sha256, AssetType type, String displayName, File file, long size) {
            this.sha256 = sha256;
            this.extension = type.extension;
            this.mimeType = type.mimeType;
            this.displayName = displayName;
            this.file = file;
            this.size = size;
        }

        String path() {
            return "assets/" + sha256 + "." + extension;
        }

        JSONObject toJson() throws JSONException {
            JSONObject object = new JSONObject();
            object.put("id", sha256);
            object.put("path", path());
            object.put("sha256", sha256);
            object.put("size", size);
            object.put("mediaType", mimeType);
            object.put("displayName", displayName);
            return object;
        }
    }

    private static final class InstallTask {
        final InstallKind kind;
        final BundleAsset asset;
        final String expectedReference;

        InstallTask(InstallKind kind, BundleAsset asset, String expectedReference) {
            this.kind = kind;
            this.asset = asset;
            this.expectedReference = expectedReference;
        }
    }

    private static final class AssetCollector {
        final Context context;
        final Request request;
        final File stagingDirectory;
        final Map<String, BundleAsset> assets = new LinkedHashMap<>();
        long totalBytes;

        AssetCollector(Context context, Request request, File stagingDirectory) {
            this.context = context;
            this.request = request;
            this.stagingDirectory = stagingDirectory;
        }

        String addReference(String reference, AssetKind kind, String fallbackName)
                throws Failure {
            if (reference == null || reference.trim().length() == 0) {
                throw new Failure(ErrorCode.MISSING_ASSET, fallbackName);
            }
            Uri uri = Uri.parse(reference.trim());
            String displayName = displayName(context, uri, fallbackName);
            String declaredMime = safeMime(context, uri);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                String path = uri.getPath();
                return addFile(path == null ? null : new File(path), kind, displayName,
                        declaredMime);
            }
            try {
                InputStream input = context.getContentResolver().openInputStream(uri);
                if (input == null) {
                    throw new Failure(ErrorCode.MISSING_ASSET, displayName);
                }
                try (InputStream closeable = input) {
                    return addStream(closeable, kind, displayName, declaredMime);
                }
            } catch (SecurityException | IOException ex) {
                throw new Failure(ErrorCode.MISSING_ASSET, displayName, ex);
            }
        }

        String addFile(File source, AssetKind kind, String displayName, String declaredMime)
                throws Failure {
            if (source == null || !source.isFile()) {
                throw new Failure(ErrorCode.MISSING_ASSET, displayName);
            }
            try (FileInputStream input = new FileInputStream(source)) {
                return addStream(input, kind, displayName, declaredMime);
            } catch (IOException ex) {
                throw new Failure(ErrorCode.MISSING_ASSET, displayName, ex);
            }
        }

        private String addStream(InputStream input, AssetKind kind, String displayName,
                String declaredMime) throws Failure {
            request.throwIfCancelled();
            File temporary = new File(stagingDirectory,
                    ".collect-" + UUID.randomUUID() + ".tmp");
            long size = 0L;
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    request.throwIfCancelled();
                    size += read;
                    if (size > MAX_ASSET_BYTES) {
                        throw new Failure(ErrorCode.TOO_LARGE, displayName);
                    }
                    output.write(buffer, 0, read);
                }
                output.flush();
                output.getFD().sync();
            } catch (Failure failure) {
                temporary.delete();
                throw failure;
            } catch (IOException ex) {
                temporary.delete();
                throw new Failure(ErrorCode.STORAGE, displayName, ex);
            }
            if (size <= 0L) {
                temporary.delete();
                throw new Failure(ErrorCode.MISSING_ASSET, displayName);
            }

            try {
                String sha256 = ProfileAssetStore.sha256(temporary);
                AssetType type = detectType(temporary, displayName, declaredMime);
                requireSupported(kind, type, temporary, displayName);
                BundleAsset existing = assets.get(sha256);
                if (existing != null) {
                    requireSupported(kind, new AssetType(existing.extension, existing.mimeType),
                            existing.file, displayName);
                    temporary.delete();
                    return placeholder(sha256);
                }
                if (assets.size() >= MAX_ASSET_COUNT
                        || totalBytes + size > MAX_TOTAL_ASSET_BYTES) {
                    temporary.delete();
                    throw new Failure(ErrorCode.TOO_LARGE, displayName);
                }
                String safeName = ProfileAssetStore.sanitizeDisplayName(displayName,
                        sha256 + "." + type.extension);
                File destination = new File(stagingDirectory, sha256 + "." + type.extension);
                if (!temporary.renameTo(destination)) {
                    temporary.delete();
                    throw new Failure(ErrorCode.STORAGE, safeName);
                }
                assets.put(sha256, new BundleAsset(
                        sha256, type, safeName, destination, size));
                totalBytes += size;
                return placeholder(sha256);
            } catch (IOException ex) {
                temporary.delete();
                throw new Failure(ErrorCode.STORAGE, displayName, ex);
            }
        }
    }

    private ProfileBundleStore() {
    }

    static Request exportAsync(Context context, List<GameProfile> profiles, String exportedAt,
            Uri destination, ExportCallback callback) {
        Request request = new Request();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            Failure failure = null;
            int assetCount = 0;
            long assetBytes = 0L;
            File jobDirectory = null;
            try {
                jobDirectory = createJobDirectory(appContext);
                ExportSummary summary = exportBundle(appContext, profiles, exportedAt,
                        destination, request, jobDirectory);
                assetCount = summary.assetCount;
                assetBytes = summary.assetBytes;
            } catch (Failure ex) {
                failure = ex;
            } catch (Exception ex) {
                failure = new Failure(ErrorCode.STORAGE, "Profile export", ex);
            } finally {
                deleteRecursively(jobDirectory);
            }
            Failure finalFailure = failure;
            int finalAssetCount = assetCount;
            long finalAssetBytes = assetBytes;
            MAIN.post(() -> {
                if (request.isCancelled()) {
                    return;
                }
                if (finalFailure == null) {
                    callback.onExported(finalAssetCount, finalAssetBytes);
                } else {
                    callback.onError(finalFailure);
                }
            });
        });
        return request;
    }

    static Request prepareImportAsync(Context context, Uri source, ImportCallback callback) {
        Request request = new Request();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            PreparedImport prepared = null;
            Failure failure = null;
            try {
                File jobDirectory = createJobDirectory(appContext);
                try {
                    prepared = prepareImport(appContext, source, request, jobDirectory);
                } catch (Exception ex) {
                    deleteRecursively(jobDirectory);
                    throw ex;
                }
            } catch (Failure ex) {
                failure = ex;
            } catch (Exception ex) {
                failure = new Failure(ErrorCode.STORAGE, "Profile import", ex);
            }
            PreparedImport finalPrepared = prepared;
            Failure finalFailure = failure;
            MAIN.post(() -> {
                if (request.isCancelled()) {
                    if (finalPrepared != null) {
                        finalPrepared.close();
                    }
                    return;
                }
                if (finalFailure == null && finalPrepared != null) {
                    callback.onPrepared(finalPrepared);
                } else {
                    callback.onError(finalFailure == null
                            ? new Failure(ErrorCode.INVALID_FORMAT, "Profile import")
                            : finalFailure);
                }
            });
        });
        return request;
    }

    static Request installAsync(Context context, PreparedImport prepared,
            InstallCallback callback) {
        Request request = new Request();
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            Failure failure = null;
            try {
                if (prepared == null || prepared.isClosed()) {
                    throw new Failure(ErrorCode.STORAGE, "Prepared import expired");
                }
                for (InstallTask task : prepared.installTasks) {
                    request.throwIfCancelled();
                    String installed;
                    if (task.kind == InstallKind.MACRO_ICON) {
                        installed = MacroIconRepository.installBundledIcon(appContext,
                                task.asset.file, task.asset.sha256);
                    } else if (task.kind == InstallKind.CANVAS) {
                        installed = CanvasAssetStore.installBundledAsset(appContext,
                                task.asset.file, task.asset.sha256, task.asset.extension);
                    } else {
                        installed = ProfileAssetStore.install(appContext, task.asset.file,
                                task.asset.sha256, task.asset.extension,
                                task.asset.displayName);
                    }
                    if (!task.expectedReference.equals(installed)) {
                        throw new Failure(ErrorCode.CORRUPT, task.asset.displayName);
                    }
                }
            } catch (Failure ex) {
                failure = ex;
            } catch (Exception ex) {
                failure = new Failure(ErrorCode.STORAGE, "Profile assets", ex);
            }
            List<GameProfile> profiles = prepared.profiles;
            prepared.close();
            Failure finalFailure = failure;
            MAIN.post(() -> {
                if (request.isCancelled()) {
                    return;
                }
                if (finalFailure == null) {
                    callback.onInstalled(profiles);
                } else {
                    callback.onError(finalFailure);
                }
            });
        });
        return request;
    }

    private static ExportSummary exportBundle(Context context, List<GameProfile> profiles,
            String exportedAt, Uri destination, Request request, File jobDirectory)
            throws Failure {
        if (profiles == null || profiles.isEmpty() || destination == null) {
            throw new Failure(ErrorCode.EMPTY, "Profile export");
        }
        File stagingDirectory = new File(jobDirectory, "assets");
        if (!stagingDirectory.mkdirs()) {
            throw new Failure(ErrorCode.STORAGE, "Profile export staging");
        }
        AssetCollector collector = new AssetCollector(context, request, stagingDirectory);
        JSONObject profileRoot;
        try {
            profileRoot = new JSONObject(ProfileStore.profilesToExportJson(profiles, exportedAt));
            rewriteExportReferences(context, profileRoot, collector);
        } catch (JSONException ex) {
            throw new Failure(ErrorCode.INVALID_FORMAT, "Profile data", ex);
        }
        byte[] profileBytes = profileRoot.toString().getBytes(StandardCharsets.UTF_8);
        if (profileBytes.length <= 0 || profileBytes.length > MAX_PROFILE_JSON_BYTES) {
            throw new Failure(ErrorCode.TOO_LARGE, "Profile data");
        }
        String profileDigest = sha256(profileBytes);
        List<BundleAsset> orderedAssets = new ArrayList<>(collector.assets.values());
        Collections.sort(orderedAssets, Comparator.comparing(asset -> asset.sha256));

        JSONObject manifest = new JSONObject();
        try {
            manifest.put("format", FORMAT);
            manifest.put("schemaVersion", SCHEMA_VERSION);
            manifest.put("createdAt", exportedAt == null ? "" : exportedAt);
            manifest.put("profilesPath", PROFILES_ENTRY);
            manifest.put("profilesSha256", profileDigest);
            manifest.put("profilesSize", profileBytes.length);
            JSONArray assetArray = new JSONArray();
            for (BundleAsset asset : orderedAssets) {
                assetArray.put(asset.toJson());
            }
            manifest.put("assets", assetArray);
            manifest.put("assetBytes", collector.totalBytes);
        } catch (JSONException ex) {
            throw new Failure(ErrorCode.INVALID_FORMAT, "Profile bundle manifest", ex);
        }

        File completed = new File(jobDirectory, "completed" + FILE_EXTENSION);
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(
                new FileOutputStream(completed)))) {
            writeZipBytes(zip, MANIFEST_ENTRY,
                    manifest.toString(2).getBytes(StandardCharsets.UTF_8), request);
            writeZipBytes(zip, PROFILES_ENTRY, profileBytes, request);
            for (BundleAsset asset : orderedAssets) {
                writeZipFile(zip, asset.path(), asset.file, request);
            }
        } catch (IOException | JSONException ex) {
            throw new Failure(ErrorCode.STORAGE, "Profile bundle", ex);
        }
        request.throwIfCancelled();
        if (!completed.isFile() || completed.length() <= 0L
                || completed.length() > MAX_BUNDLE_BYTES) {
            throw new Failure(ErrorCode.TOO_LARGE, "Profile bundle");
        }
        copyToUri(context, completed, destination, request);
        return new ExportSummary(orderedAssets.size(), collector.totalBytes);
    }

    private static PreparedImport prepareImport(Context context, Uri source, Request request,
            File jobDirectory) throws Failure {
        if (source == null) {
            throw new Failure(ErrorCode.EMPTY, "Profile import");
        }
        File inputFile = new File(jobDirectory, "source.bin");
        copyFromUri(context, source, inputFile, request);
        if (!looksLikeZip(inputFile)) {
            String raw = readTextFile(inputFile, MAX_PROFILE_JSON_BYTES);
            try {
                List<GameProfile> profiles = ProfileStore.profilesFromJson(raw);
                return new PreparedImport(profiles, 0, 0L,
                        true, jobDirectory, Collections.emptyList());
            } catch (JSONException ex) {
                throw new Failure(ErrorCode.INVALID_FORMAT, "Legacy Profile JSON", ex);
            }
        }
        return prepareBundleImport(context, inputFile, jobDirectory, request);
    }

    private static PreparedImport prepareBundleImport(Context context, File inputFile,
            File jobDirectory, Request request) throws Failure {
        File stagedDirectory = new File(jobDirectory, "staged");
        if (!stagedDirectory.mkdirs()) {
            throw new Failure(ErrorCode.STORAGE, "Profile import staging");
        }
        try (ZipFile zip = new ZipFile(inputFile)) {
            Map<String, ZipEntry> entries = indexEntries(zip);
            JSONObject manifest = new JSONObject(readZipText(zip,
                    requireEntry(entries, MANIFEST_ENTRY), MAX_MANIFEST_BYTES));
            if (!FORMAT.equals(manifest.optString("format", ""))) {
                throw new Failure(ErrorCode.INVALID_FORMAT, "Profile bundle format");
            }
            int schemaVersion = manifest.optInt("schemaVersion", -1);
            if (schemaVersion != SCHEMA_VERSION) {
                throw new Failure(ErrorCode.UNSUPPORTED_VERSION,
                        String.valueOf(schemaVersion));
            }
            if (!PROFILES_ENTRY.equals(manifest.optString("profilesPath", ""))) {
                throw new Failure(ErrorCode.UNSAFE_PATH, "Profile data path");
            }
            long expectedProfileSize = manifest.optLong("profilesSize", -1L);
            String expectedProfileDigest = normalizeDigest(
                    manifest.optString("profilesSha256", ""));
            ZipEntry profileEntry = requireEntry(entries, PROFILES_ENTRY);
            File stagedProfiles = new File(stagedDirectory, PROFILES_ENTRY);
            extractEntry(zip, profileEntry, stagedProfiles,
                    MAX_PROFILE_JSON_BYTES, expectedProfileSize, expectedProfileDigest, request);

            JSONArray manifestAssets = manifest.optJSONArray("assets");
            if (manifestAssets == null || manifestAssets.length() > MAX_ASSET_COUNT) {
                throw new Failure(ErrorCode.INVALID_FORMAT, "Profile asset manifest");
            }
            Map<String, BundleAsset> assets = new LinkedHashMap<>();
            Set<String> expectedEntries = new HashSet<>();
            expectedEntries.add(MANIFEST_ENTRY);
            expectedEntries.add(PROFILES_ENTRY);
            long totalBytes = 0L;
            for (int i = 0; i < manifestAssets.length(); i++) {
                request.throwIfCancelled();
                JSONObject item = manifestAssets.optJSONObject(i);
                if (item == null) {
                    throw new Failure(ErrorCode.INVALID_FORMAT, "Profile asset manifest");
                }
                String sha256 = normalizeDigest(item.optString("sha256", ""));
                String id = normalizeDigest(item.optString("id", ""));
                String path = item.optString("path", "");
                java.util.regex.Matcher matcher = ASSET_PATH.matcher(path);
                if (!matcher.matches() || !sha256.equals(id)
                        || !sha256.equals(matcher.group(1))) {
                    throw new Failure(ErrorCode.UNSAFE_PATH, path);
                }
                if (assets.containsKey(sha256) || !expectedEntries.add(path)) {
                    throw new Failure(ErrorCode.CORRUPT, path);
                }
                long size = item.optLong("size", -1L);
                if (size <= 0L || size > MAX_ASSET_BYTES
                        || totalBytes + size > MAX_TOTAL_ASSET_BYTES) {
                    throw new Failure(ErrorCode.TOO_LARGE, path);
                }
                String extension = matcher.group(2);
                String mediaType = item.optString("mediaType", "");
                String displayName = ProfileAssetStore.sanitizeDisplayName(
                        item.optString("displayName", ""), sha256 + "." + extension);
                ZipEntry entry = requireEntry(entries, path);
                File staged = new File(stagedDirectory, sha256 + "." + extension);
                extractEntry(zip, entry, staged, MAX_ASSET_BYTES,
                        size, sha256, request);
                AssetType detected = detectType(staged, displayName, mediaType);
                if (!extension.equals(detected.extension)
                        || !mediaType.equals(detected.mimeType)) {
                    throw new Failure(ErrorCode.CORRUPT, displayName);
                }
                assets.put(sha256, new BundleAsset(
                        sha256, detected, displayName, staged, size));
                totalBytes += size;
            }
            if (!entries.keySet().equals(expectedEntries)) {
                throw new Failure(ErrorCode.UNSAFE_PATH, "Unexpected ZIP entry");
            }
            if (manifest.optLong("assetBytes", -1L) != totalBytes) {
                throw new Failure(ErrorCode.CORRUPT, "Profile asset byte count");
            }

            JSONObject profileRoot = new JSONObject(readTextFile(
                    stagedProfiles, MAX_PROFILE_JSON_BYTES));
            ImportRewrite rewrite = rewriteImportReferences(context, profileRoot, assets);
            if (!rewrite.usedAssetIds.equals(assets.keySet())) {
                throw new Failure(ErrorCode.CORRUPT, "Unreferenced Profile asset");
            }
            List<GameProfile> profiles = ProfileStore.profilesFromJson(profileRoot.toString());
            return new PreparedImport(profiles, assets.size(), totalBytes,
                    false, jobDirectory, rewrite.installTasks);
        } catch (Failure failure) {
            throw failure;
        } catch (JSONException ex) {
            throw new Failure(ErrorCode.INVALID_FORMAT, "Profile bundle JSON", ex);
        } catch (IOException ex) {
            if (isUnsafeZipPathFailure(ex)) {
                throw new Failure(ErrorCode.UNSAFE_PATH, "Unsafe ZIP entry", ex);
            }
            throw new Failure(ErrorCode.CORRUPT, "Profile bundle ZIP", ex);
        }
    }

    private static void rewriteExportReferences(Context context, JSONObject root,
            AssetCollector collector) throws JSONException, Failure {
        JSONArray profiles = root.optJSONArray("profiles");
        if (profiles == null || profiles.length() == 0) {
            throw new Failure(ErrorCode.EMPTY, "Profile data");
        }
        for (int profileIndex = 0; profileIndex < profiles.length(); profileIndex++) {
            JSONObject profile = profiles.optJSONObject(profileIndex);
            if (profile == null) {
                throw new Failure(ErrorCode.INVALID_FORMAT, "Profile " + profileIndex);
            }
            String profileName = profile.optString("name", "Profile");
            replaceExportUri(profile, "iconUri", AssetKind.PROFILE_ICON,
                    collector, profileName + " icon");
            replaceExportUri(profile, "mapUri", AssetKind.MAP,
                    collector, profileName + " map");

            JSONArray maps = profile.optJSONArray("maps");
            if (maps != null) {
                for (int i = 0; i < maps.length(); i++) {
                    JSONObject map = maps.optJSONObject(i);
                    if (map != null) {
                        replaceExportUri(map, "uri", AssetKind.MAP, collector,
                                map.optString("title", profileName + " map"));
                    }
                }
            }
            JSONArray guides = profile.optJSONArray("guides");
            if (guides != null) {
                for (int i = 0; i < guides.length(); i++) {
                    JSONObject guide = guides.optJSONObject(i);
                    if (guide != null && GuideEntry.TYPE_FILE.equals(
                            guide.optString("type", GuideEntry.TYPE_NOTE))) {
                        replaceExportUri(guide, "content", AssetKind.GUIDE, collector,
                                guide.optString("title", profileName + " guide"));
                    }
                }
            }
            JSONArray macros = profile.optJSONArray("macros");
            if (macros != null) {
                for (int i = 0; i < macros.length(); i++) {
                    JSONObject macro = macros.optJSONObject(i);
                    if (macro == null) {
                        continue;
                    }
                    String key = macro.optString("iconKey", "").trim();
                    if (key.startsWith("user:")) {
                        MacroIconRepository.MacroIconOption option =
                                MacroIconRepository.findByKey(context, key);
                        if (option == null || option.filePath == null) {
                            throw new Failure(ErrorCode.MISSING_ASSET,
                                    macro.optString("label", "Macro icon"));
                        }
                        String reference = collector.addFile(new File(option.filePath),
                                AssetKind.MACRO_ICON, new File(option.filePath).getName(),
                                "image/png");
                        macro.put("iconKey", reference);
                    } else if (key.length() > 0 && !key.startsWith("builtin:")
                            && !key.startsWith("asset:")) {
                        throw new Failure(ErrorCode.UNSUPPORTED_ASSET, key);
                    }
                }
            }
            JSONObject layout = profile.optJSONObject("widgetLayout");
            JSONArray items = layout == null ? null : layout.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item == null || !WidgetLayout.TYPE_CANVAS.equals(
                            item.optString("type", ""))) {
                        continue;
                    }
                    JSONObject canvas = item.optJSONObject("canvas");
                    String assetId = canvas == null ? "" : canvas.optString("assetId", "").trim();
                    if (assetId.length() == 0) {
                        continue;
                    }
                    File source = CanvasAssetStore.resolve(context, assetId);
                    if (source == null) {
                        throw new Failure(ErrorCode.MISSING_ASSET,
                                profileName + " Canvas " + (i + 1));
                    }
                    String reference = collector.addFile(source, AssetKind.CANVAS,
                            source.getName(), mimeFromExtension(extension(source.getName())));
                    canvas.put("assetId", reference);
                }
            }
        }
    }

    private static ImportRewrite rewriteImportReferences(Context context, JSONObject root,
            Map<String, BundleAsset> assets) throws JSONException, Failure {
        JSONArray profiles = root.optJSONArray("profiles");
        if (profiles == null || profiles.length() == 0) {
            throw new Failure(ErrorCode.EMPTY, "Profile data");
        }
        ImportRewrite rewrite = new ImportRewrite();
        for (int profileIndex = 0; profileIndex < profiles.length(); profileIndex++) {
            JSONObject profile = profiles.optJSONObject(profileIndex);
            if (profile == null) {
                throw new Failure(ErrorCode.INVALID_FORMAT, "Profile " + profileIndex);
            }
            replaceImportReference(context, profile, "iconUri", AssetKind.PROFILE_ICON,
                    assets, rewrite, true);
            replaceImportReference(context, profile, "mapUri", AssetKind.MAP,
                    assets, rewrite, true);
            JSONArray maps = profile.optJSONArray("maps");
            if (maps != null) {
                for (int i = 0; i < maps.length(); i++) {
                    JSONObject map = maps.optJSONObject(i);
                    if (map != null) {
                        replaceImportReference(context, map, "uri", AssetKind.MAP,
                                assets, rewrite, false);
                    }
                }
            }
            JSONArray guides = profile.optJSONArray("guides");
            if (guides != null) {
                for (int i = 0; i < guides.length(); i++) {
                    JSONObject guide = guides.optJSONObject(i);
                    if (guide != null && GuideEntry.TYPE_FILE.equals(
                            guide.optString("type", GuideEntry.TYPE_NOTE))) {
                        replaceImportReference(context, guide, "content", AssetKind.GUIDE,
                                assets, rewrite, false);
                    }
                }
            }
            JSONArray macros = profile.optJSONArray("macros");
            if (macros != null) {
                for (int i = 0; i < macros.length(); i++) {
                    JSONObject macro = macros.optJSONObject(i);
                    if (macro == null) {
                        continue;
                    }
                    String key = macro.optString("iconKey", "").trim();
                    if (isPlaceholder(key)) {
                        BundleAsset asset = requireAsset(assets, key);
                        requireSupported(AssetKind.MACRO_ICON,
                                new AssetType(asset.extension, asset.mimeType),
                                asset.file, asset.displayName);
                        String installed = "user:bundle_" + asset.sha256 + ".png";
                        macro.put("iconKey", installed);
                        rewrite.add(InstallKind.MACRO_ICON, asset, installed);
                    } else if (key.startsWith("user:") || (key.length() > 0
                            && !key.startsWith("builtin:") && !key.startsWith("asset:"))) {
                        throw new Failure(ErrorCode.CORRUPT, key);
                    }
                }
            }
            JSONObject layout = profile.optJSONObject("widgetLayout");
            JSONArray items = layout == null ? null : layout.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item == null || !WidgetLayout.TYPE_CANVAS.equals(
                            item.optString("type", ""))) {
                        continue;
                    }
                    JSONObject canvas = item.optJSONObject("canvas");
                    if (canvas == null) {
                        continue;
                    }
                    String value = canvas.optString("assetId", "").trim();
                    if (value.length() == 0) {
                        continue;
                    }
                    if (!isPlaceholder(value)) {
                        throw new Failure(ErrorCode.CORRUPT, "Canvas reference");
                    }
                    BundleAsset asset = requireAsset(assets, value);
                    requireSupported(AssetKind.CANVAS,
                            new AssetType(asset.extension, asset.mimeType),
                            asset.file, asset.displayName);
                    String installed = asset.sha256 + "." + asset.extension;
                    canvas.put("assetId", installed);
                    rewrite.add(InstallKind.CANVAS, asset, installed);
                }
            }
        }
        return rewrite;
    }

    private static void replaceExportUri(JSONObject object, String key, AssetKind kind,
            AssetCollector collector, String fallbackName) throws JSONException, Failure {
        String value = object.optString(key, "").trim();
        if (value.length() > 0) {
            object.put(key, collector.addReference(value, kind, fallbackName));
        }
    }

    private static void replaceImportReference(Context context, JSONObject object, String key,
            AssetKind kind, Map<String, BundleAsset> assets, ImportRewrite rewrite,
            boolean allowEmpty) throws JSONException, Failure {
        String value = object.optString(key, "").trim();
        if (value.length() == 0 && allowEmpty) {
            return;
        }
        if (!isPlaceholder(value)) {
            throw new Failure(ErrorCode.CORRUPT, key);
        }
        BundleAsset asset = requireAsset(assets, value);
        requireSupported(kind, new AssetType(asset.extension, asset.mimeType),
                asset.file, asset.displayName);
        String installed = ProfileAssetProvider.uriFor(context,
                asset.sha256 + "." + asset.extension, asset.displayName).toString();
        object.put(key, installed);
        rewrite.add(InstallKind.PROFILE_ASSET, asset, installed);
    }

    private static BundleAsset requireAsset(Map<String, BundleAsset> assets, String placeholder)
            throws Failure {
        String id = placeholder.substring(PLACEHOLDER_PREFIX.length()).toLowerCase(Locale.US);
        if (!id.matches("^[0-9a-f]{64}$")) {
            throw new Failure(ErrorCode.CORRUPT, placeholder);
        }
        BundleAsset asset = assets.get(id);
        if (asset == null) {
            throw new Failure(ErrorCode.MISSING_ASSET, id);
        }
        return asset;
    }

    private static final class ImportRewrite {
        final List<InstallTask> installTasks = new ArrayList<>();
        final Set<String> taskKeys = new HashSet<>();
        final Set<String> usedAssetIds = new HashSet<>();

        void add(InstallKind kind, BundleAsset asset, String expectedReference) {
            usedAssetIds.add(asset.sha256);
            String key = kind.name() + ":" + asset.sha256;
            if (taskKeys.add(key)) {
                installTasks.add(new InstallTask(kind, asset, expectedReference));
            }
        }
    }

    private static final class ExportSummary {
        final int assetCount;
        final long assetBytes;

        ExportSummary(int assetCount, long assetBytes) {
            this.assetCount = assetCount;
            this.assetBytes = assetBytes;
        }
    }

    private static void requireSupported(AssetKind kind, AssetType type, File file,
            String displayName) throws Failure {
        boolean supported;
        if (kind == AssetKind.PROFILE_ICON) {
            supported = type.mimeType.startsWith("image/");
        } else if (kind == AssetKind.MAP) {
            supported = type.mimeType.startsWith("image/")
                    || "application/pdf".equals(type.mimeType)
                    || "text/html".equals(type.mimeType);
        } else if (kind == AssetKind.GUIDE) {
            supported = type.mimeType.startsWith("image/")
                    || "application/pdf".equals(type.mimeType)
                    || type.mimeType.startsWith("text/");
        } else if (kind == AssetKind.MACRO_ICON) {
            try {
                MacroIconRepository.validateBundledIcon(file);
                supported = "png".equals(type.extension);
            } catch (IOException ex) {
                supported = false;
            }
        } else {
            try {
                CanvasAssetStore.validateBundledAsset(file, type.extension);
                supported = true;
            } catch (IOException ex) {
                supported = false;
            }
        }
        if (!supported) {
            throw new Failure(ErrorCode.UNSUPPORTED_ASSET, displayName);
        }
    }

    private static AssetType detectType(File file, String displayName, String declaredMime)
            throws Failure {
        byte[] header = new byte[12];
        int count;
        try (FileInputStream input = new FileInputStream(file)) {
            count = input.read(header);
        } catch (IOException ex) {
            throw new Failure(ErrorCode.STORAGE, displayName, ex);
        }
        if (count >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF && isImage(file, -1, -1)) {
            return new AssetType("jpg", "image/jpeg");
        }
        if (count >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50
                && header[2] == 0x4E && header[3] == 0x47 && isImage(file, -1, -1)) {
            return new AssetType("png", "image/png");
        }
        if (count >= 12 && asciiEquals(header, 0, "RIFF")
                && asciiEquals(header, 8, "WEBP") && isImage(file, -1, -1)) {
            return new AssetType("webp", "image/webp");
        }
        if (count >= 6 && asciiEquals(header, 0, "GIF8") && isImage(file, -1, -1)) {
            return new AssetType("gif", "image/gif");
        }
        if (count >= 5 && asciiEquals(header, 0, "%PDF-")) {
            return new AssetType("pdf", "application/pdf");
        }
        String extension = extension(displayName);
        String mime = declaredMime == null ? "" : declaredMime.trim().toLowerCase(Locale.US);
        if ("html".equals(extension) || "htm".equals(extension) || "text/html".equals(mime)) {
            requireUtf8Text(file, displayName);
            return new AssetType("htm".equals(extension) ? "htm" : "html", "text/html");
        }
        if ("md".equals(extension) || "markdown".equals(extension)
                || "text/markdown".equals(mime)) {
            requireUtf8Text(file, displayName);
            return new AssetType("md", "text/markdown");
        }
        if ("txt".equals(extension) || "text/plain".equals(mime)
                || (mime.startsWith("text/") && !"text/html".equals(mime))) {
            requireUtf8Text(file, displayName);
            return new AssetType("txt", "text/plain");
        }
        throw new Failure(ErrorCode.UNSUPPORTED_ASSET, displayName);
    }

    private static boolean isImage(File file, int exactWidth, int exactHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return false;
        }
        return (exactWidth <= 0 || options.outWidth == exactWidth)
                && (exactHeight <= 0 || options.outHeight == exactHeight);
    }

    private static void requireUtf8Text(File file, String displayName) throws Failure {
        try (Reader reader = new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT))) {
            char[] buffer = new char[8192];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                for (int i = 0; i < count; i++) {
                    if (buffer[i] == '\0') {
                        throw new Failure(ErrorCode.UNSUPPORTED_ASSET, displayName);
                    }
                }
            }
        } catch (Failure failure) {
            throw failure;
        } catch (IOException ex) {
            throw new Failure(ErrorCode.UNSUPPORTED_ASSET, displayName, ex);
        }
    }

    private static Map<String, ZipEntry> indexEntries(ZipFile zip) throws Failure {
        Map<String, ZipEntry> entries = new HashMap<>();
        Enumeration<? extends ZipEntry> enumeration = zip.entries();
        while (enumeration.hasMoreElements()) {
            ZipEntry entry = enumeration.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() || name == null || name.length() == 0
                    || name.startsWith("/") || name.contains("\\")
                    || name.contains("../") || name.contains("/..")) {
                throw new Failure(ErrorCode.UNSAFE_PATH, name);
            }
            if (entries.put(name, entry) != null) {
                throw new Failure(ErrorCode.CORRUPT, name);
            }
        }
        return entries;
    }

    private static ZipEntry requireEntry(Map<String, ZipEntry> entries, String name)
            throws Failure {
        ZipEntry entry = entries.get(name);
        if (entry == null) {
            throw new Failure(ErrorCode.MISSING_ASSET, name);
        }
        return entry;
    }

    private static void extractEntry(ZipFile zip, ZipEntry entry, File destination,
            long limit, long expectedSize, String expectedDigest, Request request) throws Failure {
        if (expectedSize <= 0L || expectedSize > limit
                || (entry.getSize() >= 0L && entry.getSize() != expectedSize)) {
            throw new Failure(ErrorCode.CORRUPT, entry.getName());
        }
        long written = 0L;
        try (InputStream input = new BufferedInputStream(zip.getInputStream(entry));
             FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                request.throwIfCancelled();
                written += read;
                if (written > limit || written > expectedSize) {
                    throw new Failure(ErrorCode.TOO_LARGE, entry.getName());
                }
                output.write(buffer, 0, read);
            }
            output.flush();
            output.getFD().sync();
        } catch (Failure failure) {
            destination.delete();
            throw failure;
        } catch (IOException ex) {
            destination.delete();
            throw new Failure(ErrorCode.CORRUPT, entry.getName(), ex);
        }
        try {
            if (written != expectedSize
                    || !expectedDigest.equals(ProfileAssetStore.sha256(destination))) {
                destination.delete();
                throw new Failure(ErrorCode.CORRUPT, entry.getName());
            }
        } catch (IOException ex) {
            destination.delete();
            throw new Failure(ErrorCode.CORRUPT, entry.getName(), ex);
        }
    }

    private static String readZipText(ZipFile zip, ZipEntry entry, int limit) throws Failure {
        try (InputStream input = zip.getInputStream(entry)) {
            return new String(readBytes(input, limit), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new Failure(ErrorCode.CORRUPT, entry.getName(), ex);
        }
    }

    private static String readTextFile(File file, int limit) throws Failure {
        try (FileInputStream input = new FileInputStream(file)) {
            return new String(readBytes(input, limit), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new Failure(ErrorCode.INVALID_FORMAT, file.getName(), ex);
        }
    }

    private static byte[] readBytes(InputStream input, int limit) throws IOException, Failure {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > limit) {
                throw new Failure(ErrorCode.TOO_LARGE, "Profile metadata");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static void writeZipBytes(ZipOutputStream zip, String name, byte[] bytes,
            Request request) throws IOException, Failure {
        request.throwIfCancelled();
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private static void writeZipFile(ZipOutputStream zip, String name, File file,
            Request request) throws IOException, Failure {
        ZipEntry entry = new ZipEntry(name);
        entry.setTime(0L);
        zip.putNextEntry(entry);
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                request.throwIfCancelled();
                zip.write(buffer, 0, read);
            }
        }
        zip.closeEntry();
    }

    private static void copyFromUri(Context context, Uri source, File destination,
            Request request) throws Failure {
        long total = 0L;
        try (InputStream input = context.getContentResolver().openInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) {
                throw new Failure(ErrorCode.EMPTY, "Profile import");
            }
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                request.throwIfCancelled();
                total += read;
                if (total > MAX_BUNDLE_BYTES) {
                    throw new Failure(ErrorCode.TOO_LARGE, "Profile import");
                }
                output.write(buffer, 0, read);
            }
            output.flush();
            output.getFD().sync();
        } catch (Failure failure) {
            destination.delete();
            throw failure;
        } catch (IOException | SecurityException ex) {
            destination.delete();
            throw new Failure(ErrorCode.STORAGE, "Profile import", ex);
        }
        if (total <= 0L) {
            destination.delete();
            throw new Failure(ErrorCode.EMPTY, "Profile import");
        }
    }

    private static void copyToUri(Context context, File source, Uri destination,
            Request request) throws Failure {
        try (FileInputStream input = new FileInputStream(source);
             OutputStream output = context.getContentResolver().openOutputStream(destination, "wt")) {
            if (output == null) {
                throw new Failure(ErrorCode.STORAGE, "Profile export destination");
            }
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                request.throwIfCancelled();
                output.write(buffer, 0, read);
            }
            output.flush();
        } catch (Failure failure) {
            throw failure;
        } catch (IOException | SecurityException ex) {
            throw new Failure(ErrorCode.STORAGE, "Profile export destination", ex);
        }
    }

    private static File createJobDirectory(Context context) throws Failure {
        cleanupOldJobs(context);
        File parent = new File(context.getCacheDir(), JOB_DIRECTORY);
        if (!parent.exists() && !parent.mkdirs()) {
            throw new Failure(ErrorCode.STORAGE, "Profile bundle cache");
        }
        File job = new File(parent, "job-" + UUID.randomUUID());
        if (!job.mkdirs()) {
            throw new Failure(ErrorCode.STORAGE, "Profile bundle job");
        }
        return job;
    }

    private static void cleanupOldJobs(Context context) {
        File parent = new File(context.getCacheDir(), JOB_DIRECTORY);
        File[] jobs = parent.listFiles();
        if (jobs == null) {
            return;
        }
        long cutoff = System.currentTimeMillis() - STALE_JOB_MS;
        for (File job : jobs) {
            if (job.isDirectory() && JOB_NAME.matcher(job.getName()).matches()
                    && job.lastModified() < cutoff) {
                deleteRecursively(job);
            }
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private static boolean looksLikeZip(File file) throws Failure {
        byte[] header = new byte[4];
        try (FileInputStream input = new FileInputStream(file)) {
            if (input.read(header) != 4) {
                return false;
            }
        } catch (IOException ex) {
            throw new Failure(ErrorCode.STORAGE, "Profile import", ex);
        }
        return header[0] == 'P' && header[1] == 'K'
                && ((header[2] == 3 && header[3] == 4)
                || (header[2] == 5 && header[3] == 6));
    }

    private static boolean isUnsafeZipPathFailure(IOException failure) {
        Throwable current = failure;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.US);
                if (normalized.contains("invalid zip entry path")
                        || normalized.contains("unsafe zip entry path")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static String displayName(Context context, Uri uri, String fallback) {
        String name = null;
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    name = cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
        }
        if ((name == null || name.trim().length() == 0) && uri != null) {
            name = uri.getLastPathSegment();
        }
        return ProfileAssetStore.sanitizeDisplayName(name, fallback);
    }

    private static String safeMime(Context context, Uri uri) {
        try {
            String value = context.getContentResolver().getType(uri);
            return value == null ? "" : value.trim().toLowerCase(Locale.US);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String placeholder(String sha256) {
        return PLACEHOLDER_PREFIX + sha256;
    }

    private static boolean isPlaceholder(String value) {
        return value != null && value.startsWith(PLACEHOLDER_PREFIX);
    }

    private static String normalizeDigest(String value) throws Failure {
        String digest = value == null ? "" : value.trim().toLowerCase(Locale.US);
        if (!digest.matches("^[0-9a-f]{64}$")) {
            throw new Failure(ErrorCode.CORRUPT, "Invalid checksum");
        }
        return digest;
    }

    private static String extension(String name) {
        String value = name == null ? "" : name.trim().toLowerCase(Locale.US);
        int query = value.indexOf('?');
        if (query >= 0) {
            value = value.substring(0, query);
        }
        int separator = value.lastIndexOf('.');
        return separator < 0 ? "" : value.substring(separator + 1);
    }

    private static String mimeFromExtension(String extension) {
        String value = extension == null ? "" : extension.toLowerCase(Locale.US);
        if ("jpg".equals(value) || "jpeg".equals(value)) {
            return "image/jpeg";
        }
        if ("png".equals(value)) {
            return "image/png";
        }
        if ("webp".equals(value)) {
            return "image/webp";
        }
        if ("gif".equals(value)) {
            return "image/gif";
        }
        if ("pdf".equals(value)) {
            return "application/pdf";
        }
        if ("html".equals(value) || "htm".equals(value)) {
            return "text/html";
        }
        if ("md".equals(value) || "markdown".equals(value)) {
            return "text/markdown";
        }
        if ("txt".equals(value)) {
            return "text/plain";
        }
        return "";
    }

    private static boolean asciiEquals(byte[] value, int offset, String expected) {
        for (int i = 0; i < expected.length(); i++) {
            if (offset + i >= value.length
                    || value[offset + i] != (byte) expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static String sha256(byte[] bytes) throws Failure {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(bytes);
            StringBuilder value = new StringBuilder(64);
            for (byte item : digest.digest()) {
                value.append(String.format(Locale.US, "%02x", item & 0xFF));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new Failure(ErrorCode.STORAGE, "SHA-256 unavailable", ex);
        }
    }
}
