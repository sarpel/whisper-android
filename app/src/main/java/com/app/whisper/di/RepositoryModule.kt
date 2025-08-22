package com.app.whisper.di

import com.app.whisper.data.repository.FakeTranscriptionRepository
// TODO: Implement these repository classes
// import com.app.whisper.data.repository.AudioRecorderRepositoryImpl
// import com.app.whisper.data.repository.ModelRepositoryImpl
// import com.app.whisper.data.repository.TranscriptionRepositoryImpl
// import com.app.whisper.domain.repository.AudioRecorderRepository
// import com.app.whisper.domain.repository.ModelRepository
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
     * Binds TranscriptionRepository interface to its fake implementation.
     *
     * FakeTranscriptionRepository handles:
     * - Fake audio transcription operations for testing
     * - Session management simulation
     * - Result caching and storage simulation
     * - History and statistics simulation
     */
    @Binds
    @Singleton
    abstract fun bindTranscriptionRepository(
        fakeTranscriptionRepository: FakeTranscriptionRepository
    ): TranscriptionRepository

    // TODO: Implement and uncomment these bindings when repository implementations are ready
    /*
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
    */
}
