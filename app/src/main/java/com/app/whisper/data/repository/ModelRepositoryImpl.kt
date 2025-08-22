package com.app.whisper.data.repository

import android.content.Context
import androidx.tracing.trace
import com.app.whisper.data.local.database.dao.ModelDao
import com.app.whisper.data.local.database.entity.ModelEntity
import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.domain.repository.CleanupInfo
import com.app.whisper.domain.repository.DownloadProgress
import com.app.whisper.domain.repository.DownloadStatus
import com.app.whisper.domain.repository.ModelRepository
import com.app.whisper.domain.repository.ModelStorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ModelRepository for managing Whisper model downloads and storage.
 *
 * Handles model download operations, validation, storage management,
 * and cleanup operations with proper error handling and progress tracking.
 */
@Singleton
class ModelRepositoryImpl @Inject constructor(
    private val context: Context,
    private val modelDao: ModelDao,
    private val httpClient: OkHttpClient
) : ModelRepository {

    private val modelsDir = File(context.filesDir, "whisper_models")

    init {
        // Ensure models directory exists
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
    }

    override suspend fun downloadModel(model: WhisperModel): Flow<DownloadProgress> = flow {
        trace("ModelRepositoryImpl.downloadModel") {
            try {
                Timber.d("Starting download for model: ${model.name}")

                val modelFile = File(modelsDir, "${model.name}.bin")

                // Check if model already exists and is valid
                if (modelFile.exists() && validateModel(model, modelFile)) {
                    emit(DownloadProgress(
                        modelId = model.id,
                        progress = 1.0f,
                        downloadedBytes = modelFile.length(),
                        totalBytes = modelFile.length(),
                        status = DownloadStatus.COMPLETED
                    ))
                    return@trace
                }

                emit(DownloadProgress(
                    modelId = model.id,
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = model.fileSizeBytes,
                    status = DownloadStatus.PENDING
                ))

                // Create HTTP request
                val request = Request.Builder()
                    .url(model.downloadUrl)
                    .build()

                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    emit(DownloadProgress(
                        modelId = model.id,
                        progress = 0f,
                        downloadedBytes = 0L,
                        totalBytes = model.fileSizeBytes,
                        status = DownloadStatus.FAILED,
                        error = "HTTP ${response.code}: ${response.message}"
                    ))
                    return@trace
                }

                val responseBody = response.body ?: throw IOException("Empty response body")
                val totalBytes = responseBody.contentLength()

                emit(DownloadProgress(
                    modelId = model.id,
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = totalBytes,
                    status = DownloadStatus.DOWNLOADING
                ))

                // Download with progress tracking
                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(modelFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var downloadedBytes = 0L
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val progress = if (totalBytes > 0) {
                                downloadedBytes.toFloat() / totalBytes.toFloat()
                            } else {
                                0f
                            }

                            emit(DownloadProgress(
                                modelId = model.id,
                                progress = progress,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                status = DownloadStatus.DOWNLOADING,
                                downloadSpeedBytesPerSecond = calculateDownloadSpeed(downloadedBytes)
                            ))
                        }
                    }
                }

                // Validate downloaded model
                if (validateModel(model, modelFile)) {
                    // Update model status
                    model.updateStatus(com.app.whisper.domain.entity.ModelStatus.Available, modelFile.absolutePath)

                    // Save to database
                    saveModelToDatabase(model, modelFile)

                    emit(DownloadProgress(
                        modelId = model.id,
                        progress = 1.0f,
                        downloadedBytes = modelFile.length(),
                        totalBytes = modelFile.length(),
                        status = DownloadStatus.COMPLETED
                    ))

                    Timber.d("Successfully downloaded model: ${model.name}")
                } else {
                    // Delete invalid file
                    modelFile.delete()

                    emit(DownloadProgress(
                        modelId = model.id,
                        progress = 0f,
                        downloadedBytes = 0L,
                        totalBytes = model.fileSizeBytes,
                        status = DownloadStatus.FAILED,
                        error = "Model validation failed"
                    ))
                }

            } catch (e: Exception) {
                Timber.e(e, "Failed to download model: ${model.name}")
                emit(DownloadProgress(
                    modelId = model.id,
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = model.fileSizeBytes,
                    status = DownloadStatus.FAILED,
                    error = e.message ?: "Unknown error"
                ))
            }
        }
    }

    override suspend fun deleteModel(model: WhisperModel): Result<Unit> = withContext(Dispatchers.IO) {
        trace("ModelRepositoryImpl.deleteModel") {
            try {
                val modelFile = File(modelsDir, "${model.name}.bin")

                if (modelFile.exists()) {
                    val deleted = modelFile.delete()
                    if (!deleted) {
                        return@trace Result.failure(IOException("Failed to delete model file"))
                    }
                }

                // Update model status
                model.updateStatus(com.app.whisper.domain.entity.ModelStatus.NotDownloaded, null)

                // Remove from database
                modelDao.deleteModel(model.id)

                Timber.d("Successfully deleted model: ${model.name}")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete model: ${model.name}")
                Result.failure(e)
            }
        }
    }

    override suspend fun validateModel(model: WhisperModel): Result<Boolean> = withContext(Dispatchers.IO) {
        trace("ModelRepositoryImpl.validateModel") {
            try {
                val modelFile = File(modelsDir, "${model.name}.bin")
                val isValid = validateModel(model, modelFile)
                Result.success(isValid)
            } catch (e: Exception) {
                Timber.e(e, "Failed to validate model: ${model.name}")
                Result.failure(e)
            }
        }
    }

    override suspend fun getAvailableModels(): Flow<List<WhisperModel>> {
        return modelDao.getAllModels().map { entities ->
            entities.map { entity ->
                val model = WhisperModel.getById(entity.modelId) ?: WhisperModel.TINY
                model.updateStatus(
                    com.app.whisper.domain.entity.ModelStatus.valueOf(entity.status),
                    entity.localPath
                )
                model
            }
        }
    }

    override suspend fun getStorageInfo(): ModelStorageInfo = withContext(Dispatchers.IO) {
        trace("ModelRepositoryImpl.getStorageInfo") {
            val totalSpace = modelsDir.totalSpace
            val freeSpace = modelsDir.freeSpace
            val usedSpace = totalSpace - freeSpace

            val modelFiles = modelsDir.listFiles()?.filter { it.name.endsWith(".bin") } ?: emptyList()
            val modelsSize = modelFiles.sumOf { it.length() }

            ModelStorageInfo(
                totalSpaceBytes = totalSpace,
                freeSpaceBytes = freeSpace,
                usedSpaceBytes = usedSpace,
                modelsSpaceBytes = modelsSize,
                modelCount = modelFiles.size
            )
        }
    }

    override suspend fun cleanupOldModels(keepRecentCount: Int): Result<CleanupInfo> = withContext(Dispatchers.IO) {
        trace("ModelRepositoryImpl.cleanupOldModels") {
            try {
                val modelEntities = modelDao.getAllModelsSync()
                val sortedByLastUsed = modelEntities.sortedByDescending { it.lastUsedAt }

                val toDelete = sortedByLastUsed.drop(keepRecentCount)
                var deletedCount = 0
                var freedBytes = 0L

                for (entity in toDelete) {
                    val modelFile = File(entity.localPath ?: continue)
                    if (modelFile.exists()) {
                        val fileSize = modelFile.length()
                        if (modelFile.delete()) {
                            deletedCount++
                            freedBytes += fileSize
                            modelDao.deleteModel(entity.modelId)
                        }
                    }
                }

                val cleanupInfo = CleanupInfo(
                    deletedModelCount = deletedCount,
                    freedSpaceBytes = freedBytes,
                    remainingModelCount = modelEntities.size - deletedCount
                )

                Timber.d("Cleanup completed: $cleanupInfo")
                Result.success(cleanupInfo)
            } catch (e: Exception) {
                Timber.e(e, "Failed to cleanup old models")
                Result.failure(e)
            }
        }
    }

    private fun validateModel(model: WhisperModel, file: File): Boolean {
        if (!file.exists()) return false

        // Check file size
        if (file.length() != model.fileSizeBytes) {
            Timber.w("Model file size mismatch: expected ${model.fileSizeBytes}, got ${file.length()}")
            return false
        }

        // TODO: Implement checksum validation when checksums are available
        // For now, just check if file exists and has correct size
        return true
    }

    private suspend fun saveModelToDatabase(model: WhisperModel, file: File) {
        try {
            val entity = ModelEntity(
                modelId = model.id,
                name = model.name,
                status = com.app.whisper.domain.entity.ModelStatus.Available.name,
                localPath = file.absolutePath,
                downloadedAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                fileSizeBytes = file.length()
            )

            modelDao.insertModel(entity)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save model to database")
        }
    }

    private fun calculateDownloadSpeed(downloadedBytes: Long): Long {
        // Simple speed calculation - in a real implementation, this would track time
        return downloadedBytes / 1024 // KB/s approximation
    }
}
