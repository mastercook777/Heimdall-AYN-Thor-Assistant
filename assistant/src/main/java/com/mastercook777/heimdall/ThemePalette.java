package com.mastercook777.heimdall;

/** Colorway-owned colors. Geometry and draw-layer behavior belong to ThemeMaterialSpec. */
final class ThemePalette {
    final int pageBackground;
    final int flatPageBackground;
    final int surfaceBase;
    final int surfaceRaised;
    final int surfaceInset;
    final int textPrimary;
    final int textSecondary;
    final int textDisabled;
    final int edgeNeutral;
    final int edgeStrong;
    final int accent;
    final int accentStrong;
    final int accentEdge;
    final int accentGradientEnd;
    final int focus;
    final int selection;
    final int activeInput;
    final int quickActionDivider;
    final int volumeTrack;
    final int volumeActive;
    final int inputEdgeIdle;
    final int inputEdgeActive;
    final int inputGlowIdle;
    final int inputGlowActive;
    final int inputTexture;
    final int inputTextureAlt;
    final int inputCenter;

    ThemePalette(int pageBackground, int flatPageBackground,
            int surfaceBase, int surfaceRaised, int surfaceInset,
            int textPrimary, int textSecondary, int textDisabled,
            int edgeNeutral, int edgeStrong,
            int accent, int accentStrong, int accentEdge, int accentGradientEnd,
            int focus, int selection, int activeInput,
            int quickActionDivider, int volumeTrack, int volumeActive,
            int inputEdgeIdle, int inputEdgeActive,
            int inputGlowIdle, int inputGlowActive,
            int inputTexture, int inputTextureAlt, int inputCenter) {
        this.pageBackground = pageBackground;
        this.flatPageBackground = flatPageBackground;
        this.surfaceBase = surfaceBase;
        this.surfaceRaised = surfaceRaised;
        this.surfaceInset = surfaceInset;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.textDisabled = textDisabled;
        this.edgeNeutral = edgeNeutral;
        this.edgeStrong = edgeStrong;
        this.accent = accent;
        this.accentStrong = accentStrong;
        this.accentEdge = accentEdge;
        this.accentGradientEnd = accentGradientEnd;
        this.focus = focus;
        this.selection = selection;
        this.activeInput = activeInput;
        this.quickActionDivider = quickActionDivider;
        this.volumeTrack = volumeTrack;
        this.volumeActive = volumeActive;
        this.inputEdgeIdle = inputEdgeIdle;
        this.inputEdgeActive = inputEdgeActive;
        this.inputGlowIdle = inputGlowIdle;
        this.inputGlowActive = inputGlowActive;
        this.inputTexture = inputTexture;
        this.inputTextureAlt = inputTextureAlt;
        this.inputCenter = inputCenter;
    }
}
