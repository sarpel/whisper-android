package com.app.whisper.performance

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import androidx.tracing.trace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Memory optimization and management utilities.
 * 
 * This class provides memory monitoring, cache management, and
 * automatic cleanup to optimize memory usage throughout the app.
 */
@Singleton
class MemoryOptimizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceManager: PerformanceManager
) : ComponentCallbacks2 {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Memory monitoring
    private val _memoryPressure = MutableStateFlow(MemoryPressure.NORMAL)
    val memoryPressure: StateFlow<MemoryPressure> = _memoryPressure.asStateFlow()
    
    // Cache management
    private val caches = ConcurrentHashMap<String, WeakReference<MemoryCache>>()
    private val memoryMonitoringJob: Job
    
    init {
        // Register for memory callbacks
        context.registerComponentCallbacks(this)
        
        // Start memory monitoring
        memoryMonitoringJob = startMemoryMonitoring()
    }
    
    /**
     * Start continuous memory monitoring.
     */
    private fun startMemoryMonitoring(): Job = scope.launch {
        while (isActive) {
            try {
                val memoryStatus = performanceManager.monitorMemoryUsage()
                val pressure = when (memoryStatus) {
                    MemoryStatus.CRITICAL -> MemoryPressure.CRITICAL
                    MemoryStatus.HIGH -> MemoryPressure.HIGH
                    MemoryStatus.MODERATE -> MemoryPressure.MODERATE
                    MemoryStatus.NORMAL -> MemoryPressure.NORMAL
                }
                
                if (_memoryPressure.value != pressure) {
                    _memoryPressure.value = pressure
                    handleMemoryPressureChange(pressure)
                }
                
                // Monitor every 30 seconds
                delay(30_000)
            } catch (e: Exception) {
                Timber.e(e, "Error in memory monitoring")
                delay(60_000) // Wait longer on error
            }
        }
    }
    
    /**
     * Handle memory pressure changes.
     */
    private suspend fun handleMemoryPressureChange(pressure: MemoryPressure) {
        trace("MemoryOptimizer.handleMemoryPressureChange") {
            Timber.i("Memory pressure changed to: $pressure")
            
            when (pressure) {
                MemoryPressure.CRITICAL -> {
                    performAggressiveCleanup()
                }
                MemoryPressure.HIGH -> {
                    performModerateCleanup()
                }
                MemoryPressure.MODERATE -> {
                    performLightCleanup()
                }
                MemoryPressure.NORMAL -> {
                    // No immediate action needed
                }
            }
        }
    }
    
    /**
     * Register a memory cache for management.
     */
    fun registerCache(name: String, cache: MemoryCache) {
        caches[name] = WeakReference(cache)
        Timber.d("Registered cache: $name")
    }
    
    /**
     * Unregister a memory cache.
     */
    fun unregisterCache(name: String) {
        caches.remove(name)
        Timber.d("Unregistered cache: $name")
    }
    
    /**
     * Perform light memory cleanup.
     */
    private suspend fun performLightCleanup() = withContext(Dispatchers.Default) {
        trace("MemoryOptimizer.performLightCleanup") {
            Timber.d("Performing light memory cleanup")
            
            // Clear expired cache entries
            caches.values.forEach { weakRef ->
                weakRef.get()?.clearExpired()
            }
            
            // Suggest garbage collection
            System.gc()
        }
    }
    
    /**
     * Perform moderate memory cleanup.
     */
    private suspend fun performModerateCleanup() = withContext(Dispatchers.Default) {
        trace("MemoryOptimizer.performModerateCleanup") {
            Timber.i("Performing moderate memory cleanup")
            
            // Clear half of each cache
            caches.values.forEach { weakRef ->
                weakRef.get()?.clearHalf()
            }
            
            // Force garbage collection
            System.gc()
            
            // Wait a bit for GC to complete
            delay(100)
        }
    }
    
    /**
     * Perform aggressive memory cleanup.
     */
    private suspend fun performAggressiveCleanup() = withContext(Dispatchers.Default) {
        trace("MemoryOptimizer.performAggressiveCleanup") {
            Timber.w("Performing aggressive memory cleanup")
            
            // Clear all caches
            caches.values.forEach { weakRef ->
                weakRef.get()?.clear()
            }
            
            // Force multiple garbage collections
            repeat(3) {
                System.gc()
                delay(50)
            }
            
            // Additional cleanup would go here
            // e.g., release non-essential resources, compress data, etc.
        }
    }
    
    /**
     * Get memory usage statistics.
     */
    suspend fun getMemoryStats(): MemoryStats = withContext(Dispatchers.Default) {
        trace("MemoryOptimizer.getMemoryStats") {
            val memoryInfo = performanceManager.getMemoryInfo()
            val cacheStats = getCacheStats()
            
            MemoryStats(
                totalMemory = memoryInfo.totalMemory,
                availableMemory = memoryInfo.availableMemory,
                usedMemory = memoryInfo.usedMemory,
                jvmMaxMemory = memoryInfo.jvmMaxMemory,
                jvmUsedMemory = memoryInfo.jvmUsedMemory,
                nativeHeapSize = memoryInfo.nativeHeapSize,
                nativeHeapAllocated = memoryInfo.nativeHeapAllocatedSize,
                cacheCount = cacheStats.cacheCount,
                totalCacheSize = cacheStats.totalSize,
                memoryPressure = _memoryPressure.value
            )
        }
    }
    
    /**
     * Get cache statistics.
     */
    private fun getCacheStats(): CacheStats {
        var totalSize = 0L
        var cacheCount = 0
        
        caches.values.forEach { weakRef ->
            weakRef.get()?.let { cache ->
                totalSize += cache.getSize()
                cacheCount++
            }
        }
        
        return CacheStats(cacheCount, totalSize)
    }
    
    /**
     * Optimize memory allocation for large operations.
     */
    suspend fun <T> withMemoryOptimization(
        operation: suspend () -> T
    ): T = withContext(Dispatchers.Default) {
        trace("MemoryOptimizer.withMemoryOptimization") {
            // Check memory before operation
            val initialPressure = _memoryPressure.value
            if (initialPressure >= MemoryPressure.HIGH) {
                performModerateCleanup()
            }
            
            try {
                operation()
            } finally {
                // Check memory after operation
                val finalPressure = performanceManager.monitorMemoryUsage()
                if (finalPressure >= MemoryStatus.HIGH) {
                    performLightCleanup()
                }
            }
        }
    }
    
    /**
     * Create an optimized byte array with memory monitoring.
     */
    suspend fun createOptimizedByteArray(size: Int): ByteArray? = withContext(Dispatchers.Default) {
        trace("MemoryOptimizer.createOptimizedByteArray") {
            val memoryInfo = performanceManager.getMemoryInfo()
            val requiredMemory = size.toLong()
            
            // Check if we have enough memory
            if (memoryInfo.availableMemory < requiredMemory * 2) { // 2x safety margin
                Timber.w("Insufficient memory for byte array of size: $size")
                performModerateCleanup()
                
                // Check again after cleanup
                val updatedMemoryInfo = performanceManager.getMemoryInfo()
                if (updatedMemoryInfo.availableMemory < requiredMemory * 1.5) {
                    Timber.e("Still insufficient memory after cleanup")
                    return@trace null
                }
            }
            
            try {
                ByteArray(size)
            } catch (e: OutOfMemoryError) {
                Timber.e(e, "OutOfMemoryError creating byte array of size: $size")
                performAggressiveCleanup()
                null
            }
        }
    }
    
    // ComponentCallbacks2 implementation
    override fun onConfigurationChanged(newConfig: Configuration) {
        // Handle configuration changes if needed
    }
    
    override fun onLowMemory() {
        Timber.w("System low memory callback received")
        scope.launch {
            _memoryPressure.value = MemoryPressure.CRITICAL
            performAggressiveCleanup()
        }
    }
    
    override fun onTrimMemory(level: Int) {
        val pressure = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> MemoryPressure.CRITICAL
            
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> MemoryPressure.HIGH
            
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> MemoryPressure.MODERATE
            
            else -> MemoryPressure.NORMAL
        }
        
        Timber.i("Memory trim requested: level=$level, pressure=$pressure")
        scope.launch {
            _memoryPressure.value = pressure
            handleMemoryPressureChange(pressure)
        }
    }
    
    /**
     * Cleanup resources.
     */
    fun cleanup() {
        context.unregisterComponentCallbacks(this)
        memoryMonitoringJob.cancel()
        scope.cancel()
    }
}

/**
 * Memory pressure levels.
 */
enum class MemoryPressure {
    NORMAL,   // Plenty of memory available
    MODERATE, // Some memory pressure
    HIGH,     // High memory pressure
    CRITICAL  // Critical memory situation
}

/**
 * Memory statistics.
 */
data class MemoryStats(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val jvmMaxMemory: Long,
    val jvmUsedMemory: Long,
    val nativeHeapSize: Long,
    val nativeHeapAllocated: Long,
    val cacheCount: Int,
    val totalCacheSize: Long,
    val memoryPressure: MemoryPressure
)

/**
 * Cache statistics.
 */
private data class CacheStats(
    val cacheCount: Int,
    val totalSize: Long
)

/**
 * Interface for memory-managed caches.
 */
interface MemoryCache {
    fun getSize(): Long
    fun clear()
    fun clearHalf()
    fun clearExpired()
}
