package com.app.whisper.data.local.database.dao

import androidx.room.*
import com.app.whisper.data.local.database.entity.TranscriptionEntity
import com.app.whisper.data.local.database.entity.TranscriptionSessionEntity
import com.app.whisper.domain.entity.SessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for transcription-related database operations.
 * 
 * This DAO provides methods for CRUD operations on transcription results
 * and sessions in the local SQLite database.
 */
@Dao
interface TranscriptionDao {
    
    // Transcription Results Operations
    
    @Query("SELECT * FROM transcriptions WHERE id = :id")
    suspend fun getTranscriptionById(id: String): TranscriptionEntity?
    
    @Query("SELECT * FROM transcriptions WHERE session_id = :sessionId")
    suspend fun getTranscriptionBySessionId(sessionId: String): TranscriptionEntity?
    
    @Query("SELECT * FROM transcriptions ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllTranscriptions(limit: Int = 50, offset: Int = 0): List<TranscriptionEntity>
    
    @Query("SELECT * FROM transcriptions WHERE is_cached = 1 AND (cache_expires_at IS NULL OR cache_expires_at > :currentTime) ORDER BY created_at DESC LIMIT :limit")
    suspend fun getCachedTranscriptions(currentTime: Long, limit: Int = 10): List<TranscriptionEntity>
    
    @Query("SELECT * FROM transcriptions WHERE text LIKE '%' || :query || '%' ORDER BY created_at DESC LIMIT :limit")
    suspend fun searchTranscriptions(query: String, limit: Int = 50): List<TranscriptionEntity>
    
    @Query("SELECT * FROM transcriptions WHERE created_at BETWEEN :fromDate AND :toDate ORDER BY created_at DESC")
    suspend fun getTranscriptionsByDateRange(fromDate: Long, toDate: Long): List<TranscriptionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscription(transcription: TranscriptionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscriptions(transcriptions: List<TranscriptionEntity>)
    
    @Update
    suspend fun updateTranscription(transcription: TranscriptionEntity)
    
    @Delete
    suspend fun deleteTranscription(transcription: TranscriptionEntity)
    
    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteTranscriptionById(id: String)
    
    @Query("DELETE FROM transcriptions WHERE id IN (:ids)")
    suspend fun deleteTranscriptionsByIds(ids: List<String>)
    
    @Query("DELETE FROM transcriptions WHERE is_cached = 1 AND cache_expires_at < :currentTime")
    suspend fun deleteExpiredCache(currentTime: Long)
    
    @Query("DELETE FROM transcriptions WHERE is_cached = 1")
    suspend fun clearAllCache()
    
    // Transcription Sessions Operations
    
    @Query("SELECT * FROM transcription_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): TranscriptionSessionEntity?
    
    @Query("SELECT * FROM transcription_sessions ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getAllSessions(limit: Int = 50, offset: Int = 0): List<TranscriptionSessionEntity>
    
    @Query("SELECT * FROM transcription_sessions WHERE status = :status ORDER BY created_at DESC LIMIT :limit")
    suspend fun getSessionsByStatus(status: SessionStatus, limit: Int = 50): List<TranscriptionSessionEntity>
    
    @Query("SELECT * FROM transcription_sessions WHERE created_at BETWEEN :fromDate AND :toDate ORDER BY created_at DESC")
    suspend fun getSessionsByDateRange(fromDate: Long, toDate: Long): List<TranscriptionSessionEntity>
    
    @Query("SELECT * FROM transcription_sessions WHERE name LIKE '%' || :query || '%' ORDER BY created_at DESC LIMIT :limit")
    suspend fun searchSessionsByName(query: String, limit: Int = 50): List<TranscriptionSessionEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TranscriptionSessionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<TranscriptionSessionEntity>)
    
    @Update
    suspend fun updateSession(session: TranscriptionSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: TranscriptionSessionEntity)
    
    @Query("DELETE FROM transcription_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)
    
    @Query("DELETE FROM transcription_sessions WHERE id IN (:ids)")
    suspend fun deleteSessionsByIds(ids: List<String>)
    
    // Flow-based Observables
    
    @Query("SELECT * FROM transcriptions ORDER BY created_at DESC")
    fun observeAllTranscriptions(): Flow<List<TranscriptionEntity>>
    
    @Query("SELECT * FROM transcriptions WHERE id = :id")
    fun observeTranscription(id: String): Flow<TranscriptionEntity?>
    
    @Query("SELECT * FROM transcription_sessions ORDER BY created_at DESC")
    fun observeAllSessions(): Flow<List<TranscriptionSessionEntity>>
    
    @Query("SELECT * FROM transcription_sessions WHERE id = :id")
    fun observeSession(id: String): Flow<TranscriptionSessionEntity?>
    
    @Query("SELECT * FROM transcription_sessions WHERE status = :status ORDER BY created_at DESC")
    fun observeSessionsByStatus(status: SessionStatus): Flow<List<TranscriptionSessionEntity>>
    
    // Statistics and Analytics
    
    @Query("SELECT COUNT(*) FROM transcriptions")
    suspend fun getTranscriptionCount(): Int
    
    @Query("SELECT COUNT(*) FROM transcription_sessions")
    suspend fun getSessionCount(): Int
    
    @Query("SELECT COUNT(*) FROM transcription_sessions WHERE status = :status")
    suspend fun getSessionCountByStatus(status: SessionStatus): Int
    
    @Query("SELECT AVG(confidence) FROM transcriptions")
    suspend fun getAverageConfidence(): Float?
    
    @Query("SELECT AVG(processing_time_ms) FROM transcriptions")
    suspend fun getAverageProcessingTime(): Float?
    
    @Query("SELECT SUM(audio_duration_ms) FROM transcriptions")
    suspend fun getTotalAudioDuration(): Long?
    
    @Query("SELECT model_used, COUNT(*) as count FROM transcription_sessions GROUP BY model_used")
    suspend fun getModelUsageStats(): List<ModelUsageStat>
    
    @Query("SELECT language, COUNT(*) as count FROM transcriptions GROUP BY language")
    suspend fun getLanguageStats(): List<LanguageStat>
    
    // Complex Queries with Joins
    
    @Query("""
        SELECT ts.* FROM transcription_sessions ts
        LEFT JOIN transcriptions t ON ts.transcription_result_id = t.id
        WHERE t.text LIKE '%' || :query || '%'
        ORDER BY ts.created_at DESC
        LIMIT :limit
    """)
    suspend fun searchSessionsByTranscriptionText(query: String, limit: Int = 50): List<TranscriptionSessionEntity>
    
    @Query("""
        SELECT ts.*, t.* FROM transcription_sessions ts
        LEFT JOIN transcriptions t ON ts.transcription_result_id = t.id
        WHERE ts.id = :sessionId
    """)
    suspend fun getSessionWithTranscription(sessionId: String): SessionWithTranscription?
    
    // Maintenance Operations
    
    @Query("DELETE FROM transcriptions WHERE created_at < :olderThan")
    suspend fun deleteOldTranscriptions(olderThan: Long): Int
    
    @Query("DELETE FROM transcription_sessions WHERE created_at < :olderThan")
    suspend fun deleteOldSessions(olderThan: Long): Int
    
    @Query("VACUUM")
    suspend fun vacuum()
}

/**
 * Data class for model usage statistics.
 */
data class ModelUsageStat(
    val model_used: String,
    val count: Int
)

/**
 * Data class for language statistics.
 */
data class LanguageStat(
    val language: String,
    val count: Int
)

/**
 * Data class for session with transcription join result.
 */
data class SessionWithTranscription(
    @Embedded val session: TranscriptionSessionEntity,
    @Embedded(prefix = "transcription_") val transcription: TranscriptionEntity?
)
