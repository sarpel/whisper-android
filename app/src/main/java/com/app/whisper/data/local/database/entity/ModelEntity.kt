package com.app.whisper.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing Whisper model information in the local database.
 * 
 * This entity represents a Whisper model with its download status, local path,
 * and usage statistics for efficient model management.
 */
@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey
    @ColumnInfo(name = "model_id")
    val modelId: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "display_name")
    val displayName: String? = null,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "status")
    val status: String, // ModelStatus enum as string
    
    @ColumnInfo(name = "local_path")
    val localPath: String? = null,
    
    @ColumnInfo(name = "download_url")
    val downloadUrl: String? = null,
    
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,
    
    @ColumnInfo(name = "checksum")
    val checksum: String? = null,
    
    @ColumnInfo(name = "version")
    val version: String? = null,
    
    @ColumnInfo(name = "is_multilingual")
    val isMultilingual: Boolean = false,
    
    @ColumnInfo(name = "supported_languages")
    val supportedLanguages: String? = null, // JSON string of language codes
    
    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long? = null,
    
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long? = null,
    
    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,
    
    @ColumnInfo(name = "total_processing_time_ms")
    val totalProcessingTimeMs: Long = 0L,
    
    @ColumnInfo(name = "average_confidence")
    val averageConfidence: Float? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    
    /**
     * Check if model is downloaded and available.
     * 
     * @return true if model is available for use
     */
    fun isAvailable(): Boolean {
        return status == "Available" && !localPath.isNullOrBlank()
    }
    
    /**
     * Check if model is currently downloading.
     * 
     * @return true if model is being downloaded
     */
    fun isDownloading(): Boolean {
        return status == "Downloading"
    }
    
    /**
     * Check if model download failed or is corrupted.
     * 
     * @return true if model has errors
     */
    fun hasError(): Boolean {
        return status == "Error" || status == "Corrupted"
    }
    
    /**
     * Get supported languages as a list.
     * 
     * @return List of language codes, empty if not multilingual
     */
    fun getSupportedLanguagesList(): List<String> {
        return if (supportedLanguages.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                // Simple comma-separated parsing
                supportedLanguages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Get file size in human readable format.
     * 
     * @return File size string (e.g., "142 MB")
     */
    fun getFormattedFileSize(): String {
        return when {
            fileSizeBytes < 1024 -> "${fileSizeBytes}B"
            fileSizeBytes < 1024 * 1024 -> String.format("%.1f KB", fileSizeBytes / 1024.0)
            fileSizeBytes < 1024 * 1024 * 1024 -> String.format("%.0f MB", fileSizeBytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", fileSizeBytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * Get average processing speed in characters per second.
     * 
     * @return Processing speed or null if no usage data
     */
    fun getAverageProcessingSpeed(): Double? {
        return if (totalProcessingTimeMs > 0 && usageCount > 0) {
            // This is a rough estimate - in practice you'd track actual character counts
            val estimatedCharacters = usageCount * 100 // Rough estimate
            estimatedCharacters / (totalProcessingTimeMs / 1000.0)
        } else {
            null
        }
    }
    
    /**
     * Get formatted last used time.
     * 
     * @return Human readable last used time
     */
    fun getFormattedLastUsed(): String {
        val lastUsed = lastUsedAt ?: return "Never"
        val now = System.currentTimeMillis()
        val diffMs = now - lastUsed
        
        return when {
            diffMs < 60 * 1000 -> "Just now"
            diffMs < 60 * 60 * 1000 -> "${diffMs / (60 * 1000)} minutes ago"
            diffMs < 24 * 60 * 60 * 1000 -> "${diffMs / (60 * 60 * 1000)} hours ago"
            diffMs < 7 * 24 * 60 * 60 * 1000 -> "${diffMs / (24 * 60 * 60 * 1000)} days ago"
            else -> "More than a week ago"
        }
    }
    
    /**
     * Get confidence percentage.
     * 
     * @return Confidence as percentage (0-100) or null if no data
     */
    fun getConfidencePercentage(): Int? {
        return averageConfidence?.let { (it * 100).toInt() }
    }
    
    /**
     * Check if model is frequently used.
     * 
     * @return true if used more than 10 times
     */
    fun isFrequentlyUsed(): Boolean {
        return usageCount > 10
    }
    
    /**
     * Check if model was used recently.
     * 
     * @return true if used within last 7 days
     */
    fun isRecentlyUsed(): Boolean {
        val lastUsed = lastUsedAt ?: return false
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return lastUsed > weekAgo
    }
    
    /**
     * Create a copy with updated usage statistics.
     * 
     * @param processingTimeMs Processing time for this usage
     * @param confidence Confidence score for this usage
     * @return Updated entity
     */
    fun withUsage(processingTimeMs: Long, confidence: Float): ModelEntity {
        val newUsageCount = usageCount + 1
        val newTotalProcessingTime = totalProcessingTimeMs + processingTimeMs
        val newAverageConfidence = if (averageConfidence == null) {
            confidence
        } else {
            (averageConfidence * usageCount + confidence) / newUsageCount
        }
        
        return copy(
            usageCount = newUsageCount,
            totalProcessingTimeMs = newTotalProcessingTime,
            averageConfidence = newAverageConfidence,
            lastUsedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Create a copy with updated status.
     * 
     * @param newStatus New model status
     * @param newLocalPath New local path (optional)
     * @return Updated entity
     */
    fun withStatus(newStatus: String, newLocalPath: String? = null): ModelEntity {
        return copy(
            status = newStatus,
            localPath = newLocalPath ?: localPath,
            downloadedAt = if (newStatus == "Available") System.currentTimeMillis() else downloadedAt,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Create a copy with updated supported languages.
     * 
     * @param languages List of language codes
     * @return Updated entity
     */
    fun withSupportedLanguages(languages: List<String>): ModelEntity {
        val languagesString = if (languages.isEmpty()) null else languages.joinToString(",")
        return copy(
            supportedLanguages = languagesString,
            isMultilingual = languages.size > 1,
            updatedAt = System.currentTimeMillis()
        )
    }
}
