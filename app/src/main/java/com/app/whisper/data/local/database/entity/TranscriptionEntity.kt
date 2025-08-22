package com.app.whisper.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.app.whisper.data.local.database.converter.Converters
import com.app.whisper.domain.entity.TranscriptionResult
import com.app.whisper.domain.entity.TranscriptionSegment
import com.app.whisper.domain.entity.TranscriptionMetadata

/**
 * Room entity for storing transcription results in the local database.
 * 
 * This entity represents the database table structure for transcription results,
 * with type converters to handle complex data types.
 */
@Entity(tableName = "transcriptions")
@TypeConverters(Converters::class)
data class TranscriptionEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "text")
    val text: String,
    
    @ColumnInfo(name = "language")
    val language: String,
    
    @ColumnInfo(name = "confidence")
    val confidence: Float,
    
    @ColumnInfo(name = "processing_time_ms")
    val processingTimeMs: Long,
    
    @ColumnInfo(name = "audio_duration_ms")
    val audioDurationMs: Long,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "segments")
    val segments: List<TranscriptionSegment>,
    
    @ColumnInfo(name = "metadata")
    val metadata: TranscriptionMetadata,
    
    @ColumnInfo(name = "session_id")
    val sessionId: String? = null,
    
    @ColumnInfo(name = "is_cached")
    val isCached: Boolean = false,
    
    @ColumnInfo(name = "cache_expires_at")
    val cacheExpiresAt: Long? = null
) {
    
    /**
     * Convert this entity to domain model.
     * 
     * @return TranscriptionResult domain entity
     */
    fun toDomain(): TranscriptionResult = TranscriptionResult(
        id = id,
        text = text,
        language = language,
        confidence = confidence,
        processingTimeMs = processingTimeMs,
        audioDurationMs = audioDurationMs,
        createdAt = createdAt,
        segments = segments,
        metadata = metadata
    )
    
    companion object {
        /**
         * Create entity from domain model.
         * 
         * @param domain TranscriptionResult domain entity
         * @param sessionId Associated session ID
         * @param isCached Whether this is a cached result
         * @param cacheExpiresAt Cache expiration timestamp
         * @return TranscriptionEntity
         */
        fun fromDomain(
            domain: TranscriptionResult,
            sessionId: String? = null,
            isCached: Boolean = false,
            cacheExpiresAt: Long? = null
        ): TranscriptionEntity = TranscriptionEntity(
            id = domain.id,
            text = domain.text,
            language = domain.language,
            confidence = domain.confidence,
            processingTimeMs = domain.processingTimeMs,
            audioDurationMs = domain.audioDurationMs,
            createdAt = domain.createdAt,
            segments = domain.segments,
            metadata = domain.metadata,
            sessionId = sessionId,
            isCached = isCached,
            cacheExpiresAt = cacheExpiresAt
        )
    }
}

/**
 * Room entity for storing transcription sessions in the local database.
 */
@Entity(tableName = "transcription_sessions")
@TypeConverters(Converters::class)
data class TranscriptionSessionEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String,
    
    @ColumnInfo(name = "audio_metadata")
    val audioMetadata: com.app.whisper.domain.entity.AudioMetadata,
    
    @ColumnInfo(name = "transcription_result_id")
    val transcriptionResultId: String?,
    
    @ColumnInfo(name = "model_used")
    val modelUsed: String,
    
    @ColumnInfo(name = "processing_parameters")
    val processingParameters: com.app.whisper.domain.entity.ProcessingParameters,
    
    @ColumnInfo(name = "status")
    val status: com.app.whisper.domain.entity.SessionStatus,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "started_at")
    val startedAt: Long?,
    
    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
    
    @ColumnInfo(name = "error")
    val error: String?,
    
    @ColumnInfo(name = "tags")
    val tags: List<String>,
    
    @ColumnInfo(name = "notes")
    val notes: String
) {
    
    /**
     * Convert this entity to domain model.
     * 
     * @param transcriptionResult Associated transcription result
     * @return TranscriptionSession domain entity
     */
    fun toDomain(transcriptionResult: TranscriptionResult? = null): com.app.whisper.domain.entity.TranscriptionSession =
        com.app.whisper.domain.entity.TranscriptionSession(
            id = id,
            name = name,
            audioFilePath = audioFilePath,
            audioMetadata = audioMetadata,
            transcriptionResult = transcriptionResult,
            modelUsed = modelUsed,
            processingParameters = processingParameters,
            status = status,
            createdAt = createdAt,
            startedAt = startedAt,
            completedAt = completedAt,
            error = error,
            tags = tags,
            notes = notes
        )
    
    companion object {
        /**
         * Create entity from domain model.
         * 
         * @param domain TranscriptionSession domain entity
         * @return TranscriptionSessionEntity
         */
        fun fromDomain(domain: com.app.whisper.domain.entity.TranscriptionSession): TranscriptionSessionEntity =
            TranscriptionSessionEntity(
                id = domain.id,
                name = domain.name,
                audioFilePath = domain.audioFilePath,
                audioMetadata = domain.audioMetadata,
                transcriptionResultId = domain.transcriptionResult?.id,
                modelUsed = domain.modelUsed,
                processingParameters = domain.processingParameters,
                status = domain.status,
                createdAt = domain.createdAt,
                startedAt = domain.startedAt,
                completedAt = domain.completedAt,
                error = domain.error,
                tags = domain.tags,
                notes = domain.notes
            )
    }
}

/**
 * Room entity for storing Whisper model information in the local database.
 */
@Entity(tableName = "whisper_models")
@TypeConverters(Converters::class)
data class WhisperModelEntity(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "display_name")
    val displayName: String,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "size")
    val size: com.app.whisper.domain.entity.ModelSize,
    
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,
    
    @ColumnInfo(name = "download_url")
    val downloadUrl: String,
    
    @ColumnInfo(name = "version")
    val version: String,
    
    @ColumnInfo(name = "checksum")
    val checksum: String,
    
    @ColumnInfo(name = "is_multilingual")
    val isMultilingual: Boolean,
    
    @ColumnInfo(name = "supported_languages")
    val supportedLanguages: List<String>,
    
    @ColumnInfo(name = "status")
    val status: com.app.whisper.domain.entity.ModelStatus,
    
    @ColumnInfo(name = "local_path")
    val localPath: String?,
    
    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long?,
    
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long?,
    
    @ColumnInfo(name = "metadata")
    val metadata: com.app.whisper.domain.entity.ModelMetadata,
    
    @ColumnInfo(name = "is_current")
    val isCurrent: Boolean = false,
    
    @ColumnInfo(name = "download_progress")
    val downloadProgress: Float = 0.0f
) {
    
    /**
     * Convert this entity to domain model.
     * 
     * @return WhisperModel domain entity
     */
    fun toDomain(): com.app.whisper.domain.entity.WhisperModel = com.app.whisper.domain.entity.WhisperModel(
        id = id,
        name = name,
        displayName = displayName,
        description = description,
        size = size,
        fileSizeBytes = fileSizeBytes,
        downloadUrl = downloadUrl,
        version = version,
        checksum = checksum,
        isMultilingual = isMultilingual,
        supportedLanguages = supportedLanguages,
        status = status,
        localPath = localPath,
        downloadedAt = downloadedAt,
        lastUsedAt = lastUsedAt,
        metadata = metadata
    )
    
    companion object {
        /**
         * Create entity from domain model.
         * 
         * @param domain WhisperModel domain entity
         * @param isCurrent Whether this is the current active model
         * @param downloadProgress Current download progress
         * @return WhisperModelEntity
         */
        fun fromDomain(
            domain: com.app.whisper.domain.entity.WhisperModel,
            isCurrent: Boolean = false,
            downloadProgress: Float = 0.0f
        ): WhisperModelEntity = WhisperModelEntity(
            id = domain.id,
            name = domain.name,
            displayName = domain.displayName,
            description = domain.description,
            size = domain.size,
            fileSizeBytes = domain.fileSizeBytes,
            downloadUrl = domain.downloadUrl,
            version = domain.version,
            checksum = domain.checksum,
            isMultilingual = domain.isMultilingual,
            supportedLanguages = domain.supportedLanguages,
            status = domain.status,
            localPath = domain.localPath,
            downloadedAt = domain.downloadedAt,
            lastUsedAt = domain.lastUsedAt,
            metadata = domain.metadata,
            isCurrent = isCurrent,
            downloadProgress = downloadProgress
        )
    }
}
