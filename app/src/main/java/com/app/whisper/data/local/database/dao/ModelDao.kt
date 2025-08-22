package com.app.whisper.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.whisper.data.local.database.entity.ModelEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for model operations.
 * 
 * This interface defines all database operations for model entities,
 * including CRUD operations, status management, and usage analytics.
 */
@Dao
interface ModelDao {
    
    /**
     * Insert a new model.
     * 
     * @param model Model entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelEntity)
    
    /**
     * Insert multiple models.
     * 
     * @param models List of model entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelEntity>)
    
    /**
     * Update an existing model.
     * 
     * @param model Model entity to update
     */
    @Update
    suspend fun updateModel(model: ModelEntity)
    
    /**
     * Delete a model by ID.
     * 
     * @param modelId Model ID to delete
     */
    @Query("DELETE FROM models WHERE model_id = :modelId")
    suspend fun deleteModel(modelId: String)
    
    /**
     * Delete a model entity.
     * 
     * @param model Model entity to delete
     */
    @Delete
    suspend fun deleteModel(model: ModelEntity)
    
    /**
     * Delete all models.
     */
    @Query("DELETE FROM models")
    suspend fun deleteAllModels()
    
    /**
     * Get a model by ID.
     * 
     * @param modelId Model ID to search for
     * @return Model entity or null if not found
     */
    @Query("SELECT * FROM models WHERE model_id = :modelId")
    suspend fun getModelById(modelId: String): ModelEntity?
    
    /**
     * Get all models ordered by name.
     * 
     * @return Flow of all models
     */
    @Query("SELECT * FROM models ORDER BY name ASC")
    fun getAllModels(): Flow<List<ModelEntity>>
    
    /**
     * Get all models synchronously (for cleanup operations).
     * 
     * @return List of all models
     */
    @Query("SELECT * FROM models ORDER BY name ASC")
    suspend fun getAllModelsSync(): List<ModelEntity>
    
    /**
     * Get available (downloaded) models.
     * 
     * @return Flow of available models
     */
    @Query("SELECT * FROM models WHERE status = 'Available' ORDER BY last_used_at DESC")
    fun getAvailableModels(): Flow<List<ModelEntity>>
    
    /**
     * Get models by status.
     * 
     * @param status Model status to filter by
     * @return Flow of models with the specified status
     */
    @Query("SELECT * FROM models WHERE status = :status ORDER BY name ASC")
    fun getModelsByStatus(status: String): Flow<List<ModelEntity>>
    
    /**
     * Get downloading models.
     * 
     * @return Flow of models currently being downloaded
     */
    @Query("SELECT * FROM models WHERE status = 'Downloading' ORDER BY name ASC")
    fun getDownloadingModels(): Flow<List<ModelEntity>>
    
    /**
     * Get models with errors.
     * 
     * @return Flow of models with error status
     */
    @Query("SELECT * FROM models WHERE status IN ('Error', 'Corrupted') ORDER BY name ASC")
    fun getErrorModels(): Flow<List<ModelEntity>>
    
    /**
     * Get frequently used models.
     * 
     * @param minUsageCount Minimum usage count threshold
     * @return Flow of frequently used models
     */
    @Query("SELECT * FROM models WHERE usage_count >= :minUsageCount ORDER BY usage_count DESC, last_used_at DESC")
    fun getFrequentlyUsedModels(minUsageCount: Int = 5): Flow<List<ModelEntity>>
    
    /**
     * Get recently used models.
     * 
     * @param sinceTimestamp Timestamp threshold for "recent"
     * @return Flow of recently used models
     */
    @Query("SELECT * FROM models WHERE last_used_at >= :sinceTimestamp ORDER BY last_used_at DESC")
    fun getRecentlyUsedModels(sinceTimestamp: Long): Flow<List<ModelEntity>>
    
    /**
     * Get multilingual models.
     * 
     * @return Flow of multilingual models
     */
    @Query("SELECT * FROM models WHERE is_multilingual = 1 ORDER BY name ASC")
    fun getMultilingualModels(): Flow<List<ModelEntity>>
    
    /**
     * Get models supporting a specific language.
     * 
     * @param languageCode Language code to search for
     * @return Flow of models supporting the language
     */
    @Query("SELECT * FROM models WHERE supported_languages LIKE '%' || :languageCode || '%' ORDER BY name ASC")
    fun getModelsSupportingLanguage(languageCode: String): Flow<List<ModelEntity>>
    
    /**
     * Get total count of models.
     * 
     * @return Total number of models
     */
    @Query("SELECT COUNT(*) FROM models")
    suspend fun getModelCount(): Int
    
    /**
     * Get count of available models.
     * 
     * @return Number of available models
     */
    @Query("SELECT COUNT(*) FROM models WHERE status = 'Available'")
    suspend fun getAvailableModelCount(): Int
    
    /**
     * Get total size of downloaded models.
     * 
     * @return Total size in bytes
     */
    @Query("SELECT SUM(file_size_bytes) FROM models WHERE status = 'Available'")
    suspend fun getTotalDownloadedSize(): Long?
    
    /**
     * Get most used model.
     * 
     * @return Most frequently used model or null
     */
    @Query("SELECT * FROM models WHERE usage_count > 0 ORDER BY usage_count DESC, last_used_at DESC LIMIT 1")
    suspend fun getMostUsedModel(): ModelEntity?
    
    /**
     * Get least used model.
     * 
     * @return Least used model or null
     */
    @Query("SELECT * FROM models WHERE status = 'Available' ORDER BY usage_count ASC, last_used_at ASC LIMIT 1")
    suspend fun getLeastUsedModel(): ModelEntity?
    
    /**
     * Update model status.
     * 
     * @param modelId Model ID
     * @param status New status
     * @param localPath New local path (optional)
     */
    @Query("UPDATE models SET status = :status, local_path = :localPath, updated_at = :updatedAt WHERE model_id = :modelId")
    suspend fun updateModelStatus(modelId: String, status: String, localPath: String? = null, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * Update model usage statistics.
     * 
     * @param modelId Model ID
     * @param processingTimeMs Processing time to add
     * @param confidence Confidence score for this usage
     */
    @Query("""
        UPDATE models SET 
            usage_count = usage_count + 1,
            total_processing_time_ms = total_processing_time_ms + :processingTimeMs,
            average_confidence = CASE 
                WHEN average_confidence IS NULL THEN :confidence
                ELSE (average_confidence * usage_count + :confidence) / (usage_count + 1)
            END,
            last_used_at = :lastUsedAt,
            updated_at = :updatedAt
        WHERE model_id = :modelId
    """)
    suspend fun updateModelUsage(
        modelId: String, 
        processingTimeMs: Long, 
        confidence: Float,
        lastUsedAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Update model download information.
     * 
     * @param modelId Model ID
     * @param localPath Local file path
     * @param fileSizeBytes Actual file size
     * @param checksum File checksum (optional)
     */
    @Query("""
        UPDATE models SET 
            status = 'Available',
            local_path = :localPath,
            file_size_bytes = :fileSizeBytes,
            checksum = :checksum,
            downloaded_at = :downloadedAt,
            updated_at = :updatedAt
        WHERE model_id = :modelId
    """)
    suspend fun updateModelDownload(
        modelId: String,
        localPath: String,
        fileSizeBytes: Long,
        checksum: String? = null,
        downloadedAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Reset model usage statistics.
     * 
     * @param modelId Model ID
     */
    @Query("""
        UPDATE models SET 
            usage_count = 0,
            total_processing_time_ms = 0,
            average_confidence = NULL,
            last_used_at = NULL,
            updated_at = :updatedAt
        WHERE model_id = :modelId
    """)
    suspend fun resetModelUsage(modelId: String, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * Get models ordered by last used (for cleanup).
     * 
     * @return List of models ordered by last used (oldest first)
     */
    @Query("SELECT * FROM models WHERE status = 'Available' ORDER BY last_used_at ASC NULLS FIRST")
    suspend fun getModelsOrderedByLastUsed(): List<ModelEntity>
    
    /**
     * Get models larger than specified size.
     * 
     * @param minSizeBytes Minimum size in bytes
     * @return Flow of large models
     */
    @Query("SELECT * FROM models WHERE file_size_bytes >= :minSizeBytes ORDER BY file_size_bytes DESC")
    fun getLargeModels(minSizeBytes: Long): Flow<List<ModelEntity>>
    
    /**
     * Get model usage summary.
     * 
     * @return List of model usage statistics
     */
    @Query("""
        SELECT 
            model_id,
            name,
            usage_count,
            total_processing_time_ms,
            average_confidence,
            last_used_at
        FROM models 
        WHERE usage_count > 0 
        ORDER BY usage_count DESC
    """)
    suspend fun getModelUsageSummary(): List<ModelUsageSummary>
    
    /**
     * Delete models not used for a specified period.
     * 
     * @param olderThan Timestamp threshold
     */
    @Query("DELETE FROM models WHERE last_used_at < :olderThan OR (last_used_at IS NULL AND downloaded_at < :olderThan)")
    suspend fun deleteUnusedModels(olderThan: Long)
}

/**
 * Model usage summary data class.
 */
data class ModelUsageSummary(
    val modelId: String,
    val name: String,
    val usageCount: Int,
    val totalProcessingTimeMs: Long,
    val averageConfidence: Float?,
    val lastUsedAt: Long?
)
