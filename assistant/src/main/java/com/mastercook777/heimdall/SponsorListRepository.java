package com.mastercook777.heimdall;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class SponsorListRepository {
    static final String DOCUMENT_URL =
            "https://raw.githubusercontent.com/mastercook777/Heimdall-AYN-Thor-Assistant/main/docs/heimdall-supporters.json";
    private static final String PREFS = "heimdall_sponsor_list";
    private static final String KEY_DOCUMENT = "last_verified_document";
    private static final int MAX_BYTES = 32768;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    interface Listener {
        void onResult(List<String> names, boolean updated);
    }

    private SponsorListRepository() {
    }

    static List<String> cached(Context context) {
        String raw = context.getApplicationContext().getSharedPreferences(PREFS,
                Context.MODE_PRIVATE).getString(KEY_DOCUMENT, "");
        try {
            return parse(raw);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    static void refresh(Context context, Listener listener) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            List<String> names = cached(app);
            boolean updated = false;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(DOCUMENT_URL).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Cache-Control", "no-cache");
                if (connection.getResponseCode() == 200) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try (InputStream stream = connection.getInputStream()) {
                        byte[] buffer = new byte[4096];
                        int read;
                        while ((read = stream.read(buffer)) != -1) {
                            if (bytes.size() + read > MAX_BYTES) {
                                throw new IllegalArgumentException("Sponsor document too large");
                            }
                            bytes.write(buffer, 0, read);
                        }
                    }
                    String raw = bytes.toString(StandardCharsets.UTF_8.name());
                    names = parse(raw);
                    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putString(KEY_DOCUMENT, raw).apply();
                    updated = true;
                }
            } catch (Exception ignored) {
                // Keep the last verified public list when offline or unavailable.
            } finally {
                if (connection != null) connection.disconnect();
            }
            List<String> result = names;
            boolean online = updated;
            MAIN.post(() -> listener.onResult(result, online));
        });
    }

    static List<String> parse(String raw) throws Exception {
        if (raw == null || raw.length() == 0) return Collections.emptyList();
        JSONObject document = new JSONObject(raw);
        if (document.optInt("version", 0) != 1) {
            throw new IllegalArgumentException("Unsupported sponsor document version");
        }
        JSONArray supporters = document.getJSONArray("supporters");
        if (supporters.length() > 100) {
            throw new IllegalArgumentException("Too many supporters");
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (int i = 0; i < supporters.length(); i++) {
            String name = supporters.getString(i).trim();
            if (name.length() == 0 || name.length() > 40) continue;
            boolean valid = true;
            for (int j = 0; j < name.length(); j++) {
                if (Character.isISOControl(name.charAt(j))) {
                    valid = false;
                    break;
                }
            }
            if (valid) unique.add(name);
        }
        return new ArrayList<>(unique);
    }
}
