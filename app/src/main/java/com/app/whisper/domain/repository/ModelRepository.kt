package com.app.whisper.domain.repository

import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.domain.entity.ModelStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Whisper model management operations.
 * 
 * This interface defines the contract for model-related data operations,
 * including downloading, storing, validating, and managing Whisper models.
 */
interface ModelRepository {
    
    /**
     * Get all available Whisper models.
     * 
     * @param includeNotDownloaded Whether to include models that are not downloaded
     * @return List of available models
     */
    suspend fun getAllModels(includeNotDownloaded: Boolean = true): List<WhisperModel>
    
    /**
     * Get a specific model by ID.
     * 
     * @param modelId Model identifier
     * @return Model if found, null otherwise
     */
    suspend fun getModel(modelId: String): WhisperModel?
    
    /**
     * Get models with specific status.
     * 
     * @param status Model status to filter by
     * @return List of models with the specified status
     */
    suspend fun getModelsByStatus(status: ModelStatus): List<WhisperModel>
    
    /**
     * Get downloaded models that are ready for use.
     * 
     * @return List of available models
     */
    suspend fun getAvailableModels(): List<WhisperModel>
    
    /**
     * Get the currently active/selected model.
     * 
     * @return Currently active model, or null if none selected
     */
    suspend fun getCurrentModel(): WhisperModel?
    
    /**
     * Set the current active model.
     * 
     * @param modelId Model ID to set as current
     * @return Result indicating success or failure
     */
    suspend fun setCurrentModel(modelId: String): Result<Unit>
    
    /**
     * Download a Whisper model.
     * 
     * @param modelId Model ID to download
     * @return Flow of download progress (0.0 to 1.0)
     */
    fun downloadModel(modelId: String): Flow<DownloadProgress>
    
    /**
     * Cancel an ongoing model download.
     * 
     * @param modelId Model ID to cancel download for
     * @return Result indicating success or failure
     */
    suspend fun cancelDownload(modelId: String): Result<Unit>
    
    /**
     * Delete a downloaded model.
     * 
     * @param modelId Model ID to delete
     * @param deleteFiles Whether to delete associated files
     * @return Result indicating success or failure
     */
    suspend fun deleteModel(modelId: String, deleteFiles: Boolean = true): Result<Unit>
    
    /**
     * Validate a downloaded model's integrity.
     * 
     * @param modelId Model ID to validate
     * @return Result indicating validation success or failure
     */
    suspend fun validateModel(modelId: String): Result<Unit>
    
    /**
     * Update model metadata.
     * 
     * @param model Updated model information
     * @return Result indicating success or failure
     */
    suspend fun updateModel(model: WhisperModel): Result<Unit>
    
    /**
     * Check for model updates.
     * 
     * @param modelId Model ID to check for updates
     * @return Result containing update information or error
     */
    suspend fun checkForUpdates(modelId: String): Result<ModelUpdateInfo>
    
    /**
     * Update a model to the latest version.
     * 
     * @param modelId Model ID to update
     * @return Flow of update progress
     */
    fun updateModel(modelId: String): Flow<DownloadProgress>
    
    /**
     * Get model storage information.
     * 
     * @return Storage information including used/available space
     */
    suspend fun getStorageInfo(): ModelStorageInfo
    
    /**
     * Clean up unused model files and temporary data.
     * 
     * @return Result containing cleanup information
     */
    suspend fun cleanup(): Result<CleanupInfo>
    
    /**
     * Get model usage statistics.
     * 
     * @param modelId Model ID to get stats for, or null for all models
     * @return Map containing usage statistics
     */
    suspend fun getUsageStats(modelId: String? = null): Map<String, Any>
    
    /**
     * Get recommended model for current device.
     * 
     * @param preferSpeed Whether to prioritize speed over quality
     * @return Recommended model
     */
    suspend fun getRecommendedModel(preferSpeed: Boolean = false): WhisperModel
    
    /**
     * Observe model list changes as a Flow.
     * 
     * @return Flow of model lists that updates when models change
     */
    fun observeModels(): Flow<List<WhisperModel>>
    
    /**
     * Observe a specific model as a Flow.
     * 
     * @param modelId Model ID to observe
     * @return Flow of model updates
     */
    fun observeModel(modelId: String): Flow<WhisperModel?>
    
    /**
     * Observe current model changes.
     * 
     * @return Flow of current model updates
     */
    fun observeCurrentModel(): Flow<WhisperModel?>
    
    /**
     * Observe download progress for all models.
     * 
     * @return Flow of download progress updates
     */
    fun observeDownloads(): Flow<Map<String, DownloadProgress>>
    
    /**
     * Import a custom model.
     * 
     * @param filePath Path to model file
     * @param modelInfo Model information
     * @return Result containing imported model or error
     */
    suspend fun importModel(filePath: String, modelInfo: WhisperModel): Result<WhisperModel>
    
    /**
     * Export a model for sharing.
     * 
     * @param modelId Model ID to export
     * @param destinationPath Destination path for export
     * @return Result indicating success or failure
     */
    suspend fun exportModel(modelId: String, destinationPath: String): Result<Unit>
    
    /**
     * Backup model configuration and metadata.
     * 
     * @return Result containing backup data or error
     */
    suspend fun backup(): Result<String>
    
    /**
     * Restore model configuration from backup.
     * 
     * @param backupData Backup data to restore
     * @return Result indicating success or failure
     */
    suspend fun restore(backupData: String): Result<Unit>
}

/**
 * Data class representing download progress.
 */
data class DownloadProgress(
    val modelId: String,
    val progress: Float, // 0.0 to 1.0
    val downloadedBytes: Long,
    val totalBytes: Long,
    val status: DownloadStatus,
    val error: String? = null,
    val estimatedTimeRemainingMs: Long? = null,
    val downloadSpeedBytesPerSecond: Long = 0L
) {
    
    /**
     * Check if download is completed.
     * 
     * @return true if download is complete
     */
    fun isCompleted(): Boolean = status == DownloadStatus.COMPLETED
    
    /**
     * Check if download failed.
     * 
     * @return true if download failed
     */
    fun isFailed(): Boolean = status == DownloadStatus.FAILED
    
    /**
     * Check if download is in progress.
     * 
     * @return true if download is active
     */
    fun isInProgress(): Boolean = status == DownloadStatus.DOWNLOADING
    
    /**
     * Get formatted progress percentage.
     * 
     * @return Progress as percentage string (e.g., "45%")
     */
    fun getFormattedProgress(): String = "${(progress * 100).toInt()}%"
    
    /**
     * Get formatted download speed.
     * 
     * @return Speed in human-readable format
     */
    fun getFormattedSpeed(): String = when {
        downloadSpeedBytesPerSecond < 1024 -> "${downloadSpeedBytesPerSecond} B/s"
        downloadSpeedBytesPerSecond < 1024 * 1024 -> "${downloadSpeedBytesPerSecond / 1024} KB/s"
        else -> String.format("%.1f MB/s", downloadSpeedBytesPerSecond / (1024.0 * 1024.0))
    }
    
    /**
     * Get formatted estimated time remaining.
     * 
     * @return Time remaining in human-readable format
     */
    fun getFormattedTimeRemaining(): String? {
        return estimatedTimeRemainingMs?.let { timeMs ->
            val seconds = timeMs / 1000
            when {
                seconds < 60 -> "${seconds}s"
                seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
                else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
            }
        }
    }
}

/**
 * Enumeration of download status.
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Data class containing model update information.
 */
data class ModelUpdateInfo(
    val modelId: String,
    val currentVersion: String,
    val latestVersion: String,
    val hasUpdate: Boolean,
    val updateSize: Long,
    val releaseNotes: String = "",
    val isRequired: Boolean = false
)

/**
 * Data class containing model storage information.
 */
data class ModelStorageInfo(
    val totalSpaceBytes: Long,
    val usedSpaceBytes: Long,
    val availableSpaceBytes: Long,
    val modelCount: Int,
    val largestModelSize: Long,
    val oldestModelDate: Long?
) {
    
    /**
     * Get used space as percentage.
     * 
     * @return Used space percentage (0.0 to 1.0)
     */
    fun getUsedSpacePercentage(): Float = usedSpaceBytes.toFloat() / totalSpaceBytes.toFloat()
    
    /**
     * Check if storage is running low.
     * 
     * @param threshold Low space threshold (default: 0.9 = 90%)
     * @return true if storage usage is above threshold
     */
    fun isStorageLow(threshold: Float = 0.9f): Boolean = getUsedSpacePercentage() > threshold
    
    /**
     * Get formatted storage usage string.
     * 
     * @return Storage usage in human-readable format
     */
    fun getFormattedUsage(): String {
        val usedMB = usedSpaceBytes / (1024 * 1024)
        val totalMB = totalSpaceBytes / (1024 * 1024)
        return "$usedMB MB / $totalMB MB (${(getUsedSpacePercentage() * 100).toInt()}%)"
    }
}

/**
 * Data class containing cleanup operation results.
 */
data class CleanupInfo(
    val freedSpaceBytes: Long,
    val deletedFiles: Int,
    val cleanedTempFiles: Int,
    val errors: List<String> = emptyList()
) {
    
    /**
     * Check if cleanup was successful.
     * 
     * @return true if no errors occurred
     */
    fun isSuccessful(): Boolean = errors.isEmpty()
    
    /**
     * Get formatted freed space string.
     * 
     * @return Freed space in human-readable format
     */
    fun getFormattedFreedSpace(): String = when {
        freedSpaceBytes < 1024 * 1024 -> "${freedSpaceBytes / 1024} KB"
        freedSpaceBytes < 1024 * 1024 * 1024 -> "${freedSpaceBytes / (1024 * 1024)} MB"
        else -> String.format("%.1f GB", freedSpaceBytes / (1024.0 * 1024.0 * 1024.0))
    }
}
