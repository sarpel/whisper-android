package com.app.whisper.di

import com.app.whisper.domain.repository.ModelRepository
import com.app.whisper.domain.repository.TranscriptionRepository
import com.app.whisper.domain.usecase.DownloadModelUseCase
import com.app.whisper.domain.usecase.TranscribeAudioUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing use case dependencies.
 * 
 * This module creates and provides use case instances that encapsulate
 * business logic and coordinate between different repositories and services.
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    
    /**
     * Provides TranscribeAudioUseCase for audio transcription operations.
     * 
     * This use case handles:
     * - Audio transcription workflow
     * - Model validation and loading
     * - Progress tracking and reporting
     * - Error handling and recovery
     */
    @Provides
    @Singleton
    fun provideTranscribeAudioUseCase(
        transcriptionRepository: TranscriptionRepository,
        modelRepository: ModelRepository
    ): TranscribeAudioUseCase {
        return TranscribeAudioUseCase(
            transcriptionRepository = transcriptionRepository,
            modelRepository = modelRepository
        )
    }
    
    /**
     * Provides DownloadModelUseCase for model management operations.
     * 
     * This use case handles:
     * - Model download workflow
     * - Download progress tracking
     * - Model validation and verification
     * - Storage management
     */
    @Provides
    @Singleton
    fun provideDownloadModelUseCase(
        modelRepository: ModelRepository
    ): DownloadModelUseCase {
        return DownloadModelUseCase(
            modelRepository = modelRepository
        )
    }
}
