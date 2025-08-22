package com.app.whisper.di

import android.content.Context
import android.content.SharedPreferences
import com.app.whisper.data.audio.AudioRecorder
import com.app.whisper.data.manager.ModelManager
import com.app.whisper.domain.repository.AudioRecorderRepository
import com.app.whisper.domain.repository.ModelRepository
import com.app.whisper.domain.repository.TranscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Test module for Hilt dependency injection in tests.
 * 
 * This module replaces production modules with mock implementations
 * for isolated unit testing.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [
        NetworkModule::class,
        DatabaseModule::class,
        RepositoryModule::class,
        AudioModule::class
    ]
)
object TestModule {
    
    /**
     * Provides mock OkHttpClient for testing.
     */
    @Provides
    @Singleton
    fun provideTestOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }
    
    /**
     * Provides mock SharedPreferences for testing.
     */
    @Provides
    @Singleton
    fun provideTestSharedPreferences(): SharedPreferences {
        return mockk(relaxed = true)
    }
    
    /**
     * Provides mock TranscriptionRepository for testing.
     */
    @Provides
    @Singleton
    fun provideTestTranscriptionRepository(): TranscriptionRepository {
        return mockk(relaxed = true)
    }
    
    /**
     * Provides mock ModelRepository for testing.
     */
    @Provides
    @Singleton
    fun provideTestModelRepository(): ModelRepository {
        return mockk(relaxed = true)
    }
    
    /**
     * Provides mock AudioRecorderRepository for testing.
     */
    @Provides
    @Singleton
    fun provideTestAudioRecorderRepository(): AudioRecorderRepository {
        return mockk(relaxed = true)
    }
    
    /**
     * Provides mock AudioRecorder for testing.
     */
    @Provides
    @Singleton
    fun provideTestAudioRecorder(): AudioRecorder {
        return mockk(relaxed = true)
    }
    
    /**
     * Provides mock ModelManager for testing.
     */
    @Provides
    @Singleton
    fun provideTestModelManager(
        context: Context,
        httpClient: OkHttpClient,
        preferences: SharedPreferences
    ): ModelManager {
        return mockk(relaxed = true)
    }
}
