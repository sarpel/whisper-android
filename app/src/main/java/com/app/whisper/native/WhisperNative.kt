package com.app.whisper.native

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Native interface for Whisper speech-to-text functionality.
 *
 * This class provides a thread-safe Kotlin interface to the native whisper.cpp library
 * through JNI bindings. It handles model loading, audio transcription, context management,
 * and resource cleanup with proper error handling and thread safety.
 *
 * Key features:
 * - Thread-safe operations with mutex protection
 * - Automatic resource cleanup
 * - Comprehensive error handling
 * - Context lifecycle management
 * - Memory leak prevention
 */
@Singleton
class WhisperNative @Inject constructor() {

    companion object {
        private const val TAG = "WhisperNative"

        // Thread counts optimized for different ARM v8 configurations
        const val THREADS_AUTO = -1
        const val THREADS_SINGLE = 1
        const val THREADS_DUAL = 2
        const val THREADS_QUAD = 4
        const val THREADS_OCTA = 8
    }

    // Thread safety and state management
    private val contextMutex = Mutex()
    private val contextPtr = AtomicLong(0L)
    private val isInitialized = AtomicBoolean(false)
    private val isReleased = AtomicBoolean(false)

    // Model information cache
    private var currentModelPath: String? = null
    private var currentThreadCount: Int = THREADS_QUAD
    private var modelInfo: String? = null

    init {
        try {
            System.loadLibrary("whisper-jni")
            Log.i(TAG, "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library", e)
            // Don't throw here - allow graceful degradation
        }
    }

    // Native method declarations
    external fun getVersionInfo(): String
    external fun initContext(modelPath: String, nThreads: Int): Long
    external fun transcribeAudio(
        contextPtr: Long,
        audioData: FloatArray,
        sampleRate: Int,
        language: String,
        translate: Boolean
    ): String
    external fun releaseContext(contextPtr: Long)
    external fun getModelInfo(contextPtr: Long): String
    external fun isMultilingual(contextPtr: Long): Boolean

    /**
     * Initialize the Whisper context with a model file.
     * This method is thread-safe and can be called multiple times.
     *
     * @param modelPath Path to the Whisper model file (.bin)
     * @param threadCount Number of threads to use (default: THREADS_QUAD)
     * @return Result indicating success or failure
     */
    suspend fun initialize(
        modelPath: String,
        threadCount: Int = THREADS_QUAD
    ): Result<Unit> = withContext(Dispatchers.IO) {
        contextMutex.withLock {
            try {
                // Check if already released
                if (isReleased.get()) {
                    return@withContext Result.failure(
                        IllegalStateException("WhisperNative has been released")
                    )
                }

                // Validate model file
                val modelFile = File(modelPath)
                if (!modelFile.exists() || !modelFile.canRead()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Model file not found or not readable: $modelPath")
                    )
                }

                // Release existing context if any
                if (isInitialized.get()) {
                    Log.i(TAG, "Releasing existing context before reinitializing")
                    releaseContextInternal()
                }

                // Determine optimal thread count
                val optimalThreads = when (threadCount) {
                    THREADS_AUTO -> determineOptimalThreadCount()
                    else -> threadCount.coerceIn(1, 8)
                }

                Log.i(TAG, "Initializing Whisper context: model=$modelPath, threads=$optimalThreads")

                val newContextPtr = initContext(modelPath, optimalThreads)
                if (newContextPtr != 0L) {
                    contextPtr.set(newContextPtr)
                    currentModelPath = modelPath
                    currentThreadCount = optimalThreads
                    isInitialized.set(true)
                    modelInfo = null // Reset cached model info

                    Log.i(TAG, "Whisper context initialized successfully")
                    Result.success(Unit)
                } else {
                    Log.e(TAG, "Failed to initialize Whisper context")
                    Result.failure(Exception("Failed to initialize Whisper context"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during initialization", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Transcribe audio data to text.
     * This method is thread-safe and validates input parameters.
     *
     * @param audioData Audio samples as FloatArray (16kHz, mono)
     * @param language Language code (e.g., "en", "auto", "tr")
     * @param translate Whether to translate to English
     * @param sampleRate Sample rate of the audio (default: 16000)
     * @return Result containing transcribed text or error
     */
    suspend fun transcribe(
        audioData: FloatArray,
        language: String = "auto",
        translate: Boolean = false,
        sampleRate: Int = 16000
    ): Result<String> = withContext(Dispatchers.IO) {
        contextMutex.withLock {
            try {
                // Validate state
                if (isReleased.get()) {
                    return@withContext Result.failure(
                        IllegalStateException("WhisperNative has been released")
                    )
                }

                if (!isInitialized.get() || contextPtr.get() == 0L) {
                    return@withContext Result.failure(
                        IllegalStateException("Whisper context not initialized")
                    )
                }

                // Validate input
                if (audioData.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Audio data is empty")
                    )
                }

                if (sampleRate <= 0) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Invalid sample rate: $sampleRate")
                    )
                }

                Log.d(TAG, "Transcribing audio: ${audioData.size} samples, " +
                          "language=$language, translate=$translate, sampleRate=$sampleRate")

                val result = transcribeAudio(
                    contextPtr.get(),
                    audioData,
                    sampleRate,
                    language,
                    translate
                )

                Log.d(TAG, "Transcription completed: ${result.length} characters")
                Result.success(result)

            } catch (e: Exception) {
                Log.e(TAG, "Exception during transcription", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get version information from the native library.
     *
     * @return Version string
     */
    fun getVersion(): String {
        return try {
            getVersionInfo()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get version info", e)
            "Native library not available: ${e.message}"
        }
    }

    /**
     * Release native resources.
     * This method is thread-safe and can be called multiple times.
     * After calling this method, the instance cannot be reused.
     */
    suspend fun release() {
        contextMutex.withLock {
            if (!isReleased.get()) {
                Log.i(TAG, "Releasing WhisperNative resources")
                releaseContextInternal()
                isReleased.set(true)
            }
        }
    }

    /**
     * Internal method to release context without mutex (already protected).
     */
    private fun releaseContextInternal() {
        val currentPtr = contextPtr.get()
        if (currentPtr != 0L) {
            try {
                releaseContext(currentPtr)
                Log.d(TAG, "Native context released successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing native context", e)
            } finally {
                contextPtr.set(0L)
                isInitialized.set(false)
                currentModelPath = null
                modelInfo = null
            }
        }
    }

    /**
     * Check if the native context is initialized and ready for use.
     *
     * @return true if context is ready for transcription
     */
    fun isReady(): Boolean = isInitialized.get() && !isReleased.get() && contextPtr.get() != 0L

    /**
     * Get detailed model information.
     * Results are cached for performance.
     *
     * @return Model information string
     */
    fun getModelInformation(): String {
        if (!isReady()) {
            return "Model not loaded"
        }

        // Return cached info if available
        modelInfo?.let { return it }

        return try {
            val info = getModelInfo(contextPtr.get())
            modelInfo = info // Cache the result
            info
        } catch (e: Exception) {
            Log.w(TAG, "Error getting model info", e)
            "Error getting model info: ${e.message}"
        }
    }

    /**
     * Check if the loaded model supports multilingual transcription.
     *
     * @return true if model supports multiple languages
     */
    fun supportsMultilingual(): Boolean {
        if (!isReady()) {
            return false
        }

        return try {
            isMultilingual(contextPtr.get())
        } catch (e: Exception) {
            Log.w(TAG, "Error checking multilingual support", e)
            false
        }
    }

    /**
     * Get current model path.
     *
     * @return Path to the currently loaded model, or null if not initialized
     */
    fun getCurrentModelPath(): String? = currentModelPath

    /**
     * Get current thread count.
     *
     * @return Number of threads being used for transcription
     */
    fun getCurrentThreadCount(): Int = currentThreadCount

    /**
     * Determine optimal thread count based on device capabilities.
     * This is a heuristic based on available processors.
     *
     * @return Recommended thread count
     */
    private fun determineOptimalThreadCount(): Int {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        return when {
            availableProcessors >= 8 -> THREADS_QUAD  // Use 4 threads on octa-core
            availableProcessors >= 4 -> THREADS_QUAD  // Use 4 threads on quad-core
            availableProcessors >= 2 -> THREADS_DUAL  // Use 2 threads on dual-core
            else -> THREADS_SINGLE                    // Use 1 thread on single-core
        }.also {
            Log.d(TAG, "Determined optimal thread count: $it (available processors: $availableProcessors)")
        }
    }

    /**
     * Validate audio data for transcription.
     *
     * @param audioData Audio samples to validate
     * @param minLength Minimum required length in samples
     * @param maxLength Maximum allowed length in samples
     * @return Result indicating validation success or failure
     */
    fun validateAudioData(
        audioData: FloatArray,
        minLength: Int = 1600,  // 0.1 seconds at 16kHz
        maxLength: Int = 480000 // 30 seconds at 16kHz
    ): Result<Unit> {
        return when {
            audioData.isEmpty() ->
                Result.failure(IllegalArgumentException("Audio data is empty"))

            audioData.size < minLength ->
                Result.failure(IllegalArgumentException(
                    "Audio too short: ${audioData.size} samples (minimum: $minLength)"
                ))

            audioData.size > maxLength ->
                Result.failure(IllegalArgumentException(
                    "Audio too long: ${audioData.size} samples (maximum: $maxLength)"
                ))

            audioData.all { it == 0.0f } ->
                Result.failure(IllegalArgumentException("Audio data contains only silence"))

            audioData.any { it.isNaN() || it.isInfinite() } ->
                Result.failure(IllegalArgumentException("Audio data contains invalid values"))

            else -> Result.success(Unit)
        }
    }

    /**
     * Finalize method to ensure resources are cleaned up.
     * This is called by the garbage collector as a safety net.
     */
    @Suppress("DEPRECATION")
    protected fun finalize() {
        if (isReady()) {
            Log.w(TAG, "WhisperNative finalized without explicit release() call - cleaning up")
            try {
                releaseContextInternal()
            } catch (e: Exception) {
                Log.e(TAG, "Error in finalize", e)
            }
        }
    }
}
