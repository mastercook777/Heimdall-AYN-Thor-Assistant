package com.mastercook777.heimdall;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Temporary, non-IME US ANSI keyboard surface for the Thor lower display. */
@SuppressLint("ViewConstructor")
final class FullVirtualKeyboardView extends LinearLayout {
    interface Listener {
        void onPrepareInput();
        void onKeyDown(Object token, int linuxKeyCode);
        void onKeyUp(Object token);
        void onReleaseAll();
        void onMenuRequested();
        void onClose();
    }

    private static final int LAYER_PRIMARY = 0;
    private static final int LAYER_NAVIGATION = 1;
    private static final int MOD_OFF = 0;
    private static final int MOD_MOMENTARY = 1;
    private static final int MOD_LATCHED = 2;
    private static final int MOD_LOCKED = 3;
    private static final long MODIFIER_DOUBLE_TAP_MS = 360L;

    private final Listener listener;
    private final FullKeyboardMenuView menuView;
    private final LinearLayout keyboardBed;
    private final Button layerButton;
    private final List<KeycapView> keycaps = new ArrayList<>();
    private final Map<Integer, ModifierState> modifiers = new HashMap<>();
    private int layer = LAYER_PRIMARY;
    private int activeOrdinaryKeys;
    private boolean inputAvailable;
    private boolean released;

    FullVirtualKeyboardView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setPadding(dp(8), dp(6), dp(8), dp(8));
        setClickable(true);
        setFocusable(false);
        setBackground(HeimdallUi.isPearl(context)
                ? HeimdallUi.cncInputFrame(context, 14)
                : HeimdallUi.glass(context, 0xFC0B111B, 0xFF06090E,
                        0xA66A829C, 0x55344150, 14, 2));

        LinearLayout toolbar = new LinearLayout(context);
        toolbar.setOrientation(HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        addView(toolbar, new LayoutParams(LayoutParams.MATCH_PARENT, dp(46)));

        menuView = new FullKeyboardMenuView(context);
        menuView.setOnClickListener(view -> listener.onMenuRequested());
        toolbar.addView(menuView, new LayoutParams(dp(56), LayoutParams.MATCH_PARENT));

        View toolbarSpacer = new View(context);
        toolbar.addView(toolbarSpacer, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

        Button releaseAll = toolbarButton(context.getString(R.string.full_keyboard_release_all));
        releaseAll.setOnClickListener(view -> releaseAll());
        toolbar.addView(releaseAll, new LayoutParams(dp(104), dp(36)));

        layerButton = toolbarButton(context.getString(R.string.full_keyboard_navigation));
        layerButton.setOnClickListener(view -> switchLayer());
        LayoutParams layerParams = new LayoutParams(dp(82), dp(36));
        layerParams.setMargins(dp(6), 0, 0, 0);
        toolbar.addView(layerButton, layerParams);

        Button close = toolbarButton(context.getString(R.string.common_close));
        close.setOnClickListener(view -> listener.onClose());
        LayoutParams closeParams = new LayoutParams(dp(72), dp(36));
        closeParams.setMargins(dp(6), 0, 0, 0);
        toolbar.addView(close, closeParams);

        keyboardBed = new LinearLayout(context);
        keyboardBed.setOrientation(VERTICAL);
        keyboardBed.setMotionEventSplittingEnabled(true);
        keyboardBed.setPadding(dp(6), dp(6), dp(6), dp(6));
        keyboardBed.setBackground(keyboardBedBackground(context));
        LayoutParams bedParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        bedParams.setMargins(0, dp(4), 0, 0);
        addView(keyboardBed, bedParams);
        rebuildKeyboardLayer();
    }

    void prepare() {
        released = false;
        setConnecting();
        listener.onPrepareInput();
    }

    void setReady() {
        if (released) return;
        inputAvailable = true;
        menuView.setTransportState(HeimdallUi.COLOR_SUCCESS,
                R.string.full_keyboard_status_ready);
        updateEnabledState();
    }

    void setUnavailable() {
        inputAvailable = false;
        clearVisualState();
        menuView.setTransportState(HeimdallUi.COLOR_DANGER,
                R.string.full_keyboard_status_unavailable);
        updateEnabledState();
    }

    void releaseForDismiss() {
        if (released) return;
        released = true;
        clearVisualState();
        listener.onReleaseAll();
    }

    private void setConnecting() {
        inputAvailable = false;
        menuView.setTransportState(HeimdallUi.mutedTextColor(getContext()),
                R.string.full_keyboard_status_connecting);
        updateEnabledState();
    }

    private void switchLayer() {
        releaseAll();
        layer = layer == LAYER_PRIMARY ? LAYER_NAVIGATION : LAYER_PRIMARY;
        layerButton.setText(layer == LAYER_PRIMARY
                ? R.string.full_keyboard_navigation : R.string.full_keyboard_primary_layer);
        rebuildKeyboardLayer();
    }

    private void rebuildKeyboardLayer() {
        for (KeycapView keycap : keycaps) {
            keycap.cancelInput(false);
        }
        keycaps.clear();
        modifiers.clear();
        activeOrdinaryKeys = 0;
        keyboardBed.removeAllViews();
        if (layer == LAYER_NAVIGATION) {
            buildNavigationLayer();
        } else {
            buildPrimaryLayer();
        }
        updateEnabledState();
    }

    private void buildPrimaryLayer() {
        addRow(
                key("Esc", KeyboardKeyCatalog.KEY_ESC, 1.35f), spacer(0.35f),
                key("F1", KeyboardKeyCatalog.KEY_F1), key("F2", KeyboardKeyCatalog.KEY_F2),
                key("F3", KeyboardKeyCatalog.KEY_F3), key("F4", KeyboardKeyCatalog.KEY_F4),
                spacer(0.24f),
                key("F5", KeyboardKeyCatalog.KEY_F5), key("F6", KeyboardKeyCatalog.KEY_F6),
                key("F7", KeyboardKeyCatalog.KEY_F7), key("F8", KeyboardKeyCatalog.KEY_F8),
                spacer(0.24f),
                key("F9", KeyboardKeyCatalog.KEY_F9), key("F10", KeyboardKeyCatalog.KEY_F10),
                key("F11", KeyboardKeyCatalog.KEY_F11), key("F12", KeyboardKeyCatalog.KEY_F12));
        addRow(
                key("~\n`", KeyboardKeyCatalog.KEY_GRAVE),
                key("!\n1", KeyboardKeyCatalog.KEY_1), key("@\n2", KeyboardKeyCatalog.KEY_2),
                key("#\n3", KeyboardKeyCatalog.KEY_3), key("$\n4", KeyboardKeyCatalog.KEY_4),
                key("%\n5", KeyboardKeyCatalog.KEY_5), key("^\n6", KeyboardKeyCatalog.KEY_6),
                key("&\n7", KeyboardKeyCatalog.KEY_7), key("*\n8", KeyboardKeyCatalog.KEY_8),
                key("(\n9", KeyboardKeyCatalog.KEY_9), key(")\n0", KeyboardKeyCatalog.KEY_0),
                key("_\n-", KeyboardKeyCatalog.KEY_MINUS),
                key("+\n=", KeyboardKeyCatalog.KEY_EQUAL),
                key("Backspace", KeyboardKeyCatalog.KEY_BACKSPACE, 2f));
        addRow(
                key("Tab", KeyboardKeyCatalog.KEY_TAB, 1.5f),
                key("Q", KeyboardKeyCatalog.KEY_Q), key("W", KeyboardKeyCatalog.KEY_W),
                key("E", KeyboardKeyCatalog.KEY_E), key("R", KeyboardKeyCatalog.KEY_R),
                key("T", KeyboardKeyCatalog.KEY_T), key("Y", KeyboardKeyCatalog.KEY_Y),
                key("U", KeyboardKeyCatalog.KEY_U), key("I", KeyboardKeyCatalog.KEY_I),
                key("O", KeyboardKeyCatalog.KEY_O), key("P", KeyboardKeyCatalog.KEY_P),
                key("{\n[", KeyboardKeyCatalog.KEY_LEFTBRACE),
                key("}\n]", KeyboardKeyCatalog.KEY_RIGHTBRACE),
                key("|\n\\", KeyboardKeyCatalog.KEY_BACKSLASH, 1.5f));
        addRow(
                key("Caps", KeyboardKeyCatalog.KEY_CAPSLOCK, 1.8f),
                key("A", KeyboardKeyCatalog.KEY_A), key("S", KeyboardKeyCatalog.KEY_S),
                key("D", KeyboardKeyCatalog.KEY_D), key("F", KeyboardKeyCatalog.KEY_F),
                key("G", KeyboardKeyCatalog.KEY_G), key("H", KeyboardKeyCatalog.KEY_H),
                key("J", KeyboardKeyCatalog.KEY_J), key("K", KeyboardKeyCatalog.KEY_K),
                key("L", KeyboardKeyCatalog.KEY_L),
                key(":\n;", KeyboardKeyCatalog.KEY_SEMICOLON),
                key("\"\n'", KeyboardKeyCatalog.KEY_APOSTROPHE),
                key("Enter", KeyboardKeyCatalog.KEY_ENTER, 2.2f));
        addRow(
                modifier("Shift", KeyboardKeyCatalog.KEY_LEFTSHIFT, 2.4f),
                key("Z", KeyboardKeyCatalog.KEY_Z), key("X", KeyboardKeyCatalog.KEY_X),
                key("C", KeyboardKeyCatalog.KEY_C), key("V", KeyboardKeyCatalog.KEY_V),
                key("B", KeyboardKeyCatalog.KEY_B), key("N", KeyboardKeyCatalog.KEY_N),
                key("M", KeyboardKeyCatalog.KEY_M),
                key("<\n,", KeyboardKeyCatalog.KEY_COMMA),
                key(">\n.", KeyboardKeyCatalog.KEY_DOT),
                key("?\n/", KeyboardKeyCatalog.KEY_SLASH),
                modifier("Shift", KeyboardKeyCatalog.KEY_RIGHTSHIFT, 2.6f));
        addRow(
                modifier("Ctrl", KeyboardKeyCatalog.KEY_LEFTCTRL, 1.3f),
                unavailableModifier("Win", KeyboardKeyCatalog.KEY_LEFTMETA, 1.1f),
                modifier("Alt", KeyboardKeyCatalog.KEY_LEFTALT, 1.1f),
                key("Space", KeyboardKeyCatalog.KEY_SPACE, 6f),
                modifier("Alt", KeyboardKeyCatalog.KEY_RIGHTALT, 1.1f),
                unavailableModifier("Win", KeyboardKeyCatalog.KEY_RIGHTMETA, 1.1f),
                modifier("Ctrl", KeyboardKeyCatalog.KEY_RIGHTCTRL, 1.3f));
    }

    private void buildNavigationLayer() {
        LinearLayout shell = new LinearLayout(getContext());
        shell.setOrientation(HORIZONTAL);
        shell.setMotionEventSplittingEnabled(true);
        keyboardBed.addView(shell, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout arrows = navigationColumn();
        addRowTo(arrows, spacer(1f), key("Up", KeyboardKeyCatalog.KEY_UP, 1.25f), spacer(1f));
        addRowTo(arrows, key("Left", KeyboardKeyCatalog.KEY_LEFT),
                key("Down", KeyboardKeyCatalog.KEY_DOWN),
                key("Right", KeyboardKeyCatalog.KEY_RIGHT));
        addRowTo(arrows, key("Esc", KeyboardKeyCatalog.KEY_ESC),
                key("Tab", KeyboardKeyCatalog.KEY_TAB),
                key("Enter", KeyboardKeyCatalog.KEY_ENTER));
        addRowTo(arrows, modifier("Ctrl", KeyboardKeyCatalog.KEY_LEFTCTRL, 1f),
                modifier("Shift", KeyboardKeyCatalog.KEY_LEFTSHIFT, 1f),
                modifier("Alt", KeyboardKeyCatalog.KEY_LEFTALT, 1f));
        addNavigationSection(shell, arrows, 1.05f);

        GridLayout functions = navigationGrid(4, 2);
        addGridKey(functions, key("Insert", KeyboardKeyCatalog.KEY_INSERT), 0, 0);
        addGridKey(functions, key("Home", KeyboardKeyCatalog.KEY_HOME), 0, 1);
        addGridKey(functions, key("Delete", KeyboardKeyCatalog.KEY_DELETE), 1, 0);
        addGridKey(functions, key("End", KeyboardKeyCatalog.KEY_END), 1, 1);
        addGridKey(functions, key("Page Up", KeyboardKeyCatalog.KEY_PAGEUP), 2, 0);
        addGridKey(functions, key("Page Down", KeyboardKeyCatalog.KEY_PAGEDOWN), 2, 1);
        addGridKey(functions, key("Backspace", KeyboardKeyCatalog.KEY_BACKSPACE), 3, 0);
        addGridKey(functions,
                unavailableModifier("Win", KeyboardKeyCatalog.KEY_LEFTMETA, 1f), 3, 1);
        addNavigationSection(shell, functions, 0.9f);

        GridLayout numpad = navigationGrid(5, 4);
        addGridKey(numpad, key("Num", KeyboardKeyCatalog.KEY_NUMLOCK), 0, 0);
        addGridKey(numpad, key("/", KeyboardKeyCatalog.KEY_KPSLASH), 0, 1);
        addGridKey(numpad, key("*", KeyboardKeyCatalog.KEY_KPASTERISK), 0, 2);
        addGridKey(numpad, key("-", KeyboardKeyCatalog.KEY_KPMINUS), 0, 3);
        addGridKey(numpad, key("7", KeyboardKeyCatalog.KEY_KP7), 1, 0);
        addGridKey(numpad, key("8", KeyboardKeyCatalog.KEY_KP8), 1, 1);
        addGridKey(numpad, key("9", KeyboardKeyCatalog.KEY_KP9), 1, 2);
        addGridKey(numpad, key("+", KeyboardKeyCatalog.KEY_KPPLUS), 1, 2, 3, 1);
        addGridKey(numpad, key("4", KeyboardKeyCatalog.KEY_KP4), 2, 0);
        addGridKey(numpad, key("5", KeyboardKeyCatalog.KEY_KP5), 2, 1);
        addGridKey(numpad, key("6", KeyboardKeyCatalog.KEY_KP6), 2, 2);
        addGridKey(numpad, key("1", KeyboardKeyCatalog.KEY_KP1), 3, 0);
        addGridKey(numpad, key("2", KeyboardKeyCatalog.KEY_KP2), 3, 1);
        addGridKey(numpad, key("3", KeyboardKeyCatalog.KEY_KP3), 3, 2);
        addGridKey(numpad, key("Enter", KeyboardKeyCatalog.KEY_KPENTER), 3, 2, 3, 1);
        addGridKey(numpad, key("0", KeyboardKeyCatalog.KEY_KP0), 4, 1, 0, 2);
        addGridKey(numpad, key(".", KeyboardKeyCatalog.KEY_KPDOT), 4, 2);
        addNavigationSection(shell, numpad, 1.35f);
    }

    private void addRow(KeySpec... specs) {
        addRowTo(keyboardBed, specs);
    }

    private void addRowTo(LinearLayout parent, KeySpec... specs) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setMotionEventSplittingEnabled(true);
        row.setGravity(Gravity.CENTER);
        LayoutParams rowParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        rowParams.setMargins(0, dp(2), 0, dp(2));
        parent.addView(row, rowParams);
        for (KeySpec spec : specs) {
            if (spec.spacer) {
                row.addView(new View(getContext()), new LayoutParams(0,
                        LayoutParams.MATCH_PARENT, spec.units));
                continue;
            }
            KeycapView keycap = new KeycapView(getContext(), spec);
            keycaps.add(keycap);
            LayoutParams keyParams = new LayoutParams(0, LayoutParams.MATCH_PARENT, spec.units);
            keyParams.setMargins(dp(2), 0, dp(2), 0);
            row.addView(keycap, keyParams);
        }
    }

    private LinearLayout navigationColumn() {
        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(VERTICAL);
        column.setMotionEventSplittingEnabled(true);
        return column;
    }

    private GridLayout navigationGrid(int rows, int columns) {
        GridLayout grid = new GridLayout(getContext());
        grid.setRowCount(rows);
        grid.setColumnCount(columns);
        grid.setOrientation(GridLayout.HORIZONTAL);
        grid.setMotionEventSplittingEnabled(true);
        return grid;
    }

    private void addNavigationSection(LinearLayout shell, View section, float weight) {
        LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT, weight);
        params.setMargins(dp(3), dp(2), dp(3), dp(2));
        shell.addView(section, params);
    }

    private void addGridKey(GridLayout grid, KeySpec spec, int row, int column) {
        addGridKey(grid, spec, row, 1, column, 1);
    }

    private void addGridKey(GridLayout grid, KeySpec spec, int row, int rowSpan,
            int column, int columnSpan) {
        KeycapView keycap = new KeycapView(getContext(), spec);
        keycaps.add(keycap);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(row, rowSpan, 1f),
                GridLayout.spec(column, columnSpan, 1f));
        params.width = 0;
        params.height = 0;
        params.setGravity(Gravity.FILL);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        grid.addView(keycap, params);
    }

    private void ordinaryDown(KeycapView keycap) {
        if (!inputAvailable || keycap.inputDown) return;
        keycap.inputDown = true;
        activeOrdinaryKeys++;
        for (ModifierState modifier : modifiers.values()) {
            if (modifier.mode != MOD_OFF) modifier.used = true;
        }
        listener.onKeyDown(keycap, keycap.spec.code);
    }

    private void ordinaryUp(KeycapView keycap) {
        if (!keycap.inputDown) return;
        keycap.inputDown = false;
        listener.onKeyUp(keycap);
        activeOrdinaryKeys = Math.max(0, activeOrdinaryKeys - 1);
        if (activeOrdinaryKeys == 0) {
            releaseLatchedModifiers();
        }
    }

    private void modifierDown(KeycapView keycap) {
        if (!inputAvailable) return;
        ModifierState state = modifiers.get(keycap.spec.code);
        if (state == null) return;
        long now = SystemClock.uptimeMillis();
        if (state.mode == MOD_LOCKED) {
            listener.onKeyUp(state.token);
            state.reset();
            keycap.applyVisualState();
            return;
        }
        if (state.mode == MOD_LATCHED) {
            if (now - state.lastTapMs <= MODIFIER_DOUBLE_TAP_MS) {
                state.mode = MOD_LOCKED;
                state.used = false;
            } else {
                listener.onKeyUp(state.token);
                state.reset();
            }
            keycap.applyVisualState();
            return;
        }
        if (state.mode == MOD_OFF) {
            state.mode = MOD_MOMENTARY;
            state.used = false;
            listener.onKeyDown(state.token, keycap.spec.code);
            keycap.applyVisualState();
        }
    }

    private void modifierUp(KeycapView keycap) {
        ModifierState state = modifiers.get(keycap.spec.code);
        if (state == null || state.mode != MOD_MOMENTARY) return;
        if (state.used) {
            listener.onKeyUp(state.token);
            state.reset();
        } else {
            state.mode = MOD_LATCHED;
            state.lastTapMs = SystemClock.uptimeMillis();
        }
        keycap.applyVisualState();
    }

    private void releaseLatchedModifiers() {
        for (ModifierState state : modifiers.values()) {
            if (state.mode == MOD_LATCHED) {
                listener.onKeyUp(state.token);
                state.reset();
                state.keycap.applyVisualState();
            }
        }
    }

    private void releaseAll() {
        clearVisualState();
        listener.onReleaseAll();
    }

    private void clearVisualState() {
        for (KeycapView keycap : keycaps) {
            keycap.inputDown = false;
            keycap.touchDown = false;
            keycap.setPressed(false);
            keycap.animate().cancel();
            keycap.setTranslationY(0f);
        }
        for (ModifierState state : modifiers.values()) {
            state.reset();
        }
        activeOrdinaryKeys = 0;
        for (KeycapView keycap : keycaps) keycap.applyVisualState();
    }

    private void updateEnabledState() {
        for (KeycapView keycap : keycaps) {
            keycap.setEnabled(!keycap.spec.unavailable
                    && (inputAvailable || keycap.spec.action != null));
            keycap.setAlpha(keycap.isEnabled() ? 1f : 0.38f);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        clearVisualState();
        super.onDetachedFromWindow();
    }

    private Button toolbarButton(String text) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setTextSize(10);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(HeimdallUi.textColor(getContext()));
        button.setPadding(dp(6), 0, dp(6), 0);
        button.setBackground(HeimdallUi.isPearl(getContext())
                ? HeimdallUi.cncInputFrame(getContext(), 8)
                : HeimdallUi.glass(getContext(), 0xB5182330, 0xD00B1119,
                        0x776A829C, 0x33344150, 8, 1));
        if (HeimdallUi.isPearl(getContext())) {
            button.setElevation(0f);
            button.setStateListAnimator(null);
        }
        return button;
    }

    private Drawable keyboardBedBackground(Context context) {
        if (!HeimdallUi.isPearl(context)) {
            return HeimdallUi.insetPanel(context, 12);
        }
        GradientDrawable bed = new GradientDrawable();
        bed.setShape(GradientDrawable.RECTANGLE);
        bed.setColor(0xFF454A50);
        bed.setCornerRadius(dp(10));
        bed.setStroke(dp(1), 0xFF737A81);
        return bed;
    }

    private KeySpec key(String label, int code) {
        return key(label, code, 1f);
    }

    private KeySpec key(String label, int code, float units) {
        return new KeySpec(label, code, units, false, null);
    }

    private KeySpec modifier(String label, int code, float units) {
        return new KeySpec(label, code, units, true, null);
    }

    private KeySpec unavailableModifier(String label, int code, float units) {
        return new KeySpec(label, code, units, true, null, false, true);
    }

    private KeySpec actionKey(String label, float units, Runnable action) {
        return new KeySpec(label, 0, units, false, action);
    }

    private KeySpec spacer(float units) {
        return new KeySpec("", 0, units, false, null, true);
    }

    private int dp(int value) {
        return HeimdallUi.dp(getContext(), value);
    }

    private final class KeycapView extends TextView {
        final KeySpec spec;
        boolean touchDown;
        boolean inputDown;

        KeycapView(Context context, KeySpec spec) {
            super(context);
            this.spec = spec;
            setText(spec.label);
            setTextSize(spec.label.length() > 8 ? 8 : 10);
            setTextColor(HeimdallUi.textColor(context));
            setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            setGravity(Gravity.CENTER);
            setIncludeFontPadding(false);
            setPadding(dp(3), dp(1), dp(3), dp(1));
            setClickable(true);
            setFocusable(false);
            setContentDescription(spec.unavailable
                    ? context.getString(R.string.full_keyboard_win_unavailable_description)
                    : spec.label.replace('\n', ' '));
            if (spec.modifier && !spec.unavailable) {
                ModifierState state = new ModifierState(this);
                modifiers.put(spec.code, state);
            }
            applyVisualState();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                if (!isEnabled()) return true;
                touchDown = true;
                setPressed(true);
                animatePress(true);
                if (spec.action != null) {
                    return true;
                }
                if (spec.modifier) modifierDown(this); else ordinaryDown(this);
                applyVisualState();
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                boolean inside = event.getX() >= 0 && event.getY() >= 0
                        && event.getX() <= getWidth() && event.getY() <= getHeight();
                if (!inside && touchDown) cancelInput(true);
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                boolean activate = touchDown && isPressed();
                touchDown = false;
                setPressed(false);
                animatePress(false);
                if (spec.action != null) {
                    if (activate) spec.action.run();
                } else if (spec.modifier) {
                    modifierUp(this);
                } else {
                    ordinaryUp(this);
                }
                if (activate) performClick();
                applyVisualState();
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL) {
                cancelInput(true);
                return true;
            }
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        void cancelInput(boolean sendUp) {
            touchDown = false;
            setPressed(false);
            animatePress(false);
            if (spec.action == null) {
                if (spec.modifier) {
                    ModifierState state = modifiers.get(spec.code);
                    if (sendUp && state != null && state.mode == MOD_MOMENTARY) {
                        listener.onKeyUp(state.token);
                        state.reset();
                    }
                } else if (sendUp) {
                    ordinaryUp(this);
                } else {
                    inputDown = false;
                }
            }
            applyVisualState();
        }

        void animatePress(boolean down) {
            animate().cancel();
            if (ValueAnimator.areAnimatorsEnabled()
                    && !DebugPerformanceDiagnostics.isFlatUi()) {
                animate().translationY(down ? dp(1) : 0f)
                        .setDuration(down ? 45L : 75L).start();
            } else {
                setTranslationY(down ? dp(1) : 0f);
            }
        }

        void applyVisualState() {
            ModifierState modifier = spec.modifier ? modifiers.get(spec.code) : null;
            boolean active = isPressed() || inputDown
                    || (modifier != null && modifier.mode != MOD_OFF);
            boolean locked = modifier != null && modifier.mode == MOD_LOCKED;
            Drawable background;
            if (HeimdallUi.isPearl(getContext())) {
                background = HeimdallUi.cncRaised(getContext(), 7, active, false);
            } else {
                background = HeimdallUi.glass(getContext(),
                        active ? 0xFF24364A : 0xFF2B2D30,
                        active ? 0xFF101A26 : 0xFF17191C,
                        locked ? 0xFFE7B45B : (active ? 0xFF70B7FF : 0x99717A82),
                        locked ? 0x99D48A35 : (active ? 0x884EA1FF : 0x55323539),
                        7, active ? 2 : 1);
                background = new InsetDrawable(background, dp(1));
            }
            setBackground(background);
            setTextColor(locked && HeimdallUi.isPearl(getContext())
                    ? 0xFFE77F1F : HeimdallUi.textColor(getContext()));
        }
    }

    /** Matches the Keypad chassis rail while reserving a stable settings anchor. */
    private final class FullKeyboardMenuView extends View {
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int transportColor;
        private int transportDescriptionRes;

        FullKeyboardMenuView(Context context) {
            super(context);
            setClickable(true);
            setFocusable(false);
            setTransportState(HeimdallUi.mutedTextColor(context),
                    R.string.full_keyboard_status_connecting);
        }

        void setTransportState(int color, int descriptionRes) {
            transportColor = color;
            transportDescriptionRes = descriptionRes;
            setContentDescription(getContext().getString(
                    R.string.full_keyboard_menu_status_description,
                    getContext().getString(R.string.full_keyboard_menu_description),
                    getContext().getString(transportDescriptionRes)));
            invalidate();
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            boolean pearl = HeimdallUi.isPearl(getContext());
            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            for (int index = 0; index < 3; index++) {
                float x = centerX + dp((index - 1) * 12);
                dotPaint.setColor(pearl ? 0x66717A82 : 0x88414A53);
                canvas.drawCircle(x, centerY, dp(3), dotPaint);
                dotPaint.setColor(transportColor);
                canvas.drawCircle(x, centerY, dp(2), dotPaint);
            }
        }
    }

    private static final class ModifierState {
        final Object token = new Object();
        final KeycapView keycap;
        int mode = MOD_OFF;
        boolean used;
        long lastTapMs;

        ModifierState(KeycapView keycap) {
            this.keycap = keycap;
        }

        void reset() {
            mode = MOD_OFF;
            used = false;
            lastTapMs = 0L;
        }
    }

    private static final class KeySpec {
        final String label;
        final int code;
        final float units;
        final boolean modifier;
        final Runnable action;
        final boolean spacer;
        final boolean unavailable;

        KeySpec(String label, int code, float units, boolean modifier, Runnable action) {
            this(label, code, units, modifier, action, false, false);
        }

        KeySpec(String label, int code, float units, boolean modifier,
                Runnable action, boolean spacer) {
            this(label, code, units, modifier, action, spacer, false);
        }

        KeySpec(String label, int code, float units, boolean modifier,
                Runnable action, boolean spacer, boolean unavailable) {
            this.label = label;
            this.code = code;
            this.units = units;
            this.modifier = modifier;
            this.action = action;
            this.spacer = spacer;
            this.unavailable = unavailable;
        }
    }
}
