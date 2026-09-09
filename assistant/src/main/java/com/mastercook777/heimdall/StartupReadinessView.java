package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** First-pass startup presentation. The readiness state machine lives in its coordinator. */
final class StartupReadinessView extends FrameLayout {
    private final TextView subtitle;
    private final StatusRow accessibilityRow;
    private final StatusRow shizukuRow;
    private final StatusRow keyboardRow;

    StartupReadinessView(Context context) {
        super(context);
        setClickable(true);
        setFocusable(true);
        setBackgroundColor(HeimdallUi.background(context));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(28), dp(24), dp(28), dp(24));
        card.setBackground(HeimdallUi.surfacePanel(context, HeimdallUi.RADIUS_PANEL));

        TextView eyebrow = label(context.getString(R.string.startup_readiness_brand),
                11, HeimdallUi.accent(context), true);
        eyebrow.setLetterSpacing(0.16f);
        card.addView(eyebrow, new LinearLayout.LayoutParams(-1, dp(24)));

        TextView title = label(context.getString(R.string.startup_readiness_title),
                24, HeimdallUi.textColor(context), true);
        card.addView(title, new LinearLayout.LayoutParams(-1, dp(40)));

        subtitle = label(context.getString(R.string.startup_readiness_checking),
                13, HeimdallUi.mutedTextColor(context), false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, dp(38));
        subtitleParams.setMargins(0, 0, 0, dp(12));
        card.addView(subtitle, subtitleParams);

        View accentRule = new View(context);
        accentRule.setBackgroundColor(HeimdallUi.accent(context));
        LinearLayout.LayoutParams ruleParams = new LinearLayout.LayoutParams(dp(52), dp(3));
        ruleParams.setMargins(0, 0, 0, dp(14));
        card.addView(accentRule, ruleParams);

        LinearLayout statusPanel = new LinearLayout(context);
        statusPanel.setOrientation(LinearLayout.VERTICAL);
        statusPanel.setPadding(dp(14), dp(6), dp(14), dp(6));
        statusPanel.setBackground(HeimdallUi.insetPanel(context, HeimdallUi.RADIUS_CARD));
        card.addView(statusPanel, new LinearLayout.LayoutParams(-1, -2));

        accessibilityRow = new StatusRow(
                context.getString(R.string.startup_readiness_accessibility));
        shizukuRow = new StatusRow(context.getString(R.string.startup_readiness_shizuku));
        keyboardRow = new StatusRow(context.getString(R.string.startup_readiness_keyboard));
        statusPanel.addView(accessibilityRow.root, new LinearLayout.LayoutParams(-1, dp(46)));
        statusPanel.addView(shizukuRow.root, new LinearLayout.LayoutParams(-1, dp(46)));
        statusPanel.addView(keyboardRow.root, new LinearLayout.LayoutParams(-1, dp(46)));

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        cardParams.setMargins(dp(72), dp(48), dp(72), dp(48));
        addView(card, cardParams);
    }

    void render(StartupReadinessCoordinator.Snapshot snapshot) {
        accessibilityRow.render(snapshot.accessibility);
        shizukuRow.render(snapshot.shizuku);
        keyboardRow.render(snapshot.keyboard);
        if (!snapshot.terminal) {
            subtitle.setText(R.string.startup_readiness_checking);
        } else if (snapshot.allReady()) {
            subtitle.setText(R.string.startup_readiness_ready);
        } else {
            subtitle.setText(R.string.startup_readiness_available);
        }
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

    private int statusColor(StartupReadinessCoordinator.Status status) {
        switch (status) {
            case READY:
                return HeimdallUi.isPearl(getContext()) ? 0xFF2F8B59 : 0xFF5FD18A;
            case ACTION_REQUIRED:
            case UNAVAILABLE:
                return HeimdallUi.isPearl(getContext()) ? 0xFFC46B20 : 0xFFD8A13A;
            case OPTIONAL:
                return HeimdallUi.mutedTextColor(getContext());
            case CHECKING:
            default:
                return HeimdallUi.accent(getContext());
        }
    }

    private int statusText(StartupReadinessCoordinator.Status status) {
        switch (status) {
            case READY:
                return R.string.startup_readiness_status_ready;
            case OPTIONAL:
                return R.string.startup_readiness_status_optional;
            case ACTION_REQUIRED:
                return R.string.startup_readiness_status_attention;
            case UNAVAILABLE:
                return R.string.startup_readiness_status_unavailable;
            case CHECKING:
            default:
                return R.string.startup_readiness_status_checking;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class StatusRow {
        final LinearLayout root;
        final View dot;
        final TextView status;

        StatusRow(String title) {
            root = new LinearLayout(getContext());
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setGravity(Gravity.CENTER_VERTICAL);

            dot = new View(getContext());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(9), dp(9));
            dotParams.setMargins(0, 0, dp(12), 0);
            root.addView(dot, dotParams);

            TextView name = label(title, 13, HeimdallUi.textColor(getContext()), true);
            root.addView(name, new LinearLayout.LayoutParams(0, -1, 1));

            status = label("", 12, HeimdallUi.mutedTextColor(getContext()), false);
            status.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
            root.addView(status, new LinearLayout.LayoutParams(dp(132), -1));
        }

        void render(StartupReadinessCoordinator.Status value) {
            int color = statusColor(value);
            GradientDrawable dotBackground = new GradientDrawable();
            dotBackground.setShape(GradientDrawable.OVAL);
            dotBackground.setColor(color);
            dot.setBackground(dotBackground);
            status.setText(statusText(value));
            status.setTextColor(color);
        }
    }
}
