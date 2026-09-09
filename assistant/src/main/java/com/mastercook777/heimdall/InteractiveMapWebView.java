package com.mastercook777.heimdall;

import android.content.Context;
import android.view.MotionEvent;
import android.webkit.WebView;

/** WebView that exposes real HTML text-field taps to Heimdall's focus lease. */
final class InteractiveMapWebView extends WebView {
    private Runnable onTextInputRequested;

    InteractiveMapWebView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    void setOnTextInputRequested(Runnable callback) {
        onTextInputRequested = callback;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = super.onTouchEvent(event);
        if (event != null && event.getActionMasked() == MotionEvent.ACTION_UP) {
            post(this::notifyIfTextFieldWasTapped);
        }
        return handled;
    }

    private void notifyIfTextFieldWasTapped() {
        HitTestResult result = getHitTestResult();
        if (result != null && result.getType() == HitTestResult.EDIT_TEXT_TYPE
                && onTextInputRequested != null) {
            onTextInputRequested.run();
        }
    }
}
