package com.mastercook777.heimdall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.net.MalformedURLException;
import java.net.URL;

final class CommunityProfileCatalog {
    static final int VERSION = 1;
    static final int MAX_PROFILES = 200;
    static final int MAX_DOWNLOADS_PER_PROFILE = 8;
    static final long MAX_CATALOG_BYTES = 1024L * 1024L;
    static final long MAX_ICON_BYTES = 4L * 1024L * 1024L;
    static final long MAX_PREVIEW_BYTES = 8L * 1024L * 1024L;
    static final long MAX_SCREENSHOT_BYTES = 12L * 1024L * 1024L;
    static final long MAX_BUNDLE_BYTES = 512L * 1024L * 1024L;

    private static final Pattern ID = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    final int version;
    final List<Entry> profiles;

    private CommunityProfileCatalog(int version, List<Entry> profiles) {
        this.version = version;
        this.profiles = Collections.unmodifiableList(profiles);
    }

    static final class Entry {
        final String id;
        final String name;
        final String author;
        final String category;
        final String subtitle;
        final String description;
        final String iconUrl;
        final String iconSha256;
        final long iconSize;
        final String previewUrl;
        final String previewSha256;
        final long previewSize;
        final String screenshotUrl;
        final String screenshotSha256;
        final long screenshotSize;
        final int minVersionCode;
        final String minVersionName;
        final String sha256;
        final long size;
        final List<Download> downloads;

        Entry(String id, String name, String author, String category, String subtitle,
                String description, String iconUrl, String iconSha256, long iconSize,
                String previewUrl, String previewSha256, long previewSize,
                String screenshotUrl, String screenshotSha256, long screenshotSize,
                int minVersionCode, String minVersionName, String sha256, long size,
                List<Download> downloads) {
            this.id = id;
            this.name = name;
            this.author = author;
            this.category = category;
            this.subtitle = subtitle;
            this.description = description;
            this.iconUrl = iconUrl;
            this.iconSha256 = iconSha256;
            this.iconSize = iconSize;
            this.previewUrl = previewUrl;
            this.previewSha256 = previewSha256;
            this.previewSize = previewSize;
            this.screenshotUrl = screenshotUrl;
            this.screenshotSha256 = screenshotSha256;
            this.screenshotSize = screenshotSize;
            this.minVersionCode = minVersionCode;
            this.minVersionName = minVersionName;
            this.sha256 = sha256;
            this.size = size;
            this.downloads = Collections.unmodifiableList(downloads);
        }

        boolean supportsVersion(int versionCode) {
            return versionCode >= minVersionCode;
        }

        boolean hasIcon() {
            return iconUrl.length() > 0;
        }

        boolean hasScreenshot() {
            return screenshotUrl.length() > 0;
        }
    }

    static final class Download {
        final String source;
        final String url;

        Download(String source, String url) {
            this.source = source;
            this.url = url;
        }
    }

    static CommunityProfileCatalog parse(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw);
        int version = requiredInt(root, "version", 1, Integer.MAX_VALUE);
        if (version != VERSION) {
            throw new JSONException("Unsupported catalog version: " + version);
        }
        JSONArray items = root.optJSONArray("profiles");
        if (items == null || items.length() > MAX_PROFILES) {
            throw new JSONException("Invalid profiles array");
        }
        List<Entry> profiles = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < items.length(); index++) {
            JSONObject item = items.optJSONObject(index);
            if (item == null) {
                throw new JSONException("Invalid profile at " + index);
            }
            String id = requiredText(item, "id", 64);
            if (!ID.matcher(id).matches() || !ids.add(id)) {
                throw new JSONException("Invalid or duplicate profile id: " + id);
            }
            String name = requiredText(item, "name", 120);
            String author = requiredText(item, "author", 80);
            String category = requiredText(item, "category", 60);
            String subtitle = optionalText(item, "subtitle", 80);
            if (subtitle.length() == 0) {
                subtitle = category;
            }
            String description = requiredText(item, "description", 500);
            String iconUrl = optionalHttpsUrl(item, "icon");
            String iconSha256 = optionalDigest(item, "iconSha256");
            long iconSize = optionalLong(item, "iconSize", MAX_ICON_BYTES);
            validateOptionalImage("Icon", iconUrl, iconSha256, iconSize);
            String previewUrl = optionalHttpsUrl(item, "preview");
            String previewSha256 = optionalDigest(item, "previewSha256");
            long previewSize = optionalLong(item, "previewSize", MAX_PREVIEW_BYTES);
            validateOptionalImage("Preview", previewUrl, previewSha256, previewSize);
            String screenshotUrl = optionalHttpsUrl(item, "screenshot");
            String screenshotSha256 = optionalDigest(item, "screenshotSha256");
            long screenshotSize = optionalLong(item, "screenshotSize", MAX_SCREENSHOT_BYTES);
            validateOptionalImage("Screenshot", screenshotUrl, screenshotSha256, screenshotSize);
            int minVersionCode = requiredInt(item, "minVersionCode", 1,
                    Integer.MAX_VALUE);
            String minVersionName = optionalText(item, "minHeimdallVersion", 40);
            String sha256 = requiredDigest(item, "sha256");
            long size = requiredLong(item, "size", 1L, MAX_BUNDLE_BYTES);

            JSONArray downloadArray = item.optJSONArray("downloads");
            if (downloadArray == null || downloadArray.length() == 0
                    || downloadArray.length() > MAX_DOWNLOADS_PER_PROFILE) {
                throw new JSONException("Invalid downloads for " + id);
            }
            List<Download> downloads = new ArrayList<>();
            for (int downloadIndex = 0; downloadIndex < downloadArray.length();
                    downloadIndex++) {
                JSONObject download = downloadArray.optJSONObject(downloadIndex);
                if (download == null) {
                    throw new JSONException("Invalid download for " + id);
                }
                downloads.add(new Download(requiredText(download, "source", 40),
                        requiredHttpsUrl(download, "url")));
            }
            profiles.add(new Entry(id, name, author, category, subtitle, description,
                    iconUrl, iconSha256, iconSize,
                    previewUrl, previewSha256, previewSize,
                    screenshotUrl, screenshotSha256, screenshotSize, minVersionCode,
                    minVersionName, sha256, size, downloads));
        }
        return new CommunityProfileCatalog(version, profiles);
    }

    private static void validateOptionalImage(String label, String url, String sha256,
            long size) throws JSONException {
        if ((sha256.length() == 0) != (size == 0L)) {
            throw new JSONException(label + " checksum and size must be supplied together");
        }
        if (url.length() == 0 && (sha256.length() > 0 || size > 0L)) {
            throw new JSONException(label + " metadata requires a URL");
        }
    }

    private static String requiredText(JSONObject object, String name, int maxLength)
            throws JSONException {
        String value = optionalText(object, name, maxLength);
        if (value.length() == 0) {
            throw new JSONException("Missing " + name);
        }
        return value;
    }

    private static String optionalText(JSONObject object, String name, int maxLength)
            throws JSONException {
        Object raw = object.opt(name);
        if (raw == null || raw == JSONObject.NULL) {
            return "";
        }
        if (!(raw instanceof String)) {
            throw new JSONException("Invalid " + name);
        }
        String value = ((String) raw).trim();
        if (value.length() > maxLength) {
            throw new JSONException("Too long: " + name);
        }
        return value;
    }

    private static String requiredHttpsUrl(JSONObject object, String name)
            throws JSONException {
        String value = requiredText(object, name, 2048);
        validateHttpsUrl(value, name);
        return value;
    }

    private static String optionalHttpsUrl(JSONObject object, String name)
            throws JSONException {
        String value = optionalText(object, name, 2048);
        if (value.length() > 0) {
            validateHttpsUrl(value, name);
        }
        return value;
    }

    private static void validateHttpsUrl(String value, String name) throws JSONException {
        try {
            URL parsed = new URL(value);
            if (!"https".equalsIgnoreCase(parsed.getProtocol())
                    || parsed.getHost() == null || parsed.getHost().length() == 0) {
                throw new MalformedURLException("HTTPS host required");
            }
        } catch (MalformedURLException ex) {
            throw new JSONException("HTTPS required for " + name);
        }
    }

    private static String requiredDigest(JSONObject object, String name)
            throws JSONException {
        String value = optionalDigest(object, name);
        if (value.length() == 0) {
            throw new JSONException("Missing " + name);
        }
        return value;
    }

    private static String optionalDigest(JSONObject object, String name)
            throws JSONException {
        String value = optionalText(object, name, 64).toLowerCase(Locale.US);
        if (value.length() > 0 && !SHA256.matcher(value).matches()) {
            throw new JSONException("Invalid " + name);
        }
        return value;
    }

    private static int requiredInt(JSONObject object, String name, int min, int max)
            throws JSONException {
        Object value = object.opt(name);
        if (!(value instanceof Number)) {
            throw new JSONException("Invalid " + name);
        }
        long number = ((Number) value).longValue();
        if (number < min || number > max || ((Number) value).doubleValue() != number) {
            throw new JSONException("Invalid " + name);
        }
        return (int) number;
    }

    private static long requiredLong(JSONObject object, String name, long min, long max)
            throws JSONException {
        Object value = object.opt(name);
        if (!(value instanceof Number)) {
            throw new JSONException("Invalid " + name);
        }
        long number = ((Number) value).longValue();
        if (number < min || number > max || ((Number) value).doubleValue() != number) {
            throw new JSONException("Invalid " + name);
        }
        return number;
    }

    private static long optionalLong(JSONObject object, String name, long max)
            throws JSONException {
        if (!object.has(name)) {
            return 0L;
        }
        return requiredLong(object, name, 1L, max);
    }
}
