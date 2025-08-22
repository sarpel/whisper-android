# Architecture Guide 🏗️

This document provides a comprehensive overview of the Whisper Android application architecture, design patterns, and implementation details.

## 📋 Table of Contents

- [Overview](#overview)
- [Clean Architecture](#clean-architecture)
- [Layer Details](#layer-details)
- [Data Flow](#data-flow)
- [Dependency Injection](#dependency-injection)
- [Performance Architecture](#performance-architecture)
- [Security Architecture](#security-architecture)

## 🎯 Overview

Whisper Android follows **Clean Architecture** principles with clear separation of concerns, ensuring:

- **Testability**: Each layer can be tested independently
- **Maintainability**: Changes in one layer don't affect others
- **Scalability**: Easy to add new features and modify existing ones
- **Flexibility**: Business logic is independent of frameworks

## 🏛️ Clean Architecture

### Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   UI (Compose)  │  │   ViewModels    │  │   States    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │   Use Cases     │  │    Entities     │  │ Repositories│ │
│  │   (Business     │  │   (Models)      │  │ (Interfaces)│ │
│  │    Logic)       │  │                 │  │             │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │  Repositories   │  │  Data Sources   │  │   Models    │ │
│  │(Implementations)│  │  (Local/Remote) │  │   (DTOs)    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                      Native Layer                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │  whisper.cpp    │  │  JNI Bindings   │  │ Audio APIs  │ │
│  │   (C/C++)       │  │   (Kotlin/C++)  │  │  (Android)  │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Dependency Rule

Dependencies point inward:
- **Presentation** depends on **Domain**
- **Data** depends on **Domain**
- **Domain** depends on nothing (pure business logic)
- **Native** is accessed through **Data** layer

## 📱 Layer Details

### 1. Presentation Layer

**Responsibility**: UI and user interaction handling

#### Components:
- **UI Components (Compose)**
  ```kotlin
  @Composable
  fun RecordingScreen(
      uiState: TranscriptionUiState,
      onStartRecording: () -> Unit,
      onStopRecording: () -> Unit,
      // ... other callbacks
  )
  ```

- **ViewModels**
  ```kotlin
  @HiltViewModel
  class TranscriptionViewModel @Inject constructor(
      private val transcribeAudioUseCase: TranscribeAudioUseCase,
      private val audioRecorder: AudioRecorder
  ) : ViewModel()
  ```

- **UI States**
  ```kotlin
  sealed class TranscriptionUiState {
      object Initial : TranscriptionUiState()
      object Ready : TranscriptionUiState()
      data class Recording(val duration: Long) : TranscriptionUiState()
      data class Processing(val progress: Float) : TranscriptionUiState()
      data class Success(val result: TranscriptionResult) : TranscriptionUiState()
      data class Error(val error: Throwable) : TranscriptionUiState()
  }
  ```

#### Key Patterns:
- **MVVM**: ViewModel manages UI state and business logic calls
- **Unidirectional Data Flow**: State flows down, events flow up
- **State Hoisting**: State is managed at the appropriate level

### 2. Domain Layer

**Responsibility**: Business logic and rules

#### Components:
- **Entities**: Core business objects
  ```kotlin
  data class TranscriptionResult(
      val id: String,
      val text: String,
      val language: String,
      val confidence: Float,
      val processingTimeMs: Long,
      val audioDurationMs: Long
  )
  ```

- **Use Cases**: Business operations
  ```kotlin
  class TranscribeAudioUseCase @Inject constructor(
      private val transcriptionRepository: TranscriptionRepository,
      private val modelRepository: ModelRepository
  ) {
      suspend fun execute(
          audioData: AudioData,
          parameters: ProcessingParameters
      ): Flow<TranscriptionProgress>
  }
  ```

- **Repository Interfaces**: Data access contracts
  ```kotlin
  interface TranscriptionRepository {
      suspend fun transcribeAudio(
          audioData: AudioData,
          modelPath: String,
          parameters: ProcessingParameters
      ): Flow<TranscriptionProgress>
  }
  ```

#### Key Patterns:
- **Single Responsibility**: Each use case has one responsibility
- **Dependency Inversion**: Depends on abstractions, not implementations
- **Pure Functions**: Business logic is testable and predictable

### 3. Data Layer

**Responsibility**: Data access and management

#### Components:
- **Repository Implementations**
  ```kotlin
  @Singleton
  class TranscriptionRepositoryImpl @Inject constructor(
      private val whisperNative: WhisperNative,
      private val audioProcessor: AudioProcessor
  ) : TranscriptionRepository
  ```

- **Data Sources**
  ```kotlin
  @Singleton
  class ModelManager @Inject constructor(
      private val context: Context,
      private val httpClient: OkHttpClient,
      private val preferences: SharedPreferences
  )
  ```

- **Data Models**: DTOs and data transfer objects
  ```kotlin
  data class AudioData(
      val samples: ShortArray,
      val sampleRate: Int,
      val channels: Int,
      val durationMs: Long
  )
  ```

#### Key Patterns:
- **Repository Pattern**: Abstracts data access
- **Data Mapping**: Converts between data and domain models
- **Caching**: Local storage for performance and offline support

### 4. Native Layer

**Responsibility**: High-performance audio processing and ML inference

#### Components:
- **whisper.cpp Integration**
  ```cpp
  // Native C++ implementation
  extern "C" JNIEXPORT jstring JNICALL
  Java_com_app_whisper_native_WhisperNative_transcribe(
      JNIEnv *env,
      jobject thiz,
      jfloatArray audio_data,
      jstring model_path
  )
  ```

- **JNI Bindings**
  ```kotlin
  class WhisperNative {
      external fun loadModel(modelPath: String): Boolean
      external fun transcribe(
          audioData: FloatArray,
          language: String
      ): String
      
      companion object {
          init {
              System.loadLibrary("whisper-android")
          }
      }
  }
  ```

## 🔄 Data Flow

### Transcription Flow

```
User Input (UI) 
    ↓
ViewModel receives event
    ↓
ViewModel calls Use Case
    ↓
Use Case orchestrates business logic
    ↓
Repository handles data operations
    ↓
Native layer processes audio
    ↓
Results flow back through layers
    ↓
UI updates with new state
```

### Detailed Flow Example

1. **User taps record button**
   ```kotlin
   // UI
   RecordingButton(onClick = { onStartRecording() })
   ```

2. **ViewModel handles event**
   ```kotlin
   // ViewModel
   fun startRecording() {
       viewModelScope.launch {
           audioRecorder.startRecording()
               .collect { audioData ->
                   // Handle audio data
               }
       }
   }
   ```

3. **Use Case processes business logic**
   ```kotlin
   // Use Case
   suspend fun execute(audioData: AudioData): Flow<TranscriptionProgress> {
       return flow {
           emit(TranscriptionProgress.Started)
           val result = transcriptionRepository.transcribeAudio(audioData)
           emit(TranscriptionProgress.Completed(result))
       }
   }
   ```

4. **Repository coordinates data operations**
   ```kotlin
   // Repository
   override suspend fun transcribeAudio(audioData: AudioData): TranscriptionResult {
       val processedAudio = audioProcessor.preprocess(audioData)
       return whisperNative.transcribe(processedAudio, currentModel.path)
   }
   ```

## 🔧 Dependency Injection

### Hilt Architecture

```kotlin
// Application
@HiltAndroidApp
class WhisperApplication : Application()

// Activity
@AndroidEntryPoint
class MainActivity : ComponentActivity()

// ViewModel
@HiltViewModel
class TranscriptionViewModel @Inject constructor(...)

// Modules
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient
}
```

### Scoping Strategy

- **@Singleton**: Application-wide single instances
  - Repositories, Managers, Network clients
- **@ViewModelScoped**: ViewModel lifecycle
  - Use cases that maintain state
- **@ActivityScoped**: Activity lifecycle
  - UI-specific dependencies

## ⚡ Performance Architecture

### Memory Management

```kotlin
@Singleton
class MemoryOptimizer @Inject constructor() : ComponentCallbacks2 {
    
    private val _memoryPressure = MutableStateFlow(MemoryPressure.NORMAL)
    val memoryPressure: StateFlow<MemoryPressure> = _memoryPressure.asStateFlow()
    
    override fun onTrimMemory(level: Int) {
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL -> performAggressiveCleanup()
            TRIM_MEMORY_RUNNING_LOW -> performModerateCleanup()
            // ... handle other levels
        }
    }
}
```

### Device-Aware Optimization

```kotlin
@Singleton
class PerformanceManager @Inject constructor() {
    
    fun getPerformanceTier(): PerformanceTier {
        return when {
            isHighEndDevice() -> PerformanceTier.HIGH
            isMidRangeDevice() -> PerformanceTier.MEDIUM
            else -> PerformanceTier.LOW
        }
    }
    
    suspend fun optimizeForDevice(): PerformanceConfig {
        return when (getPerformanceTier()) {
            PerformanceTier.HIGH -> PerformanceConfig(
                enableAdvancedFeatures = true,
                audioBufferSize = 8192,
                maxConcurrentDownloads = 3
            )
            // ... other configurations
        }
    }
}
```

## 🔒 Security Architecture

### Data Protection

```kotlin
// Encrypted SharedPreferences
@Provides
@Singleton
fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    return EncryptedSharedPreferences.create(
        context,
        "whisper_preferences",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
```

### Permission Management

```kotlin
class PermissionHandler(private val activity: ComponentActivity) {
    
    private val _permissionState = MutableStateFlow(getInitialPermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    fun requestMicrophonePermission() {
        when {
            hasPermission() -> _permissionState.value = PermissionState.Granted
            shouldShowRationale() -> _permissionState.value = PermissionState.ShouldShowRationale
            else -> launchPermissionRequest()
        }
    }
}
```

## 🧪 Testing Architecture

### Testing Strategy

- **Unit Tests**: Domain layer (use cases, entities)
- **Integration Tests**: Data layer (repositories, data sources)
- **UI Tests**: Presentation layer (Compose components)
- **End-to-End Tests**: Complete user workflows

### Test Doubles

```kotlin
// Test Module
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
object TestRepositoryModule {
    
    @Provides
    @Singleton
    fun provideTestTranscriptionRepository(): TranscriptionRepository {
        return mockk(relaxed = true)
    }
}
```

## 📊 Metrics and Monitoring

### Performance Tracking

```kotlin
// Tracing critical paths
suspend fun transcribeAudio(audioData: AudioData): TranscriptionResult {
    return trace("TranscriptionRepository.transcribeAudio") {
        // Implementation
    }
}
```

### Error Handling

```kotlin
// Centralized error handling
sealed class WhisperError : Exception() {
    object ModelNotFound : WhisperError()
    object InsufficientMemory : WhisperError()
    data class TranscriptionFailed(val reason: String) : WhisperError()
}
```

This architecture ensures a robust, maintainable, and scalable Android application that follows modern development best practices while providing excellent performance and user experience.
