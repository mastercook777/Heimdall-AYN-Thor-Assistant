package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.Color;

/** App-global ownership for opt-in Theme to Thor joystick-light synchronization. */
final class ThemeJoystickLighting {
    static final String PREFS = "heimdall_theme_hardware";
    static final String KEY_MATCH_THEME = "match_joystick_lighting";
    static final String KEY_BRIGHTNESS_PERCENT = "joystick_lighting_brightness_percent";
    static final int MIN_BRIGHTNESS_PERCENT = 10;
    static final int MAX_BRIGHTNESS_PERCENT = 100;
    static final int DEFAULT_BRIGHTNESS_PERCENT = 100;
    static final int BRIGHTNESS_STEP_PERCENT = 5;

    private static final ThorJoystickLedAdapter ADAPTER = new ThorJoystickLedAdapter();

    private ThemeJoystickLighting() {
    }

    static boolean isEnabled(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_MATCH_THEME, false);
    }

    static int getBrightnessPercent(Context context) {
        int stored = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_BRIGHTNESS_PERCENT, DEFAULT_BRIGHTNESS_PERCENT);
        return normalizeBrightnessPercent(stored);
    }

    static void setBrightnessPercent(Context context, int brightnessPercent) {
        Context appContext = context.getApplicationContext();
        int normalized = normalizeBrightnessPercent(brightnessPercent);
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_BRIGHTNESS_PERCENT, normalized).apply();
        syncIfEnabled(appContext);
    }

    static void setEnabled(Context context, boolean enabled) {
        Context appContext = context.getApplicationContext();
        appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_MATCH_THEME, enabled).apply();
        if (enabled) {
            syncIfEnabled(appContext);
        } else {
            ADAPTER.turnOff();
        }
    }

    static void syncIfEnabled(Context context) {
        if (!isEnabled(context)) {
            return;
        }
        ADAPTER.applyThemeColor(scaleHardwareColor(
                HeimdallUi.hardwareAccent(context), getBrightnessPercent(context)));
    }

    static int normalizeBrightnessPercent(int brightnessPercent) {
        int clamped = Math.max(MIN_BRIGHTNESS_PERCENT,
                Math.min(MAX_BRIGHTNESS_PERCENT, brightnessPercent));
        int steps = (clamped - MIN_BRIGHTNESS_PERCENT
                + BRIGHTNESS_STEP_PERCENT / 2) / BRIGHTNESS_STEP_PERCENT;
        return MIN_BRIGHTNESS_PERCENT + steps * BRIGHTNESS_STEP_PERCENT;
    }

    static int scaleHardwareColor(int color, int brightnessPercent) {
        int percent = normalizeBrightnessPercent(brightnessPercent);
        if (percent == MAX_BRIGHTNESS_PERCENT) {
            return color;
        }
        return Color.rgb(
                scaleChannel(Color.red(color), percent),
                scaleChannel(Color.green(color), percent),
                scaleChannel(Color.blue(color), percent));
    }

    private static int scaleChannel(int channel, int brightnessPercent) {
        return (channel * brightnessPercent + 50) / 100;
    }
}
