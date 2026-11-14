package com.tanvir.droidinspect;

import android.os.Build;

/** Production JNI entry point for the ARM64 Native AOT decompiler. */
final class NativeBridge {
    private static volatile boolean attempted;
    private static volatile String error;

    private NativeBridge() { }

    static boolean isArm64Supported() {
        for (String abi : Build.SUPPORTED_ABIS) if ("arm64-v8a".equals(abi)) return true;
        return false;
    }

    static synchronized boolean ensureLoaded() {
        if (attempted) return error == null;
        attempted = true;
        if (!isArm64Supported()) {
            error = "C# decompilation requires an ARM64 (arm64-v8a) Android device.";
            return false;
        }
        try {
            System.loadLibrary("droidinspect_engine");
            System.loadLibrary("droidinspect_jni");
            return true;
        } catch (UnsatisfiedLinkError | SecurityException e) {
            error = "Unable to load the offline C# decompiler: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return false;
        }
    }

    static String getLoadError() {
        return error == null ? "Native decompiler is unavailable." : error;
    }

    static native String decompileFile(String assemblyPath, String referenceDirectory);
}
