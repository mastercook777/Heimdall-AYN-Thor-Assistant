package com.mastercook777.heimdall;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/** A unified keypad chassis with independent physical-style keycaps. */
@SuppressLint({"ViewConstructor", "ClickableViewAccessibility"})
final class KeyboardPadView extends ViewGroup {
    interface Listener {
        void onPress(KeyboardPad.Key key);
        void onHoldStart(Object token, KeyboardPad.Key key);
        void onHoldEnd(Object token);
        void onEditRequested();
        void onKeyEditRequested(KeyboardPad.Key key);
        void onInteractionBlocked();
    }

    private static final int CHASSIS_HEADER_DP = 34;
    private static final float CHASSIS_VISUAL_HEADER_DP = 28f;
    private static final int CHASSIS_BOTTOM_INSET_DP = 7;
    private static final int BLUE_KEYCAP_VISUAL_INSET_DP = 1;
    private static final int KEY_GAP_DP = 3;

    private final KeyboardPad pad;
    private final boolean editMode;
    private final boolean interactionEnabled;
    private final Listener listener;
    private final ChassisMenuView menuView;
    private final List<KeycapView> keycaps = new ArrayList<>();
    private final Paint chassisWellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chassisWellStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    KeyboardPadView(Context context, KeyboardPad pad, boolean editMode,
            boolean interactionEnabled, Listener listener) {
        super(context);
        this.pad = pad == null ? KeyboardPad.defaultPad() : pad;
        this.editMode = editMode;
        this.interactionEnabled = interactionEnabled;
        this.listener = listener;
        setWillNotDraw(false);
        setClipChildren(false);
        setMotionEventSplittingEnabled(true);
        boolean pearl = HeimdallUi.isPearl(context);
        chassisWellPaint.setColor(pearl ? 0xFF454A50 : 0xF214171B);
        chassisWellStrokePaint.setStyle(Paint.Style.STROKE);
        chassisWellStrokePaint.setStrokeWidth(dp(1));
        chassisWellStrokePaint.setColor(pearl ? 0xFF737A81 : 0x665C6872);
        if (editMode) {
            setBackground(null);
        } else {
            setBackground(HeimdallUi.isPearl(context)
                    ? HeimdallUi.cncInputFrame(context, HeimdallUi.RADIUS_MODULE)
                    : HeimdallUi.glass(context, 0xD2141C27, 0xE80A0E14,
                            0x665F7C9A, 0x33344150, HeimdallUi.RADIUS_MODULE, 1));
        }
        menuView = new ChassisMenuView(context);
        menuView.setVisibility(editMode ? View.GONE : View.VISIBLE);
        addView(menuView);
        refresh();
    }

    void refresh() {
        for (KeycapView keycap : keycaps) {
            keycap.releaseHeldInput();
            removeView(keycap);
        }
        keycaps.clear();
        pad.sanitize();
        for (KeyboardPad.Key key : pad.keys) {
            KeycapView keycap = new KeycapView(getContext(), key);
            keycaps.add(keycap);
            addView(keycap);
        }
        requestLayout();
        invalidate();
    }

    void release() {
        menuView.cancelPendingEdit();
        for (KeycapView keycap : keycaps) {
            keycap.releaseHeldInput();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        release();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
        int contentPadding = dp(editMode ? 4 : 6);
        int contentTop = editMode ? contentPadding : dp(CHASSIS_HEADER_DP);
        if (editMode) {
            menuView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY));
        } else {
            menuView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(dp(CHASSIS_HEADER_DP), MeasureSpec.EXACTLY));
        }
        int contentWidth = Math.max(1, width - contentPadding * 2);
        int contentHeight = Math.max(1, height - contentTop - contentPadding);
        float cellWidth = contentWidth / (float) Math.max(1, pad.columns);
        float cellHeight = contentHeight / (float) Math.max(1, pad.rows);
        for (KeycapView keycap : keycaps) {
            KeyboardPad.Geometry geometry = keycap.key.geometry;
            int childWidth = Math.max(dp(20), Math.round(cellWidth * geometry.w) - dp(KEY_GAP_DP * 2));
            int childHeight = Math.max(dp(20), Math.round(cellHeight * geometry.h) - dp(KEY_GAP_DP * 2));
            keycap.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int padding = dp(editMode ? 4 : 6);
        if (editMode) {
            menuView.layout(0, 0, 0, 0);
        } else {
            menuView.layout(0, 0, width, menuView.getMeasuredHeight());
        }
        int contentTop = editMode ? padding : dp(CHASSIS_HEADER_DP);
        int contentWidth = Math.max(1, width - padding * 2);
        int contentHeight = Math.max(1, height - contentTop - padding);
        float cellWidth = contentWidth / (float) Math.max(1, pad.columns);
        float cellHeight = contentHeight / (float) Math.max(1, pad.rows);
        int gridMinTop = Integer.MAX_VALUE;
        int gridMaxBottom = Integer.MIN_VALUE;
        for (KeycapView keycap : keycaps) {
            KeyboardPad.Geometry geometry = keycap.key.geometry;
            int rawTop = contentTop + Math.round(cellHeight * geometry.y) + dp(KEY_GAP_DP);
            gridMinTop = Math.min(gridMinTop, rawTop);
            gridMaxBottom = Math.max(gridMaxBottom, rawTop + keycap.getMeasuredHeight());
        }
        int gridOffsetY = 0;
        if (gridMinTop != Integer.MAX_VALUE) {
            int gridHeight = Math.max(0, gridMaxBottom - gridMinTop);
            float visualContentTop = editMode ? padding : dp(CHASSIS_VISUAL_HEADER_DP);
            float visualContentBottom = height
                    - (editMode ? padding : dp(CHASSIS_BOTTOM_INSET_DP));
            int centeredTop = Math.round(visualContentTop
                    + (visualContentBottom - visualContentTop - gridHeight) / 2f);
            // The visible rail may shrink, but its full 34dp edit hit area remains reserved.
            int safeGridTop = Math.max(contentTop, centeredTop);
            gridOffsetY = safeGridTop - gridMinTop;
        }
        for (KeycapView keycap : keycaps) {
            KeyboardPad.Geometry geometry = keycap.key.geometry;
            int childLeft = padding + Math.round(cellWidth * geometry.x) + dp(KEY_GAP_DP);
            int childTop = contentTop + Math.round(cellHeight * geometry.y)
                    + dp(KEY_GAP_DP) + gridOffsetY;
            keycap.layout(childLeft, childTop,
                    childLeft + keycap.getMeasuredWidth(), childTop + keycap.getMeasuredHeight());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float editorInset = dp(1f);
        RectF well = editMode
                ? new RectF(editorInset, editorInset,
                        getWidth() - editorInset, getHeight() - editorInset)
                : new RectF(dp(CHASSIS_BOTTOM_INSET_DP), dp(CHASSIS_VISUAL_HEADER_DP),
                        getWidth() - dp(CHASSIS_BOTTOM_INSET_DP),
                        getHeight() - dp(CHASSIS_BOTTOM_INSET_DP));
        canvas.drawRoundRect(well, dp(10), dp(10), chassisWellPaint);
        RectF wellStroke = new RectF(well.left + dp(0.5f), well.top + dp(0.5f),
                well.right - dp(0.5f), well.bottom - dp(0.5f));
        canvas.drawRoundRect(wellStroke, dp(10), dp(10), chassisWellStrokePaint);
    }

    private final class KeycapView extends View {
        private final KeyboardPad.Key key;
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint reliefPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Drawable icon;
        private boolean iconTintable = true;
        private boolean pressedVisual;
        private boolean heldInput;

        KeycapView(Context context, KeyboardPad.Key key) {
            super(context);
            this.key = key;
            setClickable(true);
            setFocusable(true);
            setContentDescription(KeyboardKeyCatalog.bindingSummary(key.binding));
            loadIcon();
            applySurface(false);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                setPressedVisual(true);
                if (!interactionEnabled) {
                    listener.onInteractionBlocked();
                    return true;
                }
                if (editMode) {
                    return true;
                }
                if (key.isWhileHeld()) {
                    heldInput = true;
                    listener.onHoldStart(this, key);
                } else {
                    listener.onPress(key);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_MOVE) {
                boolean inside = event.getX() >= 0 && event.getY() >= 0
                        && event.getX() <= getWidth() && event.getY() <= getHeight();
                setPressedVisual(inside);
                if (!inside) {
                    releaseHeldInput();
                }
                return true;
            }
            if (action == MotionEvent.ACTION_UP) {
                boolean activateEditor = editMode && pressedVisual && interactionEnabled;
                releaseHeldInput();
                setPressedVisual(false);
                if (activateEditor) {
                    performClick();
                    listener.onKeyEditRequested(key);
                }
                return true;
            }
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_DOWN) {
                releaseHeldInput();
                setPressedVisual(false);
                return true;
            }
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        @Override
        protected void onDetachedFromWindow() {
            releaseHeldInput();
            animate().cancel();
            super.onDetachedFromWindow();
        }

        void releaseHeldInput() {
            if (!heldInput) return;
            heldInput = false;
            listener.onHoldEnd(this);
        }

        void setPressedVisual(boolean pressed) {
            if (pressedVisual == pressed) return;
            pressedVisual = pressed;
            applySurface(pressed);
            animate().cancel();
            if (ValueAnimator.areAnimatorsEnabled() && !DebugPerformanceDiagnostics.isFlatUi()) {
                animate().translationY(pressed ? dp(1) : 0f).setDuration(pressed ? 55L : 85L).start();
            } else {
                setTranslationY(pressed ? dp(1) : 0f);
            }
            invalidate();
        }

        private void applySurface(boolean pressed) {
            if (HeimdallUi.isPearl(getContext())) {
                setBackground(new PearlKeycapDrawable(getContext(), pressed));
                setElevation(pressed ? 0f : dp(1));
            } else {
                Drawable surface = HeimdallUi.glass(getContext(),
                        pressed ? 0xFF24364A : 0xFF2B2D30,
                        pressed ? 0xFF101A26 : 0xFF17191C,
                        pressed ? 0xFF70B7FF : 0x99717A82,
                        pressed ? 0x884EA1FF : 0x55323539, 7, pressed ? 2 : 1);
                setBackground(new InsetDrawable(surface, dp(BLUE_KEYCAP_VISUAL_INSET_DP)));
                setElevation(pressed ? 0f : dp(1));
            }
        }

        private void loadIcon() {
            if (key.display.iconKey.isEmpty()) {
                icon = null;
                return;
            }
            MacroIconRepository.MacroIconOption option =
                    MacroIconRepository.findByKey(getContext(), key.display.iconKey);
            if (option == null) {
                icon = null;
                return;
            }
            icon = option.load(getContext());
            if (icon != null) icon = icon.mutate();
            iconTintable = option.tintable;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;
            if (!HeimdallUi.isPearl(getContext())) {
                reliefPaint.setStrokeWidth(dp(1));
                reliefPaint.setColor(pressedVisual ? 0x5570B7FF : 0x557B848C);
                canvas.drawLine(dp(7), dp(3), width - dp(7), dp(3), reliefPaint);
                reliefPaint.setColor(0x88090B0D);
                canvas.drawLine(dp(7), height - dp(3), width - dp(7), height - dp(3), reliefPaint);
            }
            String customLabel = key.display.label;
            String label = customLabel.isEmpty() && (key.display.isEmpty() || icon == null)
                    ? KeyboardKeyCatalog.bindingSummary(key.binding) : customLabel;
            int textColor = HeimdallUi.textColor(getContext());
            if (icon != null) {
                int iconSize = Math.max(dp(16), Math.min(Math.min(width, height) / 2, dp(34)));
                int iconLeft = (width - iconSize) / 2;
                int iconTop = label.isEmpty() ? (height - iconSize) / 2 : Math.max(dp(5), height / 5);
                if (iconTintable) icon.setTint(textColor); else icon.clearColorFilter();
                icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
                icon.draw(canvas);
            }
            if (!label.isEmpty()) {
                textPaint.setColor(textColor);
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                textPaint.setTextSize(dp(label.length() > 10 ? 9 : 11));
                String fitted = fitText(label, textPaint, width - dp(10));
                Paint.FontMetrics metrics = textPaint.getFontMetrics();
                float centerY = icon == null ? height / 2f : height - dp(10);
                float baseline = centerY - (metrics.ascent + metrics.descent) / 2f;
                canvas.drawText(fitted, width / 2f, baseline, textPaint);
            }
        }
    }

    private final class ChassisMenuView extends View {
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Runnable triggerEdit = this::triggerEdit;
        private final Runnable showFirstProgressDot = () -> showProgressDots(1);
        private final Runnable showSecondProgressDot = () -> showProgressDots(2);
        private final int touchSlop;
        private boolean pending;
        private boolean triggered;
        private int progressDots;

        ChassisMenuView(Context context) {
            super(context);
            touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
            setClickable(true);
            setLongClickable(false);
            setContentDescription(context.getString(R.string.keyboard_pad_edit_control_description));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                cancelPendingEdit();
                triggered = false;
                if (!interactionEnabled) {
                    listener.onInteractionBlocked();
                }
                if (!editMode && interactionEnabled && event.getPointerCount() == 1) {
                    pending = true;
                    postDelayed(showFirstProgressDot,
                            MacroButtonView.EDIT_LONG_PRESS_TIMEOUT_MS / 3L);
                    postDelayed(showSecondProgressDot,
                            MacroButtonView.EDIT_LONG_PRESS_TIMEOUT_MS * 2L / 3L);
                    postDelayed(triggerEdit, MacroButtonView.EDIT_LONG_PRESS_TIMEOUT_MS);
                }
                setPressed(true);
                invalidate();
                return true;
            }
            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                cancelPendingEdit();
                setPressed(false);
                invalidate();
            } else if (action == MotionEvent.ACTION_MOVE) {
                float x = event.getX();
                float y = event.getY();
                if (event.getPointerCount() != 1 || x < -touchSlop || y < -touchSlop
                        || x > getWidth() + touchSlop || y > getHeight() + touchSlop) {
                    cancelPendingEdit();
                    setPressed(false);
                    invalidate();
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                cancelPendingEdit();
                setPressed(false);
                invalidate();
                if (triggered) {
                    triggered = false;
                }
            }
            return true;
        }

        void cancelPendingEdit() {
            removeCallbacks(showFirstProgressDot);
            removeCallbacks(showSecondProgressDot);
            removeCallbacks(triggerEdit);
            pending = false;
            progressDots = 0;
            invalidate();
        }

        private void showProgressDots(int count) {
            if (!pending || !isAttachedToWindow() || !isShown() || !isEnabled()) {
                cancelPendingEdit();
                return;
            }
            progressDots = Math.max(0, Math.min(3, count));
            invalidate();
        }

        private void triggerEdit() {
            if (!pending || !isAttachedToWindow() || !isShown() || !isEnabled()) {
                cancelPendingEdit();
                return;
            }
            pending = false;
            triggered = true;
            progressDots = 3;
            invalidate();
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            listener.onEditRequested();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            boolean pearl = HeimdallUi.isPearl(getContext());
            float outerRadius = dp(3);
            float innerRadius = dp(1.8f);
            float centerY = dp(CHASSIS_VISUAL_HEADER_DP) / 2f;
            float centerX = getWidth() / 2f;
            for (int index = 0; index < 3; index++) {
                float x = centerX + dp((index - 1) * 10);
                dotPaint.setColor(pearl ? 0x66717A82 : 0x88414A53);
                canvas.drawCircle(x, centerY, outerRadius, dotPaint);
                boolean lit = index < progressDots;
                dotPaint.setColor(lit
                        ? (pearl ? 0xFFE77F1F : 0xFF70B7FF)
                        : (pearl ? 0xFF555D65 : 0xFF202A33));
                canvas.drawCircle(x, centerY, innerRadius, dotPaint);
            }
        }
    }

    /** Pearl-only vertical lighting keeps every keycap optically aligned at every aspect ratio. */
    private static final class PearlKeycapDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final float density;
        private final boolean pressed;
        private int alpha = 255;

        PearlKeycapDrawable(Context context, boolean pressed) {
            density = context.getResources().getDisplayMetrics().density;
            this.pressed = pressed;
        }

        @Override
        public void draw(Canvas canvas) {
            rect.set(getBounds());
            if (rect.width() <= 0f || rect.height() <= 0f) return;
            rect.inset(px(0.5f), px(0.5f));
            float radius = px(7f);

            RectF shadow = new RectF(rect);
            shadow.inset(px(0.4f), px(0.4f));
            shadow.offset(0f, pressed ? px(0.35f) : px(1.15f));
            fill(canvas, shadow, radius, pressed ? 0x26000000 : 0x52000000);

            RectF shell = new RectF(rect);
            shell.inset(px(0.45f), px(0.45f));
            gradient(canvas, shell, radius - px(0.45f),
                    new int[]{0xFFF9FAF9, 0xFFBBC2C7, 0xFF717C85},
                    new float[]{0f, 0.52f, 1f});

            RectF rim = new RectF(shell);
            rim.inset(px(1.25f), px(1.25f));
            float rimRadius = Math.max(0f, radius - px(1.7f));
            gradient(canvas, rim, rimRadius,
                    new int[]{0xFFFFFFFF, 0xFFF2F3F2, 0xFFB8C0C5},
                    new float[]{0f, 0.58f, 1f});

            RectF face = new RectF(rim);
            face.inset(px(1.35f), px(1.35f));
            gradient(canvas, face, Math.max(0f, rimRadius - px(1.35f)),
                    new int[]{0xFFFBFAF8, 0xFFF5F4F1}, null);

            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(px(pressed ? 1.15f : 0.7f));
            paint.setColor(withAlpha(pressed ? 0xB8E77F1F : 0x8A717B84));
            canvas.drawRoundRect(shell, radius - px(0.45f), radius - px(0.45f), paint);
        }

        private void fill(Canvas canvas, RectF bounds, float radius, int color) {
            paint.setShader(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(withAlpha(color));
            canvas.drawRoundRect(bounds, Math.max(0f, radius), Math.max(0f, radius), paint);
        }

        private void gradient(Canvas canvas, RectF bounds, float radius,
                int[] colors, float[] positions) {
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(bounds.left, bounds.top, bounds.left, bounds.bottom,
                    withAlpha(colors), positions, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(bounds, Math.max(0f, radius), Math.max(0f, radius), paint);
            paint.setShader(null);
        }

        private int[] withAlpha(int[] colors) {
            int[] adjusted = new int[colors.length];
            for (int index = 0; index < colors.length; index++) {
                adjusted[index] = withAlpha(colors[index]);
            }
            return adjusted;
        }

        private int withAlpha(int color) {
            return Color.argb(Math.round(Color.alpha(color) * (alpha / 255f)),
                    Color.red(color), Color.green(color), Color.blue(color));
        }

        private float px(float value) {
            return value * density;
        }

        @Override
        public void setAlpha(int value) {
            alpha = value;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static String fitText(String text, Paint paint, int maxWidth) {
        if (paint.measureText(text) <= maxWidth) return text;
        String ellipsis = "...";
        int end = text.length();
        while (end > 1 && paint.measureText(text, 0, end)
                + paint.measureText(ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    private int dp(int value) {
        return HeimdallUi.dp(getContext(), value);
    }

    private float dp(float value) {
        return getResources().getDisplayMetrics().density * value;
    }
}
