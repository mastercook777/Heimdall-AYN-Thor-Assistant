package com.mastercook777.heimdall;

import android.content.Context;

public interface InputBackend {
    String name();

    boolean isReady();

    void openSettings(Context context);

    void dispatchMacro(Context context, Macro macro, InputBridge.Callback callback);

    boolean dispatchTouchMove(Context context, int displayId, int width, int height,
            float dx, float dy, InputBridge.Callback callback);

    boolean startTouchpadDrag(Context context, int displayId, int width, int height,
            float anchorX, float anchorY, InputBridge.Callback callback);

    boolean moveTouchpadDrag(Context context, float dx, float dy, int strokeMs,
            InputBridge.Callback callback);

    boolean endTouchpadDrag(Context context, int strokeMs, InputBridge.Callback callback);

    boolean supportsMouseMode();

    boolean supportsRelativeMove();
}
