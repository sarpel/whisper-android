# API Reference 📚

This document provides comprehensive API documentation for the Whisper Android application, including all public interfaces, classes, and methods.

## 📋 Table of Contents

- [Domain Layer APIs](#domain-layer-apis)
- [Data Layer APIs](#data-layer-apis)
- [Presentation Layer APIs](#presentation-layer-apis)
- [Native Layer APIs](#native-layer-apis)
- [Performance APIs](#performance-apis)
- [Error Handling](#error-handling)

## 🏛️ Domain Layer APIs

### Use Cases

#### TranscribeAudioUseCase

Handles audio transcription business logic.

```kotlin
class TranscribeAudioUseCase @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository,
    private val modelRepository: ModelRepository
) {
    /**
     * Executes audio transcription with progress tracking.
     * 
     * @param audioData The audio data to transcribe
     * @param parameters Processing parameters for transcription
     * @return Flow of transcription progress updates
     */
    suspend fun execute(
        audioData: AudioData,
        parameters: ProcessingParameters
    ): Flow<TranscriptionProgress>
}
```

**Parameters:**
- `audioData: AudioData` - Audio data containing samples and metadata
- `parameters: ProcessingParameters` - Configuration for transcription

**Returns:**
- `Flow<TranscriptionProgress>` - Stream of progress updates

**Progress Types:**
```kotlin
sealed class TranscriptionProgress {
    object Started : TranscriptionProgress()
    data class ModelLoaded(val modelName: String) : TranscriptionProgress()
    object Processing : TranscriptionProgress()
    data class Completed(val result: TranscriptionResult) : TranscriptionProgress()
    data class Failed(val error: Throwable) : TranscriptionProgress()
}
```

#### DownloadModelUseCase

Manages model download operations.

```kotlin
class DownloadModelUseCase @Inject constructor(
    private val modelRepository: ModelRepository
) {
    /**
     * Downloads a Whisper model with progress tracking.
     * 
     * @param model The model to download
     * @return Flow of download progress updates
     */
    suspend fun execute(model: WhisperModel): Flow<DownloadProgress>
}
```

### Entities

#### TranscriptionResult

Represents the result of a transcription operation.

```kotlin
data class TranscriptionResult(
    val id: String,                    // Unique identifier
    val text: String,                  // Transcribed text
    val language: String,              // Detected/specified language
    val confidence: Float,             // Confidence score (0.0-1.0)
    val processingTimeMs: Long,        // Processing time in milliseconds
    val audioDurationMs: Long,         // Audio duration in milliseconds
    val segments: List<TranscriptionSegment> = emptyList(), // Optional segments
    val createdAt: Long = System.currentTimeMillis()       // Creation timestamp
) {
    /**
     * Formats the confidence as a percentage string.
     */
    fun getConfidencePercentage(): String = "${(confidence * 100).toInt()}%"
    
    /**
     * Checks if the transcription has high confidence.
     */
    fun hasHighConfidence(): Boolean = confidence >= 0.8f
}
```

#### WhisperModel

Enum representing available Whisper models.

```kotlin
enum class WhisperModel(
    val id: String,
    val displayName: String,
    val fileSizeBytes: Long,
    val downloadUrl: String,
    val description: String
) {
    TINY("tiny", "Tiny", 39L * 1024 * 1024, "...", "Fastest, lowest accuracy"),
    BASE("base", "Base", 74L * 1024 * 1024, "...", "Good balance of speed and accuracy"),
    SMALL("small", "Small", 244L * 1024 * 1024, "...", "Better accuracy, slower"),
    MEDIUM("medium", "Medium", 769L * 1024 * 1024, "...", "High accuracy, requires more resources"),
    LARGE("large", "Large", 1550L * 1024 * 1024, "...", "Highest accuracy, slowest");
    
    // Mutable properties for runtime state
    var status: ModelStatus = ModelStatus.NotDownloaded
    var localPath: String? = null
    var downloadedAt: Long? = null
    var lastUsedAt: Long? = null
    var isCurrent: Boolean = false
    
    /**
     * Gets formatted file size string.
     */
    fun getFileSizeFormatted(): String {
        val mb = fileSizeBytes / (1024 * 1024)
        return "${mb}MB"
    }
    
    /**
     * Checks if model is available for use.
     */
    fun isAvailable(): Boolean = status == ModelStatus.Available && localPath != null
}
```

#### ProcessingParameters

Configuration parameters for transcription.

```kotlin
data class ProcessingParameters(
    val language: String = "auto",           // Language code or "auto"
    val translate: Boolean = false,          // Translate to English
    val temperature: Float = 0.0f,           // Sampling temperature
    val maxTokens: Int = 224,               // Maximum tokens to generate
    val compressionRatio: Float = 2.4f,     // Compression ratio threshold
    val noSpeechThreshold: Float = 0.6f,    // No speech threshold
    val logProbThreshold: Float = -1.0f     // Log probability threshold
) {
    companion object {
        /**
         * Creates default parameters optimized for the given use case.
         */
        fun forUseCase(useCase: TranscriptionUseCase): ProcessingParameters {
            return when (useCase) {
                TranscriptionUseCase.REAL_TIME -> ProcessingParameters(
                    temperature = 0.0f,
                    maxTokens = 64
                )
                TranscriptionUseCase.HIGH_ACCURACY -> ProcessingParameters(
                    temperature = 0.2f,
                    maxTokens = 448
                )
                TranscriptionUseCase.FAST_DRAFT -> ProcessingParameters(
                    temperature = 0.8f,
                    maxTokens = 128
                )
            }
        }
    }
}
```

## 💾 Data Layer APIs

### Repositories

#### TranscriptionRepository

Interface for transcription operations.

```kotlin
interface TranscriptionRepository {
    /**
     * Transcribes audio data using the specified model.
     */
    suspend fun transcribeAudio(
        audioData: AudioData,
        modelPath: String,
        parameters: ProcessingParameters
    ): Flow<TranscriptionProgress>
    
    /**
     * Validates if a model file is valid and usable.
     */
    suspend fun validateModel(modelPath: String): Result<Boolean>
    
    /**
     * Gets supported languages for the current model.
     */
    suspend fun getSupportedLanguages(): List<String>
}
```

#### ModelRepository

Interface for model management operations.

```kotlin
interface ModelRepository {
    /**
     * Downloads a model with progress tracking.
     */
    suspend fun downloadModel(model: WhisperModel): Flow<DownloadProgress>
    
    /**
     * Deletes a downloaded model.
     */
    suspend fun deleteModel(model: WhisperModel): Result<Unit>
    
    /**
     * Gets all available models.
     */
    fun getAllModels(): List<WhisperModel>
    
    /**
     * Gets currently downloaded models.
     */
    fun getDownloadedModels(): List<WhisperModel>
    
    /**
     * Gets the current active model.
     */
    val currentModel: StateFlow<WhisperModel?>
    
    /**
     * Sets the current active model.
     */
    suspend fun setCurrentModel(model: WhisperModel): Result<Unit>
}
```

### Data Models

#### AudioData

Represents audio data with metadata.

```kotlin
data class AudioData(
    val samples: ShortArray,        // Audio samples (16-bit PCM)
    val sampleRate: Int,           // Sample rate in Hz
    val channels: Int,             // Number of channels
    val durationMs: Long,          // Duration in milliseconds
    val format: AudioFormat = AudioFormat.PCM_16BIT
) {
    companion object {
        /**
         * Creates AudioData from byte array.
         */
        fun fromByteArray(
            data: ByteArray,
            sampleRate: Int = 16000,
            channels: Int = 1
        ): AudioData
        
        /**
         * Creates AudioData from float array.
         */
        fun fromFloatArray(
            data: FloatArray,
            sampleRate: Int = 16000,
            channels: Int = 1
        ): AudioData
    }
    
    /**
     * Converts to float array for processing.
     */
    fun toFloatArray(): FloatArray
    
    /**
     * Gets audio data as byte array.
     */
    fun toByteArray(): ByteArray
}
```

## 📱 Presentation Layer APIs

### ViewModels

#### TranscriptionViewModel

Main ViewModel for transcription screen.

```kotlin
@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    private val transcribeAudioUseCase: TranscribeAudioUseCase,
    private val downloadModelUseCase: DownloadModelUseCase,
    private val audioRecorder: AudioRecorder
) : ViewModel() {
    
    /**
     * Current UI state.
     */
    val uiState: StateFlow<TranscriptionUiState>
    
    /**
     * Current recording state.
     */
    val recordingState: StateFlow<RecordingState>
    
    /**
     * Audio level for visualization.
     */
    val audioLevel: StateFlow<Float>
    
    /**
     * Starts audio recording.
     */
    fun startRecording()
    
    /**
     * Stops audio recording and starts transcription.
     */
    fun stopRecording()
    
    /**
     * Pauses audio recording.
     */
    fun pauseRecording()
    
    /**
     * Resumes audio recording.
     */
    fun resumeRecording()
    
    /**
     * Clears the current transcription result.
     */
    fun clearResult()
}
```

### UI States

#### TranscriptionUiState

Represents the current state of the transcription screen.

```kotlin
sealed class TranscriptionUiState {
    object Initial : TranscriptionUiState()
    
    data class Ready(
        val currentModel: WhisperModel?,
        val recordingState: RecordingState,
        val canRecord: Boolean
    ) : TranscriptionUiState()
    
    data class Recording(
        val recordingState: RecordingState,
        val duration: Long,
        val audioLevel: Float,
        val waveformData: WaveformData?,
        val canPause: Boolean,
        val canStop: Boolean
    ) : TranscriptionUiState()
    
    data class Processing(
        val progress: TranscriptionProgress,
        val audioData: AudioData?
    ) : TranscriptionUiState()
    
    data class Success(
        val result: TranscriptionResult,
        val processingTimeMs: Long
    ) : TranscriptionUiState()
    
    data class Error(
        val error: Throwable,
        val canRetry: Boolean = true
    ) : TranscriptionUiState()
}
```

## 🔧 Native Layer APIs

### WhisperNative

JNI interface to whisper.cpp.

```kotlin
class WhisperNative {
    /**
     * Loads a Whisper model from file.
     * 
     * @param modelPath Path to the model file
     * @return true if model loaded successfully
     */
    external fun loadModel(modelPath: String): Boolean
    
    /**
     * Transcribes audio data.
     * 
     * @param audioData Float array of audio samples
     * @param language Language code (e.g., "en", "auto")
     * @param translate Whether to translate to English
     * @return Transcription result as JSON string
     */
    external fun transcribe(
        audioData: FloatArray,
        language: String = "auto",
        translate: Boolean = false
    ): String
    
    /**
     * Gets the current model information.
     * 
     * @return Model info as JSON string
     */
    external fun getModelInfo(): String
    
    /**
     * Releases the loaded model from memory.
     */
    external fun releaseModel()
    
    /**
     * Checks if a model is currently loaded.
     * 
     * @return true if model is loaded
     */
    external fun isModelLoaded(): Boolean
    
    companion object {
        init {
            System.loadLibrary("whisper-android")
        }
    }
}
```

## ⚡ Performance APIs

### PerformanceManager

Device performance monitoring and optimization.

```kotlin
@Singleton
class PerformanceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Gets current memory usage information.
     */
    fun getMemoryInfo(): MemoryInfo
    
    /**
     * Gets CPU information.
     */
    fun getCpuInfo(): CpuInfo
    
    /**
     * Determines device performance tier.
     */
    fun getPerformanceTier(): PerformanceTier
    
    /**
     * Gets optimized configuration for current device.
     */
    suspend fun optimizeForDevice(): PerformanceConfig
    
    /**
     * Monitors memory usage and returns status.
     */
    suspend fun monitorMemoryUsage(): MemoryStatus
    
    /**
     * Logs comprehensive performance metrics.
     */
    fun logPerformanceMetrics()
}
```

### AudioOptimizer

Audio processing optimization utilities.

```kotlin
@Singleton
class AudioOptimizer @Inject constructor(
    private val performanceManager: PerformanceManager
) {
    /**
     * Gets optimal buffer size for audio recording.
     */
    suspend fun getOptimalBufferSize(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int
    ): Int
    
    /**
     * Applies noise reduction to audio data.
     */
    suspend fun applyNoiseReduction(
        audioData: ShortArray,
        sampleRate: Int,
        intensity: Float = 0.5f
    ): ShortArray
    
    /**
     * Normalizes audio levels.
     */
    suspend fun normalizeAudio(
        audioData: ShortArray,
        targetLevel: Float = 0.8f
    ): ShortArray
    
    /**
     * Generates optimized waveform data for visualization.
     */
    suspend fun generateWaveformData(
        audioData: ShortArray,
        targetPoints: Int = 100
    ): FloatArray
    
    /**
     * Detects voice activity in audio data.
     */
    suspend fun detectVoiceActivity(
        audioData: ShortArray,
        sampleRate: Int,
        threshold: Float = 0.02f
    ): Boolean
}
```

## ❌ Error Handling

### Exception Types

```kotlin
sealed class WhisperError : Exception() {
    object ModelNotFound : WhisperError()
    object ModelLoadFailed : WhisperError()
    object InsufficientMemory : WhisperError()
    object AudioRecordingFailed : WhisperError()
    object PermissionDenied : WhisperError()
    data class TranscriptionFailed(val reason: String) : WhisperError()
    data class NetworkError(val cause: Throwable) : WhisperError()
    data class StorageError(val cause: Throwable) : WhisperError()
}
```

### Result Types

All potentially failing operations return `Result<T>` for safe error handling:

```kotlin
// Example usage
val result = modelManager.downloadModel(WhisperModel.TINY)
result.fold(
    onSuccess = { /* Handle success */ },
    onFailure = { error ->
        when (error) {
            is WhisperError.NetworkError -> /* Handle network error */
            is WhisperError.InsufficientMemory -> /* Handle memory error */
            else -> /* Handle other errors */
        }
    }
)
```

This API reference provides comprehensive documentation for all public interfaces in the Whisper Android application. For implementation examples and usage patterns, refer to the test files and example code in the repository.
