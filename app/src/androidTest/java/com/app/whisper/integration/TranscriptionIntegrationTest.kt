package com.app.whisper.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.whisper.TestConfiguration
import com.app.whisper.TestDataFactory
import com.app.whisper.data.manager.ModelManager
import com.app.whisper.domain.entity.ModelStatus
import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.domain.usecase.TranscribeAudioUseCase
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
 * Integration tests for the complete transcription workflow.
 * 
 * These tests verify the end-to-end functionality of the transcription
 * system including model management, audio processing, and result generation.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TranscriptionIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var modelManager: ModelManager
    
    @Inject
    lateinit var transcribeAudioUseCase: TranscribeAudioUseCase
    
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        hiltRule.inject()
    }
    
    @Test
    fun modelManager_downloadAndSetCurrentModel_completesSuccessfully() = runTest {
        // Given
        val model = WhisperModel.TINY
        
        // Create a mock model file for testing
        val modelsDir = File(context.filesDir, "models")
        modelsDir.mkdirs()
        val modelFile = File(modelsDir, "${model.id}.bin")
        modelFile.createNewFile()
        
        // Simulate model being available
        model.status = ModelStatus.Available
        model.localPath = modelFile.absolutePath
        
        // When
        val result = modelManager.setCurrentModel(model)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        
        val currentModel = modelManager.currentModel.first()
        assertThat(currentModel).isEqualTo(model)
        assertThat(model.isCurrent).isTrue()
        
        // Cleanup
        modelFile.delete()
    }
    
    @Test
    fun modelManager_getStorageInfo_returnsCorrectInformation() = runTest {
        // Given - Create some mock model files
        val modelsDir = File(context.filesDir, "models")
        modelsDir.mkdirs()
        
        val model1File = File(modelsDir, "${WhisperModel.TINY.id}.bin")
        val model2File = File(modelsDir, "${WhisperModel.BASE.id}.bin")
        model1File.createNewFile()
        model2File.createNewFile()
        
        // Set models as available
        WhisperModel.TINY.status = ModelStatus.Available
        WhisperModel.BASE.status = ModelStatus.Available
        
        // When
        val storageInfo = modelManager.getStorageInfo()
        
        // Then
        assertThat(storageInfo.downloadedModelsCount).isEqualTo(2)
        assertThat(storageInfo.totalModelsCount).isEqualTo(WhisperModel.values().size)
        assertThat(storageInfo.availableSpace).isGreaterThan(0L)
        
        // Cleanup
        model1File.delete()
        model2File.delete()
        WhisperModel.TINY.status = ModelStatus.NotDownloaded
        WhisperModel.BASE.status = ModelStatus.NotDownloaded
    }
    
    @Test
    fun transcriptionWorkflow_withMockData_completesSuccessfully() = runTest {
        // Given
        val testAudioData = TestDataFactory.createTestAudioData()
        val testParameters = TestDataFactory.createTestProcessingParameters()
        
        // Setup a mock model as current
        val model = WhisperModel.TINY
        val modelsDir = File(context.filesDir, "models")
        modelsDir.mkdirs()
        val modelFile = File(modelsDir, "${model.id}.bin")
        modelFile.createNewFile()
        
        model.status = ModelStatus.Available
        model.localPath = modelFile.absolutePath
        modelManager.setCurrentModel(model)
        
        // When
        val progressFlow = transcribeAudioUseCase.execute(
            com.app.whisper.data.model.AudioData.fromByteArray(testAudioData),
            testParameters
        )
        
        // Then
        val progressList = progressFlow.toList()
        assertThat(progressList).isNotEmpty()
        
        // Verify we get started progress
        val startedProgress = progressList.find { 
            it is com.app.whisper.domain.usecase.TranscriptionProgress.Started 
        }
        assertThat(startedProgress).isNotNull()
        
        // Cleanup
        modelFile.delete()
        model.status = ModelStatus.NotDownloaded
        model.localPath = null
        model.isCurrent = false
    }
    
    @Test
    fun modelManager_deleteModel_removesFileAndUpdatesStatus() = runTest {
        // Given
        val model = WhisperModel.TINY
        val modelsDir = File(context.filesDir, "models")
        modelsDir.mkdirs()
        val modelFile = File(modelsDir, "${model.id}.bin")
        modelFile.createNewFile()
        
        model.status = ModelStatus.Available
        model.localPath = modelFile.absolutePath
        model.downloadedAt = System.currentTimeMillis()
        
        // When
        val result = modelManager.deleteModel(model)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(modelFile.exists()).isFalse()
        assertThat(model.status).isEqualTo(ModelStatus.NotDownloaded)
        assertThat(model.localPath).isNull()
        assertThat(model.downloadedAt).isNull()
    }
    
    @Test
    fun modelManager_getAllModels_returnsAllAvailableModels() {
        // When
        val allModels = modelManager.getAllModels()
        
        // Then
        assertThat(allModels).hasSize(WhisperModel.values().size)
        assertThat(allModels).containsExactlyElementsIn(WhisperModel.values())
    }
    
    @Test
    fun modelManager_getDownloadedModels_returnsOnlyAvailableModels() = runTest {
        // Given - Setup one available model
        val model = WhisperModel.TINY
        val modelsDir = File(context.filesDir, "models")
        modelsDir.mkdirs()
        val modelFile = File(modelsDir, "${model.id}.bin")
        modelFile.createNewFile()
        
        model.status = ModelStatus.Available
        model.localPath = modelFile.absolutePath
        
        // When
        val downloadedModels = modelManager.getDownloadedModels()
        
        // Then
        assertThat(downloadedModels).hasSize(1)
        assertThat(downloadedModels).contains(model)
        
        // Cleanup
        modelFile.delete()
        model.status = ModelStatus.NotDownloaded
        model.localPath = null
    }
    
    @Test
    fun transcriptionUseCase_withoutCurrentModel_fails() = runTest {
        // Given - No current model set
        val testAudioData = TestDataFactory.createTestAudioData()
        val testParameters = TestDataFactory.createTestProcessingParameters()
        
        // When
        val progressFlow = transcribeAudioUseCase.execute(
            com.app.whisper.data.model.AudioData.fromByteArray(testAudioData),
            testParameters
        )
        
        // Then
        val progressList = progressFlow.toList()
        
        // Should get a failure progress
        val failedProgress = progressList.find { 
            it is com.app.whisper.domain.usecase.TranscriptionProgress.Failed 
        }
        assertThat(failedProgress).isNotNull()
    }
    
    @Test
    fun modelManager_setCurrentModel_withUnavailableModel_fails() = runTest {
        // Given - Model without file
        val model = WhisperModel.TINY
        model.status = ModelStatus.NotDownloaded
        model.localPath = null
        
        // When
        val result = modelManager.setCurrentModel(model)
        
        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        
        val currentModel = modelManager.currentModel.first()
        assertThat(currentModel).isNotEqualTo(model)
    }
}
