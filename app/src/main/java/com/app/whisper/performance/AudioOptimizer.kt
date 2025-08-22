package com.app.whisper.performance

import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Build
import androidx.tracing.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Audio processing optimization utilities.
 * 
 * This class provides optimized audio processing algorithms,
 * buffer management, and performance tuning for audio operations.
 */
@Singleton
class AudioOptimizer @Inject constructor(
    private val performanceManager: PerformanceManager
) {
    
    /**
     * Optimize audio buffer size based on device performance.
     */
    suspend fun getOptimalBufferSize(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int
    ): Int = withContext(Dispatchers.Default) {
        trace("AudioOptimizer.getOptimalBufferSize") {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val performanceConfig = performanceManager.optimizeForDevice()
            
            val optimalSize = when {
                minBufferSize == AudioRecord.ERROR_BAD_VALUE -> {
                    Timber.e("Invalid audio configuration")
                    4096 // Fallback
                }
                else -> {
                    // Use performance-based multiplier
                    val multiplier = when (performanceManager.getPerformanceTier()) {
                        PerformanceTier.HIGH -> 4
                        PerformanceTier.MEDIUM -> 2
                        PerformanceTier.LOW -> 1
                    }
                    max(minBufferSize * multiplier, performanceConfig.audioBufferSize)
                }
            }
            
            Timber.d("Optimal buffer size: $optimalSize (min: $minBufferSize)")
            optimalSize
        }
    }
    
    /**
     * Apply noise reduction to audio data.
     */
    suspend fun applyNoiseReduction(
        audioData: ShortArray,
        sampleRate: Int,
        intensity: Float = 0.5f
    ): ShortArray = withContext(Dispatchers.Default) {
        trace("AudioOptimizer.applyNoiseReduction") {
            if (!performanceManager.optimizeForDevice().enableAdvancedFeatures) {
                return@trace audioData // Skip on low-end devices
            }
            
            val result = audioData.copyOf()
            val windowSize = min(512, audioData.size / 4)
            
            // Simple spectral subtraction noise reduction
            for (i in 0 until result.size - windowSize step windowSize / 2) {
                val window = result.sliceArray(i until min(i + windowSize, result.size))
                val processedWindow = processAudioWindow(window, intensity)
                processedWindow.copyInto(result, i)
            }
            
            result
        }
    }
    
    /**
     * Normalize audio levels.
     */
    suspend fun normalizeAudio(
        audioData: ShortArray,
        targetLevel: Float = 0.8f
    ): ShortArray = withContext(Dispatchers.Default) {
        trace("AudioOptimizer.normalizeAudio") {
            val result = audioData.copyOf()
            
            // Find peak amplitude
            val peak = result.maxOfOrNull { abs(it.toInt()) } ?: 1
            if (peak == 0) return@trace result
            
            // Calculate normalization factor
            val normalizationFactor = (Short.MAX_VALUE * targetLevel) / peak
            
            // Apply normalization
            for (i in result.indices) {
                result[i] = (result[i] * normalizationFactor).toInt().coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt()
                ).toShort()
            }
            
            result
        }
    }
    
    /**
     * Generate waveform data optimized for visualization.
     */
    suspend fun generateWaveformData(
        audioData: ShortArray,
        targetPoints: Int = 100
    ): FloatArray = withContext(Dispatchers.Default) {
        trace("AudioOptimizer.generateWaveformData") {
            if (audioData.isEmpty()) return@trace FloatArray(0)
            
            val result = FloatArray(targetPoints)
            val samplesPerPoint = audioData.size / targetPoints
            
            if (samplesPerPoint <= 1) {
                // If we have fewer samples than target points, interpolate
                for (i in result.indices) {
                    val index = (i * audioData.size / targetPoints.toFloat()).toInt()
                        .coerceIn(0, audioData.size - 1)
                    result[i] = abs(audioData[index].toFloat()) / Short.MAX_VALUE
                }
            } else {
                // Downsample by taking RMS of each segment
                for (i in result.indices) {
                    val startIndex = i * samplesPerPoint
                    val endIndex = min(startIndex + samplesPerPoint, audioData.size)
                    
                    var sum = 0.0
                    for (j in startIndex until endIndex) {
                        val sample = audioData[j].toFloat()
                        sum += sample * sample
                    }
                    
                    result[i] = sqrt(sum / (endIndex - startIndex)).toFloat() / Short.MAX_VALUE
                }
            }
            
            result
        }
    }
    
    /**
     * Detect voice activity in audio data.
     */
    suspend fun detectVoiceActivity(
        audioData: ShortArray,
        sampleRate: Int,
        threshold: Float = 0.02f
    ): Boolean = withContext(Dispatchers.Default) {
        trace("AudioOptimizer.detectVoiceActivity") {
            if (audioData.isEmpty()) return@trace false
            
            // Calculate RMS energy
            val rms = sqrt(audioData.map { it.toDouble() * it }.average()).toFloat()
            val normalizedRms = rms / Short.MAX_VALUE
            
            // Simple energy-based VAD
            val hasVoice = normalizedRms > threshold
            
            // Additional zero-crossing rate check for better accuracy
            if (hasVoice && performanceManager.optimizeForDevice().enableAdvancedFeatures) {
                val zcr = calculateZeroCrossingRate(audioData)
                val normalizedZcr = zcr / (sampleRate / 2.0f)
                
                // Voice typically has moderate ZCR (not too high like noise, not too low like silence)
                return@trace normalizedZcr in 0.01f..0.3f
            }
            
            hasVoice
        }
    }
    
    /**
     * Apply automatic gain control.
     */
    suspend fun applyAutomaticGainControl(
        audioData: ShortArray,
        targetLevel: Float = 0.7f,
        attackTime: Float = 0.1f,
        releaseTime: Float = 0.5f,
        sampleRate: Int = 16000
    ): ShortArray = withContext(Dispatchers.Default) {
        trace("AudioOptimizer.applyAutomaticGainControl") {
            if (!performanceManager.optimizeForDevice().enableAdvancedFeatures) {
                return@trace audioData
            }
            
            val result = audioData.copyOf()
            val attackSamples = (attackTime * sampleRate).toInt()
            val releaseSamples = (releaseTime * sampleRate).toInt()
            
            var currentGain = 1.0f
            val windowSize = min(1024, audioData.size / 10)
            
            for (i in 0 until result.size - windowSize step windowSize / 2) {
                val window = result.sliceArray(i until min(i + windowSize, result.size))
                val rms = sqrt(window.map { it.toDouble() * it }.average()).toFloat()
                val normalizedRms = rms / Short.MAX_VALUE
                
                if (normalizedRms > 0.001f) { // Avoid division by zero
                    val targetGain = targetLevel / normalizedRms
                    val clampedTargetGain = targetGain.coerceIn(0.1f, 10.0f)
                    
                    // Smooth gain changes
                    val gainDiff = clampedTargetGain - currentGain
                    val maxChange = if (gainDiff > 0) {
                        1.0f / attackSamples * windowSize
                    } else {
                        1.0f / releaseSamples * windowSize
                    }
                    
                    currentGain += gainDiff.coerceIn(-maxChange, maxChange)
                    
                    // Apply gain to window
                    for (j in window.indices) {
                        val sampleIndex = i + j
                        if (sampleIndex < result.size) {
                            result[sampleIndex] = (result[sampleIndex] * currentGain).toInt()
                                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                .toShort()
                        }
                    }
                }
            }
            
            result
        }
    }
    
    /**
     * Process audio window for noise reduction.
     */
    private fun processAudioWindow(window: ShortArray, intensity: Float): ShortArray {
        val result = window.copyOf()
        
        // Simple high-pass filter to remove low-frequency noise
        if (window.size >= 3) {
            for (i in 1 until window.size - 1) {
                val filtered = (window[i] - 0.5f * (window[i - 1] + window[i + 1]) * intensity).toInt()
                result[i] = filtered.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        
        return result
    }
    
    /**
     * Calculate zero-crossing rate.
     */
    private fun calculateZeroCrossingRate(audioData: ShortArray): Float {
        if (audioData.size < 2) return 0f
        
        var crossings = 0
        for (i in 1 until audioData.size) {
            if ((audioData[i] >= 0) != (audioData[i - 1] >= 0)) {
                crossings++
            }
        }
        
        return crossings.toFloat()
    }
    
    /**
     * Get optimal audio configuration for device.
     */
    suspend fun getOptimalAudioConfig(): OptimalAudioConfig = withContext(Dispatchers.Default) {
        trace("AudioOptimizer.getOptimalAudioConfig") {
            val performanceConfig = performanceManager.optimizeForDevice()
            val tier = performanceManager.getPerformanceTier()
            
            OptimalAudioConfig(
                sampleRate = when (tier) {
                    PerformanceTier.HIGH -> 48000
                    PerformanceTier.MEDIUM -> 44100
                    PerformanceTier.LOW -> 16000
                },
                channelConfig = AudioFormat.CHANNEL_IN_MONO,
                audioFormat = AudioFormat.ENCODING_PCM_16BIT,
                bufferSize = performanceConfig.audioBufferSize,
                enableNoiseReduction = performanceConfig.enableAdvancedFeatures,
                enableAutomaticGainControl = performanceConfig.enableAdvancedFeatures,
                processingThreads = performanceConfig.audioProcessingThreads
            )
        }
    }
}

/**
 * Optimal audio configuration for the device.
 */
data class OptimalAudioConfig(
    val sampleRate: Int,
    val channelConfig: Int,
    val audioFormat: Int,
    val bufferSize: Int,
    val enableNoiseReduction: Boolean,
    val enableAutomaticGainControl: Boolean,
    val processingThreads: Int
)
