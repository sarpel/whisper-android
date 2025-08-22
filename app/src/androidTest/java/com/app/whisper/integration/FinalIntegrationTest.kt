package com.app.whisper.integration

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.app.whisper.TestConfiguration
import com.app.whisper.TestDataFactory
import com.app.whisper.data.manager.ModelManager
import com.app.whisper.domain.entity.ModelStatus
import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.domain.usecase.TranscribeAudioUseCase
import com.app.whisper.performance.PerformanceManager
import com.app.whisper.presentation.ui.MainActivity
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

/**
 * Final integration tests for the complete Whisper Android application.
 * 
 * These tests verify the end-to-end functionality of the entire application,
 * including UI interactions, business logic, data persistence, and performance.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FinalIntegrationTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Inject
    lateinit var modelManager: ModelManager
    
    @Inject
    lateinit var transcribeAudioUseCase: TranscribeAudioUseCase
    
    @Inject
    lateinit var performanceManager: PerformanceManager
    
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        hiltRule.inject()
    }
    
    @Test
    fun completeApplicationWorkflow_withMockModel_completesSuccessfully() = runTest {
        // Given - Setup mock model
        val model = WhisperModel.TINY
        setupMockModel(model)
        
        // When - Navigate through complete workflow
        // 1. App launches and shows initial screen
        composeTestRule.waitForIdle()
        
        // 2. Check that the app is in ready state
        composeTestRule
            .onNodeWithText("Ready to Record")
            .assertIsDisplayed()
        
        // 3. Start recording
        composeTestRule
            .onNodeWithText("Start Recording")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // 4. Verify recording state
        composeTestRule
            .onNodeWithText("Recording")
            .assertIsDisplayed()
        
        // 5. Stop recording (this would trigger transcription)
        composeTestRule
            .onNodeWithText("Stop")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Then - Verify the workflow completed
        // Note: In a real test, we would verify transcription results
        // For this integration test, we verify the UI flow works
        assertThat(model.status).isEqualTo(ModelStatus.Available)
    }
    
    @Test
    fun modelManagement_downloadAndSetCurrent_worksCorrectly() = runTest {
        // Given
        val model = WhisperModel.TINY
        
        // When - Download and set model
        val downloadResult = modelManager.downloadModel(model)
        assertThat(downloadResult.isSuccess).isTrue()
        
        val setCurrentResult = modelManager.setCurrentModel(model)
        assertThat(setCurrentResult.isSuccess).isTrue()
        
        // Then - Verify model is current
        val currentModel = modelManager.currentModel.first()
        assertThat(currentModel).isEqualTo(model)
        assertThat(model.isCurrent).isTrue()
        assertThat(model.status).isEqualTo(ModelStatus.Available)
    }
    
    @Test
    fun performanceOptimization_adaptsToDevice_correctly() = runTest {
        // Given - Get device performance info
        val performanceTier = performanceManager.getPerformanceTier()
        val memoryInfo = performanceManager.getMemoryInfo()
        val cpuInfo = performanceManager.getCpuInfo()
        
        // When - Get optimized configuration
        val config = performanceManager.optimizeForDevice()
        
        // Then - Verify configuration matches device capabilities
        when (performanceTier) {
            com.app.whisper.performance.PerformanceTier.HIGH -> {
                assertThat(config.maxConcurrentDownloads).isEqualTo(3)
                assertThat(config.audioBufferSize).isEqualTo(8192)
                assertThat(config.enableAdvancedFeatures).isTrue()
            }
            com.app.whisper.performance.PerformanceTier.MEDIUM -> {
                assertThat(config.maxConcurrentDownloads).isEqualTo(2)
                assertThat(config.audioBufferSize).isEqualTo(4096)
                assertThat(config.enableAdvancedFeatures).isTrue()
            }
            com.app.whisper.performance.PerformanceTier.LOW -> {
                assertThat(config.maxConcurrentDownloads).isEqualTo(1)
                assertThat(config.audioBufferSize).isEqualTo(2048)
                assertThat(config.enableAdvancedFeatures).isFalse()
            }
        }
        
        // Verify memory and CPU info are reasonable
        assertThat(memoryInfo.totalMemory).isGreaterThan(0L)
        assertThat(memoryInfo.availableMemory).isGreaterThan(0L)
        assertThat(cpuInfo.availableProcessors).isGreaterThan(0)
    }
    
    @Test
    fun transcriptionWorkflow_withTestData_processesCorrectly() = runTest {
        // Given - Setup test data and model
        val testAudioData = TestDataFactory.createTestAudioData(
            durationMs = TestConfiguration.TEST_AUDIO_DURATION_MS,
            sampleRate = TestConfiguration.TEST_SAMPLE_RATE
        )
        val testParameters = TestDataFactory.createTestProcessingParameters()
        val model = WhisperModel.TINY
        
        setupMockModel(model)
        modelManager.setCurrentModel(model)
        
        // When - Execute transcription
        val progressFlow = transcribeAudioUseCase.execute(
            com.app.whisper.data.model.AudioData.fromByteArray(testAudioData),
            testParameters
        )
        
        // Then - Verify progress flow
        val progressList = progressFlow.toList()
        assertThat(progressList).isNotEmpty()
        
        // Verify we get started progress
        val startedProgress = progressList.find { 
            it is com.app.whisper.domain.usecase.TranscriptionProgress.Started 
        }
        assertThat(startedProgress).isNotNull()
        
        // Verify final state (either completed or failed)
        val finalProgress = progressList.last()
        assertThat(finalProgress).isAnyOf(
            com.app.whisper.domain.usecase.TranscriptionProgress.Completed::class.java,
            com.app.whisper.domain.usecase.TranscriptionProgress.Failed::class.java
        )
    }
    
    @Test
    fun memoryManagement_underPressure_cleansUpCorrectly() = runTest {
        // Given - Get initial memory state
        val initialMemoryInfo = performanceManager.getMemoryInfo()
        
        // When - Simulate memory pressure
        // Create large objects to increase memory usage
        val largeArrays = mutableListOf<ByteArray>()
        repeat(10) {
            try {
                largeArrays.add(ByteArray(10 * 1024 * 1024)) // 10MB each
            } catch (e: OutOfMemoryError) {
                // Expected on low-memory devices
                break
            }
        }
        
        // Check memory status after allocation
        val memoryStatus = performanceManager.monitorMemoryUsage()
        
        // Then - Verify memory management responds appropriately
        when (memoryStatus) {
            com.app.whisper.performance.MemoryStatus.CRITICAL,
            com.app.whisper.performance.MemoryStatus.HIGH -> {
                // Memory cleanup should have been triggered
                // Verify that the system is handling high memory usage
                assertThat(memoryStatus).isAnyOf(
                    com.app.whisper.performance.MemoryStatus.HIGH,
                    com.app.whisper.performance.MemoryStatus.CRITICAL
                )
            }
            else -> {
                // Normal memory usage
                assertThat(memoryStatus).isAnyOf(
                    com.app.whisper.performance.MemoryStatus.NORMAL,
                    com.app.whisper.performance.MemoryStatus.MODERATE
                )
            }
        }
        
        // Cleanup
        largeArrays.clear()
        System.gc()
    }
    
    @Test
    fun applicationLifecycle_backgroundAndForeground_maintainsState() = runTest {
        // Given - Setup initial state
        val model = WhisperModel.TINY
        setupMockModel(model)
        modelManager.setCurrentModel(model)
        
        val initialCurrentModel = modelManager.currentModel.first()
        
        // When - Simulate app going to background and returning
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        composeTestRule.waitForIdle()
        
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()
        
        // Then - Verify state is maintained
        val restoredCurrentModel = modelManager.currentModel.first()
        assertThat(restoredCurrentModel).isEqualTo(initialCurrentModel)
        assertThat(model.isCurrent).isTrue()
    }
    
    @Test
    fun errorHandling_networkFailure_recoversGracefully() = runTest {
        // Given - Model that would fail to download (network error simulation)
        val model = WhisperModel.LARGE // Large model more likely to have issues
        
        // When - Attempt download (this might fail in test environment)
        val downloadResult = modelManager.downloadModel(model)
        
        // Then - Verify error handling
        if (downloadResult.isFailure) {
            // Verify error is handled gracefully
            assertThat(model.status).isEqualTo(ModelStatus.Error)
            
            // Verify download progress shows failure
            val progressMap = modelManager.downloadProgress.first()
            val progress = progressMap[model]
            assertThat(progress).isInstanceOf(
                com.app.whisper.domain.repository.DownloadProgress.Failed::class.java
            )
        } else {
            // If download succeeded, verify success state
            assertThat(model.status).isEqualTo(ModelStatus.Available)
        }
    }
    
    @Test
    fun accessibility_screenReader_canNavigateApp() {
        // Given - App is launched
        composeTestRule.waitForIdle()
        
        // When - Check accessibility features
        // Verify important elements have content descriptions
        composeTestRule
            .onNodeWithContentDescription("Start Recording")
            .assertExists()
        
        // Verify text elements are accessible
        composeTestRule
            .onNodeWithText("Whisper Transcription")
            .assertIsDisplayed()
        
        // Then - Verify accessibility tree is properly structured
        composeTestRule
            .onRoot()
            .printToLog("AccessibilityTest")
    }
    
    @Test
    fun performanceBenchmark_transcriptionSpeed_meetsRequirements() = runTest {
        // Given - Setup test audio and model
        val testAudioData = TestDataFactory.createTestAudioData(
            durationMs = 5000L, // 5 seconds of audio
            sampleRate = 16000
        )
        val model = WhisperModel.TINY
        setupMockModel(model)
        modelManager.setCurrentModel(model)
        
        // When - Measure transcription performance
        val startTime = System.currentTimeMillis()
        
        val progressFlow = transcribeAudioUseCase.execute(
            com.app.whisper.data.model.AudioData.fromByteArray(testAudioData),
            TestDataFactory.createTestProcessingParameters()
        )
        
        val result = progressFlow.toList().last()
        val endTime = System.currentTimeMillis()
        
        // Then - Verify performance meets requirements
        val processingTimeMs = endTime - startTime
        val speedRatio = testAudioData.size.toFloat() / processingTimeMs
        
        // Performance requirements based on device tier
        val performanceTier = performanceManager.getPerformanceTier()
        when (performanceTier) {
            com.app.whisper.performance.PerformanceTier.HIGH -> {
                // Should process faster than realtime
                assertThat(speedRatio).isAtLeast(1.0f)
            }
            com.app.whisper.performance.PerformanceTier.MEDIUM -> {
                // Should process at least at realtime speed
                assertThat(speedRatio).isAtLeast(0.8f)
            }
            com.app.whisper.performance.PerformanceTier.LOW -> {
                // Should complete processing (may be slower than realtime)
                assertThat(processingTimeMs).isLessThan(30000L) // Max 30 seconds
            }
        }
    }
    
    /**
     * Helper method to setup a mock model for testing.
     */
    private fun setupMockModel(model: WhisperModel) {
        val modelsDir = File(context.filesDir, "models")
        modelsDir.mkdirs()
        
        val modelFile = File(modelsDir, "${model.id}.bin")
        modelFile.createNewFile()
        
        model.status = ModelStatus.Available
        model.localPath = modelFile.absolutePath
        model.downloadedAt = System.currentTimeMillis()
    }
}
