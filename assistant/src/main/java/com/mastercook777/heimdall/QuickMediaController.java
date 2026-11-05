package com.mastercook777.heimdall;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;

/** Foreground-only observer for the player's active third-party media sessions. */
final class QuickMediaController {
    enum Action { PLAY_PAUSE, PREVIOUS, NEXT }

    private static final long USABLE_ACTIONS = PlaybackState.ACTION_PLAY
            | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE
            | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT
            | PlaybackState.ACTION_SEEK_TO;

    static final class Snapshot {
        final boolean accessGranted;
        final List<MediaController> sessions;
        final MediaController selected;

        Snapshot(boolean accessGranted, List<MediaController> sessions,
                MediaController selected) {
            this.accessGranted = accessGranted;
            this.sessions = sessions;
            this.selected = selected;
        }

        boolean ambiguous() {
            return accessGranted && selected == null && sessions.size() > 1;
        }
    }

    private final Context context;
    private final ComponentName listenerComponent;
    private final MediaSessionManager manager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable onChanged;
    private final List<MediaController> sessions = new ArrayList<>();
    private final MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) { onChanged.run(); }
        @Override public void onMetadataChanged(MediaMetadata metadata) { onChanged.run(); }
        @Override public void onSessionDestroyed() { refresh(); }
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener =
            controllers -> refresh();
    private MediaSession.Token selectedToken;
    private boolean manualSelection;
    private String manualOriginPackage = "";
    private MediaSession.Token playingAtManualSelection;
    private boolean running;
    private boolean observing;
    private boolean accessGranted;

    QuickMediaController(Context context, Runnable onChanged) {
        this.context = context;
        this.onChanged = onChanged;
        listenerComponent = new ComponentName(context, QuickMediaSessionListenerService.class);
        manager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
    }

    void start() {
        if (running) return;
        running = true;
        QuickMediaSessionListenerService.setConnectionListener(this::refresh);
        refresh();
    }

    void stop() {
        running = false;
        QuickMediaSessionListenerService.setConnectionListener(null);
        detachSessionsListener();
        clearSessions();
        accessGranted = false;
    }

    private void detachSessionsListener() {
        if (observing && manager != null) {
            try {
                manager.removeOnActiveSessionsChangedListener(sessionsListener);
            } catch (RuntimeException ignored) {
                // The listener may already have been dropped after access was revoked.
            }
        }
        observing = false;
    }

    void refresh() {
        if (!running) return;
        clearSessions();
        accessGranted = false;
        if (manager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    NotificationManager notifications = (NotificationManager)
                            context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notifications == null || !notifications
                            .isNotificationListenerAccessGranted(listenerComponent)) {
                        detachSessionsListener();
                        onChanged.run();
                        return;
                    }
                }
                List<MediaController> current = manager.getActiveSessions(listenerComponent);
                accessGranted = true;
                for (MediaController controller : current) {
                    if (!context.getPackageName().equals(controller.getPackageName())) {
                        sessions.add(controller);
                        controller.registerCallback(controllerCallback, mainHandler);
                    }
                }
                if (!observing) {
                    manager.addOnActiveSessionsChangedListener(
                            sessionsListener, listenerComponent, mainHandler);
                    observing = true;
                }
            } catch (SecurityException ignored) {
                clearSessions();
                accessGranted = false;
            }
        }
        if (!accessGranted) detachSessionsListener();
        onChanged.run();
    }

    Snapshot snapshot() {
        List<MediaController> available = new ArrayList<>();
        for (MediaController controller : sessions) {
            PlaybackState state = controller.getPlaybackState();
            if (state != null && (state.getActions() & USABLE_ACTIONS) != 0) {
                available.add(controller);
            }
        }
        if (selectedToken != null && findToken(sessions, selectedToken) == null) {
            selectedToken = null;
            manualSelection = false;
        }
        MediaController selected = findToken(available, selectedToken);
        MediaController playing = uniquePlaying(available);
        String foregroundPackage = upperForegroundPackage();
        MediaController foreground = uniquePackage(available, foregroundPackage);
        if (manualSelection) {
            if (selected == null || sameSession(playing, selected)
                    || (foreground != null
                    && (sameSession(foreground, selected)
                            || !foregroundPackage.equals(manualOriginPackage)))
                    || (playing != null
                            && !sameToken(playing.getSessionToken(), playingAtManualSelection)
                            && !sameSession(playing, selected))) {
                manualSelection = false;
            }
        }
        if (!manualSelection) {
            if (foreground != null) selected = foreground;
            else if (playing != null) selected = playing;
        }
        if (selected == null && available.size() == 1) selected = available.get(0);
        if (selected != null) selectedToken = selected.getSessionToken();
        return new Snapshot(accessGranted, available, selected);
    }

    void select(MediaController controller) {
        selectedToken = controller == null ? null : controller.getSessionToken();
        manualSelection = controller != null;
        manualOriginPackage = upperForegroundPackage();
        MediaController playing = uniquePlaying(sessions);
        playingAtManualSelection = playing == null ? null : playing.getSessionToken();
        onChanged.run();
    }

    private String upperForegroundPackage() {
        if (!ForegroundAppTracker.isEnabled(context)) return "";
        ForegroundAppTracker.Snapshot foreground = ForegroundAppTracker.latest();
        if (foreground == null || foreground.displayId != Display.DEFAULT_DISPLAY
                || System.currentTimeMillis() - foreground.observedAtMs > 5000L) return "";
        return foreground.packageName;
    }

    private static MediaController findToken(List<MediaController> controllers,
            MediaSession.Token token) {
        if (token == null) return null;
        for (MediaController controller : controllers) {
            if (token.equals(controller.getSessionToken())) return controller;
        }
        return null;
    }

    private static MediaController uniquePlaying(List<MediaController> controllers) {
        MediaController selected = null;
        for (MediaController controller : controllers) {
            if (!isPlaying(controller.getPlaybackState())) continue;
            if (selected != null) return null;
            selected = controller;
        }
        return selected;
    }

    private static MediaController uniquePackage(List<MediaController> controllers,
            String packageName) {
        if (packageName.isEmpty()) return null;
        MediaController selected = null;
        for (MediaController controller : controllers) {
            if (!packageName.equals(controller.getPackageName())) continue;
            if (selected != null) return null;
            selected = controller;
        }
        return selected;
    }

    private static boolean sameSession(MediaController left, MediaController right) {
        return left != null && right != null
                && sameToken(left.getSessionToken(), right.getSessionToken());
    }

    private static boolean sameToken(MediaSession.Token left, MediaSession.Token right) {
        return left != null && left.equals(right);
    }

    boolean perform(Action action) {
        MediaController controller = snapshot().selected;
        if (controller == null) return false;
        PlaybackState state = controller.getPlaybackState();
        if (state == null) return false;
        long supported = state.getActions();
        MediaController.TransportControls controls = controller.getTransportControls();
        try {
        switch (action) {
            case PLAY_PAUSE:
                if (isPlaying(state)) {
                    if ((supported & (PlaybackState.ACTION_PAUSE
                            | PlaybackState.ACTION_PLAY_PAUSE)) == 0) return false;
                    controls.pause();
                } else {
                    if ((supported & (PlaybackState.ACTION_PLAY
                            | PlaybackState.ACTION_PLAY_PAUSE)) == 0) return false;
                    controls.play();
                }
                return true;
            case PREVIOUS:
                if ((supported & PlaybackState.ACTION_SKIP_TO_PREVIOUS) == 0) return false;
                controls.skipToPrevious();
                return true;
            case NEXT:
                if ((supported & PlaybackState.ACTION_SKIP_TO_NEXT) == 0) return false;
                controls.skipToNext();
                return true;
            default:
                return false;
        }
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    boolean seekTo(long positionMs) {
        MediaController controller = snapshot().selected;
        PlaybackState state = controller == null ? null : controller.getPlaybackState();
        if (state == null || (state.getActions() & PlaybackState.ACTION_SEEK_TO) == 0) {
            return false;
        }
        try {
            controller.getTransportControls().seekTo(Math.max(0, positionMs));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    static long duration(MediaController controller) {
        MediaMetadata metadata = controller == null ? null : controller.getMetadata();
        return metadata == null ? 0 : Math.max(0,
                metadata.getLong(MediaMetadata.METADATA_KEY_DURATION));
    }

    static long position(MediaController controller) {
        PlaybackState state = controller == null ? null : controller.getPlaybackState();
        if (state == null || state.getPosition() < 0) return 0;
        long position = state.getPosition();
        if (isPlaying(state) && state.getLastPositionUpdateTime() > 0) {
            long elapsed = Math.max(0,
                    SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime());
            position += Math.round(elapsed * state.getPlaybackSpeed());
        }
        long duration = duration(controller);
        return Math.max(0, duration > 0 ? Math.min(duration, position) : position);
    }

    static boolean supportsSeek(MediaController controller) {
        PlaybackState state = controller == null ? null : controller.getPlaybackState();
        return state != null && (state.getActions() & PlaybackState.ACTION_SEEK_TO) != 0;
    }

    static boolean supports(MediaController controller, Action action) {
        PlaybackState state = controller == null ? null : controller.getPlaybackState();
        if (state == null) return false;
        long supported = state.getActions();
        if (action == Action.PREVIOUS) {
            return (supported & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0;
        }
        if (action == Action.NEXT) {
            return (supported & PlaybackState.ACTION_SKIP_TO_NEXT) != 0;
        }
        return (supported & (isPlaying(state)
                ? PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE
                : PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PLAY_PAUSE)) != 0;
    }

    static boolean isPlaying(PlaybackState state) {
        return state != null && (state.getState() == PlaybackState.STATE_PLAYING
                || state.getState() == PlaybackState.STATE_BUFFERING);
    }

    String sourceName(MediaController controller) {
        if (controller == null) return "";
        String packageName = controller.getPackageName();
        try {
            PackageManager packages = context.getPackageManager();
            ApplicationInfo info = packages.getApplicationInfo(packageName, 0);
            String label = packages.getApplicationLabel(info).toString().trim();
            return label.isEmpty() || label.equals(packageName)
                    ? context.getString(R.string.quick_media_unknown_player)
                    : label;
        } catch (PackageManager.NameNotFoundException | SecurityException ignored) {
            return context.getString(R.string.quick_media_unknown_player);
        }
    }

    Drawable sourceIcon(MediaController controller) {
        PackageManager packages = context.getPackageManager();
        if (controller != null) {
            try {
                return packages.getApplicationIcon(controller.getPackageName());
            } catch (PackageManager.NameNotFoundException | SecurityException ignored) {
                // Keep an ordinary app icon when a session belongs to a hidden package.
            }
        }
        return packages.getDefaultActivityIcon();
    }

    Intent launchIntent(MediaController controller) {
        if (controller == null) return null;
        try {
            return context.getPackageManager().getLaunchIntentForPackage(
                    controller.getPackageName());
        } catch (SecurityException ignored) {
            return null;
        }
    }

    static String title(MediaController controller) {
        MediaMetadata metadata = controller == null ? null : controller.getMetadata();
        if (metadata == null) return "";
        CharSequence displayTitle = metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        if (displayTitle != null && displayTitle.length() > 0) return displayTitle.toString();
        CharSequence title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
        return title == null ? "" : title.toString();
    }

    static String subtitle(MediaController controller) {
        MediaMetadata metadata = controller == null ? null : controller.getMetadata();
        if (metadata == null) return "";
        CharSequence subtitle = metadata.getDescription() == null ? null
                : metadata.getDescription().getSubtitle();
        if (subtitle != null && subtitle.length() > 0) return subtitle.toString();
        CharSequence artist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
        return artist == null ? "" : artist.toString();
    }

    static Bitmap artwork(MediaController controller) {
        MediaMetadata metadata = controller == null ? null : controller.getMetadata();
        if (metadata == null) return null;
        Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (art == null) art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (art == null) art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        return art;
    }

    private void clearSessions() {
        for (MediaController controller : sessions) {
            controller.unregisterCallback(controllerCallback);
        }
        sessions.clear();
    }
}
