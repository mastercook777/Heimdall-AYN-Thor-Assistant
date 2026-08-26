package com.mastercook777.heimdall;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

final class WidgetHostLayout extends ViewGroup {
    private final WidgetLayout layout;
    private final List<WidgetLayout.Item> childItems = new ArrayList<>();
    private final int gap;

    WidgetHostLayout(Context context, WidgetLayout layout) {
        super(context);
        this.layout = layout;
        gap = HeimdallUi.dp(context, 6);
    }

    void addWidget(View child, WidgetLayout.Item item) {
        childItems.add(item);
        addView(child, new ViewGroup.LayoutParams(0, 0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        float cellW = width / (float) Math.max(1, layout.columns);
        float cellH = height / (float) Math.max(1, layout.rows);
        for (int i = 0; i < getChildCount(); i++) {
            WidgetLayout.Item item = childItems.get(i);
            int childW = Math.max(1, Math.round(item.w * cellW) - gap * 2);
            int childH = Math.max(1, Math.round(item.h * cellH) - gap * 2);
            getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(childW, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childH, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        float cellW = width / (float) Math.max(1, layout.columns);
        float cellH = height / (float) Math.max(1, layout.rows);
        for (int i = 0; i < getChildCount(); i++) {
            WidgetLayout.Item item = childItems.get(i);
            int childLeft = Math.round(item.x * cellW) + gap;
            int childTop = Math.round(item.y * cellH) + gap;
            int childRight = Math.round((item.x + item.w) * cellW) - gap;
            int childBottom = Math.round((item.y + item.h) * cellH) - gap;
            getChildAt(i).layout(childLeft, childTop, childRight, childBottom);
        }
    }
}
