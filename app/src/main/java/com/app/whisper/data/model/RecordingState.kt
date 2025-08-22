package com.app.whisper.data.model

/**
 * Sealed class representing the different states of audio recording.
 *
 * This provides type-safe state management for the audio recording system,
 * ensuring that state transitions are handled properly and all possible
 * states are accounted for.
 */
sealed class RecordingState {

    /**
     * Recording is idle - not started or completely stopped.
     */
    object Idle : RecordingState()

    /**
     * Recording is currently active and capturing audio.
     *
     * @param duration Current recording duration in milliseconds
     * @param samplesRecorded Total number of audio samples recorded
     */
    data class Recording(
        val duration: Long = 0L,
        val samplesRecorded: Long = 0L
    ) : RecordingState()

    /**
     * Recording is temporarily paused.
     *
     * @param duration Duration recorded before pausing
     * @param samplesRecorded Total samples recorded before pausing
     */
    data class Paused(
        val duration: Long,
        val samplesRecorded: Long
    ) : RecordingState()

    /**
     * Recording has been stopped and is ready for processing.
     *
     * @param totalDurationMs Total recording duration
     * @param totalSamples Total number of samples recorded
     * @param filePath Path to the recorded audio file (if saved)
     */
    data class Stopped(
        val totalDurationMs: Long,
        val totalSamples: Long,
        val filePath: String? = null
    ) : RecordingState()

    /**
     * An error occurred during recording.
     *
     * @param error The exception that caused the error
     * @param message Human-readable error message
     * @param canRecover Whether recording can be resumed after this error
     */
    data class Error(
        val error: Throwable,
        val message: String,
        val canRecover: Boolean = false
    ) : RecordingState()

    /**
     * Check if recording is currently active (recording or paused).
     *
     * @return true if recording session is active
     */
    fun isActive(): Boolean = when (this) {
        is Recording, is Paused -> true
        else -> false
    }

    /**
     * Check if recording can be started from this state.
     *
     * @return true if recording can be started
     */
    fun canStart(): Boolean = when (this) {
        is Idle, is Stopped -> true
        is Error -> canRecover
        else -> false
    }

    /**
     * Check if recording can be paused from this state.
     *
     * @return true if recording can be paused
     */
    fun canPause(): Boolean = this is Recording

    /**
     * Check if recording can be resumed from this state.
     *
     * @return true if recording can be resumed
     */
    fun canResume(): Boolean = this is Paused

    /**
     * Check if recording can be stopped from this state.
     *
     * @return true if recording can be stopped
     */
    fun canStop(): Boolean = when (this) {
        is Recording, is Paused -> true
        else -> false
    }

    /**
     * Get the current recording duration in milliseconds.
     *
     * @return Duration in milliseconds, or 0 if not applicable
     */
    fun getDurationMs(): Long = when (this) {
        is Recording -> duration
        is Paused -> duration
        is Stopped -> totalDurationMs
        else -> 0L
    }

    /**
     * Get the current number of recorded samples.
     *
     * @return Number of samples, or 0 if not applicable
     */
    fun getSampleCount(): Long = when (this) {
        is Recording -> samplesRecorded
        is Paused -> samplesRecorded
        is Stopped -> totalSamples
        else -> 0L
    }

    /**
     * Get a human-readable description of the current state.
     *
     * @return String description of the state
     */
    fun getDescription(): String = when (this) {
        is Idle -> "Ready to record"
        is Recording -> "Recording... (${duration / 1000}s)"
        is Paused -> "Paused (${duration / 1000}s recorded)"
        is Stopped -> "Stopped (${totalDurationMs / 1000}s total)"
        is Error -> "Error: $message"
    }

    companion object {
        /**
         * Create an initial idle state.
         *
         * @return New Idle state instance
         */
        fun initial(): RecordingState = Idle

        /**
         * Create an error state with a throwable.
         *
         * @param throwable The error that occurred
         * @param canRecover Whether recovery is possible
         * @return New Error state instance
         */
        fun error(throwable: Throwable, canRecover: Boolean = false): RecordingState =
            Error(throwable, throwable.message ?: "Unknown error", canRecover)

        /**
         * Create an error state with a message.
         *
         * @param message Error message
         * @param canRecover Whether recovery is possible
         * @return New Error state instance
         */
        fun error(message: String, canRecover: Boolean = false): RecordingState =
            Error(RuntimeException(message), message, canRecover)
    }
}
