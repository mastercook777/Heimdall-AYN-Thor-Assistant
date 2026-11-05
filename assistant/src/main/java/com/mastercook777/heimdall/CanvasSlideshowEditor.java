package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.Collections;

/** Profile-owned Canvas collection draft. Only the Activity's Save callback persists it. */
@SuppressLint("ViewConstructor")
final class CanvasSlideshowEditor extends LinearLayout {
    interface Listener {
        void onAddDevice();
        void onAddGallery();
        void onEdit(int index, CanvasConfig config);
        void onSave(CanvasConfig config);
        void onCancel();
    }

    private final CanvasConfig draft;
    private final Listener listener;
    private final CanvasImageView preview;
    private final TextView summary;
    private final LinearLayout rows;
    private final Button intervalButton;
    private final Button transitionButton;
    private final Button saveButton;
    private final int frameWidth;
    private final int frameHeight;
    private int selected;
    private boolean released;

    CanvasSlideshowEditor(Context context, CanvasConfig config,
            int frameWidth, int frameHeight, Listener listener) {
        super(context);
        this.draft = config == null ? new CanvasConfig() : config.copy();
        this.draft.ensureSlides();
        this.listener = listener;
        this.frameWidth = Math.max(1, frameWidth);
        this.frameHeight = Math.max(1, frameHeight);
        setOrientation(VERTICAL);
        setPadding(dp(12), dp(10), dp(12), dp(10));
        setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncFlush(context, HeimdallUi.RADIUS_PANEL)
                : HeimdallUi.glassSurface(context, ThemeGlassColors.OVERLAY,
                        HeimdallUi.RADIUS_PANEL, 2));

        TextView title = label(R.string.canvas_slideshow_manage, 17, true);
        addView(title, new LayoutParams(-1, dp(36)));
        summary = label(R.string.canvas_slideshow_hint, 12, false);
        addView(summary, new LayoutParams(-1, dp(28)));

        FrameLayout previewFrame = new FrameLayout(context);
        previewFrame.setBackgroundColor(0xFF020407);
        preview = new CanvasImageView(context);
        preview.setInteractive(false);
        preview.setClipToOutline(true);
        preview.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                if (draft.isCircular()) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                } else {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(6));
                }
            }
        });
        int previewHeight = dp(120);
        int previewMaxWidth = Math.max(dp(48),
                getResources().getDisplayMetrics().widthPixels - dp(48));
        float ratio = draft.isCircular() ? 1f
                : frameWidth / (float) Math.max(1, frameHeight);
        int previewWidth = Math.min(previewMaxWidth,
                Math.max(1, Math.round(previewHeight * ratio)));
        int fittedHeight = Math.min(previewHeight,
                Math.max(1, Math.round(previewWidth / ratio)));
        FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                previewWidth, fittedHeight, Gravity.CENTER);
        previewFrame.addView(preview, imageParams);
        LayoutParams previewParams = new LayoutParams(-1, previewHeight);
        previewParams.setMargins(0, 0, 0, dp(6));
        addView(previewFrame, previewParams);

        LinearLayout addRow = horizontal();
        addView(addRow, new LayoutParams(-1, dp(44)));
        addButton(addRow, R.string.canvas_slideshow_add_gallery, listener::onAddGallery);
        addButton(addRow, R.string.canvas_slideshow_add_device, listener::onAddDevice);

        ScrollView scroll = new ScrollView(context);
        rows = new LinearLayout(context);
        rows.setOrientation(VERTICAL);
        scroll.addView(rows);
        LayoutParams scrollParams = new LayoutParams(-1, 0, 1f);
        scrollParams.setMargins(0, dp(6), 0, dp(6));
        addView(scroll, scrollParams);

        LinearLayout intervalRow = horizontal();
        LayoutParams intervalParams = new LayoutParams(-1, dp(44));
        intervalParams.setMargins(0, 0, 0, dp(6));
        addView(intervalRow, intervalParams);
        intervalButton = addButton(intervalRow, R.string.canvas_slideshow_interval,
                this::cycleInterval);
        transitionButton = addButton(intervalRow, R.string.canvas_slideshow_transition,
                this::cycleTransition);
        intervalButton.setTextSize(12);
        transitionButton.setTextSize(12);

        LinearLayout actions = horizontal();
        addView(actions, new LayoutParams(-1, dp(48)));
        addButton(actions, R.string.common_cancel, listener::onCancel);
        saveButton = addButton(actions, R.string.common_save, () -> {
            if (draft.slides.isEmpty()) return;
            draft.normalize();
            listener.onSave(draft.copy());
        });
        refresh();
    }

    boolean isFull() { return draft.slides.size() >= CanvasConfig.MAX_SLIDES; }

    boolean add(String assetId) {
        if (released || isFull()) return false;
        float zoom = 1f;
        File source = CanvasAssetStore.resolve(getContext(), assetId);
        if (source != null) {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);
            zoom = CanvasCompositionMath.coverZoom(frameWidth, frameHeight,
                    bounds.outWidth, bounds.outHeight);
        }
        draft.slides.add(new CanvasConfig.Slide(assetId, 0.5f, 0.5f, zoom));
        selected = draft.slides.size() - 1;
        refresh();
        return true;
    }

    void updateComposition(int index, CanvasConfig config) {
        if (released || index < 0 || index >= draft.slides.size() || config == null) return;
        CanvasConfig.Slide slide = draft.slides.get(index);
        if (!slide.assetId.equals(config.assetId)) return;
        slide.focusX = config.focusX;
        slide.focusY = config.focusY;
        slide.zoom = config.zoom;
        slide.normalize();
        refresh();
    }

    void release() {
        released = true;
        preview.setTag("");
        preview.setImageDrawable(null);
        rows.removeAllViews();
    }

    private void cycleInterval() {
        int seconds = draft.slideSeconds;
        draft.slideSeconds = seconds == 5 ? 10 : seconds == 10 ? 20
                : seconds == 20 ? 30 : 5;
        refresh();
    }

    private void cycleTransition() {
        switch (draft.slideTransition) {
            case CanvasConfig.TRANSITION_NONE:
                draft.slideTransition = CanvasConfig.TRANSITION_FADE;
                break;
            case CanvasConfig.TRANSITION_FADE:
                draft.slideTransition = CanvasConfig.TRANSITION_FROM_LEFT;
                break;
            case CanvasConfig.TRANSITION_FROM_LEFT:
                draft.slideTransition = CanvasConfig.TRANSITION_FROM_RIGHT;
                break;
            default:
                draft.slideTransition = CanvasConfig.TRANSITION_NONE;
                break;
        }
        refresh();
    }

    private void duplicate(int index) {
        if (released || isFull() || index < 0 || index >= draft.slides.size()) return;
        draft.slides.add(index + 1, draft.slides.get(index).copy());
        selected = index + 1;
        refresh();
    }

    private void refresh() {
        if (released) return;
        selected = Math.max(0, Math.min(selected, draft.slides.size() - 1));
        summary.setText(getResources().getString(R.string.canvas_slideshow_count,
                draft.slides.size(), CanvasConfig.MAX_SLIDES));
        saveButton.setEnabled(!draft.slides.isEmpty());
        intervalButton.setText(getResources().getString(
                R.string.canvas_slideshow_interval_value, draft.slideSeconds));
        int transitionLabel = R.string.canvas_slideshow_transition_none;
        if (CanvasConfig.TRANSITION_FADE.equals(draft.slideTransition)) {
            transitionLabel = R.string.canvas_slideshow_transition_fade;
        } else if (CanvasConfig.TRANSITION_FROM_LEFT.equals(draft.slideTransition)) {
            transitionLabel = R.string.canvas_slideshow_transition_left;
        } else if (CanvasConfig.TRANSITION_FROM_RIGHT.equals(draft.slideTransition)) {
            transitionLabel = R.string.canvas_slideshow_transition_right;
        }
        transitionButton.setText(transitionLabel);
        preview.setTag("");
        preview.setImageDrawable(null);
        if (!draft.slides.isEmpty()) {
            preview.setComposition(draft.slideConfig(selected), false);
            File file = CanvasAssetStore.resolve(getContext(),
                    draft.slides.get(selected).assetId);
            if (file != null) {
                GalleryThumbnailLoader.load(getContext(), Uri.fromFile(file), preview,
                        available -> { });
            }
        }
        rows.removeAllViews();
        for (int i = 0; i < draft.slides.size(); i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(VERTICAL);
            row.setPadding(dp(7), dp(3), dp(7), dp(5));
            row.setBackground(HeimdallUi.isFreyaFamily(getContext())
                    ? HeimdallUi.cncMenuPanel(getContext(), 8)
                    : HeimdallUi.insetPanel(getContext(), 8));
            LayoutParams params = new LayoutParams(-1, dp(80));
            params.setMargins(0, dp(3), 0, dp(3));
            rows.addView(row, params);
            TextView number = labelText(getResources().getString(
                    R.string.canvas_slideshow_slide_number, i + 1), 13, i == selected);
            row.setOnClickListener(view -> { selected = index; refresh(); });
            row.addView(number, new LayoutParams(-1, dp(25)));
            LinearLayout buttons = horizontal();
            row.addView(buttons, new LayoutParams(-1, dp(44)));
            addSmallButton(buttons, R.string.canvas_slideshow_move_up,
                    () -> move(index, -1)).setEnabled(i > 0);
            addSmallButton(buttons, R.string.canvas_slideshow_move_down,
                    () -> move(index, 1)).setEnabled(i < draft.slides.size() - 1);
            addSmallButton(buttons, R.string.canvas_slideshow_frame, () ->
                    listener.onEdit(index, draft.slideConfig(index)));
            addSmallButton(buttons, R.string.canvas_slideshow_duplicate_slide,
                    () -> duplicate(index)).setEnabled(!isFull());
            addSmallButton(buttons, R.string.canvas_slideshow_remove, () -> {
                draft.slides.remove(index);
                refresh();
            });
        }
    }

    private void move(int index, int delta) {
        int next = index + delta;
        if (next < 0 || next >= draft.slides.size()) return;
        Collections.swap(draft.slides, index, next);
        selected = next;
        refresh();
    }

    private LinearLayout horizontal() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private TextView label(int res, int size, boolean bold) {
        return labelText(getResources().getString(res), size, bold);
    }

    private TextView labelText(String value, int size, boolean bold) {
        TextView text = new TextView(getContext());
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(HeimdallUi.textColor(getContext()));
        text.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) text.setTypeface(text.getTypeface(), android.graphics.Typeface.BOLD);
        return text;
    }

    private Button addButton(LinearLayout row, int res, Runnable action) {
        Button button = new Button(getContext());
        button.setText(res);
        button.setAllCaps(false);
        button.setOnClickListener(view -> action.run());
        HeimdallUi.applySecondaryButton(getContext(), button);
        LayoutParams params = new LayoutParams(0, -1, 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        row.addView(button, params);
        return button;
    }

    private Button addSmallButton(LinearLayout row, int res, Runnable action) {
        Button button = new Button(getContext());
        button.setText(res);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setOnClickListener(view -> action.run());
        HeimdallUi.applySecondaryButton(getContext(), button);
        LayoutParams params = new LayoutParams(0, -1, 1f);
        params.setMargins(dp(2), 0, dp(2), 0);
        row.addView(button, params);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
