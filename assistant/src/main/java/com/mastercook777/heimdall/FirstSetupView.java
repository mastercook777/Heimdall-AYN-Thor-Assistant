package com.mastercook777.heimdall;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Lightweight first-run Welcome and Setup Checklist presentation. */
final class FirstSetupView extends FrameLayout {
    interface Listener extends FirstSetupChecklistView.Listener {
        void onGetStarted();

        void onFinishSetup();
    }

    private final Listener listener;
    private FirstSetupChecklistView checklist;

    FirstSetupView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setClickable(true);
        setFocusable(true);
        setBackgroundColor(HeimdallUi.background(context));
    }

    void showWelcome() {
        checklist = null;
        removeAllViews();

        LinearLayout card = baseCard();
        card.setPadding(dp(30), dp(22), dp(30), dp(22));

        ImageView mark = new ImageView(getContext());
        mark.setImageResource(HeimdallUi.isPearl(getContext())
                ? R.drawable.ic_heimdall_header_mark_freya
                : R.drawable.ic_heimdall_header_mark_blue);
        mark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        card.addView(mark, new LinearLayout.LayoutParams(dp(58), dp(58)));

        TextView eyebrow = label(getContext().getString(R.string.first_setup_brand), 11,
                HeimdallUi.accent(getContext()), true);
        eyebrow.setLetterSpacing(0.16f);
        card.addView(eyebrow, new LinearLayout.LayoutParams(-1, dp(24)));

        TextView title = label(getContext().getString(R.string.first_setup_welcome_title), 24,
                HeimdallUi.textColor(getContext()), true);
        card.addView(title, new LinearLayout.LayoutParams(-1, dp(38)));

        TextView summary = label(getContext().getString(R.string.first_setup_welcome_summary), 13,
                HeimdallUi.mutedTextColor(getContext()), false);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.setMargins(0, 0, 0, dp(12));
        card.addView(summary, summaryParams);

        LinearLayout capabilityPanel = new LinearLayout(getContext());
        capabilityPanel.setOrientation(LinearLayout.VERTICAL);
        capabilityPanel.setPadding(dp(14), dp(6), dp(14), dp(6));
        capabilityPanel.setBackground(HeimdallUi.insetPanel(getContext(),
                HeimdallUi.RADIUS_CARD));
        capabilityPanel.addView(capabilityRow(R.drawable.ic_touchpad,
                R.string.first_setup_controls_title, R.string.first_setup_controls_summary));
        capabilityPanel.addView(capabilityRow(R.drawable.ic_map,
                R.string.first_setup_reference_title, R.string.first_setup_reference_summary));
        capabilityPanel.addView(capabilityRow(R.drawable.ic_profile,
                R.string.first_setup_profiles_title, R.string.first_setup_profiles_summary));
        card.addView(capabilityPanel, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = actionRow();
        actions.addView(secondaryButton(R.string.first_setup_skip_later,
                listener::onFinishSetup), weightedButtonParams(true));
        actions.addView(primaryButton(R.string.first_setup_get_started,
                listener::onGetStarted), weightedButtonParams(false));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(52));
        actionParams.setMargins(0, dp(14), 0, 0);
        card.addView(actions, actionParams);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        cardParams.setMargins(dp(64), dp(30), dp(64), dp(30));
        addView(card, cardParams);
    }

    void showChecklist() {
        removeAllViews();

        LinearLayout card = baseCard();
        card.setPadding(dp(24), dp(18), dp(24), dp(18));

        TextView eyebrow = label(getContext().getString(R.string.first_setup_brand), 11,
                HeimdallUi.accent(getContext()), true);
        eyebrow.setLetterSpacing(0.16f);
        card.addView(eyebrow, new LinearLayout.LayoutParams(-1, dp(22)));

        TextView title = label(getContext().getString(R.string.first_setup_checklist_title), 22,
                HeimdallUi.textColor(getContext()), true);
        card.addView(title, new LinearLayout.LayoutParams(-1, dp(34)));

        TextView summary = label(getContext().getString(R.string.first_setup_checklist_summary), 12,
                HeimdallUi.mutedTextColor(getContext()), false);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.setMargins(0, 0, 0, dp(8));
        card.addView(summary, summaryParams);

        ScrollView scroll = new ScrollView(getContext());
        scroll.setFillViewport(false);
        checklist = new FirstSetupChecklistView(getContext(), listener);
        scroll.addView(checklist, new ScrollView.LayoutParams(-1, -2));
        card.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout actions = actionRow();
        actions.addView(secondaryButton(R.string.first_setup_later,
                listener::onFinishSetup), weightedButtonParams(true));
        actions.addView(primaryButton(R.string.first_setup_continue,
                listener::onFinishSetup), weightedButtonParams(false));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(52));
        actionParams.setMargins(0, dp(6), 0, 0);
        card.addView(actions, actionParams);

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        cardParams.setMargins(dp(40), dp(12), dp(40), dp(12));
        addView(card, cardParams);
    }

    void refresh(boolean profileCreated, boolean basicTouchReady,
                 boolean advancedControlsReady) {
        if (checklist != null) {
            checklist.refresh(profileCreated, basicTouchReady, advancedControlsReady);
        }
    }

    private LinearLayout baseCard() {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(HeimdallUi.surfacePanel(getContext(), HeimdallUi.RADIUS_PANEL));
        return card;
    }

    private LinearLayout capabilityRow(int iconRes, int titleRes, int summaryRes) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(getContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(HeimdallUi.accent(getContext()));
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(icon, new LinearLayout.LayoutParams(dp(26), dp(26)));

        LinearLayout copy = new LinearLayout(getContext());
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -1, 1);
        copyParams.setMargins(dp(12), 0, 0, 0);
        row.addView(copy, copyParams);
        copy.addView(label(getContext().getString(titleRes), 13,
                HeimdallUi.textColor(getContext()), true),
                new LinearLayout.LayoutParams(-1, dp(23)));
        copy.addView(label(getContext().getString(summaryRes), 11,
                HeimdallUi.mutedTextColor(getContext()), false),
                new LinearLayout.LayoutParams(-1, -2));
        row.setPadding(0, dp(4), 0, dp(4));
        row.setMinimumHeight(dp(58));
        return row;
    }

    private LinearLayout actionRow() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private Button primaryButton(int textRes, Runnable action) {
        Button button = button(textRes, action);
        HeimdallUi.applyPrimaryActionButton(getContext(), button);
        return button;
    }

    private Button secondaryButton(int textRes, Runnable action) {
        Button button = button(textRes, action);
        HeimdallUi.applySecondaryButton(getContext(), button);
        return button;
    }

    private Button button(int textRes, Runnable action) {
        Button button = new Button(getContext());
        button.setAllCaps(false);
        button.setText(textRes);
        button.setTextSize(12);
        button.setTextColor(HeimdallUi.textColor(getContext()));
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private LinearLayout.LayoutParams weightedButtonParams(boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(first ? 0 : dp(5), 0, first ? dp(5) : 0, 0);
        return params;
    }

    private TextView label(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        view.setTypeface(HeimdallUi.typeface(bold));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
