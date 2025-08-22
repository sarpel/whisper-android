package com.app.whisper.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing transcription results in the local database.
 * 
 * This entity represents a completed transcription with all associated metadata,
 * including the transcribed text, confidence scores, and processing information.
 */
@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    
    @ColumnInfo(name = "text")
    val text: String,
    
    @ColumnInfo(name = "confidence")
    val confidence: Float,
    
    @ColumnInfo(name = "language")
    val language: String,
    
    @ColumnInfo(name = "processing_time_ms")
    val processingTimeMs: Long,
    
    @ColumnInfo(name = "model_id")
    val modelId: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "audio_length_ms")
    val audioLengthMs: Long,
    
    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String? = null,
    
    @ColumnInfo(name = "audio_file_size")
    val audioFileSize: Long? = null,
    
    @ColumnInfo(name = "sample_rate")
    val sampleRate: Int? = null,
    
    @ColumnInfo(name = "channels")
    val channels: Int? = null,
    
    @ColumnInfo(name = "tags")
    val tags: String? = null, // JSON string of tags
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    
    /**
     * Get tags as a list.
     * 
     * @return List of tags, empty if no tags
     */
    fun getTagsList(): List<String> {
        return if (tags.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                // Simple comma-separated parsing
                tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Get formatted duration string.
     * 
     * @return Duration in MM:SS format
     */
    fun getFormattedDuration(): String {
        val totalSeconds = audioLengthMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Get formatted processing time.
     * 
     * @return Processing time in seconds with 2 decimal places
     */
    fun getFormattedProcessingTime(): String {
        val seconds = processingTimeMs / 1000.0
        return String.format("%.2fs", seconds)
    }
    
    /**
     * Get confidence as percentage.
     * 
     * @return Confidence as percentage (0-100)
     */
    fun getConfidencePercentage(): Int {
        return (confidence * 100).toInt()
    }
    
    /**
     * Check if transcription is high quality.
     * 
     * @return true if confidence is above 80%
     */
    fun isHighQuality(): Boolean {
        return confidence >= 0.8f
    }
    
    /**
     * Get word count estimate.
     * 
     * @return Estimated word count
     */
    fun getWordCount(): Int {
        return text.split("\\s+".toRegex()).size
    }
    
    /**
     * Get characters per second rate.
     * 
     * @return Characters per second
     */
    fun getCharactersPerSecond(): Double {
        val durationSeconds = audioLengthMs / 1000.0
        return if (durationSeconds > 0) text.length / durationSeconds else 0.0
    }
    
    /**
     * Check if this is a long transcription.
     * 
     * @return true if audio is longer than 5 minutes
     */
    fun isLongTranscription(): Boolean {
        return audioLengthMs > 5 * 60 * 1000 // 5 minutes
    }
    
    /**
     * Get file size in human readable format.
     * 
     * @return File size string (e.g., "1.2 MB")
     */
    fun getFormattedFileSize(): String {
        val size = audioFileSize ?: return "Unknown"
        return when {
            size < 1024 -> "${size}B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * Create a copy with updated tags.
     * 
     * @param newTags List of new tags
     * @return Updated entity
     */
    fun withTags(newTags: List<String>): TranscriptionEntity {
        val tagsString = if (newTags.isEmpty()) null else newTags.joinToString(",")
        return copy(tags = tagsString, updatedAt = System.currentTimeMillis())
    }
    
    /**
     * Create a copy with updated notes.
     * 
     * @param newNotes New notes text
     * @return Updated entity
     */
    fun withNotes(newNotes: String?): TranscriptionEntity {
        return copy(notes = newNotes, updatedAt = System.currentTimeMillis())
    }
    
    /**
     * Create a copy with updated favorite status.
     * 
     * @param favorite New favorite status
     * @return Updated entity
     */
    fun withFavorite(favorite: Boolean): TranscriptionEntity {
        return copy(isFavorite = favorite, updatedAt = System.currentTimeMillis())
    }
}
