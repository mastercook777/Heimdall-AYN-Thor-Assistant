package com.mastercook777.heimdall;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

final class TranslationTextStabilizer {
    private static final float CONSECUTIVE_FRAME_SIMILARITY = 0.72f;

    private String candidate = "";
    private int stableFrames;
    private String lastConfirmedCandidate = "";
    private String lastTranslated = "";
    private boolean awaitingConfirmation;

    String accept(String rawText) {
        String normalized = normalize(rawText);
        if (normalized.length() < 2) {
            candidate = "";
            stableFrames = 0;
            awaitingConfirmation = false;
            return null;
        }

        if (isDepartureFromConfirmedCandidate(normalized)) {
            candidate = normalized;
            stableFrames = 1;
        } else if (candidate.length() == 0 || isGrowingSubtitle(candidate, normalized)) {
            candidate = normalized;
            stableFrames = 1;
        } else if (isStableMatch(normalized, candidate)) {
            stableFrames++;
            candidate = normalized;
        } else {
            candidate = normalized;
            stableFrames = 1;
        }

        if (stableFrames < 2) {
            awaitingConfirmation = true;
            return null;
        }
        awaitingConfirmation = false;
        lastConfirmedCandidate = candidate;
        if (isTranslationDuplicate(candidate, lastTranslated)) {
            return null;
        }
        return candidate;
    }

    boolean isAwaitingConfirmation() {
        return awaitingConfirmation;
    }

    void markTranslated(String sourceText) {
        lastTranslated = normalize(sourceText);
    }

    void reset() {
        candidate = "";
        stableFrames = 0;
        lastConfirmedCandidate = "";
        lastTranslated = "";
        awaitingConfirmation = false;
    }

    static String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value,
                Normalizer.Form.NFKC).replace("\r\n", "\n").replace('\r', '\n').trim();
        StringBuilder result = new StringBuilder(normalized.length());
        for (String line : normalized.split("\\n")) {
            String segment = normalizeSegment(line);
            if (segment.length() == 0) continue;
            if (result.length() > 0) result.append('\n');
            result.append(segment);
        }
        return result.toString();
    }

    static String normalizeSegment(String value) {
        return Normalizer.normalize(value == null ? "" : value,
                Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ");
    }

    static boolean isSimilar(String first, String second, float threshold) {
        String a = normalize(first).toLowerCase(Locale.ROOT);
        String b = normalize(second).toLowerCase(Locale.ROOT);
        if (a.length() == 0 || b.length() == 0) return false;
        if (a.equals(b)) return true;
        int maxLength = Math.max(a.length(), b.length());
        if (Math.abs(a.length() - b.length()) > maxLength * (1f - threshold)) return false;
        return 1f - levenshtein(a, b) / (float) maxLength >= threshold;
    }

    static boolean isTranslationDuplicate(String first, String second) {
        String a = translationDuplicateKey(first);
        String b = translationDuplicateKey(second);
        if (a.length() == 0 || b.length() == 0) return false;
        if (a.equals(b)) return true;
        int maxLength = Math.max(a.length(), b.length());
        if (Math.abs(a.length() - b.length()) > maxLength * 0.18f) return false;
        return 1f - levenshtein(a, b) / (float) maxLength >= 0.82f;
    }

    private static String translationDuplicateKey(String value) {
        String normalized = normalize(value);
        if (normalized.length() == 0) return "";
        String[] segments = normalized.split("\\n");
        for (int index = 0; index < segments.length; index++) {
            segments[index] = comparisonKey(segments[index]);
        }
        Arrays.sort(segments);
        StringBuilder key = new StringBuilder(normalized.length());
        for (String segment : segments) {
            if (segment.length() == 0) continue;
            if (key.length() > 0) key.append('|');
            key.append(segment);
        }
        return key.toString();
    }

    private static boolean isStableMatch(String first, String second) {
        String a = comparisonKey(first);
        String b = comparisonKey(second);
        if (a.length() == 0 || b.length() == 0) return false;
        if (a.equals(b)) return true;
        int maxLength = Math.max(a.length(), b.length());
        return 1f - levenshtein(a, b) / (float) maxLength
                >= CONSECUTIVE_FRAME_SIMILARITY;
    }

    private static boolean isGrowingSubtitle(String previous, String current) {
        String oldKey = comparisonKey(previous);
        String newKey = comparisonKey(current);
        return oldKey.length() >= 2
                && newKey.length() > oldKey.length()
                && newKey.startsWith(oldKey);
    }

    private boolean isDepartureFromConfirmedCandidate(String current) {
        return lastConfirmedCandidate.length() > 0
                && isSimilar(candidate, lastConfirmedCandidate, 0.90f)
                && !isSimilar(current, lastConfirmedCandidate, 0.90f);
    }

    private static String comparisonKey(String value) {
        String normalized = normalize(value).toLowerCase(Locale.ROOT);
        StringBuilder key = new StringBuilder(normalized.length());
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            if (Character.isLetterOrDigit(codePoint)) {
                key.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }
        return key.toString();
    }

    private static int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
