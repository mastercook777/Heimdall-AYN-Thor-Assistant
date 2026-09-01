package com.mastercook777.heimdall;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Positive-launch-only RetroArch context derived from bounded content-history updates. */
final class RetroArchGameContext {
    static final String PACKAGE_MAIN = "com.retroarch";
    static final String PACKAGE_AARCH64 = "com.retroarch.aarch64";
    static final String PACKAGE_RA32 = "com.retroarch.ra32";
    static final String DETECTOR_ID = "retroarch_history_positive_launch_v2";
    static final String KIND_CONTENT = "retroarch_content";
    static final String KIND_PLATFORM = "retroarch_platform";
    private static final long PROCESS_START_TOLERANCE_MS = 2_000L;
    private static final FolderPlatform[] FOLDER_PLATFORMS = new FolderPlatform[] {
            folderPlatform("panasonic_3do", "3DO", "3do"),
            folderPlatform("emerson_arcadia_2001", "Arcadia 2001", "arcadia"),
            folderPlatform("arduboy", "Arduboy", "arduboy"),
            folderPlatform("atari_2600", "Atari 2600", "atari2600"),
            folderPlatform("atari_5200", "Atari 5200", "atari5200"),
            folderPlatform("atari_7800", "Atari 7800", "atari7800"),
            folderPlatform("atari_jaguar", "Atari Jaguar", "atarijaguar"),
            folderPlatform("atari_jaguar_cd", "Atari Jaguar CD", "atarijaguarcd"),
            folderPlatform("atari_lynx", "Atari Lynx", "atarilynx"),
            folderPlatform("sega_atomiswave", "Atomiswave", "atomiswave"),
            folderPlatform("capcom_cps1", "Capcom Play System I", "cps1"),
            folderPlatform("capcom_cps2", "Capcom Play System II", "cps2"),
            folderPlatform("capcom_cps3", "Capcom Play System III", "cps3"),
            folderPlatform("epoch_cassette_vision", "Cassette Vision", "cassette"),
            folderPlatform("coleco_vision", "ColecoVision", "colecovision"),
            folderPlatform("fairchild_channel_f", "Fairchild Channel F", "channelf"),
            folderPlatform("finalburn_alpha", "FinalBurn Alpha",
                    "fba", "fbalpha", "finalburnalpha"),
            folderPlatform("finalburn_neo", "FinalBurn Neo", "fbneo", "finalburnneo"),
            folderPlatform("nintendo_game_and_watch", "Nintendo Game & Watch",
                    "gameandwatch"),
            folderPlatform("nintendo_game_boy", "Nintendo Game Boy", "gb", "gameboy"),
            folderPlatform("nintendo_game_boy_advance", "Nintendo Game Boy Advance",
                    "gba", "gameboyadvance", "nintendogameboyadvance"),
            folderPlatform("nintendo_game_boy_color", "Nintendo Game Boy Color",
                    "gbc", "gameboycolor"),
            folderPlatform("nintendo_gamecube", "Nintendo GameCube", "gc", "gamecube"),
            folderPlatform("mattel_intellivision", "Mattel Intellivision", "intellivision"),
            folderPlatform("interton_vc_4000", "Interton VC 4000", "vc4000"),
            folderPlatform("java_me", "Java ME", "j2me"),
            folderPlatform("arcade_mame", "Arcade (MAME)", "mame", "arcade"),
            folderPlatform("magnavox_odyssey_2", "Magnavox Odyssey 2",
                    "odyssey2", "videopac"),
            folderPlatform("welback_mega_duck", "Mega Duck", "megaduck", "cougarboy"),
            folderPlatform("snk_neo_geo", "SNK Neo Geo",
                    "neogeo", "snkneogeo", "neogeoaes", "neogeomvs"),
            folderPlatform("snk_neo_geo_cd", "SNK Neo Geo CD", "neogeocd"),
            folderPlatform("snk_neo_geo_pocket", "SNK Neo Geo Pocket", "ngp"),
            folderPlatform("snk_neo_geo_pocket_color", "SNK Neo Geo Pocket Color", "ngpc"),
            folderPlatform("nokia_ngage", "Nokia N-Gage", "ngage"),
            folderPlatform("nintendo_3ds", "Nintendo 3DS", "n3ds", "3ds"),
            folderPlatform("nintendo_64", "Nintendo 64", "n64", "nintendo64"),
            folderPlatform("nintendo_ds", "Nintendo DS", "nds", "nintendods"),
            folderPlatform("nintendo_ds", "Nintendo DSi", "ndsi", "nintendodsi"),
            folderPlatform("nintendo_nes", "Nintendo Entertainment System",
                    "nes", "famicom", "nintendoentertainmentsystem"),
            folderPlatform("nintendo_nes", "Famicom Disk System", "fds"),
            folderPlatform("nintendo_snes", "Nintendo Satellaview", "satellaview"),
            folderPlatform("nintendo_switch", "Nintendo Switch", "switch"),
            folderPlatform("nintendo_wii", "Nintendo Wii", "wii"),
            folderPlatform("nintendo_wii_u", "Nintendo Wii U", "wiiu"),
            folderPlatform("nintendo_wii", "Nintendo WiiWare", "wiiware"),
            folderPlatform("nintendo_snes", "Super Nintendo Entertainment System",
                    "sfc", "snes", "superfamicom", "supernintendo",
                    "supernintendoentertainmentsystem"),
            folderPlatform("nintendo_snes", "Super Nintendo Entertainment System",
                    "snesmsu1"),
            folderPlatform("nintendo_virtual_boy", "Nintendo Virtual Boy", "virtualboy"),
            folderPlatform("nintendo_pokemon_mini", "Pokemon Mini", "pokemini"),
            folderPlatform("philips_cd_i", "Philips CD-i", "cdimono1", "cdi"),
            folderPlatform("sony_psp", "Sony PlayStation Portable", "psp"),
            folderPlatform("sony_psp", "Sony PlayStation Portable", "pspminis"),
            folderPlatform("sony_playstation_vita", "Sony PlayStation Vita", "psvita"),
            folderPlatform("sony_playstation", "Sony PlayStation",
                    "psx", "ps1", "playstation"),
            folderPlatform("sony_playstation_2", "Sony PlayStation 2", "ps2"),
            folderPlatform("sony_playstation_3", "Sony PlayStation 3", "ps3"),
            folderPlatform("sega_32x", "Sega 32X", "sega32x", "32x"),
            folderPlatform("sega_cd", "Sega CD / Mega-CD", "segacd", "megacd"),
            folderPlatform("sega_game_gear", "Sega Game Gear", "gamegear", "gg"),
            folderPlatform("sega_mega_drive_genesis", "Sega Mega Drive / Genesis",
                    "genesis", "megadrive", "md"),
            folderPlatform("sega_mega_drive_genesis", "Sega Mega Drive / Genesis",
                    "genesismsu"),
            folderPlatform("sega_master_system", "Sega Master System",
                    "mastersystem", "sms"),
            folderPlatform("sega_naomi", "Sega Naomi", "naomi"),
            folderPlatform("sega_pico", "Sega Pico", "pico"),
            folderPlatform("sega_sg_1000", "Sega SG-1000", "sg1000"),
            folderPlatform("sega_saturn", "Sega Saturn", "saturn"),
            folderPlatform("sega_dreamcast", "Sega Dreamcast", "dreamcast"),
            folderPlatform("epoch_super_cassette_vision", "Super Cassette Vision",
                    "supercassette"),
            folderPlatform("nec_supergrafx", "NEC SuperGrafx", "supergrafx"),
            folderPlatform("nec_pc_engine", "NEC PC Engine / TurboGrafx-16",
                    "tg16", "pcengine", "pcengine16"),
            folderPlatform("nec_pc_engine_cd", "NEC PC Engine CD / TurboGrafx-CD",
                    "tgcd", "tg16cd", "pcenginecd", "pcecd"),
            folderPlatform("nec_pc_fx", "NEC PC-FX", "pcfx"),
            folderPlatform("bandai_wonderswan", "Bandai WonderSwan", "wonderswan", "ws"),
            folderPlatform("bandai_wonderswan_color", "Bandai WonderSwan Color",
                    "wonderswancolor", "wsc"),
            folderPlatform("watara_supervision", "Watara Supervision", "supervision"),
            folderPlatform("gce_vectrex", "Vectrex", "vectrex"),
            folderPlatform("uzebox", "Uzebox", "uzebox"),
            folderPlatform("zeebo", "Zeebo", "zeebo")
    };

    static final class LaunchRecord {
        final String contentPath;
        final String contentLabel;
        final String platformCode;
        final String platformLabel;
        final boolean platformAffectsContentIdentity;
        final long observedAt;

        LaunchRecord(String contentPath, String contentLabel, String platformCode,
                String platformLabel, boolean platformAffectsContentIdentity,
                long observedAt) {
            this.contentPath = safe(contentPath);
            this.contentLabel = safe(contentLabel);
            this.platformCode = safe(platformCode);
            this.platformLabel = safe(platformLabel);
            this.platformAffectsContentIdentity = platformAffectsContentIdentity;
            this.observedAt = observedAt;
        }

        boolean isValid() {
            return contentPath.length() > 0 && observedAt > 0L;
        }

        boolean hasPlatform() {
            return platformCode.length() > 0;
        }
    }

    private RetroArchGameContext() {
    }

    static boolean supportsPackage(String packageName) {
        return PACKAGE_MAIN.equals(packageName)
                || PACKAGE_AARCH64.equals(packageName)
                || PACKAGE_RA32.equals(packageName);
    }

    static String[] historyPathsForPackage(String packageName) {
        if (!supportsPackage(packageName)) return new String[0];
        return new String[] {
                "/storage/emulated/0/RetroArch/playlists/content_history.lpl",
                "/storage/emulated/0/RetroArch/playlists/builtin/content_history.lpl",
                "/storage/emulated/0/Android/data/" + packageName
                        + "/files/playlists/content_history.lpl"
        };
    }

    static boolean isLaunchEvidenceFresh(long historyModifiedAt, long processStartedAt) {
        return historyModifiedAt > 0L
                && processStartedAt > 0L
                && historyModifiedAt + PROCESS_START_TOLERANCE_MS >= processStartedAt;
    }

    static LaunchRecord parseHistory(String json, long observedAt) {
        try {
            if (json == null || json.length() == 0 || observedAt <= 0L) return null;
            JSONObject root = new JSONObject(json);
            JSONArray items = root.optJSONArray("items");
            if (items == null || items.length() == 0) return null;
            JSONObject newest = items.optJSONObject(0);
            if (newest == null) return null;
            String contentPath = safe(newest.optString("path", "")).trim();
            if (!isUsableContentPath(contentPath)) return null;
            String label = safeDisplayLabel(newest.optString("label", ""));
            if (label.length() == 0 || "DETECT".equalsIgnoreCase(label)) {
                label = displayNameFromPath(contentPath);
            }
            PlatformResolution platform = resolvePlatform(
                    newest.optString("db_name", ""), contentPath);
            return new LaunchRecord(contentPath, label,
                    platform == null ? "" : platform.platform.code,
                    platform == null ? "" : platform.platform.label,
                    platform != null && platform.affectsContentIdentity, observedAt);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static GameContextSnapshot snapshot(String packageName, int pid, LaunchRecord record) {
        try {
            if (!supportsPackage(packageName) || pid <= 0
                    || record == null || !record.isValid()) {
                return GameContextSnapshot.UNKNOWN;
            }
            String contentBasis = normalizeContentPath(record.contentPath);
            String platformBasis = record.hasPlatform()
                    && record.platformAffectsContentIdentity
                    ? record.platformCode : "unknown";
            String identity = sha256("retroarch-content\n" + platformBasis
                    + "\n" + contentBasis);
            String platformIdentity = record.hasPlatform()
                    ? sha256("retroarch-platform\n" + record.platformCode) : "";
            return new GameContextSnapshot(GameContextSnapshot.State.ACTIVE,
                    packageName, pid, KIND_CONTENT, identity, record.contentLabel,
                    record.observedAt, DETECTOR_ID, KIND_PLATFORM, platformIdentity,
                    record.platformLabel);
        } catch (Throwable ignored) {
            return GameContextSnapshot.UNKNOWN;
        }
    }

    private static PlatformResolution resolvePlatform(String databaseName, String contentPath) {
        String database = normalizeDatabaseName(databaseName);
        Platform mapped = platformFromDatabase(database);
        if (mapped != null) return new PlatformResolution(mapped, true);
        if (database.length() > 0) {
            return new PlatformResolution(new Platform(
                    "database:" + database.toLowerCase(Locale.ROOT), database), true);
        }
        mapped = platformFromExtension(contentPath);
        if (mapped != null) return new PlatformResolution(mapped, true);
        mapped = platformFromFolder(contentPath);
        return mapped == null ? null : new PlatformResolution(mapped, false);
    }

    private static Platform platformFromDatabase(String database) {
        String normalized = database.toLowerCase(Locale.ROOT);
        if ("nintendo - game boy advance".equals(normalized)) {
            return new Platform("nintendo_game_boy_advance", "Nintendo Game Boy Advance");
        }
        if ("nintendo - game boy color".equals(normalized)) {
            return new Platform("nintendo_game_boy_color", "Nintendo Game Boy Color");
        }
        if ("nintendo - game boy".equals(normalized)) {
            return new Platform("nintendo_game_boy", "Nintendo Game Boy");
        }
        if ("nintendo - nintendo entertainment system".equals(normalized)) {
            return new Platform("nintendo_nes", "Nintendo Entertainment System");
        }
        if ("nintendo - super nintendo entertainment system".equals(normalized)) {
            return new Platform("nintendo_snes", "Super Nintendo Entertainment System");
        }
        if ("nintendo - nintendo 64".equals(normalized)) {
            return new Platform("nintendo_64", "Nintendo 64");
        }
        if ("nintendo - nintendo ds".equals(normalized)) {
            return new Platform("nintendo_ds", "Nintendo DS");
        }
        if ("sega - mega drive - genesis".equals(normalized)) {
            return new Platform("sega_mega_drive_genesis", "Sega Mega Drive / Genesis");
        }
        return null;
    }

    private static Platform platformFromExtension(String contentPath) {
        String path = contentPath.toLowerCase(Locale.ROOT);
        int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);
        int fragment = path.indexOf('#');
        if (fragment >= 0) path = path.substring(0, fragment);
        if (hasExtension(path, ".gba")) {
            return new Platform("nintendo_game_boy_advance", "Nintendo Game Boy Advance");
        }
        if (hasExtension(path, ".gbc")) {
            return new Platform("nintendo_game_boy_color", "Nintendo Game Boy Color");
        }
        if (hasExtension(path, ".gb")) {
            return new Platform("nintendo_game_boy", "Nintendo Game Boy");
        }
        if (hasExtension(path, ".nes") || hasExtension(path, ".fds")) {
            return new Platform("nintendo_nes", "Nintendo Entertainment System");
        }
        if (hasExtension(path, ".sfc") || hasExtension(path, ".smc")) {
            return new Platform("nintendo_snes", "Super Nintendo Entertainment System");
        }
        if (hasExtension(path, ".n64") || hasExtension(path, ".z64")
                || hasExtension(path, ".v64")) {
            return new Platform("nintendo_64", "Nintendo 64");
        }
        if (hasExtension(path, ".nds")) {
            return new Platform("nintendo_ds", "Nintendo DS");
        }
        if (hasExtension(path, ".md") || hasExtension(path, ".gen")
                || hasExtension(path, ".smd")) {
            return new Platform("sega_mega_drive_genesis", "Sega Mega Drive / Genesis");
        }
        if (hasExtension(path, ".sms")) {
            return new Platform("sega_master_system", "Sega Master System");
        }
        if (hasExtension(path, ".gg")) {
            return new Platform("sega_game_gear", "Sega Game Gear");
        }
        if (hasExtension(path, ".pce") || hasExtension(path, ".sgx")) {
            return new Platform("nec_pc_engine", "NEC PC Engine / TurboGrafx-16");
        }
        if (hasExtension(path, ".ws")) {
            return new Platform("bandai_wonderswan", "Bandai WonderSwan");
        }
        if (hasExtension(path, ".wsc")) {
            return new Platform("bandai_wonderswan_color", "Bandai WonderSwan Color");
        }
        if (hasExtension(path, ".ngp")) {
            return new Platform("snk_neo_geo_pocket", "SNK Neo Geo Pocket");
        }
        if (hasExtension(path, ".ngc")) {
            return new Platform("snk_neo_geo_pocket_color", "SNK Neo Geo Pocket Color");
        }
        if (hasExtension(path, ".a26")) {
            return new Platform("atari_2600", "Atari 2600");
        }
        if (hasExtension(path, ".a78")) {
            return new Platform("atari_7800", "Atari 7800");
        }
        return null;
    }

    private static Platform platformFromFolder(String contentPath) {
        String path = Uri.decode(safe(contentPath)).replace('\\', '/');
        int query = path.indexOf('?');
        if (query >= 0) path = path.substring(0, query);
        int fragment = path.indexOf('#');
        if (fragment >= 0) path = path.substring(0, fragment);
        String[] segments = path.split("/+", -1);
        for (int index = segments.length - 2; index >= 0; index--) {
            String folder = normalizeFolderAlias(segments[index]);
            Platform platform = platformFromFolderAlias(folder);
            if (platform != null) return platform;
        }
        return null;
    }

    private static Platform platformFromFolderAlias(String folder) {
        for (FolderPlatform candidate : FOLDER_PLATFORMS) {
            if (candidate.matches(folder)) {
                return candidate.platform;
            }
        }
        return null;
    }

    private static FolderPlatform folderPlatform(String code, String label,
            String... aliases) {
        return new FolderPlatform(new Platform(code, label), aliases);
    }

    private static String normalizeFolderAlias(String value) {
        String lower = safe(value).trim().toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder(lower.length());
        for (int index = 0; index < lower.length(); index++) {
            char item = lower.charAt(index);
            if (Character.isLetterOrDigit(item)) normalized.append(item);
        }
        return normalized.toString();
    }

    private static boolean hasExtension(String path, String extension) {
        return path.endsWith(extension) || path.endsWith(extension + ".zip");
    }

    private static boolean isUsableContentPath(String value) {
        return value.length() > 0
                && !"DETECT".equalsIgnoreCase(value)
                && !"N/A".equalsIgnoreCase(value)
                && !value.startsWith("builtin:");
    }

    private static String normalizeDatabaseName(String value) {
        String clean = safeDisplayLabel(value);
        if ("DETECT".equalsIgnoreCase(clean) || "N/A".equalsIgnoreCase(clean)) return "";
        int slash = Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < clean.length()) clean = clean.substring(slash + 1);
        if (clean.toLowerCase(Locale.ROOT).endsWith(".lpl")) {
            clean = clean.substring(0, clean.length() - 4);
        }
        return clean.trim();
    }

    private static String displayNameFromPath(String contentPath) {
        String value = contentPath;
        int query = value.indexOf('?');
        if (query >= 0) value = value.substring(0, query);
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        if (slash >= 0 && slash + 1 < value.length()) value = value.substring(slash + 1);
        return safeDisplayLabel(Uri.decode(value));
    }

    private static String normalizeContentPath(String value) {
        return safe(value).trim().replace('\\', '/');
    }

    private static String safeDisplayLabel(String value) {
        String source = safe(value).trim();
        StringBuilder result = new StringBuilder(Math.min(120, source.length()));
        for (int i = 0; i < source.length() && result.length() < 120; i++) {
            char item = source.charAt(i);
            if (!Character.isISOControl(item)) result.append(item);
        }
        return result.toString().trim();
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte item : digest) {
            result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
        }
        return result.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class Platform {
        final String code;
        final String label;

        Platform(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    private static final class FolderPlatform {
        final Platform platform;
        final String[] aliases;

        FolderPlatform(Platform platform, String[] aliases) {
            this.platform = platform;
            this.aliases = aliases;
        }

        boolean matches(String folder) {
            for (String alias : aliases) {
                if (alias.equals(folder)) return true;
            }
            return false;
        }
    }

    private static final class PlatformResolution {
        final Platform platform;
        final boolean affectsContentIdentity;

        PlatformResolution(Platform platform, boolean affectsContentIdentity) {
            this.platform = platform;
            this.affectsContentIdentity = affectsContentIdentity;
        }
    }
}
