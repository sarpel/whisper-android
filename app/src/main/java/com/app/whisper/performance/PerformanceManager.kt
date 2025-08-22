package com.app.whisper.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import androidx.tracing.trace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring and optimization manager.
 * 
 * This class provides performance monitoring, memory management,
 * and optimization utilities for the Whisper Android application.
 */
@Singleton
class PerformanceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    /**
     * Get current memory usage information.
     */
    fun getMemoryInfo(): MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapAllocatedSize = Debug.getNativeHeapAllocatedSize()
        val nativeHeapFreeSize = Debug.getNativeHeapFreeSize()
        
        return MemoryInfo(
            totalMemory = memoryInfo.totalMem,
            availableMemory = memoryInfo.availMem,
            usedMemory = memoryInfo.totalMem - memoryInfo.availMem,
            lowMemory = memoryInfo.lowMemory,
            threshold = memoryInfo.threshold,
            jvmMaxMemory = runtime.maxMemory(),
            jvmTotalMemory = runtime.totalMemory(),
            jvmFreeMemory = runtime.freeMemory(),
            jvmUsedMemory = runtime.totalMemory() - runtime.freeMemory(),
            nativeHeapSize = nativeHeapSize,
            nativeHeapAllocatedSize = nativeHeapAllocatedSize,
            nativeHeapFreeSize = nativeHeapFreeSize
        )
    }
    
    /**
     * Get CPU usage information.
     */
    fun getCpuInfo(): CpuInfo {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        val isLowPowerMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
        
        return CpuInfo(
            availableProcessors = availableProcessors,
            isLowPowerMode = isLowPowerMode,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            cpuAbi = Build.CPU_ABI,
            cpuAbi2 = Build.CPU_ABI2
        )
    }
    
    /**
     * Get device performance tier.
     */
    fun getPerformanceTier(): PerformanceTier {
        val memoryInfo = getMemoryInfo()
        val cpuInfo = getCpuInfo()
        
        return when {
            // High-end devices
            memoryInfo.totalMemory >= 8L * 1024 * 1024 * 1024 && // 8GB+ RAM
            cpuInfo.availableProcessors >= 8 &&
            cpuInfo.supportedAbis.contains("arm64-v8a") -> PerformanceTier.HIGH
            
            // Mid-range devices
            memoryInfo.totalMemory >= 4L * 1024 * 1024 * 1024 && // 4GB+ RAM
            cpuInfo.availableProcessors >= 4 -> PerformanceTier.MEDIUM
            
            // Low-end devices
            else -> PerformanceTier.LOW
        }
    }
    
    /**
     * Optimize performance based on device capabilities.
     */
    suspend fun optimizeForDevice(): PerformanceConfig = withContext(Dispatchers.Default) {
        trace("PerformanceManager.optimizeForDevice") {
            val tier = getPerformanceTier()
            val memoryInfo = getMemoryInfo()
            val cpuInfo = getCpuInfo()
            
            Timber.d("Device performance tier: $tier")
            Timber.d("Available memory: ${memoryInfo.availableMemory / (1024 * 1024)} MB")
            Timber.d("CPU cores: ${cpuInfo.availableProcessors}")
            
            when (tier) {
                PerformanceTier.HIGH -> PerformanceConfig(
                    maxConcurrentDownloads = 3,
                    audioBufferSize = 8192,
                    waveformUpdateInterval = 16L, // 60 FPS
                    enableAdvancedFeatures = true,
                    useHardwareAcceleration = true,
                    maxCacheSize = 100L * 1024 * 1024, // 100MB
                    enableBackgroundProcessing = true,
                    audioProcessingThreads = 4
                )
                
                PerformanceTier.MEDIUM -> PerformanceConfig(
                    maxConcurrentDownloads = 2,
                    audioBufferSize = 4096,
                    waveformUpdateInterval = 33L, // 30 FPS
                    enableAdvancedFeatures = true,
                    useHardwareAcceleration = true,
                    maxCacheSize = 50L * 1024 * 1024, // 50MB
                    enableBackgroundProcessing = true,
                    audioProcessingThreads = 2
                )
                
                PerformanceTier.LOW -> PerformanceConfig(
                    maxConcurrentDownloads = 1,
                    audioBufferSize = 2048,
                    waveformUpdateInterval = 66L, // 15 FPS
                    enableAdvancedFeatures = false,
                    useHardwareAcceleration = false,
                    maxCacheSize = 25L * 1024 * 1024, // 25MB
                    enableBackgroundProcessing = false,
                    audioProcessingThreads = 1
                )
            }
        }
    }
    
    /**
     * Monitor memory usage and trigger cleanup if needed.
     */
    suspend fun monitorMemoryUsage(): MemoryStatus = withContext(Dispatchers.Default) {
        trace("PerformanceManager.monitorMemoryUsage") {
            val memoryInfo = getMemoryInfo()
            val usagePercentage = (memoryInfo.usedMemory.toFloat() / memoryInfo.totalMemory.toFloat()) * 100
            
            when {
                memoryInfo.lowMemory || usagePercentage > 90f -> {
                    Timber.w("Critical memory usage: ${usagePercentage.toInt()}%")
                    MemoryStatus.CRITICAL
                }
                usagePercentage > 75f -> {
                    Timber.w("High memory usage: ${usagePercentage.toInt()}%")
                    MemoryStatus.HIGH
                }
                usagePercentage > 50f -> {
                    Timber.d("Moderate memory usage: ${usagePercentage.toInt()}%")
                    MemoryStatus.MODERATE
                }
                else -> {
                    Timber.d("Normal memory usage: ${usagePercentage.toInt()}%")
                    MemoryStatus.NORMAL
                }
            }
        }
    }
    
    /**
     * Perform memory cleanup based on current usage.
     */
    suspend fun performMemoryCleanup(level: MemoryStatus) = withContext(Dispatchers.Default) {
        trace("PerformanceManager.performMemoryCleanup") {
            when (level) {
                MemoryStatus.CRITICAL -> {
                    Timber.i("Performing critical memory cleanup")
                    // Clear all non-essential caches
                    System.gc()
                    // Additional aggressive cleanup would go here
                }
                MemoryStatus.HIGH -> {
                    Timber.i("Performing high memory cleanup")
                    // Clear some caches
                    System.gc()
                }
                MemoryStatus.MODERATE -> {
                    Timber.d("Performing moderate memory cleanup")
                    // Light cleanup
                }
                MemoryStatus.NORMAL -> {
                    // No cleanup needed
                }
            }
        }
    }
    
    /**
     * Check if device supports hardware acceleration.
     */
    fun supportsHardwareAcceleration(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                getCpuInfo().supportedAbis.any { it.contains("arm64") || it.contains("x86_64") }
    }
    
    /**
     * Get recommended model based on device performance.
     */
    fun getRecommendedModel(): String {
        return when (getPerformanceTier()) {
            PerformanceTier.HIGH -> "large"
            PerformanceTier.MEDIUM -> "medium"
            PerformanceTier.LOW -> "tiny"
        }
    }
    
    /**
     * Log performance metrics.
     */
    fun logPerformanceMetrics() {
        val memoryInfo = getMemoryInfo()
        val cpuInfo = getCpuInfo()
        val tier = getPerformanceTier()
        
        Timber.i("=== Performance Metrics ===")
        Timber.i("Performance Tier: $tier")
        Timber.i("Total Memory: ${memoryInfo.totalMemory / (1024 * 1024)} MB")
        Timber.i("Available Memory: ${memoryInfo.availableMemory / (1024 * 1024)} MB")
        Timber.i("Used Memory: ${memoryInfo.usedMemory / (1024 * 1024)} MB")
        Timber.i("JVM Max Memory: ${memoryInfo.jvmMaxMemory / (1024 * 1024)} MB")
        Timber.i("JVM Used Memory: ${memoryInfo.jvmUsedMemory / (1024 * 1024)} MB")
        Timber.i("Native Heap Size: ${memoryInfo.nativeHeapSize / (1024 * 1024)} MB")
        Timber.i("CPU Cores: ${cpuInfo.availableProcessors}")
        Timber.i("Supported ABIs: ${cpuInfo.supportedAbis}")
        Timber.i("Low Power Mode: ${cpuInfo.isLowPowerMode}")
        Timber.i("Hardware Acceleration: ${supportsHardwareAcceleration()}")
        Timber.i("Recommended Model: ${getRecommendedModel()}")
        Timber.i("========================")
    }
}

/**
 * Memory usage information.
 */
data class MemoryInfo(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val lowMemory: Boolean,
    val threshold: Long,
    val jvmMaxMemory: Long,
    val jvmTotalMemory: Long,
    val jvmFreeMemory: Long,
    val jvmUsedMemory: Long,
    val nativeHeapSize: Long,
    val nativeHeapAllocatedSize: Long,
    val nativeHeapFreeSize: Long
)

/**
 * CPU information.
 */
data class CpuInfo(
    val availableProcessors: Int,
    val isLowPowerMode: Boolean,
    val supportedAbis: List<String>,
    val cpuAbi: String,
    val cpuAbi2: String?
)

/**
 * Performance configuration based on device capabilities.
 */
data class PerformanceConfig(
    val maxConcurrentDownloads: Int,
    val audioBufferSize: Int,
    val waveformUpdateInterval: Long,
    val enableAdvancedFeatures: Boolean,
    val useHardwareAcceleration: Boolean,
    val maxCacheSize: Long,
    val enableBackgroundProcessing: Boolean,
    val audioProcessingThreads: Int
)

/**
 * Device performance tiers.
 */
enum class PerformanceTier {
    LOW,    // Budget devices, limited resources
    MEDIUM, // Mid-range devices, balanced performance
    HIGH    // High-end devices, maximum performance
}

/**
 * Memory usage status levels.
 */
enum class MemoryStatus {
    NORMAL,   // < 50% memory usage
    MODERATE, // 50-75% memory usage
    HIGH,     // 75-90% memory usage
    CRITICAL  // > 90% memory usage or low memory flag
}
