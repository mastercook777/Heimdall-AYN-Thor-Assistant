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
    static final String THEME_DARK = "dark";
    static final String THEME_PEARL = "pearl";
    private static final String THEME_PREFS = "heimdall_ui";
    private static final String KEY_THEME = "theme";

    private HeimdallUi() {
    }

    static final int COLOR_BG = 0xFF070A10;
    static final int COLOR_SURFACE = 0xCC101722;
    static final int COLOR_SURFACE_RAISED = 0xD1172131;
    static final int COLOR_SURFACE_INSET = 0x990B1018;
    static final int COLOR_SURFACE_DEEP = 0xFF090D14;
    static final int COLOR_SURFACE_SOFT = 0xD91D2A3D;
    static final int COLOR_SURFACE_SELECTED = 0xD916345A;
    static final int COLOR_GLASS_TOP = 0x4DFFFFFF;
    static final int COLOR_GLASS_FILL_TOP = 0xBB101722;
    static final int COLOR_GLASS_FILL_BOTTOM = 0xCC0B1018;
    static final int COLOR_GLASS_BORDER_TOP = 0xCC62C6FF;
    static final int COLOR_GLASS_BORDER_BOTTOM = 0xAA814CFF;
    static final int COLOR_GLASS_BORDER_DIM_TOP = 0x9962C6FF;
    static final int COLOR_GLASS_BORDER_DIM_BOTTOM = 0x774B3D89;
    static final int COLOR_GLASS_ACTIVE_TOP = 0xFF4EA1FF;
    static final int COLOR_GLASS_ACTIVE_BOTTOM = 0xCC12305B;
    static final int COLOR_FOCUS_BLUE = 0xFF3F9DFF;
    static final int COLOR_FOCUS_VIOLET = 0xCC8B5CFF;
    static final int COLOR_SYSTEM_BORDER_TOP = 0x6688A8C8;
    static final int COLOR_SYSTEM_BORDER_BOTTOM = 0x44445062;
    static final int COLOR_GLASS_NEUTRAL_EDGE_TOP = 0x99D7E4F2;
    static final int COLOR_GLASS_NEUTRAL_EDGE_BOTTOM = 0x554A5666;
    static final int COLOR_ROLE_PRIMARY_TOP = 0x966FC7FF;
    static final int COLOR_ROLE_PRIMARY_BOTTOM = 0x5A4EA1FF;
    static final int COLOR_ROLE_SECONDARY_TOP = 0x6688A8C8;
    static final int COLOR_ROLE_SECONDARY_BOTTOM = 0x304E5A6A;
    static final int COLOR_ROLE_UTILITY_TOP = 0x668B78C8;
    static final int COLOR_ROLE_UTILITY_BOTTOM = 0x30483F72;
    static final int COLOR_STATE_SECTION_TOP = 0x9962C6FF;
    static final int COLOR_STATE_SECTION_BOTTOM = 0x554A5666;
    static final int COLOR_STATE_CHOICE_TOP = 0xCC62C6FF;
    static final int COLOR_STATE_CHOICE_BOTTOM = 0x884EA1FF;
    static final int COLOR_STATE_PRIMARY_TOP = 0xDD70B7FF;
    static final int COLOR_STATE_PRIMARY_BOTTOM = 0x774EA1FF;
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

    // Quick Actions belong to the control layer: neutral at rest, semantic only while active.
    static final int COLOR_QUICK_ACTION_FILL_TOP = 0xA80F1620;
    static final int COLOR_QUICK_ACTION_FILL_BOTTOM = 0xC7080C12;
    static final int COLOR_QUICK_ACTION_EDGE_TOP = 0x665F7C9A;
    static final int COLOR_QUICK_ACTION_EDGE_BOTTOM = 0x30344150;
    static final int COLOR_QUICK_ACTION_DIVIDER = 0x334E6074;
    static final int COLOR_VOLUME_TRACK = 0xFF263449;
    static final int COLOR_VOLUME_ACTIVE = 0xFF55B7E8;
    static final int COLOR_INPUT_EDGE_IDLE = 0xAA6A9DDB;
    static final int COLOR_INPUT_EDGE_ACTIVE = 0xF070B7FF;
    static final int COLOR_INPUT_GLOW_IDLE = 0x3D55B7E8;
    static final int COLOR_INPUT_GLOW_ACTIVE = 0x7755B7E8;
    static final int COLOR_INPUT_TEXTURE = 0x245A8DFF;
    static final int COLOR_INPUT_TEXTURE_ALT = 0x18FFFFFF;
    static final int COLOR_INPUT_CENTER = 0xCC70B7FF;

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
        return context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_THEME, THEME_DARK);
    }

    static void setTheme(Context context, String theme) {
        String value = THEME_PEARL.equals(theme) ? THEME_PEARL : THEME_DARK;
        context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_THEME, value).apply();
    }

    static boolean isPearl(Context context) {
        return THEME_PEARL.equals(theme(context));
    }

    static int background(Context context) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return isPearl(context) ? 0xFFE5E7E8 : 0xFF0E141B;
        }
        return isPearl(context) ? 0xFFE3E6E7 : COLOR_BG;
    }

    static int surface(Context context) {
        return isPearl(context) ? 0xFFEEF0EF : COLOR_SURFACE;
    }

    static int surfaceRaised(Context context) {
        return isPearl(context) ? 0xFFF6F5F3 : COLOR_SURFACE_RAISED;
    }

    static int surfaceInset(Context context) {
        return isPearl(context) ? 0xFFD8DCDD : COLOR_SURFACE_INSET;
    }

    static int textColor(Context context) {
        return isPearl(context) ? 0xFF263547 : COLOR_TEXT;
    }

    static int mutedTextColor(Context context) {
        return isPearl(context) ? 0xFF596A7D : COLOR_TEXT_MUTED;
    }

    static int accent(Context context) {
        return isPearl(context) ? 0xFFF08A2A : COLOR_ACCENT;
    }

    static int accentStrong(Context context) {
        return isPearl(context) ? 0xFFFFA044 : COLOR_ACCENT_STRONG;
    }

    static int border(Context context) {
        return isPearl(context) ? 0xFF9EABB8 : COLOR_BORDER;
    }

    static int quickActionDivider(Context context) {
        return isPearl(context) ? 0x66929EAA : COLOR_QUICK_ACTION_DIVIDER;
    }

    static int volumeTrack(Context context) {
        return isPearl(context) ? 0xFF8794A2 : COLOR_VOLUME_TRACK;
    }

    static int volumeActive(Context context) {
        return isPearl(context) ? 0xFFF08A2A : COLOR_VOLUME_ACTIVE;
    }

    static int inputEdge(Context context, boolean active) {
        if (isPearl(context)) {
            return active ? 0xFFF08A2A : 0xCC9AA7B4;
        }
        return active ? COLOR_INPUT_EDGE_ACTIVE : COLOR_INPUT_EDGE_IDLE;
    }

    static int inputGlow(Context context, boolean active) {
        if (isPearl(context)) {
            return active ? 0x66F08A2A : 0x339AA7B4;
        }
        return active ? COLOR_INPUT_GLOW_ACTIVE : COLOR_INPUT_GLOW_IDLE;
    }

    static int inputTexture(Context context) {
        return isPearl(context) ? 0x24FFFFFF : COLOR_INPUT_TEXTURE;
    }

    static int inputTextureAlt(Context context) {
        return isPearl(context) ? 0x18818E9B : COLOR_INPUT_TEXTURE_ALT;
    }

    static int inputCenter(Context context) {
        return isPearl(context) ? 0xFFF08A2A : COLOR_INPUT_CENTER;
    }

    static int resolveColor(Context context, int darkColor) {
        if (!isPearl(context)) {
            return darkColor;
        }
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
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp,
                    borderDp > 0 && Color.alpha(borderTopColor) >= 0x70);
        }
        if (borderDp <= 0) {
            return glassFill(context, topColor, bottomColor, radiusDp);
        }
        GradientDrawable border = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{borderTopColor, borderBottomColor});
        border.setShape(GradientDrawable.RECTANGLE);
        border.setCornerRadius(dp(context, radiusDp));

        GradientDrawable fill = glassFill(context, topColor, bottomColor, Math.max(0, radiusDp - borderDp));
        LayerDrawable layered = new LayerDrawable(new Drawable[]{border, fill});
        int inset = dp(context, borderDp);
        layered.setLayerInset(1, inset, inset, inset, inset);
        return layered;
    }

    private static GradientDrawable glassFill(Context context, int topColor, int bottomColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topColor, bottomColor});
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    static TextView text(Context context, String value, int sp, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(resolveColor(context, color));
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    static void applyModulePanel(Context context, LinearLayout view) {
        if (isPearl(context)) {
            view.setBackground(cncRaised(context, RADIUS_MODULE, false, false));
            view.setPadding(dp(context, SPACE_1), dp(context, SPACE_1), dp(context, SPACE_1), dp(context, SPACE_1));
            view.setElevation(0f);
            return;
        }
        view.setBackground(glass(context, COLOR_GLASS_FILL_TOP, COLOR_GLASS_FILL_BOTTOM,
                COLOR_GLASS_BORDER_DIM_TOP, COLOR_GLASS_BORDER_BOTTOM, RADIUS_MODULE, STROKE_MODULE));
        view.setPadding(dp(context, SPACE_1), dp(context, SPACE_1), dp(context, SPACE_1), dp(context, SPACE_1));
        view.setElevation(dp(context, 1));
    }

    static void applyHeaderPanel(Context context, LinearLayout view) {
        applySystemChromePanel(context, view);
        view.setPadding(dp(context, SPACE_3), 0, dp(context, SPACE_3), 0);
        view.setElevation(isPearl(context) ? 0f : dp(context, 2));
    }

    static void applyBottomDockPanel(Context context, LinearLayout view) {
        applySystemChromePanel(context, view);
        view.setPadding(dp(context, SPACE_1), dp(context, 2), dp(context, SPACE_1), dp(context, 2));
        view.setElevation(isPearl(context) ? 0f : dp(context, 2));
    }

    private static void applySystemChromePanel(Context context, LinearLayout view) {
        if (isPearl(context)) {
            view.setBackground(cncFlush(context, RADIUS_PANEL));
            return;
        }
        view.setBackground(glass(context, 0xCC111824, 0xE6080C12,
                COLOR_SYSTEM_BORDER_TOP, COLOR_SYSTEM_BORDER_BOTTOM, RADIUS_PANEL, STROKE_SELECTED));
    }

    static void applyInfoPill(Context context, TextView view) {
        view.setTextColor(mutedTextColor(context));
        view.setIncludeFontPadding(false);
        view.setLineSpacing(dp(context, 2), 1f);
        view.setBackground(isPearl(context)
                ? cncInset(context, RADIUS_MODULE)
                : glass(context, 0x84101824, 0xA0080C12,
                        0x445F7C9A, 0x22344150, RADIUS_MODULE, 1));
        view.setElevation(0f);
    }

    static void applyQuickActionPanel(Context context, LinearLayout view) {
        view.setBackground(isPearl(context)
                ? cncRaised(context, RADIUS_MODULE, false, false)
                : glass(context,
                        COLOR_QUICK_ACTION_FILL_TOP, COLOR_QUICK_ACTION_FILL_BOTTOM,
                        COLOR_QUICK_ACTION_EDGE_TOP, COLOR_QUICK_ACTION_EDGE_BOTTOM,
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
        button.setTextColor(textColor(context));
        button.setIncludeFontPadding(false);
        applySecondaryButton(context, button);
        button.setElevation(isPearl(context) ? 0f : dp(context, 1));
        button.setOnClickListener(v -> action.run());
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setPadding(dp(context, SPACE_2), 0, dp(context, SPACE_2), 0);
        return button;
    }

    static void applySecondaryButton(Context context, Button button) {
        button.setTextColor(textColor(context));
        button.setIncludeFontPadding(false);
        button.setBackground(isPearl(context)
                ? pearlMenuControl(context, RADIUS_BUTTON, false, false)
                : glass(context, 0xB20F1622, 0xC9090E16,
                        0x665F7C9A, 0x33344150, RADIUS_BUTTON, 1));
        button.setElevation(isPearl(context) ? 0f : dp(context, 1));
    }

    static void applySelectedButton(Context context, Button button) {
        button.setTextColor(textColor(context));
        button.setIncludeFontPadding(false);
        button.setBackground(isPearl(context)
                ? pearlMenuControl(context, RADIUS_BUTTON, true, false)
                : glass(context, 0xC3111A28, 0xD7080D16,
                        COLOR_STATE_CHOICE_TOP, COLOR_STATE_CHOICE_BOTTOM, RADIUS_BUTTON, STROKE_SELECTED));
        button.setElevation(isPearl(context) ? 0f : dp(context, 1));
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
        button.setBackground(isPearl(context)
                ? pearlMenuControl(context, RADIUS_BUTTON, true, false)
                : glass(context, 0xB20F1924, 0xC9080D14,
                        COLOR_STATE_SECTION_TOP, COLOR_STATE_SECTION_BOTTOM, RADIUS_BUTTON, STROKE_SELECTED));
        button.setElevation(0f);
    }

    static void applyPrimaryActionButton(Context context, Button button) {
        button.setTextColor(textColor(context));
        button.setIncludeFontPadding(false);
        button.setBackground(isPearl(context)
                ? pearlMenuControl(context, RADIUS_BUTTON, true, false)
                : glass(context, 0xC4142740, 0xD709111D,
                        COLOR_STATE_PRIMARY_TOP, COLOR_STATE_PRIMARY_BOTTOM, RADIUS_BUTTON, STROKE_SELECTED));
        button.setElevation(isPearl(context) ? 0f : dp(context, 1));
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
        if (isPearl(context)) {
            button.setBackground(cncRaised(context, RADIUS_CARD,
                    focused || primary, utility));
        } else {
            button.setBackground(glass(context,
                    focused ? 0xCC10233F : (utility ? 0x850C111B : 0xA6101824),
                    focused ? 0xE6091226 : (utility ? 0xAA070A10 : 0xC2070A10),
                    focused ? COLOR_FOCUS_BLUE : macroBorderTop(priority),
                    focused ? COLOR_FOCUS_VIOLET : macroBorderBottom(priority),
                    RADIUS_CARD,
                    focused || primary ? STROKE_SELECTED : STROKE_HAIRLINE));
        }
        button.setElevation(isPearl(context) ? 0f : dp(context, focused ? 2 : (primary ? 1 : 0)));
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
        if (isPearl(context)) {
            button.setBackground(pearlMenuControl(context, RADIUS_BUTTON,
                    true, priority == MACRO_UTILITY));
        } else {
            button.setBackground(glass(context, 0xB20F1622, 0xC9090E16,
                    macroBorderTop(priority), macroBorderBottom(priority), RADIUS_BUTTON, STROKE_SELECTED));
        }
        button.setElevation(isPearl(context) ? 0f : dp(context, 1));
    }

    private static int macroBorderTop(int priority) {
        if (priority == MACRO_UTILITY) {
            return COLOR_ROLE_UTILITY_TOP;
        }
        if (priority == MACRO_PRIMARY) {
            return COLOR_ROLE_PRIMARY_TOP;
        }
        return COLOR_ROLE_SECONDARY_TOP;
    }

    private static int macroBorderBottom(int priority) {
        if (priority == MACRO_UTILITY) {
            return COLOR_ROLE_UTILITY_BOTTOM;
        }
        if (priority == MACRO_PRIMARY) {
            return COLOR_ROLE_PRIMARY_BOTTOM;
        }
        return COLOR_ROLE_SECONDARY_BOTTOM;
    }

    static void applySemanticPanel(Context context, LinearLayout view, int semantic) {
        if (isPearl(context)) {
            int fill = 0x20FFFFFF;
            if (semantic == SEMANTIC_SUCCESS) {
                fill = 0x2459A979;
            } else if (semantic == SEMANTIC_WARNING) {
                fill = 0x26E77F1F;
            } else if (semantic == SEMANTIC_ERROR) {
                fill = 0x24C65C62;
            } else if (semantic == SEMANTIC_RECORDING) {
                fill = 0x2CE0525C;
            }
            view.setBackground(rounded(context, fill, 0, RADIUS_CARD, 0));
            return;
        }
        int fillTop = 0xB20F1622;
        int fillBottom = 0xC9090E16;
        int edgeTop = 0x665F7C9A;
        int edgeBottom = 0x33344150;
        if (semantic == SEMANTIC_SUCCESS) {
            fillTop = 0xA9121816;
            fillBottom = 0xC9090E0C;
            edgeTop = 0x885FD18A;
            edgeBottom = 0x33406A50;
        } else if (semantic == SEMANTIC_WARNING) {
            fillTop = 0xA9161713;
            fillBottom = 0xC90B0C0D;
            edgeTop = 0x88D8A13A;
            edgeBottom = 0x335F4822;
        } else if (semantic == SEMANTIC_ERROR) {
            fillTop = 0x9A241117;
            fillBottom = 0xB00D080A;
            edgeTop = 0x99FF6B6B;
            edgeBottom = 0x335F2A32;
        } else if (semantic == SEMANTIC_RECORDING) {
            fillTop = 0xB5281017;
            fillBottom = 0xCE10090C;
            edgeTop = 0xBBFF6B7A;
            edgeBottom = 0x555A242D;
        }
        view.setBackground(glass(context, fillTop, fillBottom,
                edgeTop, edgeBottom, RADIUS_CARD, STROKE_HAIRLINE));
    }

    static Drawable surfacePanel(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return isPearl(context)
                ? cncRaised(context, radiusDp, false, false)
                : glass(context, 0xB20F1622, 0xC9090E16,
                        0x665F7C9A, 0x33344150, radiusDp, STROKE_HAIRLINE);
    }

    static Drawable insetPanel(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return isPearl(context)
                ? cncInset(context, radiusDp)
                : glass(context, 0x76101824, 0x96070A10,
                        0x445F7C9A, 0x22344150, radiusDp, STROKE_HAIRLINE);
    }

    static Drawable fieldPanel(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return isPearl(context)
                ? new CncSurfaceDrawable(context, radiusDp,
                        CncSurfaceDrawable.FIELD, false, false)
                : insetPanel(context, radiusDp);
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

    static Drawable pearlMenuControl(Context context, int radiusDp, boolean selected, boolean muted) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, selected);
        }
        int fill = muted ? 0x12AEB7C0 : (selected ? 0x36FFFFFF : 0x20FFFFFF);
        int stroke = selected ? 0xB8E77F1F : (muted ? 0x307B8792 : 0x527B8792);
        return rounded(context, fill, stroke, radiusDp, 1);
    }

    static Drawable pearlMenuPanel(Context context, int radiusDp) {
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return rounded(context, 0x20FFFFFF, 0, radiusDp, 0);
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
        if (DebugPerformanceDiagnostics.isFlatUi()) {
            return flatSurface(context, radiusDp, false);
        }
        return new CncSurfaceDrawable(context, radiusDp,
                CncSurfaceDrawable.INPUT_FRAME, false, false);
    }

    private static Drawable flatSurface(Context context, int radiusDp, boolean selected) {
        int fill = isPearl(context) ? 0xFFE4E6E7 : 0xFF18212B;
        int stroke = selected ? accent(context)
                : (isPearl(context) ? 0xFF9CA5AD : 0xFF354353);
        return rounded(context, fill, stroke, radiusDp, 1);
    }

    static float concentricInnerRadiusDp(float outerRadiusDp, float insetDp) {
        return Math.max(0f, outerRadiusDp + 0.5f - insetDp);
    }

    private static final class CncSurfaceDrawable extends Drawable {
        static final int FLUSH = 0;
        static final int RAISED = 1;
        static final int INSET = 2;
        static final int CONTROL = 3;
        static final int SHALLOW_INSET = 4;
        static final int FIELD = 5;
        static final int INPUT_FRAME = 6;

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final float density;
        private final float radius;
        private final int mode;
        private final boolean accent;
        private final boolean muted;
        private int alpha = 255;

        CncSurfaceDrawable(Context context, int radiusDp, int mode, boolean accent, boolean muted) {
            density = context.getResources().getDisplayMetrics().density;
            radius = radiusDp * density;
            this.mode = mode;
            this.accent = accent;
            this.muted = muted;
        }

        @Override
        public void draw(Canvas canvas) {
            rect.set(getBounds());
            if (rect.width() <= 0f || rect.height() <= 0f) {
                return;
            }
            rect.inset(px(0.5f), px(0.5f));
            int top;
            int bottom;
            if (mode == INSET || mode == INPUT_FRAME) {
                top = 0xFFD2D6D8;
                bottom = 0xFFE3E6E6;
            } else if (mode == FIELD) {
                top = 0xFFF2F3F1;
                bottom = 0xFFEEF0EE;
            } else if (mode == SHALLOW_INSET) {
                top = 0xFFE1E5E6;
                bottom = 0xFFE9ECEC;
            } else if (mode == CONTROL) {
                top = muted ? 0xFFF1F1EF : 0xFFF6F6F4;
                bottom = muted ? 0xFFECEDEB : 0xFFF0F1EF;
            } else if (mode == FLUSH) {
                top = 0xFFF1F2F1;
                bottom = 0xFFE9ECEC;
            } else if (muted) {
                top = 0xFFF1F1EF;
                bottom = 0xFFE9EAE8;
            } else {
                top = 0xFFF7F6F4;
                bottom = 0xFFF0F1EF;
            }

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
                        new int[]{0xFF6D7880, 0xFFADB5BA, 0xFFF9FAF9},
                        new float[]{0f, 0.50f, 1f}, true);
            } else if (mode == INSET) {
                drawGradientLayer(canvas, shell, radius - px(0.35f),
                        new int[]{0xFF77818A, 0xFFAAB1B6, 0xFFF7F8F7},
                        new float[]{0f, 0.48f, 1f}, true);
            } else if (mode == FIELD) {
                drawGradientLayer(canvas, shell, radius - px(0.35f),
                        new int[]{0xFFCDD2D4, 0xFFE6E8E7, 0xFFF9FAF9},
                        new float[]{0f, 0.58f, 1f}, true);
            } else if (mode == SHALLOW_INSET) {
                drawGradientLayer(canvas, shell, radius - px(0.35f),
                        new int[]{0xFFA0AAB1, 0xFFD5DADD, 0xFFF8F9F8},
                        new float[]{0f, 0.52f, 1f}, true);
            } else if (mode == CONTROL) {
                drawGradientLayer(canvas, shell, radius - px(0.35f),
                        new int[]{0xFFF8F9F8, 0xFFD1D5D7, 0xFF9FA8AE},
                        new float[]{0f, 0.56f, 1f}, true);
            } else {
                drawGradientLayer(canvas, shell, radius - px(0.35f),
                        new int[]{0xFFF9FAF9, 0xFFBBC2C7, 0xFF717C85},
                        new float[]{0f, 0.52f, 1f}, true);
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
                        new int[]{0xFFFFFFFF, 0xFFFDFDFC, 0xFFC3CACF},
                        new float[]{0f, 0.64f, 1f}, true);
            } else if (mode == INSET) {
                drawGradientLayer(canvas, rim, rimRadius,
                        new int[]{0xFF8A949C, 0xFFD7DBDD, 0xFFFFFFFF},
                        new float[]{0f, 0.55f, 1f}, true);
            } else if (mode == FIELD) {
                drawGradientLayer(canvas, rim, rimRadius,
                        new int[]{0xFFFFFFFF, 0xFFF5F6F4, 0xFFDDE1E1},
                        new float[]{0f, 0.64f, 1f}, true);
            } else if (mode == SHALLOW_INSET) {
                drawGradientLayer(canvas, rim, rimRadius,
                        new int[]{0xFFB6BEC4, 0xFFE7EAEA, 0xFFFFFFFF},
                        new float[]{0f, 0.58f, 1f}, true);
            } else if (mode == CONTROL) {
                drawGradientLayer(canvas, rim, rimRadius,
                        new int[]{0xFFFFFFFF, 0xFFF5F6F5, 0xFFCDD2D5},
                        new float[]{0f, 0.62f, 1f}, true);
            } else {
                drawGradientLayer(canvas, rim, rimRadius,
                        new int[]{0xFFFFFFFF, 0xFFF2F3F2, 0xFFB8C0C5},
                        new float[]{0f, 0.58f, 1f}, true);
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
                    ? (mode == CONTROL ? 0x80E77F1F : 0x92E77F1F)
                    : (mode == FIELD ? 0x24747F87
                            : mode == SHALLOW_INSET ? 0x30747F87
                            : mode == CONTROL ? 0x34747F87
                            : (mode == FLUSH ? 0x50717C85 : 0x68717B84))));
            canvas.drawRoundRect(keyline, Math.max(0f, radius - px(0.35f)),
                    Math.max(0f, radius - px(0.35f)), paint);

            if (accent) {
                RectF glow = new RectF(rim);
                glow.inset(px(0.35f), px(0.35f));
                paint.setStrokeWidth(px(mode == CONTROL ? 2f : 3f));
                paint.setColor(withDrawableAlpha(mode == CONTROL ? 0x10F08A2A : 0x18F08A2A));
                canvas.drawRoundRect(glow, Math.max(0f, rimRadius - px(0.35f)),
                        Math.max(0f, rimRadius - px(0.35f)), paint);
            }
        }

        private void drawSolidLayer(Canvas canvas, RectF bounds, float layerRadius, int color) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setColor(withDrawableAlpha(color));
            canvas.drawRoundRect(bounds, Math.max(0f, layerRadius),
                    Math.max(0f, layerRadius), paint);
        }

        private void drawGradientLayer(Canvas canvas, RectF bounds, float layerRadius,
                int[] colors, float[] positions, boolean diagonal) {
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(alpha);
            float endX = diagonal ? bounds.right : bounds.left;
            float endY = bounds.bottom;
            paint.setShader(new LinearGradient(bounds.left, bounds.top, endX, endY,
                    colors, positions, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(bounds, Math.max(0f, layerRadius),
                    Math.max(0f, layerRadius), paint);
            paint.setShader(null);
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
        button.setBackground(glass(context, isPearl(context) ? 0x00F8FAFC : 0x00111824,
                isPearl(context) ? 0x00E1E7ED : 0x00080C12,
                0x00000000, 0x00000000, RADIUS_BUTTON, 0));
        button.setElevation(0f);
    }

    private static Drawable navSelectedDrawable(Context context) {
        boolean pearl = isPearl(context);
        GradientDrawable base = glassFill(context,
                pearl ? 0x00F8FAFC : 0x00111824,
                pearl ? 0x00E1E7ED : 0x00080C12, RADIUS_BUTTON);
        GradientDrawable indicator = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                pearl
                        ? new int[]{0x00F08A2A, 0xFFF08A2A, 0x00FFA044}
                        : new int[]{0x004EA1FF, 0xFF4EA1FF, 0x008B5CFF});
        indicator.setShape(GradientDrawable.RECTANGLE);
        indicator.setCornerRadius(dp(context, 2));
        LayerDrawable layered = new LayerDrawable(new Drawable[]{base, indicator});
        layered.setLayerInset(1, dp(context, 26), dp(context, 43), dp(context, 26), dp(context, 1));
        return layered;
    }
}
