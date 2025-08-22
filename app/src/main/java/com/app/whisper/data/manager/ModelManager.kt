package com.app.whisper.data.manager

import android.content.Context
import android.content.SharedPreferences
import com.app.whisper.domain.entity.ModelStatus
import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.domain.repository.DownloadProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Whisper model downloads, storage, and lifecycle.
 * 
 * This class handles model downloading, verification, storage management,
 * and provides status updates for model operations.
 */
@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val preferences: SharedPreferences
) {
    
    private val modelsDir = File(context.filesDir, "models")
    
    // Model states
    private val _downloadProgress = MutableStateFlow<Map<WhisperModel, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<WhisperModel, DownloadProgress>> = _downloadProgress.asStateFlow()
    
    private val _currentModel = MutableStateFlow<WhisperModel?>(null)
    val currentModel: StateFlow<WhisperModel?> = _currentModel.asStateFlow()
    
    init {
        initializeModelsDirectory()
        loadModelStates()
        loadCurrentModel()
    }
    
    /**
     * Initialize models directory.
     */
    private fun initializeModelsDirectory() {
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
    }
    
    /**
     * Load model states from storage.
     */
    private fun loadModelStates() {
        WhisperModel.values().forEach { model ->
            val modelFile = File(modelsDir, "${model.id}.bin")
            if (modelFile.exists()) {
                model.status = ModelStatus.Available
                model.localPath = modelFile.absolutePath
                model.downloadedAt = preferences.getLong("${model.id}_downloaded_at", 0L)
                model.lastUsedAt = preferences.getLong("${model.id}_last_used_at", 0L)
            } else {
                model.status = ModelStatus.NotDownloaded
                model.localPath = null
            }
        }
    }
    
    /**
     * Load current model from preferences.
     */
    private fun loadCurrentModel() {
        val currentModelId = preferences.getString("current_model", null)
        _currentModel.value = currentModelId?.let { id ->
            WhisperModel.values().find { it.id == id }
        }
        
        // Update current model flag
        WhisperModel.values().forEach { model ->
            model.isCurrent = model == _currentModel.value
        }
    }
    
    /**
     * Set current model.
     */
    suspend fun setCurrentModel(model: WhisperModel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!model.isAvailable()) {
                return@withContext Result.failure(IllegalStateException("Model is not available"))
            }
            
            // Update previous current model
            _currentModel.value?.let { previousModel ->
                previousModel.isCurrent = false
            }
            
            // Set new current model
            model.isCurrent = true
            model.lastUsedAt = System.currentTimeMillis()
            _currentModel.value = model
            
            // Save to preferences
            preferences.edit()
                .putString("current_model", model.id)
                .putLong("${model.id}_last_used_at", model.lastUsedAt!!)
                .apply()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Download a model.
     */
    suspend fun downloadModel(model: WhisperModel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (model.isAvailable()) {
                return@withContext Result.success(Unit)
            }
            
            // Update status
            model.status = ModelStatus.Downloading
            
            val modelFile = File(modelsDir, "${model.id}.bin")
            val tempFile = File(modelsDir, "${model.id}.bin.tmp")
            
            // Create request
            val request = Request.Builder()
                .url(model.downloadUrl)
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                model.status = ModelStatus.Error
                return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            }
            
            val contentLength = response.body?.contentLength() ?: -1L
            var downloadedBytes = 0L
            
            response.body?.byteStream()?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        // Update progress
                        val progress = if (contentLength > 0) {
                            (downloadedBytes.toFloat() / contentLength.toFloat())
                        } else {
                            -1f // Indeterminate progress
                        }
                        
                        val downloadProgress = DownloadProgress.InProgress(
                            downloadedBytes = downloadedBytes,
                            totalBytes = contentLength,
                            progress = progress
                        )
                        
                        _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                            put(model, downloadProgress)
                        }
                    }
                }
            }
            
            // Verify checksum
            if (!verifyChecksum(tempFile, model.checksum)) {
                tempFile.delete()
                model.status = ModelStatus.Error
                return@withContext Result.failure(Exception("Checksum verification failed"))
            }
            
            // Move temp file to final location
            if (tempFile.renameTo(modelFile)) {
                model.status = ModelStatus.Available
                model.localPath = modelFile.absolutePath
                model.downloadedAt = System.currentTimeMillis()
                
                // Save to preferences
                preferences.edit()
                    .putLong("${model.id}_downloaded_at", model.downloadedAt!!)
                    .apply()
                
                // Update progress to completed
                _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                    put(model, DownloadProgress.Completed(downloadedBytes))
                }
                
                Result.success(Unit)
            } else {
                tempFile.delete()
                model.status = ModelStatus.Error
                Result.failure(Exception("Failed to move downloaded file"))
            }
            
        } catch (e: Exception) {
            model.status = ModelStatus.Error
            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                put(model, DownloadProgress.Failed(e))
            }
            Result.failure(e)
        }
    }
    
    /**
     * Delete a model.
     */
    suspend fun deleteModel(model: WhisperModel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(modelsDir, "${model.id}.bin")
            
            if (modelFile.exists() && modelFile.delete()) {
                model.status = ModelStatus.NotDownloaded
                model.localPath = null
                model.downloadedAt = null
                
                // If this was the current model, clear it
                if (model.isCurrent) {
                    model.isCurrent = false
                    _currentModel.value = null
                    preferences.edit().remove("current_model").apply()
                }
                
                // Clear preferences
                preferences.edit()
                    .remove("${model.id}_downloaded_at")
                    .remove("${model.id}_last_used_at")
                    .apply()
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete model file"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all available models.
     */
    fun getAllModels(): List<WhisperModel> {
        return WhisperModel.values().toList()
    }
    
    /**
     * Get downloaded models.
     */
    fun getDownloadedModels(): List<WhisperModel> {
        return WhisperModel.values().filter { it.isAvailable() }
    }
    
    /**
     * Get model by ID.
     */
    fun getModelById(id: String): WhisperModel? {
        return WhisperModel.values().find { it.id == id }
    }
    
    /**
     * Get storage usage information.
     */
    fun getStorageInfo(): StorageInfo {
        val downloadedModels = getDownloadedModels()
        val totalSize = downloadedModels.sumOf { it.fileSizeBytes }
        val availableSpace = modelsDir.freeSpace
        
        return StorageInfo(
            totalDownloadedSize = totalSize,
            availableSpace = availableSpace,
            downloadedModelsCount = downloadedModels.size,
            totalModelsCount = WhisperModel.values().size
        )
    }
    
    /**
     * Verify file checksum.
     */
    private fun verifyChecksum(file: File, expectedChecksum: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            val actualChecksum = digest.digest().joinToString("") { 
                "%02x".format(it) 
            }
            
            actualChecksum.equals(expectedChecksum, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Cancel model download.
     */
    suspend fun cancelDownload(model: WhisperModel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (model.status == ModelStatus.Downloading) {
                model.status = ModelStatus.NotDownloaded
                
                // Delete temp file if exists
                val tempFile = File(modelsDir, "${model.id}.bin.tmp")
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                
                // Remove from progress
                _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                    remove(model)
                }
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("Model is not being downloaded"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Storage information data class.
 */
data class StorageInfo(
    val totalDownloadedSize: Long,
    val availableSpace: Long,
    val downloadedModelsCount: Int,
    val totalModelsCount: Int
) {
    fun getFormattedTotalSize(): String {
        return formatBytes(totalDownloadedSize)
    }
    
    fun getFormattedAvailableSpace(): String {
        return formatBytes(availableSpace)
    }
    
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return "%.1f %s".format(size, units[unitIndex])
    }
}
