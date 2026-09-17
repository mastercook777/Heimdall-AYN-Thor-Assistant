package com.mastercook777.heimdall;

/** Colorway-owned colors for the accepted Full Keyboard and Keypad renderers. */
final class ThemeKeyboardColors {
    final int bedFill;
    final int bedEdge;
    final int lockedText;
    final int lockedEdgeTop;
    final int lockedEdgeBottom;
    final int activeEdgeBottom;
    final int keycapReliefActive;
    final int keycapPressedEdge;
    final int keycapFaceTop;
    final int keycapFaceBottom;
    final int keycapPressedFaceTop;
    final int keycapPressedFaceBottom;
    final int keycapIdleEdge;
    final int keycapIdleEdgeBottom;
    final int keycapReliefIdle;
    final int keycapReliefBottom;
    final int menuActive;
    final int menuIdle;

    ThemeKeyboardColors(int bedFill, int bedEdge, int lockedText,
            int lockedEdgeTop, int lockedEdgeBottom, int activeEdgeBottom,
            int keycapReliefActive, int keycapPressedEdge,
            int keycapFaceTop, int keycapFaceBottom,
            int keycapPressedFaceTop, int keycapPressedFaceBottom,
            int keycapIdleEdge, int keycapIdleEdgeBottom,
            int keycapReliefIdle, int keycapReliefBottom,
            int menuActive, int menuIdle) {
        this.bedFill = bedFill;
        this.bedEdge = bedEdge;
        this.lockedText = lockedText;
        this.lockedEdgeTop = lockedEdgeTop;
        this.lockedEdgeBottom = lockedEdgeBottom;
        this.activeEdgeBottom = activeEdgeBottom;
        this.keycapReliefActive = keycapReliefActive;
        this.keycapPressedEdge = keycapPressedEdge;
        this.keycapFaceTop = keycapFaceTop;
        this.keycapFaceBottom = keycapFaceBottom;
        this.keycapPressedFaceTop = keycapPressedFaceTop;
        this.keycapPressedFaceBottom = keycapPressedFaceBottom;
        this.keycapIdleEdge = keycapIdleEdge;
        this.keycapIdleEdgeBottom = keycapIdleEdgeBottom;
        this.keycapReliefIdle = keycapReliefIdle;
        this.keycapReliefBottom = keycapReliefBottom;
        this.menuActive = menuActive;
        this.menuIdle = menuIdle;
    }
}
