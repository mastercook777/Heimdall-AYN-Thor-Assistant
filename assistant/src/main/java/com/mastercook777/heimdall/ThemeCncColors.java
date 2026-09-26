package com.mastercook777.heimdall;

/** Colorway-owned colors consumed by the shared Freya CNC renderer. */
final class ThemeCncColors {
    static final int FLUSH = 0;
    static final int RAISED = 1;
    static final int INSET = 2;
    static final int CONTROL = 3;
    static final int SHALLOW_INSET = 4;
    static final int FIELD = 5;
    static final int INPUT_FRAME = 6;
    private static final int SURFACE_COUNT = 7;

    static final class Surface {
        final int roleColor;
        final int faceTop;
        final int faceBottom;
        final int mutedFaceTop;
        final int mutedFaceBottom;
        private final int[] shell;
        private final int[] rim;
        final int keyline;

        Surface(int roleColor, int faceTop, int faceBottom,
                int mutedFaceTop, int mutedFaceBottom,
                int[] shell, int[] rim, int keyline) {
            if (shell == null || shell.length != 3 || rim == null || rim.length != 3) {
                throw new IllegalArgumentException("CNC shell and rim need three colors");
            }
            this.roleColor = roleColor;
            this.faceTop = faceTop;
            this.faceBottom = faceBottom;
            this.mutedFaceTop = mutedFaceTop;
            this.mutedFaceBottom = mutedFaceBottom;
            this.shell = shell.clone();
            this.rim = rim.clone();
            this.keyline = keyline;
        }

        int[] shellColors() {
            return shell.clone();
        }

        int[] rimColors() {
            return rim.clone();
        }
    }

    private final Surface[] surfaces;
    final int menuFill;
    final int menuSelectedFill;
    final int menuMutedFill;
    final int menuEdge;
    final int menuMutedEdge;
    final int menuPanelFill;
    final int fullscreenToolbarTop;
    final int fullscreenToolbarBottom;
    final int fullscreenToolbarEdgeTop;
    final int fullscreenToolbarEdgeBottom;

    ThemeCncColors(Surface flush, Surface raised, Surface inset,
            Surface control, Surface shallowInset, Surface field,
            Surface inputFrame,
            int menuFill, int menuSelectedFill, int menuMutedFill,
            int menuEdge, int menuMutedEdge, int menuPanelFill,
            int fullscreenToolbarTop, int fullscreenToolbarBottom,
            int fullscreenToolbarEdgeTop, int fullscreenToolbarEdgeBottom) {
        surfaces = new Surface[]{flush, raised, inset, control,
                shallowInset, field, inputFrame};
        for (Surface surface : surfaces) {
            if (surface == null) {
                throw new IllegalArgumentException("Every Freya CNC role needs colors");
            }
        }
        this.menuFill = menuFill;
        this.menuSelectedFill = menuSelectedFill;
        this.menuMutedFill = menuMutedFill;
        this.menuEdge = menuEdge;
        this.menuMutedEdge = menuMutedEdge;
        this.menuPanelFill = menuPanelFill;
        this.fullscreenToolbarTop = fullscreenToolbarTop;
        this.fullscreenToolbarBottom = fullscreenToolbarBottom;
        this.fullscreenToolbarEdgeTop = fullscreenToolbarEdgeTop;
        this.fullscreenToolbarEdgeBottom = fullscreenToolbarEdgeBottom;
    }

    Surface surface(int role) {
        if (role < 0 || role >= SURFACE_COUNT) {
            throw new IllegalArgumentException("Unknown CNC surface role: " + role);
        }
        return surfaces[role];
    }
}
