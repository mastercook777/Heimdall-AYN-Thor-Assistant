package com.mastercook777.heimdall;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import rikka.shizuku.Shizuku;

public final class ShizukuNativeController {
    static final String THOR_TOUCH_UNSUPPORTED = "THOR_TOUCH_UNSUPPORTED";
    private static final int REQUEST_CODE = 4109;
    private static final long BIND_TIMEOUT_MS = 10_000L;
    private static final Object LOCK = new Object();
    private static final ExecutorService BIND_EXECUTOR =
            Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "shizuku-user-service-bind");
                thread.setDaemon(true);
                return thread;
            });

    private static IBinder service;
    private static boolean binding;
    private static CountDownLatch bindLatch;
    private static int bindGeneration;

    private static final IBinder.DeathRecipient SERVICE_DEATH_RECIPIENT =
            ShizukuNativeController::clearService;

    private static final ServiceConnection CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            if (binder == null || !binder.isBinderAlive()) {
                clearService();
                return;
            }
            try {
                binder.linkToDeath(SERVICE_DEATH_RECIPIENT, 0);
            } catch (RemoteException error) {
                clearService();
                return;
            }
            synchronized (LOCK) {
                if (service != binder) {
                    unlinkDeathRecipientLocked(service);
                }
                service = binder;
                binding = false;
                bindGeneration++;
                releaseBindLatchLocked();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            clearService();
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

    public static boolean isServiceBound() {
        synchronized (LOCK) {
            if (service == null) {
                return false;
            }
            if (service.isBinderAlive()) {
                return true;
            }
            unlinkDeathRecipientLocked(service);
            service = null;
            binding = false;
            bindGeneration++;
            releaseBindLatchLocked();
            return false;
        }
    }

    public static boolean isServiceBinding() {
        synchronized (LOCK) {
            return binding;
        }
    }

    /**
     * Starts the UserService connection without executing Shizuku Binder work on the caller.
     * This method is safe to call from TouchPad and Activity lifecycle callbacks.
     */
    public static boolean requestServiceBinding(Context context) {
        if (context == null || !isPermissionGranted()) {
            return false;
        }
        Context appContext = context.getApplicationContext();
        int generation;
        synchronized (LOCK) {
            if (service != null && service.isBinderAlive()) {
                return true;
            }
            if (service != null) {
                unlinkDeathRecipientLocked(service);
                service = null;
            }
            if (binding) {
                return true;
            }
            binding = true;
            bindLatch = new CountDownLatch(1);
            generation = ++bindGeneration;
        }
        try {
            BIND_EXECUTOR.execute(() -> bindUserService(appContext, generation));
        } catch (RuntimeException error) {
            failBinding(generation);
            return false;
        }
        AssistantMainHandler.postDelayed(
                () -> failBinding(generation), BIND_TIMEOUT_MS);
        return true;
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

    public static String openVirtualMouse(Context context) {
        return virtualMouseTransaction(context,
                ShizukuNativeUserService.TRANSACTION_OPEN_VIRTUAL_MOUSE,
                0, 0, 0, 0, 0, 1500);
    }

    public static String emitVirtualMouse(Context context,
            int dx, int dy, int wheel, int button, int buttonValue) {
        return virtualMouseTransaction(context,
                ShizukuNativeUserService.TRANSACTION_EMIT_VIRTUAL_MOUSE,
                dx, dy, wheel, button, buttonValue, 0);
    }

    public static String releaseVirtualMouse(Context context) {
        return virtualMouseTransaction(context,
                ShizukuNativeUserService.TRANSACTION_RELEASE_VIRTUAL_MOUSE,
                0, 0, 0, 0, 0, 0);
    }

    public static String openVirtualKeyboard(Context context) {
        return virtualKeyboardTransaction(context,
                ShizukuNativeUserService.TRANSACTION_OPEN_VIRTUAL_KEYBOARD,
                0, 0, 1500);
    }

    public static String emitVirtualKeyboard(Context context, int keyCode, int keyValue) {
        return virtualKeyboardTransaction(context,
                ShizukuNativeUserService.TRANSACTION_EMIT_VIRTUAL_KEYBOARD,
                keyCode, keyValue, 0);
    }

    public static String releaseVirtualKeyboardKeys(Context context) {
        return virtualKeyboardTransaction(context,
                ShizukuNativeUserService.TRANSACTION_RELEASE_VIRTUAL_KEYBOARD_KEYS,
                0, 0, 0);
    }

    public static String releaseVirtualKeyboard(Context context) {
        return virtualKeyboardTransaction(context,
                ShizukuNativeUserService.TRANSACTION_RELEASE_VIRTUAL_KEYBOARD,
                0, 0, 0);
    }

    private static String virtualKeyboardTransaction(Context context, int transaction,
            int keyCode, int keyValue, long waitMs) {
        IBinder bound = getService(context, waitMs);
        if (bound == null) {
            return context.getString(R.string.virtual_keyboard_unavailable);
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuNativeUserService.DESCRIPTOR);
            if (transaction == ShizukuNativeUserService.TRANSACTION_EMIT_VIRTUAL_KEYBOARD) {
                data.writeInt(keyCode);
                data.writeInt(keyValue);
            }
            if (!bound.transact(transaction, data, reply, 0)) {
                clearService();
                return context.getString(R.string.virtual_keyboard_unavailable);
            }
            reply.readException();
            return reply.readString();
        } catch (RemoteException | RuntimeException e) {
            clearService();
            return context.getString(R.string.virtual_keyboard_unavailable);
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private static String virtualMouseTransaction(Context context, int transaction,
            int dx, int dy, int wheel, int button, int buttonValue, long waitMs) {
        IBinder bound = getService(context, waitMs);
        if (bound == null) {
            return context.getString(R.string.virtual_mouse_unavailable);
        }
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(ShizukuNativeUserService.DESCRIPTOR);
            if (transaction == ShizukuNativeUserService.TRANSACTION_EMIT_VIRTUAL_MOUSE) {
                data.writeInt(dx);
                data.writeInt(dy);
                data.writeInt(wheel);
                data.writeInt(button);
                data.writeInt(buttonValue);
            }
            if (!bound.transact(transaction, data, reply, 0)) {
                clearService();
                return context.getString(R.string.virtual_mouse_unavailable);
            }
            reply.readException();
            return reply.readString();
        } catch (RemoteException | RuntimeException e) {
            clearService();
            return context.getString(R.string.virtual_mouse_unavailable);
        } finally {
            data.recycle();
            reply.recycle();
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
        if (isServiceBound()) {
            synchronized (LOCK) {
                return service;
            }
        }
        if (!requestServiceBinding(context)) {
            requestPermission();
            return null;
        }
        if (waitMs <= 0 || Looper.myLooper() == Looper.getMainLooper()) {
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

    private static void bindUserService(Context context, int generation) {
        synchronized (LOCK) {
            if (!binding || generation != bindGeneration) {
                return;
            }
        }
        try {
            Shizuku.UserServiceArgs args = userServiceArgs(
                    context, "heimdall_native_controller_v12", 12);
            Shizuku.bindUserService(args, CONNECTION);
        } catch (Throwable error) {
            failBinding(generation);
        }
    }

    private static void failBinding(int generation) {
        synchronized (LOCK) {
            if (!binding || generation != bindGeneration) {
                return;
            }
            binding = false;
            bindGeneration++;
            releaseBindLatchLocked();
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
            unlinkDeathRecipientLocked(service);
            service = null;
            binding = false;
            bindGeneration++;
            releaseBindLatchLocked();
        }
    }

    private static void releaseBindLatchLocked() {
        if (bindLatch != null) {
            bindLatch.countDown();
            bindLatch = null;
        }
    }

    private static void unlinkDeathRecipientLocked(IBinder binder) {
        if (binder == null) {
            return;
        }
        try {
            binder.unlinkToDeath(SERVICE_DEATH_RECIPIENT, 0);
        } catch (Throwable ignored) {
        }
    }

    static void invalidateService() {
        clearService();
    }
}
