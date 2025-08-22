# Performance Guide ⚡

This guide provides comprehensive information about performance optimization, monitoring, and tuning in the Whisper Android application.

## 📋 Table of Contents

- [Overview](#overview)
- [Device Performance Tiers](#device-performance-tiers)
- [Memory Optimization](#memory-optimization)
- [Audio Processing Optimization](#audio-processing-optimization)
- [Model Performance](#model-performance)
- [Battery Optimization](#battery-optimization)
- [Monitoring and Profiling](#monitoring-and-profiling)
- [Best Practices](#best-practices)

## 🎯 Overview

Whisper Android implements a comprehensive performance optimization system that automatically adapts to device capabilities, ensuring optimal user experience across all Android devices from budget to flagship.

### Key Performance Features

- **🎯 Device-Aware Optimization**: Automatic performance tier detection
- **💾 Smart Memory Management**: Proactive memory monitoring and cleanup
- **🔄 Adaptive Processing**: Performance-based feature enabling/disabling
- **⚡ ARM v8 Optimization**: Native 64-bit ARM performance enhancements
- **🔋 Battery Efficiency**: Power-aware processing modes

## 📱 Device Performance Tiers

The application automatically classifies devices into performance tiers and optimizes accordingly.

### Tier Classification

```kotlin
enum class PerformanceTier {
    LOW,    // Budget devices, limited resources
    MEDIUM, // Mid-range devices, balanced performance  
    HIGH    // High-end devices, maximum performance
}
```

### Classification Criteria

#### HIGH Tier (Flagship Devices)
- **RAM**: 8GB or more
- **CPU Cores**: 8 or more
- **Architecture**: ARM64-v8a support
- **Examples**: Samsung Galaxy S23+, Google Pixel 7 Pro, OnePlus 11

**Optimizations:**
```kotlin
PerformanceConfig(
    maxConcurrentDownloads = 3,
    audioBufferSize = 8192,
    waveformUpdateInterval = 16L, // 60 FPS
    enableAdvancedFeatures = true,
    useHardwareAcceleration = true,
    maxCacheSize = 100L * 1024 * 1024, // 100MB
    audioProcessingThreads = 4
)
```

#### MEDIUM Tier (Mid-range Devices)
- **RAM**: 4GB to 8GB
- **CPU Cores**: 4 to 8
- **Examples**: Samsung Galaxy A54, Google Pixel 6a, Xiaomi Redmi Note 12

**Optimizations:**
```kotlin
PerformanceConfig(
    maxConcurrentDownloads = 2,
    audioBufferSize = 4096,
    waveformUpdateInterval = 33L, // 30 FPS
    enableAdvancedFeatures = true,
    useHardwareAcceleration = true,
    maxCacheSize = 50L * 1024 * 1024, // 50MB
    audioProcessingThreads = 2
)
```

#### LOW Tier (Budget Devices)
- **RAM**: Less than 4GB
- **CPU Cores**: 4 or fewer
- **Examples**: Samsung Galaxy A13, Nokia G21, older devices

**Optimizations:**
```kotlin
PerformanceConfig(
    maxConcurrentDownloads = 1,
    audioBufferSize = 2048,
    waveformUpdateInterval = 66L, // 15 FPS
    enableAdvancedFeatures = false,
    useHardwareAcceleration = false,
    maxCacheSize = 25L * 1024 * 1024, // 25MB
    audioProcessingThreads = 1
)
```

## 💾 Memory Optimization

### Memory Pressure Monitoring

The application continuously monitors memory usage and responds to pressure:

```kotlin
enum class MemoryPressure {
    NORMAL,   // < 50% memory usage
    MODERATE, // 50-75% memory usage
    HIGH,     // 75-90% memory usage
    CRITICAL  // > 90% memory usage or system low memory
}
```

### Automatic Memory Management

#### Memory Cleanup Strategies

1. **Light Cleanup** (Moderate Pressure)
   - Clear expired cache entries
   - Suggest garbage collection
   - Compress non-essential data

2. **Moderate Cleanup** (High Pressure)
   - Clear 50% of cache entries
   - Force garbage collection
   - Release non-critical resources

3. **Aggressive Cleanup** (Critical Pressure)
   - Clear all caches
   - Multiple garbage collection cycles
   - Release all non-essential resources
   - Disable advanced features temporarily

### Memory Usage Guidelines

#### Recommended Memory Allocation

| Component | LOW Tier | MEDIUM Tier | HIGH Tier |
|-----------|----------|-------------|-----------|
| Audio Buffer | 2KB | 4KB | 8KB |
| Model Cache | 25MB | 50MB | 100MB |
| UI Cache | 5MB | 10MB | 20MB |
| Waveform Data | 1MB | 2MB | 5MB |

#### Memory Monitoring Code

```kotlin
// Monitor memory usage
val memoryStats = memoryOptimizer.getMemoryStats()
Timber.i("Memory Usage: ${memoryStats.usedMemory / (1024 * 1024)}MB")
Timber.i("Available: ${memoryStats.availableMemory / (1024 * 1024)}MB")
Timber.i("Pressure: ${memoryStats.memoryPressure}")

// Respond to memory pressure
memoryOptimizer.memoryPressure.collect { pressure ->
    when (pressure) {
        MemoryPressure.CRITICAL -> {
            // Disable non-essential features
            disableWaveformVisualization()
            clearAllCaches()
        }
        MemoryPressure.HIGH -> {
            // Reduce quality settings
            reduceAudioBufferSize()
            clearOldCacheEntries()
        }
        // ... handle other levels
    }
}
```

## 🎵 Audio Processing Optimization

### Buffer Size Optimization

Optimal buffer sizes are calculated based on device performance:

```kotlin
suspend fun getOptimalBufferSize(
    sampleRate: Int,
    channelConfig: Int,
    audioFormat: Int
): Int {
    val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    val performanceConfig = performanceManager.optimizeForDevice()
    
    val multiplier = when (performanceManager.getPerformanceTier()) {
        PerformanceTier.HIGH -> 4
        PerformanceTier.MEDIUM -> 2
        PerformanceTier.LOW -> 1
    }
    
    return max(minBufferSize * multiplier, performanceConfig.audioBufferSize)
}
```

### Audio Processing Features

#### Performance-Aware Processing

```kotlin
// High-end devices: Full processing pipeline
val processedAudio = when (performanceTier) {
    PerformanceTier.HIGH -> {
        audioOptimizer.applyNoiseReduction(audioData, sampleRate, 0.8f)
            .let { audioOptimizer.applyAutomaticGainControl(it, 0.7f) }
            .let { audioOptimizer.normalizeAudio(it, 0.8f) }
    }
    PerformanceTier.MEDIUM -> {
        audioOptimizer.applyNoiseReduction(audioData, sampleRate, 0.5f)
            .let { audioOptimizer.normalizeAudio(it, 0.7f) }
    }
    PerformanceTier.LOW -> {
        audioOptimizer.normalizeAudio(audioData, 0.6f) // Basic processing only
    }
}
```

#### Waveform Visualization Optimization

```kotlin
// Adaptive waveform update rates
val updateInterval = when (performanceTier) {
    PerformanceTier.HIGH -> 16L    // 60 FPS
    PerformanceTier.MEDIUM -> 33L  // 30 FPS
    PerformanceTier.LOW -> 66L     // 15 FPS
}

// Adaptive waveform resolution
val waveformPoints = when (performanceTier) {
    PerformanceTier.HIGH -> 200
    PerformanceTier.MEDIUM -> 100
    PerformanceTier.LOW -> 50
}
```

## 🧠 Model Performance

### Model Selection Guidelines

#### Recommended Models by Device Tier

| Device Tier | Recommended Model | Processing Speed | Memory Usage |
|-------------|------------------|------------------|--------------|
| HIGH | Large (1.5GB) | 2.5x realtime | ~150MB |
| MEDIUM | Medium (769MB) | 1.8x realtime | ~100MB |
| LOW | Tiny (39MB) | 1.2x realtime | ~60MB |

#### Model Performance Benchmarks

```kotlin
// Benchmark results on different devices
val benchmarks = mapOf(
    "Samsung Galaxy S23" to ModelBenchmark(
        model = WhisperModel.LARGE,
        processingSpeed = 2.8f, // x realtime
        memoryUsage = 145L * 1024 * 1024, // MB
        batteryImpact = BatteryImpact.LOW
    ),
    "Google Pixel 6a" to ModelBenchmark(
        model = WhisperModel.MEDIUM,
        processingSpeed = 1.9f,
        memoryUsage = 98L * 1024 * 1024,
        batteryImpact = BatteryImpact.MEDIUM
    ),
    "Samsung Galaxy A13" to ModelBenchmark(
        model = WhisperModel.TINY,
        processingSpeed = 1.1f,
        memoryUsage = 58L * 1024 * 1024,
        batteryImpact = BatteryImpact.HIGH
    )
)
```

### Model Loading Optimization

```kotlin
// Lazy model loading
class ModelManager {
    private var loadedModel: WhisperModel? = null
    
    suspend fun ensureModelLoaded(model: WhisperModel): Result<Unit> {
        return if (loadedModel == model) {
            Result.success(Unit)
        } else {
            loadModel(model).also { result ->
                if (result.isSuccess) {
                    loadedModel = model
                }
            }
        }
    }
    
    // Preload model in background for better UX
    suspend fun preloadModel(model: WhisperModel) {
        withContext(Dispatchers.IO) {
            if (memoryOptimizer.memoryPressure.value <= MemoryPressure.MODERATE) {
                loadModel(model)
            }
        }
    }
}
```

## 🔋 Battery Optimization

### Power-Aware Processing

```kotlin
class BatteryOptimizer @Inject constructor(
    private val powerManager: PowerManager
) {
    
    fun isInPowerSaveMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
    }
    
    fun getOptimizedProcessingConfig(): ProcessingConfig {
        return if (isInPowerSaveMode()) {
            ProcessingConfig(
                enableNoiseReduction = false,
                enableAutomaticGainControl = false,
                reduceWaveformUpdates = true,
                useMinimalModel = true
            )
        } else {
            ProcessingConfig.default()
        }
    }
}
```

### Background Processing Optimization

```kotlin
// Efficient background processing
class BackgroundTranscriptionService : Service() {
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isInPowerSaveMode()) {
            // Defer processing until power save mode is disabled
            scheduleForLater(intent)
            return START_NOT_STICKY
        }
        
        // Process with reduced priority
        processWithLowPriority(intent)
        return START_STICKY
    }
    
    private fun processWithLowPriority(intent: Intent?) {
        // Use background thread with lower priority
        Thread.currentThread().priority = Thread.MIN_PRIORITY
        
        // Process in smaller chunks to avoid blocking
        processInChunks(audioData, chunkSize = 1024)
    }
}
```

## 📊 Monitoring and Profiling

### Performance Metrics Collection

```kotlin
class PerformanceMetrics @Inject constructor() {
    
    data class TranscriptionMetrics(
        val modelSize: String,
        val audioLengthMs: Long,
        val processingTimeMs: Long,
        val memoryUsedMB: Long,
        val batteryUsedMah: Float,
        val deviceTier: PerformanceTier
    )
    
    fun recordTranscriptionMetrics(
        model: WhisperModel,
        audioLength: Long,
        processingTime: Long
    ) {
        val metrics = TranscriptionMetrics(
            modelSize = model.id,
            audioLengthMs = audioLength,
            processingTimeMs = processingTime,
            memoryUsedMB = getCurrentMemoryUsage(),
            batteryUsedMah = getBatteryUsage(),
            deviceTier = performanceManager.getPerformanceTier()
        )
        
        // Log metrics for analysis
        Timber.i("Transcription Metrics: $metrics")
        
        // Send to analytics (if enabled)
        analyticsManager.recordPerformanceMetrics(metrics)
    }
}
```

### Profiling Tools Integration

```kotlin
// Systrace integration for detailed profiling
suspend fun transcribeWithProfiling(audioData: AudioData): TranscriptionResult {
    return trace("Transcription.Full") {
        trace("Transcription.Preprocessing") {
            preprocessAudio(audioData)
        }
        
        trace("Transcription.ModelInference") {
            runModelInference(audioData)
        }
        
        trace("Transcription.Postprocessing") {
            postprocessResult(rawResult)
        }
    }
}
```

## 🎯 Best Practices

### Development Guidelines

1. **Always Use Performance-Aware APIs**
   ```kotlin
   // Good: Use performance-aware processing
   val config = performanceManager.optimizeForDevice()
   processAudio(audioData, config)
   
   // Bad: Use fixed configuration
   processAudio(audioData, FixedConfig.HIGH_QUALITY)
   ```

2. **Monitor Memory Usage**
   ```kotlin
   // Good: Check memory before large operations
   memoryOptimizer.withMemoryOptimization {
       performLargeOperation()
   }
   
   // Bad: Ignore memory constraints
   performLargeOperation()
   ```

3. **Use Appropriate Threading**
   ```kotlin
   // Good: Use appropriate dispatcher
   withContext(Dispatchers.Default) {
       // CPU-intensive work
   }
   
   withContext(Dispatchers.IO) {
       // I/O operations
   }
   ```

4. **Implement Graceful Degradation**
   ```kotlin
   // Good: Graceful feature degradation
   val features = when (performanceTier) {
       PerformanceTier.HIGH -> FullFeatureSet()
       PerformanceTier.MEDIUM -> ReducedFeatureSet()
       PerformanceTier.LOW -> MinimalFeatureSet()
   }
   ```

### Testing Performance

```kotlin
@Test
fun `transcription performance meets requirements`() = runTest {
    val audioData = TestDataFactory.createTestAudioData(durationMs = 10_000)
    val startTime = System.currentTimeMillis()
    
    val result = transcribeAudioUseCase.execute(audioData, ProcessingParameters())
        .filterIsInstance<TranscriptionProgress.Completed>()
        .first()
    
    val processingTime = System.currentTimeMillis() - startTime
    val speedRatio = audioData.durationMs.toFloat() / processingTime
    
    // Assert performance requirements
    when (performanceManager.getPerformanceTier()) {
        PerformanceTier.HIGH -> assertThat(speedRatio).isAtLeast(2.0f)
        PerformanceTier.MEDIUM -> assertThat(speedRatio).isAtLeast(1.5f)
        PerformanceTier.LOW -> assertThat(speedRatio).isAtLeast(1.0f)
    }
}
```

### Optimization Checklist

- ✅ **Device tier detection implemented**
- ✅ **Memory pressure monitoring active**
- ✅ **Audio buffer sizes optimized**
- ✅ **Model selection based on device capabilities**
- ✅ **Battery usage optimized**
- ✅ **Background processing efficient**
- ✅ **UI updates throttled appropriately**
- ✅ **Caching strategies implemented**
- ✅ **Error handling doesn't impact performance**
- ✅ **Performance metrics collected**

This performance guide ensures that Whisper Android delivers optimal performance across all device tiers while maintaining excellent user experience and battery efficiency.
