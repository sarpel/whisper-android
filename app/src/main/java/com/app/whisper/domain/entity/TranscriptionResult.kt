package com.app.whisper.domain.entity

/**
 * Domain entity representing the result of an audio transcription.
 * 
 * This entity encapsulates all the information returned from a Whisper
 * transcription operation, including the transcribed text, confidence scores,
 * timing information, and metadata about the transcription process.
 */
data class TranscriptionResult(
    val id: String,
    val text: String,
    val language: String,
    val confidence: Float,
    val processingTimeMs: Long,
    val audioDurationMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val segments: List<TranscriptionSegment> = emptyList(),
    val metadata: TranscriptionMetadata = TranscriptionMetadata()
) {
    
    /**
     * Check if the transcription result is considered high quality.
     * 
     * @param confidenceThreshold Minimum confidence threshold (default: 0.8)
     * @return true if transcription quality is high
     */
    fun isHighQuality(confidenceThreshold: Float = 0.8f): Boolean {
        return confidence >= confidenceThreshold && text.isNotBlank()
    }
    
    /**
     * Get the transcription speed ratio (real-time factor).
     * Values < 1.0 indicate faster than real-time processing.
     * 
     * @return Processing speed ratio
     */
    fun getSpeedRatio(): Float {
        return if (audioDurationMs > 0) {
            processingTimeMs.toFloat() / audioDurationMs.toFloat()
        } else {
            Float.MAX_VALUE
        }
    }
    
    /**
     * Get the average words per minute in the transcribed text.
     * 
     * @return Words per minute, or 0 if calculation not possible
     */
    fun getWordsPerMinute(): Float {
        val wordCount = text.split("\\s+".toRegex()).size
        val durationMinutes = audioDurationMs / 60000.0f
        return if (durationMinutes > 0) wordCount / durationMinutes else 0.0f
    }
    
    /**
     * Get a summary of the transcription result.
     * 
     * @return Human-readable summary string
     */
    fun getSummary(): String = buildString {
        append("TranscriptionResult(")
        append("${text.take(50)}${if (text.length > 50) "..." else ""}, ")
        append("lang=$language, ")
        append("conf=${String.format("%.2f", confidence)}, ")
        append("${segments.size} segments, ")
        append("${String.format("%.1f", getSpeedRatio())}x speed")
        append(")")
    }
    
    /**
     * Check if this transcription contains timestamps.
     * 
     * @return true if segments have timing information
     */
    fun hasTimestamps(): Boolean = segments.any { it.startTime >= 0 && it.endTime > it.startTime }
    
    /**
     * Get the total duration covered by segments.
     * 
     * @return Total segment duration in milliseconds
     */
    fun getSegmentsDuration(): Long {
        return segments.maxOfOrNull { it.endTime } ?: 0L
    }
    
    /**
     * Find segments that contain a specific text.
     * 
     * @param query Text to search for
     * @param ignoreCase Whether to ignore case (default: true)
     * @return List of matching segments
     */
    fun findSegments(query: String, ignoreCase: Boolean = true): List<TranscriptionSegment> {
        return segments.filter { segment ->
            segment.text.contains(query, ignoreCase = ignoreCase)
        }
    }
    
    companion object {
        /**
         * Create an empty transcription result.
         * 
         * @param id Unique identifier
         * @return Empty TranscriptionResult
         */
        fun empty(id: String = ""): TranscriptionResult = TranscriptionResult(
            id = id,
            text = "",
            language = "unknown",
            confidence = 0.0f,
            processingTimeMs = 0L,
            audioDurationMs = 0L
        )
        
        /**
         * Create a failed transcription result.
         * 
         * @param id Unique identifier
         * @param error Error message
         * @return Failed TranscriptionResult
         */
        fun failed(id: String, error: String): TranscriptionResult = TranscriptionResult(
            id = id,
            text = "",
            language = "unknown",
            confidence = 0.0f,
            processingTimeMs = 0L,
            audioDurationMs = 0L,
            metadata = TranscriptionMetadata(error = error)
        )
    }
}

/**
 * Represents a segment of transcribed audio with timing information.
 */
data class TranscriptionSegment(
    val id: Int,
    val text: String,
    val startTime: Long, // milliseconds
    val endTime: Long,   // milliseconds
    val confidence: Float,
    val words: List<TranscriptionWord> = emptyList()
) {
    
    /**
     * Get the duration of this segment.
     * 
     * @return Duration in milliseconds
     */
    fun getDuration(): Long = endTime - startTime
    
    /**
     * Check if this segment overlaps with another segment.
     * 
     * @param other Other segment to check
     * @return true if segments overlap
     */
    fun overlapsWith(other: TranscriptionSegment): Boolean {
        return startTime < other.endTime && endTime > other.startTime
    }
    
    /**
     * Get the average confidence of words in this segment.
     * 
     * @return Average word confidence, or segment confidence if no words
     */
    fun getAverageWordConfidence(): Float {
        return if (words.isNotEmpty()) {
            words.map { it.confidence }.average().toFloat()
        } else {
            confidence
        }
    }
}

/**
 * Represents a single word in a transcription with timing and confidence.
 */
data class TranscriptionWord(
    val text: String,
    val startTime: Long, // milliseconds
    val endTime: Long,   // milliseconds
    val confidence: Float
) {
    
    /**
     * Get the duration of this word.
     * 
     * @return Duration in milliseconds
     */
    fun getDuration(): Long = endTime - startTime
    
    /**
     * Check if this word is considered reliable.
     * 
     * @param threshold Confidence threshold (default: 0.7)
     * @return true if word confidence is above threshold
     */
    fun isReliable(threshold: Float = 0.7f): Boolean = confidence >= threshold
}

/**
 * Metadata associated with a transcription result.
 */
data class TranscriptionMetadata(
    val modelName: String = "",
    val modelVersion: String = "",
    val audioFormat: String = "",
    val sampleRate: Int = 0,
    val channels: Int = 0,
    val fileSize: Long = 0L,
    val processingDevice: String = "",
    val threadCount: Int = 0,
    val temperature: Float = 0.0f,
    val beamSize: Int = 0,
    val error: String? = null,
    val warnings: List<String> = emptyList()
) {
    
    /**
     * Check if transcription completed without errors.
     * 
     * @return true if no error occurred
     */
    fun isSuccessful(): Boolean = error == null
    
    /**
     * Check if there are any warnings.
     * 
     * @return true if warnings exist
     */
    fun hasWarnings(): Boolean = warnings.isNotEmpty()
    
    /**
     * Get a formatted string of all warnings.
     * 
     * @return Formatted warnings string
     */
    fun getWarningsText(): String = warnings.joinToString("; ")
}
