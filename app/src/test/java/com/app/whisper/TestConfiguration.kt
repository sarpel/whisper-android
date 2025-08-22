package com.app.whisper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Test configuration and utilities for Whisper Android tests.
 * 
 * This class provides common test setup, utilities, and configuration
 * that can be shared across different test classes.
 */
object TestConfiguration {
    
    /**
     * Default test timeout in milliseconds.
     */
    const val DEFAULT_TIMEOUT_MS = 5000L
    
    /**
     * Network timeout for mock server tests.
     */
    const val NETWORK_TIMEOUT_MS = 3000L
    
    /**
     * Audio test data sample rate.
     */
    const val TEST_SAMPLE_RATE = 16000
    
    /**
     * Test audio duration in milliseconds.
     */
    const val TEST_AUDIO_DURATION_MS = 1000L
    
    /**
     * Mock model file size for testing.
     */
    const val MOCK_MODEL_SIZE_BYTES = 1024L * 1024L // 1MB
    
    /**
     * Test user ID for consistent testing.
     */
    const val TEST_USER_ID = "test_user_123"
    
    /**
     * Test model ID for consistent testing.
     */
    const val TEST_MODEL_ID = "test_model"
    
    /**
     * Test session ID for consistent testing.
     */
    const val TEST_SESSION_ID = "test_session_123"
}

/**
 * JUnit rule for setting up coroutine test dispatcher.
 * 
 * This rule automatically sets up and tears down the test dispatcher
 * for coroutine testing, ensuring proper test isolation.
 */
@ExperimentalCoroutinesApi
class CoroutineTestRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }
    
    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}

/**
 * Test data factory for creating test objects.
 */
object TestDataFactory {
    
    /**
     * Create test audio data.
     */
    fun createTestAudioData(
        durationMs: Long = TestConfiguration.TEST_AUDIO_DURATION_MS,
        sampleRate: Int = TestConfiguration.TEST_SAMPLE_RATE
    ): ByteArray {
        val samplesCount = (durationMs * sampleRate / 1000).toInt()
        val audioData = ByteArray(samplesCount * 2) // 16-bit samples
        
        // Generate simple sine wave for testing
        for (i in 0 until samplesCount) {
            val sample = (Short.MAX_VALUE * 0.5 * kotlin.math.sin(2.0 * kotlin.math.PI * 440.0 * i / sampleRate)).toInt().toShort()
            audioData[i * 2] = (sample.toInt() and 0xFF).toByte()
            audioData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        
        return audioData
    }
    
    /**
     * Create test transcription result.
     */
    fun createTestTranscriptionResult(
        text: String = "This is a test transcription result.",
        confidence: Float = 0.95f,
        language: String = "en"
    ): com.app.whisper.domain.entity.TranscriptionResult {
        return com.app.whisper.domain.entity.TranscriptionResult(
            id = "test_result_${System.currentTimeMillis()}",
            text = text,
            language = language,
            confidence = confidence,
            processingTimeMs = 1000L,
            audioDurationMs = TestConfiguration.TEST_AUDIO_DURATION_MS
        )
    }
    
    /**
     * Create test processing parameters.
     */
    fun createTestProcessingParameters(
        language: String = "auto",
        translate: Boolean = false,
        temperature: Float = 0.0f
    ): com.app.whisper.domain.entity.ProcessingParameters {
        return com.app.whisper.domain.entity.ProcessingParameters(
            language = language,
            translate = translate,
            temperature = temperature,
            maxTokens = 224,
            compressionRatio = 2.4f,
            noSpeechThreshold = 0.6f,
            logProbThreshold = -1.0f
        )
    }
    
    /**
     * Create test audio metadata.
     */
    fun createTestAudioMetadata(
        fileName: String = "test_audio.wav",
        durationMs: Long = TestConfiguration.TEST_AUDIO_DURATION_MS
    ): com.app.whisper.domain.entity.AudioMetadata {
        return com.app.whisper.domain.entity.AudioMetadata(
            fileName = fileName,
            fileSizeBytes = TestConfiguration.MOCK_MODEL_SIZE_BYTES,
            durationMs = durationMs,
            sampleRate = TestConfiguration.TEST_SAMPLE_RATE,
            channels = 1,
            bitRate = 256,
            format = "wav"
        )
    }
    
    /**
     * Create test transcription session.
     */
    fun createTestTranscriptionSession(
        name: String = "Test Session",
        audioFilePath: String = "/test/path/audio.wav"
    ): com.app.whisper.domain.entity.TranscriptionSession {
        return com.app.whisper.domain.entity.TranscriptionSession.create(
            name = name,
            audioFilePath = audioFilePath,
            audioMetadata = createTestAudioMetadata(),
            modelName = TestConfiguration.TEST_MODEL_ID
        )
    }
    
    /**
     * Create test download progress.
     */
    fun createTestDownloadProgress(
        downloadedBytes: Long = 512L * 1024L, // 512KB
        totalBytes: Long = TestConfiguration.MOCK_MODEL_SIZE_BYTES
    ): com.app.whisper.domain.repository.DownloadProgress.InProgress {
        return com.app.whisper.domain.repository.DownloadProgress.InProgress(
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            progress = downloadedBytes.toFloat() / totalBytes.toFloat()
        )
    }
}

/**
 * Test assertions and utilities.
 */
object TestAssertions {
    
    /**
     * Assert that a transcription result is valid.
     */
    fun assertValidTranscriptionResult(result: com.app.whisper.domain.entity.TranscriptionResult) {
        assert(result.id.isNotEmpty()) { "Result ID should not be empty" }
        assert(result.text.isNotEmpty()) { "Result text should not be empty" }
        assert(result.confidence in 0.0f..1.0f) { "Confidence should be between 0.0 and 1.0" }
        assert(result.processingTimeMs > 0) { "Processing time should be positive" }
        assert(result.audioDurationMs > 0) { "Audio duration should be positive" }
    }
    
    /**
     * Assert that audio data is valid.
     */
    fun assertValidAudioData(audioData: ByteArray, expectedDurationMs: Long, sampleRate: Int) {
        val expectedSamples = (expectedDurationMs * sampleRate / 1000).toInt()
        val expectedBytes = expectedSamples * 2 // 16-bit samples
        
        assert(audioData.isNotEmpty()) { "Audio data should not be empty" }
        assert(audioData.size == expectedBytes) { 
            "Audio data size should be $expectedBytes bytes, but was ${audioData.size}" 
        }
    }
    
    /**
     * Assert that a transcription session is valid.
     */
    fun assertValidTranscriptionSession(session: com.app.whisper.domain.entity.TranscriptionSession) {
        assert(session.id.isNotEmpty()) { "Session ID should not be empty" }
        assert(session.name.isNotEmpty()) { "Session name should not be empty" }
        assert(session.audioFilePath.isNotEmpty()) { "Audio file path should not be empty" }
        assert(session.createdAt > 0) { "Created timestamp should be positive" }
    }
    
    /**
     * Assert that download progress is valid.
     */
    fun assertValidDownloadProgress(progress: com.app.whisper.domain.repository.DownloadProgress) {
        when (progress) {
            is com.app.whisper.domain.repository.DownloadProgress.InProgress -> {
                assert(progress.downloadedBytes >= 0) { "Downloaded bytes should be non-negative" }
                assert(progress.totalBytes > 0) { "Total bytes should be positive" }
                assert(progress.downloadedBytes <= progress.totalBytes) { 
                    "Downloaded bytes should not exceed total bytes" 
                }
                assert(progress.progress in 0.0f..1.0f) { "Progress should be between 0.0 and 1.0" }
            }
            is com.app.whisper.domain.repository.DownloadProgress.Completed -> {
                assert(progress.totalBytes > 0) { "Total bytes should be positive" }
            }
            is com.app.whisper.domain.repository.DownloadProgress.Failed -> {
                // Error can be any throwable, no specific assertions needed
            }
        }
    }
}
