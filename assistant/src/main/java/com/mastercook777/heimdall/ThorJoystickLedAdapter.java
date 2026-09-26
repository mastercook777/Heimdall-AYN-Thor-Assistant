package com.mastercook777.heimdall;

import android.graphics.Color;
import android.os.IBinder;
import android.os.Parcel;

import java.lang.reflect.Method;

/** Minimal best-effort bridge to Thor's firmware-owned joystick LED service. */
final class ThorJoystickLedAdapter {
    private static final String SERVICE_NAME = "PServerBinder";
    private static final String LEFT_LED_NODE = "/sys/class/sn3112l/led/brightness";
    private static final String RIGHT_LED_NODE = "/sys/class/sn3112r/led/brightness";
    private static final int TRANSACTION_EXECUTE = 0;
    static final int TRANSACTION_FLAGS = 0;
    private static final int LED_BRIGHTNESS = 255;

    private volatile IBinder binder;

    boolean isAvailable() {
        return resolveBinder() != null;
    }

    void applyThemeColor(int color) {
        executeOnce(buildStaticColorCommand(color));
    }

    void turnOff() {
        executeOnce(buildTurnOffCommand());
    }

    static String buildStaticColorCommand(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        String value = red + ":" + green + ":" + blue + ":" + LED_BRIGHTNESS;
        return buildDualStickCommand(value);
    }

    static String buildTurnOffCommand() {
        return buildDualStickCommand("0:0:0:0");
    }

    private static String buildDualStickCommand(String value) {
        // Thor selects the stick by sysfs path. Prefix 1 is the only verified
        // static-color channel; additional numeric prefixes are not LED zones.
        return "echo 1-" + value + " > " + LEFT_LED_NODE
                + "; echo 1-" + value + " > " + RIGHT_LED_NODE;
    }

    private synchronized void executeOnce(String command) {
        IBinder service = resolveBinder();
        if (service == null) {
            return;
        }
        Parcel data = null;
        Parcel reply = null;
        try {
            data = Parcel.obtain();
            reply = Parcel.obtain();
            data.writeStringArray(new String[]{command, "1"});
            if (!service.transact(TRANSACTION_EXECUTE, data, reply, TRANSACTION_FLAGS)) {
                binder = null;
            }
        } catch (Throwable ignored) {
            binder = null;
        } finally {
            if (reply != null) {
                reply.recycle();
            }
            if (data != null) {
                data.recycle();
            }
        }
    }

    private IBinder resolveBinder() {
        IBinder current = binder;
        if (current != null && current.isBinderAlive()) {
            return current;
        }
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            Method getService = serviceManager.getMethod("getService", String.class);
            Object resolved = getService.invoke(null, SERVICE_NAME);
            if (resolved instanceof IBinder) {
                IBinder candidate = (IBinder) resolved;
                if (candidate.isBinderAlive()) {
                    binder = candidate;
                    return candidate;
                }
            }
        } catch (Throwable ignored) {
            // Firmware service is optional; theme rendering must continue unchanged.
        }
        binder = null;
        return null;
    }
}
