package com.app.whisper.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.app.whisper.native.WhisperNative
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for providing application-wide dependencies.
 * 
 * This module defines how to create and provide instances of various
 * dependencies that are used throughout the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // DataStore extension property
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
        name = "whisper_preferences"
    )
    
    /**
     * Provides the WhisperNative instance.
     * This is already a singleton due to the @Singleton annotation on the class.
     */
    @Provides
    @Singleton
    fun provideWhisperNative(): WhisperNative = WhisperNative()
    
    /**
     * Provides DataStore for storing user preferences.
     * 
     * @param context Application context
     * @return DataStore instance for preferences
     */
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
    
    /**
     * Provides OkHttpClient for network operations.
     * Used for downloading Whisper models and other network requests.
     * 
     * @return Configured OkHttpClient instance
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
