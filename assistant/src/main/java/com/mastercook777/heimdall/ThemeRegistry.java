package com.mastercook777.heimdall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ThemeRegistry {
    static final String ID_HEIMDALL_BLUE = "heimdall.blue";
    static final String ID_FREYA_WHITE = "freya.white";
    static final String LEGACY_DARK = "dark";
    static final String LEGACY_PEARL = "pearl";

    private static final List<ThemeDefinition> SELECTABLE;
    private static final Map<String, ThemeDefinition> BY_ID_OR_ALIAS;
    private static final ResolvedTheme DEFAULT_THEME;

    static {
        ThemeDefinition heimdallBlue = new ThemeDefinition(
                ID_HEIMDALL_BLUE,
                new String[]{LEGACY_DARK},
                ThemeFamily.HEIMDALL,
                new ThemePalette(
                        0xFF070A10, 0xFF0E141B,
                        0xCC101722, 0xD1172131, 0x990B1018,
                        0xFFE6EDF3, 0xFF9AA8B8, 0x8C9AA8B8,
                        0xFF2B3748, 0xFF3C4D63,
                        0xFF4EA1FF, 0xFF70B7FF, 0xFF4EA1FF, 0xFF8B5CFF,
                        0xFF3F9DFF,
                        0xFF4EA1FF, 0xFF70B7FF,
                        0x334E6074, 0xFF263449, 0xFF55B7E8,
                        0xAA6A9DDB, 0xF070B7FF,
                        0x3D55B7E8, 0x7755B7E8,
                        0x245A8DFF, 0x18FFFFFF, 0xCC70B7FF),
                new ThemeComponentColors(
                        0xE04EA1FF, 0xFFFFFFFF, 0xD0D8E6F2,
                        0x405F7C9A,
                        0xFF18212B, 0xFF354353,
                        0x00111824, 0x00080C12,
                        0x445F7C9A, 0x28445A72,
                        0x24000000, 0xFFD8A13A, 0xFFBFD0E2,
                        0xFFD9E8F8, 0xFF70B7FF,
                        0xFFFFFFFF, 0xCCB8C5D4, 0xFFE6EDF3,
                        0x66D27E82, 0xFFEA7175, 0xFFD27E82,
                        0xFF070A10, 0xFFD7EEFF, 0.45f,
                        0xFF05070A, 0xB8000000,
                        0x996A9DDB, 0xEE70B7FF,
                        0x665A8DFF, 0xCC9ED0FF, 0xE6E6EDF3,
                        0x145A8DFF, 0x244EA1FF,
                        0x886A9DDB, 0xEE70B7FF,
                        0x224EA1FF, 0xCC70B7FF,
                        0x1255B7E8, 0x775A8DFF, 0x996A9DDB,
                        0xAA70B7FF, 0xEE70B7FF, 0x554EA1FF,
                        0xFF7F91A6, 0xFF445A72, 0xFF8EA4BA,
                        0xFFD7EEFF, 0xFFD9E8F8, 0xFF70B7FF,
                        0xFFF4FAFF, 0x88414A53, 0xFF5FD18A, 0xFFFF6B6B,
                        new ThemeKeyboardColors(
                                0xF214171B, 0x665C6872, 0xFFE6EDF3,
                                0xFFE7B45B, 0x99D48A35, 0x884EA1FF,
                                0x5570B7FF, 0x5570B7FF,
                                0xFF70B7FF, 0xFF202A33),
                        0x334EA1FF, 0x664EA1FF, 0x884EA1FF,
                        0xDD4EA1FF, 0xFFFFFFFF,
                        new int[]{0xFF15243A, 0xFF241D3A, 0xFF192331,
                                0xFF17263B, 0xFF142A35, 0xFF101820, 0xFF142A22},
                        new int[]{0xCC1F4D78, 0xCC4A3278, 0xCC2C4B68,
                                0xCC1D5A3B, 0xCC20566A, 0xCC283B4C, 0xCC1D5A3B}),
                new SemanticStateColors(
                        appearance(0xFFE6EDF3, 0xB20F1622, 0xC9090E16,
                                0x665F7C9A, 0x33344150),
                        appearance(0xFF5FD18A, 0xA9121816, 0xC9090E0C,
                                0x885FD18A, 0x33406A50),
                        appearance(0xFFD8A13A, 0xA9161713, 0xC90B0C0D,
                                0x88D8A13A, 0x335F4822),
                        appearance(0xFFFF6B6B, 0x9A241117, 0xB00D080A,
                                0x99FF6B6B, 0x335F2A32),
                        appearance(0xFFFF6B7A, 0xB5281017, 0xCE10090C,
                                0xBBFF6B7A, 0x555A242D),
                        0x8C9AA8B8, 0xFFFF6B6B, 0xFFD8A13A),
                new ThemeMaterialSpec(ThemeFamily.HEIMDALL, false, false,
                        1, 2, 1, R.drawable.ic_heimdall_header_mark_blue),
                R.string.theme_heimdall_blue);

        ThemeDefinition freyaWhite = new ThemeDefinition(
                ID_FREYA_WHITE,
                new String[]{LEGACY_PEARL},
                ThemeFamily.FREYA,
                new ThemePalette(
                        0xFFE3E6E7, 0xFFE5E7E8,
                        0xFFEEF0EF, 0xFFF6F5F3, 0xFFD8DCDD,
                        0xFF263547, 0xFF596A7D, 0x8C596A7D,
                        0xFF9EABB8, 0xFF9EABB8,
                        0xFFF08A2A, 0xFFFFA044, 0xFFE77F1F, 0xFFFFA044,
                        0xFFF08A2A,
                        0xFFF08A2A, 0xFFF08A2A,
                        0x66929EAA, 0xFF8794A2, 0xFFF08A2A,
                        0xCC9AA7B4, 0xFFF08A2A,
                        0x339AA7B4, 0x66F08A2A,
                        0x24FFFFFF, 0x18818E9B, 0xFFF08A2A),
                new ThemeComponentColors(
                        0xE0F08A2A, 0xFF2D3C4E, 0xCC657386,
                        0x40657386,
                        0xFFE4E6E7, 0xFF9CA5AD,
                        0x00F8FAFC, 0x00E1E7ED,
                        0x287B8792, 0x207B8792,
                        0x12000000, 0xFF96500E, 0xFF536274,
                        0xFF536274, 0xFFE77F1F,
                        0xFF9B4C12, 0xCC697687, 0xFF344457,
                        0x668A5964, 0xFFA75159, 0xFF8A5964,
                        0xFFD4DCE3, 0xFF9B4C12, 0.55f,
                        0xFFD7DEE5, 0x8A3A4048,
                        0xCCF7F9FB, 0xFFF08A2A,
                        0xAAF7F9FB, 0xFFFFB05C, 0xFFF08A2A,
                        0x0FFFFFFF, 0x20F08A2A,
                        0xAAF08A2A, 0xFFF08A2A,
                        0x22F08A2A, 0xCCF08A2A,
                        0x18F08A2A, 0x88F7F9FB, 0xAAF08A2A,
                        0xC8F08A2A, 0xC8F08A2A, 0x38F08A2A,
                        0xFF788693, 0xFF909AA2, 0xFF5E6D7E,
                        0xFF344457, 0xFF596774, 0xFFF08A2A,
                        0xFFFEF4E8, 0x66717A82, 0xFF5FD18A, 0xFFFF6B6B,
                        new ThemeKeyboardColors(
                                0xFF454A50, 0xFF737A81, 0xFFE77F1F,
                                0xFFE77F1F, 0x99E77F1F, 0x88E77F1F,
                                0x55E77F1F, 0xB8E77F1F,
                                0xFFE77F1F, 0xFF555D65),
                        0x335D6975, 0x886D7B88, 0x886D7B88,
                        0xDDF08A2A, 0xFFFFFFFF,
                        new int[]{0xFFD6DEE6, 0xFFE3DFE8, 0xFFD9DEE4,
                                0xFFDCE3E9, 0xFFD6E1E3, 0xFFD4D9DE, 0xFFDCE5DF},
                        new int[]{0xFFE7E1D9, 0xFFE7E1D9, 0xFFE7E1D9,
                                0xFFE7E1D9, 0xFFE7E1D9, 0xFFE7E1D9, 0xFFE7E1D9}),
                new SemanticStateColors(
                        appearance(0xFF263547, 0x20FFFFFF, 0x20FFFFFF, 0, 0),
                        appearance(0xFF2F8B59, 0x2459A979, 0x2459A979, 0, 0),
                        appearance(0xFFC46B20, 0x26E77F1F, 0x26E77F1F, 0, 0),
                        appearance(0xFFB34A4F, 0x24C65C62, 0x24C65C62, 0, 0),
                        appearance(0xFFE0525C, 0x2CE0525C, 0x2CE0525C, 0, 0),
                        0x8C596A7D, 0xFFB34A4F, 0xFFC46B20),
                new ThemeMaterialSpec(ThemeFamily.FREYA, true, true,
                        6, 0, 0, R.drawable.ic_heimdall_header_mark_freya),
                R.string.theme_freya_white);

        List<ThemeDefinition> definitions = new ArrayList<>();
        definitions.add(heimdallBlue);
        definitions.add(freyaWhite);
        SELECTABLE = Collections.unmodifiableList(definitions);

        Map<String, ThemeDefinition> lookup = new LinkedHashMap<>();
        for (ThemeDefinition definition : SELECTABLE) {
            register(lookup, definition.id, definition);
            for (String alias : definition.legacyAliases) {
                register(lookup, alias, definition);
            }
        }
        BY_ID_OR_ALIAS = Collections.unmodifiableMap(lookup);
        DEFAULT_THEME = new ResolvedTheme(heimdallBlue);
    }

    private ThemeRegistry() {
    }

    static List<ThemeDefinition> selectableThemes() {
        return SELECTABLE;
    }

    static ResolvedTheme resolve(String storedId) {
        ThemeDefinition definition = BY_ID_OR_ALIAS.get(normalize(storedId));
        return definition == null ? DEFAULT_THEME : new ResolvedTheme(definition);
    }

    static String canonicalId(String storedId) {
        return resolve(storedId).id();
    }

    static boolean isKnown(String storedId) {
        return BY_ID_OR_ALIAS.containsKey(normalize(storedId));
    }

    private static SemanticStateColors.Appearance appearance(int foreground,
            int containerTop, int containerBottom, int edgeTop, int edgeBottom) {
        return new SemanticStateColors.Appearance(foreground, containerTop,
                containerBottom, edgeTop, edgeBottom);
    }

    private static void register(Map<String, ThemeDefinition> lookup, String key,
            ThemeDefinition definition) {
        String normalized = normalize(key);
        if (normalized.length() == 0 || lookup.put(normalized, definition) != null) {
            throw new IllegalStateException("Duplicate or empty theme id/alias: " + key);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
