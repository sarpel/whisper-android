package com.app.whisper.data.repository

import com.app.whisper.data.model.AudioData
import com.app.whisper.domain.entity.ProcessingParameters
import com.app.whisper.domain.entity.TranscriptionResult
import com.app.whisper.domain.entity.TranscriptionSession
import com.app.whisper.domain.entity.SessionStatus
import com.app.whisper.domain.repository.TranscriptionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of TranscriptionRepository for build testing.
 * This will be replaced with real implementation later.
 */
@Singleton
class FakeTranscriptionRepository @Inject constructor() : TranscriptionRepository {
    
    private val sessions = mutableMapOf<String, TranscriptionSession>()
    private val results = mutableListOf<TranscriptionResult>()
    
    override suspend fun transcribeAudio(
        audioData: AudioData,
        modelName: String,
        parameters: ProcessingParameters
    ): Result<TranscriptionResult> {
        // Simulate processing delay
        delay(2000)
        
        val result = TranscriptionResult(
            id = "fake_result_${System.currentTimeMillis()}",
            text = "This is a fake transcription result for testing purposes.",
            language = parameters.language.takeIf { it != "auto" } ?: "en",
            confidence = 0.95f,
            processingTimeMs = 2000L,
            audioDurationMs = audioData.getDurationMs()
        )
        
        results.add(result)
        return Result.success(result)
    }
    
    override suspend fun transcribeFile(
        audioFilePath: String,
        modelName: String,
        parameters: ProcessingParameters
    ): Result<TranscriptionResult> {
        // Simulate processing delay
        delay(3000)
        
        val result = TranscriptionResult(
            id = "fake_file_result_${System.currentTimeMillis()}",
            text = "This is a fake transcription result from file: ${audioFilePath.substringAfterLast('/')}",
            language = parameters.language.takeIf { it != "auto" } ?: "en",
            confidence = 0.92f,
            processingTimeMs = 3000L,
            audioDurationMs = 10000L // Fake 10 seconds
        )
        
        results.add(result)
        return Result.success(result)
    }
    
    override suspend fun startTranscriptionSession(session: TranscriptionSession): Result<TranscriptionSession> {
        val updatedSession = session.withStatus(SessionStatus.PROCESSING)
        sessions[session.id] = updatedSession
        return Result.success(updatedSession)
    }
    
    override suspend fun getSession(sessionId: String): TranscriptionSession? {
        return sessions[sessionId]
    }
    
    override suspend fun getAllSessions(limit: Int, offset: Int): List<TranscriptionSession> {
        return sessions.values.take(limit)
    }
    
    override suspend fun getSessionsByStatus(
        status: SessionStatus,
        limit: Int
    ): List<TranscriptionSession> {
        return sessions.values.filter { it.status == status }.take(limit)
    }
    
    override suspend fun getSessionsByTags(
        tags: List<String>,
        matchAll: Boolean,
        limit: Int
    ): List<TranscriptionSession> {
        return sessions.values.filter { session ->
            if (matchAll) {
                tags.all { tag -> session.hasTag(tag) }
            } else {
                tags.any { tag -> session.hasTag(tag) }
            }
        }.take(limit)
    }
    
    override suspend fun searchSessions(query: String, limit: Int): List<TranscriptionSession> {
        return sessions.values.filter { 
            it.name.contains(query, ignoreCase = true) ||
            it.transcriptionResult?.text?.contains(query, ignoreCase = true) == true
        }.take(limit)
    }
    
    override suspend fun updateSession(session: TranscriptionSession): Result<Unit> {
        sessions[session.id] = session
        return Result.success(Unit)
    }
    
    override suspend fun deleteSession(sessionId: String, deleteAudioFile: Boolean): Result<Unit> {
        sessions.remove(sessionId)
        return Result.success(Unit)
    }
    
    override suspend fun deleteSessions(sessionIds: List<String>, deleteAudioFiles: Boolean): Result<Unit> {
        sessionIds.forEach { sessions.remove(it) }
        return Result.success(Unit)
    }
    
    override suspend fun getStatistics(): Map<String, Any> {
        return mapOf(
            "totalSessions" to sessions.size,
            "totalResults" to results.size,
            "averageConfidence" to results.map { it.confidence }.average()
        )
    }
    
    override suspend fun exportSession(sessionId: String, format: String): Result<String> {
        val session = sessions[sessionId] ?: return Result.failure(Exception("Session not found"))
        return Result.success("Fake export data for session: ${session.name}")
    }
    
    override suspend fun importSession(data: String, format: String): Result<TranscriptionSession> {
        val session = TranscriptionSession.create(
            name = "Imported Session",
            audioFilePath = "/fake/path",
            audioMetadata = com.app.whisper.domain.entity.AudioMetadata(
                fileName = "imported.wav",
                fileSizeBytes = 1024,
                durationMs = 5000,
                sampleRate = 16000,
                channels = 1,
                bitRate = 256,
                format = "wav"
            ),
            modelName = "fake-model"
        )
        sessions[session.id] = session
        return Result.success(session)
    }
    
    override fun observeSessions(): Flow<List<TranscriptionSession>> = flow {
        emit(sessions.values.toList())
    }
    
    override fun observeSession(sessionId: String): Flow<TranscriptionSession?> = flow {
        emit(sessions[sessionId])
    }
    
    override suspend fun getRecentResults(limit: Int): List<TranscriptionResult> {
        return results.takeLast(limit)
    }
    
    override suspend fun cacheResult(result: TranscriptionResult): Result<Unit> {
        results.add(result)
        return Result.success(Unit)
    }
    
    override suspend fun clearCache(olderThanMs: Long): Result<Unit> {
        results.clear()
        return Result.success(Unit)
    }
    
    override suspend fun getHistory(fromDate: Long, toDate: Long): List<TranscriptionSession> {
        return sessions.values.filter { it.createdAt in fromDate..toDate }
    }
    
    override suspend fun getUsageStats(fromDate: Long, toDate: Long): Map<String, Any> {
        return mapOf(
            "sessionsInPeriod" to sessions.values.count { it.createdAt in fromDate..toDate },
            "totalProcessingTime" to results.sumOf { it.processingTimeMs }
        )
    }
    
    override suspend fun backup(includeAudioFiles: Boolean): Result<String> {
        return Result.success("Fake backup data")
    }
    
    override suspend fun restore(backupData: String, overwriteExisting: Boolean): Result<Unit> {
        return Result.success(Unit)
    }
}
