package com.mastercook777.heimdall;

import android.app.Application;

public final class HeimdallApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppLanguageManager.applyLegacyApplicationLocale(this);
    }
}
