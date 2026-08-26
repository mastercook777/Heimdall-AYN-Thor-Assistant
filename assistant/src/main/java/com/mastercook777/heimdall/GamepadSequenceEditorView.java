package com.mastercook777.heimdall;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Structured editor surface for short digital controller moves. */
@SuppressLint("ViewConstructor")
final class GamepadSequenceEditorView extends LinearLayout {
    interface TextInputBinder {
        void bind(EditText input);
    }

    private static final int[][] DIRECTIONS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0}, {0, 0}, {1, 0},
            {-1, 1}, {0, 1}, {1, 1}
    };

    private final NativeGamepadPath.Device device;
    private final List<GamepadSequenceComposer.Frame> frames = new ArrayList<>();
    private final Set<Integer> selectedButtons = new LinkedHashSet<>();
    private final List<Button> supportedActionButtons = new ArrayList<>();
    private final List<Button> unavailableActionButtons = new ArrayList<>();
    private final Button[] directionButtons = new Button[DIRECTIONS.length];
    private final LinearLayout sequenceList;
    private final TextView currentAction;
    private final TextView sequenceTitle;
    private final TextView status;
    private final EditText intervalInput;
    private final EditText holdInput;
    private final EditText currentHoldInput;
    private final Button currentHoldToggle;
    private int directionX;
    private int directionY;
    private boolean currentLongHold;

    GamepadSequenceEditorView(Context context, NativeGamepadPath.Device device) {
        this(context, device, null);
    }

    GamepadSequenceEditorView(Context context, NativeGamepadPath.Device device,
            GamepadSequenceComposer.EditableSequence restored) {
        super(context);
        this.device = device;
        setOrientation(VERTICAL);
        setPadding(dp(12), dp(8), dp(12), dp(8));

        ScrollView contentScroll = new ScrollView(context);
        contentScroll.setFillViewport(false);
        contentScroll.setScrollbarFadingEnabled(false);
        contentScroll.setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
        addView(contentScroll, new LayoutParams(MATCH_PARENT, 0, 1));

        LinearLayout content = column();
        content.setPadding(0, 0, 0, dp(4));
        contentScroll.addView(content, new ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        LinearLayout deviceRow = row();
        deviceRow.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(deviceRow, new LayoutParams(MATCH_PARENT, dp(26)));
        TextView controller = label(context.getString(R.string.gamepad_compose_controller,
                device.name, context.getString(device.dpadEncoding()
                        == GamepadSequenceComposer.DPAD_HAT
                        ? R.string.gamepad_compose_dpad_hat
                        : R.string.gamepad_compose_dpad_keys)), 10, false);
        controller.setTextColor(HeimdallUi.mutedTextColor(context));
        controller.setSingleLine(true);
        deviceRow.addView(controller, new LayoutParams(0, MATCH_PARENT, 1));

        content.addView(stepLabel(R.string.gamepad_compose_builder_step),
                new LayoutParams(MATCH_PARENT, dp(28)));
        TextView builderHint = label(context.getString(R.string.gamepad_compose_builder_hint),
                10, false);
        builderHint.setTextColor(HeimdallUi.mutedTextColor(context));
        builderHint.setSingleLine(true);
        content.addView(builderHint, new LayoutParams(MATCH_PARENT, dp(26)));

        int supportedCount = 0;
        for (GamepadSequenceComposer.ButtonSpec spec
                : GamepadSequenceComposer.DIGITAL_BUTTONS) {
            if (device.supportsDigitalKey(spec.code)) {
                supportedCount++;
            }
        }

        LinearLayout selectorRow = row();
        selectorRow.setBaselineAligned(false);
        content.addView(selectorRow, new LayoutParams(MATCH_PARENT, dp(140)));

        LinearLayout directionPanel = column();
        directionPanel.setPadding(0, 0, dp(8), 0);
        selectorRow.addView(directionPanel, new LayoutParams(0, MATCH_PARENT, 35));
        directionPanel.addView(sectionLabel(R.string.gamepad_compose_direction),
                new LayoutParams(MATCH_PARENT, dp(24)));
        for (int row = 0; row < 3; row++) {
            LinearLayout directionRow = row();
            directionPanel.addView(directionRow, new LayoutParams(MATCH_PARENT, dp(38)));
            for (int column = 0; column < 3; column++) {
                int index = row * 3 + column;
                int x = DIRECTIONS[index][0];
                int y = DIRECTIONS[index][1];
                String glyph = x == 0 && y == 0 ? "\u2014"
                        : GamepadSequenceComposer.directionLabel(x, y);
                Button button = choiceButton(glyph, () -> selectDirection(index));
                button.setContentDescription(x == 0 && y == 0
                        ? context.getString(R.string.gamepad_compose_no_direction) : glyph);
                directionButtons[index] = button;
                directionRow.addView(button);
            }
        }

        LinearLayout buttonPanel = column();
        selectorRow.addView(buttonPanel, new LayoutParams(0, MATCH_PARENT, 65));
        TextView buttonHeading = sectionLabel(context.getString(
                R.string.gamepad_compose_buttons_available, supportedCount,
                GamepadSequenceComposer.DIGITAL_BUTTONS.length));
        buttonPanel.addView(buttonHeading, new LayoutParams(MATCH_PARENT, dp(24)));
        for (int rowIndex = 0; rowIndex < 3; rowIndex++) {
            LinearLayout buttonRow = row();
            buttonPanel.addView(buttonRow, new LayoutParams(MATCH_PARENT, dp(38)));
            for (int column = 0; column < 4; column++) {
                int itemIndex = rowIndex * 4 + column;
                GamepadSequenceComposer.ButtonSpec spec =
                        GamepadSequenceComposer.DIGITAL_BUTTONS[itemIndex];
                boolean supported = device.supportsDigitalKey(spec.code);
                Button button = choiceButton(spec.label,
                        () -> toggleButton(spec.code, buttonAt(spec.code)));
                button.setTag(spec.code);
                button.setTextSize(spec.label.length() > 4 ? 8 : 10);
                if (supported) {
                    supportedActionButtons.add(button);
                } else {
                    button.setEnabled(false);
                    button.setAlpha(0.38f);
                    button.setContentDescription(context.getString(
                            R.string.gamepad_compose_button_unavailable, spec.label));
                    unavailableActionButtons.add(button);
                }
                buttonRow.addView(button);
            }
        }

        LinearLayout holdRow = row();
        holdRow.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams holdRowParams = new LayoutParams(MATCH_PARENT, dp(54));
        holdRowParams.setMargins(0, dp(5), 0, 0);
        content.addView(holdRow, holdRowParams);
        currentHoldToggle = secondaryButton(context.getString(
                R.string.gamepad_compose_long_hold), this::toggleCurrentHold);
        currentHoldToggle.setTextSize(10);
        holdRow.addView(currentHoldToggle, new LayoutParams(dp(132), MATCH_PARENT));
        currentHoldInput = timingField(holdRow,
                R.string.gamepad_compose_long_hold_duration,
                Integer.toString(GamepadSequenceComposer.DEFAULT_LONG_HOLD_MS));
        TextView holdHint = label(context.getString(
                R.string.gamepad_compose_long_hold_hint), 9, false);
        holdHint.setTextColor(HeimdallUi.mutedTextColor(context));
        holdHint.setPadding(dp(8), 0, dp(2), 0);
        holdRow.addView(holdHint, new LayoutParams(0, MATCH_PARENT, 2));
        applyCurrentHoldState();

        LinearLayout currentRow = row();
        currentRow.setGravity(Gravity.CENTER_VERTICAL);
        currentRow.setPadding(dp(8), dp(3), dp(4), dp(3));
        currentRow.setBackground(HeimdallUi.isPearl(context)
                ? HeimdallUi.cncShallowInset(context, 9)
                : HeimdallUi.insetPanel(context, 9));
        LayoutParams currentParams = new LayoutParams(MATCH_PARENT, dp(48));
        currentParams.setMargins(0, dp(6), 0, 0);
        content.addView(currentRow, currentParams);

        currentAction = label("", 11, true);
        currentAction.setTextColor(HeimdallUi.textColor(context));
        currentAction.setSingleLine(true);
        currentRow.addView(currentAction, new LayoutParams(0, MATCH_PARENT, 1));

        Button clearCurrent = secondaryButton(context.getString(
                R.string.gamepad_compose_clear_current), this::clearCurrentAction);
        clearCurrent.setTextSize(10);
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(dp(82), MATCH_PARENT);
        clearParams.setMargins(dp(4), 0, dp(4), 0);
        currentRow.addView(clearCurrent, clearParams);

        Button addAction = secondaryButton(context.getString(
                R.string.gamepad_compose_add_action), this::addCurrentAction);
        HeimdallUi.applyPrimaryActionButton(context, addAction);
        addAction.setTextSize(10);
        currentRow.addView(addAction, new LinearLayout.LayoutParams(dp(96), MATCH_PARENT));

        TextView modeWarning = label(context.getString(R.string.gamepad_compose_mode_warning),
                9, false);
        modeWarning.setTextColor(HeimdallUi.mutedTextColor(context));
        modeWarning.setLineSpacing(0f, 1.04f);
        LayoutParams warningParams = new LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        warningParams.setMargins(dp(2), dp(6), dp(2), dp(6));
        content.addView(modeWarning, warningParams);

        LinearLayout sequenceHeader = row();
        sequenceHeader.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(sequenceHeader, new LayoutParams(MATCH_PARENT, dp(38)));
        sequenceTitle = stepLabel(R.string.gamepad_compose_sequence);
        sequenceHeader.addView(sequenceTitle, new LayoutParams(0, MATCH_PARENT, 1));
        Button mirror = secondaryButton(context.getString(
                R.string.gamepad_compose_mirror_horizontal), this::mirrorSequence);
        mirror.setTextSize(10);
        sequenceHeader.addView(mirror, new LayoutParams(dp(112), dp(34)));

        sequenceList = column();
        sequenceList.setPadding(dp(8), dp(3), dp(8), dp(3));
        sequenceList.setBackground(HeimdallUi.isPearl(context)
                ? HeimdallUi.cncShallowInset(context, 9)
                : HeimdallUi.insetPanel(context, 9));
        content.addView(sequenceList, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        content.addView(stepLabel(R.string.gamepad_compose_timing_step),
                new LayoutParams(MATCH_PARENT, dp(32)));
        LinearLayout timingRow = row();
        content.addView(timingRow, new LayoutParams(MATCH_PARENT, dp(58)));
        intervalInput = timingField(timingRow, R.string.gamepad_compose_frame_interval,
                Integer.toString(restored == null
                        ? GamepadSequenceComposer.DEFAULT_FRAME_INTERVAL_MS
                        : restored.frameIntervalMs));
        holdInput = timingField(timingRow, R.string.gamepad_compose_final_hold,
                Integer.toString(restored == null
                        ? GamepadSequenceComposer.DEFAULT_FINAL_HOLD_MS
                        : restored.finalHoldMs));

        status = label(context.getString(R.string.gamepad_compose_timing_hint), 10, false);
        status.setTextColor(HeimdallUi.mutedTextColor(context));
        status.setLineSpacing(0f, 1.06f);
        status.setPadding(dp(4), dp(6), dp(4), dp(4));
        content.addView(status, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        selectDirection(4);
        if (restored != null) {
            frames.addAll(restored.frames);
            status.setText(getResources().getQuantityString(
                    R.plurals.gamepad_compose_existing_loaded,
                    frames.size(), frames.size()));
        }
        renderCurrentAction();
        renderSequence();
    }

    void bindTextInputs(TextInputBinder binder) {
        if (binder == null) {
            return;
        }
        binder.bind(currentHoldInput);
        binder.bind(intervalInput);
        binder.bind(holdInput);
    }

    boolean hasFrames() {
        return !frames.isEmpty();
    }

    String buildSequence() {
        return GamepadSequenceComposer.build(frames, device.dpadEncoding(),
                timingValue(intervalInput, 40), timingValue(holdInput, 60));
    }

    boolean exceedsReplayDurationLimit() {
        return GamepadSequenceComposer.durationMs(frames,
                timingValue(intervalInput, GamepadSequenceComposer.DEFAULT_FRAME_INTERVAL_MS),
                timingValue(holdInput, GamepadSequenceComposer.DEFAULT_FINAL_HOLD_MS))
                > GamepadSequencePolicy.MAX_REPLAY_DURATION_MS;
    }

    void showStatus(int stringRes) {
        status.setText(stringRes);
    }

    void showStatus(String message) {
        status.setText(message);
    }

    void setControlsEnabled(boolean enabled) {
        setChildrenEnabled(this, enabled);
        status.setEnabled(true);
        for (Button button : unavailableActionButtons) {
            button.setEnabled(false);
            button.setAlpha(0.38f);
        }
        if (enabled) {
            applyCurrentHoldState();
        }
    }

    private void addCurrentAction() {
        GamepadSequenceComposer.Frame frame = new GamepadSequenceComposer.Frame(
                directionX, directionY, new ArrayList<>(selectedButtons),
                currentLongHold ? holdValue(currentHoldInput,
                        GamepadSequenceComposer.DEFAULT_LONG_HOLD_MS) : 0);
        if (frame.isEmpty()) {
            showStatus(R.string.gamepad_compose_action_empty);
            return;
        }
        if (frames.size() >= GamepadSequenceComposer.MAX_FRAMES) {
            showStatus(getResources().getQuantityString(R.plurals.gamepad_compose_action_limit,
                    GamepadSequenceComposer.MAX_FRAMES, GamepadSequenceComposer.MAX_FRAMES));
            return;
        }
        frames.add(frame);
        int addedIndex = frames.size();
        clearCurrentAction();
        showStatus(getResources().getString(R.string.gamepad_compose_action_added, addedIndex));
        renderSequence();
    }

    private void clearCurrentAction() {
        selectedButtons.clear();
        for (Button button : supportedActionButtons) {
            HeimdallUi.applyChoiceButton(getContext(), button, false);
        }
        currentLongHold = false;
        applyCurrentHoldState();
        selectDirection(4);
        renderCurrentAction();
    }

    private void toggleCurrentHold() {
        currentLongHold = !currentLongHold;
        applyCurrentHoldState();
        renderCurrentAction();
    }

    private void applyCurrentHoldState() {
        HeimdallUi.applyChoiceButton(getContext(), currentHoldToggle, currentLongHold);
        currentHoldInput.setEnabled(currentLongHold);
        currentHoldInput.setAlpha(currentLongHold ? 1f : 0.45f);
    }

    private void mirrorSequence() {
        if (frames.isEmpty()) {
            showStatus(R.string.gamepad_compose_mirror_empty);
            return;
        }
        List<GamepadSequenceComposer.Frame> mirrored =
                GamepadSequenceComposer.mirrorHorizontally(frames);
        frames.clear();
        frames.addAll(mirrored);
        renderSequence();
        showStatus(R.string.gamepad_compose_mirror_applied);
    }

    private void selectDirection(int index) {
        directionX = DIRECTIONS[index][0];
        directionY = DIRECTIONS[index][1];
        for (int i = 0; i < directionButtons.length; i++) {
            if (directionButtons[i] != null) {
                HeimdallUi.applyChoiceButton(getContext(), directionButtons[i], i == index);
            }
        }
        renderCurrentAction();
    }

    private void toggleButton(int code, Button button) {
        if (button == null || !button.isEnabled()) {
            return;
        }
        if (selectedButtons.contains(code)) {
            selectedButtons.remove(code);
        } else {
            selectedButtons.add(code);
        }
        HeimdallUi.applyChoiceButton(getContext(), button, selectedButtons.contains(code));
        renderCurrentAction();
    }

    private void renderCurrentAction() {
        if (currentAction == null) {
            return;
        }
        GamepadSequenceComposer.Frame frame = new GamepadSequenceComposer.Frame(
                directionX, directionY, new ArrayList<>(selectedButtons),
                currentLongHold ? holdValue(currentHoldInput,
                        GamepadSequenceComposer.DEFAULT_LONG_HOLD_MS) : 0);
        String value = frame.isEmpty()
                ? getContext().getString(R.string.gamepad_compose_current_action_empty)
                : displayLabel(frame);
        currentAction.setText(getContext().getString(
                R.string.gamepad_compose_current_action, value));
    }

    private Button buttonAt(int code) {
        for (Button button : supportedActionButtons) {
            Object tag = button.getTag();
            if (tag instanceof Integer && (Integer) tag == code) {
                return button;
            }
        }
        return null;
    }

    private void renderSequence() {
        if (sequenceList == null || sequenceTitle == null) {
            return;
        }
        sequenceTitle.setText(getContext().getString(R.string.gamepad_compose_sequence_count,
                frames.size(), GamepadSequenceComposer.MAX_FRAMES));
        sequenceList.removeAllViews();
        if (frames.isEmpty()) {
            TextView empty = label(getContext().getString(
                    R.string.gamepad_compose_sequence_empty), 10, false);
            empty.setTextColor(HeimdallUi.mutedTextColor(getContext()));
            empty.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            empty.setPadding(dp(4), 0, dp(4), 0);
            sequenceList.addView(empty, new LayoutParams(MATCH_PARENT, dp(48)));
            return;
        }
        for (int index = 0; index < frames.size(); index++) {
            final int frameIndex = index;
            LinearLayout item = row();
            item.setGravity(Gravity.CENTER_VERTICAL);
            TextView itemLabel = label((index + 1) + ".  "
                    + displayLabel(frames.get(index)), 11, true);
            itemLabel.setTextColor(HeimdallUi.textColor(getContext()));
            itemLabel.setSingleLine(true);
            item.addView(itemLabel, new LayoutParams(0, MATCH_PARENT, 1));
            ImageButton delete = iconButton(R.drawable.ic_trash,
                    getContext().getString(
                            R.string.gamepad_compose_remove_action, index + 1), true, () -> {
                frames.remove(frameIndex);
                showStatus(R.string.gamepad_compose_timing_hint);
                renderSequence();
            });
            item.addView(delete, new LayoutParams(dp(48), dp(48)));
            sequenceList.addView(item, new LayoutParams(MATCH_PARENT, dp(52)));
        }
    }

    private String displayLabel(GamepadSequenceComposer.Frame frame) {
        String label = frame.displayLabel();
        return frame.holdOverrideMs > 0
                ? getContext().getString(R.string.gamepad_compose_hold_suffix,
                        label, frame.holdOverrideMs)
                : label;
    }

    private EditText timingField(LinearLayout parent, int labelRes, String value) {
        LinearLayout field = column();
        LayoutParams fieldParams = new LayoutParams(0, MATCH_PARENT, 1);
        fieldParams.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(field, fieldParams);
        TextView label = label(getContext().getString(labelRes), 9, false);
        label.setTextColor(HeimdallUi.mutedTextColor(getContext()));
        field.addView(label, new LayoutParams(MATCH_PARENT, dp(20)));
        EditText input = new EditText(getContext());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(value);
        input.setTextSize(12);
        input.setGravity(Gravity.CENTER);
        input.setTextColor(HeimdallUi.textColor(getContext()));
        input.setBackground(HeimdallUi.fieldPanel(getContext(), 7));
        field.addView(input, new LayoutParams(MATCH_PARENT, dp(36)));
        return input;
    }

    private int timingValue(EditText input, int fallback) {
        try {
            return GamepadSequenceComposer.clampTiming(
                    Integer.parseInt(input.getText().toString().trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int holdValue(EditText input, int fallback) {
        try {
            return GamepadSequenceComposer.clampHold(
                    Integer.parseInt(input.getText().toString().trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Button choiceButton(String text, Runnable action) {
        Button button = HeimdallUi.baseButton(getContext(), text, action);
        button.setTextSize(11);
        button.setPadding(dp(2), 0, dp(2), 0);
        HeimdallUi.applyChoiceButton(getContext(), button, false);
        LayoutParams params = new LayoutParams(0, MATCH_PARENT, 1);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        button.setLayoutParams(params);
        return button;
    }

    private Button secondaryButton(String text, Runnable action) {
        Button button = HeimdallUi.baseButton(getContext(), text, action);
        button.setAllCaps(false);
        HeimdallUi.applySecondaryButton(getContext(), button);
        return button;
    }

    private ImageButton iconButton(int iconRes, String description,
            boolean danger, Runnable action) {
        ImageButton button = new ImageButton(getContext());
        button.setImageResource(iconRes);
        button.setColorFilter(danger
                ? (HeimdallUi.isPearl(getContext())
                        ? 0xFFB34A4F : HeimdallUi.COLOR_DANGER)
                : HeimdallUi.textColor(getContext()));
        button.setContentDescription(description);
        button.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        button.setMinimumWidth(dp(48));
        button.setMinimumHeight(dp(48));
        button.setPadding(dp(10), dp(8), dp(10), dp(8));
        button.setBackground(HeimdallUi.isPearl(getContext())
                ? HeimdallUi.pearlMenuControl(getContext(), 8, false, false)
                : HeimdallUi.surfacePanel(getContext(), 8));
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private TextView stepLabel(int stringRes) {
        return stepLabel(getContext().getString(stringRes));
    }

    private TextView stepLabel(String text) {
        TextView view = label(text, 12, true);
        view.setTextColor(HeimdallUi.textColor(getContext()));
        view.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        return view;
    }

    private TextView sectionLabel(int stringRes) {
        return sectionLabel(getContext().getString(stringRes));
    }

    private TextView sectionLabel(String text) {
        TextView view = label(text, 10, true);
        view.setTextColor(HeimdallUi.textColor(getContext()));
        view.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        return view;
    }

    private TextView label(String text, float sizeSp, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        return view;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(HORIZONTAL);
        return layout;
    }

    private void setChildrenEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                setChildrenEnabled(group.getChildAt(index), enabled);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
