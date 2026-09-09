package com.mastercook777.heimdall;

final class CanvasAnimationPolicy {
    private CanvasAnimationPolicy() {
    }

    static boolean canAssign(WidgetLayout layout, WidgetLayout.Item target, boolean animated) {
        if (!animated || layout == null) {
            return true;
        }
        for (WidgetLayout.Item item : layout.items) {
            if (item == null || item == target
                    || !WidgetLayout.TYPE_CANVAS.equals(item.type)
                    || item.canvasConfig == null) {
                continue;
            }
            if (item.canvasConfig.animated && item.canvasConfig.hasAsset()) {
                return false;
            }
        }
        return true;
    }
}
