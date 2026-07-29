package com.mastercook777.heimdall;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class NativeGamepadPath {
    private static final int ABS_X = 0;
    private static final int ABS_Y = 1;
    private static final int ABS_Z = 2;
    private static final int ABS_RX = 3;
    private static final int ABS_RY = 4;
    private static final int ABS_RZ = 5;
    private static final int BTN_SOUTH = 304;
    private static final int BTN_EAST = 305;
    private static final int BTN_NORTH = 307;
    private static final int BTN_WEST = 308;

    public static final class Device {
        public final String path;
        public final String name;
        public final int rightStickAxisX;
        public final int rightStickAxisY;
        public final boolean readable;
        public final boolean writable;
        private final int score;

        private Device(String path, String name, int axisX, int axisY, int score) {
            this.path = path;
            this.name = name == null || name.trim().length() == 0
                    ? "Thor Controller" : name.trim();
            this.rightStickAxisX = axisX;
            this.rightStickAxisY = axisY;
            this.score = score;
            File file = new File(path);
            this.readable = file.exists() && file.canRead();
            this.writable = file.exists() && file.canWrite();
        }

        public String axisLabel() {
            return axisName(rightStickAxisX) + " / " + axisName(rightStickAxisY);
        }
    }

    private NativeGamepadPath() {
    }

    public static String resolve() {
        Device device = resolveController();
        return device == null ? null : device.path;
    }

    public static Device resolveDevice() {
        return resolveController();
    }

    public static String require() {
        return requireDevice().path;
    }

    public static Device requireDevice() {
        Device device = resolveController();
        if (device == null) {
            throw new IllegalStateException("Thor physical controller not found");
        }
        return device;
    }

    public static String requireWritable() {
        Device device = requireDevice();
        if (!device.writable) {
            throw new IllegalStateException("Physical controller is not writable: " + device.path);
        }
        return device.path;
    }

    public static String requireReadable() {
        Device device = requireDevice();
        if (!device.readable) {
            throw new IllegalStateException("Physical controller is not readable: " + device.path);
        }
        return device.path;
    }

    public static String statusLabel(Context context) {
        return statusLabel(context, false);
    }

    public static String statusLabel(Context context, boolean shizukuReady) {
        Device device = resolveController();
        if (device == null) {
            return context.getString(R.string.controller_status_not_found);
        }
        if (shizukuReady) {
            return context.getString(R.string.controller_status_connected_shizuku, device.name);
        }
        return context.getString(device.writable ? R.string.controller_status_connected
                : R.string.controller_status_not_writable, device.name);
    }

    public static String debugLabel(Context context) {
        return debugLabel(context, false);
    }

    public static String debugLabel(Context context, boolean shizukuReady) {
        List<Device> devices = resolveControllerCandidates();
        if (devices.isEmpty()) {
            return context.getString(R.string.controller_status_not_found);
        }
        StringBuilder builder = new StringBuilder();
        for (Device device : devices) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(device.name).append("  ").append(device.path)
                    .append("  right=").append(device.axisLabel());
            if (shizukuReady) {
                builder.append(device.readable && device.writable
                        ? " / " + context.getString(R.string.controller_debug_direct_access)
                        : " / " + context.getString(R.string.controller_debug_shizuku_access));
            } else {
                builder.append(" / ").append(context.getString(R.string.controller_debug_access_flags,
                        yesNo(context, device.readable), yesNo(context, device.writable)));
            }
        }
        return builder.toString();
    }

    public static String permissionHint(Context context) {
        return permissionHint(context, false);
    }

    public static String permissionHint(Context context, boolean shizukuReady) {
        Device device = resolveController();
        if (device == null) {
            return context.getString(R.string.controller_hint_not_found);
        }
        String identity = device.name + " (" + device.axisLabel() + ")";
        if (device.readable && device.writable) {
            return context.getString(R.string.controller_hint_direct_ready, identity);
        }
        if (shizukuReady) {
            return context.getString(R.string.controller_hint_shizuku_ready, identity);
        }
        int permissionId = !device.readable && !device.writable
                ? R.string.controller_permission_read_write
                : (!device.readable ? R.string.controller_permission_read
                : R.string.controller_permission_write);
        return context.getString(R.string.controller_hint_permission_required,
                identity, context.getString(permissionId));
    }

    public static String userFacingError(String result) {
        if (result == null || result.trim().length() == 0) {
            return "Physical controller operation failed";
        }
        String lower = result.toLowerCase(Locale.US);
        if (lower.contains("/dev/input/event") && lower.contains("failed")) {
            return "Physical controller operation failed: "
                    + (lower.contains("permission") ? "permission denied"
                    : result.substring(result.indexOf("failed")));
        }
        return result;
    }

    public static String userFacingError(Context context, String result) {
        String normalized = userFacingError(result);
        String lower = normalized.toLowerCase(Locale.US);
        if (lower.contains("failed") || lower.contains("permission denied")) {
            return context.getString(lower.contains("permission")
                    ? R.string.native_controller_permission_denied
                    : R.string.native_controller_write_failed);
        }
        return normalized;
    }

    public static boolean operationSucceeded(String result) {
        String lower = result == null ? "" : result.toLowerCase(Locale.US);
        return "ok".equals(lower) || lower.contains(" ok")
                || lower.startsWith("seq:") || "no_events".equals(lower);
    }

    private static Device resolveController() {
        List<Device> devices = resolveControllerCandidates();
        return devices.isEmpty() ? null : devices.get(0);
    }

    private static List<Device> resolveControllerCandidates() {
        List<Device> devices = new ArrayList<>();
        File inputDir = new File("/sys/class/input");
        File[] files = inputDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("event")) {
                    addUnique(devices, createCandidate("/dev/input/" + file.getName(),
                            readFirstLine(new File(file, "device/name"))));
                }
            }
        }
        addProcCandidates(devices);
        Collections.sort(devices, new Comparator<Device>() {
            @Override
            public int compare(Device left, Device right) {
                int scoreOrder = Integer.compare(right.score, left.score);
                if (scoreOrder != 0) return scoreOrder;
                int leftAccess = (left.writable ? 2 : 0) + (left.readable ? 1 : 0);
                int rightAccess = (right.writable ? 2 : 0) + (right.readable ? 1 : 0);
                int accessOrder = Integer.compare(rightAccess, leftAccess);
                return accessOrder != 0 ? accessOrder : left.path.compareTo(right.path);
            }
        });
        return devices;
    }

    private static void addProcCandidates(List<Device> devices) {
        File source = new File("/proc/bus/input/devices");
        if (!source.exists() || !source.canRead()) return;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(source));
            String line;
            String name = null;
            String event = null;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() == 0) {
                    if (event != null) addUnique(devices, createCandidate("/dev/input/" + event, name));
                    name = null;
                    event = null;
                } else if (line.startsWith("N:")) {
                    int firstQuote = line.indexOf('"');
                    int lastQuote = line.lastIndexOf('"');
                    name = firstQuote >= 0 && lastQuote > firstQuote
                            ? line.substring(firstQuote + 1, lastQuote) : line;
                } else if (line.startsWith("H:")) {
                    event = parseEventHandler(line);
                }
            }
            if (event != null) addUnique(devices, createCandidate("/dev/input/" + event, name));
            reader.close();
        } catch (IOException | SecurityException ignored) {
        }
    }

    private static Device createCandidate(String path, String fallbackName) {
        String eventName = new File(path).getName();
        File sysDevice = new File("/sys/class/input/" + eventName + "/device");
        String name = readFirstLine(new File(sysDevice, "name"));
        if (name == null || name.trim().length() == 0) name = fallbackName;
        String lower = name == null ? "" : name.toLowerCase(Locale.US);
        if (isExcludedName(lower)) return null;

        BigInteger abs = readCapability(new File(sysDevice, "capabilities/abs"));
        BigInteger keys = readCapability(new File(sysDevice, "capabilities/key"));
        boolean odinName = lower.contains("odin controller")
                || (lower.contains("odin") && lower.contains("controller"));
        boolean xboxName = lower.contains("xbox") || lower.contains("x-box")
                || lower.contains("xinput");
        boolean controllerName = odinName || xboxName || lower.contains("controller")
                || lower.contains("gamepad") || lower.contains("joystick");

        if (abs == null) {
            if (odinName) return new Device(path, name, ABS_Z, ABS_RZ, 1000);
            if (xboxName) return new Device(path, name, ABS_RX, ABS_RY, 900);
            return null;
        }

        boolean hasLeft = hasBit(abs, ABS_X) && hasBit(abs, ABS_Y);
        boolean hasStandardRight = hasBit(abs, ABS_RX) && hasBit(abs, ABS_RY);
        boolean hasOdinRight = hasBit(abs, ABS_Z) && hasBit(abs, ABS_RZ);
        boolean hasGamepadKeys = keys != null && (hasBit(keys, BTN_SOUTH)
                || hasBit(keys, BTN_EAST) || hasBit(keys, BTN_NORTH) || hasBit(keys, BTN_WEST));
        if (!hasLeft || (!hasStandardRight && !hasOdinRight)) return null;

        int axisX;
        int axisY;
        if (odinName && hasOdinRight) {
            axisX = ABS_Z;
            axisY = ABS_RZ;
        } else if (hasStandardRight) {
            axisX = ABS_RX;
            axisY = ABS_RY;
        } else {
            axisX = ABS_Z;
            axisY = ABS_RZ;
        }

        int score;
        if (odinName) score = 1000;
        else if (xboxName) score = 900;
        else if (controllerName && hasGamepadKeys) score = 600;
        else if (hasGamepadKeys) score = 350;
        else return null;
        return new Device(path, name, axisX, axisY, score);
    }

    private static boolean isExcludedName(String lower) {
        return lower.contains("virtual mouse") || lower.contains("touchscreen")
                || lower.contains("touch screen") || lower.contains("fts_ts")
                || lower.contains("keyboard") || lower.contains("gpio-keys")
                || lower.contains("uinput") || lower.contains("heimdall");
    }

    private static BigInteger readCapability(File file) {
        String value = readFirstLine(file);
        if (value == null) return null;
        try {
            String[] words = value.trim().split("\\s+");
            BigInteger result = BigInteger.ZERO;
            for (int index = 0; index < words.length; index++) {
                String word = words[words.length - 1 - index];
                if (word.length() > 0) {
                    result = result.or(new BigInteger(word, 16).shiftLeft(index * 64));
                }
            }
            return result;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean hasBit(BigInteger value, int bit) {
        return value != null && bit >= 0 && value.testBit(bit);
    }

    private static String parseEventHandler(String line) {
        for (String part : line.split("\\s+")) {
            if (part.startsWith("event")) return part;
        }
        return null;
    }

    private static void addUnique(List<Device> devices, Device candidate) {
        if (candidate == null) return;
        for (Device device : devices) {
            if (device.path.equals(candidate.path)) return;
        }
        devices.add(candidate);
    }

    private static String axisName(int code) {
        if (code == ABS_Z) return "ABS_Z";
        if (code == ABS_RX) return "ABS_RX";
        if (code == ABS_RY) return "ABS_RY";
        if (code == ABS_RZ) return "ABS_RZ";
        return "ABS_" + code;
    }

    private static String yesNo(Context context, boolean value) {
        return context.getString(value ? R.string.common_yes : R.string.common_no);
    }

    private static String readFirstLine(File file) {
        if (!file.exists() || !file.canRead()) return null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            reader.close();
            return line;
        } catch (IOException | SecurityException ignored) {
            return null;
        }
    }
}
