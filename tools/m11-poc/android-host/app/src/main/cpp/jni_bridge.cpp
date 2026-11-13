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

    if (length > 0) {
        env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(length),
                                reinterpret_cast<const jbyte*>(value));
        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(bytes);
            return nullptr;
        }
    }

    jclass stringClass = env->FindClass("java/lang/String");
    if (stringClass == nullptr || env->ExceptionCheck()) {
        env->DeleteLocalRef(bytes);
        if (stringClass != nullptr) env->DeleteLocalRef(stringClass);
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(
        stringClass, "<init>", "([BLjava/lang/String;)V");
    if (constructor == nullptr || env->ExceptionCheck()) {
        env->DeleteLocalRef(stringClass);
        env->DeleteLocalRef(bytes);
        return nullptr;
    }

    jstring charset = env->NewStringUTF("UTF-8");
    if (charset == nullptr || env->ExceptionCheck()) {
        if (charset != nullptr) env->DeleteLocalRef(charset);
        env->DeleteLocalRef(stringClass);
        env->DeleteLocalRef(bytes);
        return nullptr;
    }

    jstring result = static_cast<jstring>(
        env->NewObject(stringClass, constructor, bytes, charset));

    env->DeleteLocalRef(charset);
    env->DeleteLocalRef(stringClass);
    env->DeleteLocalRef(bytes);
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_tanvir_m11poc_NativeBridge_decompileFile(
    JNIEnv* env, jclass, jstring path, jstring references) {

    if (env == nullptr) return nullptr;
    if (path == nullptr || references == nullptr)
        return toJava(env, "M11 DECOMPILATION ERROR: Missing input path or reference directory");

    const char* nativePath = env->GetStringUTFChars(path, nullptr);
    if (nativePath == nullptr || env->ExceptionCheck()) {
        if (nativePath != nullptr) env->ReleaseStringUTFChars(path, nativePath);
        return nullptr;
    }

    const char* nativeReferences = env->GetStringUTFChars(references, nullptr);
    if (nativeReferences == nullptr || env->ExceptionCheck()) {
        if (nativeReferences != nullptr)
            env->ReleaseStringUTFChars(references, nativeReferences);
        env->ReleaseStringUTFChars(path, nativePath);
        return nullptr;
    }

    void* nativeResult = DroidInspect_DecompileFile(nativePath, nativeReferences);

    env->ReleaseStringUTFChars(references, nativeReferences);
    env->ReleaseStringUTFChars(path, nativePath);

    if (nativeResult == nullptr)
        return toJava(env, "M11 DECOMPILATION ERROR: Native engine returned no allocated result");

    jstring javaResult = toJava(env, static_cast<const char*>(nativeResult));
    DroidInspect_FreeResult(nativeResult);
    return javaResult;
}
