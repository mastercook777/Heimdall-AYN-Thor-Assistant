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
    final int menuActive;
    final int menuIdle;

    ThemeKeyboardColors(int bedFill, int bedEdge, int lockedText,
            int lockedEdgeTop, int lockedEdgeBottom, int activeEdgeBottom,
            int keycapReliefActive, int keycapPressedEdge,
            int menuActive, int menuIdle) {
        this.bedFill = bedFill;
        this.bedEdge = bedEdge;
        this.lockedText = lockedText;
        this.lockedEdgeTop = lockedEdgeTop;
        this.lockedEdgeBottom = lockedEdgeBottom;
        this.activeEdgeBottom = activeEdgeBottom;
        this.keycapReliefActive = keycapReliefActive;
        this.keycapPressedEdge = keycapPressedEdge;
        this.menuActive = menuActive;
        this.menuIdle = menuIdle;
    }
}
