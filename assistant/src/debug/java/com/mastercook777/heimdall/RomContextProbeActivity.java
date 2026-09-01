package com.mastercook777.heimdall;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Debug-only manual surface for the passive ROM context evidence collector. */
public final class RomContextProbeActivity extends Activity {
    private static final int REQUEST_EXPORT = 7814;
    private static final String STATE_TARGET = "target";
    private static final String STATE_STARTED = "started";
    private static final String STATE_REPORT = "report";

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "rom-context-probe-client");
        thread.setDaemon(true);
        return thread;
    });

    private RomContextProbeClient client;
    private int selectedTarget = RomContextProbeCollector.TARGET_RETROARCH;
    private long sessionStartedEpochMs;
    private String report = "";
    private TextView statusView;
    private TextView reportView;
    private Button retroArchButton;
    private Button ppssppButton;
    private Button edenButton;
    private Button startButton;
    private Button captureButton;
    private Button exportButton;
    private boolean busy;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!BuildConfig.DEBUG) {
            finish();
            return;
        }
        client = new RomContextProbeClient(this);
        if (state != null) {
            selectedTarget = state.getInt(STATE_TARGET,
                    RomContextProbeCollector.TARGET_RETROARCH);
            sessionStartedEpochMs = state.getLong(STATE_STARTED, 0L);
            report = state.getString(STATE_REPORT, "");
        }
        setContentView(buildContent());
        updateTargetStyles();
        updateStateText();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt(STATE_TARGET, selectedTarget);
        state.putLong(STATE_STARTED, sessionStartedEpochMs);
        state.putString(STATE_REPORT, report);
    }

    @Override
    protected void onDestroy() {
        if (client != null) client.close();
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(14));
        root.setBackgroundColor(HeimdallUi.background(this));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(58)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        header.addView(titles, new LinearLayout.LayoutParams(0, -1, 1f));
        TextView title = label(getString(R.string.rom_context_probe_title), 20, true);
        titles.addView(title, new LinearLayout.LayoutParams(-1, 0, 1f));
        TextView subtitle = label(getString(R.string.rom_context_probe_subtitle), 11, false);
        subtitle.setTextColor(HeimdallUi.mutedTextColor(this));
        titles.addView(subtitle, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button close = button(getString(R.string.rom_context_probe_close), this::finish, false);
        header.addView(close, new LinearLayout.LayoutParams(dp(112), dp(44)));

        LinearLayout setup = new LinearLayout(this);
        setup.setOrientation(LinearLayout.VERTICAL);
        setup.setPadding(dp(12), dp(10), dp(12), dp(10));
        setup.setBackground(HeimdallUi.surfacePanel(this, 12));
        LinearLayout.LayoutParams setupParams = new LinearLayout.LayoutParams(-1, -2);
        setupParams.setMargins(0, dp(8), 0, dp(8));
        root.addView(setup, setupParams);

        TextView target = label(getString(R.string.rom_context_probe_target), 12, true);
        setup.addView(target, new LinearLayout.LayoutParams(-1, dp(28)));

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        setup.addView(choices, new LinearLayout.LayoutParams(-1, dp(46)));
        retroArchButton = choiceButton(R.string.rom_context_probe_retroarch,
                RomContextProbeCollector.TARGET_RETROARCH);
        ppssppButton = choiceButton(R.string.rom_context_probe_ppsspp,
                RomContextProbeCollector.TARGET_PPSSPP);
        edenButton = choiceButton(R.string.rom_context_probe_eden,
                RomContextProbeCollector.TARGET_EDEN);
        choices.addView(retroArchButton, weightedButtonParams());
        choices.addView(ppssppButton, weightedButtonParams());
        choices.addView(edenButton, weightedButtonParams());

        TextView instructions = label(getString(R.string.rom_context_probe_instructions), 11, false);
        instructions.setTextColor(HeimdallUi.mutedTextColor(this));
        instructions.setLineSpacing(0f, 1.12f);
        LinearLayout.LayoutParams instructionParams = new LinearLayout.LayoutParams(-1, -2);
        instructionParams.setMargins(0, dp(8), 0, dp(6));
        setup.addView(instructions, instructionParams);

        statusView = label("", 11, false);
        statusView.setPadding(dp(8), dp(6), dp(8), dp(6));
        statusView.setBackground(HeimdallUi.insetPanel(this, 8));
        setup.addView(statusView, new LinearLayout.LayoutParams(-1, dp(40)));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(48));
        actionParams.setMargins(0, dp(7), 0, 0);
        setup.addView(actions, actionParams);
        startButton = button(getString(R.string.rom_context_probe_start),
                this::startSession, false);
        captureButton = button(getString(R.string.rom_context_probe_capture),
                this::captureSnapshot, true);
        exportButton = button(getString(R.string.rom_context_probe_export),
                this::requestExport, false);
        actions.addView(startButton, weightedButtonParams());
        actions.addView(captureButton, weightedButtonParams());
        actions.addView(exportButton, weightedButtonParams());

        ScrollView reportScroll = new ScrollView(this);
        reportScroll.setFillViewport(true);
        reportScroll.setBackground(HeimdallUi.insetPanel(this, 10));
        root.addView(reportScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        reportView = label(report, 10, false);
        reportView.setTypeface(Typeface.MONOSPACE);
        reportView.setTextIsSelectable(true);
        reportView.setPadding(dp(12), dp(10), dp(12), dp(10));
        reportScroll.addView(reportView, new ScrollView.LayoutParams(-1, -2));
        return root;
    }

    private Button choiceButton(int textRes, int target) {
        Button button = button(getString(textRes), () -> {
            selectedTarget = target;
            sessionStartedEpochMs = 0L;
            report = "";
            updateTargetStyles();
            updateStateText();
        }, false);
        return button;
    }

    private void updateTargetStyles() {
        HeimdallUi.applyChoiceButton(this, retroArchButton,
                selectedTarget == RomContextProbeCollector.TARGET_RETROARCH);
        HeimdallUi.applyChoiceButton(this, ppssppButton,
                selectedTarget == RomContextProbeCollector.TARGET_PPSSPP);
        HeimdallUi.applyChoiceButton(this, edenButton,
                selectedTarget == RomContextProbeCollector.TARGET_EDEN);
    }

    private void startSession() {
        sessionStartedEpochMs = System.currentTimeMillis();
        report = "";
        updateStateText();
    }

    private void captureSnapshot() {
        if (sessionStartedEpochMs <= 0L) {
            toast(R.string.rom_context_probe_not_started);
            return;
        }
        if (!client.isAuthorized()) {
            ShizukuNativeController.requestPermission();
            toast(R.string.rom_context_probe_shizuku_required);
            return;
        }
        setBusy(true);
        statusView.setText(R.string.rom_context_probe_capturing);
        int target = selectedTarget;
        long since = sessionStartedEpochMs;
        executor.execute(() -> {
            String result = client.capture(target, since);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                setBusy(false);
                if (result.startsWith("probe-error=")) {
                    statusView.setText(result);
                    toast(R.string.rom_context_probe_capture_failed);
                } else {
                    report = result;
                    reportView.setText(result);
                    updateStateText();
                }
            });
        });
    }

    private void requestExport() {
        if (report.isEmpty()) {
            toast(R.string.rom_context_probe_export_empty);
            return;
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                .format(new Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "heimdall-rom-context-"
                + RomContextProbeCollector.targetName(selectedTarget) + "-"
                + timestamp + ".txt");
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_EXPORT);
        } catch (RuntimeException error) {
            toast(R.string.rom_context_probe_export_failed);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_EXPORT || resultCode != RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        String snapshot = report;
        executor.execute(() -> {
            boolean success = false;
            try (OutputStream output = getContentResolver().openOutputStream(uri, "w")) {
                if (output != null) {
                    output.write(snapshot.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    success = true;
                }
            } catch (Exception ignored) {
            }
            boolean exported = success;
            runOnUiThread(() -> toast(exported
                    ? R.string.rom_context_probe_export_complete
                    : R.string.rom_context_probe_export_failed));
        });
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        startButton.setEnabled(!busy);
        retroArchButton.setEnabled(!busy);
        ppssppButton.setEnabled(!busy);
        edenButton.setEnabled(!busy);
        captureButton.setEnabled(!busy);
        exportButton.setEnabled(!busy && !report.isEmpty());
        captureButton.setAlpha(busy ? 0.5f : 1f);
    }

    private void updateStateText() {
        if (statusView == null || reportView == null) return;
        statusView.setText(sessionStartedEpochMs > 0L
                ? R.string.rom_context_probe_started
                : R.string.rom_context_probe_not_started);
        reportView.setText(report);
        exportButton.setEnabled(!busy && !report.isEmpty());
        exportButton.setAlpha(report.isEmpty() ? 0.5f : 1f);
    }

    private Button button(String text, Runnable action, boolean primary) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(11);
        button.setSingleLine(true);
        button.setOnClickListener(view -> action.run());
        if (primary) HeimdallUi.applyPrimaryActionButton(this, button);
        else HeimdallUi.applySecondaryButton(this, button);
        return button;
    }

    private TextView label(String value, int sizeSp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(HeimdallUi.textColor(this));
        view.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1f);
        params.setMargins(dp(3), dp(2), dp(3), dp(2));
        return params;
    }

    private void toast(int stringRes) {
        Toast.makeText(this, stringRes, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
