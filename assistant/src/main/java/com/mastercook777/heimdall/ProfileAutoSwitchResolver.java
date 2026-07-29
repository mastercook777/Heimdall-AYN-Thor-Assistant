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
        if (profiles == null || snapshot == null || snapshot.packageName.length() == 0) {
            return NO_MATCH;
        }

        String packageName = normalize(snapshot.packageName);
        String context = normalize(snapshot.contextText());
        int packageMatches = 0;
        int onlyPackageMatch = NO_MATCH;
        int defaultMatch = NO_MATCH;
        int contextMatch = NO_MATCH;

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
            String hint = normalize(profile.romContextHint);
            if (hint.length() > 0 && context.contains(hint)) {
                if (contextMatch != NO_MATCH) {
                    return currentBoundToPackage(profiles, currentIndex, packageName)
                            ? currentIndex : NO_MATCH;
                }
                contextMatch = i;
            }
        }

        if (contextMatch != NO_MATCH) {
            return contextMatch;
        }
        if (currentBoundToPackage(profiles, currentIndex, packageName)) {
            return currentIndex;
        }
        if (defaultMatch >= 0) {
            return defaultMatch;
        }
        return packageMatches == 1 ? onlyPackageMatch : NO_MATCH;
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
