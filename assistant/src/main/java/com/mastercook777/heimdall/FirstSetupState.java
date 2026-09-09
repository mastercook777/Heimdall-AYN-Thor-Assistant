package com.mastercook777.heimdall;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistence for the optional first-setup surface, separate from startup readiness. */
final class FirstSetupState {
    static final int PHASE_WELCOME = 0;
    static final int PHASE_CHECKLIST = 1;
    static final int PHASE_RESOLVED = 2;

    private static final String PREFS = "heimdall_first_setup";
    private static final String KEY_INITIALIZED = "initialized_v1";
    private static final String KEY_PHASE = "phase_v1";
    private static final String KEY_PROFILE_CREATED = "profile_created_v1";

    private FirstSetupState() {
    }

    static void initialize(Context context, boolean hadStoredProfiles) {
        SharedPreferences preferences = preferences(context);
        if (preferences.getBoolean(KEY_INITIALIZED, false)) return;
        preferences.edit()
                .putBoolean(KEY_INITIALIZED, true)
                .putInt(KEY_PHASE, hadStoredProfiles ? PHASE_RESOLVED : PHASE_WELCOME)
                .putBoolean(KEY_PROFILE_CREATED, hadStoredProfiles)
                .apply();
    }

    static int phase(Context context) {
        return preferences(context).getInt(KEY_PHASE, PHASE_WELCOME);
    }

    static void markChecklistStarted(Context context) {
        preferences(context).edit().putInt(KEY_PHASE, PHASE_CHECKLIST).apply();
    }

    static void markResolved(Context context) {
        preferences(context).edit().putInt(KEY_PHASE, PHASE_RESOLVED).apply();
    }

    static boolean isProfileCreated(Context context) {
        return preferences(context).getBoolean(KEY_PROFILE_CREATED, false);
    }

    static void markProfileCreated(Context context) {
        preferences(context).edit().putBoolean(KEY_PROFILE_CREATED, true).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
