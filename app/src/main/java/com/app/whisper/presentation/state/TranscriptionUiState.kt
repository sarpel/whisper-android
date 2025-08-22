package com.app.whisper.presentation.state

import com.app.whisper.data.model.AudioData
import com.app.whisper.data.model.RecordingState
import com.app.whisper.data.model.WaveformData
import com.app.whisper.domain.entity.ProcessingParameters
import com.app.whisper.domain.entity.TranscriptionResult
import com.app.whisper.domain.entity.TranscriptionSession
import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.domain.usecase.TranscriptionProgress

/**
 * UI state for the transcription screen.
 * 
 * This sealed class represents all possible states of the transcription UI,
 * providing type-safe state management for the presentation layer.
 */
sealed class TranscriptionUiState {
    
    /**
     * Initial state when the screen is first loaded.
     */
    object Initial : TranscriptionUiState()
    
    /**
     * Loading state while initializing the transcription system.
     */
    object Loading : TranscriptionUiState()
    
    /**
     * Ready state when the system is prepared for transcription.
     * 
     * @param currentModel Currently selected model
     * @param recordingState Current recording state
     * @param recentResults Recent transcription results
     * @param canRecord Whether recording is available
     */
    data class Ready(
        val currentModel: WhisperModel?,
        val recordingState: RecordingState,
        val recentResults: List<TranscriptionResult> = emptyList(),
        val canRecord: Boolean = true,
        val processingParameters: ProcessingParameters = ProcessingParameters()
    ) : TranscriptionUiState()
    
    /**
     * Recording state when audio is being captured.
     * 
     * @param recordingState Current recording state
     * @param waveformData Real-time waveform data
     * @param duration Recording duration in milliseconds
     * @param canStop Whether recording can be stopped
     * @param canPause Whether recording can be paused
     */
    data class Recording(
        val recordingState: RecordingState,
        val waveformData: WaveformData? = null,
        val duration: Long = 0L,
        val canStop: Boolean = true,
        val canPause: Boolean = true
    ) : TranscriptionUiState()
    
    /**
     * Processing state when transcription is in progress.
     * 
     * @param progress Current transcription progress
     * @param audioData Audio data being processed
     * @param sessionId Associated session ID (if any)
     * @param canCancel Whether processing can be cancelled
     */
    data class Processing(
        val progress: TranscriptionProgress,
        val audioData: AudioData? = null,
        val sessionId: String? = null,
        val canCancel: Boolean = true
    ) : TranscriptionUiState()
    
    /**
     * Success state when transcription is completed.
     * 
     * @param result Transcription result
     * @param session Associated session (if any)
     * @param audioData Original audio data
     * @param processingTimeMs Time taken for processing
     * @param canRetry Whether transcription can be retried
     * @param canSave Whether result can be saved
     * @param canShare Whether result can be shared
     */
    data class Success(
        val result: TranscriptionResult,
        val session: TranscriptionSession? = null,
        val audioData: AudioData? = null,
        val processingTimeMs: Long = 0L,
        val canRetry: Boolean = true,
        val canSave: Boolean = true,
        val canShare: Boolean = true
    ) : TranscriptionUiState()
    
    /**
     * Error state when an error occurs.
     * 
     * @param error The error that occurred
     * @param canRetry Whether the operation can be retried
     * @param canRecover Whether recovery is possible
     * @param previousState Previous state before error (for recovery)
     */
    data class Error(
        val error: Throwable,
        val canRetry: Boolean = true,
        val canRecover: Boolean = false,
        val previousState: TranscriptionUiState? = null
    ) : TranscriptionUiState()
    
    /**
     * Check if the current state allows starting a new recording.
     * 
     * @return true if recording can be started
     */
    fun canStartRecording(): Boolean = when (this) {
        is Ready -> canRecord
        is Success -> true
        is Error -> canRecover
        else -> false
    }
    
    /**
     * Check if the current state allows stopping recording.
     * 
     * @return true if recording can be stopped
     */
    fun canStopRecording(): Boolean = when (this) {
        is Recording -> canStop
        else -> false
    }
    
    /**
     * Check if the current state allows pausing recording.
     * 
     * @return true if recording can be paused
     */
    fun canPauseRecording(): Boolean = when (this) {
        is Recording -> canPause
        else -> false
    }
    
    /**
     * Check if transcription is currently in progress.
     * 
     * @return true if transcription is active
     */
    fun isTranscribing(): Boolean = this is Processing
    
    /**
     * Check if recording is currently active.
     * 
     * @return true if recording is active
     */
    fun isRecording(): Boolean = this is Recording
    
    /**
     * Check if the state represents a successful transcription.
     * 
     * @return true if transcription was successful
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Check if the state represents an error.
     * 
     * @return true if there's an error
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Get the current model if available.
     * 
     * @return Current model or null
     */
    fun getCurrentModel(): WhisperModel? = when (this) {
        is Ready -> currentModel
        else -> null
    }
    
    /**
     * Get the current recording state if available.
     * 
     * @return Recording state or null
     */
    fun getRecordingState(): RecordingState? = when (this) {
        is Ready -> recordingState
        is Recording -> recordingState
        else -> null
    }
    
    /**
     * Get the transcription result if available.
     * 
     * @return Transcription result or null
     */
    fun getTranscriptionResult(): TranscriptionResult? = when (this) {
        is Success -> result
        else -> null
    }
    
    /**
     * Get the current error if any.
     * 
     * @return Error or null
     */
    fun getError(): Throwable? = when (this) {
        is Error -> error
        else -> null
    }
    
    /**
     * Get a human-readable description of the current state.
     * 
     * @return State description
     */
    fun getDescription(): String = when (this) {
        is Initial -> "Initializing..."
        is Loading -> "Loading transcription system..."
        is Ready -> "Ready to record"
        is Recording -> "Recording audio... (${duration / 1000}s)"
        is Processing -> progress.getDescription()
        is Success -> "Transcription completed"
        is Error -> "Error: ${error.message}"
    }
    
    /**
     * Check if the UI should show a loading indicator.
     * 
     * @return true if loading should be shown
     */
    fun isLoading(): Boolean = when (this) {
        is Loading, is Processing -> true
        else -> false
    }
    
    /**
     * Check if the UI should be interactive.
     * 
     * @return true if user interaction is allowed
     */
    fun isInteractive(): Boolean = when (this) {
        is Ready, is Success -> true
        is Recording -> true // Some interactions allowed during recording
        is Error -> canRetry || canRecover
        else -> false
    }
    
    companion object {
        /**
         * Create an initial ready state.
         * 
         * @param model Current model
         * @return Ready state
         */
        fun ready(model: WhisperModel? = null): TranscriptionUiState = Ready(
            currentModel = model,
            recordingState = RecordingState.Idle,
            canRecord = model?.isAvailable() == true
        )
        
        /**
         * Create an error state from an exception.
         * 
         * @param error The error that occurred
         * @param previousState Previous state for recovery
         * @return Error state
         */
        fun error(error: Throwable, previousState: TranscriptionUiState? = null): TranscriptionUiState = Error(
            error = error,
            canRetry = true,
            canRecover = previousState != null,
            previousState = previousState
        )
    }
}
