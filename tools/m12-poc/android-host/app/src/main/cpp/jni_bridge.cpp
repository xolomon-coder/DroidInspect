#include <jni.h>
#include <cstring>
#include <climits>

extern "C" void* DroidInspect_M12_Compile(const char*, const char*, const char*, const char*);
extern "C" void* DroidInspect_M12_ResolveDependencies(const char*, const char*, const char*);
extern "C" void DroidInspect_M12_FreeResult(void*);

static jstring toJava(JNIEnv* env, const char* value) {
    if (value == nullptr) value = "M12 compiler returned no result.";
    size_t length = std::strlen(value);
    if (length > static_cast<size_t>(INT_MAX)) return env->NewStringUTF("M12 result exceeded JNI limit.");
    jbyteArray bytes = env->NewByteArray(static_cast<jsize>(length));
    if (bytes == nullptr) return nullptr;
    env->SetByteArrayRegion(bytes, 0, static_cast<jsize>(length), reinterpret_cast<const jbyte*>(value));
    if (env->ExceptionCheck()) { env->DeleteLocalRef(bytes); return nullptr; }
    jclass cls = env->FindClass("java/lang/String");
    if (cls == nullptr) { env->DeleteLocalRef(bytes); return nullptr; }
    jmethodID ctor = env->GetMethodID(cls, "<init>", "([BLjava/lang/String;)V");
    if (ctor == nullptr) { env->DeleteLocalRef(cls); env->DeleteLocalRef(bytes); return nullptr; }
    jstring charset = env->NewStringUTF("UTF-8");
    jstring result = static_cast<jstring>(env->NewObject(cls, ctor, bytes, charset));
    env->DeleteLocalRef(charset); env->DeleteLocalRef(cls); env->DeleteLocalRef(bytes);
    return result;
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_tanvir_m12poc_NativeBridge_resolveDependencies(JNIEnv* env, jclass, jstring root, jstring local, jstring refs) {
    if (env == nullptr || root == nullptr || local == nullptr || refs == nullptr)
        return toJava(env, "M13 JNI ERROR: Missing dependency resolver path.");
    const char* r = env->GetStringUTFChars(root, nullptr);
    const char* l = env->GetStringUTFChars(local, nullptr);
    const char* f = env->GetStringUTFChars(refs, nullptr);
    if (r == nullptr || l == nullptr || f == nullptr) {
        if (f != nullptr) env->ReleaseStringUTFChars(refs, f);
        if (l != nullptr) env->ReleaseStringUTFChars(local, l);
        if (r != nullptr) env->ReleaseStringUTFChars(root, r);
        return toJava(env, "M13 JNI ERROR: Cannot read dependency resolver path.");
    }
    void* result = DroidInspect_M12_ResolveDependencies(r, l, f);
    env->ReleaseStringUTFChars(refs, f);
    env->ReleaseStringUTFChars(local, l);
    env->ReleaseStringUTFChars(root, r);
    if (result == nullptr) return toJava(env, "M13 native dependency resolver returned no result.");
    jstring text = toJava(env, static_cast<const char*>(result));
    DroidInspect_M12_FreeResult(result);
    return text;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_tanvir_m12poc_NativeBridge_compile(JNIEnv* env, jclass, jstring source, jstring output, jstring refs, jstring original) {
    if (env == nullptr || source == nullptr || output == nullptr || refs == nullptr || original == nullptr)
        return toJava(env, "M12 JNI ERROR: Missing compiler path.");
    const char* s = env->GetStringUTFChars(source, nullptr);
    const char* o = env->GetStringUTFChars(output, nullptr);
    const char* r = env->GetStringUTFChars(refs, nullptr);
    const char* a = env->GetStringUTFChars(original, nullptr);
    if (s == nullptr || o == nullptr || r == nullptr || a == nullptr)
        return toJava(env, "M12 JNI ERROR: Cannot read path.");
    void* result = DroidInspect_M12_Compile(s, o, r, a);
    env->ReleaseStringUTFChars(original, a);
    env->ReleaseStringUTFChars(refs, r);
    env->ReleaseStringUTFChars(output, o);
    env->ReleaseStringUTFChars(source, s);
    if (result == nullptr) return toJava(env, "M12 native compiler returned an empty result.");
    jstring text = toJava(env, static_cast<const char*>(result));
    DroidInspect_M12_FreeResult(result);
    return text;
}
