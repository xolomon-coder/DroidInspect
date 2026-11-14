#include <jni.h>
#include <climits>
#include <cstring>

extern "C" void* DroidInspect_DecompileFile(const char*, const char*);
extern "C" void DroidInspect_FreeResult(void*);

static jstring toJava(JNIEnv* env, const char* value) {
    if (env == nullptr) return nullptr;
    if (value == nullptr) value = "M11 DECOMPILATION ERROR: Native engine returned no result";
    size_t length = std::strlen(value);
    if (length > static_cast<size_t>(INT_MAX)) {
        value = "M11 DECOMPILATION ERROR: Decompiled output exceeds the JNI string limit";
        length = std::strlen(value);
    }
    jbyteArray bytes = env->NewByteArray(static_cast<jsize>(length));
    if (bytes == nullptr || env->ExceptionCheck()) {
        if (bytes != nullptr) env->DeleteLocalRef(bytes);
        return nullptr;
    }
    if (length) {
        env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(length),
                                reinterpret_cast<const jbyte*>(value));
        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(bytes);
            return nullptr;
        }
    }
    jclass cls = env->FindClass("java/lang/String");
    if (cls == nullptr || env->ExceptionCheck()) {
        if (cls != nullptr) env->DeleteLocalRef(cls);
        env->DeleteLocalRef(bytes);
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(cls, "<init>", "([BLjava/lang/String;)V");
    if (ctor == nullptr || env->ExceptionCheck()) {
        env->DeleteLocalRef(cls);
        env->DeleteLocalRef(bytes);
        return nullptr;
    }
    jstring charset = env->NewStringUTF("UTF-8");
    if (charset == nullptr || env->ExceptionCheck()) {
        if (charset != nullptr) env->DeleteLocalRef(charset);
        env->DeleteLocalRef(cls);
        env->DeleteLocalRef(bytes);
        return nullptr;
    }
    jstring result = static_cast<jstring>(env->NewObject(cls, ctor, bytes, charset));
    env->DeleteLocalRef(charset);
    env->DeleteLocalRef(cls);
    env->DeleteLocalRef(bytes);
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_tanvir_droidinspect_NativeBridge_decompileFile(
        JNIEnv* env, jclass, jstring path, jstring references) {
    if (env == nullptr) return nullptr;
    if (path == nullptr || references == nullptr)
        return toJava(env, "M11 DECOMPILATION ERROR: Missing input path or reference directory");
    const char* p = env->GetStringUTFChars(path, nullptr);
    if (p == nullptr || env->ExceptionCheck()) return nullptr;
    const char* r = env->GetStringUTFChars(references, nullptr);
    if (r == nullptr || env->ExceptionCheck()) {
        env->ReleaseStringUTFChars(path, p);
        return nullptr;
    }
    void* native = DroidInspect_DecompileFile(p, r);
    env->ReleaseStringUTFChars(references, r);
    env->ReleaseStringUTFChars(path, p);
    if (native == nullptr)
        return toJava(env, "M11 DECOMPILATION ERROR: Native engine returned no allocated result");
    jstring result = toJava(env, static_cast<const char*>(native));
    DroidInspect_FreeResult(native);
    return result;
}
