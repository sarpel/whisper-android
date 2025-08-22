package com.app.whisper.data.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Data class representing waveform visualization data.
 * 
 * This class provides processed audio data suitable for waveform visualization,
 * including amplitude levels, RMS values, and peak detection for creating
 * visual representations of audio signals.
 */
data class WaveformData(
    val amplitudes: FloatArray,
    val rmsValues: FloatArray,
    val peakValues: FloatArray,
    val timestampMs: Long = System.currentTimeMillis(),
    val sampleRate: Int = 16000,
    val windowSizeMs: Int = 50 // Size of each amplitude window in milliseconds
) {
    
    /**
     * Get the total duration represented by this waveform data.
     * 
     * @return Duration in milliseconds
     */
    fun getTotalDurationMs(): Long = amplitudes.size * windowSizeMs.toLong()
    
    /**
     * Get the number of waveform points.
     * 
     * @return Number of amplitude points
     */
    fun getPointCount(): Int = amplitudes.size
    
    /**
     * Get the maximum amplitude value in this waveform.
     * 
     * @return Maximum amplitude (0.0 to 1.0 typically)
     */
    fun getMaxAmplitude(): Float = amplitudes.maxOrNull() ?: 0.0f
    
    /**
     * Get the average amplitude across the entire waveform.
     * 
     * @return Average amplitude
     */
    fun getAverageAmplitude(): Float = 
        if (amplitudes.isNotEmpty()) amplitudes.average().toFloat() else 0.0f
    
    /**
     * Get the maximum RMS value in this waveform.
     * 
     * @return Maximum RMS value
     */
    fun getMaxRMS(): Float = rmsValues.maxOrNull() ?: 0.0f
    
    /**
     * Get the average RMS across the entire waveform.
     * 
     * @return Average RMS value
     */
    fun getAverageRMS(): Float = 
        if (rmsValues.isNotEmpty()) rmsValues.average().toFloat() else 0.0f
    
    /**
     * Detect voice activity based on RMS threshold.
     * 
     * @param threshold RMS threshold for voice activity (default: 0.02)
     * @return Array of boolean values indicating voice activity for each window
     */
    fun detectVoiceActivity(threshold: Float = 0.02f): BooleanArray =
        BooleanArray(rmsValues.size) { i -> rmsValues[i] > threshold }
    
    /**
     * Get normalized amplitudes (0.0 to 1.0 range).
     * 
     * @return Normalized amplitude array
     */
    fun getNormalizedAmplitudes(): FloatArray {
        val maxAmp = getMaxAmplitude()
        return if (maxAmp > 0.0f) {
            FloatArray(amplitudes.size) { i -> amplitudes[i] / maxAmp }
        } else {
            amplitudes.copyOf()
        }
    }
    
    /**
     * Get a subset of waveform data for a specific time range.
     * 
     * @param startMs Start time in milliseconds
     * @param endMs End time in milliseconds
     * @return New WaveformData for the specified range, or null if invalid range
     */
    fun getSubset(startMs: Long, endMs: Long): WaveformData? {
        if (startMs < 0 || endMs <= startMs) return null
        
        val startIndex = (startMs / windowSizeMs).toInt()
        val endIndex = min((endMs / windowSizeMs).toInt(), amplitudes.size)
        
        if (startIndex >= amplitudes.size || endIndex <= startIndex) return null
        
        val length = endIndex - startIndex
        return WaveformData(
            amplitudes = amplitudes.copyOfRange(startIndex, endIndex),
            rmsValues = rmsValues.copyOfRange(startIndex, endIndex),
            peakValues = peakValues.copyOfRange(startIndex, endIndex),
            timestampMs = timestampMs + startMs,
            sampleRate = sampleRate,
            windowSizeMs = windowSizeMs
        )
    }
    
    /**
     * Combine this waveform data with another waveform data.
     * Both must have the same sample rate and window size.
     * 
     * @param other WaveformData to combine with
     * @return Combined WaveformData, or null if incompatible
     */
    fun combineWith(other: WaveformData): WaveformData? {
        if (sampleRate != other.sampleRate || windowSizeMs != other.windowSizeMs) {
            return null
        }
        
        return WaveformData(
            amplitudes = amplitudes + other.amplitudes,
            rmsValues = rmsValues + other.rmsValues,
            peakValues = peakValues + other.peakValues,
            timestampMs = min(timestampMs, other.timestampMs),
            sampleRate = sampleRate,
            windowSizeMs = windowSizeMs
        )
    }
    
    /**
     * Downsample the waveform data to reduce the number of points.
     * 
     * @param targetPoints Target number of points
     * @return Downsampled WaveformData
     */
    fun downsample(targetPoints: Int): WaveformData {
        if (targetPoints >= amplitudes.size) return this
        if (targetPoints <= 0) return WaveformData.empty()
        
        val ratio = amplitudes.size.toFloat() / targetPoints
        val newAmplitudes = FloatArray(targetPoints)
        val newRmsValues = FloatArray(targetPoints)
        val newPeakValues = FloatArray(targetPoints)
        
        for (i in 0 until targetPoints) {
            val startIdx = (i * ratio).toInt()
            val endIdx = min(((i + 1) * ratio).toInt(), amplitudes.size)
            
            // Take maximum values in each window for better visualization
            var maxAmp = 0.0f
            var maxRms = 0.0f
            var maxPeak = 0.0f
            
            for (j in startIdx until endIdx) {
                maxAmp = max(maxAmp, amplitudes[j])
                maxRms = max(maxRms, rmsValues[j])
                maxPeak = max(maxPeak, peakValues[j])
            }
            
            newAmplitudes[i] = maxAmp
            newRmsValues[i] = maxRms
            newPeakValues[i] = maxPeak
        }
        
        return WaveformData(
            amplitudes = newAmplitudes,
            rmsValues = newRmsValues,
            peakValues = newPeakValues,
            timestampMs = timestampMs,
            sampleRate = sampleRate,
            windowSizeMs = (windowSizeMs * ratio).toInt()
        )
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as WaveformData
        
        if (!amplitudes.contentEquals(other.amplitudes)) return false
        if (!rmsValues.contentEquals(other.rmsValues)) return false
        if (!peakValues.contentEquals(other.peakValues)) return false
        if (timestampMs != other.timestampMs) return false
        if (sampleRate != other.sampleRate) return false
        if (windowSizeMs != other.windowSizeMs) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = amplitudes.contentHashCode()
        result = 31 * result + rmsValues.contentHashCode()
        result = 31 * result + peakValues.contentHashCode()
        result = 31 * result + timestampMs.hashCode()
        result = 31 * result + sampleRate
        result = 31 * result + windowSizeMs
        return result
    }
    
    override fun toString(): String {
        return "WaveformData(points=${amplitudes.size}, duration=${getTotalDurationMs()}ms, " +
                "maxAmp=${String.format("%.4f", getMaxAmplitude())}, " +
                "avgRms=${String.format("%.4f", getAverageRMS())})"
    }
    
    companion object {
        /**
         * Create empty waveform data.
         * 
         * @return Empty WaveformData instance
         */
        fun empty(): WaveformData = WaveformData(
            amplitudes = FloatArray(0),
            rmsValues = FloatArray(0),
            peakValues = FloatArray(0)
        )
        
        /**
         * Create waveform data from audio data.
         * 
         * @param audioData Source audio data
         * @param windowSizeMs Size of each waveform window in milliseconds
         * @return WaveformData generated from the audio
         */
        fun fromAudioData(audioData: AudioData, windowSizeMs: Int = 50): WaveformData {
            val samplesPerWindow = (audioData.sampleRate * windowSizeMs) / 1000
            val windowCount = (audioData.samples.size + samplesPerWindow - 1) / samplesPerWindow
            
            val amplitudes = FloatArray(windowCount)
            val rmsValues = FloatArray(windowCount)
            val peakValues = FloatArray(windowCount)
            
            for (i in 0 until windowCount) {
                val startIdx = i * samplesPerWindow
                val endIdx = min(startIdx + samplesPerWindow, audioData.samples.size)
                
                var sumSquares = 0.0
                var peak = 0.0f
                var maxAbs = 0.0f
                
                for (j in startIdx until endIdx) {
                    val sample = audioData.samples[j]
                    val absSample = abs(sample)
                    
                    sumSquares += sample * sample
                    peak = max(peak, absSample)
                    maxAbs = max(maxAbs, absSample)
                }
                
                val windowSize = endIdx - startIdx
                rmsValues[i] = if (windowSize > 0) sqrt(sumSquares / windowSize).toFloat() else 0.0f
                peakValues[i] = peak
                amplitudes[i] = maxAbs
            }
            
            return WaveformData(
                amplitudes = amplitudes,
                rmsValues = rmsValues,
                peakValues = peakValues,
                timestampMs = audioData.timestampMs,
                sampleRate = audioData.sampleRate,
                windowSizeMs = windowSizeMs
            )
        }
    }
}
