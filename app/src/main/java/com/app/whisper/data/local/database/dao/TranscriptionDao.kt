package com.app.whisper.data.local.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.app.whisper.data.local.database.entity.TranscriptionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for transcription operations.
 *
 * This interface defines all database operations for transcription entities,
 * including CRUD operations, search, and analytics queries.
 */
@Dao
interface TranscriptionDao {

    /**
     * Insert a new transcription.
     *
     * @param transcription Transcription entity to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscription(transcription: TranscriptionEntity)

    /**
     * Insert multiple transcriptions.
     *
     * @param transcriptions List of transcription entities to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscriptions(transcriptions: List<TranscriptionEntity>)

    /**
     * Update an existing transcription.
     *
     * @param transcription Transcription entity to update
     */
    @Update
    suspend fun updateTranscription(transcription: TranscriptionEntity)

    /**
     * Delete a transcription by session ID.
     *
     * @param sessionId Session ID of the transcription to delete
     */
    @Query("DELETE FROM transcriptions WHERE session_id = :sessionId")
    suspend fun deleteTranscription(sessionId: String)

    /**
     * Delete a transcription entity.
     *
     * @param transcription Transcription entity to delete
     */
    @Delete
    suspend fun deleteTranscription(transcription: TranscriptionEntity)

    /**
     * Delete all transcriptions.
     */
    @Query("DELETE FROM transcriptions")
    suspend fun deleteAllTranscriptions()

    /**
     * Get a transcription by session ID.
     *
     * @param sessionId Session ID to search for
     * @return Transcription entity or null if not found
     */
    @Query("SELECT * FROM transcriptions WHERE session_id = :sessionId")
    suspend fun getTranscriptionById(sessionId: String): TranscriptionEntity?

    /**
     * Get all transcriptions ordered by creation date (newest first).
     *
     * @return Flow of all transcriptions
     */
    @Query("SELECT * FROM transcriptions ORDER BY created_at DESC")
    fun getAllTranscriptions(): Flow<List<TranscriptionEntity>>

    /**
     * Get recent transcriptions with limit.
     *
     * @param limit Maximum number of transcriptions to return
     * @return Flow of recent transcriptions
     */
    @Query("SELECT * FROM transcriptions ORDER BY created_at DESC LIMIT :limit")
    fun getRecentTranscriptions(limit: Int): Flow<List<TranscriptionEntity>>

    /**
     * Get favorite transcriptions.
     *
     * @return Flow of favorite transcriptions
     */
    @Query("SELECT * FROM transcriptions WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun getFavoriteTranscriptions(): Flow<List<TranscriptionEntity>>

    /**
     * Get transcriptions by language.
     *
     * @param language Language code to filter by
     * @return Flow of transcriptions in the specified language
     */
    @Query("SELECT * FROM transcriptions WHERE language = :language ORDER BY created_at DESC")
    fun getTranscriptionsByLanguage(language: String): Flow<List<TranscriptionEntity>>

    /**
     * Get transcriptions by model ID.
     *
     * @param modelId Model ID to filter by
     * @return Flow of transcriptions using the specified model
     */
    @Query("SELECT * FROM transcriptions WHERE model_id = :modelId ORDER BY created_at DESC")
    fun getTranscriptionsByModel(modelId: String): Flow<List<TranscriptionEntity>>

    /**
     * Search transcriptions by text content.
     *
     * @param query Search query
     * @return Flow of matching transcriptions
     */
    @Query("SELECT * FROM transcriptions WHERE text LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun searchTranscriptions(query: String): Flow<List<TranscriptionEntity>>

    /**
     * Get transcriptions with high confidence (>= 80%).
     *
     * @return Flow of high-confidence transcriptions
     */
    @Query("SELECT * FROM transcriptions WHERE confidence >= 0.8 ORDER BY confidence DESC, created_at DESC")
    fun getHighConfidenceTranscriptions(): Flow<List<TranscriptionEntity>>

    /**
     * Get transcriptions within a date range.
     *
     * @param startTime Start timestamp (inclusive)
     * @param endTime End timestamp (inclusive)
     * @return Flow of transcriptions within the date range
     */
    @Query("SELECT * FROM transcriptions WHERE created_at BETWEEN :startTime AND :endTime ORDER BY created_at DESC")
    fun getTranscriptionsInDateRange(startTime: Long, endTime: Long): Flow<List<TranscriptionEntity>>

    /**
     * Get transcriptions longer than specified duration.
     *
     * @param minDurationMs Minimum duration in milliseconds
     * @return Flow of long transcriptions
     */
    @Query("SELECT * FROM transcriptions WHERE audio_length_ms >= :minDurationMs ORDER BY audio_length_ms DESC")
    fun getLongTranscriptions(minDurationMs: Long): Flow<List<TranscriptionEntity>>

    /**
     * Get total count of transcriptions.
     *
     * @return Total number of transcriptions
     */
    @Query("SELECT COUNT(*) FROM transcriptions")
    suspend fun getTranscriptionCount(): Int

    /**
     * Get total audio duration processed.
     *
     * @return Total audio duration in milliseconds
     */
    @Query("SELECT SUM(audio_length_ms) FROM transcriptions")
    suspend fun getTotalAudioDuration(): Long?

    /**
     * Get average confidence score.
     *
     * @return Average confidence score
     */
    @Query("SELECT AVG(confidence) FROM transcriptions")
    suspend fun getAverageConfidence(): Float?

    /**
     * Get transcription statistics by language.
     *
     * @return List of language statistics
     */
    @Query("SELECT language, COUNT(*) as count, AVG(confidence) as avg_confidence FROM transcriptions GROUP BY language ORDER BY count DESC")
    suspend fun getLanguageStatistics(): List<LanguageStats>

    /**
     * Get transcription statistics by model.
     *
     * @return List of model statistics
     */
    @Query("SELECT model_id, COUNT(*) as count, AVG(confidence) as avg_confidence, AVG(processing_time_ms) as avg_processing_time FROM transcriptions GROUP BY model_id ORDER BY count DESC")
    suspend fun getModelStatistics(): List<ModelStats>

    /**
     * Get most recent transcription.
     *
     * @return Most recent transcription or null
     */
    @Query("SELECT * FROM transcriptions ORDER BY created_at DESC LIMIT 1")
    suspend fun getMostRecentTranscription(): TranscriptionEntity?

    /**
     * Update favorite status.
     *
     * @param sessionId Session ID of the transcription
     * @param isFavorite New favorite status
     */
    @Query("UPDATE transcriptions SET is_favorite = :isFavorite, updated_at = :updatedAt WHERE session_id = :sessionId")
    suspend fun updateFavoriteStatus(sessionId: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    /**
     * Update notes.
     *
     * @param sessionId Session ID of the transcription
     * @param notes New notes text
     */
    @Query("UPDATE transcriptions SET notes = :notes, updated_at = :updatedAt WHERE session_id = :sessionId")
    suspend fun updateNotes(sessionId: String, notes: String?, updatedAt: Long = System.currentTimeMillis())

    /**
     * Update tags.
     *
     * @param sessionId Session ID of the transcription
     * @param tags New tags string
     */
    @Query("UPDATE transcriptions SET tags = :tags, updated_at = :updatedAt WHERE session_id = :sessionId")
    suspend fun updateTags(sessionId: String, tags: String?, updatedAt: Long = System.currentTimeMillis())

    /**
     * Delete old transcriptions beyond a certain count.
     *
     * @param keepCount Number of recent transcriptions to keep
     */
    @Query("DELETE FROM transcriptions WHERE session_id NOT IN (SELECT session_id FROM transcriptions ORDER BY created_at DESC LIMIT :keepCount)")
    suspend fun deleteOldTranscriptions(keepCount: Int)

    /**
     * Delete transcriptions older than specified timestamp.
     *
     * @param olderThan Timestamp threshold
     */
    @Query("DELETE FROM transcriptions WHERE created_at < :olderThan")
    suspend fun deleteTranscriptionsOlderThan(olderThan: Long)
}

/**
 * Language statistics data class.
 */
data class LanguageStats(
    val language: String,
    val count: Int,
    @ColumnInfo(name = "avg_confidence") val avgConfidence: Float
)

/**
 * Model statistics data class.
 */
data class ModelStats(
    @ColumnInfo(name = "model_id") val modelId: String,
    val count: Int,
    @ColumnInfo(name = "avg_confidence") val avgConfidence: Float,
    @ColumnInfo(name = "avg_processing_time") val avgProcessingTime: Float
)
