package com.mastercook777.heimdall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ThemeRegistry {
    static final String ID_HEIMDALL_BLUE = "heimdall.blue";
    static final String ID_HEIMDALL_AMBER = "heimdall.amber";
    static final String ID_HEIMDALL_NOCTURNE = "heimdall.nocturne";
    static final String ID_FREYA_WHITE = "freya.white";
    static final String ID_FREYA_ROSEWOOD = "freya.rosewood";
    static final String ID_FREYA_CELADON = "freya.celadon";
    static final String LEGACY_DARK = "dark";
    static final String LEGACY_PEARL = "pearl";

    private static final ThemeMaterialSpec HEIMDALL_MATERIALS =
            new ThemeMaterialSpec(ThemeFamily.HEIMDALL, false, false,
                    1, 2, 1, R.drawable.ic_heimdall_header_mark_blue);
    private static final ThemeMaterialSpec FREYA_MATERIALS =
            new ThemeMaterialSpec(ThemeFamily.FREYA, true, true,
                    6, 0, 0, R.drawable.ic_heimdall_header_mark_freya);

    private static final List<ThemeDefinition> SELECTABLE;
    private static final Map<String, ThemeDefinition> BY_ID_OR_ALIAS;
    private static final ResolvedTheme DEFAULT_THEME;

    static {
        ThemeDefinition heimdallBlue = new ThemeDefinition(
                ID_HEIMDALL_BLUE,
                new String[]{LEGACY_DARK},
                ThemeFamily.HEIMDALL,
                0xFF1450B0,
                new ThemePalette(
                        0xFF070A10, 0xFF0E141B,
                        0xCC101722, 0xD1172131, 0x990B1018,
                        0xFF0F1622, 0xFF101824,
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
                        0x24000000, 0xFFD8A13A, 0xFFBFD0E2, 0xFF8ACAF0,
                        0xFFBDEBFF, 0xCCEFF9FF, 0xAAF4FAFF,
                        0xFFD9E8F8, 0xFF70B7FF,
                        0xFFFFFFFF, 0xCCB8C5D4, 0xFFE6EDF3,
                        0x66D27E82, 0xFFEA7175, 0xFFD27E82,
                        0xFF070A10, 0xFFD7EEFF, 0.45f,
                        0xFF05070A, 0xB8000000,
                        0x996A9DDB, 0xEE70B7FF,
                        0x665A8DFF, 0xCC9ED0FF, 0xE6E6EDF3,
                        0x145A8DFF, 0x244EA1FF,
                        0x2E000000,
                        0xFF0D1520, 0xFF0D1520, 0xFF0F1E2A, 0xFF0F1E2A,
                        0x886A9DDB, 0xEE70B7FF,
                        0x224EA1FF, 0xCC70B7FF,
                        0x1255B7E8, 0x775A8DFF, 0x996A9DDB,
                        0xAA70B7FF, 0xEE70B7FF, 0x554EA1FF,
                        0xFF7F91A6, 0xFF445A72, 0xFF8EA4BA,
                        0xFFD7EEFF,
                        0xFF58A6FF, 0xE6296FD6, 0xFF70B7FF, 0xE5162230, 0xFF445A72,
                        0xFFD9E8F8, 0xFF70B7FF,
                        0xFFF4FAFF, 0x88414A53, 0xFF5FD18A, 0xFFFF6B6B,
                        new ThemeKeyboardColors(
                                0xF214171B, 0x665C6872, 0xFFE6EDF3,
                                0xFFE7B45B, 0x99D48A35, 0x884EA1FF,
                                0x5570B7FF, 0x5570B7FF,
                                0xFF2B2D30, 0xFF17191C, 0xFF24364A, 0xFF101A26,
                                0x99717A82, 0x55323539, 0x557B848C, 0x88090B0D,
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
                HEIMDALL_MATERIALS,
                heimdallBlueGlassColors(),
                null,
                R.string.theme_heimdall_blue);

        ThemeDefinition heimdallAmber = new ThemeDefinition(
                ID_HEIMDALL_AMBER,
                new String[0],
                ThemeFamily.HEIMDALL,
                0xFFC2550A,
                new ThemePalette(
                        0xFF090B0D, 0xFF090B0D,
                        0xFF090B0D, 0xFF121619, 0xFF07090A,
                        0xFF101417, 0xFF0C1013,
                        0xFFEEECE6, 0xFFA5A59F, 0x8CA5A59F,
                        0xFF303538, 0xFF4B4B45,
                        0xFFD99A2B, 0xFFE7A436, 0xFFD99A2B, 0xFF725528,
                        0xFFE7A436,
                        0xFFD99A2B, 0xFFF0B84C,
                        0x33303538, 0xFF303538, 0xFFD99A2B,
                        0xAA725528, 0xF0F0B84C,
                        0x3D725528, 0x77D99A2B,
                        0x24725528, 0x18EEECE6, 0xCCF0B84C),
                new ThemeComponentColors(
                        0xE0D99A2B, 0xFFEEECE6, 0xD0A5A59F,
                        0x40303538,
                        0xFF101417, 0xFF303538,
                        0x00101417, 0x0007090A,
                        0x44303538, 0x28303538,
                        0x24000000, 0xFFD8A13A, 0xFFC7C3BA, 0xFFD99A2B,
                        0xFFF4C76E, 0xCCEEECE6, 0xAAF8F3E8,
                        0xFFD3D0C8, 0xFFD99A2B,
                        0xFFEEECE6, 0xCCA5A59F, 0xFFEEECE6,
                        0x66D27E82, 0xFFEA7175, 0xFFD27E82,
                        0xFF090B0D, 0xFFE7A436, 0.45f,
                        0xFF050607, 0xB8000000,
                        0x99725528, 0xEEF0B84C,
                        0x66725528, 0xCCE7A436, 0xE6F0B84C,
                        0x14725528, 0x24D99A2B,
                        0x2E000000,
                        0xFF101417, 0xFF0C1013, 0xFF101417, 0xFF0C1013,
                        0x88725528, 0xEEF0B84C,
                        0x22D99A2B, 0xCCF0B84C,
                        0x12725528, 0x77D99A2B, 0x99E7A436,
                        0xAAF0B84C, 0xEEE7A436, 0x55D99A2B,
                        0xFF7C817F, 0xFF3A3F42, 0xFFA5A59F,
                        0xFFEEECE6,
                        0xFFF0B84C, 0xE6D99A2B, 0xFFF0B84C, 0xE6101417, 0xFF4B4B45,
                        0xFF6F7371, 0xFFD99A2B,
                        0xFFFFF2D5, 0x88414A53, 0xFF5FD18A, 0xFFFF6B6B,
                        new ThemeKeyboardColors(
                                0xF2101417, 0x66303538, 0xFFF0B84C,
                                0xFFF0B84C, 0x99D99A2B, 0x88E7A436,
                                0x55F0B84C, 0x55F0B84C,
                                0xFF2A2D2E, 0xFF171A1B, 0xFF303334, 0xFF1B1F20,
                                0x994B4B45, 0x55303538, 0x554B4B45, 0x8807090A,
                                0xFFE7A436, 0xFF202426),
                        0x33D99A2B, 0x66725528, 0x88725528,
                        0xDDE7A436, 0xFFEEECE6,
                        new int[]{0xFF171A1B, 0xFF1C1811, 0xFF15191B,
                                0xFF181A16, 0xFF171B1B, 0xFF101214, 0xFF171A15},
                        new int[]{0xCC5B431C, 0xCC725528, 0xCC4C3A1C,
                                0xCC5A411D, 0xCC665026, 0xCC40341F, 0xCC574321}),
                new SemanticStateColors(
                        appearance(0xFFEEECE6, 0xB2101417, 0xC9090B0D,
                                0x66303538, 0x33303538),
                        appearance(0xFF5FD18A, 0xA9121816, 0xC9090E0C,
                                0x885FD18A, 0x33406A50),
                        appearance(0xFFD8A13A, 0xA9161713, 0xC90B0C0D,
                                0x88D8A13A, 0x335F4822),
                        appearance(0xFFFF6B6B, 0x9A241117, 0xB00D080A,
                                0x99FF6B6B, 0x335F2A32),
                        appearance(0xFFFF6B7A, 0xB5281017, 0xCE10090C,
                                0xBBFF6B7A, 0x555A242D),
                        0x8CA5A59F, 0xFFFF6B6B, 0xFFD8A13A),
                HEIMDALL_MATERIALS,
                heimdallAmberGlassColors(),
                null,
                R.string.theme_heimdall_amber);

        ThemeDefinition heimdallNocturne = new ThemeDefinition(
                ID_HEIMDALL_NOCTURNE,
                new String[0],
                ThemeFamily.HEIMDALL,
                0xFF318A42,
                new ThemePalette(
                        0xFF0E0C10, 0xFF0E0C10,
                        0xFF16131B, 0xFF231C29, 0xFF09080C,
                        0xFF1E1823, 0xFF131016,
                        0xFFF1EDF4, 0xFFAAA3B0, 0x8CAAA3B0,
                        0xFF33293C, 0xFF50385F,
                        0xFF809D74, 0xFF9BC089, 0xFF809D74, 0xFF4F5F4C,
                        0xFF9BC089,
                        0xFF809D74, 0xFFADD297,
                        0x3333293C, 0xFF33293C, 0xFF809D74,
                        0xAA557A4F, 0xF0ADD297,
                        0x3D4F5F4C, 0x77809D74,
                        0x244F5F4C, 0x18F1EDF4, 0xCCADD297),
                new ThemeComponentColors(
                        0xE0809D74, 0xFFF1EDF4, 0xD0AAA3B0,
                        0x4033293C,
                        0xFF1E1823, 0xFF33293C,
                        0x001E1823, 0x0009080C,
                        0x4433293C, 0x2833293C,
                        0x24000000, 0xFFD8A13A, 0xFFCFC9D1, 0xFF809D74,
                        0xFFADD297, 0xCCF1EDF4, 0xAAF8F4FA,
                        0xFF809D74, 0xFF809D74,
                        0xFFF1EDF4, 0xCCAAA3B0, 0xFFF1EDF4,
                        0x66D27E82, 0xFFEA7175, 0xFFD27E82,
                        0xFF0E0C10, 0xFF9BC089, 0.45f,
                        0xFF050607, 0xB8000000,
                        0x994F5F4C, 0xEEADD297,
                        0x664F5F4C, 0xCC9BC089, 0xE6ADD297,
                        0x144F5F4C, 0x24809D74,
                        0x2E000000,
                        0xFF1E1823, 0xFF131016, 0xFF1E1823, 0xFF131016,
                        0x884F5F4C, 0xEEADD297,
                        0x22809D74, 0xCCADD297,
                        0x12809D74, 0x77809D74, 0x999BC089,
                        0xAAADD297, 0xEE9BC089, 0x55809D74,
                        0xFF817987, 0xFF33293C, 0xFFAAA3B0,
                        0xFFF1EDF4,
                        0xFF9BC089, 0xE6809D74, 0xFFADD297, 0xE51E1823, 0xFF50385F,
                        0xFF817A86, 0xFF809D74,
                        0xFFEAF4E5, 0x88414A53, 0xFF5FD18A, 0xFFFF6B6B,
                        new ThemeKeyboardColors(
                                0xF21E1823, 0x6633293C, 0xFFADD297,
                                0xFFADD297, 0x99809D74, 0x889BC089,
                                0x55ADD297, 0x55ADD297,
                                0xFF231C29, 0xFF16131B, 0xFF2A2230, 0xFF19151E,
                                0x9950385F, 0x5533293C, 0x5550385F, 0x8809080C,
                                0xFF9BC089, 0xFF261E2B),
                        0x33809D74, 0x664F5F4C, 0x8850385F,
                        0xDD9BC089, 0xFFF1EDF4,
                        new int[]{0xFF211A28, 0xFF2A1F31, 0xFF201A26,
                                0xFF1C241F, 0xFF1A2320, 0xFF151117, 0xFF1B221C},
                        new int[]{0xCC3D4A39, 0xCC4F5F4C, 0xCC455640,
                                0xCC4F5F4C, 0xCC465A41, 0xCC394535, 0xCC465A41}),
                new SemanticStateColors(
                        appearance(0xFFF1EDF4, 0xB21E1823, 0xC909080C,
                                0x6650385F, 0x3333293C),
                        appearance(0xFF5FD18A, 0xA9121816, 0xC9090E0C,
                                0x885FD18A, 0x33406A50),
                        appearance(0xFFD8A13A, 0xA9161713, 0xC90B0C0D,
                                0x88D8A13A, 0x335F4822),
                        appearance(0xFFFF6B6B, 0x9A241117, 0xB00D080A,
                                0x99FF6B6B, 0x335F2A32),
                        appearance(0xFFFF6B7A, 0xB5281017, 0xCE10090C,
                                0xBBFF6B7A, 0x555A242D),
                        0x8CAAA3B0, 0xFFFF6B6B, 0xFFD8A13A),
                HEIMDALL_MATERIALS,
                heimdallNocturneGlassColors(),
                null,
                R.string.theme_heimdall_nocturne);

        ThemeDefinition freyaWhite = new ThemeDefinition(
                ID_FREYA_WHITE,
                new String[]{LEGACY_PEARL},
                ThemeFamily.FREYA,
                0xFFCF6818,
                new ThemePalette(
                        0xFFE3E6E7, 0xFFE5E7E8,
                        0xFFEEF0EF, 0xFFF6F5F3, 0xFFD8DCDD,
                        0xFFE5E8E9, 0xFFF8F8F6,
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
                        0x12000000, 0xFF96500E, 0xFF536274, 0xFFF08A2A,
                        0xFFFFC17A, 0xCCFFF4E8, 0xAAFFF5E8,
                        0xFF536274, 0xFFE77F1F,
                        0xFF9B4C12, 0xCC697687, 0xFF344457,
                        0x668A5964, 0xFFA75159, 0xFF8A5964,
                        0xFFD4DCE3, 0xFF9B4C12, 0.55f,
                        0xFFD7DEE5, 0x8A3A4048,
                        0xCCF7F9FB, 0xFFF08A2A,
                        0xAAF7F9FB, 0xFFFFB05C, 0xFFF08A2A,
                        0x0FFFFFFF, 0x20F08A2A,
                        0x22404A52,
                        0xFF717B81, 0xFF566168, 0xFF7C868C, 0xFF626C72,
                        0xAAF08A2A, 0xFFF08A2A,
                        0x22F08A2A, 0xCCF08A2A,
                        0x18F08A2A, 0x88F7F9FB, 0xAAF08A2A,
                        0xC8F08A2A, 0xC8F08A2A, 0x38F08A2A,
                        0xFF788693, 0xFF909AA2, 0xFF5E6D7E,
                        0xFF344457,
                        0xFFF08A2A, 0xE6F08A2A, 0xFFFFA044, 0xE5E5E8E9, 0xFF9EABB8,
                        0xFF596774, 0xFFF08A2A,
                        0xFFFEF4E8, 0x66717A82, 0xFF5FD18A, 0xFFFF6B6B,
                        new ThemeKeyboardColors(
                                0xFF454A50, 0xFF737A81, 0xFFE77F1F,
                                0xFFE77F1F, 0x99E77F1F, 0x88E77F1F,
                                0x55E77F1F, 0xB8E77F1F,
                                0xFFFBFAF8, 0xFFF5F4F1, 0xFFFBFAF8, 0xFFF5F4F1,
                                0x8A717B84, 0x55717B84, 0x55717B84, 0x88566168,
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
                FREYA_MATERIALS,
                null,
                freyaWhiteCncColors(),
                R.string.theme_freya_white);

        ThemeDefinition freyaRosewood = new ThemeDefinition(
                ID_FREYA_ROSEWOOD,
                new String[0],
                ThemeFamily.FREYA,
                0xFFA84C68,
                new ThemePalette(
                        0xFFF1E7E2, 0xFFF1E7E2,
                        0xFFF6EEEA, 0xFFFBF5F1, 0xFFD9CCC6,
                        0xFFE9DDD7, 0xFFFFF9F5,
                        0xFF49332D, 0xFF785E55, 0xFFA4938C,
                        0xFFB8A49B, 0xFF806055,
                        0xFFC65D7B, 0xFFD97993, 0xFFDFA1B2, 0xFFD97993,
                        0xFFD16A87,
                        0xFFC65D7B, 0xFFD97993,
                        0xFFD0BFB8, 0xFFA18479, 0xFFC65D7B,
                        0xCCA18479, 0xFFD97993,
                        0x33A18479, 0x66D97993,
                        0x24F6EEEA, 0x18785E55, 0xFFC65D7B),
                new ThemeComponentColors(
                        0xE0C65D7B, 0xFF49332D, 0xCC785E55,
                        0x40B8A49B,
                        0xFFF4ECE8, 0xFFB8A49B,
                        0x00FBF5F1, 0x00E9DDD7,
                        0x40D0BFB8, 0x28D0BFB8,
                        0x12000000, 0xFFB4772E, 0xFF76574D, 0xFFC65D7B,
                        0xFFD97993, 0xCCFBF5F1, 0xAAFDF8F5,
                        0xFF563B33, 0xFFD97993,
                        0xFFC65D7B, 0xCC785E55, 0xFF49332D,
                        0x66A4938C, 0xFFC8493E, 0xFF8E7D77,
                        0xFFD5C7C0, 0xFFC65D7B, 0.55f,
                        0xFFD5C7C0, 0x8A49332D,
                        0xCCA18479, 0xFFD97993,
                        0xAAA18479, 0xFFDFA1B2, 0xFFC65D7B,
                        0x0FF6EEEA, 0x20D97993,
                        0x2256443F,
                        0xFF806F69, 0xFF66544E, 0xFF806F69, 0xFF66544E,
                        0x66A18479, 0xFFD16A87,
                        0x22D97993, 0xCCD97993,
                        0x18D97993, 0x88F6EEEA, 0xAAC65D7B,
                        0xC8DE879F, 0xC8DE879F, 0x38D97993,
                        0xFFA18479, 0xFFA18479, 0xFF785E55,
                        0xFF49332D,
                        0xFFD97993, 0xE6C65D7B, 0xFFD97993, 0xE5E9DDD7, 0xFF806055,
                        0xFF76574D, 0xFFC65D7B,
                        0xFFFBF5F1, 0xFFFBF5F1, 0xFF3F8A66, 0xFFC8493E,
                        new ThemeKeyboardColors(
                                0xFF76574D, 0xFFA18479, 0xFF49332D,
                                0xFFDFA1B2, 0x99C65D7B, 0x88D97993,
                                0x55DFA1B2, 0xB8DE879F,
                                0xFFFFF9F5, 0xFFF6EBE6, 0xFFFFF9F5, 0xFFF6EBE6,
                                0x8A806055, 0x55806055, 0x55806055, 0x8866544E,
                                0xFFC65D7B, 0xFF563B33),
                        0x33A18479, 0x88B8A49B, 0x88B8A49B,
                        0xDDC65D7B, 0xFFFFFFFF,
                        new int[]{0xFFE8DDD8, 0xFFF1E7E2, 0xFFE9DDD7,
                                0xFFF4ECE8, 0xFFE5D8D2, 0xFFD9CCC6, 0xFFEDE3DE},
                        new int[]{0xFFF4DCE3, 0xFFF4DCE3, 0xFFF4DCE3,
                                0xFFF4DCE3, 0xFFF4DCE3, 0xFFF4DCE3, 0xFFF4DCE3}),
                new SemanticStateColors(
                        appearance(0xFF49332D, 0x20FBF5F1, 0x20FBF5F1, 0, 0),
                        appearance(0xFF3F8A66, 0x243F8A66, 0x243F8A66, 0, 0),
                        appearance(0xFFB4772E, 0x26B4772E, 0x26B4772E, 0, 0),
                        appearance(0xFFC8493E, 0x24C8493E, 0x24C8493E, 0, 0),
                        appearance(0xFFD43D4B, 0x2CD43D4B, 0x2CD43D4B, 0, 0),
                        0xFFA4938C, 0xFF8E7D77, 0xFFB4772E),
                FREYA_MATERIALS,
                null,
                freyaRosewoodCncColors(),
                R.string.theme_freya_rosewood);

        ThemeDefinition freyaCeladon = new ThemeDefinition(
                ID_FREYA_CELADON,
                new String[0],
                ThemeFamily.FREYA,
                0xFF248F62,
                new ThemePalette(
                        0xFFEEEAE3, 0xFFEEEAE3,
                        0xFFECE8E1, 0xFFF7F4EE, 0xFFD1D1C7,
                        0xFFE6E2DA, 0xFFE1DED7,
                        0xFF2E3431, 0xFF68716C, 0x8C68716C,
                        0xFFB7BEB9, 0xFF8F9A95,
                        0xFF6F947F, 0xFF557B67, 0xFF9EB4A8, 0xFF9EB4A8,
                        0xFF557B67,
                        0xFF6F947F, 0xFF416B57,
                        0x33949C97, 0xFF8F9A95, 0xFF6F947F,
                        0xAA829B8E, 0xFF416B57,
                        0x33829B8E, 0x666F947F,
                        0x24F7F4EE, 0x1868716C, 0xFF6F947F),
                new ThemeComponentColors(
                        0xE06F947F, 0xFF2E3431, 0xCC68716C,
                        0x40B7BEB9,
                        0xFFE6E2DA, 0xFFB7BEB9,
                        0x00F7F4EE, 0x00E6E2DA,
                        0x40B7BEB9, 0x28B7BEB9,
                        0x12000000, 0xFFB4772E, 0xFF68716C, 0xFF6F947F,
                        0xFF557B67, 0xCCF7F4EE, 0xAAF9F7F2,
                        0xFF4F5C56, 0xFF557B67,
                        0xFF557B67, 0xCC68716C, 0xFF2E3431,
                        0x6668716C, 0xFFC8493E, 0xFF68716C,
                        0xFFD1D1C7, 0xFF557B67, 0.55f,
                        0xFFD1D1C7, 0x8A2E3431,
                        0xCC829B8E, 0xFF557B67,
                        0xAA829B8E, 0xFF9EB4A8, 0xFF6F947F,
                        0x0FF7F4EE, 0x206F947F,
                        0x22545F5A,
                        0xFF71847A, 0xFF586A61, 0xFF71847A, 0xFF586A61,
                        0x66829B8E, 0xFF416B57,
                        0x226F947F, 0xCC557B67,
                        0x186F947F, 0x88F7F4EE, 0xAA6F947F,
                        0xC8557B67, 0xC8557B67, 0x386F947F,
                        0xFF8F9A95, 0xFFB7BEB9, 0xFF68716C,
                        0xFF2E3431,
                        0xFF557B67, 0xE66F947F, 0xFF557B67, 0xE5E6E2DA, 0xFF8F9A95,
                        0xFF68716C, 0xFF6F947F,
                        0xFFF7F4EE, 0x66717A75, 0xFF3F8A66, 0xFFC8493E,
                        new ThemeKeyboardColors(
                                0xFF68716C, 0xFF8F9A95, 0xFF2E3431,
                                0xFF9EB4A8, 0x996F947F, 0x88557B67,
                                0x559EB4A8, 0xB86F947F,
                                0xFFF7F4EE, 0xFFECE8E1, 0xFFF7F4EE, 0xFFECE8E1,
                                0x8A8F9A95, 0x55B7BEB9, 0x558F9A95, 0x88586A61,
                                0xFF557B67, 0xFF4F5C56),
                        0x33829B8E, 0x888F9A95, 0x888F9A95,
                        0xDD6F947F, 0xFFFFFFFF,
                        new int[]{0xFFDDE2DE, 0xFFE6E2DA, 0xFFD9DFDB,
                                0xFFE4E8E4, 0xFFD7E1DC, 0xFFD1D1C7, 0xFFDEE5E0},
                        new int[]{0xFFDCE8E1, 0xFFDCE8E1, 0xFFDCE8E1,
                                0xFFDCE8E1, 0xFFDCE8E1, 0xFFDCE8E1, 0xFFDCE8E1}),
                new SemanticStateColors(
                        appearance(0xFF2E3431, 0x20F7F4EE, 0x20F7F4EE, 0, 0),
                        appearance(0xFF3F8A66, 0x243F8A66, 0x243F8A66, 0, 0),
                        appearance(0xFFB4772E, 0x26B4772E, 0x26B4772E, 0, 0),
                        appearance(0xFFC8493E, 0x24C8493E, 0x24C8493E, 0, 0),
                        appearance(0xFFD43D4B, 0x2CD43D4B, 0x2CD43D4B, 0, 0),
                        0x8C68716C, 0xFF68716C, 0xFFB4772E),
                FREYA_MATERIALS,
                null,
                freyaCeladonCncColors(),
                R.string.theme_freya_celadon);

        List<ThemeDefinition> definitions = new ArrayList<>();
        definitions.add(heimdallBlue);
        definitions.add(heimdallAmber);
        definitions.add(heimdallNocturne);
        definitions.add(freyaWhite);
        definitions.add(freyaRosewood);
        definitions.add(freyaCeladon);
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

    private static ThemeGlassColors heimdallBlueGlassColors() {
        return new ThemeGlassColors(
                new ThemeGlassColors.Surface[]{
                        glassSurface(0xBB101722, 0xCC0B1018, 0x9962C6FF, 0xAA814CFF),
                        glassSurface(0xCC111824, 0xE6080C12, 0x6688A8C8, 0x44445062),
                        glassSurface(0x84101824, 0xA0080C12, 0x445F7C9A, 0x22344150),
                        glassSurface(0xA80F1620, 0xC7080C12, 0x665F7C9A, 0x30344150),
                        glassSurface(0xB20F1622, 0xC9090E16, 0x665F7C9A, 0x33344150),
                        glassSurface(0xC3111A28, 0xD7080D16, 0xCC62C6FF, 0x884EA1FF),
                        glassSurface(0xB20F1924, 0xC9080D14, 0x9962C6FF, 0x554A5666),
                        glassSurface(0xC4142740, 0xD709111D, 0xDD70B7FF, 0x774EA1FF),
                        glassSurface(0x850C111B, 0xAA070A10, 0x668B78C8, 0x30483F72),
                        glassSurface(0xA6101824, 0xC2070A10, 0x6688A8C8, 0x304E5A6A),
                        glassSurface(0xA6101824, 0xC2070A10, 0x966FC7FF, 0x5A4EA1FF),
                        glassSurface(0xCC10233F, 0xE6091226, 0xFF3F9DFF, 0xCC8B5CFF),
                        glassSurface(0x76101824, 0x96070A10, 0x445F7C9A, 0x22344150),
                        glassSurface(0x76101824, 0x96070A10, 0x445F7C9A, 0x22344150),
                        glassSurface(0xDD111824, 0xEE080C12, 0x6688A8C8, 0x44445062),
                        glassSurface(0xB20F1622, 0xD0090E16, 0x665F7C9A, 0x33344150),
                        glassSurface(0xA6111824, 0xC9080C12, 0x6688A8C8, 0x44445062),
                        glassSurface(0xD2141C27, 0xE80A0E14, 0x665F7C9A, 0x33344150),
                        glassSurface(0xFC0B111B, 0xFF06090E, 0xA66A829C, 0x55344150),
                        glassSurface(0xB5182330, 0xD00B1119, 0x776A829C, 0x33344150),
                        glassSurface(0xFA0B111B, 0xFF070A10, 0x886A829C, 0x44344150),
                        glassSurface(0xF00B111B, 0xFA070A10, 0x884EA1FF, 0x44344150),
                        glassSurface(0xB20C131D, 0xD2070B11, 0x555F7C9A, 0x33344150),
                        glassSurface(0xB20C131D, 0xD2070B11, 0x7770B7FF, 0x33445A72),
                        glassSurface(0xFF111824, 0xFF080C12, 0x6688A8C8, 0x44445062),
                        glassSurface(0xB2131B27, 0xD0080D14, 0xAA70B7FF, 0x55445A72),
                        glassSurface(0xA6101722, 0xC9080C12, 0x665F7C9A, 0x33344150),
                        glassSurface(0xB2111A26, 0xD0080D14, 0xAA70B7FF, 0x55445A72),
                        glassSurface(0xB2111A26, 0xD0080D14, 0xBB70B7FF, 0x55445A72),
                        glassSurface(0xD0121C29, 0xE0080D14, 0xAA70B7FF, 0x55445A72),
                        glassSurface(0xC00E1620, 0xD0070B11, 0x665F7184, 0x22344150),
                        glassSurface(0xE0182636, 0xED09121D, 0xCC70B7FF, 0x664EA1FF),
                        glassSurface(0xC3111A28, 0xD7080D16, 0xCC62C6FF, 0x884EA1FF),
                        glassSurface(0xD0142740, 0xE609111D, 0xCC70B7FF, 0x664EA1FF),
                        glassSurface(0xA9121A26, 0xB9080D14, 0x5570B7FF, 0x22344150),
                        glassSurface(0xFF0B1018, 0xFF0B1018, 0xFF2B3748, 0xFF2B3748)
                },
                new int[]{0xFF1B2536, 0xFF0A0D13, 0xFF030407},
                0x665A7494, 0x339AA8B8, 0x6670B7FF,
                new int[]{0xFF566171, 0xFF242A33, 0xFF0D1015},
                0x665A7494, 0xAA4EA1FF,
                new int[]{0xFF1B3247, 0xFF122638, 0xFF09141F},
                0x7044637D, 0x344D6478,
                new int[]{0x303E91D6, 0x1E2D70B8, 0x0C0C3C68});
    }

    private static ThemeGlassColors heimdallAmberGlassColors() {
        return new ThemeGlassColors(
                new ThemeGlassColors.Surface[]{
                        glassSurface(0xBB121619, 0xCC090B0D, 0x994B4B45, 0xAA303538),
                        glassSurface(0xCC121619, 0xE6090B0D, 0x884B4B45, 0x44303538),
                        glassSurface(0x840C1013, 0xA007090A, 0x444B4B45, 0x22303538),
                        glassSurface(0xA8101417, 0xC7090B0D, 0x66303538, 0x30303538),
                        glassSurface(0xB2101417, 0xC9090B0D, 0x66303538, 0x33303538),
                        glassSurface(0xC3101417, 0xD7090B0D, 0xCCE7A436, 0x88D99A2B),
                        glassSurface(0xB2101417, 0xC9090B0D, 0x99D99A2B, 0x55725528),
                        glassSurface(0xC4101417, 0xD7090B0D, 0xDDE7A436, 0x77725528),
                        glassSurface(0x850C1013, 0xAA07090A, 0x664B4B45, 0x30303538),
                        glassSurface(0xA6101417, 0xC207090A, 0x66303538, 0x30303538),
                        glassSurface(0xC0121619, 0xD00C1013, 0x88D99A2B, 0x44303538),
                        glassSurface(0xCC121619, 0xE6090B0D, 0xFFE7A436, 0xCC725528),
                        glassSurface(0x7607090A, 0x9607090A, 0x44303538, 0x22303538),
                        glassSurface(0x760C1013, 0x9607090A, 0x444B4B45, 0x22303538),
                        glassSurface(0xDD121619, 0xEE090B0D, 0x884B4B45, 0x44303538),
                        glassSurface(0xB2101417, 0xD00C1013, 0x66303538, 0x33303538),
                        glassSurface(0xA6121619, 0xC9090B0D, 0x884B4B45, 0x44303538),
                        glassSurface(0xD2121619, 0xE8090B0D, 0x66303538, 0x33303538),
                        glassSurface(0xFC0C1013, 0xFF07090A, 0xA64B4B45, 0x55303538),
                        glassSurface(0xB5101417, 0xD00C1013, 0x774B4B45, 0x33303538),
                        glassSurface(0xFA121619, 0xFF090B0D, 0x884B4B45, 0x44303538),
                        glassSurface(0xF0121619, 0xFA090B0D, 0x884B4B45, 0x44303538),
                        glassSurface(0xB20C1013, 0xD207090A, 0x55303538, 0x33303538),
                        glassSurface(0xB20C1013, 0xD207090A, 0x77725528, 0x33303538),
                        glassSurface(0xFF121619, 0xFF090B0D, 0x884B4B45, 0x44303538),
                        glassSurface(0xB2121619, 0xD0090B0D, 0xAA725528, 0x55303538),
                        glassSurface(0xA6101417, 0xC9090B0D, 0x66303538, 0x33303538),
                        glassSurface(0xB2121619, 0xD0090B0D, 0xAAE7A436, 0x55725528),
                        glassSurface(0xB2121619, 0xD0090B0D, 0xBBE7A436, 0x55725528),
                        glassSurface(0xD0101417, 0xE00C1013, 0xAAE7A436, 0x55725528),
                        glassSurface(0xC0101417, 0xD007090A, 0x66303538, 0x22303538),
                        glassSurface(0xE0121619, 0xED090B0D, 0xCCE7A436, 0x66725528),
                        glassSurface(0xC3101417, 0xD7090B0D, 0xCCE7A436, 0x88725528),
                        glassSurface(0xD0101417, 0xE6090B0D, 0xCCE7A436, 0x66725528),
                        glassSurface(0xA9121619, 0xB9090B0D, 0x55E7A436, 0x22303538),
                        glassSurface(0xFF0C1013, 0xFF0C1013, 0xFF303538, 0xFF303538)
                },
                new int[]{0xFF121619, 0xFF0C1013, 0xFF07090A},
                0x66303538, 0x334B4B45, 0x66725528,
                new int[]{0xFF2A2D2E, 0xFF171A1B, 0xFF0C1013},
                0xAA4B4B45, 0xAAF0B84C,
                new int[]{0xFF121619, 0xFF101417, 0xFF07090A},
                0x704B4B45, 0x34303538,
                new int[]{0x30F0B84C, 0x1ED99A2B, 0x0C725528});
    }

    private static ThemeGlassColors heimdallNocturneGlassColors() {
        return new ThemeGlassColors(
                new ThemeGlassColors.Surface[]{
                        glassSurface(0xBB231C29, 0xCC0E0C10, 0x9950385F, 0xAA33293C),
                        glassSurface(0xCC231C29, 0xE60E0C10, 0x8850385F, 0x4433293C),
                        glassSurface(0x84131016, 0xA009080C, 0x4450385F, 0x2233293C),
                        glassSurface(0xA81E1823, 0xC70E0C10, 0x6633293C, 0x3033293C),
                        glassSurface(0xB21E1823, 0xC909080C, 0x6633293C, 0x3333293C),
                        glassSurface(0xC31E1823, 0xD709080C, 0xCCADD297, 0x88809D74),
                        glassSurface(0xB21E1823, 0xC909080C, 0x99809D74, 0x554F5F4C),
                        glassSurface(0xC41E1823, 0xD709080C, 0xDDADD297, 0x774F5F4C),
                        glassSurface(0x85131016, 0xAA09080C, 0x6650385F, 0x3033293C),
                        glassSurface(0xA61E1823, 0xC209080C, 0x6633293C, 0x3033293C),
                        glassSurface(0xC0231C29, 0xD0131016, 0x88809D74, 0x4433293C),
                        glassSurface(0xCC231C29, 0xE60E0C10, 0xFF9BC089, 0xCC4F5F4C),
                        glassSurface(0x7609080C, 0x9609080C, 0x4433293C, 0x2233293C),
                        glassSurface(0x76131016, 0x9609080C, 0x4450385F, 0x2233293C),
                        glassSurface(0xDD231C29, 0xEE0E0C10, 0x8850385F, 0x4433293C),
                        glassSurface(0xB21E1823, 0xD0131016, 0x6633293C, 0x3333293C),
                        glassSurface(0xA6231C29, 0xC90E0C10, 0x8850385F, 0x4433293C),
                        glassSurface(0xD2231C29, 0xE80E0C10, 0x6633293C, 0x3333293C),
                        glassSurface(0xFC131016, 0xFF09080C, 0xA650385F, 0x5533293C),
                        glassSurface(0xB51E1823, 0xD0131016, 0x7750385F, 0x3333293C),
                        glassSurface(0xFA231C29, 0xFF0E0C10, 0x8850385F, 0x4433293C),
                        glassSurface(0xF0231C29, 0xFA0E0C10, 0x8850385F, 0x4433293C),
                        glassSurface(0xB2131016, 0xD209080C, 0x5533293C, 0x3333293C),
                        glassSurface(0xB2131016, 0xD209080C, 0x774F5F4C, 0x3333293C),
                        glassSurface(0xFF231C29, 0xFF0E0C10, 0x8850385F, 0x4433293C),
                        glassSurface(0xB2231C29, 0xD00E0C10, 0xAA4F5F4C, 0x5533293C),
                        glassSurface(0xA61E1823, 0xC90E0C10, 0x6633293C, 0x3333293C),
                        glassSurface(0xB2231C29, 0xD00E0C10, 0xAA9BC089, 0x554F5F4C),
                        glassSurface(0xB2231C29, 0xD00E0C10, 0xBB9BC089, 0x554F5F4C),
                        glassSurface(0xD01E1823, 0xE0131016, 0xAA9BC089, 0x554F5F4C),
                        glassSurface(0xC01E1823, 0xD009080C, 0x6633293C, 0x2233293C),
                        glassSurface(0xE0231C29, 0xED0E0C10, 0xCCADD297, 0x664F5F4C),
                        glassSurface(0xC31E1823, 0xD709080C, 0xCCADD297, 0x884F5F4C),
                        glassSurface(0xD01E1823, 0xE60E0C10, 0xCCADD297, 0x664F5F4C),
                        glassSurface(0xA9231C29, 0xB90E0C10, 0x559BC089, 0x2233293C),
                        glassSurface(0xFF131016, 0xFF131016, 0xFF33293C, 0xFF33293C)
                },
                new int[]{0xFF231C29, 0xFF131016, 0xFF09080C},
                0x6633293C, 0x3350385F, 0x664F5F4C,
                new int[]{0xFF302536, 0xFF1F1924, 0xFF131016},
                0xAA50385F, 0xAAADD297,
                new int[]{0xFF231C29, 0xFF1E1823, 0xFF09080C},
                0x7050385F, 0x3433293C,
                new int[]{0x30ADD297, 0x1E809D74, 0x0C4F5F4C});
    }

    private static ThemeGlassColors.Surface glassSurface(
            int top, int bottom, int edgeTop, int edgeBottom) {
        return new ThemeGlassColors.Surface(top, bottom, edgeTop, edgeBottom);
    }

    private static ThemeCncColors freyaWhiteCncColors() {
        int[] raisedShell = {0xFFF9FAF9, 0xFFBBC2C7, 0xFF717C85};
        int[] raisedRim = {0xFFFFFFFF, 0xFFF2F3F2, 0xFFB8C0C5};
        return new ThemeCncColors(
                cncSurface(0xFFEEF0EF, 0xFFF1F2F1, 0xFFE9ECEC,
                        0xFFF1F2F1, 0xFFE9ECEC, raisedShell, raisedRim, 0x50717C85),
                cncSurface(0xFFF6F5F3, 0xFFF7F6F4, 0xFFF0F1EF,
                        0xFFF1F1EF, 0xFFE9EAE8, raisedShell, raisedRim, 0x68717B84),
                cncSurface(0xFFD8DCDD, 0xFFD2D6D8, 0xFFE3E6E6,
                        0xFFD2D6D8, 0xFFE3E6E6,
                        new int[]{0xFF77818A, 0xFFAAB1B6, 0xFFF7F8F7},
                        new int[]{0xFF8A949C, 0xFFD7DBDD, 0xFFFFFFFF}, 0x68717B84),
                cncSurface(0xFFF3F3F1, 0xFFF6F6F4, 0xFFF0F1EF,
                        0xFFF1F1EF, 0xFFECEDEB,
                        new int[]{0xFFF8F9F8, 0xFFD1D5D7, 0xFF9FA8AE},
                        new int[]{0xFFFFFFFF, 0xFFF5F6F5, 0xFFCDD2D5}, 0x34747F87),
                cncSurface(0xFFE5E8E9, 0xFFE1E5E6, 0xFFE9ECEC,
                        0xFFE1E5E6, 0xFFE9ECEC,
                        new int[]{0xFFA0AAB1, 0xFFD5DADD, 0xFFF8F9F8},
                        new int[]{0xFFB6BEC4, 0xFFE7EAEA, 0xFFFFFFFF}, 0x30747F87),
                cncSurface(0xFFF0F1EF, 0xFFF2F3F1, 0xFFEEF0EE,
                        0xFFF2F3F1, 0xFFEEF0EE,
                        new int[]{0xFFCDD2D4, 0xFFE6E8E7, 0xFFF9FAF9},
                        new int[]{0xFFFFFFFF, 0xFFF5F6F4, 0xFFDDE1E1}, 0x24747F87),
                cncSurface(0xFFD8DCDD, 0xFFD2D6D8, 0xFFE3E6E6,
                        0xFFD2D6D8, 0xFFE3E6E6,
                        new int[]{0xFF6D7880, 0xFFADB5BA, 0xFFF9FAF9},
                        new int[]{0xFFFFFFFF, 0xFFFDFDFC, 0xFFC3CACF}, 0x68717B84),
                0x20FFFFFF, 0x36FFFFFF, 0x12AEB7C0,
                0x527B8792, 0x307B8792, 0x20FFFFFF,
                0xDDF6F5F3, 0xEEEEF0EF, 0xAAFFFFFF, 0x669EABB8);
    }

    private static ThemeCncColors freyaRosewoodCncColors() {
        int[] raisedShell = {0xFFFFF9F5, 0xFFCAB8B0, 0xFF806055};
        int[] raisedRim = {0xFFFFFDFC, 0xFFF7EEEA, 0xFFC4ADA4};
        return new ThemeCncColors(
                cncSurface(0xFFF6EEEA, 0xFFF8F1ED, 0xFFF1E7E2,
                        0xFFF8F1ED, 0xFFF1E7E2, raisedShell, raisedRim, 0x50806055),
                cncSurface(0xFFFBF5F1, 0xFFFFF9F5, 0xFFF6EBE6,
                        0xFFF4ECE8, 0xFFE8DDD8, raisedShell, raisedRim, 0x68806055),
                cncSurface(0xFFD9CCC6, 0xFFD5C7C0, 0xFFE3D8D3,
                        0xFFD5C7C0, 0xFFE3D8D3,
                        new int[]{0xFF806055, 0xFFB8A49B, 0xFFFBF5F1},
                        new int[]{0xFFA18479, 0xFFE8DDD8, 0xFFFFF9F5}, 0x68806055),
                cncSurface(0xFFF4ECE8, 0xFFF8F1ED, 0xFFEFE3DE,
                        0xFFEFE5E0, 0xFFE8DDD8,
                        new int[]{0xFFFFF9F5, 0xFFD0BFB8, 0xFFA18479},
                        new int[]{0xFFFFFDFC, 0xFFF4ECE8, 0xFFD9CCC6}, 0x34806055),
                cncSurface(0xFFE8DDD8, 0xFFE4D8D2, 0xFFECE2DD,
                        0xFFE4D8D2, 0xFFECE2DD,
                        new int[]{0xFFA18479, 0xFFD9CCC6, 0xFFFBF5F1},
                        new int[]{0xFFB8A49B, 0xFFE8DDD8, 0xFFFFF9F5}, 0x30806055),
                cncSurface(0xFFE9DDD7, 0xFFECE1DC, 0xFFE6D8D2,
                        0xFFECE1DC, 0xFFE6D8D2,
                        new int[]{0xFFD0BFB8, 0xFFE8DDD8, 0xFFFBF5F1},
                        new int[]{0xFFFFFDFC, 0xFFF4ECE8, 0xFFD9CCC6}, 0x24806055),
                cncSurface(0xFFD5C7C0, 0xFFCFC0B9, 0xFFDACDC7,
                        0xFFCFC0B9, 0xFFDACDC7,
                        new int[]{0xFF806055, 0xFFB8A49B, 0xFFFBF5F1},
                        new int[]{0xFFFFFDFC, 0xFFF8F0EC, 0xFFD0BFB8}, 0x68806055),
                0x20FBF5F1, 0x36F4DCE3, 0x12A18479,
                0x52B8A49B, 0x30A18479, 0x20FBF5F1,
                0xDDFBF5F1, 0xEEF6EEEA, 0xAAFFFDFC, 0x66B8A49B);
    }

    private static ThemeCncColors freyaCeladonCncColors() {
        int[] raisedShell = {0xFFFBF9F5, 0xFFC9CEC9, 0xFF8F9A95};
        int[] raisedRim = {0xFFFFFFFF, 0xFFF3F0EA, 0xFFB7BEB9};
        return new ThemeCncColors(
                cncSurface(0xFFECE8E1, 0xFFF0ECE5, 0xFFE6E2DA,
                        0xFFF0ECE5, 0xFFE6E2DA, raisedShell, raisedRim, 0x508F9A95),
                cncSurface(0xFFF7F4EE, 0xFFFBF9F5, 0xFFECE8E1,
                        0xFFF3F0E9, 0xFFE6E2DA, raisedShell, raisedRim, 0x688F9A95),
                cncSurface(0xFFD1D1C7, 0xFFCACCC5, 0xFFE0DED7,
                        0xFFCACCC5, 0xFFE0DED7,
                        new int[]{0xFF7F8C86, 0xFFB7BEB9, 0xFFF7F4EE},
                        new int[]{0xFF8F9A95, 0xFFE2E1DB, 0xFFFFFFFF}, 0x688F9A95),
                cncSurface(0xFFE2DFD7, 0xFFF0ECE5, 0xFFDDDAD2,
                        0xFFEAE7E0, 0xFFD8D7CF,
                        new int[]{0xFFF9F7F2, 0xFFD4D7D2, 0xFFA1AAA5},
                        new int[]{0xFFFFFFFF, 0xFFF5F2EC, 0xFFCBCFCB}, 0x348F9A95),
                cncSurface(0xFFE6E2DA, 0xFFE2DED6, 0xFFEAE7E0,
                        0xFFE2DED6, 0xFFEAE7E0,
                        new int[]{0xFFA5AEA9, 0xFFD8DAD6, 0xFFF7F4EE},
                        new int[]{0xFFBBC1BD, 0xFFE8E7E2, 0xFFFFFFFF}, 0x308F9A95),
                cncSurface(0xFFE1DED7, 0xFFE8E5DE, 0xFFDDDAD3,
                        0xFFE8E5DE, 0xFFDDDAD3,
                        new int[]{0xFFD1D4D0, 0xFFE7E5E0, 0xFFF9F7F2},
                        new int[]{0xFFFFFFFF, 0xFFF4F1EB, 0xFFD9DCD8}, 0x248F9A95),
                cncSurface(0xFFCDCEC6, 0xFFC8CAC3, 0xFFD8D8D0,
                        0xFFC8CAC3, 0xFFD8D8D0,
                        new int[]{0xFF71847A, 0xFFB7BEB9, 0xFFF7F4EE},
                        new int[]{0xFFFFFFFF, 0xFFF9F6F0, 0xFFD0D4D0}, 0x688F9A95),
                0x20F7F4EE, 0x36E4EBE7, 0x12829B8E,
                0x52B7BEB9, 0x308F9A95, 0x20F7F4EE,
                0xDDF7F4EE, 0xEEECE8E1, 0xAAFFFFFF, 0x66B7BEB9);
    }

    private static ThemeCncColors.Surface cncSurface(int roleColor,
            int faceTop, int faceBottom, int mutedFaceTop, int mutedFaceBottom,
            int[] shell, int[] rim, int keyline) {
        return new ThemeCncColors.Surface(roleColor, faceTop, faceBottom,
                mutedFaceTop, mutedFaceBottom, shell, rim, keyline);
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
