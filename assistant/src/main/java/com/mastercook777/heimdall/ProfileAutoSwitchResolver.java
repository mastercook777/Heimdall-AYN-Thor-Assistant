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
        boolean identityAmbiguous = false;
        int platformMatch = NO_MATCH;
        boolean platformAmbiguous = false;
        int appOnlyMatches = 0;
        int onlyAppOnlyMatch = NO_MATCH;
        int appOnlyDefaultMatch = NO_MATCH;
        boolean hasActiveGameContext = isActiveContextForPackage(packageName, gameContext);

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
                identityAmbiguous = identityMatch != NO_MATCH;
                if (identityMatch == NO_MATCH) identityMatch = i;
            }
            if (isPlatformIdentityMatch(packageName, gameContext, binding)) {
                platformAmbiguous = platformMatch != NO_MATCH;
                if (platformMatch == NO_MATCH) platformMatch = i;
            }
            if (hasActiveGameContext && !binding.isBound()) {
                appOnlyMatches++;
                onlyAppOnlyMatch = i;
                if (profile.defaultForPackage) {
                    appOnlyDefaultMatch = appOnlyDefaultMatch == NO_MATCH
                            ? i : AMBIGUOUS;
                }
            }
        }

        if (identityAmbiguous) {
            return currentHasExactIdentity(profiles, currentIndex,
                    packageName, gameContext) ? currentIndex : NO_MATCH;
        }
        if (identityMatch != NO_MATCH) return identityMatch;
        if (platformAmbiguous) {
            return currentHasPlatformIdentity(profiles, currentIndex,
                    packageName, gameContext) ? currentIndex : NO_MATCH;
        }
        if (platformMatch != NO_MATCH) return platformMatch;
        if (hasActiveGameContext && appOnlyMatches > 0) {
            if (currentIsAppOnlyPackageMatch(profiles, currentIndex, packageName)) {
                return currentIndex;
            }
            if (appOnlyDefaultMatch >= 0) {
                return appOnlyDefaultMatch;
            }
            return appOnlyMatches == 1 ? onlyAppOnlyMatch : NO_MATCH;
        }
        if (currentBoundToPackage(profiles, currentIndex, packageName)) {
            return currentIndex;
        }
        if (defaultMatch >= 0) {
            return defaultMatch;
        }
        return packageMatches == 1 ? onlyPackageMatch : NO_MATCH;
    }

    static String diagnosticSummary(List<GameProfile> profiles, int currentIndex,
            ForegroundAppTracker.Snapshot snapshot, GameContextSnapshot gameContext) {
        if (profiles == null || snapshot == null || snapshot.packageName.length() == 0) {
            return "profiles=unavailable";
        }
        String packageName = normalize(snapshot.packageName);
        int packageMatches = 0;
        int exactMatches = 0;
        int platformMatches = 0;
        int appOnlyMatches = 0;
        for (GameProfile profile : profiles) {
            if (!packageName.equals(normalize(profile.packageHint))) {
                continue;
            }
            packageMatches++;
            GameContextBinding binding = profile.safeGameContextBinding();
            if (isExactIdentityMatch(packageName, gameContext, binding)) {
                exactMatches++;
            }
            if (isPlatformIdentityMatch(packageName, gameContext, binding)) {
                platformMatches++;
            }
            if (isActiveContextForPackage(packageName, gameContext) && !binding.isBound()) {
                appOnlyMatches++;
            }
        }
        boolean currentPackage = currentBoundToPackage(profiles, currentIndex, packageName);
        boolean currentBound = currentPackage
                && profiles.get(currentIndex).safeGameContextBinding().isBound();
        return "packageMatches=" + packageMatches
                + " exactMatches=" + exactMatches
                + " platformMatches=" + platformMatches
                + " appOnlyMatches=" + appOnlyMatches
                + " currentPackage=" + currentPackage
                + " currentBound=" + currentBound;
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

    private static boolean currentHasPlatformIdentity(List<GameProfile> profiles,
            int currentIndex, String packageName, GameContextSnapshot context) {
        return currentIndex >= 0 && currentIndex < profiles.size()
                && isPlatformIdentityMatch(packageName, context,
                profiles.get(currentIndex).safeGameContextBinding());
    }

    private static boolean isPlatformIdentityMatch(String packageName,
            GameContextSnapshot context, GameContextBinding binding) {
        return context != null
                && context.state == GameContextSnapshot.State.ACTIVE
                && packageName.equals(normalize(context.packageName))
                && context.hasPlatformIdentity()
                && binding != null
                && binding.isPlatformBinding()
                && binding.kind.equals(context.platformKind)
                && binding.identityKey.equals(context.platformIdentityKey);
    }

    private static boolean isActiveContextForPackage(String packageName,
            GameContextSnapshot context) {
        return context != null
                && context.state == GameContextSnapshot.State.ACTIVE
                && packageName.equals(normalize(context.packageName));
    }

    private static boolean currentIsAppOnlyPackageMatch(List<GameProfile> profiles,
            int currentIndex, String packageName) {
        return currentBoundToPackage(profiles, currentIndex, packageName)
                && !profiles.get(currentIndex).safeGameContextBinding().isBound();
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
