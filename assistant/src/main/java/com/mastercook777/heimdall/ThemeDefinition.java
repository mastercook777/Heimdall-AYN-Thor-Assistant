package com.mastercook777.heimdall;

final class ThemeDefinition {
    final String id;
    final String[] legacyAliases;
    final ThemeFamily family;
    final int hardwareAccent;
    final ThemePalette palette;
    final ThemeComponentColors componentColors;
    final SemanticStateColors semanticStates;
    final ThemeMaterialSpec materials;
    final ThemeGlassColors glassColors;
    final ThemeCncColors cncColors;
    final int displayNameRes;

    ThemeDefinition(String id, String[] legacyAliases, ThemeFamily family,
            int hardwareAccent,
            ThemePalette palette, ThemeComponentColors componentColors,
            SemanticStateColors semanticStates,
            ThemeMaterialSpec materials, ThemeGlassColors glassColors,
            ThemeCncColors cncColors,
            int displayNameRes) {
        if (id == null || id.trim().length() == 0) {
            throw new IllegalArgumentException("Theme id must not be empty");
        }
        if (family == null || palette == null || componentColors == null
                || semanticStates == null || materials == null) {
            throw new IllegalArgumentException("Theme definition parts must not be null");
        }
        if (materials.family != family) {
            throw new IllegalArgumentException("Theme family and material family must match");
        }
        if ((family == ThemeFamily.FREYA) != (cncColors != null)) {
            throw new IllegalArgumentException("Only Freya definitions provide CNC colors");
        }
        if ((family == ThemeFamily.HEIMDALL) != (glassColors != null)) {
            throw new IllegalArgumentException("Only Heimdall definitions provide glass colors");
        }
        this.id = id;
        this.legacyAliases = legacyAliases.clone();
        this.family = family;
        this.hardwareAccent = hardwareAccent;
        this.palette = palette;
        this.componentColors = componentColors;
        this.semanticStates = semanticStates;
        this.materials = materials;
        this.glassColors = glassColors;
        this.cncColors = cncColors;
        this.displayNameRes = displayNameRes;
    }
}
