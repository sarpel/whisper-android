package com.app.whisper.data.audio

import androidx.tracing.trace
import com.app.whisper.data.model.AudioData
import com.app.whisper.data.model.WaveformData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Implementation of AudioProcessor interface for audio processing operations.
 * 
 * Provides audio format conversion, noise reduction, normalization,
 * and waveform generation optimized for speech recognition.
 */
@Singleton
class AudioProcessorImpl @Inject constructor() : AudioProcessor {

    companion object {
        private const val WHISPER_SAMPLE_RATE = 16000
        private const val WHISPER_CHANNELS = 1
        private const val WHISPER_BIT_DEPTH = 16
        
        // Noise reduction parameters
        private const val NOISE_GATE_THRESHOLD = 0.01f
        private const val NOISE_REDUCTION_FACTOR = 0.3f
        
        // Normalization parameters
        private const val TARGET_RMS = 0.1f
        private const val MAX_GAIN = 10.0f
    }

    override suspend fun convertToWhisperFormat(audioData: AudioData): Result<AudioData> = withContext(Dispatchers.IO) {
        trace("AudioProcessorImpl.convertToWhisperFormat") {
            try {
                Timber.d("Converting audio to Whisper format: ${audioData.sampleRate}Hz -> ${WHISPER_SAMPLE_RATE}Hz")
                
                val inputSamples = audioData.getSamples()
                val convertedSamples = when {
                    audioData.sampleRate == WHISPER_SAMPLE_RATE && audioData.channels == WHISPER_CHANNELS -> {
                        // Already in correct format
                        inputSamples
                    }
                    audioData.sampleRate != WHISPER_SAMPLE_RATE -> {
                        // Resample to 16kHz
                        resampleAudio(inputSamples, audioData.sampleRate, WHISPER_SAMPLE_RATE)
                    }
                    audioData.channels != WHISPER_CHANNELS -> {
                        // Convert to mono
                        convertToMono(inputSamples, audioData.channels)
                    }
                    else -> inputSamples
                }
                
                val convertedAudio = AudioData(
                    samples = convertedSamples,
                    sampleRate = WHISPER_SAMPLE_RATE,
                    channels = WHISPER_CHANNELS,
                    bitDepth = WHISPER_BIT_DEPTH
                )
                
                Timber.d("Audio conversion completed: ${convertedSamples.size} samples")
                Result.success(convertedAudio)
            } catch (e: Exception) {
                Timber.e(e, "Failed to convert audio to Whisper format")
                Result.failure(e)
            }
        }
    }

    override suspend fun normalizeAudio(audioData: AudioData): Result<AudioData> = withContext(Dispatchers.IO) {
        trace("AudioProcessorImpl.normalizeAudio") {
            try {
                val samples = audioData.getSamples()
                val normalizedSamples = normalizeAmplitude(samples)
                
                val normalizedAudio = audioData.copy(samples = normalizedSamples)
                
                Timber.d("Audio normalization completed")
                Result.success(normalizedAudio)
            } catch (e: Exception) {
                Timber.e(e, "Failed to normalize audio")
                Result.failure(e)
            }
        }
    }

    override suspend fun reduceNoise(audioData: AudioData): Result<AudioData> = withContext(Dispatchers.IO) {
        trace("AudioProcessorImpl.reduceNoise") {
            try {
                val samples = audioData.getSamples()
                val denoisedSamples = applyNoiseGate(samples)
                
                val denoisedAudio = audioData.copy(samples = denoisedSamples)
                
                Timber.d("Noise reduction completed")
                Result.success(denoisedAudio)
            } catch (e: Exception) {
                Timber.e(e, "Failed to reduce noise")
                Result.failure(e)
            }
        }
    }

    override suspend fun generateWaveform(audioData: AudioData, targetPoints: Int): Result<WaveformData> = withContext(Dispatchers.IO) {
        trace("AudioProcessorImpl.generateWaveform") {
            try {
                val samples = audioData.getSamples()
                val samplesPerPoint = samples.size / targetPoints
                val waveformPoints = mutableListOf<Float>()
                
                for (i in 0 until targetPoints) {
                    val startIndex = i * samplesPerPoint
                    val endIndex = min(startIndex + samplesPerPoint, samples.size)
                    
                    var maxAmplitude = 0f
                    for (j in startIndex until endIndex) {
                        maxAmplitude = max(maxAmplitude, abs(samples[j]))
                    }
                    
                    waveformPoints.add(maxAmplitude)
                }
                
                val waveformData = WaveformData(
                    points = waveformPoints,
                    sampleRate = audioData.sampleRate,
                    durationMs = audioData.getDurationMs()
                )
                
                Timber.d("Waveform generation completed: ${waveformPoints.size} points")
                Result.success(waveformData)
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate waveform")
                Result.failure(e)
            }
        }
    }

    override suspend fun trimSilence(audioData: AudioData, threshold: Float): Result<AudioData> = withContext(Dispatchers.IO) {
        trace("AudioProcessorImpl.trimSilence") {
            try {
                val samples = audioData.getSamples()
                
                // Find start of audio content
                var startIndex = 0
                for (i in samples.indices) {
                    if (abs(samples[i]) > threshold) {
                        startIndex = i
                        break
                    }
                }
                
                // Find end of audio content
                var endIndex = samples.size - 1
                for (i in samples.size - 1 downTo 0) {
                    if (abs(samples[i]) > threshold) {
                        endIndex = i
                        break
                    }
                }
                
                if (startIndex >= endIndex) {
                    // Audio is all silence
                    return@trace Result.success(audioData.copy(samples = floatArrayOf()))
                }
                
                val trimmedSamples = samples.sliceArray(startIndex..endIndex)
                val trimmedAudio = audioData.copy(samples = trimmedSamples)
                
                Timber.d("Silence trimming completed: ${samples.size} -> ${trimmedSamples.size} samples")
                Result.success(trimmedAudio)
            } catch (e: Exception) {
                Timber.e(e, "Failed to trim silence")
                Result.failure(e)
            }
        }
    }

    override suspend fun saveToFile(audioData: AudioData, outputFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        trace("AudioProcessorImpl.saveToFile") {
            try {
                FileOutputStream(outputFile).use { fos ->
                    // Write WAV header
                    writeWavHeader(fos, audioData)
                    
                    // Write audio data
                    val samples = audioData.getSamples()
                    val byteBuffer = ByteBuffer.allocate(samples.size * 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    
                    for (sample in samples) {
                        val intSample = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        byteBuffer.putShort(intSample.toShort())
                    }
                    
                    fos.write(byteBuffer.array())
                }
                
                Timber.d("Audio saved to file: ${outputFile.absolutePath}")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save audio to file")
                Result.failure(e)
            }
        }
    }

    private fun resampleAudio(samples: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate) return samples
        
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val newLength = (samples.size / ratio).toInt()
        val resampled = FloatArray(newLength)
        
        for (i in resampled.indices) {
            val srcIndex = (i * ratio).toInt()
            if (srcIndex < samples.size) {
                resampled[i] = samples[srcIndex]
            }
        }
        
        return resampled
    }

    private fun convertToMono(samples: FloatArray, channels: Int): FloatArray {
        if (channels == 1) return samples
        
        val monoSamples = FloatArray(samples.size / channels)
        for (i in monoSamples.indices) {
            var sum = 0f
            for (ch in 0 until channels) {
                sum += samples[i * channels + ch]
            }
            monoSamples[i] = sum / channels
        }
        
        return monoSamples
    }

    private fun normalizeAmplitude(samples: FloatArray): FloatArray {
        // Calculate RMS
        var sumSquares = 0.0
        for (sample in samples) {
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / samples.size).toFloat()
        
        if (rms == 0f) return samples
        
        // Calculate gain
        val gain = min(TARGET_RMS / rms, MAX_GAIN)
        
        // Apply gain
        return samples.map { it * gain }.toFloatArray()
    }

    private fun applyNoiseGate(samples: FloatArray): FloatArray {
        return samples.map { sample ->
            if (abs(sample) < NOISE_GATE_THRESHOLD) {
                sample * NOISE_REDUCTION_FACTOR
            } else {
                sample
            }
        }.toFloatArray()
    }

    private fun writeWavHeader(fos: FileOutputStream, audioData: AudioData) {
        val samples = audioData.getSamples()
        val dataSize = samples.size * 2 // 16-bit samples
        val fileSize = dataSize + 36
        
        val header = ByteBuffer.allocate(44)
        header.order(ByteOrder.LITTLE_ENDIAN)
        
        // RIFF header
        header.put("RIFF".toByteArray())
        header.putInt(fileSize)
        header.put("WAVE".toByteArray())
        
        // Format chunk
        header.put("fmt ".toByteArray())
        header.putInt(16) // PCM format chunk size
        header.putShort(1) // PCM format
        header.putShort(audioData.channels.toShort())
        header.putInt(audioData.sampleRate)
        header.putInt(audioData.sampleRate * audioData.channels * 2) // Byte rate
        header.putShort((audioData.channels * 2).toShort()) // Block align
        header.putShort(16) // Bits per sample
        
        // Data chunk
        header.put("data".toByteArray())
        header.putInt(dataSize)
        
        fos.write(header.array())
    }
}

/**
 * Interface for audio processing operations.
 */
interface AudioProcessor {
    suspend fun convertToWhisperFormat(audioData: AudioData): Result<AudioData>
    suspend fun normalizeAudio(audioData: AudioData): Result<AudioData>
    suspend fun reduceNoise(audioData: AudioData): Result<AudioData>
    suspend fun generateWaveform(audioData: AudioData, targetPoints: Int = 100): Result<WaveformData>
    suspend fun trimSilence(audioData: AudioData, threshold: Float = 0.01f): Result<AudioData>
    suspend fun saveToFile(audioData: AudioData, outputFile: File): Result<Unit>
}
