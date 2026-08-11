package com.mastercook777.heimdall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class GuideTextDocument {
    static final int MAX_BYTES = 4 * 1024 * 1024;
    private static final int TARGET_CHUNK_CHARS = 4096;
    private static final int HARD_MAX_CHUNK_CHARS = 65536;
    private static final int BOOKMARK_HEADING_SEARCH_CHARS = 16000;

    static final class Chunk {
        final int start;
        final int end;
        final String text;

        Chunk(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }

    final String text;
    final String fingerprint;
    final List<Chunk> chunks;
    private int maxColumns;

    GuideTextDocument(String text) {
        this.text = text == null ? "" : text;
        fingerprint = GuideEntry.readingFingerprint(this.text);
        chunks = Collections.unmodifiableList(buildChunks(this.text));
        maxColumns = calculateMaxVisualColumns();
    }

    int chunkIndexForAnchor(int anchor) {
        if (chunks.isEmpty()) {
            return 0;
        }
        int target = Math.max(0, Math.min(text.length(), anchor));
        int low = 0;
        int high = chunks.size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            Chunk chunk = chunks.get(middle);
            if (target < chunk.start) {
                high = middle - 1;
            } else if (target >= chunk.end && middle < chunks.size() - 1) {
                low = middle + 1;
            } else {
                return middle;
            }
        }
        return Math.max(0, Math.min(chunks.size() - 1, low));
    }

    int maxVisualColumns() {
        return maxColumns;
    }

    String nearbyMarkdownHeading(int anchor) {
        if (text.length() == 0) {
            return "";
        }
        int target = Math.max(0, Math.min(text.length(), anchor));
        int lineStart = lineStartAt(target);
        int minimum = Math.max(0, target - BOOKMARK_HEADING_SEARCH_CHARS);
        while (lineStart >= minimum) {
            String line = lineAt(lineStart);
            String heading = atxHeading(line);
            if (heading.length() > 0) {
                return heading;
            }
            if (isSetextUnderline(line)) {
                int previousStart = previousLineStart(lineStart);
                if (previousStart >= minimum) {
                    String previous = cleanHeading(lineAt(previousStart));
                    if (previous.length() > 0) {
                        return previous;
                    }
                }
            }
            if (lineStart == 0) {
                break;
            }
            lineStart = previousLineStart(lineStart);
        }
        return "";
    }

    private int calculateMaxVisualColumns() {
        int tab = 4;
        int column = 0;
        int maximum = 1;
        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '\n' || value == '\r') {
                maximum = Math.max(maximum, column);
                column = 0;
            } else if (value == '\t') {
                column += tab - (column % tab);
            } else {
                column++;
            }
        }
        return Math.min(4096, Math.max(maximum, column));
    }

    private int lineStartAt(int anchor) {
        int newline = text.lastIndexOf('\n', Math.max(-1, anchor - 1));
        return newline < 0 ? 0 : newline + 1;
    }

    private int previousLineStart(int lineStart) {
        if (lineStart <= 0) {
            return -1;
        }
        int newline = text.lastIndexOf('\n', Math.max(-1, lineStart - 2));
        return newline < 0 ? 0 : newline + 1;
    }

    private String lineAt(int lineStart) {
        int end = text.indexOf('\n', lineStart);
        if (end < 0) {
            end = text.length();
        }
        if (end > lineStart && text.charAt(end - 1) == '\r') {
            end--;
        }
        return text.substring(lineStart, end);
    }

    private static String atxHeading(String line) {
        int index = 0;
        while (index < line.length() && index < 3 && line.charAt(index) == ' ') {
            index++;
        }
        int markerStart = index;
        while (index < line.length() && index - markerStart < 6
                && line.charAt(index) == '#') {
            index++;
        }
        if (index == markerStart || (index < line.length()
                && !Character.isWhitespace(line.charAt(index)))) {
            return "";
        }
        return cleanHeading(line.substring(index));
    }

    private static boolean isSetextUnderline(String line) {
        String trimmed = line.trim();
        if (trimmed.length() < 3) {
            return false;
        }
        char marker = trimmed.charAt(0);
        if (marker != '=' && marker != '-') {
            return false;
        }
        for (int index = 1; index < trimmed.length(); index++) {
            if (trimmed.charAt(index) != marker) {
                return false;
            }
        }
        return true;
    }

    private static String cleanHeading(String raw) {
        String value = raw == null ? "" : raw.trim();
        while (value.endsWith("#")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        value = value.replaceAll("\\s+", " ");
        return value.length() <= 80 ? value : value.substring(0, 80).trim();
    }

    private static List<Chunk> buildChunks(String text) {
        ArrayList<Chunk> result = new ArrayList<>();
        if (text.length() == 0) {
            result.add(new Chunk(0, 0, ""));
            return result;
        }
        int start = 0;
        while (start < text.length()) {
            int target = Math.min(text.length(), start + TARGET_CHUNK_CHARS);
            int end = target;
            if (target < text.length()) {
                int newline = text.lastIndexOf('\n', target - 1);
                if (newline >= start) {
                    end = newline + 1;
                } else {
                    newline = text.indexOf('\n', target);
                    end = newline >= 0 && newline < start + HARD_MAX_CHUNK_CHARS
                            ? newline + 1
                            : Math.min(text.length(), start + HARD_MAX_CHUNK_CHARS);
                }
            }
            if (end <= start) {
                end = Math.min(text.length(), start + TARGET_CHUNK_CHARS);
            }
            result.add(new Chunk(start, end, text.substring(start, end)));
            start = end;
        }
        return result;
    }
}
