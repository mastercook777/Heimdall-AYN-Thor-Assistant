package com.mastercook777.heimdall;

import android.content.Context;
import android.webkit.WebSettings;

import java.util.Locale;

/** Profile-owned browser identity for the constrained Interactive Map WebView. */
final class InteractiveMapBrowserSettings {
    static final String MODE_MOBILE = "mobile";
    static final String MODE_DESKTOP = "desktop";

    private InteractiveMapBrowserSettings() {
    }

    static String normalize(String mode) {
        return MODE_DESKTOP.equals(mode == null ? "" : mode.trim().toLowerCase(Locale.US))
                ? MODE_DESKTOP
                : MODE_MOBILE;
    }

    static boolean isDesktop(String mode) {
        return MODE_DESKTOP.equals(normalize(mode));
    }

    static void apply(Context context, WebSettings settings, String mode) {
        if (context == null || settings == null) {
            return;
        }
        if (isDesktop(mode)) {
            settings.setUserAgentString(desktopUserAgent(
                    WebSettings.getDefaultUserAgent(context)));
        } else {
            settings.setUserAgentString(null);
        }
    }

    static String desktopUserAgent(String defaultUserAgent) {
        String userAgent = defaultUserAgent == null ? "" : defaultUserAgent.trim();
        if (userAgent.length() == 0) {
            return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        }
        String desktop = userAgent.replaceFirst("\\([^)]*Android[^)]*\\)",
                "(X11; Linux x86_64)");
        desktop = desktop.replace(" Version/4.0", "");
        desktop = desktop.replace(" Mobile", "");
        return desktop;
    }
}
