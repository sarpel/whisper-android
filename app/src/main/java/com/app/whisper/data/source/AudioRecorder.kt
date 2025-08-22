package com.app.whisper.data.source

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.util.Log
import androidx.core.content.ContextCompat
import com.app.whisper.data.model.AudioData
import com.app.whisper.data.model.AudioRecorderConfig
import com.app.whisper.data.model.RecordingState
import com.app.whisper.data.model.WaveformData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio recorder implementation for capturing real-time audio data.
 *
 * This class provides a comprehensive audio recording solution with:
 * - Real-time audio capture using Android AudioRecord
 * - Thread-safe state management
 * - Waveform data generation for visualization
 * - Voice activity detection
 * - Automatic silence detection
 * - Coroutines-based async operations
 */
@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "AudioRecorder"
        private const val AUDIO_BUFFER_READ_TIMEOUT_MS = 100L
    }

    // Configuration and state
    private var config = AudioRecorderConfig.forWhisper()
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val recordingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Thread safety
    private val recordingMutex = Mutex()

    // State management
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.initial())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    // Audio data streams
    private val _audioDataFlow = MutableSharedFlow<AudioData>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val audioDataFlow: Flow<AudioData> = _audioDataFlow.asSharedFlow()

    private val _waveformDataFlow = MutableSharedFlow<WaveformData>(
        replay = 1,
        extraBufferCapacity = 5
    )
    val waveformDataFlow: Flow<WaveformData> = _waveformDataFlow.asSharedFlow()

    // Recording statistics
    private var recordingStartTime = 0L
    private var totalSamplesRecorded = 0L
    private var sequenceNumber = 0L

    /**
     * Configure the audio recorder with new settings.
     * Can only be called when recording is not active.
     *
     * @param newConfig New configuration to apply
     * @return Result indicating success or failure
     */
    suspend fun configure(newConfig: AudioRecorderConfig): Result<Unit> =
        recordingMutex.withLock {
            try {
                if (_recordingState.value.isActive()) {
                    return Result.failure(
                        IllegalStateException("Cannot configure while recording is active")
                    )
                }

                // Validate configuration
                val validationResult = newConfig.validate()
                if (validationResult.isFailure) {
                    return validationResult
                }

                // Release existing AudioRecord if any
                releaseAudioRecord()

                config = newConfig
                Log.i(TAG, "Audio recorder configured: ${config.getDescription()}")

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to configure audio recorder", e)
                Result.failure(e)
            }
        }

    /**
     * Start audio recording.
     *
     * @return Result indicating success or failure
     */
    suspend fun startRecording(): Result<Unit> = recordingMutex.withLock {
        try {
            val currentState = _recordingState.value
            if (!currentState.canStart()) {
                return Result.failure(
                    IllegalStateException("Cannot start recording from state: ${currentState.getDescription()}")
                )
            }

            // Check microphone permission
            if (!hasAudioPermission()) {
                val error = SecurityException("Microphone permission not granted")
                _recordingState.value = RecordingState.error(error, canRecover = true)
                return Result.failure(error)
            }

            // Initialize AudioRecord
            val initResult = initializeAudioRecord()
            if (initResult.isFailure) {
                val error = initResult.exceptionOrNull() ?: Exception("Failed to initialize AudioRecord")
                _recordingState.value = RecordingState.error(error, canRecover = true)
                return Result.failure(error)
            }

            // Start recording
            audioRecord?.startRecording()

            // Reset statistics
            recordingStartTime = System.currentTimeMillis()
            totalSamplesRecorded = 0L
            sequenceNumber = 0L

            // Update state
            _recordingState.value = RecordingState.Recording()

            // Start recording coroutine
            recordingJob = recordingScope.launch {
                runRecordingLoop()
            }

            Log.i(TAG, "Audio recording started")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            _recordingState.value = RecordingState.error(e, canRecover = true)
            Result.failure(e)
        }
    }

    /**
     * Stop audio recording.
     *
     * @return Result indicating success or failure
     */
    suspend fun stopRecording(): Result<Unit> = recordingMutex.withLock {
        try {
            val currentState = _recordingState.value
            if (!currentState.canStop()) {
                return Result.failure(
                    IllegalStateException("Cannot stop recording from state: ${currentState.getDescription()}")
                )
            }

            // Cancel recording job
            recordingJob?.cancel()
            recordingJob = null

            // Stop AudioRecord
            audioRecord?.stop()

            // Calculate final statistics
            val totalDuration = System.currentTimeMillis() - recordingStartTime

            // Update state
            _recordingState.value = RecordingState.Stopped(
                totalDurationMs = totalDuration,
                totalSamples = totalSamplesRecorded
            )

            Log.i(TAG, "Audio recording stopped: ${totalDuration}ms, $totalSamplesRecorded samples")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            _recordingState.value = RecordingState.error(e, canRecover = false)
            Result.failure(e)
        }
    }

    /**
     * Pause audio recording.
     *
     * @return Result indicating success or failure
     */
    suspend fun pauseRecording(): Result<Unit> = recordingMutex.withLock {
        try {
            val currentState = _recordingState.value
            if (!currentState.canPause()) {
                return Result.failure(
                    IllegalStateException("Cannot pause recording from state: ${currentState.getDescription()}")
                )
            }

            // Pause AudioRecord
            audioRecord?.stop()

            // Cancel recording job but keep AudioRecord initialized
            recordingJob?.cancel()
            recordingJob = null

            // Calculate current duration
            val currentDuration = System.currentTimeMillis() - recordingStartTime

            // Update state
            _recordingState.value = RecordingState.Paused(
                duration = currentDuration,
                samplesRecorded = totalSamplesRecorded
            )

            Log.i(TAG, "Audio recording paused")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause recording", e)
            _recordingState.value = RecordingState.error(e, canRecover = true)
            Result.failure(e)
        }
    }

    /**
     * Resume audio recording from paused state.
     *
     * @return Result indicating success or failure
     */
    suspend fun resumeRecording(): Result<Unit> = recordingMutex.withLock {
        try {
            val currentState = _recordingState.value
            if (!currentState.canResume()) {
                return Result.failure(
                    IllegalStateException("Cannot resume recording from state: ${currentState.getDescription()}")
                )
            }

            // Resume AudioRecord
            audioRecord?.startRecording()

            // Update state
            _recordingState.value = RecordingState.Recording(
                duration = currentState.getDurationMs(),
                samplesRecorded = currentState.getSampleCount()
            )

            // Restart recording coroutine
            recordingJob = recordingScope.launch {
                runRecordingLoop()
            }

            Log.i(TAG, "Audio recording resumed")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume recording", e)
            _recordingState.value = RecordingState.error(e, canRecover = true)
            Result.failure(e)
        }
    }

    /**
     * Get current recording configuration.
     *
     * @return Current AudioRecorderConfig
     */
    fun getConfiguration(): AudioRecorderConfig = config

    /**
     * Check if microphone permission is granted.
     *
     * @return true if permission is granted
     */
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Release all resources and clean up.
     */
    fun release() {
        recordingScope.launch {
            recordingMutex.withLock {
                try {
                    // Cancel recording job
                    recordingJob?.cancel()
                    recordingJob = null

                    // Release AudioRecord
                    releaseAudioRecord()

                    // Update state
                    _recordingState.value = RecordingState.Idle

                    Log.i(TAG, "Audio recorder released")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during release", e)
                }
            }
        }

        // Cancel the recording scope
        recordingScope.cancel()
    }

    /**
     * Initialize AudioRecord with current configuration.
     */
    private fun initializeAudioRecord(): Result<Unit> {
        try {
            releaseAudioRecord()

            val bufferSize = config.calculateBufferSize()

            audioRecord = AudioRecord(
                config.audioSource,
                config.sampleRate,
                config.channelConfig,
                config.audioFormat,
                bufferSize
            )

            val state = audioRecord?.state
            if (state != AudioRecord.STATE_INITIALIZED) {
                releaseAudioRecord()
                return Result.failure(
                    IllegalStateException("AudioRecord initialization failed, state: $state")
                )
            }

            Log.d(TAG, "AudioRecord initialized: bufferSize=$bufferSize")
            return Result.success(Unit)

        } catch (e: Exception) {
            releaseAudioRecord()
            return Result.failure(e)
        }
    }

    /**
     * Release AudioRecord resources.
     */
    private fun releaseAudioRecord() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord", e)
        } finally {
            audioRecord = null
        }
    }

    /**
     * Main recording loop that runs in a coroutine.
     */
    private suspend fun runRecordingLoop() {
        val audioRecord = this.audioRecord ?: return
        val bufferSize = config.getSamplesPerBuffer()
        val audioBuffer = ShortArray(bufferSize)

        Log.d(TAG, "Recording loop started with buffer size: $bufferSize")

        try {
            while (recordingScope.isActive && _recordingState.value is RecordingState.Recording) {
                // Read audio data
                val samplesRead = audioRecord.read(audioBuffer, 0, bufferSize)

                if (samplesRead > 0) {
                    // Process audio data
                    processAudioBuffer(audioBuffer, samplesRead)

                    // Update statistics
                    totalSamplesRecorded += samplesRead
                    val currentDuration = System.currentTimeMillis() - recordingStartTime

                    // Update recording state
                    _recordingState.value = RecordingState.Recording(
                        duration = currentDuration,
                        samplesRecorded = totalSamplesRecorded
                    )

                    // Check for maximum duration
                    if (currentDuration >= config.maxRecordingDurationMs) {
                        Log.i(TAG, "Maximum recording duration reached")
                        stopRecording()
                        break
                    }

                } else {
                    // Handle read errors
                    when (samplesRead) {
                        AudioRecord.ERROR_INVALID_OPERATION -> {
                            Log.e(TAG, "AudioRecord invalid operation")
                            _recordingState.value = RecordingState.error(
                                "Invalid AudioRecord operation", canRecover = true
                            )
                            break
                        }
                        AudioRecord.ERROR_BAD_VALUE -> {
                            Log.e(TAG, "AudioRecord bad value")
                            _recordingState.value = RecordingState.error(
                                "Bad AudioRecord value", canRecover = true
                            )
                            break
                        }
                        else -> {
                            Log.w(TAG, "AudioRecord read returned: $samplesRead")
                            delay(AUDIO_BUFFER_READ_TIMEOUT_MS)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in recording loop", e)
            _recordingState.value = RecordingState.error(e, canRecover = true)
        }

        Log.d(TAG, "Recording loop ended")
    }

    /**
     * Process audio buffer and emit audio data.
     */
    private suspend fun processAudioBuffer(buffer: ShortArray, samplesRead: Int) {
        try {
            // Create audio data from buffer
            val audioData = AudioData.fromPCM16(
                pcmSamples = buffer.copyOf(samplesRead),
                sampleRate = config.sampleRate,
                channelCount = config.getChannelCount()
            ).copy(
                timestampMs = System.currentTimeMillis(),
                sequenceNumber = sequenceNumber++
            )

            // Emit audio data
            _audioDataFlow.tryEmit(audioData)

            // Generate and emit waveform data
            val waveformData = WaveformData.fromAudioData(
                audioData = audioData,
                windowSizeMs = config.waveformWindowSizeMs
            )
            _waveformDataFlow.tryEmit(waveformData)

            // Voice activity detection if enabled
            if (config.enableVoiceActivityDetection) {
                val hasVoiceActivity = audioData.hasActivity(config.voiceActivityThreshold)
                if (!hasVoiceActivity) {
                    // TODO: Implement silence timeout logic
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing audio buffer", e)
        }
    }
}
