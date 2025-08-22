package com.app.whisper.domain.entity

/**
 * Domain entity representing a complete transcription session.
 * 
 * This entity encapsulates all information about a transcription session,
 * including the audio input, processing parameters, results, and session metadata.
 */
data class TranscriptionSession(
    val id: String,
    val name: String,
    val audioFilePath: String,
    val audioMetadata: AudioMetadata,
    val transcriptionResult: TranscriptionResult?,
    val modelUsed: String,
    val processingParameters: ProcessingParameters,
    val status: SessionStatus,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val error: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String = ""
) {
    
    /**
     * Check if the session is completed successfully.
     * 
     * @return true if session completed with results
     */
    fun isCompleted(): Boolean = status == SessionStatus.COMPLETED && transcriptionResult != null
    
    /**
     * Check if the session is currently processing.
     * 
     * @return true if session is in progress
     */
    fun isProcessing(): Boolean = status == SessionStatus.PROCESSING
    
    /**
     * Check if the session failed.
     * 
     * @return true if session failed with error
     */
    fun isFailed(): Boolean = status == SessionStatus.FAILED
    
    /**
     * Get the total processing duration.
     * 
     * @return Processing duration in milliseconds, or null if not applicable
     */
    fun getProcessingDuration(): Long? {
        return if (startedAt != null && completedAt != null) {
            completedAt - startedAt
        } else null
    }
    
    /**
     * Get the session duration from creation to completion.
     * 
     * @return Total session duration in milliseconds
     */
    fun getTotalDuration(): Long {
        val endTime = completedAt ?: System.currentTimeMillis()
        return endTime - createdAt
    }
    
    /**
     * Get the processing speed ratio (real-time factor).
     * 
     * @return Speed ratio, or null if not calculable
     */
    fun getSpeedRatio(): Float? {
        val processingDuration = getProcessingDuration()
        return if (processingDuration != null && audioMetadata.durationMs > 0) {
            processingDuration.toFloat() / audioMetadata.durationMs.toFloat()
        } else null
    }
    
    /**
     * Get a human-readable status description.
     * 
     * @return Status description string
     */
    fun getStatusDescription(): String = when (status) {
        SessionStatus.CREATED -> "Created"
        SessionStatus.QUEUED -> "Queued for processing"
        SessionStatus.PROCESSING -> "Processing..."
        SessionStatus.COMPLETED -> "Completed successfully"
        SessionStatus.FAILED -> "Failed: ${error ?: "Unknown error"}"
        SessionStatus.CANCELLED -> "Cancelled"
    }
    
    /**
     * Check if the session has a specific tag.
     * 
     * @param tag Tag to check for
     * @return true if session has the tag
     */
    fun hasTag(tag: String): Boolean = tags.contains(tag)
    
    /**
     * Create a copy with updated status.
     * 
     * @param newStatus New session status
     * @param error Error message (for failed status)
     * @return Updated session instance
     */
    fun withStatus(newStatus: SessionStatus, error: String? = null): TranscriptionSession {
        val now = System.currentTimeMillis()
        return copy(
            status = newStatus,
            error = error,
            startedAt = if (newStatus == SessionStatus.PROCESSING && startedAt == null) now else startedAt,
            completedAt = if (newStatus == SessionStatus.COMPLETED || newStatus == SessionStatus.FAILED) now else completedAt
        )
    }
    
    /**
     * Create a copy with transcription result.
     * 
     * @param result Transcription result
     * @return Updated session instance
     */
    fun withResult(result: TranscriptionResult): TranscriptionSession = copy(
        transcriptionResult = result,
        status = SessionStatus.COMPLETED,
        completedAt = System.currentTimeMillis()
    )
    
    /**
     * Create a copy with additional tags.
     * 
     * @param newTags Tags to add
     * @return Updated session instance
     */
    fun withTags(vararg newTags: String): TranscriptionSession = copy(
        tags = (tags + newTags).distinct()
    )
    
    /**
     * Get a summary of the session.
     * 
     * @return Human-readable summary
     */
    fun getSummary(): String = buildString {
        append("Session '$name' (${getStatusDescription()})")
        if (transcriptionResult != null) {
            append(" - ${transcriptionResult.text.take(100)}")
            if (transcriptionResult.text.length > 100) append("...")
        }
        append(" [${audioMetadata.getFormattedDuration()}]")
    }
    
    companion object {
        /**
         * Create a new transcription session.
         * 
         * @param name Session name
         * @param audioFilePath Path to audio file
         * @param audioMetadata Audio file metadata
         * @param modelName Model to use for transcription
         * @param parameters Processing parameters
         * @return New TranscriptionSession instance
         */
        fun create(
            name: String,
            audioFilePath: String,
            audioMetadata: AudioMetadata,
            modelName: String,
            parameters: ProcessingParameters = ProcessingParameters()
        ): TranscriptionSession = TranscriptionSession(
            id = generateId(),
            name = name,
            audioFilePath = audioFilePath,
            audioMetadata = audioMetadata,
            transcriptionResult = null,
            modelUsed = modelName,
            processingParameters = parameters,
            status = SessionStatus.CREATED
        )
        
        /**
         * Generate a unique session ID.
         * 
         * @return Unique session identifier
         */
        private fun generateId(): String = "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

/**
 * Enumeration of transcription session status.
 */
enum class SessionStatus {
    CREATED,
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Audio file metadata.
 */
data class AudioMetadata(
    val fileName: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val sampleRate: Int,
    val channels: Int,
    val bitRate: Int,
    val format: String,
    val codec: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    
    /**
     * Get formatted duration string.
     * 
     * @return Duration in MM:SS format
     */
    fun getFormattedDuration(): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Get formatted file size string.
     * 
     * @return File size in human-readable format
     */
    fun getFormattedSize(): String = when {
        fileSizeBytes < 1024 -> "$fileSizeBytes B"
        fileSizeBytes < 1024 * 1024 -> "${fileSizeBytes / 1024} KB"
        fileSizeBytes < 1024 * 1024 * 1024 -> "${fileSizeBytes / (1024 * 1024)} MB"
        else -> String.format("%.1f GB", fileSizeBytes / (1024.0 * 1024.0 * 1024.0))
    }
    
    /**
     * Check if audio format is compatible with Whisper.
     * 
     * @return true if format is supported
     */
    fun isWhisperCompatible(): Boolean {
        return sampleRate == 16000 && channels == 1 && format.lowercase() in listOf("wav", "pcm", "flac")
    }
    
    /**
     * Get audio quality assessment.
     * 
     * @return Quality level based on sample rate and bit rate
     */
    fun getQualityLevel(): String = when {
        sampleRate >= 44100 && bitRate >= 320 -> "High"
        sampleRate >= 22050 && bitRate >= 128 -> "Medium"
        sampleRate >= 16000 && bitRate >= 64 -> "Standard"
        else -> "Low"
    }
}

/**
 * Processing parameters for transcription.
 */
data class ProcessingParameters(
    val language: String = "auto",
    val translate: Boolean = false,
    val enableTimestamps: Boolean = false,
    val enableWordTimestamps: Boolean = false,
    val temperature: Float = 0.0f,
    val beamSize: Int = 5,
    val maxTokens: Int = -1,
    val compressionRatioThreshold: Float = 2.4f,
    val logProbThreshold: Float = -1.0f,
    val noSpeechThreshold: Float = 0.6f,
    val threadCount: Int = 4,
    val enableVAD: Boolean = false,
    val vadThreshold: Float = 0.02f
) {
    
    /**
     * Check if parameters are optimized for speed.
     * 
     * @return true if configured for fast processing
     */
    fun isSpeedOptimized(): Boolean {
        return temperature == 0.0f && beamSize <= 1 && !enableWordTimestamps
    }
    
    /**
     * Check if parameters are optimized for quality.
     * 
     * @return true if configured for high quality
     */
    fun isQualityOptimized(): Boolean {
        return beamSize >= 5 && enableTimestamps
    }
    
    /**
     * Get a description of the processing configuration.
     * 
     * @return Human-readable configuration description
     */
    fun getDescription(): String = buildString {
        append("Lang: $language")
        if (translate) append(", Translate")
        if (enableTimestamps) append(", Timestamps")
        if (enableWordTimestamps) append(", Word-level")
        append(", Beam: $beamSize")
        append(", Threads: $threadCount")
        if (enableVAD) append(", VAD")
    }
    
    companion object {
        /**
         * Create parameters optimized for speed.
         * 
         * @return Speed-optimized parameters
         */
        fun forSpeed(): ProcessingParameters = ProcessingParameters(
            temperature = 0.0f,
            beamSize = 1,
            enableTimestamps = false,
            enableWordTimestamps = false,
            threadCount = 4
        )
        
        /**
         * Create parameters optimized for quality.
         * 
         * @return Quality-optimized parameters
         */
        fun forQuality(): ProcessingParameters = ProcessingParameters(
            temperature = 0.0f,
            beamSize = 5,
            enableTimestamps = true,
            enableWordTimestamps = false,
            threadCount = 4
        )
        
        /**
         * Create parameters for real-time transcription.
         * 
         * @return Real-time optimized parameters
         */
        fun forRealTime(): ProcessingParameters = ProcessingParameters(
            temperature = 0.0f,
            beamSize = 1,
            enableTimestamps = false,
            enableWordTimestamps = false,
            threadCount = 2,
            enableVAD = true,
            vadThreshold = 0.02f
        )
    }
}
