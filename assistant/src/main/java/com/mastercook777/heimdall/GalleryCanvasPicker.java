package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Gallery source chooser. It never changes a Canvas or Profile. */
@SuppressLint("ViewConstructor")
final class GalleryCanvasPicker extends LinearLayout {
    interface Listener {
        void onSelected(GalleryMediaGrid.Item item);
        void onClose();
    }

    private static final int MAPS = 0;
    private static final int SCREENSHOTS = 1;
    private static final int OTHER = 2;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private final String profileId;
    private final List<GalleryMediaGrid.Item> maps;
    private final List<GalleryMediaGrid.Item> other;
    private final GalleryMediaGrid grid;
    private final Button[] tabs = new Button[3];
    private int category = MAPS;
    private int loadGeneration;
    private boolean released;

    GalleryCanvasPicker(Context context, String profileId,
            List<GalleryMediaGrid.Item> maps, List<GalleryMediaGrid.Item> other,
            Listener listener) {
        super(context);
        this.profileId = profileId;
        this.maps = new ArrayList<>(maps);
        this.other = new ArrayList<>(other);
        setOrientation(VERTICAL);
        setPadding(dp(12), dp(10), dp(12), dp(12));
        setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncFlush(context, HeimdallUi.RADIUS_PANEL)
                : HeimdallUi.glassSurface(context, ThemeGlassColors.OVERLAY,
                        HeimdallUi.RADIUS_PANEL, 2));

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(context);
        title.setText(R.string.gallery_canvas_picker_title);
        title.setTextColor(HeimdallUi.textColor(context));
        title.setTextSize(HeimdallUi.TYPE_EDITOR_TITLE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LayoutParams(0, -1, 1));
        Button close = actionButton(context, R.string.common_close, listener::onClose);
        header.addView(close, new LayoutParams(dp(96), dp(44)));
        addView(header, new LayoutParams(-1, dp(50)));

        LinearLayout tabRow = new LinearLayout(context);
        tabRow.setPadding(dp(3), dp(3), dp(3), dp(3));
        tabRow.setBackground(HeimdallUi.isFreyaFamily(context)
                ? new ColorDrawable(Color.TRANSPARENT)
                : HeimdallUi.insetPanel(context, HeimdallUi.RADIUS_BUTTON));
        LayoutParams tabRowParams = new LayoutParams(-1, dp(50));
        tabRowParams.setMargins(0, dp(2), 0, dp(8));
        addView(tabRow, tabRowParams);

        grid = new GalleryMediaGrid(context, new GalleryMediaGrid.Listener() {
            @Override public void onOpen(GalleryMediaGrid.Item item) {
                listener.onSelected(item);
            }
            @Override public void onRemove(GalleryMediaGrid.Item item) { }
            @Override public void onUseForCanvas(GalleryMediaGrid.Item item) { }
        }, false);
        addView(grid, new LayoutParams(-1, 0, 1));

        int[] labels = {R.string.gallery_maps, R.string.gallery_screenshots,
                R.string.gallery_other};
        for (int i = 0; i < tabs.length; i++) {
            final int selectedCategory = i;
            tabs[i] = actionButton(context, labels[i], () -> select(selectedCategory));
            LayoutParams params = new LayoutParams(0, -1, 1);
            params.setMargins(dp(2), 0, dp(2), 0);
            tabRow.addView(tabs[i], params);
        }
        select(MAPS);
    }

    void release() {
        released = true;
        loadGeneration++;
    }

    @Override
    protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }

    private void select(int next) {
        if (released) return;
        category = next;
        int generation = ++loadGeneration;
        for (int i = 0; i < tabs.length; i++) {
            HeimdallUi.applySectionButton(getContext(), tabs[i], i == category);
        }
        if (category == MAPS) {
            grid.showItems(maps, getContext().getString(
                    R.string.gallery_canvas_picker_maps_empty));
        } else if (category == OTHER) {
            grid.showItems(other, getContext().getString(R.string.gallery_other_empty));
        } else {
            grid.showMessage(getContext().getString(R.string.gallery_loading));
            Context app = getContext().getApplicationContext();
            new Thread(() -> {
                List<GalleryScreenshotStore.Entry> entries = null;
                try (GalleryScreenshotStore store = new GalleryScreenshotStore(app)) {
                    entries = store.list(profileId);
                } catch (Exception ignored) { }
                List<GalleryScreenshotStore.Entry> result = entries;
                MAIN.post(() -> {
                    if (released || generation != loadGeneration
                            || category != SCREENSHOTS) return;
                    if (result == null) {
                        grid.showMessage(getContext().getString(R.string.gallery_load_failed));
                    } else {
                        List<GalleryMediaGrid.Item> items = new ArrayList<>();
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm",
                                Locale.getDefault());
                        for (GalleryScreenshotStore.Entry entry : result) {
                            items.add(new GalleryMediaGrid.Item(entry.uri,
                                    format.format(new Date(entry.createdAt)),
                                    getContext().getString(R.string.gallery_screenshot_detail),
                                    null, false));
                        }
                        grid.showItems(items, getContext().getString(
                                R.string.gallery_screenshots_empty));
                    }
                });
            }, "heimdall-canvas-gallery-screenshots").start();
        }
    }

    private Button actionButton(Context context, int label, Runnable action) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setOnClickListener(view -> action.run());
        HeimdallUi.applySecondaryButton(context, button);
        return button;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
