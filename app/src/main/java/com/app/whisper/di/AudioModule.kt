package com.app.whisper.di

import android.content.Context
import android.media.AudioManager
import com.app.whisper.data.audio.AudioRecorder
import com.app.whisper.data.audio.AudioRecorderImpl
import com.app.whisper.data.audio.AudioProcessor
import com.app.whisper.data.audio.AudioProcessorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing audio-related dependencies.
 * 
 * This module configures audio recording, processing, and system
 * audio management components for the Whisper transcription system.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    
    /**
     * Binds AudioRecorder interface to its implementation.
     * 
     * AudioRecorderImpl provides:
     * - High-quality audio recording
     * - Real-time audio level monitoring
     * - Configurable audio parameters
     * - Recording state management
     */
    @Binds
    @Singleton
    abstract fun bindAudioRecorder(
        audioRecorderImpl: AudioRecorderImpl
    ): AudioRecorder
    
    /**
     * Binds AudioProcessor interface to its implementation.
     * 
     * AudioProcessorImpl provides:
     * - Audio format conversion
     * - Noise reduction and filtering
     * - Audio normalization
     * - Waveform data generation
     */
    @Binds
    @Singleton
    abstract fun bindAudioProcessor(
        audioProcessorImpl: AudioProcessorImpl
    ): AudioProcessor
    
    companion object {
        /**
         * Provides Android AudioManager for system audio operations.
         * 
         * AudioManager is used for:
         * - Audio focus management
         * - Volume control
         * - Audio routing information
         * - Audio device detection
         */
        @Provides
        @Singleton
        fun provideAudioManager(@ApplicationContext context: Context): AudioManager {
            return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        
        /**
         * Provides audio configuration parameters.
         * 
         * These parameters define the default audio recording settings
         * optimized for speech recognition with Whisper models.
         */
        @Provides
        @Singleton
        fun provideAudioConfig(): AudioConfig {
            return AudioConfig(
                sampleRate = 16000, // Whisper's preferred sample rate
                channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO,
                audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeMultiplier = 2, // For stable recording
                enableNoiseReduction = true,
                enableEchoCancellation = true,
                enableAutomaticGainControl = true
            )
        }
    }
}

/**
 * Audio configuration data class.
 * 
 * Contains all audio recording parameters that can be configured
 * based on device capabilities and user preferences.
 */
data class AudioConfig(
    val sampleRate: Int,
    val channelConfig: Int,
    val audioFormat: Int,
    val bufferSizeMultiplier: Int,
    val enableNoiseReduction: Boolean,
    val enableEchoCancellation: Boolean,
    val enableAutomaticGainControl: Boolean
) {
    /**
     * Get the minimum buffer size for audio recording.
     */
    fun getBufferSize(): Int {
        val minBufferSize = android.media.AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        )
        return minBufferSize * bufferSizeMultiplier
    }
    
    /**
     * Check if the audio configuration is valid.
     */
    fun isValid(): Boolean {
        return android.media.AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        ) != android.media.AudioRecord.ERROR_BAD_VALUE
    }
    
    /**
     * Get audio source for recording.
     */
    fun getAudioSource(): Int {
        return when {
            enableNoiseReduction -> android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION
            else -> android.media.MediaRecorder.AudioSource.MIC
        }
    }
    
    /**
     * Get bytes per sample based on audio format.
     */
    fun getBytesPerSample(): Int {
        return when (audioFormat) {
            android.media.AudioFormat.ENCODING_PCM_8BIT -> 1
            android.media.AudioFormat.ENCODING_PCM_16BIT -> 2
            android.media.AudioFormat.ENCODING_PCM_FLOAT -> 4
            else -> 2 // Default to 16-bit
        }
    }
    
    /**
     * Get channel count.
     */
    fun getChannelCount(): Int {
        return when (channelConfig) {
            android.media.AudioFormat.CHANNEL_IN_MONO -> 1
            android.media.AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> 1 // Default to mono
        }
    }
    
    /**
     * Calculate bytes per second for this configuration.
     */
    fun getBytesPerSecond(): Int {
        return sampleRate * getBytesPerSample() * getChannelCount()
    }
    
    /**
     * Create a copy with modified sample rate.
     */
    fun withSampleRate(newSampleRate: Int): AudioConfig {
        return copy(sampleRate = newSampleRate)
    }
    
    /**
     * Create a copy with modified noise reduction setting.
     */
    fun withNoiseReduction(enabled: Boolean): AudioConfig {
        return copy(enableNoiseReduction = enabled)
    }
    
    /**
     * Create a copy optimized for the given use case.
     */
    fun optimizeFor(useCase: AudioUseCase): AudioConfig {
        return when (useCase) {
            AudioUseCase.REAL_TIME -> copy(
                bufferSizeMultiplier = 1,
                enableNoiseReduction = true,
                enableEchoCancellation = true
            )
            AudioUseCase.HIGH_QUALITY -> copy(
                bufferSizeMultiplier = 4,
                enableNoiseReduction = true,
                enableEchoCancellation = true,
                enableAutomaticGainControl = true
            )
            AudioUseCase.LOW_LATENCY -> copy(
                bufferSizeMultiplier = 1,
                enableNoiseReduction = false,
                enableEchoCancellation = false,
                enableAutomaticGainControl = false
            )
        }
    }
}

/**
 * Audio use case enum for configuration optimization.
 */
enum class AudioUseCase {
    REAL_TIME,      // For live transcription
    HIGH_QUALITY,   // For best accuracy
    LOW_LATENCY     // For minimal delay
}
