package com.mastercook777.heimdall;

public final class UinputNativeProbe {
    static {
        try {
            System.loadLibrary("thor_uinput_probe");
        } catch (UnsatisfiedLinkError normalLoadError) {
            // The PServer-launched app_process has our APK on CLASSPATH but no
            // app ClassLoader native-library search path. The APK's JNI libs
            // are stored unextracted, so load the aligned ZIP entry directly.
            String apk = System.getenv("CLASSPATH");
            if (android.os.Process.myUid() != 0 || apk == null
                    || !apk.matches("[A-Za-z0-9_./:=+~\\-]+")
                    || !apk.endsWith("/base.apk")) {
                throw normalLoadError;
            }
            String abi = android.os.Process.is64Bit() ? "arm64-v8a" : "armeabi-v7a";
            System.load(apk + "!/lib/" + abi + "/libthor_uinput_probe.so");
        }
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
