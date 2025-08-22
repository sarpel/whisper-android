package com.app.whisper.di

import android.content.Context
import com.app.whisper.performance.AudioOptimizer
import com.app.whisper.performance.MemoryOptimizer
import com.app.whisper.performance.PerformanceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for performance optimization dependencies.
 * 
 * This module provides performance monitoring, memory optimization,
 * and audio processing optimization services.
 */
@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {
    
    /**
     * Provides PerformanceManager for device performance monitoring.
     */
    @Provides
    @Singleton
    fun providePerformanceManager(
        @ApplicationContext context: Context
    ): PerformanceManager {
        return PerformanceManager(context)
    }
    
    /**
     * Provides AudioOptimizer for audio processing optimization.
     */
    @Provides
    @Singleton
    fun provideAudioOptimizer(
        performanceManager: PerformanceManager
    ): AudioOptimizer {
        return AudioOptimizer(performanceManager)
    }
    
    /**
     * Provides MemoryOptimizer for memory management and optimization.
     */
    @Provides
    @Singleton
    fun provideMemoryOptimizer(
        @ApplicationContext context: Context,
        performanceManager: PerformanceManager
    ): MemoryOptimizer {
        return MemoryOptimizer(context, performanceManager)
    }
}
