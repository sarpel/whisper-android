package com.app.whisper.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.whisper.data.model.AudioData
import com.app.whisper.data.model.RecordingState
import com.app.whisper.data.source.AudioRecorder
import com.app.whisper.domain.entity.ProcessingParameters
import com.app.whisper.domain.repository.ModelRepository
import com.app.whisper.domain.usecase.TranscribeAudioUseCase
import com.app.whisper.domain.usecase.TranscriptionProgress
import com.app.whisper.presentation.state.TranscriptionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the transcription screen.
 *
 * This ViewModel manages the state and business logic for audio recording,
 * transcription, and result presentation. It coordinates between the audio
 * recorder, transcription use case, and UI state.
 */
@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val transcribeAudioUseCase: TranscribeAudioUseCase,
    private val modelRepository: ModelRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<TranscriptionUiState>(TranscriptionUiState.Initial)
    val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()

    // Events
    private val _events = MutableSharedFlow<TranscriptionEvent>()
    val events = _events.asSharedFlow()

    // Processing parameters
    private val _processingParameters = MutableStateFlow(ProcessingParameters())
    val processingParameters: StateFlow<ProcessingParameters> = _processingParameters.asStateFlow()

    // Current jobs
    private var transcriptionJob: Job? = null
    private var recordingObservationJob: Job? = null

    // Audio data buffer
    private val audioDataBuffer = mutableListOf<AudioData>()

    init {
        initialize()
    }

    /**
     * Initialize the ViewModel.
     */
    private fun initialize() {
        viewModelScope.launch {
            try {
                _uiState.value = TranscriptionUiState.Loading

                // Get current model
                val currentModel = modelRepository.getCurrentModel()

                // Get recent results
                val recentResults = transcribeAudioUseCase.getRecentResults(5)
                    .getOrElse { emptyList() }

                // Set ready state
                _uiState.value = TranscriptionUiState.Ready(
                    currentModel = currentModel,
                    recordingState = RecordingState.Idle,
                    recentResults = recentResults,
                    canRecord = currentModel?.isAvailable() == true && audioRecorder.hasAudioPermission(),
                    processingParameters = _processingParameters.value
                )

                // Observe model changes
                observeModelChanges()

            } catch (e: Exception) {
                _uiState.value = TranscriptionUiState.Error(
                    error = e,
                    canRetry = true,
                    canRecover = false
                )
            }
        }
    }

    /**
     * Start audio recording.
     */
    fun startRecording() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (!currentState.canStartRecording()) {
                    _events.emit(TranscriptionEvent.Error("Cannot start recording in current state"))
                    return@launch
                }

                // Clear previous audio data
                audioDataBuffer.clear()

                // Start recording
                val recordingResult = audioRecorder.startRecording()
                if (recordingResult.isFailure) {
                    _events.emit(TranscriptionEvent.Error(
                        recordingResult.exceptionOrNull()?.message ?: "Failed to start recording"
                    ))
                    return@launch
                }

                // Observe recording state and audio data
                observeRecording()

                _events.emit(TranscriptionEvent.RecordingStarted)

            } catch (e: Exception) {
                _events.emit(TranscriptionEvent.Error("Failed to start recording: ${e.message}"))
            }
        }
    }

    /**
     * Stop audio recording and start transcription.
     */
    fun stopRecording() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (!currentState.canStopRecording()) {
                    _events.emit(TranscriptionEvent.Error("Cannot stop recording in current state"))
                    return@launch
                }

                // Stop recording
                val stopResult = audioRecorder.stopRecording()
                if (stopResult.isFailure) {
                    _events.emit(TranscriptionEvent.Error(
                        stopResult.exceptionOrNull()?.message ?: "Failed to stop recording"
                    ))
                    return@launch
                }

                // Stop observing recording
                recordingObservationJob?.cancel()

                // Combine audio data
                val combinedAudioData = combineAudioData(audioDataBuffer)
                if (combinedAudioData == null) {
                    _events.emit(TranscriptionEvent.Error("No audio data recorded"))
                    return@launch
                }

                // Start transcription
                startTranscription(combinedAudioData)

                _events.emit(TranscriptionEvent.RecordingStopped)

            } catch (e: Exception) {
                _events.emit(TranscriptionEvent.Error("Failed to stop recording: ${e.message}"))
            }
        }
    }

    /**
     * Pause audio recording.
     */
    fun pauseRecording() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (!currentState.canPauseRecording()) {
                    _events.emit(TranscriptionEvent.Error("Cannot pause recording in current state"))
                    return@launch
                }

                val pauseResult = audioRecorder.pauseRecording()
                if (pauseResult.isFailure) {
                    _events.emit(TranscriptionEvent.Error(
                        pauseResult.exceptionOrNull()?.message ?: "Failed to pause recording"
                    ))
                    return@launch
                }

                _events.emit(TranscriptionEvent.RecordingPaused)

            } catch (e: Exception) {
                _events.emit(TranscriptionEvent.Error("Failed to pause recording: ${e.message}"))
            }
        }
    }

    /**
     * Resume audio recording.
     */
    fun resumeRecording() {
        viewModelScope.launch {
            try {
                val resumeResult = audioRecorder.resumeRecording()
                if (resumeResult.isFailure) {
                    _events.emit(TranscriptionEvent.Error(
                        resumeResult.exceptionOrNull()?.message ?: "Failed to resume recording"
                    ))
                    return@launch
                }

                _events.emit(TranscriptionEvent.RecordingResumed)

            } catch (e: Exception) {
                _events.emit(TranscriptionEvent.Error("Failed to resume recording: ${e.message}"))
            }
        }
    }

    /**
     * Start transcription with audio data.
     *
     * @param audioData Audio data to transcribe
     */
    private fun startTranscription(audioData: AudioData) {
        transcriptionJob?.cancel()
        transcriptionJob = viewModelScope.launch {
            transcribeAudioUseCase.execute(audioData, _processingParameters.value)
                .onEach { progress ->
                    _uiState.value = TranscriptionUiState.Processing(
                        progress = progress,
                        audioData = audioData,
                        canCancel = true
                    )

                    when (progress) {
                        is TranscriptionProgress.Completed -> {
                            _uiState.value = TranscriptionUiState.Success(
                                result = progress.result,
                                session = progress.session,
                                audioData = audioData,
                                processingTimeMs = progress.result.processingTimeMs
                            )
                            _events.emit(TranscriptionEvent.TranscriptionCompleted(progress.result))
                        }
                        is TranscriptionProgress.Failed -> {
                            _uiState.value = TranscriptionUiState.Error(
                                error = progress.error,
                                canRetry = true,
                                canRecover = true,
                                previousState = TranscriptionUiState.ready(getCurrentModel())
                            )
                            _events.emit(TranscriptionEvent.Error(progress.error.message ?: "Transcription failed"))
                        }
                        else -> {
                            // Progress update - UI state already updated above
                        }
                    }
                }
                .catch { error ->
                    _uiState.value = TranscriptionUiState.Error(
                        error = error,
                        canRetry = true,
                        canRecover = true,
                        previousState = TranscriptionUiState.ready(getCurrentModel())
                    )
                    _events.emit(TranscriptionEvent.Error(error.message ?: "Transcription failed"))
                }
                .launchIn(this)
        }
    }

    /**
     * Cancel ongoing transcription.
     */
    fun cancelTranscription() {
        transcriptionJob?.cancel()
        transcriptionJob = null

        _uiState.value = TranscriptionUiState.ready(getCurrentModel())

        viewModelScope.launch {
            _events.emit(TranscriptionEvent.TranscriptionCancelled)
        }
    }

    /**
     * Retry the last operation.
     */
    fun retry() {
        val currentState = _uiState.value
        if (currentState is TranscriptionUiState.Error && currentState.canRetry) {
            if (currentState.previousState != null) {
                _uiState.value = currentState.previousState
            } else {
                initialize()
            }
        }
    }

    /**
     * Update processing parameters.
     *
     * @param parameters New processing parameters
     */
    fun updateProcessingParameters(parameters: ProcessingParameters) {
        _processingParameters.value = parameters

        // Update UI state if in ready state
        val currentState = _uiState.value
        if (currentState is TranscriptionUiState.Ready) {
            _uiState.value = currentState.copy(processingParameters = parameters)
        }
    }

    /**
     * Clear the current transcription result.
     */
    fun clearResult() {
        _uiState.value = TranscriptionUiState.ready(getCurrentModel())
    }

    /**
     * Observe recording state and audio data.
     */
    private fun observeRecording() {
        recordingObservationJob?.cancel()
        recordingObservationJob = viewModelScope.launch {
            combine(
                audioRecorder.recordingState,
                audioRecorder.audioDataFlow,
                audioRecorder.waveformDataFlow
            ) { recordingState, audioData, waveformData ->
                Triple(recordingState, audioData, waveformData)
            }.collect { (recordingState, audioData, waveformData) ->
                // Buffer audio data
                audioDataBuffer.add(audioData)

                // Update UI state
                _uiState.value = TranscriptionUiState.Recording(
                    recordingState = recordingState,
                    waveformData = waveformData,
                    duration = recordingState.getDurationMs(),
                    canStop = recordingState.canStop(),
                    canPause = recordingState.canPause()
                )
            }
        }
    }

    /**
     * Observe model changes.
     */
    private fun observeModelChanges() {
        modelRepository.observeCurrentModel()
            .onEach { model ->
                val currentState = _uiState.value
                if (currentState is TranscriptionUiState.Ready) {
                    _uiState.value = currentState.copy(
                        currentModel = model,
                        canRecord = model?.isAvailable() == true && audioRecorder.hasAudioPermission()
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Combine multiple audio data chunks into a single AudioData.
     *
     * @param audioDataList List of audio data chunks
     * @return Combined audio data or null if empty
     */
    private fun combineAudioData(audioDataList: List<AudioData>): AudioData? {
        if (audioDataList.isEmpty()) return null

        val firstAudio = audioDataList.first()
        val totalSamples = audioDataList.sumOf { it.samples.size }
        val combinedSamples = FloatArray(totalSamples)

        var offset = 0
        for (audioData in audioDataList) {
            audioData.samples.copyInto(combinedSamples, offset)
            offset += audioData.samples.size
        }

        return AudioData(
            samples = combinedSamples,
            sampleRate = firstAudio.sampleRate,
            channelCount = firstAudio.channelCount,
            timestampMs = firstAudio.timestampMs,
            sequenceNumber = firstAudio.sequenceNumber
        )
    }

    /**
     * Get the current model.
     *
     * @return Current model or null
     */
    private fun getCurrentModel() = _uiState.value.getActiveModel()

    override fun onCleared() {
        super.onCleared()
        transcriptionJob?.cancel()
        recordingObservationJob?.cancel()
        audioRecorder.release()
    }
}

/**
 * Sealed class representing transcription events.
 */
sealed class TranscriptionEvent {
    object RecordingStarted : TranscriptionEvent()
    object RecordingStopped : TranscriptionEvent()
    object RecordingPaused : TranscriptionEvent()
    object RecordingResumed : TranscriptionEvent()
    data class TranscriptionCompleted(val result: com.app.whisper.domain.entity.TranscriptionResult) : TranscriptionEvent()
    object TranscriptionCancelled : TranscriptionEvent()
    data class Error(val message: String) : TranscriptionEvent()

    /**
     * Get a human-readable description of the event.
     *
     * @return Event description
     */
    fun getDescription(): String = when (this) {
        is RecordingStarted -> "Recording started"
        is RecordingStopped -> "Recording stopped"
        is RecordingPaused -> "Recording paused"
        is RecordingResumed -> "Recording resumed"
        is TranscriptionCompleted -> "Transcription completed"
        is TranscriptionCancelled -> "Transcription cancelled"
        is Error -> "Error: $message"
    }
}
