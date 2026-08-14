package com.mastercook777.heimdall;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

import java.lang.reflect.Method;

public final class ShizukuNativeUserService extends Binder {
    private static final String THOR_TOUCH_UNSUPPORTED = "THOR_TOUCH_UNSUPPORTED";
    public static final String DESCRIPTOR =
            BuildConfig.APPLICATION_ID + ".IShizukuNativeService";
    public static final int TRANSACTION_PING = IBinder.FIRST_CALL_TRANSACTION;
    public static final int TRANSACTION_EMIT_GAMEPAD = IBinder.FIRST_CALL_TRANSACTION + 1;
    public static final int TRANSACTION_EMIT_RIGHT_STICK = IBinder.FIRST_CALL_TRANSACTION + 2;
    public static final int TRANSACTION_CAPTURE_SEQUENCE = IBinder.FIRST_CALL_TRANSACTION + 3;
    public static final int TRANSACTION_INJECT_TOUCH_EVENT = IBinder.FIRST_CALL_TRANSACTION + 4;
    public static final int TRANSACTION_INJECT_MAPPED_TOUCH_EVENT = IBinder.FIRST_CALL_TRANSACTION + 5;
    public static final int TRANSACTION_RELEASE_MAPPED_TOUCH = IBinder.FIRST_CALL_TRANSACTION + 6;
    public static final int TRANSACTION_OPEN_VIRTUAL_MOUSE = IBinder.FIRST_CALL_TRANSACTION + 7;
    public static final int TRANSACTION_EMIT_VIRTUAL_MOUSE = IBinder.FIRST_CALL_TRANSACTION + 8;
    public static final int TRANSACTION_RELEASE_VIRTUAL_MOUSE = IBinder.FIRST_CALL_TRANSACTION + 9;
    private long touchDownTime;
    private boolean touchSessionActive;
    private boolean touchUsesThorMappedDevice;

    public ShizukuNativeUserService() {
        attachInterface(null, DESCRIPTOR);
    }

    public ShizukuNativeUserService(Context context) {
        attachInterface(null, DESCRIPTOR);
    }

    public String ping() {
        return "heimdall shizuku native service protocol=10";
    }

    public String emitGamepadStep(String path, String value, int holdMs) {
        try {
            return NativeGamepadPath.userFacingError(UinputNativeProbe.emitEvdevCombo(path, value, holdMs));
        } catch (Throwable t) {
            return "Physical controller replay failed: " + t.getClass().getSimpleName()
                    + ": " + (t.getMessage() == null ? "Shizuku UserService failed" : t.getMessage());
        }
    }

    public String emitRightStick(String path, float x, float y, int axisX, int axisY) {
        try {
            return NativeGamepadPath.userFacingError(
                    UinputNativeProbe.emitNativeRightStick(path, x, y, axisX, axisY));
        } catch (Throwable t) {
            return "Native right-stick output failed: " + t.getClass().getSimpleName()
                    + ": " + (t.getMessage() == null ? "Shizuku UserService failed" : t.getMessage());
        }
    }

    public String captureSequence(String path, int durationMs) {
        try {
            return NativeGamepadPath.userFacingError(UinputNativeProbe.captureEvdevSequence(path, durationMs));
        } catch (Throwable t) {
            return "Physical controller recording failed: " + t.getClass().getSimpleName()
                    + ": " + (t.getMessage() == null ? "Shizuku UserService failed" : t.getMessage());
        }
    }

    public synchronized String injectTouchEvent(int displayId, int action, int x, int y,
            int width, int height, int rotation) {
        return injectTouchEventInternal(displayId, action, x, y, width, height,
                rotation, false);
    }

    public synchronized String injectMappedTouchEvent(int displayId, int action, int x, int y,
            int width, int height, int rotation) {
        return injectTouchEventInternal(displayId, action, x, y, width, height,
                rotation, true);
    }

    private String injectTouchEventInternal(int displayId, int action, int x, int y,
            int width, int height, int rotation, boolean requireThorMappedDevice) {
        try {
            if (action == MotionEvent.ACTION_DOWN) {
                UinputNativeProbe.releaseThorMappedTouch();
                touchUsesThorMappedDevice = false;
                touchSessionActive = false;
                if (displayId == 0) {
                    String nativeResult = UinputNativeProbe.emitThorMappedTouch(
                            action, x, y, width, height, rotation);
                    if ("ok".equals(nativeResult)) {
                        touchUsesThorMappedDevice = true;
                        touchSessionActive = true;
                        touchDownTime = 0L;
                        return "ok";
                    }
                    if (!THOR_TOUCH_UNSUPPORTED.equals(nativeResult)) {
                        return nativeResult;
                    }
                }
                if (requireThorMappedDevice) {
                    return THOR_TOUCH_UNSUPPORTED;
                }
                touchSessionActive = true;
                return injectLegacyTouchEvent(displayId, action, x, y);
            }
            if (!touchSessionActive) {
                return "Native touch injection failed: touch session is not active";
            }
            if (touchUsesThorMappedDevice) {
                String result = UinputNativeProbe.emitThorMappedTouch(
                        action, x, y, width, height, rotation);
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL
                        || !"ok".equals(result)) {
                    touchSessionActive = false;
                    touchUsesThorMappedDevice = false;
                    if (!"ok".equals(result)) {
                        UinputNativeProbe.releaseThorMappedTouch();
                    }
                }
                return result;
            }
            if (requireThorMappedDevice) {
                touchSessionActive = false;
                return THOR_TOUCH_UNSUPPORTED;
            }
            String result = injectLegacyTouchEvent(displayId, action, x, y);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                touchSessionActive = false;
            }
            return result;
        } catch (Throwable t) {
            touchSessionActive = false;
            touchUsesThorMappedDevice = false;
            try {
                UinputNativeProbe.releaseThorMappedTouch();
            } catch (Throwable ignored) {
            }
            return "Native touch injection failed: " + t.getClass().getSimpleName()
                    + ": " + (t.getMessage() == null ? "touch injection failed" : t.getMessage());
        }
    }

    public synchronized String releaseMappedTouch() {
        try {
            UinputNativeProbe.releaseThorMappedTouch();
            touchDownTime = 0L;
            touchSessionActive = false;
            touchUsesThorMappedDevice = false;
            return "ok";
        } catch (Throwable t) {
            touchDownTime = 0L;
            touchSessionActive = false;
            touchUsesThorMappedDevice = false;
            return "Thor mapped touch release failed";
        }
    }

    private synchronized String openVirtualMouse() {
        try {
            return UinputNativeProbe.openVirtualMouse();
        } catch (Throwable t) {
            return "Virtual mouse could not open";
        }
    }

    private synchronized String emitVirtualMouse(
            int dx, int dy, int wheel, int button, int buttonValue) {
        try {
            return UinputNativeProbe.emitVirtualMouseFrame(
                    dx, dy, wheel, button, buttonValue);
        } catch (Throwable t) {
            UinputNativeProbe.releaseVirtualMouse();
            return "Virtual mouse input failed";
        }
    }

    private synchronized String releaseVirtualMouse() {
        try {
            return UinputNativeProbe.releaseVirtualMouse();
        } catch (Throwable t) {
            return "Virtual mouse release failed";
        }
    }

    private String injectLegacyTouchEvent(int displayId, int action, int x, int y) {
        try {
            long now = SystemClock.uptimeMillis();
            if (action == MotionEvent.ACTION_DOWN || touchDownTime <= 0L) {
                touchDownTime = now;
            }
            MotionEvent event = MotionEvent.obtain(
                    touchDownTime,
                    now,
                    action,
                    x,
                    y,
                    1.0f,
                    1.0f,
                    0,
                    1.0f,
                    1.0f,
                    0,
                    0);
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            setEventDisplayId(event, displayId);
            boolean ok = injectMotionEvent(event);
            event.recycle();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                touchDownTime = 0L;
            }
            return ok ? "ok" : "Native touch injection failed: InputManager rejected event";
        } catch (Throwable t) {
            return "Native touch injection failed: " + t.getClass().getSimpleName()
                    + ": " + (t.getMessage() == null ? "InputManager injection failed" : t.getMessage());
        }
    }

    private void setEventDisplayId(MotionEvent event, int displayId) {
        try {
            Method method = MotionEvent.class.getMethod("setDisplayId", int.class);
            method.invoke(event, displayId);
        } catch (Throwable ignored) {
        }
    }

    private boolean injectMotionEvent(MotionEvent event) throws Exception {
        Class<?> inputManagerClass = Class.forName("android.hardware.input.InputManager");
        Method getInstance = inputManagerClass.getDeclaredMethod("getInstance");
        Object inputManager = getInstance.invoke(null);
        Method inject = inputManagerClass.getDeclaredMethod("injectInputEvent",
                android.view.InputEvent.class, int.class);
        Object result = inject.invoke(inputManager, event, 0);
        return result instanceof Boolean && (Boolean) result;
    }

    @Override
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code == INTERFACE_TRANSACTION) {
            reply.writeString(DESCRIPTOR);
            return true;
        }
        data.enforceInterface(DESCRIPTOR);
        if (code == TRANSACTION_PING) {
            reply.writeNoException();
            reply.writeString(ping());
            return true;
        }
        if (code == TRANSACTION_EMIT_GAMEPAD) {
            String path = data.readString();
            String value = data.readString();
            int holdMs = data.readInt();
            reply.writeNoException();
            reply.writeString(emitGamepadStep(path, value, holdMs));
            return true;
        }
        if (code == TRANSACTION_EMIT_RIGHT_STICK) {
            String path = data.readString();
            float x = data.readFloat();
            float y = data.readFloat();
            int axisX = data.readInt();
            int axisY = data.readInt();
            reply.writeNoException();
            reply.writeString(emitRightStick(path, x, y, axisX, axisY));
            return true;
        }
        if (code == TRANSACTION_CAPTURE_SEQUENCE) {
            String path = data.readString();
            int durationMs = data.readInt();
            reply.writeNoException();
            reply.writeString(captureSequence(path, durationMs));
            return true;
        }
        if (code == TRANSACTION_INJECT_TOUCH_EVENT) {
            int displayId = data.readInt();
            int action = data.readInt();
            int x = data.readInt();
            int y = data.readInt();
            int width = data.readInt();
            int height = data.readInt();
            int rotation = data.readInt();
            reply.writeNoException();
            reply.writeString(injectTouchEvent(displayId, action, x, y, width, height, rotation));
            return true;
        }
        if (code == TRANSACTION_INJECT_MAPPED_TOUCH_EVENT) {
            int displayId = data.readInt();
            int action = data.readInt();
            int x = data.readInt();
            int y = data.readInt();
            int width = data.readInt();
            int height = data.readInt();
            int rotation = data.readInt();
            reply.writeNoException();
            reply.writeString(injectMappedTouchEvent(
                    displayId, action, x, y, width, height, rotation));
            return true;
        }
        if (code == TRANSACTION_RELEASE_MAPPED_TOUCH) {
            reply.writeNoException();
            reply.writeString(releaseMappedTouch());
            return true;
        }
        if (code == TRANSACTION_OPEN_VIRTUAL_MOUSE) {
            reply.writeNoException();
            reply.writeString(openVirtualMouse());
            return true;
        }
        if (code == TRANSACTION_EMIT_VIRTUAL_MOUSE) {
            int dx = data.readInt();
            int dy = data.readInt();
            int wheel = data.readInt();
            int button = data.readInt();
            int buttonValue = data.readInt();
            reply.writeNoException();
            reply.writeString(emitVirtualMouse(dx, dy, wheel, button, buttonValue));
            return true;
        }
        if (code == TRANSACTION_RELEASE_VIRTUAL_MOUSE) {
            reply.writeNoException();
            reply.writeString(releaseVirtualMouse());
            return true;
        }
        return super.onTransact(code, data, reply, flags);
    }

    public void destroy() {
        try {
            UinputNativeProbe.releaseThorMappedTouch();
        } catch (Throwable ignored) {
        }
        try {
            UinputNativeProbe.releaseVirtualMouse();
        } catch (Throwable ignored) {
        }
        System.exit(0);
    }
}
