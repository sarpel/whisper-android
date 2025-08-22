package com.app.whisper.data.model

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Data class representing captured audio data with metadata.
 * 
 * This class encapsulates raw audio samples along with their metadata,
 * providing utilities for audio analysis and processing.
 */
data class AudioData(
    val samples: FloatArray,
    val sampleRate: Int,
    val channelCount: Int = 1,
    val timestampMs: Long = System.currentTimeMillis(),
    val sequenceNumber: Long = 0L
) {
    
    /**
     * Get the duration of this audio chunk in milliseconds.
     * 
     * @return Duration in milliseconds
     */
    fun getDurationMs(): Long = (samples.size * 1000L) / sampleRate
    
    /**
     * Get the duration of this audio chunk in seconds.
     * 
     * @return Duration in seconds
     */
    fun getDurationSeconds(): Float = samples.size.toFloat() / sampleRate
    
    /**
     * Calculate the RMS (Root Mean Square) energy of the audio signal.
     * This is useful for volume level indication and voice activity detection.
     * 
     * @return RMS energy value (0.0 to 1.0 range typically)
     */
    fun calculateRMS(): Float {
        if (samples.isEmpty()) return 0.0f
        
        var sumSquares = 0.0
        for (sample in samples) {
            sumSquares += sample * sample
        }
        return sqrt(sumSquares / samples.size).toFloat()
    }
    
    /**
     * Calculate the peak amplitude of the audio signal.
     * 
     * @return Peak amplitude (0.0 to 1.0 range typically)
     */
    fun calculatePeak(): Float {
        if (samples.isEmpty()) return 0.0f
        
        var peak = 0.0f
        for (sample in samples) {
            val abs = abs(sample)
            if (abs > peak) {
                peak = abs
            }
        }
        return peak
    }
    
    /**
     * Check if this audio chunk contains significant audio activity.
     * 
     * @param threshold RMS threshold for activity detection (default: 0.01)
     * @return true if audio activity is detected
     */
    fun hasActivity(threshold: Float = 0.01f): Boolean {
        return calculateRMS() > threshold
    }
    
    /**
     * Check if the audio data is valid (not empty, no NaN/Infinite values).
     * 
     * @return true if audio data is valid
     */
    fun isValid(): Boolean {
        if (samples.isEmpty()) return false
        if (sampleRate <= 0) return false
        if (channelCount <= 0) return false
        
        // Check for invalid sample values
        for (sample in samples) {
            if (sample.isNaN() || sample.isInfinite()) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Get audio statistics for debugging and monitoring.
     * 
     * @return Map containing various audio statistics
     */
    fun getStatistics(): Map<String, Float> {
        val rms = calculateRMS()
        val peak = calculatePeak()
        val mean = if (samples.isNotEmpty()) samples.average().toFloat() else 0.0f
        val min = samples.minOrNull() ?: 0.0f
        val max = samples.maxOrNull() ?: 0.0f
        
        return mapOf(
            "rms" to rms,
            "peak" to peak,
            "mean" to mean,
            "min" to min,
            "max" to max,
            "samples" to samples.size.toFloat(),
            "duration_ms" to getDurationMs().toFloat(),
            "sample_rate" to sampleRate.toFloat(),
            "channels" to channelCount.toFloat()
        )
    }
    
    /**
     * Create a copy of this AudioData with different samples.
     * 
     * @param newSamples New audio samples
     * @return New AudioData instance with updated samples
     */
    fun withSamples(newSamples: FloatArray): AudioData = copy(samples = newSamples)
    
    /**
     * Create a copy of this AudioData with a different sample rate.
     * Note: This doesn't resample the audio, just changes the metadata.
     * 
     * @param newSampleRate New sample rate
     * @return New AudioData instance with updated sample rate
     */
    fun withSampleRate(newSampleRate: Int): AudioData = copy(sampleRate = newSampleRate)
    
    /**
     * Combine this AudioData with another AudioData.
     * Both must have the same sample rate and channel count.
     * 
     * @param other AudioData to combine with
     * @return New AudioData with combined samples, or null if incompatible
     */
    fun combineWith(other: AudioData): AudioData? {
        if (sampleRate != other.sampleRate || channelCount != other.channelCount) {
            return null
        }
        
        val combinedSamples = FloatArray(samples.size + other.samples.size)
        samples.copyInto(combinedSamples, 0)
        other.samples.copyInto(combinedSamples, samples.size)
        
        return copy(
            samples = combinedSamples,
            timestampMs = minOf(timestampMs, other.timestampMs),
            sequenceNumber = minOf(sequenceNumber, other.sequenceNumber)
        )
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as AudioData
        
        if (!samples.contentEquals(other.samples)) return false
        if (sampleRate != other.sampleRate) return false
        if (channelCount != other.channelCount) return false
        if (timestampMs != other.timestampMs) return false
        if (sequenceNumber != other.sequenceNumber) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + channelCount
        result = 31 * result + timestampMs.hashCode()
        result = 31 * result + sequenceNumber.hashCode()
        return result
    }
    
    override fun toString(): String {
        return "AudioData(samples=${samples.size}, sampleRate=$sampleRate, " +
                "channels=$channelCount, duration=${getDurationMs()}ms, " +
                "rms=${String.format("%.4f", calculateRMS())}, " +
                "peak=${String.format("%.4f", calculatePeak())})"
    }
    
    companion object {
        /**
         * Create an empty AudioData instance.
         * 
         * @param sampleRate Sample rate for the empty data
         * @param channelCount Number of channels
         * @return Empty AudioData instance
         */
        fun empty(sampleRate: Int = 16000, channelCount: Int = 1): AudioData =
            AudioData(
                samples = FloatArray(0),
                sampleRate = sampleRate,
                channelCount = channelCount
            )
        
        /**
         * Create AudioData from PCM16 samples.
         * 
         * @param pcmSamples 16-bit PCM samples
         * @param sampleRate Sample rate
         * @param channelCount Number of channels
         * @return AudioData with converted float samples
         */
        fun fromPCM16(
            pcmSamples: ShortArray,
            sampleRate: Int,
            channelCount: Int = 1
        ): AudioData {
            val floatSamples = FloatArray(pcmSamples.size)
            val scale = 1.0f / 32768.0f
            
            for (i in pcmSamples.indices) {
                floatSamples[i] = pcmSamples[i] * scale
            }
            
            return AudioData(
                samples = floatSamples,
                sampleRate = sampleRate,
                channelCount = channelCount
            )
        }
        
        /**
         * Create AudioData with silence.
         * 
         * @param durationMs Duration of silence in milliseconds
         * @param sampleRate Sample rate
         * @param channelCount Number of channels
         * @return AudioData filled with silence
         */
        fun silence(
            durationMs: Long,
            sampleRate: Int = 16000,
            channelCount: Int = 1
        ): AudioData {
            val sampleCount = ((durationMs * sampleRate) / 1000).toInt()
            return AudioData(
                samples = FloatArray(sampleCount) { 0.0f },
                sampleRate = sampleRate,
                channelCount = channelCount
            )
        }
    }
}
