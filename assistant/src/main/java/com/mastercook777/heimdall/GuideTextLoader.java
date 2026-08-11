package com.mastercook777.heimdall;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class GuideTextLoader {
    enum Failure {
        NONE,
        TOO_LARGE,
        UNREADABLE
    }

    static final class Result {
        final GuideTextDocument document;
        final Failure failure;

        Result(GuideTextDocument document, Failure failure) {
            this.document = document;
            this.failure = failure;
        }
    }

    interface Callback {
        void onLoaded(Result result);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "HeimdallGuideText");
        thread.setDaemon(true);
        return thread;
    });

    private GuideTextLoader() {
    }

    static void load(Context context, GuideEntry guide, Callback callback) {
        Context appContext = context.getApplicationContext();
        String type = guide == null ? GuideEntry.TYPE_NOTE : guide.type;
        String content = guide == null || guide.content == null ? "" : guide.content;
        Handler main = new Handler(Looper.getMainLooper());
        EXECUTOR.execute(() -> {
            Result result = GuideEntry.TYPE_NOTE.equals(type)
                    ? loadNote(content) : loadFile(appContext, content);
            main.post(() -> callback.onLoaded(result));
        });
    }

    private static Result loadNote(String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > GuideTextDocument.MAX_BYTES) {
            return new Result(null, Failure.TOO_LARGE);
        }
        return new Result(new GuideTextDocument(content), Failure.NONE);
    }

    private static Result loadFile(Context context, String rawUri) {
        try {
            String value = rawUri == null ? "" : rawUri.trim();
            Uri uri = Uri.parse(value.contains("://") ? value : "file://" + value);
            try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) {
                    return new Result(null, Failure.UNREADABLE);
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[16 * 1024];
                int remaining = GuideTextDocument.MAX_BYTES + 1;
                while (remaining > 0) {
                    int read = input.read(buffer, 0, Math.min(buffer.length, remaining));
                    if (read < 0) {
                        break;
                    }
                    output.write(buffer, 0, read);
                    remaining -= read;
                }
                if (output.size() > GuideTextDocument.MAX_BYTES || input.read() >= 0) {
                    return new Result(null, Failure.TOO_LARGE);
                }
                String decoded = GuideTextDecoder.decode(output.toByteArray(), false);
                return new Result(new GuideTextDocument(decoded), Failure.NONE);
            }
        } catch (Exception failure) {
            return new Result(null, Failure.UNREADABLE);
        }
    }
}
