package com.mastercook777.heimdall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Paged search over decoded Guide text. Anchors use the reader's source offsets. */
final class GuideTextSearch {
    static final int PAGE_SIZE = 20;
    static final int MAX_QUERY_CHARS = 80;

    static final class Hit {
        final int anchor;
        final int horizontalColumn;
        final int length;
        final String snippet;

        Hit(int anchor, int horizontalColumn, int length, String snippet) {
            this.anchor = anchor;
            this.horizontalColumn = horizontalColumn;
            this.length = length;
            this.snippet = snippet;
        }
    }

    static final class Result {
        final int page;
        final int total;
        final List<Hit> hits;

        Result(int page, int total, List<Hit> hits) {
            this.page = page;
            this.total = total;
            this.hits = Collections.unmodifiableList(hits);
        }
    }

    private GuideTextSearch() { }

    static Result search(String source, String rawQuery, int page) {
        String text = source == null ? "" : source;
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() > MAX_QUERY_CHARS) {
            query = query.substring(0, MAX_QUERY_CHARS);
        }
        int safePage = Math.max(0, page);
        List<Hit> hits = new ArrayList<>(PAGE_SIZE);
        if (query.isEmpty() || query.length() > text.length()) {
            return new Result(safePage, 0, hits);
        }
        long first = (long) safePage * PAGE_SIZE;
        int total = 0;
        int lastStart = text.length() - query.length();
        for (int index = 0; index <= lastStart; index++) {
            if ((index & 4095) == 0 && Thread.currentThread().isInterrupted()) {
                return null;
            }
            if (!text.regionMatches(true, index, query, 0, query.length())) {
                continue;
            }
            if (total >= first && hits.size() < PAGE_SIZE) {
                hits.add(new Hit(index, visualColumn(text, index), query.length(),
                        snippet(text, index, query.length())));
            }
            total++;
        }
        return new Result(safePage, total, hits);
    }

    private static int visualColumn(String text, int anchor) {
        int start = text.lastIndexOf('\n', Math.max(-1, anchor - 1)) + 1;
        int column = 0;
        for (int index = start; index < anchor; index++) {
            if (text.charAt(index) == '\t') {
                column += 4 - column % 4;
            } else {
                column++;
            }
        }
        return column;
    }

    private static String snippet(String text, int anchor, int length) {
        int lineStart = text.lastIndexOf('\n', Math.max(-1, anchor - 1)) + 1;
        int newline = text.indexOf('\n', anchor);
        int lineEnd = newline < 0 ? text.length() : newline;
        int start = Math.max(lineStart, anchor - 36);
        int end = Math.min(lineEnd, anchor + length + 56);
        String value = text.substring(start, end).replace('\t', ' ').trim();
        if (start > lineStart) value = "..." + value;
        if (end < lineEnd) value += "...";
        return value;
    }
}
