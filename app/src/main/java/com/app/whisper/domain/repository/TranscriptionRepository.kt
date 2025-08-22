package com.app.whisper.domain.repository

import com.app.whisper.domain.entity.TranscriptionResult
import com.app.whisper.domain.entity.TranscriptionSession
import com.app.whisper.domain.entity.ProcessingParameters
import com.app.whisper.data.model.AudioData
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for transcription operations.
 * 
 * This interface defines the contract for transcription-related data operations,
 * following the Repository pattern to abstract data sources and provide a clean
 * API for the domain layer.
 */
interface TranscriptionRepository {
    
    /**
     * Transcribe audio data using the specified model.
     * 
     * @param audioData Audio data to transcribe
     * @param modelName Name of the Whisper model to use
     * @param parameters Processing parameters
     * @return Result containing transcription result or error
     */
    suspend fun transcribeAudio(
        audioData: AudioData,
        modelName: String,
        parameters: ProcessingParameters = ProcessingParameters()
    ): Result<TranscriptionResult>
    
    /**
     * Transcribe audio from file path.
     * 
     * @param audioFilePath Path to audio file
     * @param modelName Name of the Whisper model to use
     * @param parameters Processing parameters
     * @return Result containing transcription result or error
     */
    suspend fun transcribeFile(
        audioFilePath: String,
        modelName: String,
        parameters: ProcessingParameters = ProcessingParameters()
    ): Result<TranscriptionResult>
    
    /**
     * Start a new transcription session.
     * 
     * @param session Transcription session to start
     * @return Result containing updated session or error
     */
    suspend fun startTranscriptionSession(session: TranscriptionSession): Result<TranscriptionSession>
    
    /**
     * Get transcription session by ID.
     * 
     * @param sessionId Session identifier
     * @return Session if found, null otherwise
     */
    suspend fun getSession(sessionId: String): TranscriptionSession?
    
    /**
     * Get all transcription sessions.
     * 
     * @param limit Maximum number of sessions to return
     * @param offset Offset for pagination
     * @return List of transcription sessions
     */
    suspend fun getAllSessions(limit: Int = 50, offset: Int = 0): List<TranscriptionSession>
    
    /**
     * Get sessions with specific status.
     * 
     * @param status Session status to filter by
     * @param limit Maximum number of sessions to return
     * @return List of sessions with the specified status
     */
    suspend fun getSessionsByStatus(
        status: com.app.whisper.domain.entity.SessionStatus,
        limit: Int = 50
    ): List<TranscriptionSession>
    
    /**
     * Get sessions that contain specific tags.
     * 
     * @param tags Tags to search for
     * @param matchAll Whether all tags must be present (true) or any tag (false)
     * @param limit Maximum number of sessions to return
     * @return List of sessions matching the tag criteria
     */
    suspend fun getSessionsByTags(
        tags: List<String>,
        matchAll: Boolean = false,
        limit: Int = 50
    ): List<TranscriptionSession>
    
    /**
     * Search sessions by text content in transcription results.
     * 
     * @param query Search query
     * @param limit Maximum number of sessions to return
     * @return List of sessions containing the search query
     */
    suspend fun searchSessions(query: String, limit: Int = 50): List<TranscriptionSession>
    
    /**
     * Update an existing transcription session.
     * 
     * @param session Updated session
     * @return Result indicating success or failure
     */
    suspend fun updateSession(session: TranscriptionSession): Result<Unit>
    
    /**
     * Delete a transcription session.
     * 
     * @param sessionId Session ID to delete
     * @param deleteAudioFile Whether to also delete the associated audio file
     * @return Result indicating success or failure
     */
    suspend fun deleteSession(sessionId: String, deleteAudioFile: Boolean = false): Result<Unit>
    
    /**
     * Delete multiple sessions.
     * 
     * @param sessionIds List of session IDs to delete
     * @param deleteAudioFiles Whether to also delete associated audio files
     * @return Result indicating success or failure
     */
    suspend fun deleteSessions(
        sessionIds: List<String>,
        deleteAudioFiles: Boolean = false
    ): Result<Unit>
    
    /**
     * Get transcription statistics.
     * 
     * @return Map containing various statistics
     */
    suspend fun getStatistics(): Map<String, Any>
    
    /**
     * Export transcription session data.
     * 
     * @param sessionId Session ID to export
     * @param format Export format (json, txt, srt, etc.)
     * @return Result containing exported data as string or error
     */
    suspend fun exportSession(sessionId: String, format: String = "json"): Result<String>
    
    /**
     * Import transcription session data.
     * 
     * @param data Serialized session data
     * @param format Import format
     * @return Result containing imported session or error
     */
    suspend fun importSession(data: String, format: String = "json"): Result<TranscriptionSession>
    
    /**
     * Observe transcription sessions as a Flow.
     * 
     * @return Flow of session lists that updates when sessions change
     */
    fun observeSessions(): Flow<List<TranscriptionSession>>
    
    /**
     * Observe a specific session as a Flow.
     * 
     * @param sessionId Session ID to observe
     * @return Flow of session updates
     */
    fun observeSession(sessionId: String): Flow<TranscriptionSession?>
    
    /**
     * Get recent transcription results for caching/quick access.
     * 
     * @param limit Maximum number of results to return
     * @return List of recent transcription results
     */
    suspend fun getRecentResults(limit: Int = 10): List<TranscriptionResult>
    
    /**
     * Cache a transcription result for quick access.
     * 
     * @param result Transcription result to cache
     * @return Result indicating success or failure
     */
    suspend fun cacheResult(result: TranscriptionResult): Result<Unit>
    
    /**
     * Clear cached transcription results.
     * 
     * @param olderThanMs Clear results older than this timestamp
     * @return Result indicating success or failure
     */
    suspend fun clearCache(olderThanMs: Long = 0L): Result<Unit>
    
    /**
     * Get transcription history for analytics.
     * 
     * @param fromDate Start date for history (timestamp)
     * @param toDate End date for history (timestamp)
     * @return List of transcription sessions in the date range
     */
    suspend fun getHistory(fromDate: Long, toDate: Long): List<TranscriptionSession>
    
    /**
     * Get usage statistics for a specific time period.
     * 
     * @param fromDate Start date (timestamp)
     * @param toDate End date (timestamp)
     * @return Map containing usage statistics
     */
    suspend fun getUsageStats(fromDate: Long, toDate: Long): Map<String, Any>
    
    /**
     * Backup transcription data.
     * 
     * @param includeAudioFiles Whether to include audio files in backup
     * @return Result containing backup data or error
     */
    suspend fun backup(includeAudioFiles: Boolean = false): Result<String>
    
    /**
     * Restore transcription data from backup.
     * 
     * @param backupData Backup data to restore
     * @param overwriteExisting Whether to overwrite existing sessions
     * @return Result indicating success or failure
     */
    suspend fun restore(backupData: String, overwriteExisting: Boolean = false): Result<Unit>
}
