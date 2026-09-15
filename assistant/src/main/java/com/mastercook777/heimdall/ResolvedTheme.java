package com.mastercook777.heimdall;

/** Immutable theme resolved from one stable registry definition. */
final class ResolvedTheme {
    final ThemeDefinition definition;
    final ThemePalette palette;
    final ThemeComponentColors componentColors;
    final SemanticStateColors semanticStates;
    final ThemeMaterialSpec materials;

    ResolvedTheme(ThemeDefinition definition) {
        this.definition = definition;
        this.palette = definition.palette;
        this.componentColors = definition.componentColors;
        this.semanticStates = definition.semanticStates;
        this.materials = definition.materials;
    }

    String id() {
        return definition.id;
    }

    ThemeFamily family() {
        return definition.family;
    }
}
