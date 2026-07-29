package com.mastercook777.heimdall;

import android.app.Activity;
import android.app.LocaleManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Central app-language owner. Profile data must never depend on this preference. */
final class AppLanguageManager {
    static final String LANGUAGE_SYSTEM = "";
    static final String LANGUAGE_ENGLISH = "en";
    static final String LANGUAGE_SIMPLIFIED_CHINESE = "zh-CN";

    private static final String PREFS = "heimdall_app_language";
    private static final String KEY_LANGUAGE_TAG = "language_tag";

    private AppLanguageManager() {
    }

    static void applyLegacyApplicationLocale(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return;
        }
        applyLegacyResources(context, storedLegacyLanguage(context));
    }

    static String currentLanguage(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            LocaleManager manager = context.getSystemService(LocaleManager.class);
            if (manager == null || manager.getApplicationLocales().isEmpty()) {
                return LANGUAGE_SYSTEM;
            }
            return normalize(manager.getApplicationLocales().get(0).toLanguageTag());
        }
        return storedLegacyLanguage(context);
    }

    static void setLanguage(Activity activity, String languageTag) {
        String normalized = normalize(languageTag);
        if (normalized.equals(currentLanguage(activity))) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            LocaleManager manager = activity.getSystemService(LocaleManager.class);
            if (manager != null) {
                manager.setApplicationLocales(normalized.length() == 0
                        ? LocaleList.getEmptyLocaleList()
                        : LocaleList.forLanguageTags(normalized));
            }
            return;
        }
        preferences(activity).edit().putString(KEY_LANGUAGE_TAG, normalized).commit();
        applyLegacyResources(activity.getApplicationContext(), normalized);
        applyLegacyResources(activity, normalized);
        activity.recreate();
    }

    private static String storedLegacyLanguage(Context context) {
        return normalize(preferences(context).getString(KEY_LANGUAGE_TAG, LANGUAGE_SYSTEM));
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String normalize(String languageTag) {
        if (languageTag == null || languageTag.trim().length() == 0) {
            return LANGUAGE_SYSTEM;
        }
        Locale locale = Locale.forLanguageTag(languageTag);
        if ("zh".equalsIgnoreCase(locale.getLanguage())) {
            return LANGUAGE_SIMPLIFIED_CHINESE;
        }
        if ("en".equalsIgnoreCase(locale.getLanguage())) {
            return LANGUAGE_ENGLISH;
        }
        return LANGUAGE_SYSTEM;
    }

    @SuppressWarnings("deprecation")
    private static void applyLegacyResources(Context context, String languageTag) {
        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        LocaleList locales = languageTag.length() == 0
                ? Resources.getSystem().getConfiguration().getLocales()
                : localesWithSystemFallback(languageTag);
        configuration.setLocales(locales);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    private static LocaleList localesWithSystemFallback(String languageTag) {
        List<Locale> locales = new ArrayList<>();
        Locale selected = Locale.forLanguageTag(languageTag);
        locales.add(selected);
        LocaleList system = Resources.getSystem().getConfiguration().getLocales();
        for (int index = 0; index < system.size(); index++) {
            Locale candidate = system.get(index);
            boolean duplicate = false;
            for (Locale locale : locales) {
                if (locale.toLanguageTag().equalsIgnoreCase(candidate.toLanguageTag())) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                locales.add(candidate);
            }
        }
        return new LocaleList(locales.toArray(new Locale[0]));
    }
}
