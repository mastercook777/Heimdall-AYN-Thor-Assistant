package com.mastercook777.heimdall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;

public final class CoordinateCaptureActivity extends Activity {
    public static final String ACTION_CAPTURED =
            BuildConfig.APPLICATION_ID + ".ACTION_COORDINATE_CAPTURED";
    public static final String ACTION_REGION_CAPTURED =
            BuildConfig.APPLICATION_ID + ".ACTION_REGION_CAPTURED";
    public static final String ACTION_REGION_PREVIEW =
            BuildConfig.APPLICATION_ID + ".ACTION_REGION_PREVIEW";
    public static final String ACTION_REGION_CANCELLED =
            BuildConfig.APPLICATION_ID + ".ACTION_REGION_CANCELLED";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_STEP = "step";
    public static final String EXTRA_REGION_LEFT = "region_left";
    public static final String EXTRA_REGION_TOP = "region_top";
    public static final String EXTRA_REGION_RIGHT = "region_right";
    public static final String EXTRA_REGION_BOTTOM = "region_bottom";
    public static final String EXTRA_DISPLAY_ID = "display_id";
    public static final String EXTRA_TARGET_ASPECT = "target_aspect";
    public static final String MODE_TAP = "tap";
    public static final String MODE_HOLD = "hold";
    public static final String MODE_SWIPE = "swipe";
    public static final String MODE_REGION = "region";

    private static final int BG = 0x2205070A;
    private static final int TEXT = 0xFFE6EDF3;
    private static final int PRIMARY = 0xFF58A6FF;
    private static final int MUTED = 0xFF8B949E;

    private boolean regionResultSent;

    public static Intent createIntent(Context context, String mode) {
        Intent intent = new Intent(context, CoordinateCaptureActivity.class);
        intent.putExtra(EXTRA_MODE, mode);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().setDimAmount(0f);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(new CaptureView(this, getIntent().getStringExtra(EXTRA_MODE)));
    }

    private void finishWithStep(String step) {
        Intent result = new Intent(ACTION_CAPTURED);
        result.setPackage(getPackageName());
        result.putExtra(EXTRA_STEP, step);
        sendBroadcast(result);
        finishAndRemoveTask();
    }

    private void sendRegion(String action, int displayId, float left, float top,
            float right, float bottom) {
        Intent result = new Intent(action);
        result.setPackage(getPackageName());
        result.putExtra(EXTRA_DISPLAY_ID, displayId);
        result.putExtra(EXTRA_REGION_LEFT, left);
        result.putExtra(EXTRA_REGION_TOP, top);
        result.putExtra(EXTRA_REGION_RIGHT, right);
        result.putExtra(EXTRA_REGION_BOTTOM, bottom);
        result.putExtra(EXTRA_TARGET_ASPECT,
                Math.max(0.2f, Math.min(5f,
                        getIntent().getFloatExtra(EXTRA_TARGET_ASPECT, 1f))));
        sendBroadcast(result);
    }

    private void finishWithRegion(int displayId, float left, float top, float right, float bottom) {
        regionResultSent = true;
        sendRegion(ACTION_REGION_CAPTURED, displayId, left, top, right, bottom);
        finishAndRemoveTask();
    }

    private void cancelRegionCapture() {
        if (!regionResultSent) {
            regionResultSent = true;
            Intent result = new Intent(ACTION_REGION_CANCELLED);
            result.setPackage(getPackageName());
            sendBroadcast(result);
        }
        finishAndRemoveTask();
    }

    @Override
    public void onBackPressed() {
        if (MODE_REGION.equals(getIntent().getStringExtra(EXTRA_MODE))) {
            cancelRegionCapture();
        } else {
            finishAndRemoveTask();
        }
    }

    @Override
    protected void onDestroy() {
        if (MODE_REGION.equals(getIntent().getStringExtra(EXTRA_MODE)) && !regionResultSent) {
            Intent result = new Intent(ACTION_REGION_CANCELLED);
            result.setPackage(getPackageName());
            sendBroadcast(result);
            regionResultSent = true;
        }
        super.onDestroy();
    }

    private final class CaptureView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String mode;
        private final int displayId;
        private float downX = -1;
        private float downY = -1;
        private float currentX = -1;
        private float currentY = -1;
        private long downTime;
        private boolean selectionTooSmall;
        private final float targetAspectRatio;
        private final RectF selectedRegion = new RectF();
        private final RectF cancelButton = new RectF();
        private final RectF confirmButton = new RectF();
        private boolean adjustingRegion;
        private boolean movingRegion;
        private int resizingCorner = -1;
        private float moveOffsetX;
        private float moveOffsetY;
        private long lastPreviewAtMs;

        CaptureView(Context context, String mode) {
            super(context);
            if (MODE_REGION.equals(mode)) {
                this.mode = MODE_REGION;
            } else if (MODE_SWIPE.equals(mode)) {
                this.mode = MODE_SWIPE;
            } else if (MODE_HOLD.equals(mode)) {
                this.mode = MODE_HOLD;
            } else {
                this.mode = MODE_TAP;
            }
            Display display = getWindowManager().getDefaultDisplay();
            displayId = display == null ? Display.DEFAULT_DISPLAY : display.getDisplayId();
            targetAspectRatio = Math.max(0.2f, Math.min(5f,
                    getIntent().getFloatExtra(EXTRA_TARGET_ASPECT, 1f)));
            setBackgroundColor(Color.TRANSPARENT);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (adjustingRegion) {
                drawAdjustmentOverlay(canvas);
                return;
            }
            canvas.drawColor(BG);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(TEXT);
            paint.setTextSize(34);
            canvas.drawText(getString(MODE_REGION.equals(mode) ? R.string.capture_title_region
                    : MODE_SWIPE.equals(mode) ? R.string.capture_title_swipe
                    : (MODE_HOLD.equals(mode) ? R.string.capture_title_hold
                            : R.string.capture_title_tap)), 36, 60, paint);

            paint.setColor(MUTED);
            paint.setTextSize(22);
            canvas.drawText(MODE_REGION.equals(mode)
                    ? getString(R.string.capture_instruction_region)
                    : MODE_SWIPE.equals(mode)
                    ? getString(R.string.capture_instruction_swipe)
                    : (MODE_HOLD.equals(mode)
                    ? getString(R.string.capture_instruction_hold)
                    : getString(R.string.capture_instruction_tap)),
                    36, 96, paint);
            canvas.drawText(MODE_REGION.equals(mode)
                    ? getString(R.string.capture_display_target_status, displayId,
                            targetAspectRatio)
                    : getString(R.string.capture_display_status, displayId), 36, 126, paint);

            if (MODE_REGION.equals(mode) && downX >= 0 && downY >= 0
                    && currentX >= 0 && currentY >= 0) {
                RectF region = constrainedRegion(currentX, currentY);
                float left = region.left;
                float top = region.top;
                float right = region.right;
                float bottom = region.bottom;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0x3358A6FF);
                canvas.drawRect(left, top, right, bottom, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                paint.setColor(selectionTooSmall ? 0xFFFF6B7A : PRIMARY);
                canvas.drawRect(left, top, right, bottom, paint);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(20);
                canvas.drawText(Math.round(right - left) + " x " + Math.round(bottom - top),
                        left + 10, Math.max(160, top - 10), paint);
                return;
            }

            if (downX >= 0 && downY >= 0) {
                paint.setColor(PRIMARY);
                canvas.drawCircle(downX, downY, 20, paint);
                paint.setTextSize(20);
                canvas.drawText(getString(R.string.capture_point_start,
                        Math.round(downX), Math.round(downY)), downX + 28, downY + 8, paint);
            }
            if (currentX >= 0 && currentY >= 0) {
                paint.setColor(0xFF7EE787);
                canvas.drawCircle(currentX, currentY, 18, paint);
                if (MODE_SWIPE.equals(mode) && downX >= 0 && downY >= 0) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(4);
                    canvas.drawLine(downX, downY, currentX, currentY, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
                paint.setTextSize(20);
                canvas.drawText(getString(R.string.capture_point_end,
                        Math.round(currentX), Math.round(currentY)),
                        currentX + 28, currentY + 8, paint);
            }
        }

        private void drawAdjustmentOverlay(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x4405070A);
            canvas.drawRect(0, 0, getWidth(), selectedRegion.top, paint);
            canvas.drawRect(0, selectedRegion.bottom, getWidth(), getHeight(), paint);
            canvas.drawRect(0, selectedRegion.top, selectedRegion.left, selectedRegion.bottom, paint);
            canvas.drawRect(selectedRegion.right, selectedRegion.top,
                    getWidth(), selectedRegion.bottom, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(PRIMARY);
            canvas.drawRoundRect(selectedRegion, 12, 12, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(PRIMARY);
            float handleRadius = 13;
            canvas.drawCircle(selectedRegion.left, selectedRegion.top, handleRadius, paint);
            canvas.drawCircle(selectedRegion.right, selectedRegion.top, handleRadius, paint);
            canvas.drawCircle(selectedRegion.right, selectedRegion.bottom, handleRadius, paint);
            canvas.drawCircle(selectedRegion.left, selectedRegion.bottom, handleRadius, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(TEXT);
            paint.setTextSize(30);
            canvas.drawText(getString(R.string.capture_adjust_title), 36, 58, paint);
            paint.setColor(MUTED);
            paint.setTextSize(21);
            canvas.drawText(getString(R.string.capture_adjust_help), 36, 94, paint);

            float buttonHeight = 58;
            float buttonWidth = 138;
            float gap = 16;
            float margin = 28;
            confirmButton.set(getWidth() - margin - buttonWidth, margin,
                    getWidth() - margin, margin + buttonHeight);
            cancelButton.set(confirmButton.left - gap - buttonWidth, margin,
                    confirmButton.left - gap, margin + buttonHeight);
            drawControl(canvas, cancelButton, getString(R.string.common_cancel), false);
            drawControl(canvas, confirmButton, getString(R.string.capture_confirm_apply), true);

            paint.setColor(TEXT);
            paint.setTextSize(19);
            canvas.drawText(Math.round(selectedRegion.width()) + " x "
                            + Math.round(selectedRegion.height()),
                    selectedRegion.left + 10, Math.max(132, selectedRegion.top - 10), paint);
        }

        private void drawControl(Canvas canvas, RectF bounds, String label, boolean primary) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(primary ? 0xE6296FD6 : 0xE5162230);
            canvas.drawRoundRect(bounds, 12, 12, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(primary ? 0xFF70B7FF : 0xFF445A72);
            canvas.drawRoundRect(bounds, 12, 12, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(TEXT);
            paint.setTextSize(22);
            paint.setTextAlign(Paint.Align.CENTER);
            float baseline = bounds.centerY() - (paint.ascent() + paint.descent()) / 2f;
            canvas.drawText(label, bounds.centerX(), baseline, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            currentX = event.getX();
            currentY = event.getY();
            if (MODE_REGION.equals(mode) && adjustingRegion) {
                return handleRegionAdjustment(event);
            }
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                downX = event.getX();
                downY = event.getY();
                downTime = event.getEventTime();
                selectionTooSmall = false;
                invalidate();
                return true;
            }

            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                invalidate();
                return true;
            }

            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                if (MODE_REGION.equals(mode)) {
                    RectF region = constrainedRegion(event.getX(), event.getY());
                    if (region.width() < 48 || region.height() < 48) {
                        selectionTooSmall = true;
                        currentX = event.getX();
                        currentY = event.getY();
                        invalidate();
                        return true;
                    }
                    selectedRegion.set(region);
                    adjustingRegion = true;
                    movingRegion = false;
                    sendPreview(true);
                    invalidate();
                    return true;
                }
                long duration = Math.max(1L, event.getEventTime() - downTime);
                String step;
                if (MODE_SWIPE.equals(mode)) {
                    step = "swipe:" + displayId + "," + Math.round(downX) + "," + Math.round(downY) + ","
                            + Math.round(event.getX()) + "," + Math.round(event.getY()) + "," + duration;
                } else if (MODE_HOLD.equals(mode)) {
                    step = "hold:" + displayId + "," + Math.round(event.getX()) + "," + Math.round(event.getY()) + ","
                            + Math.max(80L, duration);
                } else {
                    step = "tap:" + displayId + "," + Math.round(event.getX()) + "," + Math.round(event.getY());
                }
                finishWithStep(step);
                return true;
            }

            if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                if (MODE_REGION.equals(mode)) {
                    downX = -1;
                    downY = -1;
                    currentX = -1;
                    currentY = -1;
                    invalidate();
                } else {
                    finishAndRemoveTask();
                }
                return true;
            }

            return true;
        }

        private boolean handleRegionAdjustment(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (confirmButton.contains(event.getX(), event.getY())) {
                    finishWithRegion(displayId,
                            selectedRegion.left / Math.max(1f, getWidth()),
                            selectedRegion.top / Math.max(1f, getHeight()),
                            selectedRegion.right / Math.max(1f, getWidth()),
                            selectedRegion.bottom / Math.max(1f, getHeight()));
                    return true;
                }
                if (cancelButton.contains(event.getX(), event.getY())) {
                    cancelRegionCapture();
                    return true;
                }
                resizingCorner = hitResizeCorner(event.getX(), event.getY());
                movingRegion = resizingCorner < 0
                        && selectedRegion.contains(event.getX(), event.getY());
                if (movingRegion) {
                    moveOffsetX = event.getX() - selectedRegion.left;
                    moveOffsetY = event.getY() - selectedRegion.top;
                }
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                    && (movingRegion || resizingCorner >= 0)) {
                if (resizingCorner >= 0) {
                    resizeSelectedRegion(event.getX(), event.getY());
                } else {
                    moveSelectedRegion(event.getX() - moveOffsetX, event.getY() - moveOffsetY);
                }
                sendPreview(false);
                invalidate();
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                if (movingRegion || resizingCorner >= 0) {
                    if (resizingCorner >= 0) {
                        resizeSelectedRegion(event.getX(), event.getY());
                    } else {
                        moveSelectedRegion(event.getX() - moveOffsetX, event.getY() - moveOffsetY);
                    }
                    sendPreview(true);
                    invalidate();
                }
                movingRegion = false;
                resizingCorner = -1;
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                movingRegion = false;
                resizingCorner = -1;
                return true;
            }
            return true;
        }

        private void moveSelectedRegion(float left, float top) {
            float width = selectedRegion.width();
            float height = selectedRegion.height();
            float clampedLeft = Math.max(0f, Math.min(left, getWidth() - width));
            float clampedTop = Math.max(0f, Math.min(top, getHeight() - height));
            selectedRegion.set(clampedLeft, clampedTop,
                    clampedLeft + width, clampedTop + height);
        }

        private int hitResizeCorner(float x, float y) {
            float hitRadius = 44;
            float hitRadiusSquared = hitRadius * hitRadius;
            if (distanceSquared(x, y, selectedRegion.left, selectedRegion.top) <= hitRadiusSquared) {
                return 0;
            }
            if (distanceSquared(x, y, selectedRegion.right, selectedRegion.top) <= hitRadiusSquared) {
                return 1;
            }
            if (distanceSquared(x, y, selectedRegion.right, selectedRegion.bottom) <= hitRadiusSquared) {
                return 2;
            }
            if (distanceSquared(x, y, selectedRegion.left, selectedRegion.bottom) <= hitRadiusSquared) {
                return 3;
            }
            return -1;
        }

        private float distanceSquared(float x1, float y1, float x2, float y2) {
            float dx = x1 - x2;
            float dy = y1 - y2;
            return dx * dx + dy * dy;
        }

        private void resizeSelectedRegion(float pointerX, float pointerY) {
            boolean resizeLeft = resizingCorner == 0 || resizingCorner == 3;
            boolean resizeTop = resizingCorner == 0 || resizingCorner == 1;
            float anchorX = resizeLeft ? selectedRegion.right : selectedRegion.left;
            float anchorY = resizeTop ? selectedRegion.bottom : selectedRegion.top;
            float directionX = resizeLeft ? -1f : 1f;
            float directionY = resizeTop ? -1f : 1f;
            float width = Math.max(48f, Math.abs(pointerX - anchorX));
            float height = Math.max(48f, Math.abs(pointerY - anchorY));
            if (width / height > targetAspectRatio) {
                height = width / targetAspectRatio;
            } else {
                width = height * targetAspectRatio;
            }
            float availableWidth = directionX < 0 ? anchorX : getWidth() - anchorX;
            float availableHeight = directionY < 0 ? anchorY : getHeight() - anchorY;
            float fit = Math.min(1f, Math.min(
                    availableWidth / Math.max(1f, width),
                    availableHeight / Math.max(1f, height)));
            width *= fit;
            height *= fit;
            float oppositeX = anchorX + directionX * width;
            float oppositeY = anchorY + directionY * height;
            selectedRegion.set(
                    Math.min(anchorX, oppositeX), Math.min(anchorY, oppositeY),
                    Math.max(anchorX, oppositeX), Math.max(anchorY, oppositeY));
        }

        private void sendPreview(boolean force) {
            long now = android.os.SystemClock.uptimeMillis();
            if (!force && now - lastPreviewAtMs < 16L) {
                return;
            }
            lastPreviewAtMs = now;
            sendRegion(ACTION_REGION_PREVIEW, displayId,
                    selectedRegion.left / Math.max(1f, getWidth()),
                    selectedRegion.top / Math.max(1f, getHeight()),
                    selectedRegion.right / Math.max(1f, getWidth()),
                    selectedRegion.bottom / Math.max(1f, getHeight()));
        }

        private RectF constrainedRegion(float endX, float endY) {
            float dx = endX - downX;
            float dy = endY - downY;
            float directionX = dx < 0 ? -1f : 1f;
            float directionY = dy < 0 ? -1f : 1f;
            float width = Math.max(1f, Math.abs(dx));
            float height = Math.max(1f, Math.abs(dy));
            if (width / height > targetAspectRatio) {
                height = width / targetAspectRatio;
            } else {
                width = height * targetAspectRatio;
            }

            float availableWidth = directionX > 0 ? getWidth() - downX : downX;
            float availableHeight = directionY > 0 ? getHeight() - downY : downY;
            float scale = Math.min(1f, Math.min(
                    availableWidth / Math.max(1f, width),
                    availableHeight / Math.max(1f, height)));
            width *= Math.max(0f, scale);
            height *= Math.max(0f, scale);
            float oppositeX = downX + directionX * width;
            float oppositeY = downY + directionY * height;
            return new RectF(
                    Math.max(0f, Math.min(downX, oppositeX)),
                    Math.max(0f, Math.min(downY, oppositeY)),
                    Math.min(getWidth(), Math.max(downX, oppositeX)),
                    Math.min(getHeight(), Math.max(downY, oppositeY)));
        }
    }
}
