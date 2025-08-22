package com.app.whisper.domain.usecase

import com.app.whisper.data.model.AudioData
import com.app.whisper.domain.entity.ProcessingParameters
import com.app.whisper.domain.entity.TranscriptionResult
import com.app.whisper.domain.entity.TranscriptionSession
import com.app.whisper.domain.repository.ModelRepository
import com.app.whisper.domain.repository.TranscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for transcribing audio data.
 * 
 * This use case orchestrates the transcription process, including model validation,
 * session management, and result processing. It encapsulates the business logic
 * for audio transcription operations.
 */
@Singleton
class TranscribeAudioUseCase @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository,
    private val modelRepository: ModelRepository
) {
    
    /**
     * Transcribe audio data with the current model.
     * 
     * @param audioData Audio data to transcribe
     * @param parameters Processing parameters
     * @return Flow of transcription progress and result
     */
    suspend fun execute(
        audioData: AudioData,
        parameters: ProcessingParameters = ProcessingParameters()
    ): Flow<TranscriptionProgress> = flow {
        try {
            emit(TranscriptionProgress.Started)
            
            // Validate audio data
            val audioValidation = validateAudioData(audioData)
            if (audioValidation.isFailure) {
                emit(TranscriptionProgress.Failed(
                    audioValidation.exceptionOrNull() ?: Exception("Audio validation failed")
                ))
                return@flow
            }
            
            // Get current model
            val currentModel = modelRepository.getCurrentModel()
            if (currentModel == null) {
                emit(TranscriptionProgress.Failed(Exception("No model selected")))
                return@flow
            }
            
            if (!currentModel.isAvailable()) {
                emit(TranscriptionProgress.Failed(Exception("Selected model is not available")))
                return@flow
            }
            
            emit(TranscriptionProgress.ModelLoaded(currentModel.displayName))
            
            // Validate model compatibility with parameters
            val compatibilityCheck = validateModelCompatibility(currentModel, parameters)
            if (compatibilityCheck.isFailure) {
                emit(TranscriptionProgress.Failed(
                    compatibilityCheck.exceptionOrNull() ?: Exception("Model compatibility check failed")
                ))
                return@flow
            }
            
            emit(TranscriptionProgress.Processing)
            
            // Perform transcription
            val transcriptionResult = transcriptionRepository.transcribeAudio(
                audioData = audioData,
                modelName = currentModel.name,
                parameters = parameters
            )
            
            if (transcriptionResult.isSuccess) {
                val result = transcriptionResult.getOrThrow()
                
                // Update model usage
                modelRepository.updateModel(currentModel.withLastUsed())
                
                // Cache result if it's high quality
                if (result.isHighQuality()) {
                    transcriptionRepository.cacheResult(result)
                }
                
                emit(TranscriptionProgress.Completed(result))
            } else {
                emit(TranscriptionProgress.Failed(
                    transcriptionResult.exceptionOrNull() ?: Exception("Transcription failed")
                ))
            }
            
        } catch (e: Exception) {
            emit(TranscriptionProgress.Failed(e))
        }
    }
    
    /**
     * Transcribe audio from file with session management.
     * 
     * @param audioFilePath Path to audio file
     * @param sessionName Name for the transcription session
     * @param parameters Processing parameters
     * @return Flow of transcription progress and result
     */
    suspend fun executeWithSession(
        audioFilePath: String,
        sessionName: String,
        parameters: ProcessingParameters = ProcessingParameters()
    ): Flow<TranscriptionProgress> = flow {
        try {
            emit(TranscriptionProgress.Started)
            
            // Get current model
            val currentModel = modelRepository.getCurrentModel()
            if (currentModel == null) {
                emit(TranscriptionProgress.Failed(Exception("No model selected")))
                return@flow
            }
            
            // Create audio metadata (simplified - in real implementation, extract from file)
            val audioMetadata = com.app.whisper.domain.entity.AudioMetadata(
                fileName = audioFilePath.substringAfterLast('/'),
                fileSizeBytes = 0L, // Would be extracted from file
                durationMs = 0L,    // Would be extracted from file
                sampleRate = 16000,
                channels = 1,
                bitRate = 256,
                format = "wav"
            )
            
            // Create transcription session
            val session = TranscriptionSession.create(
                name = sessionName,
                audioFilePath = audioFilePath,
                audioMetadata = audioMetadata,
                modelName = currentModel.name,
                parameters = parameters
            )
            
            emit(TranscriptionProgress.SessionCreated(session.id))
            
            // Start session
            val sessionResult = transcriptionRepository.startTranscriptionSession(session)
            if (sessionResult.isFailure) {
                emit(TranscriptionProgress.Failed(
                    sessionResult.exceptionOrNull() ?: Exception("Failed to start session")
                ))
                return@flow
            }
            
            emit(TranscriptionProgress.Processing)
            
            // Perform transcription
            val transcriptionResult = transcriptionRepository.transcribeFile(
                audioFilePath = audioFilePath,
                modelName = currentModel.name,
                parameters = parameters
            )
            
            if (transcriptionResult.isSuccess) {
                val result = transcriptionResult.getOrThrow()
                
                // Update session with result
                val updatedSession = session.withResult(result)
                transcriptionRepository.updateSession(updatedSession)
                
                // Update model usage
                modelRepository.updateModel(currentModel.withLastUsed())
                
                emit(TranscriptionProgress.Completed(result, updatedSession))
            } else {
                // Update session with error
                val failedSession = session.withStatus(
                    com.app.whisper.domain.entity.SessionStatus.FAILED,
                    transcriptionResult.exceptionOrNull()?.message
                )
                transcriptionRepository.updateSession(failedSession)
                
                emit(TranscriptionProgress.Failed(
                    transcriptionResult.exceptionOrNull() ?: Exception("Transcription failed")
                ))
            }
            
        } catch (e: Exception) {
            emit(TranscriptionProgress.Failed(e))
        }
    }
    
    /**
     * Get recent transcription results from cache.
     * 
     * @param limit Maximum number of results to return
     * @return List of recent transcription results
     */
    suspend fun getRecentResults(limit: Int = 10): Result<List<TranscriptionResult>> {
        return try {
            val results = transcriptionRepository.getRecentResults(limit)
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validate audio data for transcription.
     * 
     * @param audioData Audio data to validate
     * @return Result indicating validation success or failure
     */
    private fun validateAudioData(audioData: AudioData): Result<Unit> {
        return when {
            !audioData.isValid() -> 
                Result.failure(IllegalArgumentException("Invalid audio data"))
            
            audioData.getDurationMs() < 100 -> 
                Result.failure(IllegalArgumentException("Audio too short (minimum 100ms)"))
            
            audioData.getDurationMs() > 30 * 60 * 1000 -> 
                Result.failure(IllegalArgumentException("Audio too long (maximum 30 minutes)"))
            
            audioData.sampleRate != 16000 -> 
                Result.failure(IllegalArgumentException("Audio must be 16kHz for optimal results"))
            
            audioData.channelCount != 1 -> 
                Result.failure(IllegalArgumentException("Audio must be mono for optimal results"))
            
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate model compatibility with processing parameters.
     * 
     * @param model Whisper model to validate
     * @param parameters Processing parameters
     * @return Result indicating compatibility
     */
    private fun validateModelCompatibility(
        model: com.app.whisper.domain.entity.WhisperModel,
        parameters: ProcessingParameters
    ): Result<Unit> {
        return when {
            !model.isAvailable() -> 
                Result.failure(IllegalStateException("Model is not available"))
            
            parameters.language != "auto" && !model.supportsLanguage(parameters.language) -> 
                Result.failure(IllegalArgumentException(
                    "Model ${model.name} does not support language: ${parameters.language}"
                ))
            
            parameters.translate && !model.isMultilingual -> 
                Result.failure(IllegalArgumentException(
                    "Translation requires a multilingual model"
                ))
            
            else -> Result.success(Unit)
        }
    }
}

/**
 * Sealed class representing transcription progress states.
 */
sealed class TranscriptionProgress {
    object Started : TranscriptionProgress()
    data class ModelLoaded(val modelName: String) : TranscriptionProgress()
    data class SessionCreated(val sessionId: String) : TranscriptionProgress()
    object Processing : TranscriptionProgress()
    data class Completed(
        val result: TranscriptionResult,
        val session: TranscriptionSession? = null
    ) : TranscriptionProgress()
    data class Failed(val error: Throwable) : TranscriptionProgress()
    
    /**
     * Check if transcription is in progress.
     * 
     * @return true if transcription is active
     */
    fun isInProgress(): Boolean = this is Started || this is ModelLoaded || 
                                 this is SessionCreated || this is Processing
    
    /**
     * Check if transcription completed successfully.
     * 
     * @return true if transcription completed
     */
    fun isCompleted(): Boolean = this is Completed
    
    /**
     * Check if transcription failed.
     * 
     * @return true if transcription failed
     */
    fun isFailed(): Boolean = this is Failed
    
    /**
     * Get a human-readable description of the progress.
     * 
     * @return Progress description
     */
    fun getDescription(): String = when (this) {
        is Started -> "Starting transcription..."
        is ModelLoaded -> "Model loaded: $modelName"
        is SessionCreated -> "Session created: $sessionId"
        is Processing -> "Processing audio..."
        is Completed -> "Transcription completed"
        is Failed -> "Transcription failed: ${error.message}"
    }
}
