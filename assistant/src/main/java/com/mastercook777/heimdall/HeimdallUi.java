package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class HeimdallUi {
    static final String THEME_HEIMDALL_BLUE = ThemeRegistry.ID_HEIMDALL_BLUE;
    static final String THEME_FREYA_WHITE = ThemeRegistry.ID_FREYA_WHITE;
    private static final String THEME_PREFS = "heimdall_ui";
    private static final String KEY_THEME = "theme";
    private static volatile String activeProfileThemeId = "";

    private HeimdallUi() {
    }

    static final int COLOR_BG = 0xFF070A10;
    static final int COLOR_SURFACE = 0xCC101722;
    static final int COLOR_SURFACE_RAISED = 0xD1172131;
    static final int COLOR_SURFACE_INSET = 0x990B1018;
    static final int COLOR_SURFACE_DEEP = 0xFF090D14;
    static final int COLOR_SURFACE_SOFT = 0xD91D2A3D;
    static final int COLOR_SURFACE_SELECTED = 0xD916345A;
    static final int COLOR_FOCUS_BLUE = 0xFF3F9DFF;
    static final int COLOR_ACCENT = 0xFF4EA1FF;
    static final int COLOR_ACCENT_STRONG = 0xFF70B7FF;
    static final int COLOR_ACCENT_DARK = 0xFF1D4F8D;
    static final int COLOR_TEXT = 0xFFE6EDF3;
    static final int COLOR_TEXT_MUTED = 0xFF9AA8B8;
    static final int COLOR_TEXT_INVERSE = 0xFF05070A;
    static final int COLOR_BORDER = 0xFF2B3748;
    static final int COLOR_BORDER_STRONG = 0xFF3C4D63;
    static final int COLOR_DANGER = 0xFFFF6B6B;
    static final int COLOR_DANGER_BG = 0xFF2A151A;
    static final int COLOR_SUCCESS = 0xFF5FD18A;
    static final int COLOR_WARNING = 0xFFD8A13A;


    static final int TYPE_PAGE_TITLE = 16;
    static final int TYPE_MODULE_TITLE = 14;
    static final int TYPE_BODY = 13;
    static final int TYPE_HELP = 12;
    static final int TYPE_BUTTON = 13;
    static final int TYPE_BUTTON_COMPACT = 12;
    static final int TYPE_EDITOR_TITLE = 17;
    static final int TYPE_SECTION_TITLE = 14;
    static final int TYPE_LABEL = 12;
    static final int TYPE_META = 11;

    static final int SPACE_1 = 4;
    static final int SPACE_2 = 8;
    static final int SPACE_3 = 12;
    static final int SPACE_4 = 16;
    static final int SPACE_WIDGET_GAP = 3;

    static final int RADIUS_SMALL = 8;
    static final int RADIUS_BUTTON = 9;
    static final int RADIUS_CARD = 10;
    static final int RADIUS_MODULE = 12;
    static final int RADIUS_PANEL = 14;

    static final int HEIGHT_BUTTON_MIN = 48;
    static final int HEIGHT_ICON_BUTTON = 48;
    static final int HEIGHT_HEADER = 48;
    static final int HEIGHT_DOCK = 56;
    static final int HEIGHT_SETTINGS_FOOTER = 48;

    static final float MACRO_ICON_SHARE_STANDARD = 0.44f;
    static final float MACRO_ICON_SHARE_UTILITY = 0.40f;
    static final float MACRO_ICON_SIZE_SCALE = 1.15f;
    static final int MACRO_ICON_MIN = 34;
    static final int MACRO_ICON_MAX = 58;
    static final int MACRO_ICON_LABEL_GAP = 6;
    static final int QUICK_ACTION_ICON_SIZE = 30;
    static final int VOLUME_TRACK_HEIGHT = 4;
    static final int VOLUME_THUMB_SIZE = 13;
    static final int INPUT_SURFACE_INSET = 2;
    static final int INPUT_SURFACE_RADIUS = 10;

    static final int STROKE_HAIRLINE = 1;
    static final int STROKE_SELECTED = 2;
    static final int STROKE_MODULE = 3;

    static final int SEMANTIC_NEUTRAL = 0;
    static final int SEMANTIC_SUCCESS = 1;
    static final int SEMANTIC_WARNING = 2;
    static final int SEMANTIC_ERROR = 3;
    static final int SEMANTIC_RECORDING = 4;

    static final int MACRO_PRIMARY = 0;
    static final int MACRO_SECONDARY = 1;
    static final int MACRO_UTILITY = 2;

    static String theme(Context context) {
        return resolvedTheme(context).id();
    }

    static String globalTheme(Context context) {
        String stored = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_THEME, ThemeRegistry.LEGACY_DARK);
        return ThemeRegistry.resolve(stored).id();
    }

    static void setTheme(Context context, String theme) {
        String value = ThemeRegistry.canonicalId(theme);
        context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_THEME, value).apply();
    }

    static void setActiveProfileTheme(String theme) {
        activeProfileThemeId = ThemeRegistry.isKnown(theme)
                ? ThemeRegistry.canonicalId(theme) : "";
    }

    static void clearActiveProfileTheme() {
        activeProfileThemeId = "";
    }

    static ResolvedTheme resolvedTheme(Context context) {
        String profileTheme = activeProfileThemeId;
        if (profileTheme.length() > 0) {
            return ThemeRegistry.resolve(profileTheme);
        }
        String stored = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_THEME, ThemeRegistry.LEGACY_DARK);
        return ThemeRegistry.resolve(stored);
    }

    static ThemeDefinition themeDefinition(Context context) {
        return resolvedTheme(context).definition;
    }

    static ThemeMaterialSpec materialSpec(Context context) {
        return resolvedTheme(context).materials;
    }

    static ThemeComponentColors componentColors(Context context) {
        return resolvedTheme(context).componentColors;
    }

    static ThemeGlassColors glassColors(Context context) {
        ThemeGlassColors colors = resolvedTheme(context).glassColors;
        if (colors == null) {
            throw new IllegalStateException("Glass colors requested outside the Heimdall family");
        }
        return colors;
    }

    static ThemeCncColors cncColors(Context context) {
        ThemeCncColors colors = resolvedTheme(context).cncColors;
        if (colors == null) {
            throw new IllegalStateException("CNC colors requested outside the Freya family");
        }
        return colors;
    }

    static int semanticColor(Context context, int semantic) {
        return resolvedTheme(context).semanticStates.appearance(semantic).foreground;
    }

    static boolean isFreyaFamily(Context context) {
        return resolvedTheme(context).family() == ThemeFamily.FREYA;
    }

    static int mediaFrameContentInsetDp(Context context) {
        return materialSpec(context).mediaFrameContentInsetDp;
    }

    static float mediaFrameInnerRadiusDp(Context context, float outerRadiusDp) {
        int insetDp = mediaFrameContentInsetDp(context);
        return materialSpec(context).aspectInvariantPlayLighting
                ? concentricInnerRadiusDp(outerRadiusDp, insetDp)
                : Math.max(0f, outerRadiusDp - insetDp);
    }

    static int background(Context context) {
        ThemePalette palette = resolvedTheme(context).palette;
        return DebugPerformanceDiagnostics.isFlatUi()
                ? palette.flatPageBackground : palette.pageBackground;
    }

    static int surface(Context context) {
        return resolvedTheme(context).palette.surfaceBase;
    }

    static int surfaceRaised(Context context) {
        return resolvedTheme(context).palette.surfaceRaised;
    }

    static int surfaceInset(Context context) {
        return resolvedTheme(context).palette.surfaceInset;
    }

    static int textColor(Context context) {
        return resolvedTheme(context).palette.textPrimary;
    }

    static int mutedTextColor(Context context) {
        return resolvedTheme(context).palette.textSecondary;
    }

    static int accent(Context context) {
        return resolvedTheme(context).palette.accent;
    }

    static int accentStrong(Context context) {
        return resolvedTheme(context).palette.accentStrong;
    }

    static int activeInput(Context context) {
        return resolvedTheme(context).palette.activeInput;
    }

    static int border(Context context) {
        return resolvedTheme(context).palette.edgeNeutral;
    }

    static int quickActionDivider(Context context) {
        return resolvedTheme(context).palette.quickActionDivider;
    }

    static int volumeTrack(Context context) {
        return resolvedTheme(context).palette.volumeTrack;
    }

    static int volumeActive(Context context) {
        return resolvedTheme(context).palette.volumeActive;
    }

    static int inputEdge(Context context, boolean active) {
        ThemePalette palette = resolvedTheme(context).palette;
        return active ? palette.inputEdgeActive : palette.inputEdgeIdle;
    }

    static int inputGlow(Context context, boolean active) {
        ThemePalette palette = resolvedTheme(context).palette;
        return active ? palette.inputGlowActive : palette.inputGlowIdle;
    }

    static int inputTexture(Context context) {
        return resolvedTheme(context).palette.inputTexture;
    }

    static int inputTextureAlt(Context context) {
        return resolvedTheme(context).palette.inputTextureAlt;
    }

    static int inputCenter(Context context) {
        return resolvedTheme(context).palette.inputCenter;
    }

    static int resolveColor(Context context, int darkColor) {
        if (darkColor == COLOR_BG) return background(context);
        if (darkColor == COLOR_SURFACE) return surface(context);
        if (darkColor == COLOR_SURFACE_RAISED || darkColor == COLOR_SURFACE_SOFT) return surfaceRaised(context);
        if (darkColor == COLOR_SURFACE_INSET || darkColor == COLOR_SURFACE_DEEP) return surfaceInset(context);
        if (darkColor == COLOR_TEXT) return textColor(context);
        if (darkColor == COLOR_TEXT_MUTED) return mutedTextColor(context);
        if (darkColor == COLOR_ACCENT || darkColor == COLOR_FOCUS_BLUE) return accent(context);
        if (darkColor == COLOR_ACCENT_STRONG) return accentStrong(context);
        if (darkColor == COLOR_BORDER || darkColor == COLOR_BORDER_STRONG) return border(context);
        return darkColor;
    }

    static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    static GradientDrawable rounded(Context context, int color, int strokeColor, int radiusDp) {
        return rounded(context, color, strokeColor, radiusDp, 1);
    }

    static GradientDrawable rounded(Context context, int color, int strokeColor, int radiusDp, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        if (strokeColor != 0 && strokeDp > 0) {
            drawable.setStroke(dp(context, strokeDp), strokeColor);
        }
        return drawable;
    }

    static Drawable glass(Context context, int topColor, int bottomColor, int strokeColor, int radiusDp) {
        return glass(context, topColor, bottomColor, strokeColor, strokeColor, radiusDp, 1);
    }

    static Drawable glass(Context context, int topColor, int bottomColor,
            int borderTopColor, int borderBottomColor, int radiusDp, int borderDp) {
        return glass(context, topColor, bottomColor, borderTopColor, borderBottomColor,
                radiusDp, borderDp, false);
    }

    static Drawable glassSurface(Context context, int role,
            int radiusDp, int borderDp) {
        ThemeGlassColors.Surface colors = glassColors(context).surface(role);
        return glass(context, colors.top, colors.bottom,
                colors.edgeTop, colors.edgeBottom, radiusDp, borderDp);
    }

    static Drawable settingsContentPanel(Context context, int radiusDp) {
        if (isFreyaFamily(context)) {
            return cncFlush(context, radiusDp);
        }
        ThemeGlassColors.Surface colors = glassColors(context)
                .surface(ThemeGlassColors.SETTINGS_CONTENT);
        return rounded(context, colors.top, colors.edgeTop, radiusDp);
    }

    static Drawable translationContentPanel(Context context, int radiusDp) {
        return settingsContentPanel(context, radiusDp);
    }

    static Drawable hardwareMonitorContentPanel(Context context, int radiusDp) {
        return settingsContentPanel(context, radiusDp);
    }

    static int translationTextColor(Context context) {
        return isFreyaFamily(context) ? textColor(context) : accent(context);
    }

    static Drawable glassCircle(Context context, int topColor, int bottomColor,
            int borderTopColor, int borderBottomColor, int borderDp) {
        return glass(context, topColor, bottomColor, borderTopColor, borderBottomColor,
                0, borderDp, true);
    }

    private static Drawable glass(Context context, int topColor, int bottomColor,
            int borderTopColor, int borderBottomColor, int radiusDp, int borderDp,
            boolean circular) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp,
                    borderDp > 0 && Color.alpha(borderTopColor) >= 0x70, circular);
        }
        if (borderDp <= 0) {
            return glassFill(context, topColor, bottomColor, radiusDp, circular);
        }
        GradientDrawable border = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{borderTopColor, borderBottomColor});
        border.setShape(circular ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
        if (!circular) {
            border.setCornerRadius(dp(context, radiusDp));
        }

        GradientDrawable fill = glassFill(context, topColor, bottomColor,
                Math.max(0, radiusDp - borderDp), circular);
        LayerDrawable layered = new LayerDrawable(new Drawable[]{border, fill});
        int inset = dp(context, borderDp);
        layered.setLayerInset(1, inset, inset, inset, inset);
        return layered;
    }

    private static GradientDrawable glassFill(Context context, int topColor, int bottomColor, int radiusDp) {
        return glassFill(context, topColor, bottomColor, radiusDp, false);
    }

    private static GradientDrawable glassFill(Context context, int topColor, int bottomColor,
            int radiusDp, boolean circular) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topColor, bottomColor});
        drawable.setShape(circular ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
        if (!circular) {
            drawable.setCornerRadius(dp(context, radiusDp));
        }
        return drawable;
    }

    static TextView text(Context context, String value, int sp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(resolveColor(context, color));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTypeface(typeface(bold));
        return view;
    }

    static Typeface typeface(boolean bold) {
        return Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL);
    }

    static void applyModulePanel(Context context, LinearLayout view) {
        if (isFreyaFamily(context)) {
            view.setBackground(cncRaised(context, RADIUS_MODULE, false, false));
            view.setPadding(dp(context, SPACE_1), dp(context, SPACE_1), dp(context, SPACE_1), dp(context, SPACE_1));
            view.setElevation(0f);
            return;
        }
        view.setBackground(glassSurface(context, ThemeGlassColors.MODULE,
                RADIUS_MODULE, STROKE_MODULE));
        view.setPadding(dp(context, SPACE_1), dp(context, SPACE_1), dp(context, SPACE_1), dp(context, SPACE_1));
        view.setElevation(dp(context, 1));
    }

    static void applyHeaderPanel(Context context, LinearLayout view) {
        applySystemChromePanel(context, view);
        view.setPadding(dp(context, SPACE_3), 0, dp(context, SPACE_3), 0);
        view.setElevation(isFreyaFamily(context) ? 0f : dp(context, 2));
    }

    static void applyBottomDockPanel(Context context, LinearLayout view) {
        applySystemChromePanel(context, view);
        view.setPadding(dp(context, SPACE_1), dp(context, 2), dp(context, SPACE_1), dp(context, 2));
        view.setElevation(isFreyaFamily(context) ? 0f : dp(context, 2));
    }

    private static void applySystemChromePanel(Context context, LinearLayout view) {
        if (isFreyaFamily(context)) {
            view.setBackground(cncFlush(context, RADIUS_PANEL));
            return;
        }
        view.setBackground(glassSurface(context, ThemeGlassColors.SYSTEM_CHROME,
                RADIUS_PANEL, STROKE_SELECTED));
    }

    static void applyInfoPill(Context context, TextView view) {
        view.setTextColor(mutedTextColor(context));
        view.setIncludeFontPadding(false);
        view.setLineSpacing(dp(context, 2), 1f);
        view.setBackground(isFreyaFamily(context)
                ? cncInset(context, RADIUS_MODULE)
                : glassSurface(context, ThemeGlassColors.INFO, RADIUS_MODULE, 1));
        view.setElevation(0f);
    }

    static void applyQuickActionPanel(Context context, LinearLayout view) {
        view.setBackground(isFreyaFamily(context)
                ? cncKeyboardShell(context, RADIUS_MODULE)
                : glassSurface(context, ThemeGlassColors.QUICK_ACTIONS,
                        RADIUS_MODULE, STROKE_HAIRLINE));
        view.setPadding(dp(context, SPACE_1), dp(context, SPACE_1),
                dp(context, SPACE_1), dp(context, SPACE_1));
        view.setElevation(0f);
    }

    static Button baseButton(Context context, String label, Runnable action) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(TYPE_BUTTON);
        button.setAllCaps(false);
        button.setTypeface(typeface(false));
        button.setTextColor(textColor(context));
        button.setIncludeFontPadding(false);
        applySecondaryButton(context, button);
        button.setElevation(isFreyaFamily(context) ? 0f : dp(context, 1));
        button.setOnClickListener(v -> action.run());
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(dp(context, SPACE_2), 0, dp(context, SPACE_2), 0);
        return button;
    }

    static void applySecondaryButton(Context context, Button button) {
        button.setTextColor(textColor(context));
        button.setIncludeFontPadding(false);
        button.setBackground(isFreyaFamily(context)
                ? cncMenuControl(context, RADIUS_BUTTON, false, false)
                : glassSurface(context, ThemeGlassColors.CONTROL, RADIUS_BUTTON, 1));
        button.setElevation(isFreyaFamily(context) ? 0f : dp(context, 1));
    }

    static void applySelectedButton(Context context, Button button) {
        button.setTextColor(textColor(context));
        button.setIncludeFontPadding(false);
        button.setBackground(isFreyaFamily(context)
                ? cncMenuControl(context, RADIUS_BUTTON, true, false)
                : glassSurface(context, ThemeGlassColors.SELECTED_CONTROL,
                        RADIUS_BUTTON, STROKE_SELECTED));
        button.setElevation(isFreyaFamily(context) ? 0f : dp(context, 1));
    }

    static void applySectionButton(Context context, Button button, boolean selected) {
        if (!selected) {
            applySecondaryButton(context, button);
            button.setTextColor(mutedTextColor(context));
            button.setElevation(0f);
            return;
        }
        button.setTextColor(textColor(context));
        button.setIncludeFontPadding(false);
        button.setBackground(isFreyaFamily(context)
                ? cncMenuControl(context, RADIUS_BUTTON, true, false)
                : glassSurface(context, ThemeGlassColors.SECTION_SELECTED,
                        RADIUS_BUTTON, STROKE_SELECTED));
        button.setElevation(0f);
    }

    static void applyPrimaryActionButton(Context context, Button button) {
        button.setTextColor(textColor(context));
        button.setIncludeFontPadding(false);
        button.setBackground(isFreyaFamily(context)
                ? cncMenuControl(context, RADIUS_BUTTON, true, false)
                : glassSurface(context, ThemeGlassColors.PRIMARY_ACTION,
                        RADIUS_BUTTON, STROKE_SELECTED));
        button.setElevation(isFreyaFamily(context) ? 0f : dp(context, 1));
    }

    static void applyChoiceButton(Context context, Button button, boolean selected) {
        if (selected) {
            applySelectedButton(context, button);
            return;
        }
        applySecondaryButton(context, button);
        button.setTextColor(mutedTextColor(context));
        button.setElevation(0f);
    }

    static void applyMacroButton(Context context, Button button, boolean highlighted) {
        applyMacroButton(context, button, highlighted ? MACRO_PRIMARY : MACRO_SECONDARY, highlighted, 0);
    }

    static void applyMacroButton(Context context, Button button, boolean highlighted, int variant) {
        applyMacroButton(context, button, highlighted ? MACRO_PRIMARY : MACRO_SECONDARY, highlighted, variant);
    }

    static void applyMacroButton(Context context, Button button, int priority, boolean focused, int variant) {
        boolean primary = priority == MACRO_PRIMARY;
        boolean utility = priority == MACRO_UTILITY;
        button.setTextColor(textColor(context));
        button.setTextSize(primary || focused ? TYPE_MODULE_TITLE + 1 : TYPE_MODULE_TITLE);
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(false);
        if (isFreyaFamily(context)) {
            button.setBackground(cncMacroControl(context, RADIUS_CARD,
                    focused || primary, utility));
        } else {
            button.setBackground(glassSurface(context,
                    focused ? ThemeGlassColors.MACRO_FOCUSED : macroGlassRole(priority),
                    RADIUS_CARD, focused || primary ? STROKE_SELECTED : STROKE_HAIRLINE));
        }
        button.setElevation(isFreyaFamily(context) ? 0f : dp(context, focused ? 2 : (primary ? 1 : 0)));
        button.setPadding(dp(context, SPACE_2), dp(context, SPACE_1), dp(context, SPACE_2), dp(context, SPACE_1));
    }

    static void applyMacroRoleChoiceButton(Context context, Button button, int priority, boolean selected) {
        if (!selected) {
            applySecondaryButton(context, button);
            button.setTextColor(mutedTextColor(context));
            button.setElevation(0f);
            return;
        }
        button.setTextColor(textColor(context));
        if (isFreyaFamily(context)) {
            button.setBackground(cncMenuControl(context, RADIUS_BUTTON,
                    true, priority == MACRO_UTILITY));
        } else {
            ThemeGlassColors.Surface fill = glassColors(context)
                    .surface(ThemeGlassColors.CONTROL);
            ThemeGlassColors.Surface role = glassColors(context)
                    .surface(macroGlassRole(priority));
            button.setBackground(glass(context, fill.top, fill.bottom,
                    role.edgeTop, role.edgeBottom, RADIUS_BUTTON, STROKE_SELECTED));
        }
        button.setElevation(isFreyaFamily(context) ? 0f : dp(context, 1));
    }

    static Drawable glassCircleSurface(Context context, int role, int borderDp) {
        ThemeGlassColors.Surface colors = glassColors(context).surface(role);
        return glassCircle(context, colors.top, colors.bottom,
                colors.edgeTop, colors.edgeBottom, borderDp);
    }

    private static int macroGlassRole(int priority) {
        if (priority == MACRO_UTILITY) {
            return ThemeGlassColors.MACRO_UTILITY;
        }
        if (priority == MACRO_PRIMARY) {
            return ThemeGlassColors.MACRO_PRIMARY;
        }
        return ThemeGlassColors.MACRO_SECONDARY;
    }

    static void applySemanticPanel(Context context, LinearLayout view, int semantic) {
        ResolvedTheme theme = resolvedTheme(context);
        SemanticStateColors.Appearance appearance =
                theme.semanticStates.appearance(semantic);
        if (theme.family() == ThemeFamily.FREYA) {
            view.setBackground(rounded(context, appearance.containerTop,
                    0, RADIUS_CARD, 0));
            return;
        }
        view.setBackground(glass(context,
                appearance.containerTop, appearance.containerBottom,
                appearance.edgeTop, appearance.edgeBottom,
                RADIUS_CARD, STROKE_HAIRLINE));
    }

    static Drawable surfacePanel(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return isFreyaFamily(context)
                ? cncRaised(context, radiusDp, false, false)
                : glassSurface(context, ThemeGlassColors.CONTROL,
                        radiusDp, STROKE_HAIRLINE);
    }

    static Drawable insetPanel(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return isFreyaFamily(context)
                ? cncInset(context, radiusDp)
                : glassSurface(context, ThemeGlassColors.INSET,
                        radiusDp, STROKE_HAIRLINE);
    }

    static Drawable fieldPanel(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return isFreyaFamily(context)
                ? new CncSurfaceDrawable(context, radiusDp,
                        CncSurfaceDrawable.FIELD, false, false)
                : glassSurface(context, ThemeGlassColors.FIELD,
                        radiusDp, STROKE_HAIRLINE);
    }

    static Drawable fullscreenToolbarPanel(Context context, int radiusDp) {
        return isFreyaFamily(context)
                ? cncFlush(context, radiusDp)
                : glassSurface(context, ThemeGlassColors.FULLSCREEN_CHROME,
                        radiusDp, STROKE_SELECTED);
    }

    static Drawable fullscreenInteractiveToolbarPanel(Context context, int radiusDp) {
        if (!isFreyaFamily(context)) {
            return fullscreenToolbarPanel(context, radiusDp);
        }
        ThemeCncColors colors = cncColors(context);
        return glass(context, colors.fullscreenToolbarTop,
                colors.fullscreenToolbarBottom, colors.fullscreenToolbarEdgeTop,
                colors.fullscreenToolbarEdgeBottom, radiusDp, STROKE_SELECTED);
    }

    static Drawable fullscreenToolbarControl(Context context, int radiusDp) {
        return isFreyaFamily(context)
                ? cncMenuControl(context, radiusDp, false, false)
                : glassSurface(context, ThemeGlassColors.FULLSCREEN_CONTROL,
                        radiusDp, STROKE_HAIRLINE);
    }

    static Drawable fullscreenRevealControl(Context context, int radiusDp) {
        return isFreyaFamily(context)
                ? cncMenuControl(context, radiusDp, false, false)
                : glassSurface(context, ThemeGlassColors.FULLSCREEN_REVEAL,
                        radiusDp, STROKE_HAIRLINE);
    }

    static Drawable cncRaised(Context context, int radiusDp, boolean accent, boolean muted) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, accent);
        }
        return new CncSurfaceDrawable(context, radiusDp, CncSurfaceDrawable.RAISED, accent, muted);
    }

    static Drawable cncControl(Context context, int radiusDp, boolean accent, boolean muted) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, accent);
        }
        return new CncSurfaceDrawable(context, radiusDp, CncSurfaceDrawable.CONTROL, accent, muted);
    }

    static Drawable cncMenuControl(Context context, int radiusDp, boolean selected, boolean muted) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, selected);
        }
        ThemePalette palette = resolvedTheme(context).palette;
        ThemeCncColors cnc = cncColors(context);
        int fill = muted ? cnc.menuMutedFill : (selected ? cnc.menuSelectedFill : cnc.menuFill);
        int stroke = selected ? withAlpha(palette.accentEdge, 0xB8)
                : (muted ? cnc.menuMutedEdge : cnc.menuEdge);
        return rounded(context, fill, stroke, radiusDp, 1);
    }

    static Drawable cncMenuPanel(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return rounded(context, cncColors(context).menuPanelFill, 0, radiusDp, 0);
    }

    static Drawable cncInset(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return new CncSurfaceDrawable(context, radiusDp, CncSurfaceDrawable.INSET, false, false);
    }

    static Drawable cncShallowInset(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return new CncSurfaceDrawable(context, radiusDp,
                CncSurfaceDrawable.SHALLOW_INSET, false, false);
    }

    static Drawable cncFlush(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return new CncSurfaceDrawable(context, radiusDp, CncSurfaceDrawable.FLUSH, false, false);
    }

    static Drawable cncInputFrame(Context context, int radiusDp) {
        return cncInputFrame(context, radiusDp, false);
    }

    static Drawable cncInputFrame(Context context, int radiusDp, boolean circular) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false, circular);
        }
        return new CncSurfaceDrawable(context, radiusDp,
                CncSurfaceDrawable.INPUT_FRAME, false, false, circular);
    }

    /** Keyboard-shell layers with vertical lighting that stays optically level at any width. */
    private static Drawable cncKeyboardShell(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return new CncSurfaceDrawable(context, radiusDp,
                CncSurfaceDrawable.INPUT_FRAME, false, false, false, true);
    }

    /** A slightly raised keyboard-shell derivative for independently pressable Macro controls. */
    private static Drawable cncMacroControl(Context context, int radiusDp,
            boolean accent, boolean muted) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, accent);
        }
        return new CncSurfaceDrawable(context, radiusDp,
                CncSurfaceDrawable.RAISED, accent, muted, false, true);
    }

    private static Drawable flatSurface(Context context, int radiusDp, boolean selected) {
        return flatSurface(context, radiusDp, selected, false);
    }

    private static Drawable flatSurface(Context context, int radiusDp, boolean selected,
            boolean circular) {
        ThemeComponentColors colors = componentColors(context);
        int fill = colors.flatSurfaceFill;
        int stroke = selected ? accent(context) : colors.flatSurfaceEdge;
        if (circular) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(fill);
            drawable.setStroke(dp(context, 1), stroke);
            return drawable;
        }
        return rounded(context, fill, stroke, radiusDp, 1);
    }

    static float concentricInnerRadiusDp(float outerRadiusDp, float insetDp) {
        return Math.max(0f, outerRadiusDp + 0.5f - insetDp);
    }

    private static final class CncSurfaceDrawable extends Drawable {
        static final int FLUSH = ThemeCncColors.FLUSH;
        static final int RAISED = ThemeCncColors.RAISED;
        static final int INSET = ThemeCncColors.INSET;
        static final int CONTROL = ThemeCncColors.CONTROL;
        static final int SHALLOW_INSET = ThemeCncColors.SHALLOW_INSET;
        static final int FIELD = ThemeCncColors.FIELD;
        static final int INPUT_FRAME = ThemeCncColors.INPUT_FRAME;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final float density;
        private final float radius;
        private final int mode;
        private final boolean accent;
        private final boolean muted;
        private final boolean circular;
        private final boolean aspectInvariantLighting;
        private final int accentColor;
        private final int accentEdgeColor;
        private final ThemeCncColors.Surface colors;
        private final int[] shellColors;
        private final int[] rimColors;
        private int alpha = 255;

        CncSurfaceDrawable(Context context, int radiusDp, int mode, boolean accent, boolean muted) {
            this(context, radiusDp, mode, accent, muted, false);
        }

        CncSurfaceDrawable(Context context, int radiusDp, int mode, boolean accent, boolean muted,
                boolean circular) {
            this(context, radiusDp, mode, accent, muted, circular, false);
        }

        CncSurfaceDrawable(Context context, int radiusDp, int mode, boolean accent, boolean muted,
                boolean circular, boolean aspectInvariantLighting) {
            density = context.getResources().getDisplayMetrics().density;
            radius = radiusDp * density;
            this.mode = mode;
            this.accent = accent;
            this.muted = muted;
            this.circular = circular;
            this.aspectInvariantLighting = aspectInvariantLighting;
            ThemePalette palette = resolvedTheme(context).palette;
            accentColor = palette.accent;
            accentEdgeColor = palette.accentEdge;
            colors = cncColors(context).surface(mode);
            shellColors = colors.shellColors();
            rimColors = colors.rimColors();
        }

        @Override
        public void draw(Canvas canvas) {
            rect.set(getBounds());
            if (rect.width() <= 0f || rect.height() <= 0f) {
                return;
            }
            rect.inset(px(0.5f), px(0.5f));
            int top = muted ? colors.mutedFaceTop : colors.faceTop;
            int bottom = muted ? colors.mutedFaceBottom : colors.faceBottom;

            float shadowOffset = mode == SHALLOW_INSET || mode == FIELD ? 0f
                    : mode == CONTROL ? px(0.35f)
                    : (mode == FLUSH ? px(0.55f) : px(1.1f));
            RectF shadow = new RectF(rect);
            shadow.inset(px(0.15f), px(0.15f));
            shadow.offset(0f, shadowOffset);
            drawSolidLayer(canvas, shadow, radius,
                    mode == FIELD ? 0x06000000
                            : mode == SHALLOW_INSET ? 0x0C000000
                            : mode == CONTROL ? 0x0E000000
                            : (mode == FLUSH ? 0x18000000 : 0x30000000));

            RectF shell = new RectF(rect);
            shell.inset(px(0.35f), px(0.35f));
            if (mode == INPUT_FRAME) {
                drawGradientLayer(canvas, shell, radius - px(0.35f),
                        shellColors,
                        new float[]{0f, 0.50f, 1f}, !aspectInvariantLighting);
            } else if (mode == INSET) {
                drawGradientLayer(canvas, shell, radius - px(0.35f),
                        shellColors,
                        new float[]{0f, 0.48f, 1f}, true);
            } else if (mode == FIELD) {
                drawGradientLayer(canvas, shell, radius - px(0.35f),
                        shellColors,
                        new float[]{0f, 0.58f, 1f}, true);
            } else if (mode == SHALLOW_INSET) {
                drawGradientLayer(canvas, shell, radius - px(0.35f),
                        shellColors,
                        new float[]{0f, 0.52f, 1f}, true);
            } else if (mode == CONTROL) {
                drawGradientLayer(canvas, shell, radius - px(0.35f),
                        shellColors,
                        new float[]{0f, 0.56f, 1f}, true);
            } else {
                drawGradientLayer(canvas, shell, radius - px(0.35f),
                        shellColors,
                        new float[]{0f, 0.52f, 1f}, !aspectInvariantLighting);
            }

            float rimInset = mode == INPUT_FRAME ? px(1.55f)
                    : mode == FIELD ? px(0.45f)
                    : mode == SHALLOW_INSET ? px(0.65f)
                    : mode == CONTROL ? px(0.7f)
                    : (mode == FLUSH ? px(0.95f) : px(1.25f));
            RectF rim = new RectF(shell);
            rim.inset(rimInset, rimInset);
            float rimRadius = Math.max(0f, radius - px(0.35f) - rimInset);
            if (mode == INPUT_FRAME) {
                drawGradientLayer(canvas, rim, rimRadius,
                        rimColors,
                        new float[]{0f, 0.64f, 1f}, !aspectInvariantLighting);
            } else if (mode == INSET) {
                drawGradientLayer(canvas, rim, rimRadius,
                        rimColors,
                        new float[]{0f, 0.55f, 1f}, true);
            } else if (mode == FIELD) {
                drawGradientLayer(canvas, rim, rimRadius,
                        rimColors,
                        new float[]{0f, 0.64f, 1f}, true);
            } else if (mode == SHALLOW_INSET) {
                drawGradientLayer(canvas, rim, rimRadius,
                        rimColors,
                        new float[]{0f, 0.58f, 1f}, true);
            } else if (mode == CONTROL) {
                drawGradientLayer(canvas, rim, rimRadius,
                        rimColors,
                        new float[]{0f, 0.62f, 1f}, true);
            } else {
                drawGradientLayer(canvas, rim, rimRadius,
                        rimColors,
                        new float[]{0f, 0.58f, 1f}, !aspectInvariantLighting);
            }

            float faceInset = mode == INPUT_FRAME ? px(1.45f)
                    : mode == FIELD ? px(0.45f)
                    : mode == SHALLOW_INSET ? px(0.7f)
                    : mode == CONTROL ? px(0.65f)
                    : (mode == FLUSH ? px(0.8f) : px(1.35f));
            RectF face = new RectF(rim);
            face.inset(faceInset, faceInset);
            float faceRadius = Math.max(0f, rimRadius - faceInset);
            drawGradientLayer(canvas, face, faceRadius,
                    new int[]{top, bottom}, null, false);

            RectF keyline = new RectF(shell);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(px(accent
                    ? (mode == CONTROL ? 0.9f : 1.15f)
                    : (mode == FIELD ? 0.35f : mode == CONTROL ? 0.5f : 0.65f)));
            paint.setShader(null);
            paint.setColor(withDrawableAlpha(accent
                    ? withAlpha(accentEdgeColor, mode == CONTROL ? 0x80 : 0x92)
                    : colors.keyline));
            drawShape(canvas, keyline, Math.max(0f, radius - px(0.35f)), paint);

            if (accent) {
                RectF glow = new RectF(rim);
                glow.inset(px(0.35f), px(0.35f));
                paint.setStrokeWidth(px(mode == CONTROL ? 2f : 3f));
                paint.setColor(withDrawableAlpha(withAlpha(
                        accentColor, mode == CONTROL ? 0x10 : 0x18)));
                drawShape(canvas, glow, Math.max(0f, rimRadius - px(0.35f)), paint);
            }
        }

        private void drawSolidLayer(Canvas canvas, RectF bounds, float layerRadius, int color) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setColor(withDrawableAlpha(color));
            drawShape(canvas, bounds, Math.max(0f, layerRadius), paint);
        }

        private void drawGradientLayer(Canvas canvas, RectF bounds, float layerRadius,
                int[] colors, float[] positions, boolean diagonal) {
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(alpha);
            float endX = diagonal ? bounds.right : bounds.left;
            float endY = bounds.bottom;
            paint.setShader(new LinearGradient(bounds.left, bounds.top, endX, endY,
                    colors, positions, Shader.TileMode.CLAMP));
            drawShape(canvas, bounds, Math.max(0f, layerRadius), paint);
            paint.setShader(null);
        }

        private void drawShape(Canvas canvas, RectF bounds, float layerRadius, Paint layerPaint) {
            if (circular) {
                canvas.drawOval(bounds, layerPaint);
            } else {
                canvas.drawRoundRect(bounds, layerRadius, layerRadius, layerPaint);
            }
        }

        private float px(float value) {
            return value * density;
        }

        private int withDrawableAlpha(int color) {
            int composedAlpha = Math.round(Color.alpha(color) * (alpha / 255f));
            return Color.argb(composedAlpha, Color.red(color), Color.green(color), Color.blue(color));
        }

        @Override
        public void setAlpha(int value) {
            alpha = value;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    static void applyNavButton(Context context, Button button, boolean selected) {
        button.setTextColor(selected ? textColor(context) : mutedTextColor(context));
        button.setTextSize(12);
        button.setIncludeFontPadding(false);
        button.setGravity(Gravity.CENTER);
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            button.setBackground(flatSurface(context, RADIUS_BUTTON, selected));
            button.setElevation(0f);
            return;
        }
        ThemeComponentColors colors = componentColors(context);
        button.setBackground(glass(context, colors.navTransparentTop,
                colors.navTransparentBottom,
                0x00000000, 0x00000000, RADIUS_BUTTON, 0));
        button.setElevation(0f);
    }

    private static Drawable navSelectedDrawable(Context context) {
        ThemePalette palette = resolvedTheme(context).palette;
        ThemeComponentColors colors = componentColors(context);
        GradientDrawable base = glassFill(context,
                colors.navTransparentTop, colors.navTransparentBottom, RADIUS_BUTTON);
        GradientDrawable indicator = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{withAlpha(palette.accent, 0), palette.accent,
                        withAlpha(palette.accentGradientEnd, 0)});
        indicator.setShape(GradientDrawable.RECTANGLE);
        indicator.setCornerRadius(dp(context, 2));
        LayerDrawable layered = new LayerDrawable(new Drawable[]{base, indicator});
        layered.setLayerInset(1, dp(context, 26), dp(context, 43), dp(context, 26), dp(context, 1));
        return layered;
    }

    static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
