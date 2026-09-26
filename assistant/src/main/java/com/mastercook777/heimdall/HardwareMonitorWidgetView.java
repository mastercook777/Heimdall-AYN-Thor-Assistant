package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

@SuppressLint("ViewConstructor")
final class HardwareMonitorWidgetView extends FrameLayout {
    private static final String UNAVAILABLE_VALUE = "\u2014";

    private final ImageView cpuIcon;
    private final TextView cpuValue;
    private final ImageView ramIcon;
    private final TextView ramValue;
    private final HardwareMonitorProgressView ramProgress;
    private final HardwareMonitorSampler sampler;
    private String lastCpuValue = "";
    private String lastRamValue = "";
    private int lastRamPercent = -1;
    private boolean lastRamWarning;
    private int lastCpuColor;
    private int lastRamColor;
    private String lastContentDescription = "";

    HardwareMonitorWidgetView(Context context) {
        super(context);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        FrameLayout displayFrame = new FrameLayout(context);
        displayFrame.setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncInputFrame(context, HeimdallUi.RADIUS_MODULE, false)
                : HeimdallUi.glassSurface(context, ThemeGlassColors.MEDIA_FRAME,
                        HeimdallUi.RADIUS_MODULE, HeimdallUi.STROKE_HAIRLINE));
        addView(displayFrame, new LayoutParams(-1, -1));

        FrameLayout viewport = new FrameLayout(context);
        viewport.setBackground(HeimdallUi.hardwareMonitorContentPanel(
                context, Math.round(HeimdallUi.mediaFrameInnerRadiusDp(
                        context, HeimdallUi.RADIUS_MODULE))));
        viewport.setClipToOutline(true);
        viewport.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float radius = HeimdallUi.mediaFrameInnerRadiusDp(
                        getContext(), HeimdallUi.RADIUS_MODULE);
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(radius));
            }
        });
        int frameInset = dp(HeimdallUi.mediaFrameContentInsetDp(context));
        LayoutParams viewportParams = new LayoutParams(-1, -1);
        viewportParams.setMargins(frameInset, frameInset, frameInset, frameInset);
        displayFrame.addView(viewport, viewportParams);

        LinearLayout dashboard = new LinearLayout(context);
        dashboard.setOrientation(LinearLayout.HORIZONTAL);
        dashboard.setGravity(Gravity.CENTER_VERTICAL);
        dashboard.setPadding(dp(12), dp(8), dp(12), dp(8));
        dashboard.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        viewport.addView(dashboard, new FrameLayout.LayoutParams(-1, -1));

        MetricViews cpu = createMetric(context, R.drawable.ic_hardware_temperature,
                R.string.hardware_monitor_cpu, false);
        cpuIcon = cpu.icon;
        cpuValue = cpu.value;
        dashboard.addView(cpu.root, new LinearLayout.LayoutParams(0, -1, 1f));

        View divider = new View(context);
        divider.setBackgroundColor(
                HeimdallUi.componentColors(context).structuralDividerSubtle);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(dp(1), -1);
        dividerParams.setMargins(dp(10), dp(3), dp(10), dp(3));
        dashboard.addView(divider, dividerParams);

        MetricViews ram = createMetric(context, R.drawable.ic_hardware_memory,
                R.string.hardware_monitor_ram, true);
        ramIcon = ram.icon;
        ramValue = ram.value;
        ramProgress = ram.progress;
        dashboard.addView(ram.root, new LinearLayout.LayoutParams(0, -1, 1.15f));

        sampler = new HardwareMonitorSampler(context, this::renderSnapshot);
        renderSnapshot(new HardwareMonitorSampler.Snapshot(null, -1, -1L, -1L, false));
    }

    void resume() {
        sampler.start();
    }

    void pause() {
        sampler.stop();
    }

    void release() {
        sampler.release();
    }

    private MetricViews createMetric(Context context, int iconResource, int labelResource,
            boolean includeProgress) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(22)));

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconResource);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setImageTintList(ColorStateList.valueOf(withAlpha(
                HeimdallUi.accent(context), 0.82f)));
        header.addView(icon, new LinearLayout.LayoutParams(dp(17), dp(17)));

        TextView label = new TextView(context);
        label.setText(labelResource);
        label.setTextSize(HeimdallUi.TYPE_META);
        label.setTextColor(HeimdallUi.mutedTextColor(context));
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setPadding(dp(5), 0, 0, 0);
        label.setSingleLine(true);
        header.addView(label, new LinearLayout.LayoutParams(0, -1, 1f));

        TextView value = new TextView(context);
        value.setTextSize(includeProgress ? 17 : 22);
        value.setTextColor(HeimdallUi.textColor(context));
        value.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        value.setSingleLine(true);
        value.setEllipsize(TextUtils.TruncateAt.END);
        root.addView(value, new LinearLayout.LayoutParams(-1, 0, 1f));

        HardwareMonitorProgressView progress = null;
        if (includeProgress) {
            progress = new HardwareMonitorProgressView(context);
            LinearLayout.LayoutParams progressParams =
                    new LinearLayout.LayoutParams(-1, dp(5));
            progressParams.setMargins(0, dp(2), 0, dp(2));
            root.addView(progress, progressParams);
        }
        return new MetricViews(root, icon, value, progress);
    }

    private void renderSnapshot(HardwareMonitorSampler.Snapshot snapshot) {
        String cpuText = snapshot.cpuCelsius == null
                ? UNAVAILABLE_VALUE
                : Math.round(snapshot.cpuCelsius) + " \u00b0C";
        String ramText = snapshot.hasRam()
                ? formatRamPair(snapshot.ramUsedBytes, snapshot.ramTotalBytes)
                : UNAVAILABLE_VALUE;
        int ramPercent = snapshot.ramUsedPercent();
        boolean cpuWarning = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && snapshot.thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE;
        boolean cpuCritical = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && snapshot.thermalStatus >= PowerManager.THERMAL_STATUS_CRITICAL;
        int cpuColor = snapshot.cpuCelsius == null
                ? HeimdallUi.mutedTextColor(getContext())
                : cpuCritical
                        ? HeimdallUi.semanticColor(getContext(), HeimdallUi.SEMANTIC_ERROR)
                        : cpuWarning
                                ? HeimdallUi.semanticColor(
                                        getContext(), HeimdallUi.SEMANTIC_WARNING)
                                : HeimdallUi.textColor(getContext());
        int ramColor = snapshot.hasRam()
                ? HeimdallUi.textColor(getContext())
                : HeimdallUi.mutedTextColor(getContext());

        if (!cpuText.equals(lastCpuValue)) {
            lastCpuValue = cpuText;
            cpuValue.setText(cpuText);
        }
        if (!ramText.equals(lastRamValue)) {
            lastRamValue = ramText;
            ramValue.setText(ramText);
        }
        if (cpuColor != lastCpuColor) {
            lastCpuColor = cpuColor;
            cpuValue.setTextColor(cpuColor);
            cpuIcon.setImageTintList(ColorStateList.valueOf(withAlpha(
                    cpuWarning ? cpuColor : HeimdallUi.accent(getContext()),
                    snapshot.cpuCelsius == null ? 0.62f : 0.82f)));
        }
        if (ramColor != lastRamColor) {
            lastRamColor = ramColor;
            ramValue.setTextColor(ramColor);
            ramIcon.setImageTintList(ColorStateList.valueOf(withAlpha(
                    snapshot.hasRam() ? HeimdallUi.accent(getContext()) : ramColor,
                    snapshot.hasRam() ? 0.82f : 0.66f)));
        }
        if (ramPercent != lastRamPercent || snapshot.lowMemory != lastRamWarning) {
            lastRamPercent = ramPercent;
            lastRamWarning = snapshot.lowMemory;
            ramProgress.setProgress(ramPercent, snapshot.lowMemory);
        }
        String unavailable = getContext().getString(R.string.hardware_monitor_unavailable);
        String contentDescription = getContext().getString(
                R.string.hardware_monitor_content_description,
                snapshot.cpuCelsius == null ? unavailable : cpuText,
                snapshot.hasRam() ? ramText : unavailable);
        if (!contentDescription.equals(lastContentDescription)) {
            lastContentDescription = contentDescription;
            setContentDescription(contentDescription);
        }
    }

    private static String formatRamPair(long usedBytes, long totalBytes) {
        return formatRamNumber(usedBytes) + " / " + formatRamNumber(totalBytes) + " GB";
    }

    private static String formatRamNumber(long bytes) {
        double gigabytes = bytes / 1_000_000_000d;
        double rounded = Math.rint(gigabytes);
        if (Math.abs(gigabytes - rounded) < 0.05d) {
            return String.format(Locale.getDefault(), "%.0f", rounded);
        }
        return String.format(Locale.getDefault(), "%.1f", gigabytes);
    }

    private static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(Color.alpha(color) * alpha),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class MetricViews {
        final LinearLayout root;
        final ImageView icon;
        final TextView value;
        final HardwareMonitorProgressView progress;

        MetricViews(LinearLayout root, ImageView icon, TextView value,
                HardwareMonitorProgressView progress) {
            this.root = root;
            this.icon = icon;
            this.value = value;
            this.progress = progress;
        }
    }
}
