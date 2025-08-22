package com.app.whisper.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for providing network-related dependencies.
 * 
 * This module configures OkHttpClient with appropriate settings for
 * model downloads, caching, and logging.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Provides OkHttpClient for network operations.
     * 
     * Configured with:
     * - Connection and read timeouts for large file downloads
     * - Cache for efficient model downloads
     * - Logging interceptor for debugging
     * - User agent for proper identification
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .cache(cache)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "WhisperAndroid/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    /**
     * Provides HTTP cache for OkHttpClient.
     * 
     * Cache is stored in app's cache directory with 50MB limit.
     * This helps with resuming interrupted downloads and avoiding
     * re-downloading the same model files.
     */
    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context): Cache {
        val cacheDir = File(context.cacheDir, "http_cache")
        val cacheSize = 50L * 1024 * 1024 // 50MB
        return Cache(cacheDir, cacheSize)
    }
    
    /**
     * Provides HTTP logging interceptor.
     * 
     * Logging level is set based on build type:
     * - DEBUG: BODY level for detailed logging
     * - RELEASE: NONE level for no logging
     */
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (com.app.whisper.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
}
