package com.mastercook777.heimdall;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;

public final class ShizukuNativeController {
    static final String THOR_TOUCH_UNSUPPORTED = "THOR_TOUCH_UNSUPPORTED";
    private static final int REQUEST_CODE = 4109;
    private static final Object LOCK = new Object();

    private static IBinder service;
    private static boolean binding;
    private static CountDownLatch bindLatch;

    private static final ServiceConnection CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            synchronized (LOCK) {
                service = binder;
                binding = false;
                if (bindLatch != null) {
                    bindLatch.countDown();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (LOCK) {
                service = null;
                binding = false;
            }
        }
    };

    private ShizukuNativeController() {
    }

    public static boolean isBinderAlive() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isPermissionGranted() {
        try {
            return isBinderAlive() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isReady() {
        return isPermissionGranted();
    }

    public static String statusLabel() {
        if (!isBinderAlive()) {
            return "Shizuku not running";
        }
        if (!isPermissionGranted()) {
            return "Shizuku permission required";
        }
        String suffix;
        synchronized (LOCK) {
            suffix = service == null ? " / service not bound" : " / service bound";
        }
        try {
            return "Shizuku ready uid=" + Shizuku.getUid() + suffix;
        } catch (Throwable ignored) {
            return "Shizuku ready" + suffix;
        }
    }

    public static void requestPermission() {
        try {
            if (isBinderAlive() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED
                    && !Shizuku.shouldShowRequestPermissionRationale()) {
                Shizuku.requestPermission(REQUEST_CODE);
            }
        } catch (Throwable ignored) {
        }
    }

    public static String emitGamepadStep(Context context, String path, String value, int holdMs) {
        IBinder bound = getService(context, 1500);
        if (bound == null) {
            return context.getString(R.string.native_controller_replay_failed);
        }
        return emitGamepadStep(context, bound, path, value, holdMs);
    }

    private static String emitGamepadStep(Context context, IBinder bound,
            String path, String value, int holdMs) {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuNativeUserService.DESCRIPTOR);
            data.writeString(path);
            data.writeString(value);
            data.writeInt(holdMs);
            bound.transact(ShizukuNativeUserService.TRANSACTION_EMIT_GAMEPAD, data, reply, 0);
            reply.readException();
            return reply.readString();
        } catch (RemoteException e) {
            clearService();
            return context.getString(R.string.native_controller_replay_failed);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public static String replayGamepadStep(Context context, String path, String value, int holdMs) {
        IBinder bound = getService(context, 1500);
        if (bound == null) {
            return context.getString(R.string.native_controller_replay_failed);
        }
        if (value != null && value.startsWith("seq:")
                && !GamepadSequencePolicy.inspect(value).containsSystemNavigationKey()) {
            return ShizukuGamepadSequenceReplay.replay(value,
                    event -> emitGamepadStep(context, bound, path, event, holdMs));
        }
        // Keep system-navigation down/up events in one Binder call. Ordinary controller
        // sequences retain the accepted short-transaction route used with Thor mapping.
        return emitGamepadStep(context, bound, path, value, holdMs);
    }

    public static String emitRightStick(Context context, NativeGamepadPath.Device device,
            float x, float y) {
        IBinder bound = getService(context, 0);
        if (bound == null) {
            return context.getString(R.string.native_controller_write_failed);
        }
        try {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken(ShizukuNativeUserService.DESCRIPTOR);
            data.writeString(device.path);
            data.writeFloat(x);
            data.writeFloat(y);
            data.writeInt(device.rightStickAxisX);
            data.writeInt(device.rightStickAxisY);
            bound.transact(ShizukuNativeUserService.TRANSACTION_EMIT_RIGHT_STICK, data, reply, 0);
            reply.readException();
            String result = reply.readString();
            data.recycle();
            reply.recycle();
            return result;
        } catch (RemoteException e) {
            clearService();
            return context.getString(R.string.native_controller_write_failed);
        }
    }

    public static String captureSequence(Context context, String path, int durationMs) {
        IBinder bound = getService(context, Math.max(1500, durationMs + 500L));
        if (bound == null) {
            return context.getString(R.string.native_controller_record_failed);
        }
        try {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken(ShizukuNativeUserService.DESCRIPTOR);
            data.writeString(path);
            data.writeInt(durationMs);
            bound.transact(ShizukuNativeUserService.TRANSACTION_CAPTURE_SEQUENCE, data, reply, 0);
            reply.readException();
            String result = reply.readString();
            data.recycle();
            reply.recycle();
            return result;
        } catch (RemoteException e) {
            clearService();
            return context.getString(R.string.native_controller_record_failed);
        }
    }

    public static boolean warmUp(Context context, long waitMs) {
        return getService(context, waitMs) != null;
    }

    public static String injectTouchEvent(Context context, int displayId, int action, int x, int y,
            int width, int height, int rotation) {
        return injectTouchEvent(context, ShizukuNativeUserService.TRANSACTION_INJECT_TOUCH_EVENT,
                displayId, action, x, y, width, height, rotation);
    }

    public static String injectMappedTouchEvent(Context context, int displayId, int action,
            int x, int y, int width, int height, int rotation) {
        return injectTouchEvent(context,
                ShizukuNativeUserService.TRANSACTION_INJECT_MAPPED_TOUCH_EVENT,
                displayId, action, x, y, width, height, rotation);
    }

    private static String injectTouchEvent(Context context, int transaction, int displayId,
            int action, int x, int y, int width, int height, int rotation) {
        IBinder bound = getService(context, 0);
        if (bound == null) {
            return context.getString(R.string.native_touch_connection_failed);
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuNativeUserService.DESCRIPTOR);
            data.writeInt(displayId);
            data.writeInt(action);
            data.writeInt(x);
            data.writeInt(y);
            data.writeInt(width);
            data.writeInt(height);
            data.writeInt(rotation);
            bound.transact(transaction, data, reply, 0);
            reply.readException();
            return reply.readString();
        } catch (RemoteException e) {
            clearService();
            return context.getString(R.string.native_touch_connection_failed);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    public static String releaseMappedTouch(Context context) {
        IBinder bound = getService(context, 0);
        if (bound == null) {
            return context.getString(R.string.native_touch_connection_failed);
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuNativeUserService.DESCRIPTOR);
            bound.transact(ShizukuNativeUserService.TRANSACTION_RELEASE_MAPPED_TOUCH,
                    data, reply, 0);
            reply.readException();
            return reply.readString();
        } catch (RemoteException e) {
            clearService();
            return context.getString(R.string.native_touch_connection_failed);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private static IBinder getService(Context context, long waitMs) {
        if (!isPermissionGranted()) {
            requestPermission();
            return null;
        }
        synchronized (LOCK) {
            if (service != null) {
                return service;
            }
            if (!binding) {
                binding = true;
                bindLatch = new CountDownLatch(1);
                try {
                    Shizuku.UserServiceArgs args = userServiceArgs(
                            context, "heimdall_native_controller_v10", 10);
                    Shizuku.bindUserService(args, CONNECTION);
                } catch (Throwable t) {
                    binding = false;
                    if (bindLatch != null) {
                        bindLatch.countDown();
                    }
                    return null;
                }
            }
        }
        if (waitMs <= 0) {
            synchronized (LOCK) {
                return service;
            }
        }
        try {
            CountDownLatch latch;
            synchronized (LOCK) {
                latch = bindLatch;
            }
            if (latch != null) {
                latch.await(waitMs, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (LOCK) {
            return service;
        }
    }

    private static Shizuku.UserServiceArgs userServiceArgs(Context context, String tag, int version) {
        return new Shizuku.UserServiceArgs(
                new ComponentName(context.getPackageName(), ShizukuNativeUserService.class.getName()))
                .daemon(false)
                .debuggable(true)
                .processNameSuffix("shizuku_native_v" + version)
                .tag(tag)
                .version(version);
    }

    private static void clearService() {
        synchronized (LOCK) {
            service = null;
            binding = false;
        }
    }

    static void invalidateService() {
        clearService();
    }
}
