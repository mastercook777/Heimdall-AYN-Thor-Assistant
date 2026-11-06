package com.mastercook777.heimdall;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Read-only directory for the current text revision. */
@android.annotation.SuppressLint("ViewConstructor")
final class GuideChapterPanel extends LinearLayout {
    interface Listener {
        void onJump(GuideChapterIndex.Chapter chapter);
        void onClose();
    }

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "HeimdallGuideChapters");
        thread.setDaemon(true);
        return thread;
    });

    private final TextView status;
    private final ChapterAdapter adapter;
    private Future<?> pending;
    private boolean released;

    GuideChapterPanel(Context context, GuideTextDocument document, boolean markdown,
            Listener listener) {
        super(context);
        setOrientation(VERTICAL);
        setFocusableInTouchMode(true);
        requestFocus();
        setPadding(dp(14), dp(12), dp(14), dp(12));
        setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncFlush(context, 12)
                : HeimdallUi.glassSurface(context, ThemeGlassColors.OPAQUE_CHROME, 12, 1));

        TextView title = new TextView(context);
        title.setText(R.string.guide_chapters_title);
        title.setTextSize(15);
        title.setTextColor(HeimdallUi.textColor(context));
        title.setGravity(Gravity.CENTER_VERTICAL);
        addView(title, new LayoutParams(-1, dp(38)));

        status = new TextView(context);
        status.setText(R.string.guide_chapters_loading);
        status.setTextSize(12);
        status.setTextColor(HeimdallUi.mutedTextColor(context));
        status.setGravity(Gravity.CENTER_VERTICAL);
        addView(status, new LayoutParams(-1, dp(38)));

        adapter = new ChapterAdapter(listener);
        ListView list = new ListView(context);
        list.setAdapter(adapter);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setVerticalScrollBarEnabled(true);
        addView(list, new LayoutParams(-1, 0, 1));

        Button close = new Button(context);
        close.setText(R.string.common_close);
        close.setAllCaps(false);
        close.setTextSize(12);
        close.setMinHeight(0);
        HeimdallUi.applySecondaryButton(context, close);
        close.setOnClickListener(view -> listener.onClose());
        LayoutParams closeParams = new LayoutParams(-1, dp(44));
        closeParams.setMargins(0, dp(8), 0, 0);
        addView(close, closeParams);

        pending = WORKER.submit(() -> {
            GuideChapterIndex.Result result = GuideChapterIndex.build(document, markdown);
            post(() -> {
                if (released) return;
                adapter.chapters = result.chapters;
                adapter.notifyDataSetChanged();
                if (result.chapters.isEmpty()) {
                    status.setText(R.string.guide_chapters_empty);
                } else if (result.truncated) {
                    status.setText(R.string.guide_chapters_limit);
                } else {
                    status.setText(context.getString(R.string.guide_chapters_count,
                            result.chapters.size()));
                }
            });
        });
    }

    private final class ChapterAdapter extends BaseAdapter {
        private final Listener listener;
        private List<GuideChapterIndex.Chapter> chapters = Collections.emptyList();

        ChapterAdapter(Listener listener) { this.listener = listener; }

        @Override public int getCount() { return chapters.size(); }
        @Override public Object getItem(int position) { return chapters.get(position); }
        @Override public long getItemId(int position) { return chapters.get(position).anchor; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            Button open;
            if (convertView instanceof LinearLayout) {
                row = (LinearLayout) convertView;
                open = (Button) row.getChildAt(0);
            } else {
                row = new LinearLayout(getContext());
                row.setGravity(Gravity.CENTER_VERTICAL);
                open = new Button(getContext());
                open.setAllCaps(false);
                open.setSingleLine(true);
                open.setEllipsize(android.text.TextUtils.TruncateAt.END);
                open.setTextSize(13);
                open.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                open.setMinHeight(0);
                HeimdallUi.applySecondaryButton(getContext(), open);
                row.addView(open, new LayoutParams(0, dp(48), 1));
            }
            GuideChapterIndex.Chapter chapter = chapters.get(position);
            open.setText(chapter.title);
            open.setPadding(dp(10 + Math.min(4, chapter.level - 1) * 12), 0, dp(10), 0);
            open.setOnClickListener(view -> listener.onJump(chapter));
            row.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, dp(52)));
            open.setContentDescription(getContext().getString(R.string.guide_chapters_open,
                    chapter.title));
            return row;
        }
    }

    @Override protected void onDetachedFromWindow() {
        released = true;
        if (pending != null) pending.cancel(true);
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
