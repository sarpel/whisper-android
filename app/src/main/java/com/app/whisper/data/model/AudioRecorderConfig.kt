package com.app.whisper.data.model

import android.media.AudioFormat
import android.media.MediaRecorder

/**
 * Configuration data class for audio recording settings.
 * 
 * This class encapsulates all the configuration parameters needed for
 * audio recording, providing sensible defaults optimized for speech
 * recognition with Whisper.
 */
data class AudioRecorderConfig(
    val sampleRate: Int = 16000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val bufferSizeMultiplier: Int = 2,
    val enableNoiseReduction: Boolean = true,
    val enableEchoCancellation: Boolean = true,
    val enableAutomaticGainControl: Boolean = true,
    val maxRecordingDurationMs: Long = 30 * 60 * 1000L, // 30 minutes
    val waveformWindowSizeMs: Int = 50,
    val voiceActivityThreshold: Float = 0.02f,
    val silenceTimeoutMs: Long = 3000L, // 3 seconds of silence
    val enableVoiceActivityDetection: Boolean = false
) {
    
    /**
     * Get the number of channels based on channel configuration.
     * 
     * @return Number of audio channels
     */
    fun getChannelCount(): Int = when (channelConfig) {
        AudioFormat.CHANNEL_IN_MONO -> 1
        AudioFormat.CHANNEL_IN_STEREO -> 2
        else -> 1
    }
    
    /**
     * Get the number of bytes per sample based on audio format.
     * 
     * @return Bytes per sample
     */
    fun getBytesPerSample(): Int = when (audioFormat) {
        AudioFormat.ENCODING_PCM_16BIT -> 2
        AudioFormat.ENCODING_PCM_8BIT -> 1
        AudioFormat.ENCODING_PCM_FLOAT -> 4
        else -> 2
    }
    
    /**
     * Calculate the optimal buffer size for recording.
     * 
     * @return Buffer size in bytes
     */
    fun calculateBufferSize(): Int {
        val minBufferSize = android.media.AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        )
        
        return if (minBufferSize != android.media.AudioRecord.ERROR_BAD_VALUE) {
            minBufferSize * bufferSizeMultiplier
        } else {
            // Fallback calculation
            val bytesPerSecond = sampleRate * getChannelCount() * getBytesPerSample()
            (bytesPerSecond * 0.1).toInt() // 100ms buffer
        }
    }
    
    /**
     * Calculate the number of samples per buffer.
     * 
     * @return Number of samples per buffer
     */
    fun getSamplesPerBuffer(): Int {
        val bufferSize = calculateBufferSize()
        return bufferSize / (getChannelCount() * getBytesPerSample())
    }
    
    /**
     * Get the duration of one buffer in milliseconds.
     * 
     * @return Buffer duration in milliseconds
     */
    fun getBufferDurationMs(): Long {
        val samplesPerBuffer = getSamplesPerBuffer()
        return (samplesPerBuffer * 1000L) / sampleRate
    }
    
    /**
     * Validate the configuration parameters.
     * 
     * @return Result indicating validation success or failure
     */
    fun validate(): Result<Unit> {
        return when {
            sampleRate <= 0 -> 
                Result.failure(IllegalArgumentException("Sample rate must be positive: $sampleRate"))
            
            sampleRate < 8000 || sampleRate > 48000 -> 
                Result.failure(IllegalArgumentException("Sample rate out of range: $sampleRate (8000-48000)"))
            
            bufferSizeMultiplier <= 0 -> 
                Result.failure(IllegalArgumentException("Buffer size multiplier must be positive: $bufferSizeMultiplier"))
            
            maxRecordingDurationMs <= 0 -> 
                Result.failure(IllegalArgumentException("Max recording duration must be positive: $maxRecordingDurationMs"))
            
            waveformWindowSizeMs <= 0 -> 
                Result.failure(IllegalArgumentException("Waveform window size must be positive: $waveformWindowSizeMs"))
            
            voiceActivityThreshold < 0.0f || voiceActivityThreshold > 1.0f -> 
                Result.failure(IllegalArgumentException("Voice activity threshold must be 0.0-1.0: $voiceActivityThreshold"))
            
            silenceTimeoutMs < 0 -> 
                Result.failure(IllegalArgumentException("Silence timeout must be non-negative: $silenceTimeoutMs"))
            
            calculateBufferSize() == android.media.AudioRecord.ERROR_BAD_VALUE -> 
                Result.failure(IllegalArgumentException("Invalid audio configuration"))
            
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Check if this configuration is compatible with Whisper requirements.
     * 
     * @return true if compatible with Whisper
     */
    fun isWhisperCompatible(): Boolean {
        return sampleRate == 16000 && 
               channelConfig == AudioFormat.CHANNEL_IN_MONO &&
               audioFormat == AudioFormat.ENCODING_PCM_16BIT
    }
    
    /**
     * Get a human-readable description of the configuration.
     * 
     * @return Configuration description string
     */
    fun getDescription(): String = buildString {
        append("AudioConfig(")
        append("${sampleRate}Hz, ")
        append("${getChannelCount()}ch, ")
        append("${getBytesPerSample() * 8}bit, ")
        append("buffer=${getBufferDurationMs()}ms")
        if (enableNoiseReduction) append(", NR")
        if (enableEchoCancellation) append(", EC")
        if (enableAutomaticGainControl) append(", AGC")
        if (enableVoiceActivityDetection) append(", VAD")
        append(")")
    }
    
    companion object {
        /**
         * Create a configuration optimized for Whisper speech recognition.
         * 
         * @return AudioRecorderConfig optimized for Whisper
         */
        fun forWhisper(): AudioRecorderConfig = AudioRecorderConfig(
            sampleRate = 16000,
            channelConfig = AudioFormat.CHANNEL_IN_MONO,
            audioFormat = AudioFormat.ENCODING_PCM_16BIT,
            audioSource = MediaRecorder.AudioSource.MIC,
            bufferSizeMultiplier = 2,
            enableNoiseReduction = true,
            enableEchoCancellation = true,
            enableAutomaticGainControl = true
        )
        
        /**
         * Create a high-quality configuration for music or high-fidelity recording.
         * 
         * @return AudioRecorderConfig for high-quality recording
         */
        fun highQuality(): AudioRecorderConfig = AudioRecorderConfig(
            sampleRate = 44100,
            channelConfig = AudioFormat.CHANNEL_IN_STEREO,
            audioFormat = AudioFormat.ENCODING_PCM_16BIT,
            audioSource = MediaRecorder.AudioSource.MIC,
            bufferSizeMultiplier = 4,
            enableNoiseReduction = false,
            enableEchoCancellation = false,
            enableAutomaticGainControl = false
        )
        
        /**
         * Create a low-latency configuration for real-time applications.
         * 
         * @return AudioRecorderConfig for low-latency recording
         */
        fun lowLatency(): AudioRecorderConfig = AudioRecorderConfig(
            sampleRate = 16000,
            channelConfig = AudioFormat.CHANNEL_IN_MONO,
            audioFormat = AudioFormat.ENCODING_PCM_16BIT,
            audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            bufferSizeMultiplier = 1,
            enableNoiseReduction = true,
            enableEchoCancellation = true,
            enableAutomaticGainControl = true
        )
        
        /**
         * Create a configuration for voice calls or communication.
         * 
         * @return AudioRecorderConfig optimized for voice communication
         */
        fun forVoiceCall(): AudioRecorderConfig = AudioRecorderConfig(
            sampleRate = 8000,
            channelConfig = AudioFormat.CHANNEL_IN_MONO,
            audioFormat = AudioFormat.ENCODING_PCM_16BIT,
            audioSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            bufferSizeMultiplier = 2,
            enableNoiseReduction = true,
            enableEchoCancellation = true,
            enableAutomaticGainControl = true
        )
        
        /**
         * Create a configuration with custom parameters.
         * 
         * @param sampleRate Sample rate in Hz
         * @param channels Number of channels (1 or 2)
         * @param bitDepth Bit depth (8 or 16)
         * @return Custom AudioRecorderConfig
         */
        fun custom(
            sampleRate: Int,
            channels: Int = 1,
            bitDepth: Int = 16
        ): AudioRecorderConfig {
            val channelConfig = if (channels == 2) {
                AudioFormat.CHANNEL_IN_STEREO
            } else {
                AudioFormat.CHANNEL_IN_MONO
            }
            
            val audioFormat = if (bitDepth == 8) {
                AudioFormat.ENCODING_PCM_8BIT
            } else {
                AudioFormat.ENCODING_PCM_16BIT
            }
            
            return AudioRecorderConfig(
                sampleRate = sampleRate,
                channelConfig = channelConfig,
                audioFormat = audioFormat
            )
        }
    }
}
