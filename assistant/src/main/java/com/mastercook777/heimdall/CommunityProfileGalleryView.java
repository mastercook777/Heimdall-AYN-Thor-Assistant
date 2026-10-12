package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressLint("ViewConstructor")
final class CommunityProfileGalleryView extends LinearLayout {
    interface Listener {
        void onCloseRequested();
        void onImportRequested(CommunityProfileCatalog.Entry entry);
    }

    private final Listener listener;
    private final String catalogUrl;
    private final int currentVersionCode;
    private final LinearLayout body;
    private final TextView status;
    private final ImageButton refresh;
    private final List<CommunityProfileRepository.Request> previewRequests = new ArrayList<>();
    private CommunityProfileRepository.Request catalogRequest;
    private CommunityProfileCatalog catalog;
    private CommunityProfileCatalog.Entry selected;
    private ProgressBar downloadProgressBar;
    private int downloadProgress;
    private boolean downloading;
    private boolean busy;

    CommunityProfileGalleryView(Context context, String catalogUrl, int currentVersionCode,
            Listener listener) {
        super(context);
        this.listener = listener;
        this.catalogUrl = catalogUrl;
        this.currentVersionCode = currentVersionCode;
        setOrientation(VERTICAL);
        setPadding(dp(12), dp(10), dp(12), dp(10));
        setBackground(HeimdallUi.settingsContentPanel(context, 14));

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(HORIZONTAL);
        TextView title = label(R.string.community_profiles, HeimdallUi.TYPE_EDITOR_TITLE, true);
        header.addView(title, new LayoutParams(0, dp(40), 1f));
        refresh = headerIconButton(R.drawable.ic_refresh,
                R.string.common_refresh, this::refresh);
        header.addView(refresh, new LayoutParams(dp(44), dp(40)));
        Button close = closeButton(listener::onCloseRequested);
        LayoutParams closeParams = new LayoutParams(dp(44), dp(40));
        closeParams.setMargins(dp(6), 0, 0, 0);
        header.addView(close, closeParams);
        addView(header, new LayoutParams(-1, dp(44)));

        status = label(R.string.community_profiles_loading, HeimdallUi.TYPE_META, false);
        status.setTextColor(HeimdallUi.mutedTextColor(context));
        status.setSingleLine(true);
        status.setEllipsize(TextUtils.TruncateAt.END);
        LayoutParams statusParams = new LayoutParams(-1, dp(22));
        statusParams.setMargins(0, 0, 0, dp(4));
        addView(status, statusParams);

        body = new LinearLayout(context);
        body.setOrientation(HORIZONTAL);
        addView(body, new LayoutParams(-1, 0, 1f));
    }

    void start() {
        refresh();
    }

    void close() {
        cancelCatalog();
        cancelPreviews();
    }

    void setOperationState(boolean active, String message) {
        busy = active;
        downloading = false;
        downloadProgress = 0;
        refresh.setEnabled(!active);
        if (message != null && message.length() > 0) {
            status.setText(message);
        }
        render();
    }

    void startDownload(String message) {
        busy = true;
        downloading = true;
        downloadProgress = 0;
        refresh.setEnabled(false);
        if (message != null && message.length() > 0) {
            status.setText(message);
        }
        render();
    }

    void updateDownloadProgress(long downloadedBytes, long totalBytes) {
        if (!downloading) {
            return;
        }
        downloadProgress = progressForBytes(downloadedBytes, totalBytes);
        applyDownloadProgress();
    }

    void finishOperation() {
        busy = false;
        downloading = false;
        downloadProgress = 0;
        refresh.setEnabled(true);
        if (catalog != null) {
            status.setText(getResources().getQuantityString(
                    R.plurals.community_profile_count,
                    catalog.profiles.size(), catalog.profiles.size()));
        }
        render();
    }

    private void refresh() {
        if (busy) {
            return;
        }
        cancelCatalog();
        status.setText(R.string.community_profiles_loading);
        refresh.setEnabled(false);
        catalogRequest = CommunityProfileRepository.loadCatalogAsync(getContext(), catalogUrl,
                new CommunityProfileRepository.CatalogCallback() {
                    @Override
                    public void onCatalog(CommunityProfileCatalog loaded, boolean cached,
                            long fetchedAt) {
                        catalog = loaded;
                        refresh.setEnabled(!busy);
                        if (cached) {
                            String time = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                    DateFormat.SHORT).format(new Date(fetchedAt));
                            status.setText(getResources().getString(
                                    R.string.community_profiles_cached, time));
                        } else {
                            status.setText(getResources().getQuantityString(
                                    R.plurals.community_profile_count,
                                    loaded.profiles.size(), loaded.profiles.size()));
                        }
                        render();
                    }

                    @Override
                    public void onFailure(boolean cachedAvailable, String detail) {
                        refresh.setEnabled(!busy);
                        status.setText(cachedAvailable
                                ? R.string.community_profiles_refresh_failed_cached
                                : R.string.error_community_profiles_catalog);
                        if (!cachedAvailable) {
                            render();
                        }
                    }
                });
    }

    private void render() {
        cancelPreviews();
        downloadProgressBar = null;
        body.removeAllViews();
        if (selected != null && catalog != null && catalog.profiles.contains(selected)) {
            renderGallery(catalog.profiles, selected);
            return;
        }
        if (catalog == null) {
            TextView empty = label(R.string.community_profiles_loading,
                    HeimdallUi.TYPE_BODY, false);
            empty.setTextColor(HeimdallUi.mutedTextColor(getContext()));
            empty.setGravity(Gravity.CENTER);
            body.addView(empty, new LayoutParams(-1, -1));
            return;
        }
        if (catalog.profiles.isEmpty()) {
            TextView empty = label(R.string.community_profiles_empty,
                    HeimdallUi.TYPE_BODY, false);
            empty.setTextColor(HeimdallUi.mutedTextColor(getContext()));
            empty.setGravity(Gravity.CENTER);
            body.addView(empty, new LayoutParams(-1, -1));
            return;
        }
        selected = catalog.profiles.get(0);
        renderGallery(catalog.profiles, selected);
    }

    private void renderGallery(List<CommunityProfileCatalog.Entry> profiles,
            CommunityProfileCatalog.Entry activeEntry) {
        ScrollView navigation = new ScrollView(getContext());
        navigation.setFillViewport(false);
        navigation.setPadding(0, 0, dp(4), 0);
        navigation.setClipToPadding(false);

        LinearLayout list = new LinearLayout(getContext());
        list.setOrientation(VERTICAL);
        for (CommunityProfileCatalog.Entry entry : profiles) {
            list.addView(profileRow(entry, entry == activeEntry),
                    spaced(-1, dp(52), 0, 2));
        }
        navigation.addView(list, new ScrollView.LayoutParams(-1, -2));
        body.addView(navigation, new LayoutParams(dp(214), -1));

        View divider = new View(getContext());
        divider.setBackgroundColor(HeimdallUi.border(getContext()));
        LayoutParams dividerParams = new LayoutParams(dp(1), -1);
        dividerParams.setMargins(dp(3), dp(3), dp(8), dp(3));
        body.addView(divider, dividerParams);

        LinearLayout detailPanel = new LinearLayout(getContext());
        detailPanel.setOrientation(VERTICAL);
        detailPanel.setPadding(dp(2), 0, 0, 0);
        renderDetail(detailPanel, activeEntry);
        body.addView(detailPanel, new LayoutParams(0, -1, 1f));
    }

    private View profileRow(CommunityProfileCatalog.Entry entry, boolean active) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), dp(6), dp(3));
        row.setBackground(active
                ? (HeimdallUi.isFreyaFamily(getContext())
                        ? HeimdallUi.cncMenuControl(getContext(), 8, true, false)
                        : HeimdallUi.glassSurface(getContext(),
                                ThemeGlassColors.PROFILE_LIST_SELECTED, 8, 1))
                : HeimdallUi.rounded(getContext(), Color.TRANSPARENT,
                        Color.TRANSPARENT, 8, 0));
        row.setClickable(true);
        row.setFocusable(true);
        row.setSelected(active);
        row.setContentDescription(entry.name + ", " + entry.subtitle);
        row.setOnClickListener(view -> {
            if (!busy) {
                selected = entry;
                render();
            }
        });

        View indicator = new View(getContext());
        indicator.setBackgroundColor(active ? HeimdallUi.accent(getContext())
                : Color.TRANSPARENT);
        row.addView(indicator, new LayoutParams(dp(3), -1));

        View icon = profileIcon(entry);
        LayoutParams iconParams = new LayoutParams(dp(36), dp(36));
        iconParams.setMargins(dp(7), 0, dp(8), 0);
        row.addView(icon, iconParams);

        LinearLayout copy = new LinearLayout(getContext());
        copy.setOrientation(VERTICAL);
        TextView name = text(entry.name, HeimdallUi.TYPE_LABEL, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(name, new LayoutParams(-1, dp(25)));
        TextView subtitle = text(entry.subtitle, HeimdallUi.TYPE_META, false);
        subtitle.setTextColor(HeimdallUi.mutedTextColor(getContext()));
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(subtitle, new LayoutParams(-1, dp(18)));
        row.addView(copy, new LayoutParams(0, -1, 1f));
        return row;
    }

    private void renderDetail(LinearLayout detail, CommunityProfileCatalog.Entry entry) {
        TextView name = text(entry.name, HeimdallUi.TYPE_EDITOR_TITLE, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        detail.addView(name, new LayoutParams(-1, dp(32)));
        TextView meta = text(getResources().getString(R.string.community_profile_meta,
                entry.author, entry.subtitle), HeimdallUi.TYPE_LABEL, false);
        meta.setTextColor(HeimdallUi.mutedTextColor(getContext()));
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        detail.addView(meta, spaced(-1, dp(20), 0, 4));

        String minimum = entry.minVersionName.length() > 0
                ? entry.minVersionName : String.valueOf(entry.minVersionCode);
        boolean supported = entry.supportsVersion(currentVersionCode);

        Button download = button(R.string.community_profile_download_import,
                () -> listener.onImportRequested(entry));
        HeimdallUi.applyPrimaryActionButton(getContext(), download);
        download.setEnabled(supported && !busy);
        detail.addView(download, spaced(-1, dp(48), 0, 5));

        downloadProgressBar = new ProgressBar(getContext(), null,
                android.R.attr.progressBarStyleHorizontal);
        downloadProgressBar.setIndeterminate(false);
        downloadProgressBar.setMax(1000);
        downloadProgressBar.setProgressTintList(ColorStateList.valueOf(
                HeimdallUi.accent(getContext())));
        downloadProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(
                HeimdallUi.border(getContext())));
        downloadProgressBar.setVisibility(downloading ? View.VISIBLE : View.GONE);
        detail.addView(downloadProgressBar, spaced(-1, dp(8), 0, 5));
        applyDownloadProgress();

        String fileSize = getResources().getString(
                R.string.community_profile_file_size,
                Formatter.formatShortFileSize(getContext(), entry.size));
        String requirement = getResources().getString(
                R.string.community_profile_min_version, minimum);
        TextView downloadInfo = text(fileSize + "   •   " + requirement,
                HeimdallUi.TYPE_META, false);
        downloadInfo.setTextColor(HeimdallUi.mutedTextColor(getContext()));
        downloadInfo.setSingleLine(true);
        downloadInfo.setEllipsize(TextUtils.TruncateAt.END);
        detail.addView(downloadInfo, new LayoutParams(-1, dp(22)));
        if (!supported) {
            TextView warning = label(R.string.community_profile_incompatible,
                    HeimdallUi.TYPE_HELP, false);
            warning.setTextColor(HeimdallUi.semanticColor(getContext(),
                    HeimdallUi.SEMANTIC_WARNING));
            detail.addView(warning, spaced(-1, -2, 2, 6));
        }

        ScrollView contentScroll = new ScrollView(getContext());
        contentScroll.setFillViewport(true);
        contentScroll.setClipToPadding(false);
        contentScroll.setVerticalScrollBarEnabled(true);
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(VERTICAL);
        content.setPadding(0, dp(4), dp(3), dp(10));

        TextView description = text(entry.description, HeimdallUi.TYPE_BODY, false);
        description.setLineSpacing(0f, 1.12f);
        content.addView(description, spaced(-1, -2, 0, 8));

        if (entry.hasScreenshot()) {
            ImageView screenshot = screenshotView();
            screenshot.setContentDescription(entry.name);
            content.addView(screenshot, new LayoutParams(-1, -2));
            loadScreenshot(entry, screenshot, 720, 640);
        }

        contentScroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        detail.addView(contentScroll, new LayoutParams(-1, 0, 1f));
    }

    private void loadIcon(CommunityProfileCatalog.Entry entry, ImageView view,
            int width, int height) {
        loadImage(entry, view, width, height, false);
    }

    private void loadScreenshot(CommunityProfileCatalog.Entry entry, ImageView view,
            int width, int height) {
        loadImage(entry, view, width, height, true);
    }

    private void loadImage(CommunityProfileCatalog.Entry entry, ImageView view,
            int width, int height, boolean screenshot) {
        final CommunityProfileRepository.Request[] holder = new CommunityProfileRepository.Request[1];
        CommunityProfileRepository.PreviewCallback callback =
                new CommunityProfileRepository.PreviewCallback() {
                    @Override
                    public void onLoaded(Bitmap bitmap) {
                        previewRequests.remove(holder[0]);
                        if (view.getParent() != null) {
                            view.setImageBitmap(bitmap);
                        } else {
                            bitmap.recycle();
                        }
                    }

                    @Override
                    public void onFailure() {
                        previewRequests.remove(holder[0]);
                        view.setContentDescription(getResources().getString(
                                R.string.community_profile_preview_unavailable));
                    }
                };
        holder[0] = screenshot
                ? CommunityProfileRepository.loadScreenshotAsync(getContext(), entry,
                        width, height, callback)
                : CommunityProfileRepository.loadIconAsync(getContext(), entry,
                        width, height, callback);
        previewRequests.add(holder[0]);
    }

    private View profileIcon(CommunityProfileCatalog.Entry entry) {
        FrameLayout frame = new FrameLayout(getContext());
        frame.setBackground(HeimdallUi.fieldPanel(getContext(), 7));
        frame.setClipToOutline(true);

        String fallbackText = entry.name.length() == 0 ? "H" : entry.name.substring(0, 1);
        TextView fallback = text(fallbackText, HeimdallUi.TYPE_LABEL, true);
        fallback.setGravity(Gravity.CENTER);
        fallback.setTextColor(HeimdallUi.mutedTextColor(getContext()));
        frame.addView(fallback, new FrameLayout.LayoutParams(-1, -1));

        if (entry.hasIcon()) {
            ImageView image = new ImageView(getContext());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setContentDescription(entry.name);
            frame.addView(image, new FrameLayout.LayoutParams(-1, -1));
            loadIcon(entry, image, 96, 96);
        }
        return frame;
    }

    private ImageView screenshotView() {
        ImageView view = new ImageView(getContext());
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setAdjustViewBounds(true);
        view.setMinimumHeight(dp(140));
        view.setBackground(HeimdallUi.fieldPanel(getContext(), 8));
        view.setContentDescription(getResources().getString(
                R.string.community_profile_preview));
        return view;
    }

    private ImageButton headerIconButton(int iconRes, int descriptionRes, Runnable action) {
        ImageButton button = new ImageButton(getContext());
        button.setImageResource(iconRes);
        button.setColorFilter(HeimdallUi.textColor(getContext()));
        button.setContentDescription(getResources().getString(descriptionRes));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setBackground(HeimdallUi.fullscreenToolbarControl(
                getContext(), HeimdallUi.RADIUS_BUTTON));
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private Button closeButton(Runnable action) {
        Button button = new Button(getContext());
        button.setText("\u00d7");
        button.setTextSize(HeimdallUi.TYPE_BUTTON);
        button.setTextColor(HeimdallUi.textColor(getContext()));
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(false);
        button.setContentDescription(getResources().getString(R.string.common_close));
        button.setPadding(0, 0, 0, 0);
        HeimdallUi.applySecondaryButton(getContext(), button);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private Button button(int textRes, Runnable action) {
        Button button = new Button(getContext());
        button.setAllCaps(false);
        button.setText(textRes);
        button.setTextSize(HeimdallUi.TYPE_BUTTON);
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(false);
        button.setMinimumHeight(0);
        button.setMinHeight(0);
        button.setPadding(dp(10), 0, dp(10), 0);
        HeimdallUi.applySecondaryButton(getContext(), button);
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private TextView label(int textRes, int size, boolean bold) {
        return text(getResources().getString(textRes), size, bold);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(HeimdallUi.textColor(getContext()));
        view.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private LayoutParams spaced(int width, int height, int top, int bottom) {
        LayoutParams params = new LayoutParams(width, height);
        params.setMargins(0, dp(top), 0, dp(bottom));
        return params;
    }

    private void cancelCatalog() {
        if (catalogRequest != null) {
            catalogRequest.cancel();
            catalogRequest = null;
        }
    }

    private void cancelPreviews() {
        for (CommunityProfileRepository.Request request : previewRequests) {
            request.cancel();
        }
        previewRequests.clear();
    }

    private void applyDownloadProgress() {
        if (downloadProgressBar == null || !downloading) {
            return;
        }
        downloadProgressBar.setProgress(downloadProgress);
        int percent = Math.round(downloadProgress / 10f);
        downloadProgressBar.setContentDescription(getResources().getString(
                R.string.community_profile_download_progress, percent));
    }

    static int progressForBytes(long downloadedBytes, long totalBytes) {
        if (totalBytes <= 0L || downloadedBytes <= 0L) {
            return 0;
        }
        if (downloadedBytes >= totalBytes) {
            return 1000;
        }
        return Math.max(0, Math.min(1000,
                (int) Math.round(downloadedBytes * 1000d / totalBytes)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
