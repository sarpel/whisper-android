package com.app.whisper.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import androidx.tracing.trace
import com.app.whisper.data.model.AudioData
import com.app.whisper.data.model.AudioRecorderConfig
import com.app.whisper.data.model.RecordingState
import com.app.whisper.domain.repository.AudioRecorderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AudioRecorder interface using Android's AudioRecord API.
 * 
 * Provides high-quality audio recording with real-time processing capabilities,
 * optimized for speech recognition and transcription use cases.
 */
@Singleton
class AudioRecorderImpl @Inject constructor(
    private val context: Context
) : AudioRecorder {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var outputFile: File? = null
    
    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    override val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private val _audioLevels = MutableStateFlow(0f)
    override val audioLevels: StateFlow<Float> = _audioLevels.asStateFlow()
    
    private var config = AudioRecorderConfig.default()
    
    override suspend fun initialize(config: AudioRecorderConfig): Result<Unit> = withContext(Dispatchers.IO) {
        trace("AudioRecorderImpl.initialize") {
            try {
                this@AudioRecorderImpl.config = config
                
                // Check permissions
                if (!hasAudioPermission()) {
                    return@trace Result.failure(SecurityException("Audio recording permission not granted"))
                }
                
                // Initialize AudioRecord
                val bufferSize = AudioRecord.getMinBufferSize(
                    config.sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    return@trace Result.failure(IllegalStateException("Invalid audio configuration"))
                }
                
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    config.sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 2
                )
                
                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    return@trace Result.failure(IllegalStateException("Failed to initialize AudioRecord"))
                }
                
                Timber.d("AudioRecorder initialized with config: $config")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize AudioRecorder")
                Result.failure(e)
            }
        }
    }
    
    override suspend fun startRecording(outputFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        trace("AudioRecorderImpl.startRecording") {
            try {
                if (_recordingState.value != RecordingState.IDLE) {
                    return@trace Result.failure(IllegalStateException("Recording already in progress"))
                }
                
                val recorder = audioRecord ?: return@trace Result.failure(
                    IllegalStateException("AudioRecorder not initialized")
                )
                
                this@AudioRecorderImpl.outputFile = outputFile
                _recordingState.value = RecordingState.RECORDING
                
                recorder.startRecording()
                
                recordingJob = scope.launch {
                    recordAudio(recorder, outputFile)
                }
                
                Timber.d("Started recording to: ${outputFile.absolutePath}")
                Result.success(Unit)
            } catch (e: Exception) {
                _recordingState.value = RecordingState.ERROR
                Timber.e(e, "Failed to start recording")
                Result.failure(e)
            }
        }
    }
    
    override suspend fun stopRecording(): Result<AudioData> = withContext(Dispatchers.IO) {
        trace("AudioRecorderImpl.stopRecording") {
            try {
                if (_recordingState.value != RecordingState.RECORDING) {
                    return@trace Result.failure(IllegalStateException("Not currently recording"))
                }
                
                _recordingState.value = RecordingState.STOPPING
                
                recordingJob?.cancel()
                recordingJob = null
                
                audioRecord?.stop()
                
                val file = outputFile ?: return@trace Result.failure(
                    IllegalStateException("No output file specified")
                )
                
                _recordingState.value = RecordingState.IDLE
                _audioLevels.value = 0f
                
                val audioData = AudioData.fromFile(file)
                Timber.d("Recording stopped, created AudioData: ${audioData.getDurationMs()}ms")
                
                Result.success(audioData)
            } catch (e: Exception) {
                _recordingState.value = RecordingState.ERROR
                Timber.e(e, "Failed to stop recording")
                Result.failure(e)
            }
        }
    }
    
    override suspend fun pauseRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        trace("AudioRecorderImpl.pauseRecording") {
            try {
                if (_recordingState.value != RecordingState.RECORDING) {
                    return@trace Result.failure(IllegalStateException("Not currently recording"))
                }
                
                audioRecord?.stop()
                _recordingState.value = RecordingState.PAUSED
                
                Timber.d("Recording paused")
                Result.success(Unit)
            } catch (e: Exception) {
                _recordingState.value = RecordingState.ERROR
                Timber.e(e, "Failed to pause recording")
                Result.failure(e)
            }
        }
    }
    
    override suspend fun resumeRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        trace("AudioRecorderImpl.resumeRecording") {
            try {
                if (_recordingState.value != RecordingState.PAUSED) {
                    return@trace Result.failure(IllegalStateException("Recording not paused"))
                }
                
                audioRecord?.startRecording()
                _recordingState.value = RecordingState.RECORDING
                
                Timber.d("Recording resumed")
                Result.success(Unit)
            } catch (e: Exception) {
                _recordingState.value = RecordingState.ERROR
                Timber.e(e, "Failed to resume recording")
                Result.failure(e)
            }
        }
    }
    
    override fun release() {
        scope.cancel()
        recordingJob?.cancel()
        audioRecord?.release()
        audioRecord = null
        _recordingState.value = RecordingState.IDLE
        _audioLevels.value = 0f
        Timber.d("AudioRecorder released")
    }
    
    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private suspend fun recordAudio(recorder: AudioRecord, outputFile: File) {
        val bufferSize = AudioRecord.getMinBufferSize(
            config.sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        val buffer = ShortArray(bufferSize)
        
        try {
            FileOutputStream(outputFile).use { fos ->
                while (scope.isActive && _recordingState.value == RecordingState.RECORDING) {
                    val bytesRead = recorder.read(buffer, 0, buffer.size)
                    
                    if (bytesRead > 0) {
                        // Convert to byte array and write to file
                        val byteBuffer = ByteArray(bytesRead * 2)
                        for (i in 0 until bytesRead) {
                            val sample = buffer[i]
                            byteBuffer[i * 2] = (sample and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                        }
                        fos.write(byteBuffer)
                        
                        // Calculate audio level for visualization
                        val level = calculateAudioLevel(buffer, bytesRead)
                        _audioLevels.value = level
                    }
                }
            }
        } catch (e: IOException) {
            Timber.e(e, "Error writing audio data")
            _recordingState.value = RecordingState.ERROR
        }
    }
    
    private fun calculateAudioLevel(buffer: ShortArray, length: Int): Float {
        var sum = 0.0
        for (i in 0 until length) {
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        val rms = kotlin.math.sqrt(sum / length)
        return (rms / Short.MAX_VALUE).toFloat()
    }
}

/**
 * Interface for audio recording operations.
 */
interface AudioRecorder {
    val recordingState: StateFlow<RecordingState>
    val audioLevels: StateFlow<Float>
    
    suspend fun initialize(config: AudioRecorderConfig): Result<Unit>
    suspend fun startRecording(outputFile: File): Result<Unit>
    suspend fun stopRecording(): Result<AudioData>
    suspend fun pauseRecording(): Result<Unit>
    suspend fun resumeRecording(): Result<Unit>
    fun release()
}
