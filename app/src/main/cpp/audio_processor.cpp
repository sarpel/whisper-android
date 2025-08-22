#include <jni.h>
#include <android/log.h>
#include <vector>
#include <algorithm>
#include <cmath>
#include <cstring>

#define LOG_TAG "AudioProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" {

/**
 * Convert PCM16 audio data to float array
 * Input: 16-bit signed integers (-32768 to 32767)
 * Output: float array (-1.0 to 1.0)
 */
JNIEXPORT jfloatArray JNICALL
Java_com_app_whisper_native_AudioProcessor_pcm16ToFloat(
    JNIEnv* env,
    jobject /* this */,
    jshortArray pcm_data) {
    
    jsize length = env->GetArrayLength(pcm_data);
    jshort* pcm = env->GetShortArrayElements(pcm_data, nullptr);
    
    if (pcm == nullptr) {
        LOGE("Failed to get PCM data");
        return nullptr;
    }
    
    // Create float array
    jfloatArray float_array = env->NewFloatArray(length);
    if (float_array == nullptr) {
        env->ReleaseShortArrayElements(pcm_data, pcm, JNI_ABORT);
        LOGE("Failed to create float array");
        return nullptr;
    }
    
    jfloat* float_data = env->GetFloatArrayElements(float_array, nullptr);
    if (float_data == nullptr) {
        env->ReleaseShortArrayElements(pcm_data, pcm, JNI_ABORT);
        LOGE("Failed to get float array elements");
        return nullptr;
    }
    
    // Convert PCM16 to float
    const float scale = 1.0f / 32768.0f;
    for (jsize i = 0; i < length; ++i) {
        float_data[i] = static_cast<float>(pcm[i]) * scale;
    }
    
    // Release arrays
    env->ReleaseFloatArrayElements(float_array, float_data, 0);
    env->ReleaseShortArrayElements(pcm_data, pcm, JNI_ABORT);
    
    LOGD("Converted %d PCM16 samples to float", length);
    return float_array;
}

/**
 * Resample audio data to target sample rate
 * Simple linear interpolation resampling
 */
JNIEXPORT jfloatArray JNICALL
Java_com_app_whisper_native_AudioProcessor_resampleAudio(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray audio_data,
    jint source_rate,
    jint target_rate) {
    
    if (source_rate == target_rate) {
        // No resampling needed, return copy
        return audio_data;
    }
    
    jsize source_length = env->GetArrayLength(audio_data);
    jfloat* source_audio = env->GetFloatArrayElements(audio_data, nullptr);
    
    if (source_audio == nullptr) {
        LOGE("Failed to get source audio data");
        return nullptr;
    }
    
    // Calculate target length
    double ratio = static_cast<double>(target_rate) / static_cast<double>(source_rate);
    jsize target_length = static_cast<jsize>(source_length * ratio);
    
    // Create target array
    jfloatArray target_array = env->NewFloatArray(target_length);
    if (target_array == nullptr) {
        env->ReleaseFloatArrayElements(audio_data, source_audio, JNI_ABORT);
        LOGE("Failed to create target array");
        return nullptr;
    }
    
    jfloat* target_audio = env->GetFloatArrayElements(target_array, nullptr);
    if (target_audio == nullptr) {
        env->ReleaseFloatArrayElements(audio_data, source_audio, JNI_ABORT);
        LOGE("Failed to get target array elements");
        return nullptr;
    }
    
    // Linear interpolation resampling
    for (jsize i = 0; i < target_length; ++i) {
        double source_index = static_cast<double>(i) / ratio;
        jsize index = static_cast<jsize>(source_index);
        double fraction = source_index - index;
        
        if (index >= source_length - 1) {
            target_audio[i] = source_audio[source_length - 1];
        } else {
            // Linear interpolation
            target_audio[i] = source_audio[index] * (1.0f - fraction) + 
                             source_audio[index + 1] * fraction;
        }
    }
    
    // Release arrays
    env->ReleaseFloatArrayElements(target_array, target_audio, 0);
    env->ReleaseFloatArrayElements(audio_data, source_audio, JNI_ABORT);
    
    LOGI("Resampled audio from %d Hz to %d Hz (%d -> %d samples)", 
         source_rate, target_rate, source_length, target_length);
    
    return target_array;
}

/**
 * Apply high-pass filter to remove DC offset and low-frequency noise
 */
JNIEXPORT jfloatArray JNICALL
Java_com_app_whisper_native_AudioProcessor_highPassFilter(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray audio_data,
    jfloat cutoff_freq,
    jint sample_rate) {
    
    jsize length = env->GetArrayLength(audio_data);
    jfloat* audio = env->GetFloatArrayElements(audio_data, nullptr);
    
    if (audio == nullptr) {
        LOGE("Failed to get audio data");
        return nullptr;
    }
    
    // Create output array
    jfloatArray filtered_array = env->NewFloatArray(length);
    if (filtered_array == nullptr) {
        env->ReleaseFloatArrayElements(audio_data, audio, JNI_ABORT);
        LOGE("Failed to create filtered array");
        return nullptr;
    }
    
    jfloat* filtered = env->GetFloatArrayElements(filtered_array, nullptr);
    if (filtered == nullptr) {
        env->ReleaseFloatArrayElements(audio_data, audio, JNI_ABORT);
        LOGE("Failed to get filtered array elements");
        return nullptr;
    }
    
    // Simple high-pass filter (first-order)
    const float dt = 1.0f / sample_rate;
    const float rc = 1.0f / (2.0f * M_PI * cutoff_freq);
    const float alpha = rc / (rc + dt);
    
    filtered[0] = audio[0];
    for (jsize i = 1; i < length; ++i) {
        filtered[i] = alpha * (filtered[i-1] + audio[i] - audio[i-1]);
    }
    
    // Release arrays
    env->ReleaseFloatArrayElements(filtered_array, filtered, 0);
    env->ReleaseFloatArrayElements(audio_data, audio, JNI_ABORT);
    
    LOGD("Applied high-pass filter with cutoff %.1f Hz", cutoff_freq);
    return filtered_array;
}

/**
 * Normalize audio amplitude to prevent clipping
 */
JNIEXPORT jfloatArray JNICALL
Java_com_app_whisper_native_AudioProcessor_normalizeAudio(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray audio_data,
    jfloat target_level) {
    
    jsize length = env->GetArrayLength(audio_data);
    jfloat* audio = env->GetFloatArrayElements(audio_data, nullptr);
    
    if (audio == nullptr) {
        LOGE("Failed to get audio data");
        return nullptr;
    }
    
    // Find maximum absolute value
    float max_val = 0.0f;
    for (jsize i = 0; i < length; ++i) {
        float abs_val = std::abs(audio[i]);
        if (abs_val > max_val) {
            max_val = abs_val;
        }
    }
    
    if (max_val == 0.0f) {
        // Silent audio, return as-is
        env->ReleaseFloatArrayElements(audio_data, audio, JNI_ABORT);
        return audio_data;
    }
    
    // Create normalized array
    jfloatArray normalized_array = env->NewFloatArray(length);
    if (normalized_array == nullptr) {
        env->ReleaseFloatArrayElements(audio_data, audio, JNI_ABORT);
        LOGE("Failed to create normalized array");
        return nullptr;
    }
    
    jfloat* normalized = env->GetFloatArrayElements(normalized_array, nullptr);
    if (normalized == nullptr) {
        env->ReleaseFloatArrayElements(audio_data, audio, JNI_ABORT);
        LOGE("Failed to get normalized array elements");
        return nullptr;
    }
    
    // Normalize to target level
    float scale = target_level / max_val;
    for (jsize i = 0; i < length; ++i) {
        normalized[i] = audio[i] * scale;
    }
    
    // Release arrays
    env->ReleaseFloatArrayElements(normalized_array, normalized, 0);
    env->ReleaseFloatArrayElements(audio_data, audio, JNI_ABORT);
    
    LOGD("Normalized audio: max %.3f -> %.3f (scale: %.3f)", max_val, target_level, scale);
    return normalized_array;
}

/**
 * Calculate RMS (Root Mean Square) energy of audio signal
 */
JNIEXPORT jfloat JNICALL
Java_com_app_whisper_native_AudioProcessor_calculateRMS(
    JNIEnv* env,
    jobject /* this */,
    jfloatArray audio_data) {
    
    jsize length = env->GetArrayLength(audio_data);
    jfloat* audio = env->GetFloatArrayElements(audio_data, nullptr);
    
    if (audio == nullptr) {
        LOGE("Failed to get audio data");
        return 0.0f;
    }
    
    double sum_squares = 0.0;
    for (jsize i = 0; i < length; ++i) {
        sum_squares += audio[i] * audio[i];
    }
    
    float rms = std::sqrt(sum_squares / length);
    
    env->ReleaseFloatArrayElements(audio_data, audio, JNI_ABORT);
    
    return rms;
}

} // extern "C"
