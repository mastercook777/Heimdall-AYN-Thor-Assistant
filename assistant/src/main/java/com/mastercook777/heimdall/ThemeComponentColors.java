package com.mastercook777.heimdall;

/** Colorway-owned colors used by extracted UI components. */
final class ThemeComponentColors {
    static final int GRID_TOUCHPAD = 0;
    static final int GRID_MACRO_GROUP = 1;
    static final int GRID_KEYBOARD_PAD = 2;
    static final int GRID_QUICK_ACTIONS = 3;
    static final int GRID_MAGNIFIER = 4;
    static final int GRID_CANVAS = 5;
    static final int GRID_OTHER = 6;
    private static final int GRID_COLOR_COUNT = 7;

    final int dockIndicator;
    final int dockIconSelected;
    final int dockIconIdle;
    final int dockSettingsDivider;
    final int flatSurfaceFill;
    final int flatSurfaceEdge;
    final int navTransparentTop;
    final int navTransparentBottom;
    final int structuralDivider;
    final int structuralDividerSubtle;
    final int gridDraftShield;
    final int gridDraftWarning;
    final int quickVolumeIcon;
    final int quickVolumeThumbIdle;
    final int quickVolumeThumbPressed;
    final int quickVolumeThumbEdge;
    final int quickVolumeThumbHighlight;
    final int quickActionIconIdle;
    final int quickActionMagnifierStop;
    final int macroIconFocused;
    final int macroIconUtility;
    final int macroIconDefault;
    final int settingsExitIconDisabled;
    final int settingsExitIconPressed;
    final int settingsExitIconIdle;
    final int fullscreenBackground;
    final int selectedCheckIcon;
    final float disabledControlAlpha;
    final int mediaPreviewBackground;
    final int overlayScrim;
    final int precisionReticleOuterIdle;
    final int precisionReticleOuterActive;
    final int precisionReticleInnerIdle;
    final int precisionReticleInnerActive;
    final int precisionReticleCenter;
    final int inputDepthIdleCenter;
    final int inputDepthActiveCenter;
    final int inputDepthOuter;
    final int inputFaceIdleTop;
    final int inputFaceIdleBottom;
    final int inputFaceActiveTop;
    final int inputFaceActiveBottom;
    final int inputFocusCornerIdle;
    final int inputFocusCornerActive;
    final int touchPointHalo;
    final int touchPointCore;
    final int inputCenterHalo;
    final int inputCenterRing;
    final int inputCenterAccentRing;
    final int canvasPressedEdge;
    final int macroPressedOuter;
    final int macroPressedInner;
    final int controlUnchecked;
    final int sliderTrack;
    final int mediaHintText;
    final int profileFallbackText;
    final int capturePrimary;
    final int captureControlFill;
    final int captureControlEdge;
    final int captureSecondaryFill;
    final int captureSecondaryEdge;
    final int batteryShell;
    final int batteryFill;
    final int batteryChargingBolt;
    final int statusLampOuter;
    final int statusLampSuccess;
    final int statusLampError;
    final ThemeKeyboardColors keyboard;
    final int gridLine;
    final int gridOuterBoundary;
    final int gridItemBoundary;
    final int gridResizeHandle;
    final int gridResizeGlyph;
    private final int[] gridPreview;
    private final int[] gridSelected;

    ThemeComponentColors(int dockIndicator, int dockIconSelected, int dockIconIdle,
            int dockSettingsDivider,
            int flatSurfaceFill, int flatSurfaceEdge,
            int navTransparentTop, int navTransparentBottom,
            int structuralDivider, int structuralDividerSubtle,
            int gridDraftShield, int gridDraftWarning, int quickVolumeIcon,
            int quickVolumeThumbIdle,
            int quickVolumeThumbPressed, int quickVolumeThumbEdge,
            int quickVolumeThumbHighlight,
            int quickActionIconIdle, int quickActionMagnifierStop,
            int macroIconFocused, int macroIconUtility, int macroIconDefault,
            int settingsExitIconDisabled, int settingsExitIconPressed,
            int settingsExitIconIdle, int fullscreenBackground,
            int selectedCheckIcon, float disabledControlAlpha,
            int mediaPreviewBackground, int overlayScrim,
            int precisionReticleOuterIdle, int precisionReticleOuterActive,
            int precisionReticleInnerIdle, int precisionReticleInnerActive,
            int precisionReticleCenter,
            int inputDepthIdleCenter, int inputDepthActiveCenter,
            int inputDepthOuter,
            int inputFaceIdleTop, int inputFaceIdleBottom,
            int inputFaceActiveTop, int inputFaceActiveBottom,
            int inputFocusCornerIdle, int inputFocusCornerActive,
            int touchPointHalo, int touchPointCore,
            int inputCenterHalo, int inputCenterRing, int inputCenterAccentRing,
            int canvasPressedEdge, int macroPressedOuter, int macroPressedInner,
            int controlUnchecked, int sliderTrack, int mediaHintText,
            int profileFallbackText,
            int capturePrimary, int captureControlFill, int captureControlEdge,
            int captureSecondaryFill, int captureSecondaryEdge,
            int batteryShell, int batteryFill,
            int batteryChargingBolt, int statusLampOuter,
            int statusLampSuccess, int statusLampError,
            ThemeKeyboardColors keyboard,
            int gridLine, int gridOuterBoundary, int gridItemBoundary,
            int gridResizeHandle, int gridResizeGlyph,
            int[] gridPreview, int[] gridSelected) {
        if (keyboard == null || gridPreview == null || gridPreview.length != GRID_COLOR_COUNT
                || gridSelected == null || gridSelected.length != GRID_COLOR_COUNT) {
            throw new IllegalArgumentException("Grid component color tables must have seven entries");
        }
        this.dockIndicator = dockIndicator;
        this.dockIconSelected = dockIconSelected;
        this.dockIconIdle = dockIconIdle;
        this.dockSettingsDivider = dockSettingsDivider;
        this.flatSurfaceFill = flatSurfaceFill;
        this.flatSurfaceEdge = flatSurfaceEdge;
        this.navTransparentTop = navTransparentTop;
        this.navTransparentBottom = navTransparentBottom;
        this.structuralDivider = structuralDivider;
        this.structuralDividerSubtle = structuralDividerSubtle;
        this.gridDraftShield = gridDraftShield;
        this.gridDraftWarning = gridDraftWarning;
        this.quickVolumeIcon = quickVolumeIcon;
        this.quickVolumeThumbIdle = quickVolumeThumbIdle;
        this.quickVolumeThumbPressed = quickVolumeThumbPressed;
        this.quickVolumeThumbEdge = quickVolumeThumbEdge;
        this.quickVolumeThumbHighlight = quickVolumeThumbHighlight;
        this.quickActionIconIdle = quickActionIconIdle;
        this.quickActionMagnifierStop = quickActionMagnifierStop;
        this.macroIconFocused = macroIconFocused;
        this.macroIconUtility = macroIconUtility;
        this.macroIconDefault = macroIconDefault;
        this.settingsExitIconDisabled = settingsExitIconDisabled;
        this.settingsExitIconPressed = settingsExitIconPressed;
        this.settingsExitIconIdle = settingsExitIconIdle;
        this.fullscreenBackground = fullscreenBackground;
        this.selectedCheckIcon = selectedCheckIcon;
        this.disabledControlAlpha = disabledControlAlpha;
        this.mediaPreviewBackground = mediaPreviewBackground;
        this.overlayScrim = overlayScrim;
        this.precisionReticleOuterIdle = precisionReticleOuterIdle;
        this.precisionReticleOuterActive = precisionReticleOuterActive;
        this.precisionReticleInnerIdle = precisionReticleInnerIdle;
        this.precisionReticleInnerActive = precisionReticleInnerActive;
        this.precisionReticleCenter = precisionReticleCenter;
        this.inputDepthIdleCenter = inputDepthIdleCenter;
        this.inputDepthActiveCenter = inputDepthActiveCenter;
        this.inputDepthOuter = inputDepthOuter;
        this.inputFaceIdleTop = inputFaceIdleTop;
        this.inputFaceIdleBottom = inputFaceIdleBottom;
        this.inputFaceActiveTop = inputFaceActiveTop;
        this.inputFaceActiveBottom = inputFaceActiveBottom;
        this.inputFocusCornerIdle = inputFocusCornerIdle;
        this.inputFocusCornerActive = inputFocusCornerActive;
        this.touchPointHalo = touchPointHalo;
        this.touchPointCore = touchPointCore;
        this.inputCenterHalo = inputCenterHalo;
        this.inputCenterRing = inputCenterRing;
        this.inputCenterAccentRing = inputCenterAccentRing;
        this.canvasPressedEdge = canvasPressedEdge;
        this.macroPressedOuter = macroPressedOuter;
        this.macroPressedInner = macroPressedInner;
        this.controlUnchecked = controlUnchecked;
        this.sliderTrack = sliderTrack;
        this.mediaHintText = mediaHintText;
        this.profileFallbackText = profileFallbackText;
        this.capturePrimary = capturePrimary;
        this.captureControlFill = captureControlFill;
        this.captureControlEdge = captureControlEdge;
        this.captureSecondaryFill = captureSecondaryFill;
        this.captureSecondaryEdge = captureSecondaryEdge;
        this.batteryShell = batteryShell;
        this.batteryFill = batteryFill;
        this.batteryChargingBolt = batteryChargingBolt;
        this.statusLampOuter = statusLampOuter;
        this.statusLampSuccess = statusLampSuccess;
        this.statusLampError = statusLampError;
        this.keyboard = keyboard;
        this.gridLine = gridLine;
        this.gridOuterBoundary = gridOuterBoundary;
        this.gridItemBoundary = gridItemBoundary;
        this.gridResizeHandle = gridResizeHandle;
        this.gridResizeGlyph = gridResizeGlyph;
        this.gridPreview = gridPreview.clone();
        this.gridSelected = gridSelected.clone();
    }

    int gridPreview(int slot) {
        return gridPreview[slot];
    }

    int gridSelected(int slot) {
        return gridSelected[slot];
    }
}
