package com.mastercook777.heimdall;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class TranslationApiClient {
    interface Callback {
        void onSuccess(String translatedText);
        void onError(String message);
    }

    static final class Request {
        private volatile boolean cancelled;
        private volatile HttpURLConnection connection;
        private Future<?> future;

        void cancel() {
            cancelled = true;
            if (connection != null) connection.disconnect();
            if (future != null) future.cancel(true);
        }
    }

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1),
            new ThreadPoolExecutor.DiscardOldestPolicy());
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    Request translate(TranslationProviderConfig config, String sourceText,
            String targetLanguage, Callback callback) {
        Request request = new Request();
        TranslationProviderConfig snapshot = config.copy();
        String preparedSource = prepareSourceText(sourceText);
        request.future = executor.submit(() -> execute(request, snapshot, preparedSource,
                targetLanguage, callback));
        return request;
    }

    void shutdown() {
        executor.shutdownNow();
    }

    static String prepareSourceText(String sourceText) {
        return TranslationTextStabilizer.normalizeSegment(
                TranslationTextStabilizer.normalize(sourceText));
    }

    private void execute(Request request, TranslationProviderConfig config, String sourceText,
            String targetLanguage, Callback callback) {
        if (!config.isComplete()) {
            postError(request, callback, "Translation provider is not configured");
            return;
        }
        if (sourceText.length() == 0) {
            postError(request, callback, "Translation source was empty");
            return;
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(chatCompletionsUrl(config.resolvedBaseUrl()));
            if (!"https".equalsIgnoreCase(url.getProtocol())) {
                postError(request, callback, "HTTPS is required");
                return;
            }
            connection = (HttpURLConnection) url.openConnection();
            request.connection = connection;
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + config.apiKey);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            JSONObject body = new JSONObject();
            body.put("model", config.resolvedModel());
            body.put("temperature", 0.1d);
            body.put("max_tokens", 768);
            body.put("stream", false);
            if (TranslationProviderConfig.SILICONFLOW_GLOBAL_MODEL.equals(config.resolvedModel())) {
                body.put("enable_thinking", false);
            }
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content",
                    "Translate the following game UI text into "
                            + TranslationConfig.targetPromptName(targetLanguage)
                            + ". Return only the translation and preserve names and tone."));
            messages.put(new JSONObject().put("role", "user").put("content", sourceText));
            body.put("messages", messages);

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = readBody(stream);
            if (request.cancelled) return;
            if (status < 200 || status >= 300) {
                postError(request, callback, friendlyHttpError(status, response));
                return;
            }
            JSONObject result = new JSONObject(response);
            JSONArray choices = result.optJSONArray("choices");
            JSONObject first = choices == null ? null : choices.optJSONObject(0);
            JSONObject message = first == null ? null : first.optJSONObject("message");
            String translated = message == null ? "" : message.optString("content", "").trim();
            if (translated.length() == 0) {
                postError(request, callback, "Translation response was empty");
                return;
            }
            mainHandler.post(() -> {
                if (!request.cancelled) callback.onSuccess(translated);
            });
        } catch (java.net.SocketTimeoutException ignored) {
            postError(request, callback, "Translation request timed out");
        } catch (Exception ignored) {
            postError(request, callback, "Translation request failed");
        } finally {
            if (connection != null) connection.disconnect();
            if (request.connection == connection) request.connection = null;
        }
    }

    private void postError(Request request, Callback callback, String message) {
        mainHandler.post(() -> {
            if (!request.cancelled) callback.onError(message);
        });
    }

    private static String chatCompletionsUrl(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value.endsWith("/chat/completions") ? value : value + "/chat/completions";
    }

    private static String readBody(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && result.length() < 131072) {
                result.append(line);
            }
        }
        return result.toString();
    }

    private static String friendlyHttpError(int status, String response) {
        try {
            JSONObject error = new JSONObject(response).optJSONObject("error");
            String message = error == null ? "" : error.optString("message", "").trim();
            if (message.length() > 0) return "HTTP " + status + ": " + message;
        } catch (Exception ignored) {
        }
        return "Translation service returned HTTP " + status;
    }
}
