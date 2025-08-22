package com.app.whisper.domain.usecase

import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.domain.repository.DownloadProgress
import com.app.whisper.domain.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for downloading Whisper models.
 *
 * This use case orchestrates the model download process, including validation,
 * progress tracking, and post-download verification. It encapsulates the business
 * logic for model management operations.
 */
@Singleton
class DownloadModelUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {

    /**
     * Download a Whisper model.
     *
     * @param modelId Model ID to download
     * @return Flow of download progress
     */
    suspend fun execute(modelId: String): Flow<ModelDownloadResult> {
        return modelRepository.downloadModel(modelId)
            .onStart {
                // Validate model before starting download
                val model = modelRepository.getModel(modelId)
                if (model == null) {
                    throw IllegalArgumentException("Model not found: $modelId")
                }

                if (model.isAvailable()) {
                    throw IllegalStateException("Model is already downloaded: $modelId")
                }

                // Check storage space
                val storageInfo = modelRepository.getStorageInfo()
                if (storageInfo.availableSpaceBytes < model.fileSizeBytes * 1.2) { // 20% buffer
                    throw IllegalStateException("Insufficient storage space for model download")
                }
            }
            .map { progress ->
                when {
                    progress.isCompleted() -> {
                        // Validate downloaded model
                        val validationResult = modelRepository.validateModel(modelId)
                        if (validationResult.isSuccess) {
                            ModelDownloadResult.Completed(progress)
                        } else {
                            ModelDownloadResult.Failed(
                                progress.copy(
                                    status = com.app.whisper.domain.repository.DownloadStatus.FAILED,
                                    error = "Model validation failed: ${validationResult.exceptionOrNull()?.message}"
                                )
                            )
                        }
                    }
                    progress.isFailed() -> ModelDownloadResult.Failed(progress)
                    progress.isInProgress() -> ModelDownloadResult.InProgress(progress)
                    else -> ModelDownloadResult.InProgress(progress)
                }
            }
            .catch { error ->
                emit(ModelDownloadResult.Failed(
                    DownloadProgress(
                        modelId = modelId,
                        progress = 0.0f,
                        downloadedBytes = 0L,
                        totalBytes = 0L,
                        status = com.app.whisper.domain.repository.DownloadStatus.FAILED,
                        error = error.message
                    )
                ))
            }
            .onCompletion { error ->
                if (error != null) {
                    // Clean up failed download
                    try {
                        modelRepository.deleteModel(modelId, deleteFiles = true)
                    } catch (e: Exception) {
                        // Log cleanup error but don't throw
                    }
                }
            }
    }

    /**
     * Cancel an ongoing model download.
     *
     * @param modelId Model ID to cancel download for
     * @return Result indicating success or failure
     */
    suspend fun cancelDownload(modelId: String): Result<Unit> {
        return try {
            val cancelResult = modelRepository.cancelDownload(modelId)
            if (cancelResult.isSuccess) {
                // Clean up partial download
                modelRepository.deleteModel(modelId, deleteFiles = true)
            }
            cancelResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get recommended model for the current device.
     *
     * @param preferSpeed Whether to prioritize speed over quality
     * @return Recommended model
     */
    suspend fun getRecommendedModel(preferSpeed: Boolean = false): Result<WhisperModel> {
        return try {
            val model = modelRepository.getRecommendedModel(preferSpeed)
            Result.success(model)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if a model can be downloaded on the current device.
     *
     * @param modelId Model ID to check
     * @return Result containing compatibility information
     */
    suspend fun checkDownloadCompatibility(modelId: String): Result<DownloadCompatibility> {
        return try {
            val model = modelRepository.getModel(modelId)
                ?: return Result.failure(IllegalArgumentException("Model not found: $modelId"))

            val storageInfo = modelRepository.getStorageInfo()
            val availableMemoryMB = Runtime.getRuntime().maxMemory() / (1024 * 1024)

            val compatibility = DownloadCompatibility(
                canDownload = true,
                hasEnoughStorage = storageInfo.availableSpaceBytes >= model.fileSizeBytes * 1.2,
                hasEnoughMemory = model.isRecommendedFor(availableMemoryMB),
                requiredSpaceBytes = model.fileSizeBytes,
                availableSpaceBytes = storageInfo.availableSpaceBytes,
                requiredMemoryMB = model.getRequiredMemoryMB(),
                availableMemoryMB = availableMemoryMB,
                warnings = buildList {
                    if (!model.isRecommendedFor(availableMemoryMB)) {
                        add("Model may be too large for optimal performance on this device")
                    }
                    if (storageInfo.isStorageLow()) {
                        add("Device storage is running low")
                    }
                }
            )

            Result.success(compatibility.copy(
                canDownload = compatibility.hasEnoughStorage && compatibility.hasEnoughMemory
            ))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all available models with download status.
     *
     * @return List of models with their current status
     */
    suspend fun getAvailableModels(): Result<List<WhisperModel>> {
        return try {
            val models = modelRepository.getAllModels(includeNotDownloaded = true)
            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a downloaded model.
     *
     * @param modelId Model ID to delete
     * @param deleteFiles Whether to delete associated files
     * @return Result indicating success or failure
     */
    suspend fun deleteModel(modelId: String, deleteFiles: Boolean = true): Result<Unit> {
        return try {
            // Check if model is currently in use
            val currentModel = modelRepository.getCurrentModel()
            if (currentModel?.id == modelId) {
                return Result.failure(IllegalStateException("Cannot delete currently active model"))
            }

            modelRepository.deleteModel(modelId, deleteFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set a model as the current active model.
     *
     * @param modelId Model ID to set as current
     * @return Result indicating success or failure
     */
    suspend fun setCurrentModel(modelId: String): Result<Unit> {
        return try {
            val model = modelRepository.getModel(modelId)
                ?: return Result.failure(IllegalArgumentException("Model not found: $modelId"))

            if (!model.isAvailable()) {
                return Result.failure(IllegalStateException("Model is not available: $modelId"))
            }

            modelRepository.setCurrentModel(modelId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Sealed class representing model download results.
 */
sealed class ModelDownloadResult {
    data class InProgress(val progress: DownloadProgress) : ModelDownloadResult()
    data class Completed(val progress: DownloadProgress) : ModelDownloadResult()
    data class Failed(val progress: DownloadProgress) : ModelDownloadResult()

    /**
     * Get the underlying download progress.
     *
     * @return DownloadProgress instance
     */
    fun getDownloadProgress(): DownloadProgress = when (this) {
        is InProgress -> progress
        is Completed -> progress
        is Failed -> progress
    }

    /**
     * Check if download is in progress.
     *
     * @return true if download is active
     */
    fun isInProgress(): Boolean = this is InProgress

    /**
     * Check if download completed successfully.
     *
     * @return true if download completed
     */
    fun isCompleted(): Boolean = this is Completed

    /**
     * Check if download failed.
     *
     * @return true if download failed
     */
    fun isFailed(): Boolean = this is Failed
}

/**
 * Data class containing download compatibility information.
 */
data class DownloadCompatibility(
    val canDownload: Boolean,
    val hasEnoughStorage: Boolean,
    val hasEnoughMemory: Boolean,
    val requiredSpaceBytes: Long,
    val availableSpaceBytes: Long,
    val requiredMemoryMB: Long,
    val availableMemoryMB: Long,
    val warnings: List<String> = emptyList()
) {

    /**
     * Get a human-readable compatibility summary.
     *
     * @return Compatibility summary string
     */
    fun getSummary(): String = buildString {
        if (canDownload) {
            append("Compatible for download")
        } else {
            append("Not compatible: ")
            val issues = mutableListOf<String>()
            if (!hasEnoughStorage) issues.add("insufficient storage")
            if (!hasEnoughMemory) issues.add("insufficient memory")
            append(issues.joinToString(", "))
        }

        if (warnings.isNotEmpty()) {
            append("\nWarnings: ${warnings.joinToString("; ")}")
        }
    }

    /**
     * Get formatted storage information.
     *
     * @return Storage info string
     */
    fun getStorageInfo(): String {
        val requiredMB = requiredSpaceBytes / (1024 * 1024)
        val availableMB = availableSpaceBytes / (1024 * 1024)
        return "Required: ${requiredMB}MB, Available: ${availableMB}MB"
    }

    /**
     * Get formatted memory information.
     *
     * @return Memory info string
     */
    fun getMemoryInfo(): String {
        return "Required: ${requiredMemoryMB}MB, Available: ${availableMemoryMB}MB"
    }
}
