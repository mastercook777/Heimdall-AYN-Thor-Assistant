package com.mastercook777.heimdall;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

public final class ScreenRecordingService extends Service {
    static final String ACTION_START = BuildConfig.APPLICATION_ID + ".START_RECORDING";
    static final String ACTION_STOP = BuildConfig.APPLICATION_ID + ".STOP_RECORDING";
    static final String EXTRA_RESULT_CODE = "result_code";
    static final String EXTRA_RESULT_DATA = "result_data";
    static final String EXTRA_PROFILE_NAME = "profile_name";
    static final String EXTRA_WIDTH = "width";
    static final String EXTRA_HEIGHT = "height";
    static final String EXTRA_DENSITY = "density";

    private static final String CHANNEL_ID = "heimdall_screen_recording";
    private static final int NOTIFICATION_ID = 1401;
    private static volatile boolean recording;
    private static volatile String lastStatus = "";
    private static volatile boolean lastStatusError;

    private MediaProjection projection;
    private ProjectionRecorder recorder;
    private ParcelFileDescriptor outputDescriptor;
    private Uri outputUri;
    private boolean stopping;

    static boolean isRecording() {
        return recording;
    }

    static String lastStatus() {
        return lastStatus;
    }

    static boolean lastStatusIsError() {
        return lastStatusError;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        if (ACTION_STOP.equals(intent.getAction())) {
            stopRecording(true);
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(intent.getAction()) && !recording) {
            startForeground(NOTIFICATION_ID, createNotification());
            startRecording(intent);
        }
        return START_NOT_STICKY;
    }

    private void startRecording(Intent intent) {
        try {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
            int width = even(Math.max(640, intent.getIntExtra(EXTRA_WIDTH, 1920)));
            int height = even(Math.max(480, intent.getIntExtra(EXTRA_HEIGHT, 1080)));
            int density = Math.max(1, intent.getIntExtra(EXTRA_DENSITY, 320));
            String profile = intent.getStringExtra(EXTRA_PROFILE_NAME);

            outputUri = CaptureStorage.createVideo(getContentResolver(), profile);
            if (outputUri == null) {
                throw new IllegalStateException("cannot create video output");
            }
            outputDescriptor = getContentResolver().openFileDescriptor(outputUri, "w");
            if (outputDescriptor == null) {
                throw new IllegalStateException("cannot open video output");
            }
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            projection = manager == null ? null : manager.getMediaProjection(resultCode, resultData);
            if (projection == null) {
                throw new IllegalStateException("media projection unavailable");
            }
            projection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    stopRecording(true);
                }
            }, null);
            recorder = new ProjectionRecorder(projection, width, height, density, outputDescriptor);
            recorder.start(this);
            recording = true;
            lastStatus = getString(R.string.screen_recording_status_active, width, height);
            lastStatusError = false;
        } catch (Throwable ignored) {
            lastStatus = getString(R.string.screen_recording_status_start_failed);
            lastStatusError = true;
            stopRecording(false);
        }
    }

    private synchronized void stopRecording(boolean publish) {
        if (stopping) {
            return;
        }
        stopping = true;
        if (recorder != null) {
            try {
                publish = recorder.stop() && publish;
            } catch (Throwable ignored) {
                publish = false;
            }
            recorder = null;
        }
        if (projection != null) {
            MediaProjection activeProjection = projection;
            projection = null;
            try {
                activeProjection.stop();
            } catch (Throwable ignored) {
            }
        }
        if (outputDescriptor != null) {
            try {
                outputDescriptor.close();
            } catch (Throwable ignored) {
            }
            outputDescriptor = null;
        }
        try {
            if (publish) {
                CaptureStorage.publish(getContentResolver(), outputUri);
                lastStatus = getString(R.string.screen_recording_status_saved);
                lastStatusError = false;
            } else {
                CaptureStorage.discard(getContentResolver(), outputUri);
                lastStatus = getString(R.string.screen_recording_status_save_failed);
                lastStatusError = true;
            }
        } catch (Throwable ignored) {
            lastStatus = getString(R.string.screen_recording_status_save_failed);
            lastStatusError = true;
        }
        outputUri = null;
        recording = false;
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
        stopping = false;
    }

    private Notification createNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, getString(R.string.screen_recording_notification_channel),
                    NotificationManager.IMPORTANCE_LOW));
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(R.drawable.ic_video)
                .setContentTitle(getString(R.string.screen_recording_notification_title))
                .setContentText(getString(R.string.screen_recording_notification_text))
                .setOngoing(true)
                .build();
    }

    private int even(int value) {
        return value % 2 == 0 ? value : value - 1;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (recording || recorder != null) {
            stopRecording(false);
        }
        super.onDestroy();
    }
}
