package com.tanvir.m12poc;

/** JNI bridge to the offline Roslyn Native AOT compiler packaged for ARM64. */
public final class NativeBridge {
    private static volatile String loadError;

    static {
        try {
            System.loadLibrary("droidinspect_m12_compiler");
            System.loadLibrary("droidinspect_m12_jni");
        } catch (Throwable error) {
            loadError = "Unable to load offline C# compiler: "
                    + (error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        }
    }

    private NativeBridge() { }

    public static String getLoadError() {
        return loadError;
    }

    public static native String compile(String sourceDirectory, String outputPath, String referenceDirectory);
}
