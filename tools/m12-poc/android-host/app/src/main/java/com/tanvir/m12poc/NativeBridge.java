package com.tanvir.m12poc;

final class NativeBridge {
    static {
        System.loadLibrary("droidinspect_m12_compiler");
        System.loadLibrary("droidinspect_m12_jni");
    }
    private NativeBridge() { }
    static native String compile(String sourceDirectory, String outputPath, String referenceDirectory, String originalAssemblyPath);
}
