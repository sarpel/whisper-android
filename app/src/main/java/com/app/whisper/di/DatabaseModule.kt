package com.app.whisper.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database and storage-related dependencies.
 * 
 * This module provides SharedPreferences with encryption for secure
 * storage of app settings, model states, and user preferences.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provides encrypted SharedPreferences for secure data storage.
     * 
     * Uses Android's EncryptedSharedPreferences with AES256-GCM encryption
     * for storing sensitive data like model states, user preferences,
     * and app configuration.
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return try {
            // Create master key for encryption
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            // Create encrypted shared preferences
            EncryptedSharedPreferences.create(
                context,
                "whisper_preferences",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences("whisper_preferences_fallback", Context.MODE_PRIVATE)
        }
    }
    
    /**
     * Provides regular SharedPreferences for non-sensitive data.
     * 
     * Used for caching and temporary data that doesn't require encryption.
     */
    @Provides
    @Singleton
    @CachePreferences
    fun provideCacheSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("whisper_cache", Context.MODE_PRIVATE)
    }
}

/**
 * Qualifier annotation for cache SharedPreferences.
 */
@Retention(AnnotationRetention.BINARY)
@javax.inject.Qualifier
annotation class CachePreferences
