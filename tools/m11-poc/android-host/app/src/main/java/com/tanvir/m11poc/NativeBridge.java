package com.tanvir.m11poc;
final class NativeBridge {
 static { System.loadLibrary("droidinspect_engine"); System.loadLibrary("droidinspect_jni"); }
 static native String decompileFile(String assemblyPath,String referenceDirectory);
}
