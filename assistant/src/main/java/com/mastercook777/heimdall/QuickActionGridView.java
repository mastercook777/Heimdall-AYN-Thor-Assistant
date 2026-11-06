package com.mastercook777.heimdall;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

/** Packs selected Quick Actions into rows without reserving disabled slots. */
final class QuickActionGridView extends ViewGroup {
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int columns = 1;
    private int rows = 1;

    QuickActionGridView(Context context) {
        super(context);
        setWillNotDraw(false);
        dividerPaint.setColor(HeimdallUi.quickActionDivider(context));
        dividerPaint.setStrokeWidth(HeimdallUi.dp(context, 1));
    }

    static int requiredRows(Context context, int width, int count) {
        if (count <= 0) return 0;
        int minWidth = HeimdallUi.dp(context, 48);
        int columns = Math.min(count, Math.max(1, width / Math.max(1, minWidth)));
        return (count + columns - 1) / columns;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int count = getChildCount();
        columns = Math.min(Math.max(1, count),
                Math.max(1, width / Math.max(1, HeimdallUi.dp(getContext(), 48))));
        rows = Math.max(1, (count + columns - 1) / columns);
        int height = resolveSize(rows * HeimdallUi.dp(getContext(), 56), heightMeasureSpec);
        for (int index = 0; index < count; index++) {
            int row = index / columns;
            int column = index % columns;
            int rowCount = Math.min(columns, count - row * columns);
            int left = Math.round(column * width / (float) rowCount);
            int right = Math.round((column + 1) * width / (float) rowCount);
            int top = Math.round(row * height / (float) rows);
            int bottom = Math.round((row + 1) * height / (float) rows);
            getChildAt(index).measure(
                    MeasureSpec.makeMeasureSpec(Math.max(1, right - left), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(Math.max(1, bottom - top), MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int count = getChildCount();
        for (int index = 0; index < count; index++) {
            int row = index / columns;
            int column = index % columns;
            int rowCount = Math.min(columns, count - row * columns);
            int childLeft = Math.round(column * width / (float) rowCount);
            int childRight = Math.round((column + 1) * width / (float) rowCount);
            int childTop = Math.round(row * height / (float) rows);
            int childBottom = Math.round((row + 1) * height / (float) rows);
            getChildAt(index).layout(childLeft, childTop, childRight, childBottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = getChildCount();
        for (int index = 0; index < count; index++) {
            View child = getChildAt(index);
            int column = index % columns;
            if (column > 0) {
                canvas.drawLine(child.getLeft(), child.getTop() + HeimdallUi.dp(getContext(), 7),
                        child.getLeft(), child.getBottom() - HeimdallUi.dp(getContext(), 7),
                        dividerPaint);
            }
            if (index / columns > 0 && column == 0) {
                canvas.drawLine(HeimdallUi.dp(getContext(), 7), child.getTop(),
                        getWidth() - HeimdallUi.dp(getContext(), 7), child.getTop(),
                        dividerPaint);
            }
        }
    }
}
