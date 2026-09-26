package com.mastercook777.heimdall;

/** Family-owned material behavior and geometry; colorways do not change these values. */
final class ThemeMaterialSpec {
    final ThemeFamily family;
    final boolean cncSurfaces;
    final boolean aspectInvariantPlayLighting;
    final int mediaFrameContentInsetDp;
    final int systemChromeElevationDp;
    final int controlElevationDp;
    final int headerBrandMarkRes;

    ThemeMaterialSpec(ThemeFamily family, boolean cncSurfaces,
            boolean aspectInvariantPlayLighting, int mediaFrameContentInsetDp,
            int systemChromeElevationDp, int controlElevationDp,
            int headerBrandMarkRes) {
        this.family = family;
        this.cncSurfaces = cncSurfaces;
        this.aspectInvariantPlayLighting = aspectInvariantPlayLighting;
        this.mediaFrameContentInsetDp = mediaFrameContentInsetDp;
        this.systemChromeElevationDp = systemChromeElevationDp;
        this.controlElevationDp = controlElevationDp;
        this.headerBrandMarkRes = headerBrandMarkRes;
    }
}
