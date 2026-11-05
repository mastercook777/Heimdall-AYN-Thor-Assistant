package com.mastercook777.heimdall;

import android.view.Display;

import java.util.List;
import java.util.Locale;

/** A configured Profile is the fallback only for a verified unbound upper scene. */
final class DesktopProfilePolicy {
    private String manuallySelectedScene = "";

    static int findDesktopIndex(List<GameProfile> profiles, String profileId) {
        if (profiles == null || profileId == null || profileId.trim().isEmpty()) return -1;
        for (int i = 0; i < profiles.size(); i++) {
            if (profileId.equals(profiles.get(i).safeProfileId())) return i;
        }
        return -1;
    }

    static boolean isConfirmedUnbound(List<GameProfile> profiles,
            ForegroundAppTracker.Snapshot foreground, GameContextSnapshot game,
            boolean upperLauncherVisible) {
        if (upperLauncherVisible && foreground == null) return true;
        if (profiles == null || foreground == null
                || foreground.displayId != Display.DEFAULT_DISPLAY
                || foreground.packageName.length() == 0) return false;

        String packageName = normalize(foreground.packageName);
        boolean activeGame = game != null
                && game.state == GameContextSnapshot.State.ACTIVE
                && packageName.equals(normalize(game.packageName));
        boolean confirmedNoGame = game != null
                && game.state == GameContextSnapshot.State.NONE
                && packageName.equals(normalize(game.packageName));
        for (GameProfile profile : profiles) {
            if (!packageName.equals(normalize(profile.packageHint))) continue;
            GameContextBinding binding = profile.safeGameContextBinding();
            if (confirmedNoGame) {
                if (!binding.isBound()) return false;
                continue;
            }
            if (!activeGame) return false;
            if (!binding.isBound()
                    || (binding.kind.equals(game.kind)
                            && binding.identityKey.equals(game.identityKey))
                    || (game.hasPlatformIdentity() && binding.isPlatformBinding()
                            && binding.kind.equals(game.platformKind)
                            && binding.identityKey.equals(game.platformIdentityKey))) {
                return false;
            }
        }
        return true;
    }

    void recordManualSelection(ForegroundAppTracker.Snapshot foreground,
            GameContextSnapshot game, boolean upperLauncherVisible) {
        manuallySelectedScene = sceneKey(foreground, game, upperLauncherVisible);
    }

    boolean suppressesAutomaticSwitch(ForegroundAppTracker.Snapshot foreground,
            GameContextSnapshot game, boolean upperLauncherVisible) {
        String scene = sceneKey(foreground, game, upperLauncherVisible);
        if (scene.length() == 0 || !scene.equals(manuallySelectedScene)) {
            manuallySelectedScene = "";
            return false;
        }
        return true;
    }

    private static String sceneKey(ForegroundAppTracker.Snapshot foreground,
            GameContextSnapshot game, boolean upperLauncherVisible) {
        if (upperLauncherVisible && foreground == null) return "launcher";
        if (foreground == null || foreground.displayId != Display.DEFAULT_DISPLAY) return "";
        String packageName = normalize(foreground.packageName);
        if (game != null && game.state == GameContextSnapshot.State.ACTIVE
                && packageName.equals(normalize(game.packageName))) {
            return packageName + ":" + game.kind + ":" + game.identityKey
                    + ":" + game.observedAt;
        }
        return packageName;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
