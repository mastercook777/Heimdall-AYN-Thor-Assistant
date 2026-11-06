package com.mastercook777.heimdall;

import android.os.Process;
import android.view.Display;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** Bounded, Thor-only physical-display brightness operations in the root helper. */
final class ThorDisplayBrightnessProbe {
    private static final float MIN_BRIGHTNESS = 0.05f;

    private ThorDisplayBrightnessProbe() { }

    static String readOnlySnapshot() {
        try {
            DisplayPair pair = resolve();
            return "physical displays=" + pair.upperId + "(" + pair.upperUniqueId + ")="
                    + pair.upperBrightness + "," + pair.lowerId + "("
                    + pair.lowerUniqueId + ")=" + pair.lowerBrightness + " count=2";
        } catch (Throwable error) {
            return "brightness probe unavailable: " + errorName(error);
        }
    }

    static String readLinked() {
        try {
            DisplayPair pair = resolve();
            return "ok," + pair.upperBrightness + "," + pair.lowerBrightness;
        } catch (Throwable error) {
            return "unavailable:" + errorName(error);
        }
    }

    static synchronized String writeLinked(float target, boolean commit) {
        if (!Float.isFinite(target) || target < MIN_BRIGHTNESS || target > 1f) {
            return "invalid brightness";
        }
        try {
            DisplayPair pair = resolve();
            Method setter = pair.managerClass.getMethod(commit
                    ? "setBrightness" : "setTemporaryBrightness", int.class, float.class);
            try {
                setter.invoke(pair.manager, pair.upperId, target);
                setter.invoke(pair.manager, pair.lowerId, target);
            } catch (Throwable error) {
                // A partial two-display write must not leave the screens at unrelated levels.
                try {
                    setter.invoke(pair.manager, pair.upperId, pair.upperBrightness);
                    setter.invoke(pair.manager, pair.lowerId, pair.lowerBrightness);
                } catch (Throwable ignored) { }
                return "write failed:" + errorName(error);
            }
            return "ok";
        } catch (Throwable error) {
            return "unavailable:" + errorName(error);
        }
    }

    private static DisplayPair resolve() throws Exception {
        if (Process.myUid() != 0) throw new SecurityException("helper uid");
        Class<?> managerClass = Class.forName("android.hardware.display.DisplayManagerGlobal");
        Object manager = managerClass.getMethod("getInstance").invoke(null);
        if (manager == null) throw new IllegalStateException("display manager");
        Method getInfo = managerClass.getMethod("getDisplayInfo", int.class);
        Method getBrightness = managerClass.getMethod("getBrightness", int.class);
        int[] ids = (int[]) managerClass.getMethod("getDisplayIds").invoke(manager);
        if (ids == null) throw new IllegalStateException("display ids");
        Field uniqueIdField = Class.forName("android.view.DisplayInfo").getField("uniqueId");
        ArrayList<Integer> physical = new ArrayList<>(2);
        ArrayList<String> uniqueIds = new ArrayList<>(2);
        for (int id : ids) {
            Object info = getInfo.invoke(manager, id);
            if (info == null) continue;
            String uniqueId = (String) uniqueIdField.get(info);
            if (uniqueId != null && uniqueId.startsWith("local:")) {
                physical.add(id);
                uniqueIds.add(uniqueId);
            }
        }
        if (physical.size() != 2 || !physical.contains(Display.DEFAULT_DISPLAY)) {
            throw new IllegalStateException("expected two physical displays including 0");
        }
        int upperIndex = physical.get(0) == Display.DEFAULT_DISPLAY ? 0 : 1;
        int lowerIndex = 1 - upperIndex;
        float upper = ((Number) getBrightness.invoke(manager, physical.get(upperIndex)))
                .floatValue();
        float lower = ((Number) getBrightness.invoke(manager, physical.get(lowerIndex)))
                .floatValue();
        if (!Float.isFinite(upper) || !Float.isFinite(lower)
                || upper < 0f || upper > 1f || lower < 0f || lower > 1f) {
            throw new IllegalStateException("invalid display brightness");
        }
        return new DisplayPair(managerClass, manager, physical.get(upperIndex),
                physical.get(lowerIndex), uniqueIds.get(upperIndex),
                uniqueIds.get(lowerIndex), upper, lower);
    }

    private static String errorName(Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        return cause.getClass().getSimpleName();
    }

    private static final class DisplayPair {
        final Class<?> managerClass;
        final Object manager;
        final int upperId;
        final int lowerId;
        final String upperUniqueId;
        final String lowerUniqueId;
        final float upperBrightness;
        final float lowerBrightness;

        DisplayPair(Class<?> managerClass, Object manager, int upperId, int lowerId,
                String upperUniqueId, String lowerUniqueId, float upperBrightness,
                float lowerBrightness) {
            this.managerClass = managerClass;
            this.manager = manager;
            this.upperId = upperId;
            this.lowerId = lowerId;
            this.upperUniqueId = upperUniqueId;
            this.lowerUniqueId = lowerUniqueId;
            this.upperBrightness = upperBrightness;
            this.lowerBrightness = lowerBrightness;
        }
    }
}
