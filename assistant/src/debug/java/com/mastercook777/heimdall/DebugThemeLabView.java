package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Debug-only, read-only coverage surface for registered theme definitions. */
final class DebugThemeLabView extends LinearLayout {
    private final List<ThemeDefinition> definitions = ThemeRegistry.selectableThemes();
    private final List<Button> selectors = new ArrayList<>();
    private final LinearLayout preview;
    private ThemeDefinition selected;

    DebugThemeLabView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setPadding(dp(8), dp(6), dp(8), dp(8));

        selected = definitionForId(HeimdallUi.theme(context));

        LinearLayout selectorRow = new LinearLayout(context);
        selectorRow.setOrientation(HORIZONTAL);
        selectorRow.setGravity(Gravity.CENTER_VERTICAL);
        addView(selectorRow, new LayoutParams(-1, dp(48)));
        for (ThemeDefinition definition : definitions) {
            Button selector = new Button(context);
            selector.setAllCaps(false);
            selector.setText(context.getString(definition.displayNameRes));
            selector.setTextSize(HeimdallUi.TYPE_BUTTON_COMPACT);
            selector.setMinWidth(0);
            selector.setMinHeight(0);
            selector.setPadding(dp(10), 0, dp(10), 0);
            selector.setOnClickListener(view -> select(definition));
            selectors.add(selector);
            LayoutParams selectorParams = new LayoutParams(0, dp(42), 1f);
            selectorParams.setMargins(dp(3), dp(2), dp(3), dp(2));
            selectorRow.addView(selector, selectorParams);
        }

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        addView(scroll, new LayoutParams(-1, 0, 1f));
        preview = new LinearLayout(context);
        preview.setOrientation(VERTICAL);
        preview.setPadding(dp(10), dp(8), dp(10), dp(14));
        scroll.addView(preview, new ScrollView.LayoutParams(-1, -2));

        render();
    }

    private ThemeDefinition definitionForId(String id) {
        for (ThemeDefinition definition : definitions) {
            if (definition.id.equals(id)) {
                return definition;
            }
        }
        return definitions.get(0);
    }

    private void select(ThemeDefinition definition) {
        if (definition == null || definition == selected) {
            return;
        }
        selected = definition;
        render();
    }

    void selectThemeForTesting(String id) {
        select(definitionForId(id));
    }

    String selectedThemeIdForTesting() {
        return selected.id;
    }

    private void render() {
        ThemePalette palette = selected.palette;
        preview.removeAllViews();
        preview.setBackgroundColor(palette.pageBackground);
        styleSelectors();

        TextView identity = label(
                getContext().getString(selected.displayNameRes)
                        + "  /  " + selected.id
                        + "  /  family=" + selected.family.name(),
                HeimdallUi.TYPE_BODY, palette.textPrimary, true);
        preview.addView(identity, block(-2, 2, 2));
        TextView contract = label(
                "Local preview only \u00b7 no preference write \u00b7 no Profile ownership",
                HeimdallUi.TYPE_META, palette.textSecondary, false);
        preview.addView(contract, block(-2, 0, 8));

        addSection("Core palette");
        addSwatchGrid(new String[]{
                "Page", "Surface", "Raised", "Inset", "Control", "Field", "Text", "Muted",
                "Disabled", "Edge", "Strong edge", "Accent", "Accent strong",
                "Focus", "Selection", "Active input"
        }, new int[]{
                palette.pageBackground, palette.surfaceBase, palette.surfaceRaised,
                palette.surfaceInset, palette.surfaceControl, palette.surfaceField,
                palette.textPrimary, palette.textSecondary,
                palette.textDisabled, palette.edgeNeutral, palette.edgeStrong,
                palette.accent, palette.accentStrong, palette.focus,
                palette.selection, palette.activeInput
        });

        addSection("Text hierarchy");
        LinearLayout typeCard = card(palette.surfaceBase, palette.edgeNeutral, 10);
        typeCard.setOrientation(VERTICAL);
        typeCard.addView(label("Page title / primary information",
                HeimdallUi.TYPE_PAGE_TITLE, palette.textPrimary, true));
        typeCard.addView(label("Body copy and ordinary values",
                HeimdallUi.TYPE_BODY, palette.textPrimary, false));
        typeCard.addView(label("Help, metadata, and secondary context",
                HeimdallUi.TYPE_HELP, palette.textSecondary, false));
        typeCard.addView(label("Disabled content remains readable but subordinate",
                HeimdallUi.TYPE_META, palette.textDisabled, false));
        preview.addView(typeCard, block(-2, 2, 8));

        addSection("Family materials");
        addMaterialRows();

        addSection("Semantic states");
        addSemanticRow(new String[]{"Neutral", "Success", "Warning", "Error", "Recording"},
                new SemanticStateColors.Appearance[]{
                        selected.semanticStates.neutral, selected.semanticStates.success,
                        selected.semanticStates.warning, selected.semanticStates.error,
                        selected.semanticStates.recording
                });
        LinearLayout semanticFlags = new LinearLayout(getContext());
        semanticFlags.setOrientation(HORIZONTAL);
        semanticFlags.addView(flag("Disabled", selected.semanticStates.disabled), weighted(1));
        semanticFlags.addView(flag("Unavailable", selected.semanticStates.unavailable), weighted(1));
        semanticFlags.addView(flag("Experimental", selected.semanticStates.experimental), weighted(1));
        preview.addView(semanticFlags, block(dp(52), 2, 8));

        addSection("Component state coverage");
        addComponentRow(new String[]{"Selection", "Focus", "Press", "Running"},
                new int[]{palette.selection, palette.focus,
                        palette.accentEdge, palette.activeInput});
        addComponentRow(new String[]{"Quick idle", "Quick pressed", "Recording", "Error"},
                new int[]{palette.edgeNeutral, palette.accent,
                        selected.semanticStates.recording.foreground,
                        selected.semanticStates.error.foreground});
        addComponentRow(new String[]{"Dock idle", "Dock selected", "Touch idle", "Touch active"},
                new int[]{palette.textSecondary, palette.selection,
                        palette.inputEdgeIdle, palette.inputEdgeActive});
        addGovernedSurfaceCoverage();

        addSection("Keyboard states");
        ThemeKeyboardColors keyboard = selected.componentColors.keyboard;
        addComponentRow(new String[]{"Key idle", "Pressed", "Modifier latched", "Modifier locked"},
                new int[]{keyboard.keycapIdleEdge, keyboard.keycapPressedEdge,
                        palette.selection, keyboard.lockedEdgeTop});

        ThemeMaterialSpec material = selected.materials;
        String materialMeta = String.format(Locale.ROOT,
                "cnc=%s  aspectInvariant=%s  mediaInset=%ddp  chromeElevation=%ddp  controlElevation=%ddp",
                material.cncSurfaces, material.aspectInvariantPlayLighting,
                material.mediaFrameContentInsetDp, material.systemChromeElevationDp,
                material.controlElevationDp);
        TextView meta = label(materialMeta, HeimdallUi.TYPE_META,
                palette.textSecondary, false);
        meta.setGravity(Gravity.CENTER_HORIZONTAL);
        preview.addView(meta, block(-2, 8, 2));
    }

    private void styleSelectors() {
        ThemePalette palette = selected.palette;
        for (int index = 0; index < selectors.size(); index++) {
            boolean active = definitions.get(index) == selected;
            Button button = selectors.get(index);
            button.setTextColor(active ? palette.textPrimary : palette.textSecondary);
            button.setBackground(shape(
                    active ? palette.surfaceRaised : palette.surfaceInset,
                    active ? palette.selection : palette.edgeNeutral,
                    active ? 2 : 1, 9));
        }
    }

    private void addSection(String title) {
        ThemePalette palette = selected.palette;
        TextView section = label(title, HeimdallUi.TYPE_SECTION_TITLE,
                palette.textPrimary, true);
        section.setGravity(Gravity.CENTER_VERTICAL);
        preview.addView(section, block(dp(30), 6, 2));
        View divider = new View(getContext());
        divider.setBackgroundColor(palette.quickActionDivider);
        preview.addView(divider, block(dp(1), 0, 4));
    }

    private void addSwatchGrid(String[] names, int[] colors) {
        for (int start = 0; start < names.length; start += 4) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            for (int index = start; index < Math.min(start + 4, names.length); index++) {
                row.addView(swatch(names[index], colors[index]), weighted(1));
            }
            for (int index = names.length; index < start + 4; index++) {
                row.addView(new View(getContext()), weighted(1));
            }
            preview.addView(row, block(dp(68), 1, 1));
        }
    }

    private View swatch(String name, int color) {
        ThemePalette palette = selected.palette;
        LinearLayout item = card(color, palette.edgeNeutral, 8);
        item.setOrientation(VERTICAL);
        item.setGravity(Gravity.BOTTOM);
        item.setPadding(dp(7), dp(5), dp(7), dp(5));
        int foreground = readableText(color);
        item.addView(label(name, HeimdallUi.TYPE_META, foreground, true));
        item.addView(label(hex(color), 9, foreground, false));
        return item;
    }

    private void addMaterialRows() {
        ThemePalette palette = selected.palette;
        LinearLayout primary = new LinearLayout(getContext());
        primary.setOrientation(HORIZONTAL);
        primary.addView(material("Flush", materialRoleColor(ThemeCncColors.FLUSH),
                palette.edgeNeutral, false), weighted(1));
        primary.addView(material("Raised", materialRoleColor(ThemeCncColors.RAISED),
                palette.edgeStrong, false), weighted(1));
        primary.addView(material("Inset", materialRoleColor(ThemeCncColors.INSET),
                palette.edgeNeutral, true), weighted(1));
        primary.addView(material("Shallow", materialRoleColor(ThemeCncColors.SHALLOW_INSET),
                palette.edgeNeutral, true), weighted(1));
        preview.addView(primary, block(dp(76), 2, 2));

        LinearLayout secondary = new LinearLayout(getContext());
        secondary.setOrientation(HORIZONTAL);
        secondary.addView(material("Control", materialRoleColor(ThemeCncColors.CONTROL),
                palette.edgeNeutral, false), weighted(1));
        secondary.addView(material("Field", materialRoleColor(ThemeCncColors.FIELD),
                palette.activeInput, true), weighted(1));
        secondary.addView(material("Media/Input",
                materialRoleColor(ThemeCncColors.INPUT_FRAME), palette.focus, true), weighted(1));
        secondary.addView(new View(getContext()), weighted(1));
        preview.addView(secondary, block(dp(76), 2, 8));
    }

    private int materialRoleColor(int role) {
        if (selected.cncColors != null) {
            return selected.cncColors.surface(role).roleColor;
        }
        if (role == ThemeCncColors.INSET) {
            return selected.glassColors.surface(ThemeGlassColors.INSET).top;
        }
        if (role == ThemeCncColors.SHALLOW_INSET) {
            return selected.glassColors.surface(ThemeGlassColors.INFO).top;
        }
        if (role == ThemeCncColors.FIELD) {
            return selected.glassColors.surface(ThemeGlassColors.FIELD).top;
        }
        if (role == ThemeCncColors.INPUT_FRAME) {
            return selected.glassColors.surface(ThemeGlassColors.KEYBOARD_FRAME).top;
        }
        return selected.glassColors.surface(role == ThemeCncColors.RAISED
                ? ThemeGlassColors.MODULE : ThemeGlassColors.CONTROL).top;
    }

    private View material(String name, int fill, int edge, boolean inset) {
        ThemePalette palette = selected.palette;
        LinearLayout item = card(fill, edge, selected.materials.cncSurfaces ? 8 : 12);
        item.setGravity(Gravity.CENTER);
        if (!inset && selected.materials.controlElevationDp > 0) {
            item.setElevation(dp(selected.materials.controlElevationDp));
        }
        TextView value = label(name + (inset ? " \u2193" : " \u2191"),
                HeimdallUi.TYPE_META, palette.textPrimary, true);
        value.setGravity(Gravity.CENTER);
        item.addView(value, new LayoutParams(-1, -1));
        return item;
    }

    private void addSemanticRow(String[] names, SemanticStateColors.Appearance[] states) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        for (int index = 0; index < names.length; index++) {
            row.addView(semantic(names[index], states[index]), weighted(1));
        }
        preview.addView(row, block(dp(72), 2, 2));
    }

    private View semantic(String name, SemanticStateColors.Appearance state) {
        LinearLayout item = new LinearLayout(getContext());
        item.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{state.containerTop, state.containerBottom});
        background.setCornerRadius(dp(9));
        background.setStroke(dp(1), state.edgeTop != 0
                ? state.edgeTop : withAlpha(state.foreground, 0x66));
        item.setBackground(background);
        TextView label = label("\u25cf  " + name, HeimdallUi.TYPE_META,
                state.foreground, true);
        label.setGravity(Gravity.CENTER);
        item.addView(label, new LayoutParams(-1, -1));
        return item;
    }

    private View flag(String name, int color) {
        LinearLayout item = card(selected.palette.surfaceInset, color, 8);
        item.setGravity(Gravity.CENTER);
        TextView label = label(name, HeimdallUi.TYPE_META, color, true);
        label.setGravity(Gravity.CENTER);
        item.addView(label, new LayoutParams(-1, -1));
        return item;
    }

    private void addComponentRow(String[] names, int[] stateColors) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        for (int index = 0; index < names.length; index++) {
            row.addView(component(names[index], stateColors[index]), weighted(1));
        }
        preview.addView(row, block(dp(58), 2, 2));
    }

    private void addGovernedSurfaceCoverage() {
        if (selected.glassColors != null) {
            ThemeGlassColors.Surface macro = selected.glassColors.surface(
                    ThemeGlassColors.MACRO_PRIMARY);
            ThemeGlassColors.Surface focused = selected.glassColors.surface(
                    ThemeGlassColors.MACRO_FOCUSED);
            ThemeGlassColors.Surface settings = selected.glassColors.surface(
                    ThemeGlassColors.SETTINGS_CONTENT);
            addComponentRow(new String[]{"Macro high body", "Macro high edge",
                            "Macro focused", "Settings content"},
                    new int[]{macro.top, macro.edgeTop, focused.edgeTop, settings.top});
            return;
        }
        addComponentRow(new String[]{"Macro high body", "Macro high edge",
                        "Macro focused", "Settings content"},
                new int[]{selected.cncColors.surface(ThemeCncColors.RAISED).roleColor,
                        selected.palette.edgeStrong, selected.palette.focus,
                        selected.cncColors.surface(ThemeCncColors.FLUSH).roleColor});
    }

    private View component(String name, int stateColor) {
        ThemePalette palette = selected.palette;
        LinearLayout item = card(palette.surfaceRaised, stateColor, 9);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(8), 0, dp(8), 0);
        View indicator = new View(getContext());
        indicator.setBackground(shape(stateColor, stateColor, 0, 99));
        item.addView(indicator, new LayoutParams(dp(8), dp(8)));
        TextView value = label(name, HeimdallUi.TYPE_META, palette.textPrimary, true);
        LayoutParams valueParams = new LayoutParams(0, -1, 1f);
        valueParams.setMargins(dp(7), 0, 0, 0);
        item.addView(value, valueParams);
        return item;
    }

    private LinearLayout card(int fill, int stroke, int radiusDp) {
        LinearLayout card = new LinearLayout(getContext());
        card.setBackground(shape(fill, stroke, 1, radiusDp));
        return card;
    }

    private GradientDrawable shape(int fill, int stroke, int strokeDp, int radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) {
            shape.setStroke(dp(strokeDp), stroke);
        }
        return shape;
    }

    private TextView label(String value, int sp, int color, boolean bold) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setIncludeFontPadding(false);
        if (bold) {
            text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return text;
    }

    private LayoutParams weighted(float weight) {
        LayoutParams params = new LayoutParams(0, -1, weight);
        params.setMargins(dp(3), dp(2), dp(3), dp(2));
        return params;
    }

    private LayoutParams block(int heightPx, int topDp, int bottomDp) {
        LayoutParams params = new LayoutParams(-1, heightPx);
        params.setMargins(0, dp(topDp), 0, dp(bottomDp));
        return params;
    }

    private int readableText(int background) {
        double luminance = (0.299d * Color.red(background)
                + 0.587d * Color.green(background)
                + 0.114d * Color.blue(background)) / 255d;
        return luminance > 0.58d ? 0xFF17212C : Color.WHITE;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private String hex(int color) {
        return String.format(Locale.ROOT, "#%08X", color);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
