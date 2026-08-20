package com.mastercook777.heimdall;

public final class UinputNativeProbe {
    static {
        System.loadLibrary("thor_uinput_probe");
    }

    private UinputNativeProbe() {
    }

    public static native String emitEvdevCombo(String path, String combo, int holdMs);

    public static native String captureEvdevSequence(String path, int durationMs);

    public static native String emitNativeRightStick(
            String path, float x, float y, int axisX, int axisY);

    public static native String emitThorMappedTouch(
            int action, int x, int y, int width, int height, int rotation);

    public static native void releaseThorMappedTouch();

    public static native String openVirtualMouse();

    public static native String emitVirtualMouseFrame(
            int dx, int dy, int wheel, int button, int buttonValue);

    public static native String releaseVirtualMouse();

    public static native String openVirtualKeyboard();

    public static native String emitVirtualKeyboardKey(int keyCode, int keyValue);

    public static native String releaseVirtualKeyboardKeys();

    public static native String releaseVirtualKeyboard();
}
