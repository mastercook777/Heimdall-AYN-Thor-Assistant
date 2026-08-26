package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

final class WidgetGridEditor extends View {
    static final int COLUMNS = 6;
    static final int ROWS = 8;

    interface Host {
        WidgetLayout currentLayout();
        WidgetLayout editableLayout();
        void replaceDraftLayout(WidgetLayout layout);
        void ensureMacroCapacity(int requiredCount);
        void showDebug(String message);
        void showError(String message);
    }

    private static final int MODE_NONE = 0;
    private static final int MODE_MOVE = 1;
    private static final int MODE_RESIZE = 2;

    private final Host host;
    private final Paint editorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF editorRect = new RectF();
    private int selectedIndex = -1;
    private int mode = MODE_NONE;
    private int originalX;
    private int originalY;
    private int originalW;
    private int originalH;
    private float downX;
    private float downY;
    private int grabCellX;
    private int grabCellY;

    WidgetGridEditor(Context context, Host host) {
        super(context);
        this.host = host;
        setBackground(HeimdallUi.insetPanel(context, 10));
        setContentDescription(context.getString(R.string.grid_editor_canvas_description));
    }

    void addWidget(String type) {
        WidgetLayout layout = host.editableLayout();
        if (WidgetLayout.TYPE_MAGNIFIER.equals(type)
                && layout.findItem(WidgetLayout.TYPE_MAGNIFIER) != null) {
            host.showError(getContext().getString(R.string.grid_editor_magnifier_limit));
            return;
        }
        int[] size = defaultWidgetSize(type);
        WidgetLayout.Item item = firstAvailableWidgetItem(layout, type, size[0], size[1]);
        if (item == null) {
            host.showError(getContext().getString(R.string.grid_editor_no_space_add));
            return;
        }
        configureNewWidgetItem(layout, item);
        layout.items.add(item);
        layout.preset = WidgetLayout.PRESET_CUSTOM;
        layout.sanitize();
        selectedIndex = layout.items.size() - 1;
        invalidate();
        host.showDebug(getContext().getString(R.string.grid_editor_added, widgetTypeLabel(type)));
    }

    void duplicateSelectedWidget() {
        WidgetLayout layout = host.editableLayout();
        if (selectedIndex < 0 || selectedIndex >= layout.items.size()) {
            host.showError(getContext().getString(R.string.grid_editor_select_copy));
            return;
        }
        WidgetLayout.Item source = layout.items.get(selectedIndex);
        if (!WidgetLayout.TYPE_MACRO_GROUP.equals(source.type)
                && !WidgetLayout.TYPE_KEYBOARD_PAD.equals(source.type)) {
            host.showError(getContext().getString(R.string.grid_editor_copy_macro_only));
            return;
        }
        WidgetLayout.Item copy = firstAvailableWidgetItem(
                layout, source.type, source.w, source.h);
        if (copy == null) {
            host.showError(getContext().getString(R.string.grid_editor_no_space_copy));
            return;
        }
        copy.macroStart = source.macroStart;
        copy.macroCount = source.macroCount;
        copy.macroColumns = source.macroColumns;
        copy.macroRows = source.macroRows;
        copy.macroRightHandPriority = source.macroRightHandPriority;
        copy.macroIconOnly = source.macroIconOnly;
        if (WidgetLayout.TYPE_KEYBOARD_PAD.equals(source.type)) {
            copy.keyboardPad = source.safeKeyboardPad().copy();
        }
        layout.items.add(copy);
        layout.preset = WidgetLayout.PRESET_CUSTOM;
        layout.sanitize();
        selectedIndex = layout.items.size() - 1;
        invalidate();
        host.showDebug(getContext().getString(R.string.grid_editor_copied));
    }

    void deleteSelectedWidget() {
        WidgetLayout layout = host.editableLayout();
        if (selectedIndex < 0 || selectedIndex >= layout.items.size()) {
            host.showError(getContext().getString(R.string.grid_editor_select_delete));
            return;
        }
        layout.items.remove(selectedIndex);
        layout.preset = WidgetLayout.PRESET_CUSTOM;
        layout.sanitize();
        selectedIndex = -1;
        invalidate();
        host.showDebug(getContext().getString(R.string.grid_editor_deleted));
    }

    void resetPreview() {
        host.replaceDraftLayout(WidgetLayout.defaultLayout());
        invalidate();
        host.showDebug(getContext().getString(R.string.grid_editor_reset_preview));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableW = Math.max(1, MeasureSpec.getSize(widthMeasureSpec));
        int availableH = Math.max(1, MeasureSpec.getSize(heightMeasureSpec));
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            setMeasuredDimension(availableW, availableH);
            return;
        }
        int targetH = Math.round(availableW * realGridAspectHeight());
        setMeasuredDimension(availableW, Math.min(availableH, targetH));
    }

    private float realGridAspectHeight() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float width = Math.max(1f, metrics.widthPixels - dp(8));
        float height = Math.max(1f, metrics.heightPixels
                - dp(8) - dp(44) - dp(6) - dp(50) - dp(6));
        return height / width;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        WidgetLayout layout = host.currentLayout();
        float cellW = getWidth() / (float) layout.columns;
        float cellH = getHeight() / (float) layout.rows;

        editorPaint.setStyle(Paint.Style.STROKE);
        editorPaint.setStrokeWidth(dp(1));
        editorPaint.setColor(HeimdallUi.isPearl(getContext()) ? 0x335D6975 : 0x334EA1FF);
        for (int col = 1; col < layout.columns; col++) {
            canvas.drawLine(col * cellW, 0, col * cellW, getHeight(), editorPaint);
        }
        for (int row = 1; row < layout.rows; row++) {
            canvas.drawLine(0, row * cellH, getWidth(), row * cellH, editorPaint);
        }

        editorPaint.setStrokeWidth(dp(2));
        editorPaint.setColor(HeimdallUi.isPearl(getContext()) ? 0x886D7B88 : 0x664EA1FF);
        canvas.drawRect(0, 0, getWidth(), getHeight(), editorPaint);
        if (layout.items.isEmpty()) {
            editorPaint.setStyle(Paint.Style.FILL);
            editorPaint.setTextAlign(Paint.Align.CENTER);
            editorPaint.setTextSize(dp(15));
            editorPaint.setColor(HeimdallUi.mutedTextColor(getContext()));
            canvas.drawText(getContext().getString(R.string.grid_editor_empty),
                    getWidth() * 0.5f, getHeight() * 0.5f, editorPaint);
            editorPaint.setTextAlign(Paint.Align.LEFT);
            return;
        }
        for (int i = 0; i < layout.items.size(); i++) {
            drawEditorItem(canvas, layout.items.get(i), i, cellW, cellH);
        }
    }

    private void drawEditorItem(Canvas canvas, WidgetLayout.Item item,
            int index, float cellW, float cellH) {
        float gap = dp(4);
        editorRect.set(item.x * cellW + gap, item.y * cellH + gap,
                (item.x + item.w) * cellW - gap, (item.y + item.h) * cellH - gap);
        boolean selected = index == selectedIndex;
        editorPaint.setStyle(Paint.Style.FILL);
        editorPaint.setColor(selected ? selectedWidgetColor(item.type) : widgetPreviewColor(item.type));
        canvas.drawRoundRect(editorRect, dp(10), dp(10), editorPaint);
        editorPaint.setStyle(Paint.Style.STROKE);
        editorPaint.setStrokeWidth(selected ? dp(3) : dp(1));
        editorPaint.setColor(selected ? HeimdallUi.accent(getContext())
                : (HeimdallUi.isPearl(getContext()) ? 0x886D7B88 : 0x884EA1FF));
        canvas.drawRoundRect(editorRect, dp(10), dp(10), editorPaint);
        editorPaint.setStyle(Paint.Style.FILL);
        editorPaint.setColor(HeimdallUi.textColor(getContext()));
        editorPaint.setTextSize(dp(15));
        editorPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(widgetTypeLabel(item.type), editorRect.left + dp(10),
                editorRect.top + dp(24), editorPaint);
        editorPaint.setColor(HeimdallUi.mutedTextColor(getContext()));
        editorPaint.setTextSize(dp(12));
        canvas.drawText(widgetEditorMeta(item), editorRect.left + dp(10),
                editorRect.top + dp(44), editorPaint);
        if (selected) {
            float handle = dp(24);
            editorPaint.setColor(HeimdallUi.isPearl(getContext()) ? 0xDDF08A2A : 0xDD4EA1FF);
            canvas.drawRoundRect(editorRect.right - handle, editorRect.bottom - handle,
                    editorRect.right, editorRect.bottom, dp(8), dp(8), editorPaint);
            editorPaint.setColor(0xFFFFFFFF);
            editorPaint.setStrokeWidth(dp(2));
            canvas.drawLine(editorRect.right - dp(7), editorRect.bottom - dp(18),
                    editorRect.right - dp(18), editorRect.bottom - dp(7), editorPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        WidgetLayout layout = host.editableLayout();
        if (layout.items.isEmpty()) {
            return true;
        }
        float cellW = getWidth() / (float) layout.columns;
        float cellH = getHeight() / (float) layout.rows;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                downX = event.getX();
                downY = event.getY();
                selectedIndex = hitTest(layout, downX, downY, cellW, cellH);
                if (selectedIndex < 0) {
                    mode = MODE_NONE;
                    invalidate();
                    return true;
                }
                WidgetLayout.Item item = layout.items.get(selectedIndex);
                originalX = item.x;
                originalY = item.y;
                originalW = item.w;
                originalH = item.h;
                grabCellX = Math.max(0, Math.min(item.w - 1,
                        (int) ((downX - item.x * cellW) / cellW)));
                grabCellY = Math.max(0, Math.min(item.h - 1,
                        (int) ((downY - item.y * cellH) / cellH)));
                mode = hitResizeHandle(item, downX, downY, cellW, cellH)
                        ? MODE_RESIZE : MODE_MOVE;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (selectedIndex >= 0 && selectedIndex < layout.items.size()) {
                    updateDraggedItem(layout, layout.items.get(selectedIndex),
                            event.getX(), event.getY(), cellW, cellH);
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                finishDrag(layout);
                mode = MODE_NONE;
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                restoreOriginal(layout);
                mode = MODE_NONE;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    private int hitTest(WidgetLayout layout, float x, float y, float cellW, float cellH) {
        for (int i = layout.items.size() - 1; i >= 0; i--) {
            WidgetLayout.Item item = layout.items.get(i);
            if (x >= item.x * cellW && x <= (item.x + item.w) * cellW
                    && y >= item.y * cellH && y <= (item.y + item.h) * cellH) {
                return i;
            }
        }
        return -1;
    }

    private boolean hitResizeHandle(WidgetLayout.Item item,
            float x, float y, float cellW, float cellH) {
        float right = (item.x + item.w) * cellW;
        float bottom = (item.y + item.h) * cellH;
        float handle = dp(34);
        return x >= right - handle && x <= right && y >= bottom - handle && y <= bottom;
    }

    private void updateDraggedItem(WidgetLayout layout, WidgetLayout.Item item,
            float x, float y, float cellW, float cellH) {
        if (mode == MODE_RESIZE) {
            int right = Math.max(item.x + 1,
                    Math.min(layout.columns, (int) Math.ceil(x / cellW)));
            int bottom = Math.max(item.y + 1,
                    Math.min(layout.rows, (int) Math.ceil(y / cellH)));
            item.w = Math.max(1, Math.min(layout.columns - item.x, right - item.x));
            item.h = Math.max(1, Math.min(layout.rows - item.y, bottom - item.y));
        } else if (mode == MODE_MOVE) {
            int nextX = Math.round(x / cellW) - grabCellX;
            int nextY = Math.round(y / cellH) - grabCellY;
            item.x = Math.max(0, Math.min(layout.columns - item.w, nextX));
            item.y = Math.max(0, Math.min(layout.rows - item.h, nextY));
        }
    }

    private void finishDrag(WidgetLayout layout) {
        if (selectedIndex < 0 || selectedIndex >= layout.items.size()) {
            return;
        }
        WidgetLayout.Item item = layout.items.get(selectedIndex);
        if (mode == MODE_RESIZE) {
            if (widgetOverlaps(layout, item, selectedIndex)) {
                restoreOriginal(layout);
                host.showError(getContext().getString(R.string.grid_editor_resize_overlap));
                return;
            }
            markCustom(layout);
            host.showDebug(getContext().getString(R.string.grid_editor_resized));
            return;
        }
        int overlap = singleOverlapIndex(layout, item, selectedIndex);
        if (overlap == -1) {
            markCustom(layout);
            host.showDebug(getContext().getString(R.string.grid_editor_moved));
        } else if (overlap >= 0 && trySwap(layout, selectedIndex, overlap)) {
            markCustom(layout);
            host.showDebug(getContext().getString(R.string.grid_editor_swapped));
        } else {
            restoreOriginal(layout);
            host.showError(getContext().getString(R.string.grid_editor_position_conflict));
        }
    }

    private void markCustom(WidgetLayout layout) {
        layout.preset = WidgetLayout.PRESET_CUSTOM;
        layout.sanitize();
        invalidate();
    }

    private boolean trySwap(WidgetLayout layout, int movingIndex, int targetIndex) {
        WidgetLayout.Item moving = layout.items.get(movingIndex);
        WidgetLayout.Item target = layout.items.get(targetIndex);
        WidgetLayout.Item movingAtTarget =
                new WidgetLayout.Item(moving.type, target.x, target.y, moving.w, moving.h);
        WidgetLayout.Item targetAtOriginal =
                new WidgetLayout.Item(target.type, originalX, originalY, target.w, target.h);
        clampWidgetItem(layout, movingAtTarget);
        clampWidgetItem(layout, targetAtOriginal);
        if (movingAtTarget.x != target.x || movingAtTarget.y != target.y
                || targetAtOriginal.x != originalX || targetAtOriginal.y != originalY) {
            return false;
        }
        if (!canPlaceWidget(layout, movingAtTarget, movingIndex, targetIndex)
                || !canPlaceWidget(layout, targetAtOriginal, movingIndex, targetIndex)) {
            return false;
        }
        moving.x = movingAtTarget.x;
        moving.y = movingAtTarget.y;
        target.x = targetAtOriginal.x;
        target.y = targetAtOriginal.y;
        return true;
    }

    private void restoreOriginal(WidgetLayout layout) {
        if (selectedIndex < 0 || selectedIndex >= layout.items.size()) {
            return;
        }
        WidgetLayout.Item item = layout.items.get(selectedIndex);
        item.x = originalX;
        item.y = originalY;
        item.w = originalW;
        item.h = originalH;
    }

    private void configureNewWidgetItem(WidgetLayout layout, WidgetLayout.Item item) {
        if (WidgetLayout.TYPE_KEYBOARD_PAD.equals(item.type)) {
            item.keyboardPad = KeyboardPad.defaultPad();
            return;
        }
        if (!WidgetLayout.TYPE_MACRO_GROUP.equals(item.type)) {
            return;
        }
        int start = nextMacroStart(layout);
        item.macroStart = start;
        item.macroCount = 4;
        item.macroColumns = 2;
        item.macroRows = 2;
        item.macroRightHandPriority = true;
        host.ensureMacroCapacity(start + item.macroCount);
    }

    private int nextMacroStart(WidgetLayout layout) {
        int next = 0;
        for (WidgetLayout.Item item : layout.items) {
            if (WidgetLayout.TYPE_MACRO_GROUP.equals(item.type)) {
                next = Math.max(next, item.macroStart + item.macroCount);
            }
        }
        return Math.max(0, Math.min(23, next));
    }

    private int singleOverlapIndex(WidgetLayout layout,
            WidgetLayout.Item candidate, int ignoreIndex) {
        int overlap = -1;
        for (int i = 0; i < layout.items.size(); i++) {
            if (i == ignoreIndex) {
                continue;
            }
            if (rectsOverlap(candidate, layout.items.get(i))) {
                if (overlap >= 0) {
                    return -2;
                }
                overlap = i;
            }
        }
        return overlap;
    }

    private boolean canPlaceWidget(WidgetLayout layout,
            WidgetLayout.Item candidate, int ignoreA, int ignoreB) {
        for (int i = 0; i < layout.items.size(); i++) {
            if (i != ignoreA && i != ignoreB
                    && rectsOverlap(candidate, layout.items.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean rectsOverlap(WidgetLayout.Item a, WidgetLayout.Item b) {
        return a.x < b.x + b.w && a.x + a.w > b.x
                && a.y < b.y + b.h && a.y + a.h > b.y;
    }

    private int[] defaultWidgetSize(String type) {
        if (WidgetLayout.TYPE_TOUCHPAD.equals(type)) return new int[]{3, 4};
        if (WidgetLayout.TYPE_MACRO_GROUP.equals(type)) return new int[]{2, 4};
        if (WidgetLayout.TYPE_KEYBOARD_PAD.equals(type)) return new int[]{3, 4};
        if (WidgetLayout.TYPE_QUICK_ACTIONS.equals(type)) return new int[]{2, 2};
        if (WidgetLayout.TYPE_MAGNIFIER.equals(type)) return new int[]{3, 3};
        if (WidgetLayout.TYPE_CANVAS.equals(type)) return new int[]{3, 3};
        return new int[]{2, 1};
    }

    private WidgetLayout.Item firstAvailableWidgetItem(WidgetLayout layout,
            String type, int width, int height) {
        for (int y = 0; y <= layout.rows - height; y++) {
            for (int x = 0; x <= layout.columns - width; x++) {
                WidgetLayout.Item candidate = new WidgetLayout.Item(type, x, y, width, height);
                if (!widgetOverlaps(layout, candidate, -1)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void clampWidgetItem(WidgetLayout layout, WidgetLayout.Item item) {
        item.x = Math.max(0, Math.min(layout.columns - 1, item.x));
        item.y = Math.max(0, Math.min(layout.rows - 1, item.y));
        item.w = Math.max(1, Math.min(layout.columns - item.x, item.w));
        item.h = Math.max(1, Math.min(layout.rows - item.y, item.h));
    }

    private boolean widgetOverlaps(WidgetLayout layout,
            WidgetLayout.Item candidate, int ignoreIndex) {
        for (int i = 0; i < layout.items.size(); i++) {
            if (i == ignoreIndex) continue;
            WidgetLayout.Item item = layout.items.get(i);
            boolean separated = candidate.x + candidate.w <= item.x
                    || item.x + item.w <= candidate.x
                    || candidate.y + candidate.h <= item.y
                    || item.y + item.h <= candidate.y;
            if (!separated) return true;
        }
        return false;
    }

    private int widgetPreviewColor(String type) {
        if (HeimdallUi.isPearl(getContext())) {
            if (WidgetLayout.TYPE_TOUCHPAD.equals(type)) return 0xFFD6DEE6;
            if (WidgetLayout.TYPE_MACRO_GROUP.equals(type)) return 0xFFE3DFE8;
            if (WidgetLayout.TYPE_KEYBOARD_PAD.equals(type)) return 0xFFD9DEE4;
            if (WidgetLayout.TYPE_QUICK_ACTIONS.equals(type)) return 0xFFDCE3E9;
            if (WidgetLayout.TYPE_MAGNIFIER.equals(type)) return 0xFFD6E1E3;
            if (WidgetLayout.TYPE_CANVAS.equals(type)) return 0xFFD4D9DE;
            return 0xFFDCE5DF;
        }
        if (WidgetLayout.TYPE_TOUCHPAD.equals(type)) return 0xFF15243A;
        if (WidgetLayout.TYPE_MACRO_GROUP.equals(type)) return 0xFF241D3A;
        if (WidgetLayout.TYPE_KEYBOARD_PAD.equals(type)) return 0xFF192331;
        if (WidgetLayout.TYPE_QUICK_ACTIONS.equals(type)) return 0xFF17263B;
        if (WidgetLayout.TYPE_MAGNIFIER.equals(type)) return 0xFF142A35;
        if (WidgetLayout.TYPE_CANVAS.equals(type)) return 0xFF101820;
        return 0xFF142A22;
    }

    private int selectedWidgetColor(String type) {
        if (HeimdallUi.isPearl(getContext())) return 0xFFE7E1D9;
        if (WidgetLayout.TYPE_TOUCHPAD.equals(type)) return 0xCC1F4D78;
        if (WidgetLayout.TYPE_MACRO_GROUP.equals(type)) return 0xCC4A3278;
        if (WidgetLayout.TYPE_KEYBOARD_PAD.equals(type)) return 0xCC2C4B68;
        if (WidgetLayout.TYPE_MAGNIFIER.equals(type)) return 0xCC20566A;
        if (WidgetLayout.TYPE_CANVAS.equals(type)) return 0xCC283B4C;
        return 0xCC1D5A3B;
    }

    private String widgetTypeLabel(String type) {
        if (WidgetLayout.TYPE_TOUCHPAD.equals(type)) return getContext().getString(R.string.grid_widget_touch);
        if (WidgetLayout.TYPE_MACRO_GROUP.equals(type)) return getContext().getString(R.string.grid_widget_macro);
        if (WidgetLayout.TYPE_KEYBOARD_PAD.equals(type)) return getContext().getString(R.string.grid_widget_keyboard_pad);
        if (WidgetLayout.TYPE_STATUS.equals(type)) return getContext().getString(R.string.grid_widget_status);
        if (WidgetLayout.TYPE_CANVAS.equals(type)) return getContext().getString(R.string.canvas_name);
        if (WidgetLayout.TYPE_QUICK_ACTIONS.equals(type)) return getContext().getString(R.string.grid_widget_quick_actions);
        if (WidgetLayout.TYPE_MAGNIFIER.equals(type)) return getContext().getString(R.string.grid_widget_magnifier);
        return getContext().getString(R.string.grid_widget_module);
    }

    private String widgetEditorMeta(WidgetLayout.Item item) {
        String size = item.w + " x " + item.h;
        if (WidgetLayout.TYPE_KEYBOARD_PAD.equals(item.type)) {
            return size + "  " + item.safeKeyboardPad().keys.size()
                    + " " + getContext().getString(R.string.keyboard_pad_keys_short);
        }
        if (!WidgetLayout.TYPE_MACRO_GROUP.equals(item.type)) return size;
        return size + "  M" + (item.macroStart + 1) + "-"
                + (item.macroStart + item.macroCount);
    }

    private int dp(int value) {
        return HeimdallUi.dp(getContext(), value);
    }
}
