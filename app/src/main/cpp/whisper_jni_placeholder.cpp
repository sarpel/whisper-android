#include <jni.h>
#include <android/log.h>
#include <string>

#define LOG_TAG "WhisperJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

/**
 * Placeholder JNI function for testing native build.
 * This will be replaced with actual Whisper JNI implementation once
 * the Accelerate framework issue is resolved.
 */
JNIEXPORT jstring JNICALL
Java_com_app_whisper_native_WhisperNative_getVersionInfo(
    JNIEnv* env,
    jobject /* this */) {
    
    LOGI("WhisperJNI placeholder called");
    
    std::string version = "Whisper Android v1.0.0 - Placeholder Native Layer";
    return env->NewStringUTF(version.c_str());
}

/**
 * Placeholder function for context initialization.
 */
JNIEXPORT jlong JNICALL
Java_com_app_whisper_native_WhisperNative_initContext(
    JNIEnv* env,
    jobject /* this */,
    jstring model_path,
    jint n_threads) {
    
    LOGI("Placeholder initContext called");
    
    // Return dummy context pointer for now
    return reinterpret_cast<jlong>(nullptr);
}

/**
 * Placeholder function for audio transcription.
 */
JNIEXPORT jstring JNICALL
Java_com_app_whisper_native_WhisperNative_transcribeAudio(
    JNIEnv* env,
    jobject /* this */,
    jlong context_ptr,
    jfloatArray audio_data,
    jint sample_rate,
    jstring language,
    jboolean translate) {
    
    LOGI("Placeholder transcribeAudio called");
    
    std::string result = "Placeholder transcription result - whisper.cpp integration pending";
    return env->NewStringUTF(result.c_str());
}

/**
 * Placeholder function for context cleanup.
 */
JNIEXPORT void JNICALL
Java_com_app_whisper_native_WhisperNative_releaseContext(
    JNIEnv* env,
    jobject /* this */,
    jlong context_ptr) {
    
    LOGI("Placeholder releaseContext called");
    
    // Nothing to release in placeholder
}

/**
 * Placeholder function for model information.
 */
JNIEXPORT jstring JNICALL
Java_com_app_whisper_native_WhisperNative_getModelInfo(
    JNIEnv* env,
    jobject /* this */,
    jlong context_ptr) {
    
    LOGI("Placeholder getModelInfo called");
    
    std::string info = "Placeholder model info - no model loaded";
    return env->NewStringUTF(info.c_str());
}

/**
 * Placeholder function for multilingual check.
 */
JNIEXPORT jboolean JNICALL
Java_com_app_whisper_native_WhisperNative_isMultilingual(
    JNIEnv* env,
    jobject /* this */,
    jlong context_ptr) {
    
    LOGI("Placeholder isMultilingual called");
    
    return JNI_FALSE;  // Placeholder always returns false
}

} // extern "C"
