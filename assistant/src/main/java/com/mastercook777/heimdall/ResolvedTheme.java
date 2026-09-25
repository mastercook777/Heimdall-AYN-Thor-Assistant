package com.mastercook777.heimdall;

/** Immutable theme resolved from one stable registry definition. */
final class ResolvedTheme {
    final ThemeDefinition definition;
    final int hardwareAccent;
    final ThemePalette palette;
    final ThemeComponentColors componentColors;
    final SemanticStateColors semanticStates;
    final ThemeMaterialSpec materials;
    final ThemeGlassColors glassColors;
    final ThemeCncColors cncColors;

    ResolvedTheme(ThemeDefinition definition) {
        this.definition = definition;
        this.hardwareAccent = definition.hardwareAccent;
        this.palette = definition.palette;
        this.componentColors = definition.componentColors;
        this.semanticStates = definition.semanticStates;
        this.materials = definition.materials;
        this.glassColors = definition.glassColors;
        this.cncColors = definition.cncColors;
    }

    String id() {
        return definition.id;
    }

    ThemeFamily family() {
        return definition.family;
    }
}
