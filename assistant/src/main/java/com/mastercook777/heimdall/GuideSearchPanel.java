package com.mastercook777.heimdall;

import android.content.Context;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Search UI for the current decoded Guide; it never changes Guide data. */
final class GuideSearchPanel extends LinearLayout {
    interface Listener {
        void onSearchRequested(Runnable action);
        void onJump(GuideTextSearch.Hit hit);
        void onClose();
    }

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "HeimdallGuideSearch");
        thread.setDaemon(true);
        return thread;
    });

    private final GuideTextDocument document;
    private final Listener listener;
    private final EditText query;
    private final TextView status;
    private final ScrollView resultScroll;
    private final LinearLayout results;
    private final Button previous;
    private final Button next;
    private Future<?> pending;
    private int generation;
    private int page;
    private String currentQuery = "";
    private boolean released;

    GuideSearchPanel(Context context, GuideTextDocument document, Listener listener) {
        super(context);
        this.document = document;
        this.listener = listener;
        setOrientation(VERTICAL);
        setFocusableInTouchMode(true);
        requestFocus();
        setPadding(dp(14), dp(12), dp(14), dp(12));
        setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncFlush(context, 12)
                : HeimdallUi.glassSurface(context, ThemeGlassColors.OPAQUE_CHROME, 12, 1));

        TextView title = label(R.string.guide_search_title, 15);
        title.setGravity(Gravity.CENTER_VERTICAL);
        addView(title, new LayoutParams(-1, dp(38)));

        LinearLayout inputRow = new LinearLayout(context);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        addView(inputRow, new LayoutParams(-1, dp(48)));
        query = new EditText(context);
        query.setSingleLine(true);
        query.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(GuideTextSearch.MAX_QUERY_CHARS)});
        query.setTextSize(13);
        query.setTextColor(HeimdallUi.textColor(context));
        query.setHintTextColor(HeimdallUi.mutedTextColor(context));
        query.setHint(R.string.guide_search_hint);
        query.setBackground(HeimdallUi.fieldPanel(context, 8));
        query.setPadding(dp(10), 0, dp(10), 0);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        query.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            requestSearch();
            return true;
        });
        inputRow.addView(query, new LayoutParams(0, dp(44), 1));
        Button search = button(R.string.guide_search_action, this::requestSearch);
        LayoutParams searchParams = new LayoutParams(dp(90), dp(44));
        searchParams.setMargins(dp(8), 0, 0, 0);
        inputRow.addView(search, searchParams);

        status = label(R.string.guide_search_prompt, 12);
        status.setTextColor(HeimdallUi.mutedTextColor(context));
        LayoutParams statusParams = new LayoutParams(-1, dp(42));
        statusParams.setMargins(0, dp(4), 0, dp(2));
        addView(status, statusParams);

        resultScroll = new ScrollView(context);
        resultScroll.setVerticalScrollBarEnabled(true);
        results = new LinearLayout(context);
        results.setOrientation(VERTICAL);
        resultScroll.addView(results, new ScrollView.LayoutParams(-1, -2));
        addView(resultScroll, new LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(context);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        LayoutParams footerParams = new LayoutParams(-1, dp(48));
        footerParams.setMargins(0, dp(8), 0, 0);
        addView(footer, footerParams);
        previous = button(R.string.guide_search_previous, () -> searchPage(page - 1));
        next = button(R.string.guide_search_next, () -> searchPage(page + 1));
        Button close = button(R.string.common_close, listener::onClose);
        footer.addView(previous, new LayoutParams(0, dp(44), 1));
        LayoutParams nextParams = new LayoutParams(0, dp(44), 1);
        nextParams.setMargins(dp(6), 0, dp(6), 0);
        footer.addView(next, nextParams);
        footer.addView(close, new LayoutParams(0, dp(44), 1));
        setPagingEnabled(false, false);
    }

    EditText queryInput() {
        return query;
    }

    private void requestSearch() {
        listener.onSearchRequested(() -> {
            if (released) return;
            currentQuery = query.getText().toString().trim();
            if (currentQuery.length() > GuideTextSearch.MAX_QUERY_CHARS) {
                currentQuery = currentQuery.substring(0, GuideTextSearch.MAX_QUERY_CHARS);
            }
            if (currentQuery.isEmpty()) {
                if (pending != null) pending.cancel(true);
                generation++;
                results.removeAllViews();
                status.setText(R.string.guide_search_prompt);
                setPagingEnabled(false, false);
                return;
            }
            searchPage(0);
        });
    }

    private void searchPage(int requestedPage) {
        if (released || currentQuery.isEmpty() || requestedPage < 0) return;
        if (pending != null) pending.cancel(true);
        int request = ++generation;
        String phrase = currentQuery;
        status.setText(R.string.guide_search_loading);
        setPagingEnabled(false, false);
        pending = WORKER.submit(() -> {
            GuideTextSearch.Result found = GuideTextSearch.search(
                    document.text, phrase, requestedPage);
            post(() -> {
                if (released || request != generation || found == null) return;
                showResult(found);
            });
        });
    }

    private void showResult(GuideTextSearch.Result found) {
        page = found.page;
        results.removeAllViews();
        resultScroll.scrollTo(0, 0);
        if (found.total == 0) {
            status.setText(R.string.guide_search_no_results);
            setPagingEnabled(false, false);
            return;
        }
        int first = page * GuideTextSearch.PAGE_SIZE + 1;
        int last = first + found.hits.size() - 1;
        status.setText(getContext().getString(R.string.guide_search_count,
                first, last, found.total));
        for (int index = 0; index < found.hits.size(); index++) {
            GuideTextSearch.Hit hit = found.hits.get(index);
            Button row = button(0, () -> listener.onJump(hit));
            row.setText((first + index) + "  " + hit.snippet);
            row.setSingleLine(false);
            row.setMaxLines(2);
            row.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            LayoutParams params = new LayoutParams(-1, dp(54));
            params.setMargins(0, 0, 0, dp(4));
            results.addView(row, params);
        }
        setPagingEnabled(page > 0, last < found.total);
    }

    private void setPagingEnabled(boolean hasPrevious, boolean hasNext) {
        previous.setEnabled(hasPrevious);
        previous.setAlpha(hasPrevious ? 1f : 0.45f);
        next.setEnabled(hasNext);
        next.setAlpha(hasNext ? 1f : 0.45f);
    }

    private TextView label(int text, int size) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(HeimdallUi.textColor(getContext()));
        return view;
    }

    private Button button(int text, Runnable action) {
        Button view = new Button(getContext());
        if (text != 0) view.setText(text);
        view.setAllCaps(false);
        view.setTextSize(12);
        view.setMinHeight(0);
        view.setMinimumHeight(0);
        HeimdallUi.applySecondaryButton(getContext(), view);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    @Override
    protected void onDetachedFromWindow() {
        released = true;
        generation++;
        if (pending != null) pending.cancel(true);
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
