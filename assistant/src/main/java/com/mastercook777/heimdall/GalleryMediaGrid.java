package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Image-first Gallery surface. The Activity owns resource and save decisions. */
@SuppressLint("ViewConstructor")
final class GalleryMediaGrid extends FrameLayout {
    interface Listener {
        void onOpen(Item item);
        void onRemove(Item item);
        void onUseForCanvas(Item item);
    }

    static final class Item {
        final Uri uri;
        final String title;
        final String detail;
        final CanvasConfig canvas;
        final boolean removable;

        Item(Uri uri, String title, String detail, CanvasConfig canvas, boolean removable) {
            this.uri = uri;
            this.title = title;
            this.detail = detail;
            this.canvas = canvas;
            this.removable = removable;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final GridView grid;
    private final TextView empty;
    private final BaseAdapter adapter;
    private final Listener listener;
    private final boolean showCanvasAction;

    GalleryMediaGrid(Context context, Listener listener) {
        this(context, listener, true);
    }

    GalleryMediaGrid(Context context, Listener listener, boolean showCanvasAction) {
        super(context);
        this.listener = listener;
        this.showCanvasAction = showCanvasAction;
        grid = new GridView(context);
        grid.setNumColumns(2);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(dp(8));
        grid.setVerticalSpacing(dp(8));
        grid.setPadding(dp(6), dp(4), dp(6), dp(8));
        grid.setClipToPadding(false);
        grid.setVerticalScrollBarEnabled(false);
        grid.setSelector(android.R.color.transparent);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return items.size(); }
            @Override public Object getItem(int position) { return items.get(position); }
            @Override public long getItemId(int position) { return position; }
            @Override public View getView(int position, View old, ViewGroup parent) {
                View card = old == null ? createCard() : old;
                bindCard((CardHolder) card.getTag(), items.get(position));
                return card;
            }
        };
        grid.setAdapter(adapter);
        addView(grid, new LayoutParams(-1, -1));

        empty = new TextView(context);
        empty.setGravity(Gravity.CENTER);
        empty.setTextSize(13);
        empty.setTextColor(HeimdallUi.mutedTextColor(context));
        empty.setPadding(dp(32), dp(24), dp(32), dp(24));
        empty.setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncMenuPanel(context, 10)
                : HeimdallUi.insetPanel(context, 10));
        LayoutParams emptyParams = new LayoutParams(-1, dp(190), Gravity.CENTER);
        emptyParams.setMargins(dp(8), 0, dp(8), 0);
        addView(empty, emptyParams);
        showMessage(context.getString(R.string.gallery_loading));
    }

    void showMessage(String message) {
        items.clear();
        adapter.notifyDataSetChanged();
        empty.setText(message);
        empty.setVisibility(VISIBLE);
        grid.setVisibility(GONE);
    }

    void showItems(List<Item> incoming, String emptyMessage) {
        items.clear();
        if (incoming != null) items.addAll(incoming);
        adapter.notifyDataSetChanged();
        empty.setText(emptyMessage);
        empty.setVisibility(items.isEmpty() ? VISIBLE : GONE);
        grid.setVisibility(items.isEmpty() ? GONE : VISIBLE);
    }

    private static final class CardHolder {
        LinearLayout card;
        ImageView image;
        TextView previewState;
        TextView title;
        TextView detail;
        Button useForCanvas;
        ImageButton remove;
    }

    private View createCard() {
        Context context = getContext();
        CardHolder holder = new CardHolder();
        LinearLayout card = new LinearLayout(context);
        holder.card = card;
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(4), dp(4), dp(4), dp(4));
        card.setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncMenuControl(context, HeimdallUi.RADIUS_CARD, false, false)
                : HeimdallUi.glassSurface(context, ThemeGlassColors.LIST_CONTROL,
                        HeimdallUi.RADIUS_CARD, 1));
        card.setLayoutParams(new AbsListView.LayoutParams(-1, dp(210)));

        FrameLayout preview = new FrameLayout(context);
        preview.setBackgroundColor(0xFF05070A);
        ImageView image = new ImageView(context);
        holder.image = image;
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.addView(image, new LayoutParams(-1, -1));
        TextView previewState = new TextView(context);
        holder.previewState = previewState;
        previewState.setGravity(Gravity.CENTER);
        previewState.setTextSize(11);
        previewState.setTextColor(HeimdallUi.mutedTextColor(context));
        preview.addView(previewState, new LayoutParams(-1, -1));
        card.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout caption = new LinearLayout(context);
        caption.setGravity(Gravity.CENTER_VERTICAL);
        caption.setPadding(dp(7), 0, dp(2), 0);
        card.addView(caption, new LinearLayout.LayoutParams(-1, dp(48)));
        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        caption.addView(labels, new LinearLayout.LayoutParams(0, -1, 1));
        TextView title = new TextView(context);
        holder.title = title;
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setTextColor(HeimdallUi.textColor(context));
        title.setTextSize(12);
        labels.addView(title);
        TextView detail = new TextView(context);
        holder.detail = detail;
        detail.setSingleLine(true);
        detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
        detail.setTextColor(HeimdallUi.mutedTextColor(context));
        detail.setTextSize(10);
        labels.addView(detail);

        Button useForCanvas = new Button(context);
        holder.useForCanvas = useForCanvas;
        useForCanvas.setText(R.string.gallery_use_for_canvas_short);
        useForCanvas.setTextSize(HeimdallUi.TYPE_BUTTON_COMPACT);
        useForCanvas.setAllCaps(false);
        useForCanvas.setMinHeight(0);
        useForCanvas.setMinimumHeight(0);
        useForCanvas.setPadding(0, 0, 0, 0);
        HeimdallUi.applySecondaryButton(context, useForCanvas);
        useForCanvas.setContentDescription(context.getString(R.string.gallery_use_for_canvas));
        LinearLayout.LayoutParams useParams = new LinearLayout.LayoutParams(dp(68), dp(42));
        useParams.setMargins(dp(3), 0, 0, 0);
        caption.addView(useForCanvas, useParams);

        ImageButton remove = new ImageButton(context);
        holder.remove = remove;
        remove.setImageResource(R.drawable.ic_trash);
        remove.setColorFilter(HeimdallUi.mutedTextColor(context));
        remove.setBackgroundColor(Color.TRANSPARENT);
        remove.setContentDescription(context.getString(R.string.gallery_remove_reference));
        caption.addView(remove, new LinearLayout.LayoutParams(dp(44), dp(44)));
        card.setTag(holder);
        return card;
    }

    private void bindCard(CardHolder holder, Item item) {
        holder.title.setText(item.title);
        holder.detail.setText(item.detail);
        holder.remove.setVisibility(item.removable && showCanvasAction ? VISIBLE : GONE);
        holder.remove.setOnClickListener(item.removable && showCanvasAction
                ? view -> listener.onRemove(item) : null);
        holder.useForCanvas.setVisibility(showCanvasAction ? VISIBLE : GONE);
        holder.useForCanvas.setEnabled(item.uri != null);
        holder.useForCanvas.setAlpha(item.uri != null ? 1f : 0.45f);
        holder.useForCanvas.setOnClickListener(showCanvasAction
                ? view -> listener.onUseForCanvas(item) : null);
        holder.card.setOnClickListener(view -> listener.onOpen(item));
        holder.card.setContentDescription(item.title + ", " + item.detail);
        holder.previewState.setText(R.string.gallery_loading_preview);
        holder.previewState.setVisibility(VISIBLE);
        GalleryThumbnailLoader.load(getContext(), item.uri, holder.image, available -> {
            holder.previewState.setVisibility(available ? GONE : VISIBLE);
            if (!available) holder.previewState.setText(R.string.gallery_image_unavailable);
        });
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
