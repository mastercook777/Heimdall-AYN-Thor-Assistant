package com.mastercook777.heimdall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

@SuppressLint("ViewConstructor")
final class TranslationWidgetView extends FrameLayout {
    interface Listener {
        void onEditRequested(WidgetLayout.Item item);
        void onRegionRequested(WidgetLayout.Item item);
        void onRunningChanged(WidgetLayout.Item item, boolean running);
        void onRuntimeSnapshot(WidgetLayout.Item item,
                TranslationRuntimeController.Snapshot snapshot);
    }

    private final WidgetLayout.Item item;
    private final TextView translationText;
    private final TextView statusText;
    private final ImageButton runtimeControl;
    private final TranslationRuntimeController controller;
    private final Listener listener;
    private final ScrollView translationScroll;
    private final int editTouchSlop;
    private final Runnable triggerEditLongPress = this::triggerEditLongPress;
    private boolean userPaused;
    private boolean lifecycleResumed;
    private boolean editLongPressPending;
    private boolean editLongPressTriggered;
    private float editDownX;
    private float editDownY;

    TranslationWidgetView(Context context, GameProfile profile, WidgetLayout.Item item,
            boolean initiallyPaused, TranslationRuntimeController.Snapshot initialSnapshot,
            Listener listener) {
        super(context);
        this.item = item;
        this.listener = listener;
        editTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        userPaused = initiallyPaused;
        setClickable(true);
        setLongClickable(true);
        setContentDescription(context.getString(
                R.string.translation_widget_content_description));

        FrameLayout displayFrame = new FrameLayout(context);
        displayFrame.setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncInputFrame(context, HeimdallUi.RADIUS_MODULE, false)
                : HeimdallUi.glassSurface(context, ThemeGlassColors.MEDIA_FRAME,
                        HeimdallUi.RADIUS_MODULE, HeimdallUi.STROKE_HAIRLINE));
        addView(displayFrame, new LayoutParams(-1, -1));

        FrameLayout viewport = new FrameLayout(context);
        viewport.setBackground(HeimdallUi.translationContentPanel(
                context, Math.round(HeimdallUi.mediaFrameInnerRadiusDp(
                        context, HeimdallUi.RADIUS_MODULE))));
        viewport.setClipToOutline(true);
        viewport.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float radius = HeimdallUi.mediaFrameInnerRadiusDp(
                        getContext(), HeimdallUi.RADIUS_MODULE);
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(radius));
            }
        });
        int frameInset = dp(HeimdallUi.mediaFrameContentInsetDp(context));
        FrameLayout.LayoutParams viewportParams = new FrameLayout.LayoutParams(-1, -1);
        viewportParams.setMargins(frameInset, frameInset, frameInset, frameInset);
        displayFrame.addView(viewport, viewportParams);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(dp(13), dp(8), dp(10), dp(8));
        viewport.addView(content, new FrameLayout.LayoutParams(-1, -1));

        TranslationConfig config = item.safeTranslation();
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(header, new LinearLayout.LayoutParams(-1, dp(32)));

        TextView directionText = new TextView(context);
        directionText.setText(context.getString(R.string.translation_direction,
                TranslationConfig.sourceCode(config.ocrScript),
                TranslationConfig.targetCode(config.targetLanguage)));
        directionText.setTextSize(HeimdallUi.TYPE_HELP);
        directionText.setTextColor(HeimdallUi.mutedTextColor(context));
        directionText.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(directionText, new LinearLayout.LayoutParams(0, -1, 1f));

        ImageButton regionControl = new ImageButton(context);
        regionControl.setPadding(dp(7), dp(7), dp(7), dp(7));
        regionControl.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        regionControl.setFocusable(true);
        regionControl.setImageResource(R.drawable.ic_region_select);
        regionControl.setImageTintList(ColorStateList.valueOf(
                HeimdallUi.mutedTextColor(context)));
        regionControl.setContentDescription(context.getString(
                R.string.translation_quick_select_region));
        regionControl.setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncShallowInset(context, HeimdallUi.RADIUS_BUTTON)
                : HeimdallUi.glassSurface(context, ThemeGlassColors.CONTROL,
                        HeimdallUi.RADIUS_BUTTON, HeimdallUi.STROKE_HAIRLINE));
        regionControl.setOnClickListener(view -> listener.onRegionRequested(item));
        LinearLayout.LayoutParams regionControlParams =
                new LinearLayout.LayoutParams(dp(32), dp(32));
        regionControlParams.rightMargin = dp(5);
        header.addView(regionControl, regionControlParams);

        runtimeControl = new ImageButton(context);
        runtimeControl.setPadding(dp(7), dp(7), dp(7), dp(7));
        runtimeControl.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        runtimeControl.setFocusable(true);
        runtimeControl.setBackground(HeimdallUi.isFreyaFamily(context)
                ? HeimdallUi.cncShallowInset(context, HeimdallUi.RADIUS_BUTTON)
                : HeimdallUi.glassSurface(context, ThemeGlassColors.CONTROL,
                        HeimdallUi.RADIUS_BUTTON, HeimdallUi.STROKE_HAIRLINE));
        runtimeControl.setOnClickListener(view -> toggleUserPaused());
        header.addView(runtimeControl, new LinearLayout.LayoutParams(dp(32), dp(32)));

        translationText = new TextView(context);
        translationText.setText(R.string.translation_waiting_text);
        translationText.setTextSize(HeimdallUi.TYPE_SECTION_TITLE);
        translationText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        translationText.setTextColor(HeimdallUi.translationTextColor(context));
        translationText.setGravity(Gravity.TOP | Gravity.START);
        translationText.setLineSpacing(dp(2), 1f);
        translationText.setPadding(0, dp(2), dp(4), dp(4));
        translationScroll = new ScrollView(context);
        translationScroll.setFillViewport(true);
        translationScroll.setVerticalScrollBarEnabled(true);
        translationScroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        translationScroll.addView(translationText,
                new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams translationParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        translationParams.topMargin = dp(2);
        content.addView(translationScroll, translationParams);

        statusText = new TextView(context);
        statusText.setTextSize(HeimdallUi.TYPE_META);
        statusText.setTextColor(HeimdallUi.mutedTextColor(context));
        statusText.setMaxLines(1);
        content.addView(statusText, new LinearLayout.LayoutParams(-1, -2));

        setLongClickable(false);
        controller = new TranslationRuntimeController(context, profile.safeProfileId(),
                item.itemId, config, initialSnapshot, this::render);
        updateRuntimeControl();
        if (userPaused) render(null, TranslationRuntimeController.Status.PAUSED);
    }

    void resume() {
        lifecycleResumed = true;
        if (!userPaused) controller.start();
    }

    void pause() {
        lifecycleResumed = false;
        controller.stop();
    }

    void release() {
        lifecycleResumed = false;
        cancelEditLongPress();
        listener.onRuntimeSnapshot(item, controller.snapshot());
        controller.release();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            cancelEditLongPress();
            editLongPressTriggered = false;
            editDownX = event.getX();
            editDownY = event.getY();
            if (event.getPointerCount() == 1) {
                editLongPressPending = true;
                postDelayed(triggerEditLongPress,
                        HeimdallInteraction.EDIT_LONG_PRESS_TIMEOUT_MS);
            }
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            cancelEditLongPress();
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (event.getPointerCount() != 1
                    || Math.abs(event.getX() - editDownX) > editTouchSlop
                    || Math.abs(event.getY() - editDownY) > editTouchSlop) {
                cancelEditLongPress();
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            boolean consume = editLongPressTriggered;
            cancelEditLongPress();
            editLongPressTriggered = false;
            if (consume) return true;
        }
        return super.dispatchTouchEvent(event);
    }

    private void toggleUserPaused() {
        userPaused = !userPaused;
        if (userPaused) {
            controller.stop();
            render(null, TranslationRuntimeController.Status.PAUSED);
        } else if (lifecycleResumed) {
            controller.start();
        }
        updateRuntimeControl();
        listener.onRunningChanged(item, !userPaused);
    }

    private void updateRuntimeControl() {
        runtimeControl.setImageResource(userPaused ? R.drawable.ic_power : R.drawable.ic_stop);
        runtimeControl.setImageTintList(ColorStateList.valueOf(userPaused
                ? HeimdallUi.accent(getContext()) : HeimdallUi.mutedTextColor(getContext())));
        runtimeControl.setContentDescription(getContext().getString(userPaused
                ? R.string.translation_start : R.string.translation_stop));
    }

    private void render(String translation, TranslationRuntimeController.Status status) {
        if (translation != null && translation.trim().length() > 0) {
            String nextTranslation = translation.trim();
            if (!nextTranslation.contentEquals(translationText.getText())) {
                translationText.setText(nextTranslation);
                translationScroll.post(() -> translationScroll.scrollTo(0, 0));
            }
        }
        statusText.setText(statusText(status));
        boolean error = status == TranslationRuntimeController.Status.API_ERROR
                || status == TranslationRuntimeController.Status.OCR_ERROR
                || status == TranslationRuntimeController.Status.ACCESSIBILITY_UNAVAILABLE
                || status == TranslationRuntimeController.Status.NETWORK_UNAVAILABLE
                || status == TranslationRuntimeController.Status.PROVIDER_UNCONFIGURED;
        statusText.setTextColor(error
                ? HeimdallUi.semanticColor(getContext(), HeimdallUi.SEMANTIC_ERROR)
                : HeimdallUi.mutedTextColor(getContext()));
    }

    private void triggerEditLongPress() {
        if (!editLongPressPending || !isAttachedToWindow() || !isShown() || !isEnabled()) {
            cancelEditLongPress();
            return;
        }
        editLongPressPending = false;
        editLongPressTriggered = true;
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now,
                MotionEvent.ACTION_CANCEL, 0f, 0f, 0);
        super.dispatchTouchEvent(cancel);
        cancel.recycle();
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        listener.onEditRequested(item);
    }

    private void cancelEditLongPress() {
        if (editLongPressPending) removeCallbacks(triggerEditLongPress);
        editLongPressPending = false;
    }

    private int statusText(TranslationRuntimeController.Status status) {
        switch (status) {
            case TRANSLATING: return R.string.translation_status_translating;
            case PAUSED: return R.string.translation_status_paused;
            case NO_TEXT: return R.string.translation_status_no_text;
            case ACCESSIBILITY_UNAVAILABLE: return R.string.translation_status_accessibility;
            case PROVIDER_UNCONFIGURED: return R.string.translation_status_provider;
            case NETWORK_UNAVAILABLE: return R.string.translation_status_network;
            case OCR_ERROR: return R.string.translation_status_ocr_error;
            case API_ERROR: return R.string.translation_status_api_error;
            case STARTING: return R.string.translation_status_starting;
            case SCANNING:
            default: return R.string.translation_status_scanning;
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
