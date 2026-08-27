package com.mastercook777.heimdall;

import java.util.List;
import java.util.Locale;

final class ProfileAutoSwitchResolver {
    static final int NO_MATCH = -1;
    private static final int AMBIGUOUS = -2;

    private ProfileAutoSwitchResolver() {
    }

    static int resolve(List<GameProfile> profiles, int currentIndex,
            ForegroundAppTracker.Snapshot snapshot) {
        return resolve(profiles, currentIndex, snapshot, GameContextSnapshot.UNKNOWN);
    }

    static int resolve(List<GameProfile> profiles, int currentIndex,
            ForegroundAppTracker.Snapshot snapshot, GameContextSnapshot gameContext) {
        if (profiles == null || snapshot == null || snapshot.packageName.length() == 0) {
            return NO_MATCH;
        }

        String packageName = normalize(snapshot.packageName);
        int packageMatches = 0;
        int onlyPackageMatch = NO_MATCH;
        int defaultMatch = NO_MATCH;
        int identityMatch = NO_MATCH;

        for (int i = 0; i < profiles.size(); i++) {
            GameProfile profile = profiles.get(i);
            if (!packageName.equals(normalize(profile.packageHint))) {
                continue;
            }
            packageMatches++;
            onlyPackageMatch = i;
            if (profile.defaultForPackage) {
                defaultMatch = defaultMatch == NO_MATCH ? i : AMBIGUOUS;
            }
            GameContextBinding binding = profile.safeGameContextBinding();
            if (isExactIdentityMatch(packageName, gameContext, binding)) {
                if (identityMatch != NO_MATCH) {
                    return currentHasExactIdentity(profiles, currentIndex,
                            packageName, gameContext) ? currentIndex : NO_MATCH;
                }
                identityMatch = i;
            }
        }

        if (identityMatch != NO_MATCH) return identityMatch;
        if (currentBoundToPackage(profiles, currentIndex, packageName)) {
            return currentIndex;
        }
        if (defaultMatch >= 0) {
            return defaultMatch;
        }
        return packageMatches == 1 ? onlyPackageMatch : NO_MATCH;
    }

    private static boolean currentHasExactIdentity(List<GameProfile> profiles, int currentIndex,
            String packageName, GameContextSnapshot context) {
        return currentIndex >= 0 && currentIndex < profiles.size()
                && isExactIdentityMatch(packageName, context,
                profiles.get(currentIndex).safeGameContextBinding());
    }

    private static boolean isExactIdentityMatch(String packageName, GameContextSnapshot context,
            GameContextBinding binding) {
        return context != null
                && context.state == GameContextSnapshot.State.ACTIVE
                && packageName.equals(normalize(context.packageName))
                && binding != null
                && binding.isBound()
                && binding.kind.equals(context.kind)
                && binding.identityKey.equals(context.identityKey);
    }

    private static boolean currentBoundToPackage(List<GameProfile> profiles, int currentIndex,
            String packageName) {
        return currentIndex >= 0 && currentIndex < profiles.size()
                && packageName.equals(normalize(profiles.get(currentIndex).packageHint));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
