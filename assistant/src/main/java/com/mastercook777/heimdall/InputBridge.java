package com.mastercook777.heimdall;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

public final class InputBridge {
    public interface Callback {
        void onStatus(String message);

        default void onError(String message) {
            onStatus(message);
        }
    }

    public static final class BackendOption {
        public final String id;
        public final String name;
        public final String description;
        public final boolean available;
        public final boolean supportsMouseMode;
        public final boolean supportsRelativeMove;

        private BackendOption(String id, String name, String description, boolean available,
                boolean supportsMouseMode, boolean supportsRelativeMove) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.available = available;
            this.supportsMouseMode = supportsMouseMode;
            this.supportsRelativeMove = supportsRelativeMove;
        }
    }

    public static final String BACKEND_ACCESSIBILITY = "accessibility";
    public static final String BACKEND_SHIZUKU = "shizuku";
    public static final String BACKEND_ROOT_UINPUT = "root_uinput";
    public static final String BACKEND_VIRTUAL_HID = "virtual_hid";

    private static final String PREFS = "input_backend";
    private static final String KEY_SELECTED_BACKEND = "selected_backend";
    private static final InputBackend ACCESSIBILITY_BACKEND = new AccessibilityInputBackend();
    private static final ShizukuInputBackend SHIZUKU_BACKEND = new ShizukuInputBackend();

    private InputBridge() {
    }

    public static String backendName() {
        return ACCESSIBILITY_BACKEND.name();
    }

    public static String backendName(Context context) {
        return activeBackend(context).name();
    }

    public static BackendOption[] backendOptions(Context context) {
        return new BackendOption[] {
                new BackendOption(BACKEND_ACCESSIBILITY, "Accessibility",
                        resourceText(context, R.string.backend_description_accessibility),
                        true, false, false),
                new BackendOption(BACKEND_SHIZUKU, "Shizuku / Shell",
                        resourceText(context, R.string.backend_description_shizuku),
                        ShizukuNativeController.isBinderAlive(), false, true),
                new BackendOption(BACKEND_ROOT_UINPUT, "Root / uinput",
                        resourceText(context, R.string.backend_description_root_uinput),
                        false, true, true),
                new BackendOption(BACKEND_VIRTUAL_HID, "Virtual HID",
                        resourceText(context, R.string.backend_description_virtual_hid),
                        false, true, true)
        };
    }

    public static BackendOption[] backendOptions() {
        return backendOptions(null);
    }

    public static String selectedBackendId(Context context) {
        if (context == null) {
            return BACKEND_ACCESSIBILITY;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SELECTED_BACKEND, BACKEND_ACCESSIBILITY);
    }

    public static boolean setSelectedBackendId(Context context, String backendId) {
        BackendOption option = findOption(backendId);
        if (context == null || option == null || !option.available) {
            return false;
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SELECTED_BACKEND, option.id)
                .apply();
        return true;
    }

    public static boolean isReady() {
        return ACCESSIBILITY_BACKEND.isReady();
    }

    public static boolean isReady(Context context) {
        return activeBackend(context).isReady();
    }

    public static void openSettings(Context context) {
        activeBackend(context).openSettings(context);
    }

    public static void dispatch(Context context, Macro macro, Callback callback) {
        activeBackend(context).dispatchMacro(context, macro, callback);
    }

    public static void dispatch(Context context, Macro macro,
            boolean enhancedTouchCoexistence, Callback callback) {
        if (enhancedTouchCoexistence) {
            SHIZUKU_BACKEND.dispatchMappedTouchMacro(context, macro, callback);
            return;
        }
        dispatch(context, macro, callback);
    }

    public static boolean dispatchTouchMove(Context context, int displayId, int width, int height,
            float dx, float dy, Callback callback) {
        return activeBackend(context).dispatchTouchMove(context, displayId, width, height, dx, dy, callback);
    }

    public static boolean startTouchpadDrag(Context context, int displayId, int width, int height,
            float anchorX, float anchorY, Callback callback) {
        return activeBackend(context).startTouchpadDrag(context, displayId, width, height, anchorX, anchorY, callback);
    }

    public static boolean moveTouchpadDrag(Context context, float dx, float dy, int strokeMs, Callback callback) {
        return activeBackend(context).moveTouchpadDrag(context, dx, dy, strokeMs, callback);
    }

    public static boolean endTouchpadDrag(Context context, int strokeMs, Callback callback) {
        return activeBackend(context).endTouchpadDrag(context, strokeMs, callback);
    }

    public static boolean startShizukuTouchpadDrag(Context context, int displayId, int width, int height,
            float anchorX, float anchorY, Callback callback) {
        return SHIZUKU_BACKEND.startNativeTouchDrag(context, displayId, width, height, anchorX, anchorY, callback);
    }

    public static boolean moveShizukuTouchpadDrag(Context context, float dx, float dy, int strokeMs, Callback callback) {
        return SHIZUKU_BACKEND.moveNativeTouchDrag(context, dx, dy, strokeMs, callback);
    }

    public static boolean endShizukuTouchpadDrag(Context context, int strokeMs, Callback callback) {
        return SHIZUKU_BACKEND.endNativeTouchDrag(context, strokeMs, callback);
    }

    public static void cancelShizukuTouchpadDrag(Context context) {
        SHIZUKU_BACKEND.cancelNativeTouchDrag(context);
    }

    public static boolean supportsMouseMode() {
        return ACCESSIBILITY_BACKEND.supportsMouseMode();
    }

    public static boolean supportsMouseMode(Context context) {
        return activeBackend(context).supportsMouseMode();
    }

    public static boolean supportsRelativeMove() {
        return ACCESSIBILITY_BACKEND.supportsRelativeMove();
    }

    public static boolean supportsRelativeMove(Context context) {
        return activeBackend(context).supportsRelativeMove();
    }

    public static String emitNativeRightStick(Context context, NativeGamepadPath.Device device,
            float x, float y) {
        if (BACKEND_SHIZUKU.equals(selectedBackendId(context)) && ShizukuNativeController.isReady()) {
            return ShizukuNativeController.emitRightStick(context, device, x, y);
        }
        String path = NativeGamepadPath.requireWritable();
        return UinputNativeProbe.emitNativeRightStick(path, x, y,
                device.rightStickAxisX, device.rightStickAxisY);
    }

    public static String captureNativeGamepadSequence(Context context, String path, int durationMs) {
        if (BACKEND_SHIZUKU.equals(selectedBackendId(context))) {
            if (!ShizukuNativeController.isReady()) {
                return context.getString(R.string.controller_enhancement_unavailable);
            }
            return ShizukuNativeController.captureSequence(context, path, durationMs);
        }
        return UinputNativeProbe.captureEvdevSequence(NativeGamepadPath.requireReadable(), durationMs);
    }

    public static String replayNativeGamepadSequence(Context context, String sequence) {
        if (BACKEND_SHIZUKU.equals(selectedBackendId(context))) {
            if (!ShizukuNativeController.isReady()) {
                return context.getString(R.string.controller_enhancement_unavailable);
            }
            return ShizukuNativeController.replayGamepadStep(
                    context, NativeGamepadPath.require(), sequence, 90);
        }
        return UinputNativeProbe.emitEvdevCombo(
                NativeGamepadPath.requireWritable(), sequence, 90);
    }

    public static String getLastExternalPackageName() {
        return ThorAccessibilityService.getLastExternalPackageName();
    }

    public static List<String> getRecentPackageNames() {
        return ThorAccessibilityService.getRecentPackageNames();
    }

    private static InputBackend activeBackend(Context context) {
        String selected = selectedBackendId(context);
        BackendOption option = findOption(selected);
        if (option == null || !option.available) {
            return ACCESSIBILITY_BACKEND;
        }
        if (BACKEND_ACCESSIBILITY.equals(option.id)) {
            return ACCESSIBILITY_BACKEND;
        }
        if (BACKEND_SHIZUKU.equals(option.id)) {
            return SHIZUKU_BACKEND;
        }
        return ACCESSIBILITY_BACKEND;
    }

    private static BackendOption findOption(String backendId) {
        for (BackendOption option : backendOptions()) {
            if (option.id.equals(backendId)) {
                return option;
            }
        }
        return null;
    }

    private static String resourceText(Context context, int resourceId) {
        return context == null ? "" : context.getString(resourceId);
    }
}
