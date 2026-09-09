package com.mastercook777.heimdall;

import android.content.Context;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared checklist content used by first launch and the persistent Settings entry. */
final class FirstSetupChecklistView extends LinearLayout {
    interface Listener {
        void onCreateProfile();

        void onImportProfile();

        void onOpenBasicTouch();

        void onOpenAdvancedControls();
    }

    private final TextView profileStatus;
    private final TextView basicTouchStatus;
    private final TextView advancedControlsStatus;
    private final Button basicTouchAction;
    private final Button advancedControlsAction;

    FirstSetupChecklistView(Context context, Listener listener) {
        super(context);
        setOrientation(VERTICAL);

        LinearLayout profileCard = checklistCard();
        profileStatus = addHeader(profileCard, R.string.first_setup_profile_title);
        addDescription(profileCard, R.string.first_setup_profile_summary);
        LinearLayout profileActions = actionRow(profileCard);
        profileActions.addView(actionButton(R.string.first_setup_create_profile,
                listener::onCreateProfile), weightedButtonParams(true));
        profileActions.addView(actionButton(R.string.first_setup_import_profile,
                listener::onImportProfile), weightedButtonParams(false));
        addView(profileCard, cardParams());

        LinearLayout basicCard = checklistCard();
        basicTouchStatus = addHeader(basicCard, R.string.first_setup_basic_touch_title);
        addDescription(basicCard, R.string.first_setup_basic_touch_summary);
        basicTouchAction = actionButton(R.string.first_setup_enable_open,
                listener::onOpenBasicTouch);
        basicCard.addView(basicTouchAction, fullButtonParams());
        addView(basicCard, cardParams());

        LinearLayout advancedCard = checklistCard();
        advancedControlsStatus = addHeader(advancedCard,
                R.string.first_setup_advanced_controls_title);
        addDescription(advancedCard, R.string.first_setup_advanced_controls_summary);
        advancedControlsAction = actionButton(R.string.first_setup_set_up_shizuku,
                listener::onOpenAdvancedControls);
        advancedCard.addView(advancedControlsAction, fullButtonParams());
        addView(advancedCard, cardParams());
    }

    void refresh(boolean profileCreated, boolean basicTouchReady,
                 boolean advancedControlsReady) {
        renderStatus(profileStatus, profileCreated,
                R.string.first_setup_status_profile_created,
                R.string.first_setup_status_profile_pending);
        renderStatus(basicTouchStatus, basicTouchReady,
                R.string.first_setup_status_ready,
                R.string.first_setup_status_not_enabled);
        renderStatus(advancedControlsStatus, advancedControlsReady,
                R.string.first_setup_status_ready,
                R.string.first_setup_status_not_configured);
        basicTouchAction.setText(basicTouchReady
                ? R.string.first_setup_manage : R.string.first_setup_enable_open);
        advancedControlsAction.setText(advancedControlsReady
                ? R.string.first_setup_manage : R.string.first_setup_set_up_shizuku);
    }

    private LinearLayout checklistCard() {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(VERTICAL);
        card.setPadding(dp(14), dp(8), dp(14), dp(8));
        card.setBackground(HeimdallUi.fieldPanel(getContext(), HeimdallUi.RADIUS_CARD));
        return card;
    }

    private TextView addHeader(LinearLayout card, int titleRes) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(24));
        card.addView(row, new LayoutParams(-1, -2));

        TextView title = label(getContext().getString(titleRes), 13,
                HeimdallUi.textColor(getContext()), true);
        title.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        row.addView(title, new LayoutParams(0, -2, 1));

        TextView status = label("", 11, HeimdallUi.mutedTextColor(getContext()), true);
        status.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        LayoutParams statusParams = new LayoutParams(-2, -2);
        statusParams.setMargins(dp(10), 0, 0, 0);
        row.addView(status, statusParams);
        return status;
    }

    private void addDescription(LinearLayout card, int summaryRes) {
        TextView summary = label(getContext().getString(summaryRes), 11,
                HeimdallUi.mutedTextColor(getContext()), false);
        summary.setGravity(Gravity.TOP | Gravity.START);
        LayoutParams params = new LayoutParams(-1, -2);
        params.setMargins(0, dp(2), 0, dp(7));
        card.addView(summary, params);
    }

    private LinearLayout actionRow(LinearLayout card) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        card.addView(row, new LayoutParams(-1, dp(44)));
        return row;
    }

    private Button actionButton(int textRes, Runnable action) {
        Button button = new Button(getContext());
        button.setAllCaps(false);
        button.setText(textRes);
        button.setTextSize(11);
        button.setTextColor(HeimdallUi.textColor(getContext()));
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setOnClickListener(view -> action.run());
        HeimdallUi.applySecondaryButton(getContext(), button);
        return button;
    }

    private LayoutParams weightedButtonParams(boolean first) {
        LayoutParams params = new LayoutParams(0, -1, 1);
        params.setMargins(first ? 0 : dp(4), 0, first ? dp(4) : 0, 0);
        return params;
    }

    private LayoutParams fullButtonParams() {
        return new LayoutParams(-1, dp(44));
    }

    private LayoutParams cardParams() {
        LayoutParams params = new LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(6));
        return params;
    }

    private void renderStatus(TextView view, boolean ready, int readyRes, int pendingRes) {
        view.setText(ready ? readyRes : pendingRes);
        view.setTextColor(ready ? successColor() : HeimdallUi.mutedTextColor(getContext()));
    }

    private int successColor() {
        return HeimdallUi.isPearl(getContext()) ? 0xFF2F8B59 : 0xFF5FD18A;
    }

    private TextView label(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(HeimdallUi.typeface(bold));
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
