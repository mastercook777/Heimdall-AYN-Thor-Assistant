package com.mastercook777.heimdall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** A bounded, source-offset directory for one decoded Guide revision. */
final class GuideChapterIndex {
    static final int MAX_CHAPTERS = 500;
    private static final Pattern ENGLISH = Pattern.compile(
            "(?i)^(chapter|section|part)\\s+(?:[0-9]+|[ivxlcdm]+)(?:\\b|(?=[:. -]))(?:\\s*[:. -]\\s*|\\s+).*$");
    private static final Pattern CHINESE = Pattern.compile(
            "^第[0-9一二三四五六七八九十百千零〇两]+[章节篇部](?:\\s*[:：.、—-]?\\s*.*)?$");

    static final class Chapter {
        final String title;
        final int anchor;
        final int level;
        final String fingerprint;

        Chapter(String title, int anchor, int level, String fingerprint) {
            this.title = title;
            this.anchor = anchor;
            this.level = level;
            this.fingerprint = fingerprint;
        }

        boolean belongsTo(String currentFingerprint) {
            return fingerprint.equals(currentFingerprint);
        }
    }

    static final class Result {
        final List<Chapter> chapters;
        final boolean truncated;

        Result(List<Chapter> chapters, boolean truncated) {
            this.chapters = Collections.unmodifiableList(chapters);
            this.truncated = truncated;
        }
    }

    private GuideChapterIndex() {}

    static Result build(GuideTextDocument document, boolean markdown) {
        ArrayList<Chapter> chapters = new ArrayList<>();
        String source = document.text;
        int start = 0;
        int previousStart = -1;
        String previousLine = "";
        boolean previousEligible = false;
        char fenceMarker = 0;
        int fenceLength = 0;
        boolean truncated = false;
        while (start < source.length() && !Thread.currentThread().isInterrupted()) {
            int end = source.indexOf('\n', start);
            if (end < 0) end = source.length();
            int contentEnd = end > start && source.charAt(end - 1) == '\r' ? end - 1 : end;
            String line = source.substring(start, contentEnd);
            String stripped = stripUpToThreeSpaces(line);
            if (markdown) {
                char marker = fenceMarker(stripped);
                int markers = marker == 0 ? 0 : markerCount(stripped, marker);
                if (fenceMarker != 0) {
                    if (marker == fenceMarker && markers >= fenceLength
                            && stripped.substring(markers).trim().isEmpty()) {
                        fenceMarker = 0;
                    }
                    previousEligible = false;
                } else if (markers >= 3) {
                    fenceMarker = marker;
                    fenceLength = markers;
                    previousEligible = false;
                } else {
                    int atxLevel = atxLevel(stripped);
                    if (atxLevel > 0) {
                        String title = cleanAtx(stripped.substring(atxLevel));
                        if (!title.isEmpty()) {
                            truncated = add(chapters, title, start, atxLevel,
                                    document.fingerprint);
                        }
                    } else if (previousEligible && setextLevel(stripped) > 0) {
                        truncated = add(chapters, clean(previousLine), previousStart,
                                setextLevel(stripped), document.fingerprint);
                    }
                    previousEligible = atxLevel == 0 && line.length() <= 100
                            && !line.trim().isEmpty() && !line.startsWith("    ")
                            && !line.startsWith("\t") && setextLevel(stripped) == 0;
                }
            } else if (line.length() <= 100 && stripped.length() == line.length()
                    && (ENGLISH.matcher(stripped).matches()
                    || CHINESE.matcher(stripped).matches())) {
                truncated = add(chapters, clean(stripped), start, 1, document.fingerprint);
            }
            if (truncated) break;
            previousStart = start;
            previousLine = line;
            start = end == source.length() ? end : end + 1;
        }
        return new Result(chapters, truncated);
    }

    private static boolean add(List<Chapter> chapters, String title, int anchor, int level,
            String fingerprint) {
        if (title.isEmpty()) return false;
        if (chapters.size() == MAX_CHAPTERS) return true;
        chapters.add(new Chapter(title, anchor, level, fingerprint));
        return false;
    }

    private static String stripUpToThreeSpaces(String line) {
        int spaces = 0;
        while (spaces < line.length() && spaces < 3 && line.charAt(spaces) == ' ') spaces++;
        return line.substring(spaces);
    }

    private static char fenceMarker(String line) {
        if (line.isEmpty()) return 0;
        char marker = line.charAt(0);
        return marker == '`' || marker == '~' ? marker : 0;
    }

    private static int markerCount(String line, char marker) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == marker) count++;
        return count;
    }

    private static int atxLevel(String line) {
        int count = markerCount(line, '#');
        return count >= 1 && count <= 6
                && (count == line.length() || Character.isWhitespace(line.charAt(count)))
                ? count : 0;
    }

    private static int setextLevel(String line) {
        String value = line.trim();
        if (value.length() < 3) return 0;
        char marker = value.charAt(0);
        if (marker != '=' && marker != '-') return 0;
        for (int i = 1; i < value.length(); i++) {
            if (value.charAt(i) != marker) return 0;
        }
        return marker == '=' ? 1 : 2;
    }

    private static String clean(String line) {
        String value = line.trim();
        value = value.replaceAll("\\s+", " ");
        return value.length() <= 80 ? value : value.substring(0, 80).trim();
    }

    private static String cleanAtx(String line) {
        String value = line.trim();
        while (value.endsWith("#")) value = value.substring(0, value.length() - 1).trim();
        return clean(value);
    }
}
