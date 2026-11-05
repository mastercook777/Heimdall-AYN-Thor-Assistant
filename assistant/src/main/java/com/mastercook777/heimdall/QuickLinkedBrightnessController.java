package com.mastercook777.heimdall;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.ImageButton;
import android.widget.SeekBar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** One Quick Actions brightness row. All helper I/O stays off the UI thread. */
final class QuickLinkedBrightnessController {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "quick-linked-brightness");
        thread.setDaemon(true);
        return thread;
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private final Context context;
    private final SeekBar slider;
    private final ImageButton icon;
    private final Runnable onUnavailable;
    private boolean tracking;
    private int generation;
    private long lastPreviewMs;

    QuickLinkedBrightnessController(Context context, SeekBar slider, ImageButton icon,
            Runnable onUnavailable) {
        this.context = context.getApplicationContext();
        this.slider = slider;
        this.icon = icon;
        this.onUnavailable = onUnavailable;
        slider.setMax(100);
        slider.setProgress(50);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress,
                    boolean fromUser) {
                if (!fromUser || !tracking) return;
                slider.setContentDescription(context.getString(
                        R.string.quick_actions_linked_brightness) + " " + progress + "%");
                long now = SystemClock.elapsedRealtime();
                if (now - lastPreviewMs < 100) return;
                lastPreviewMs = now;
                int request = ++generation;
                float target = target(progress);
                WORKER.execute(() -> {
                    if (request != generation || !available()) return;
                    String result = ThorDirectController.writeLinkedBrightness(
                            QuickLinkedBrightnessController.this.context, target, false);
                    if (!"ok".equals(result)) failure(request);
                });
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { tracking = true; }
            @Override public void onStopTrackingTouch(SeekBar bar) {
                tracking = false;
                int request = ++generation;
                float target = target(bar.getProgress());
                WORKER.execute(() -> {
                    if (request != generation || !available()) {
                        failure(request);
                        return;
                    }
                    String result = ThorDirectController.writeLinkedBrightness(
                            QuickLinkedBrightnessController.this.context, target, true);
                    if (!"ok".equals(result)) failure(request);
                });
            }
        });
        icon.setOnClickListener(view -> {
            if (!available()) onUnavailable.run();
            else refresh();
        });
        slider.post(this::refresh);
    }

    void refresh() {
        if (tracking) return;
        int request = ++generation;
        boolean enabled = available();
        slider.setEnabled(enabled);
        icon.setAlpha(enabled ? 1f : 0.45f);
        icon.setContentDescription(context.getString(enabled
                ? R.string.quick_actions_linked_brightness
                : ThorDirectController.isReady(context)
                ? R.string.quick_actions_brightness_auto
                : R.string.quick_actions_brightness_unavailable));
        if (!enabled) return;
        WORKER.execute(() -> {
            String result = ThorDirectController.readLinkedBrightness(context);
            MAIN.post(() -> {
                if (request != generation || tracking || !slider.isAttachedToWindow()) return;
                String[] parts = result.split(",");
                if (parts.length != 3 || !"ok".equals(parts[0])) {
                    failure(request);
                    return;
                }
                try {
                    float upper = Float.parseFloat(parts[1]);
                    float lower = Float.parseFloat(parts[2]);
                    if (!Float.isFinite(upper) || !Float.isFinite(lower)) {
                        failure(request);
                        return;
                    }
                    int percent = Math.max(0, Math.min(100,
                            Math.round((((upper + lower) / 2f) - 0.05f) / 0.95f * 100f)));
                    slider.setProgress(percent);
                    slider.setContentDescription(context.getString(
                            R.string.quick_actions_linked_brightness) + " "
                            + (Math.abs(upper - lower) > 0.02f ? "~" : "")
                            + percent + "%");
                } catch (NumberFormatException error) {
                    failure(request);
                }
            });
        });
    }

    private void failure(int request) {
        MAIN.post(() -> {
            if (request != generation || !slider.isAttachedToWindow()) return;
            slider.setEnabled(false);
            icon.setAlpha(0.45f);
            icon.setContentDescription(context.getString(R.string.quick_actions_brightness_failed));
            onUnavailable.run();
        });
    }

    private boolean available() {
        return ThorDirectController.isReady(context)
                && Settings.System.getInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                        == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
    }

    private static float target(int progress) {
        return 0.05f + Math.max(0, Math.min(100, progress)) * 0.0095f;
    }
}
