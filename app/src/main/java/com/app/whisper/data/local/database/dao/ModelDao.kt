package com.app.whisper.data.local.database.dao

import androidx.room.*
import com.app.whisper.data.local.database.entity.WhisperModelEntity
import com.app.whisper.domain.entity.ModelStatus
import com.app.whisper.domain.entity.ModelSize
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Whisper model-related database operations.
 * 
 * This DAO provides methods for CRUD operations on Whisper models
 * in the local SQLite database.
 */
@Dao
interface ModelDao {
    
    // Basic CRUD Operations
    
    @Query("SELECT * FROM whisper_models WHERE id = :id")
    suspend fun getModelById(id: String): WhisperModelEntity?
    
    @Query("SELECT * FROM whisper_models ORDER BY name ASC")
    suspend fun getAllModels(): List<WhisperModelEntity>
    
    @Query("SELECT * FROM whisper_models WHERE status = :status ORDER BY name ASC")
    suspend fun getModelsByStatus(status: ModelStatus): List<WhisperModelEntity>
    
    @Query("SELECT * FROM whisper_models WHERE status = 'DOWNLOADED' ORDER BY last_used_at DESC")
    suspend fun getAvailableModels(): List<WhisperModelEntity>
    
    @Query("SELECT * FROM whisper_models WHERE is_current = 1 LIMIT 1")
    suspend fun getCurrentModel(): WhisperModelEntity?
    
    @Query("SELECT * FROM whisper_models WHERE size = :size ORDER BY name ASC")
    suspend fun getModelsBySize(size: ModelSize): List<WhisperModelEntity>
    
    @Query("SELECT * FROM whisper_models WHERE is_multilingual = :isMultilingual ORDER BY name ASC")
    suspend fun getModelsByMultilingualSupport(isMultilingual: Boolean): List<WhisperModelEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: WhisperModelEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<WhisperModelEntity>)
    
    @Update
    suspend fun updateModel(model: WhisperModelEntity)
    
    @Delete
    suspend fun deleteModel(model: WhisperModelEntity)
    
    @Query("DELETE FROM whisper_models WHERE id = :id")
    suspend fun deleteModelById(id: String)
    
    @Query("DELETE FROM whisper_models WHERE id IN (:ids)")
    suspend fun deleteModelsByIds(ids: List<String>)
    
    // Status and Current Model Management
    
    @Query("UPDATE whisper_models SET status = :status WHERE id = :id")
    suspend fun updateModelStatus(id: String, status: ModelStatus)
    
    @Query("UPDATE whisper_models SET status = :status, local_path = :localPath, downloaded_at = :downloadedAt WHERE id = :id")
    suspend fun updateModelDownloaded(id: String, status: ModelStatus, localPath: String, downloadedAt: Long)
    
    @Query("UPDATE whisper_models SET download_progress = :progress WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, progress: Float)
    
    @Query("UPDATE whisper_models SET last_used_at = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)
    
    @Transaction
    suspend fun setCurrentModel(id: String) {
        // Clear current flag from all models
        clearCurrentModel()
        // Set the specified model as current
        setModelAsCurrent(id)
    }
    
    @Query("UPDATE whisper_models SET is_current = 0")
    suspend fun clearCurrentModel()
    
    @Query("UPDATE whisper_models SET is_current = 1 WHERE id = :id")
    suspend fun setModelAsCurrent(id: String)
    
    // Flow-based Observables
    
    @Query("SELECT * FROM whisper_models ORDER BY name ASC")
    fun observeAllModels(): Flow<List<WhisperModelEntity>>
    
    @Query("SELECT * FROM whisper_models WHERE id = :id")
    fun observeModel(id: String): Flow<WhisperModelEntity?>
    
    @Query("SELECT * FROM whisper_models WHERE is_current = 1 LIMIT 1")
    fun observeCurrentModel(): Flow<WhisperModelEntity?>
    
    @Query("SELECT * FROM whisper_models WHERE status = :status ORDER BY name ASC")
    fun observeModelsByStatus(status: ModelStatus): Flow<List<WhisperModelEntity>>
    
    @Query("SELECT * FROM whisper_models WHERE status = 'DOWNLOADED' ORDER BY last_used_at DESC")
    fun observeAvailableModels(): Flow<List<WhisperModelEntity>>
    
    @Query("SELECT * FROM whisper_models WHERE status = 'DOWNLOADING' ORDER BY name ASC")
    fun observeDownloadingModels(): Flow<List<WhisperModelEntity>>
    
    // Statistics and Analytics
    
    @Query("SELECT COUNT(*) FROM whisper_models")
    suspend fun getModelCount(): Int
    
    @Query("SELECT COUNT(*) FROM whisper_models WHERE status = :status")
    suspend fun getModelCountByStatus(status: ModelStatus): Int
    
    @Query("SELECT SUM(file_size_bytes) FROM whisper_models WHERE status = 'DOWNLOADED'")
    suspend fun getTotalDownloadedSize(): Long?
    
    @Query("SELECT size, COUNT(*) as count FROM whisper_models GROUP BY size")
    suspend fun getModelSizeDistribution(): List<ModelSizeDistribution>
    
    @Query("SELECT status, COUNT(*) as count FROM whisper_models GROUP BY status")
    suspend fun getModelStatusDistribution(): List<ModelStatusDistribution>
    
    @Query("SELECT * FROM whisper_models WHERE last_used_at IS NOT NULL ORDER BY last_used_at DESC LIMIT :limit")
    suspend fun getRecentlyUsedModels(limit: Int = 5): List<WhisperModelEntity>
    
    @Query("SELECT AVG(file_size_bytes) FROM whisper_models")
    suspend fun getAverageModelSize(): Float?
    
    // Search and Filter Operations
    
    @Query("SELECT * FROM whisper_models WHERE name LIKE '%' || :query || '%' OR display_name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchModels(query: String): List<WhisperModelEntity>
    
    @Query("""
        SELECT * FROM whisper_models 
        WHERE (:includeNotDownloaded = 1 OR status = 'DOWNLOADED')
        AND (:sizeFilter IS NULL OR size = :sizeFilter)
        AND (:multilingualFilter IS NULL OR is_multilingual = :multilingualFilter)
        ORDER BY name ASC
    """)
    suspend fun getFilteredModels(
        includeNotDownloaded: Boolean = true,
        sizeFilter: ModelSize? = null,
        multilingualFilter: Boolean? = null
    ): List<WhisperModelEntity>
    
    // Maintenance Operations
    
    @Query("DELETE FROM whisper_models WHERE status = 'CORRUPTED'")
    suspend fun deleteCorruptedModels(): Int
    
    @Query("DELETE FROM whisper_models WHERE status = 'NOT_DOWNLOADED' AND downloaded_at IS NULL")
    suspend fun deleteNotDownloadedModels(): Int
    
    @Query("UPDATE whisper_models SET download_progress = 0.0 WHERE status != 'DOWNLOADING'")
    suspend fun resetDownloadProgress(): Int
    
    @Query("SELECT * FROM whisper_models WHERE local_path IS NOT NULL AND status = 'DOWNLOADED'")
    suspend fun getModelsWithLocalFiles(): List<WhisperModelEntity>
    
    @Query("VACUUM")
    suspend fun vacuum()
    
    // Batch Operations
    
    @Transaction
    suspend fun replaceAllModels(models: List<WhisperModelEntity>) {
        deleteAllModels()
        insertModels(models)
    }
    
    @Query("DELETE FROM whisper_models")
    suspend fun deleteAllModels()
    
    @Transaction
    suspend fun updateModelsStatus(ids: List<String>, status: ModelStatus) {
        ids.forEach { id ->
            updateModelStatus(id, status)
        }
    }
}

/**
 * Data class for model size distribution statistics.
 */
data class ModelSizeDistribution(
    val size: ModelSize,
    val count: Int
)

/**
 * Data class for model status distribution statistics.
 */
data class ModelStatusDistribution(
    val status: ModelStatus,
    val count: Int
)
