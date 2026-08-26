package com.mastercook777.heimdall;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class SystemStatusController {
    private final Activity activity;
    private TextView timeText;
    private ImageView networkIcon;
    private BatteryStatusView batteryIcon;
    private TextView batteryText;
    private String lastTime = "";
    private String lastNetworkLabel = "";
    private String lastBatteryLabel = "";
    private boolean receiverRegistered;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };

    SystemStatusController(Activity activity) {
        this.activity = activity;
    }

    BatteryStatusView createBatteryView() {
        return new BatteryStatusView(activity);
    }

    void bind(TextView timeText, ImageView networkIcon,
            BatteryStatusView batteryIcon, TextView batteryText) {
        this.timeText = timeText;
        this.networkIcon = networkIcon;
        this.batteryIcon = batteryIcon;
        this.batteryText = batteryText;
    }

    void clearViews() {
        timeText = null;
        networkIcon = null;
        batteryIcon = null;
        batteryText = null;
    }

    void resetCachedLabels() {
        lastTime = "";
        lastNetworkLabel = "";
        lastBatteryLabel = "";
    }

    void start() {
        update();
        if (DebugPerformanceDiagnostics.isStaticUi() || receiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        activity.registerReceiver(statusReceiver, filter);
        receiverRegistered = true;
    }

    void stop() {
        if (!receiverRegistered) {
            return;
        }
        activity.unregisterReceiver(statusReceiver);
        receiverRegistered = false;
    }

    void update() {
        if (timeText == null || batteryText == null || networkIcon == null || batteryIcon == null) {
            return;
        }
        String time = new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
        if (!time.equals(lastTime)) {
            timeText.setText(time);
            lastTime = time;
        }
        String network = networkLabel();
        if (!network.equals(lastNetworkLabel)) {
            networkIcon.setImageResource("Wi-Fi".equals(network)
                    ? R.drawable.ic_status_wifi : R.drawable.ic_status_wifi_off);
            networkIcon.setAlpha("No net".equals(network) ? 0.55f : 1f);
            lastNetworkLabel = network;
        }
        BatterySnapshot battery = batterySnapshot();
        batteryIcon.setBattery(battery.percent, battery.charging);
        String label = battery.label();
        if (!label.equals(lastBatteryLabel)) {
            batteryText.setText(label);
            lastBatteryLabel = label;
        }
    }

    private BatterySnapshot batterySnapshot() {
        Intent intent = activity.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (intent == null) {
            return new BatterySnapshot(-1, false);
        }
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            return new BatterySnapshot(-1, false);
        }
        int percent = Math.round(level * 100f / scale);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        return new BatterySnapshot(percent, charging);
    }

    private String networkLabel() {
        ConnectivityManager manager =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return "No net";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities =
                    manager.getNetworkCapabilities(manager.getActiveNetwork());
            if (capabilities == null) {
                return "No net";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "Wi-Fi";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "Cell";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "LAN";
            }
            return "Net";
        }
        return manager.getActiveNetworkInfo() != null
                && manager.getActiveNetworkInfo().isConnected() ? "Net" : "No net";
    }

    private static final class BatterySnapshot {
        final int percent;
        final boolean charging;

        BatterySnapshot(int percent, boolean charging) {
            this.percent = percent;
            this.charging = charging;
        }

        String label() {
            if (percent < 0) {
                return "--%";
            }
            return percent + "%" + (charging ? "+" : "");
        }
    }

    static final class BatteryStatusView extends View {
        private final Paint batteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path batteryBolt = new Path();
        private int percent = -1;
        private boolean charging;

        BatteryStatusView(Context context) {
            super(context);
        }

        void setBattery(int value, boolean isCharging) {
            if (percent == value && charging == isCharging) {
                return;
            }
            percent = value;
            charging = isCharging;
            invalidate();
        }

        @Override
        public void invalidate() {
            DebugPerformanceDiagnostics.countInvalidate("Header battery");
            super.invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            DebugPerformanceDiagnostics.countDraw("Header battery");
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            boolean pearl = HeimdallUi.isPearl(getContext());
            float shellLeft = dp(2);
            float shellTop = height * 0.27f;
            float shellRight = width - dp(4);
            float shellBottom = height * 0.73f;
            float radius = dp(2);
            batteryPaint.setShader(null);
            batteryPaint.setStyle(Paint.Style.STROKE);
            batteryPaint.setStrokeWidth(dp(1));
            batteryPaint.setColor(pearl ? 0xFF596774 : 0xFFD9E8F8);
            canvas.drawRoundRect(shellLeft, shellTop, shellRight, shellBottom,
                    radius, radius, batteryPaint);
            batteryPaint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(shellRight + dp(1), height * 0.39f, width - dp(1),
                    height * 0.61f, dp(1), dp(1), batteryPaint);

            float level = percent < 0 ? 0.25f : Math.max(0.06f, Math.min(1f, percent / 100f));
            float inset = dp(2);
            float fillRight = shellLeft + inset
                    + (shellRight - shellLeft - inset * 2f) * level;
            batteryPaint.setColor(pearl ? 0xFFF08A2A : 0xFF70B7FF);
            canvas.drawRoundRect(shellLeft + inset, shellTop + inset, fillRight,
                    shellBottom - inset, dp(1), dp(1), batteryPaint);
            if (charging) {
                batteryPaint.setColor(pearl ? 0xFFFEF4E8 : 0xFFF4FAFF);
                batteryBolt.reset();
                float cx = (shellLeft + shellRight) / 2f;
                float cy = height / 2f;
                batteryBolt.moveTo(cx + dp(1), cy - dp(4));
                batteryBolt.lineTo(cx - dp(2), cy);
                batteryBolt.lineTo(cx, cy);
                batteryBolt.lineTo(cx - dp(1), cy + dp(4));
                batteryBolt.lineTo(cx + dp(3), cy - dp(1));
                batteryBolt.lineTo(cx + dp(1), cy - dp(1));
                batteryBolt.close();
                canvas.drawPath(batteryBolt, batteryPaint);
            }
        }

        private int dp(int value) {
            return Math.round(value * getResources().getDisplayMetrics().density);
        }
    }
}
