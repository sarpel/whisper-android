package com.app.whisper.di

import com.app.whisper.data.repository.AudioRecorderRepositoryImpl
import com.app.whisper.data.repository.ModelRepositoryImpl
import com.app.whisper.data.repository.TranscriptionRepositoryImpl
import com.app.whisper.domain.repository.AudioRecorderRepository
import com.app.whisper.domain.repository.ModelRepository
import com.app.whisper.domain.repository.TranscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to their implementations.
 * 
 * This module provides the dependency injection bindings for all repository
 * interfaces used throughout the application, following the repository pattern
 * for clean architecture separation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Binds TranscriptionRepository interface to its implementation.
     * 
     * TranscriptionRepositoryImpl handles:
     * - Audio transcription operations
     * - Session management
     * - Result caching and storage
     * - History and statistics
     */
    @Binds
    @Singleton
    abstract fun bindTranscriptionRepository(
        transcriptionRepositoryImpl: TranscriptionRepositoryImpl
    ): TranscriptionRepository
    
    /**
     * Binds ModelRepository interface to its implementation.
     * 
     * ModelRepositoryImpl handles:
     * - Model download and management
     * - Model validation and verification
     * - Storage and cleanup operations
     * - Model metadata management
     */
    @Binds
    @Singleton
    abstract fun bindModelRepository(
        modelRepositoryImpl: ModelRepositoryImpl
    ): ModelRepository
    
    /**
     * Binds AudioRecorderRepository interface to its implementation.
     * 
     * AudioRecorderRepositoryImpl handles:
     * - Audio recording operations
     * - Audio format conversion
     * - Recording state management
     * - Audio file operations
     */
    @Binds
    @Singleton
    abstract fun bindAudioRecorderRepository(
        audioRecorderRepositoryImpl: AudioRecorderRepositoryImpl
    ): AudioRecorderRepository
}
