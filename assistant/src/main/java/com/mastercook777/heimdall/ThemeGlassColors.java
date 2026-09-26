package com.mastercook777.heimdall;

/** Colorway-owned colors consumed by the shared Heimdall glass renderer. */
final class ThemeGlassColors {
    static final int MODULE = 0;
    static final int SYSTEM_CHROME = 1;
    static final int INFO = 2;
    static final int QUICK_ACTIONS = 3;
    static final int CONTROL = 4;
    static final int SELECTED_CONTROL = 5;
    static final int SECTION_SELECTED = 6;
    static final int PRIMARY_ACTION = 7;
    static final int MACRO_UTILITY = 8;
    static final int MACRO_SECONDARY = 9;
    static final int MACRO_PRIMARY = 10;
    static final int MACRO_FOCUSED = 11;
    static final int INSET = 12;
    static final int FIELD = 13;
    static final int FULLSCREEN_CHROME = 14;
    static final int FULLSCREEN_CONTROL = 15;
    static final int FULLSCREEN_REVEAL = 16;
    static final int KEYBOARD_FRAME = 17;
    static final int FULL_KEYBOARD_FRAME = 18;
    static final int KEYBOARD_TOOLBAR = 19;
    static final int OVERLAY = 20;
    static final int PICKER = 21;
    static final int MEDIA_FRAME = 22;
    static final int MAGNIFIER_FRAME = 23;
    static final int OPAQUE_CHROME = 24;
    static final int PROFILE_ICON = 25;
    static final int LIST_CONTROL = 26;
    static final int PROFILE_LIST_SELECTED = 27;
    static final int MAP_SELECTED = 28;
    static final int MAP_BADGE = 29;
    static final int ICON_CELL = 30;
    static final int KEYBOARD_ICON_SELECTED = 31;
    static final int MACRO_ICON_SELECTED = 32;
    static final int SELECTED_BADGE = 33;
    static final int SETTINGS_SECTION_SELECTED = 34;
    static final int SETTINGS_CONTENT = 35;
    private static final int SURFACE_COUNT = 36;

    static final class Surface {
        final int top;
        final int bottom;
        final int edgeTop;
        final int edgeBottom;

        Surface(int top, int bottom, int edgeTop, int edgeBottom) {
            this.top = top;
            this.bottom = bottom;
            this.edgeTop = edgeTop;
            this.edgeBottom = edgeBottom;
        }
    }

    private final Surface[] surfaces;
    final int[] rightStickWell;
    final int rightStickWellEdge;
    final int rightStickDeadZone;
    final int rightStickTrailIdle;
    final int[] rightStickCap;
    final int rightStickCapIdleEdge;
    final int rightStickCapActiveEdge;
    final int[] virtualMouseStrip;
    final int virtualMouseStripEdge;
    final int virtualMouseDivider;
    final int[] virtualMousePressed;

    ThemeGlassColors(Surface[] surfaces,
            int[] rightStickWell, int rightStickWellEdge, int rightStickDeadZone,
            int rightStickTrailIdle, int[] rightStickCap,
            int rightStickCapIdleEdge, int rightStickCapActiveEdge,
            int[] virtualMouseStrip, int virtualMouseStripEdge,
            int virtualMouseDivider, int[] virtualMousePressed) {
        if (surfaces == null || surfaces.length != SURFACE_COUNT
                || rightStickWell == null || rightStickWell.length != 3
                || rightStickCap == null || rightStickCap.length != 3
                || virtualMouseStrip == null || virtualMouseStrip.length != 3
                || virtualMousePressed == null || virtualMousePressed.length != 3) {
            throw new IllegalArgumentException("Invalid Heimdall glass color table");
        }
        this.surfaces = surfaces.clone();
        this.rightStickWell = rightStickWell.clone();
        this.rightStickWellEdge = rightStickWellEdge;
        this.rightStickDeadZone = rightStickDeadZone;
        this.rightStickTrailIdle = rightStickTrailIdle;
        this.rightStickCap = rightStickCap.clone();
        this.rightStickCapIdleEdge = rightStickCapIdleEdge;
        this.rightStickCapActiveEdge = rightStickCapActiveEdge;
        this.virtualMouseStrip = virtualMouseStrip.clone();
        this.virtualMouseStripEdge = virtualMouseStripEdge;
        this.virtualMouseDivider = virtualMouseDivider;
        this.virtualMousePressed = virtualMousePressed.clone();
    }

    Surface surface(int role) {
        if (role < 0 || role >= surfaces.length) {
            throw new IllegalArgumentException("Unknown Heimdall glass role: " + role);
        }
        return surfaces[role];
    }
}
