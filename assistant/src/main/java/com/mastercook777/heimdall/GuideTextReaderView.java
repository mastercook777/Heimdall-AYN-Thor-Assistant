package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.TabStopSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ListView;
import android.widget.TextView;

final class GuideTextReaderView extends FrameLayout {
    private static final int TAB_COLUMNS = 4;
    static final class Position {
        final int anchor;
        final int anchorTop;
        final int horizontalColumn;
        final int viewportWidth;

        Position(int anchor, int anchorTop, int horizontalColumn, int viewportWidth) {
            this.anchor = anchor;
            this.anchorTop = anchorTop;
            this.horizontalColumn = horizontalColumn;
            this.viewportWidth = viewportWidth;
        }
    }

    private final boolean originalLayout;
    private ListView list;
    private HorizontalScrollView horizontalScroll;
    private GuideTextDocument document;
    private float originalCharacterWidth;
    private int preservedHorizontalColumn;

    GuideTextReaderView(Context context, boolean originalLayout) {
        super(context);
        this.originalLayout = originalLayout;
        setBackground(HeimdallUi.isPearl(context)
                ? HeimdallUi.cncShallowInset(context, 8)
                : HeimdallUi.insetPanel(context, 8));
    }

    void showMessage(String message, int color) {
        removeAllViews();
        list = null;
        horizontalScroll = null;
        TextView view = new TextView(getContext());
        view.setText(message);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(16), dp(16), dp(16), dp(16));
        addView(view, new FrameLayout.LayoutParams(-1, -1));
    }

    void setDocument(GuideTextDocument document, Position restore) {
        this.document = document;
        preservedHorizontalColumn = restore == null ? 0 : restore.horizontalColumn;
        removeAllViews();
        list = buildList();
        if (originalLayout) {
            horizontalScroll = new HorizontalScrollView(getContext());
            horizontalScroll.setFillViewport(true);
            horizontalScroll.setHorizontalScrollBarEnabled(true);
            horizontalScroll.setScrollbarFadingEnabled(false);
            TextView measuring = createRowTextView();
            Paint paint = measuring.getPaint();
            originalCharacterWidth = Math.max(1f, paint.measureText("M"));
            int contentWidth = Math.max(getResources().getDisplayMetrics().widthPixels,
                    Math.round(document.maxVisualColumns() * originalCharacterWidth)
                            + dp(28));
            horizontalScroll.addView(list,
                    new HorizontalScrollView.LayoutParams(contentWidth, -1));
            addView(horizontalScroll, new FrameLayout.LayoutParams(-1, -1));
        } else {
            horizontalScroll = null;
            addView(list, new FrameLayout.LayoutParams(-1, -1));
        }
        if (restore != null) {
            restorePosition(restore);
        }
    }

    Position capturePosition() {
        if (document == null || list == null || list.getChildCount() == 0) {
            return new Position(0, 0, 0, getWidth());
        }
        int first = list.getFirstVisiblePosition();
        TextView row = rowTextAt(0);
        GuideTextDocument.Chunk chunk = document.chunks.get(
                Math.max(0, Math.min(document.chunks.size() - 1, first)));
        if (row == null || row.getLayout() == null || row.length() == 0) {
            return new Position(chunk.start, 0, horizontalColumn(), getWidth());
        }
        Layout layout = row.getLayout();
        int localY = Math.max(0, list.getPaddingTop() - row.getTop()
                - row.getTotalPaddingTop());
        int line = layout.getLineForVertical(localY);
        int anchor = chunk.start + layout.getLineStart(line);
        int anchorTop = row.getTop() + row.getTotalPaddingTop()
                + layout.getLineTop(line) - list.getPaddingTop();
        return new Position(anchor, anchorTop, horizontalColumn(), getWidth());
    }

    void scrollToPosition(Position position) {
        if (position == null || document == null || list == null) {
            return;
        }
        restorePosition(position);
    }

    private ListView buildList() {
        ListView result = new ListView(getContext());
        result.setAdapter(new ChunkAdapter());
        result.setDivider(null);
        result.setDividerHeight(0);
        result.setCacheColorHint(0x00000000);
        result.setClipToPadding(false);
        result.setPadding(dp(4), dp(4), dp(4), dp(12));
        result.setVerticalScrollBarEnabled(true);
        result.setScrollbarFadingEnabled(false);
        return result;
    }

    private void restorePosition(Position position) {
        int chunkIndex = document.chunkIndexForAnchor(position.anchor);
        list.setSelectionFromTop(chunkIndex, position.anchorTop);
        post(() -> {
            if (document == null || list == null) {
                return;
            }
            int visibleIndex = chunkIndex - list.getFirstVisiblePosition();
            TextView row = rowTextAt(visibleIndex);
            if (row != null && row.getLayout() != null && row.length() > 0) {
                GuideTextDocument.Chunk chunk = document.chunks.get(chunkIndex);
                int localAnchor = Math.max(0,
                        Math.min(row.length() - 1, position.anchor - chunk.start));
                int line = row.getLayout().getLineForOffset(localAnchor);
                int childTop = list.getPaddingTop() + position.anchorTop
                        - row.getTotalPaddingTop() - row.getLayout().getLineTop(line);
                list.setSelectionFromTop(chunkIndex, childTop);
            }
            if (horizontalScroll != null) {
                int x = Math.max(0,
                        Math.round(position.horizontalColumn * originalCharacterWidth));
                horizontalScroll.scrollTo(x, 0);
            }
        });
    }

    private int horizontalColumn() {
        if (horizontalScroll == null || originalCharacterWidth <= 0f) {
            return preservedHorizontalColumn;
        }
        preservedHorizontalColumn = Math.max(0,
                Math.round(horizontalScroll.getScrollX() / originalCharacterWidth));
        return preservedHorizontalColumn;
    }

    private TextView rowTextAt(int childIndex) {
        if (list == null || childIndex < 0 || childIndex >= list.getChildCount()) {
            return null;
        }
        View child = list.getChildAt(childIndex);
        return child instanceof TextView ? (TextView) child : null;
    }

    private TextView createRowTextView() {
        TextView row = new TextView(getContext());
        row.setTextColor(HeimdallUi.textColor(getContext()));
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, originalLayout ? 12 : 13);
        row.setGravity(Gravity.TOP | Gravity.START);
        row.setPadding(dp(10), 0, dp(10), 0);
        row.setIncludeFontPadding(true);
        row.setTextIsSelectable(true);
        if (originalLayout) {
            row.setTypeface(Typeface.MONOSPACE);
            row.setHorizontallyScrolling(true);
            row.setLineSpacing(0f, 1f);
        } else {
            row.setHorizontallyScrolling(false);
            row.setLineSpacing(dp(2), 1f);
        }
        return row;
    }

    private CharSequence displayText(String raw) {
        if (!originalLayout || raw.indexOf('\t') < 0) {
            return raw;
        }
        SpannableString value = new SpannableString(raw);
        float width = Math.max(1f, originalCharacterWidth);
        int step = Math.max(1, Math.round(width * TAB_COLUMNS));
        int maximum = Math.min(512, Math.max(1, document.maxVisualColumns()
                / TAB_COLUMNS + 2));
        for (int index = 1; index <= maximum; index++) {
            value.setSpan(new TabStopSpan.Standard(index * step), 0, value.length(),
                    SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class ChunkAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return document == null ? 0 : document.chunks.size();
        }

        @Override
        public Object getItem(int position) {
            return document.chunks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView row = convertView instanceof TextView
                    ? (TextView) convertView : createRowTextView();
            row.setText(displayText(document.chunks.get(position).text));
            return row;
        }
    }
}
