package com.app.whisper.native

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Native audio processing utilities for Whisper Android.
 *
 * This class provides thread-safe JNI bindings to native audio processing functions
 * optimized for ARM v8 processors. It handles audio format conversion, resampling,
 * filtering, normalization, and audio analysis with comprehensive error handling.
 *
 * All operations are performed on background threads and are safe to call from
 * any thread context.
 */
@Singleton
class AudioProcessor @Inject constructor() {

    companion object {
        private const val TAG = "AudioProcessor"

        const val WHISPER_SAMPLE_RATE = 16000
        const val DEFAULT_HIGH_PASS_CUTOFF = 80.0f // Hz
        const val DEFAULT_NORMALIZE_LEVEL = 0.95f
        const val MIN_AUDIO_LENGTH = 1600 // 0.1 seconds at 16kHz
        const val MAX_AUDIO_LENGTH = 480000 // 30 seconds at 16kHz

        init {
            try {
                System.loadLibrary("whisper-jni")
                Log.i(TAG, "Native audio processing library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native audio processing library", e)
            }
        }
    }

    // Native method declarations
    external fun pcm16ToFloat(pcmData: ShortArray): FloatArray?
    external fun resampleAudio(audioData: FloatArray, sourceRate: Int, targetRate: Int): FloatArray?
    external fun highPassFilter(audioData: FloatArray, cutoffFreq: Float, sampleRate: Int): FloatArray?
    external fun normalizeAudio(audioData: FloatArray, targetLevel: Float): FloatArray?
    external fun calculateRMS(audioData: FloatArray): Float

    /**
     * Convert PCM16 audio data to float array suitable for Whisper.
     * This operation is performed on a background thread.
     *
     * @param pcmData 16-bit signed integer audio samples
     * @return Result containing float array normalized to [-1.0, 1.0] range
     */
    suspend fun convertPcm16ToFloat(pcmData: ShortArray): Result<FloatArray> =
        withContext(Dispatchers.Default) {
            try {
                if (pcmData.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("PCM data is empty")
                    )
                }

                Log.d(TAG, "Converting ${pcmData.size} PCM16 samples to float")

                val result = pcm16ToFloat(pcmData)
                if (result != null) {
                    Log.d(TAG, "PCM16 conversion successful")
                    Result.success(result)
                } else {
                    Log.e(TAG, "PCM16 conversion failed - native function returned null")
                    Result.failure(Exception("PCM16 conversion failed"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during PCM16 conversion", e)
                Result.failure(e)
            }
        }

    /**
     * Resample audio to Whisper's required 16kHz sample rate.
     * This operation is performed on a background thread.
     *
     * @param audioData Input audio samples
     * @param sourceRate Original sample rate
     * @param targetRate Target sample rate (default: 16000 Hz)
     * @return Result containing resampled audio data
     */
    suspend fun resampleToWhisperRate(
        audioData: FloatArray,
        sourceRate: Int,
        targetRate: Int = WHISPER_SAMPLE_RATE
    ): Result<FloatArray> = withContext(Dispatchers.Default) {
        try {
            if (audioData.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Audio data is empty")
                )
            }

            if (sourceRate <= 0 || targetRate <= 0) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid sample rate: source=$sourceRate, target=$targetRate")
                )
            }

            if (sourceRate == targetRate) {
                Log.d(TAG, "No resampling needed (${sourceRate}Hz)")
                return@withContext Result.success(audioData)
            }

            Log.d(TAG, "Resampling audio from ${sourceRate}Hz to ${targetRate}Hz (${audioData.size} samples)")

            val result = resampleAudio(audioData, sourceRate, targetRate)
            if (result != null) {
                Log.d(TAG, "Resampling successful: ${audioData.size} -> ${result.size} samples")
                Result.success(result)
            } else {
                Log.e(TAG, "Resampling failed - native function returned null")
                Result.failure(Exception("Audio resampling failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during resampling", e)
            Result.failure(e)
        }
    }

    /**
     * Apply high-pass filter to remove DC offset and low-frequency noise.
     * This operation is performed on a background thread.
     *
     * @param audioData Input audio samples
     * @param cutoffFreq Cutoff frequency in Hz (default: 80 Hz)
     * @param sampleRate Sample rate of the audio
     * @return Result containing filtered audio data
     */
    suspend fun applyHighPassFilter(
        audioData: FloatArray,
        cutoffFreq: Float = DEFAULT_HIGH_PASS_CUTOFF,
        sampleRate: Int = WHISPER_SAMPLE_RATE
    ): Result<FloatArray> = withContext(Dispatchers.Default) {
        try {
            if (audioData.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Audio data is empty")
                )
            }

            if (cutoffFreq <= 0 || sampleRate <= 0) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid parameters: cutoff=$cutoffFreq, sampleRate=$sampleRate")
                )
            }

            Log.d(TAG, "Applying high-pass filter: cutoff=${cutoffFreq}Hz, sampleRate=${sampleRate}Hz")

            val result = highPassFilter(audioData, cutoffFreq, sampleRate)
            if (result != null) {
                Log.d(TAG, "High-pass filtering successful")
                Result.success(result)
            } else {
                Log.e(TAG, "High-pass filtering failed - native function returned null")
                Result.failure(Exception("High-pass filtering failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during high-pass filtering", e)
            Result.failure(e)
        }
    }

    /**
     * Normalize audio amplitude to prevent clipping and improve transcription quality.
     * This operation is performed on a background thread.
     *
     * @param audioData Input audio samples
     * @param targetLevel Target normalization level (default: 0.95)
     * @return Result containing normalized audio data
     */
    suspend fun normalizeAmplitude(
        audioData: FloatArray,
        targetLevel: Float = DEFAULT_NORMALIZE_LEVEL
    ): Result<FloatArray> = withContext(Dispatchers.Default) {
        try {
            if (audioData.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Audio data is empty")
                )
            }

            if (targetLevel <= 0.0f || targetLevel > 1.0f) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid target level: $targetLevel (must be 0.0 < level <= 1.0)")
                )
            }

            Log.d(TAG, "Normalizing audio amplitude to level $targetLevel")

            val result = normalizeAudio(audioData, targetLevel)
            if (result != null) {
                Log.d(TAG, "Audio normalization successful")
                Result.success(result)
            } else {
                Log.e(TAG, "Audio normalization failed - native function returned null")
                Result.failure(Exception("Audio normalization failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during audio normalization", e)
            Result.failure(e)
        }
    }



    /**
     * Calculate the RMS (Root Mean Square) energy of the audio signal.
     * Useful for voice activity detection and audio level monitoring.
     *
     * @param audioData Input audio samples
     * @return RMS energy value, or 0.0f on error
     */
    fun calculateAudioRMS(audioData: FloatArray): Float {
        return try {
            calculateRMS(audioData)
        } catch (e: Exception) {
            e.printStackTrace()
            0.0f
        }
    }

    /**
     * Complete audio preprocessing pipeline for Whisper transcription.
     * Applies all necessary processing steps in the correct order.
     * This operation is performed on a background thread.
     *
     * @param pcmData Raw PCM16 audio data
     * @param sourceRate Original sample rate
     * @param applyFiltering Whether to apply high-pass filtering
     * @param applyNormalization Whether to normalize amplitude
     * @return Result containing processed audio ready for Whisper
     */
    suspend fun preprocessAudioForWhisper(
        pcmData: ShortArray,
        sourceRate: Int,
        applyFiltering: Boolean = true,
        applyNormalization: Boolean = true
    ): Result<FloatArray> = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Starting audio preprocessing pipeline: ${pcmData.size} samples at ${sourceRate}Hz")

            // Step 1: Convert PCM16 to float
            val floatResult = convertPcm16ToFloat(pcmData)
            if (floatResult.isFailure) {
                return@withContext Result.failure(
                    Exception("PCM16 conversion failed", floatResult.exceptionOrNull())
                )
            }
            var audioData = floatResult.getOrNull() ?: return@withContext Result.failure(
                Exception("PCM16 conversion returned null")
            )

            // Step 2: Resample to 16kHz if needed
            if (sourceRate != WHISPER_SAMPLE_RATE) {
                val resampleResult = resampleToWhisperRate(audioData, sourceRate)
                if (resampleResult.isFailure) {
                    return@withContext Result.failure(
                        Exception("Resampling failed", resampleResult.exceptionOrNull())
                    )
                }
                audioData = resampleResult.getOrNull() ?: return@withContext Result.failure(
                    Exception("Resampling returned null")
                )
            }

            // Step 3: Apply high-pass filter to remove low-frequency noise
            if (applyFiltering) {
                val filterResult = applyHighPassFilter(audioData)
                if (filterResult.isFailure) {
                    Log.w(TAG, "High-pass filtering failed, continuing without filtering",
                          filterResult.exceptionOrNull())
                    // Continue without filtering rather than failing
                } else {
                    filterResult.getOrNull()?.let { audioData = it }
                }
            }

            // Step 4: Normalize amplitude
            if (applyNormalization) {
                val normalizeResult = normalizeAmplitude(audioData)
                if (normalizeResult.isFailure) {
                    Log.w(TAG, "Normalization failed, continuing without normalization",
                          normalizeResult.exceptionOrNull())
                    // Continue without normalization rather than failing
                } else {
                    normalizeResult.getOrNull()?.let { audioData = it }
                }
            }

            Log.d(TAG, "Audio preprocessing completed successfully: ${audioData.size} samples")
            Result.success(audioData)

        } catch (e: Exception) {
            Log.e(TAG, "Exception during audio preprocessing", e)
            Result.failure(e)
        }
    }

    /**
     * Check if audio contains sufficient energy for transcription.
     *
     * @param audioData Audio samples to analyze
     * @param threshold Minimum RMS threshold (default: 0.01)
     * @return true if audio has sufficient energy
     */
    fun hasAudioActivity(audioData: FloatArray, threshold: Float = 0.01f): Boolean {
        val rms = calculateAudioRMS(audioData)
        return rms > threshold
    }

    /**
     * Get audio statistics for debugging and monitoring.
     *
     * @param audioData Audio samples to analyze
     * @return Map containing audio statistics
     */
    fun getAudioStatistics(audioData: FloatArray): Map<String, Float> {
        val rms = calculateAudioRMS(audioData)
        val max = audioData.maxOrNull() ?: 0.0f
        val min = audioData.minOrNull() ?: 0.0f
        val mean = audioData.average().toFloat()

        return mapOf(
            "rms" to rms,
            "max" to max,
            "min" to min,
            "mean" to mean,
            "length" to audioData.size.toFloat(),
            "duration_seconds" to (audioData.size.toFloat() / WHISPER_SAMPLE_RATE)
        )
    }
}
