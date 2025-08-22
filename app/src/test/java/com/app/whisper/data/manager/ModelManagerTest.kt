package com.app.whisper.data.manager

import android.content.Context
import android.content.SharedPreferences
import com.app.whisper.CoroutineTestRule
import com.app.whisper.TestConfiguration
import com.app.whisper.TestDataFactory
import com.app.whisper.domain.entity.ModelStatus
import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.domain.repository.DownloadProgress
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Unit tests for ModelManager.
 * 
 * Tests model download, storage, validation, and lifecycle management.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ModelManagerTest {
    
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()
    
    private lateinit var modelManager: ModelManager
    private lateinit var context: Context
    private lateinit var mockPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var httpClient: OkHttpClient
    private lateinit var mockWebServer: MockWebServer
    private lateinit var modelsDir: File
    
    @Before
    fun setUp() {
        // Setup context
        context = RuntimeEnvironment.getApplication()
        
        // Setup mock preferences
        mockPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        every { mockPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        
        // Setup mock web server
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        // Setup HTTP client
        httpClient = OkHttpClient.Builder()
            .build()
        
        // Setup models directory
        modelsDir = File(context.filesDir, "models")
        if (modelsDir.exists()) {
            modelsDir.deleteRecursively()
        }
        modelsDir.mkdirs()
        
        // Create ModelManager instance
        modelManager = ModelManager(context, httpClient, mockPreferences)
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
        if (modelsDir.exists()) {
            modelsDir.deleteRecursively()
        }
    }
    
    @Test
    fun `getAllModels returns all available models`() {
        // When
        val models = modelManager.getAllModels()
        
        // Then
        assertThat(models).isNotEmpty()
        assertThat(models).containsExactlyElementsIn(WhisperModel.values())
    }
    
    @Test
    fun `getDownloadedModels returns only available models`() {
        // Given - Create a fake model file
        val modelFile = File(modelsDir, "${WhisperModel.TINY.id}.bin")
        modelFile.createNewFile()
        WhisperModel.TINY.status = ModelStatus.Available
        WhisperModel.TINY.localPath = modelFile.absolutePath
        
        // When
        val downloadedModels = modelManager.getDownloadedModels()
        
        // Then
        assertThat(downloadedModels).hasSize(1)
        assertThat(downloadedModels.first()).isEqualTo(WhisperModel.TINY)
    }
    
    @Test
    fun `setCurrentModel succeeds for available model`() = runTest {
        // Given - Setup available model
        val model = WhisperModel.TINY
        val modelFile = File(modelsDir, "${model.id}.bin")
        modelFile.createNewFile()
        model.status = ModelStatus.Available
        model.localPath = modelFile.absolutePath
        
        // When
        val result = modelManager.setCurrentModel(model)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(modelManager.currentModel.first()).isEqualTo(model)
        assertThat(model.isCurrent).isTrue()
        
        // Verify preferences were updated
        verify { mockEditor.putString("current_model", model.id) }
        verify { mockEditor.putLong("${model.id}_last_used_at", any()) }
    }
    
    @Test
    fun `setCurrentModel fails for unavailable model`() = runTest {
        // Given - Model without file
        val model = WhisperModel.TINY
        model.status = ModelStatus.NotDownloaded
        model.localPath = null
        
        // When
        val result = modelManager.setCurrentModel(model)
        
        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }
    
    @Test
    fun `downloadModel succeeds with valid response`() = runTest {
        // Given - Setup mock server response
        val testData = TestDataFactory.createTestAudioData()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(okio.Buffer().write(testData))
                .setHeader("Content-Length", testData.size.toString())
        )
        
        val model = WhisperModel.TINY
        // Update model URL to point to mock server
        val testUrl = mockWebServer.url("/test-model.bin").toString()
        
        // When
        val result = modelManager.downloadModel(model)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        
        // Verify download progress was tracked
        val progressMap = modelManager.downloadProgress.first()
        assertThat(progressMap).containsKey(model)
        
        // Verify model status was updated
        assertThat(model.status).isEqualTo(ModelStatus.Available)
        assertThat(model.localPath).isNotNull()
        
        // Verify file was created
        val modelFile = File(model.localPath!!)
        assertThat(modelFile.exists()).isTrue()
        assertThat(modelFile.length()).isEqualTo(testData.size.toLong())
    }
    
    @Test
    fun `downloadModel fails with network error`() = runTest {
        // Given - Setup mock server error response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("Not Found")
        )
        
        val model = WhisperModel.TINY
        
        // When
        val result = modelManager.downloadModel(model)
        
        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(model.status).isEqualTo(ModelStatus.Error)
        
        // Verify progress shows failure
        val progressMap = modelManager.downloadProgress.first()
        val progress = progressMap[model]
        assertThat(progress).isInstanceOf(DownloadProgress.Failed::class.java)
    }
    
    @Test
    fun `deleteModel removes file and updates status`() = runTest {
        // Given - Setup downloaded model
        val model = WhisperModel.TINY
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
        
        // Verify preferences were cleared
        verify { mockEditor.remove("${model.id}_downloaded_at") }
        verify { mockEditor.remove("${model.id}_last_used_at") }
    }
    
    @Test
    fun `deleteModel clears current model if it was current`() = runTest {
        // Given - Setup current model
        val model = WhisperModel.TINY
        val modelFile = File(modelsDir, "${model.id}.bin")
        modelFile.createNewFile()
        model.status = ModelStatus.Available
        model.localPath = modelFile.absolutePath
        model.isCurrent = true
        
        // When
        val result = modelManager.deleteModel(model)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(model.isCurrent).isFalse()
        assertThat(modelManager.currentModel.first()).isNull()
        
        // Verify current model preference was cleared
        verify { mockEditor.remove("current_model") }
    }
    
    @Test
    fun `getStorageInfo returns correct information`() {
        // Given - Setup some downloaded models
        val model1 = WhisperModel.TINY
        val model2 = WhisperModel.BASE
        
        val file1 = File(modelsDir, "${model1.id}.bin")
        val file2 = File(modelsDir, "${model2.id}.bin")
        file1.createNewFile()
        file2.createNewFile()
        
        model1.status = ModelStatus.Available
        model2.status = ModelStatus.Available
        
        // When
        val storageInfo = modelManager.getStorageInfo()
        
        // Then
        assertThat(storageInfo.downloadedModelsCount).isEqualTo(2)
        assertThat(storageInfo.totalModelsCount).isEqualTo(WhisperModel.values().size)
        assertThat(storageInfo.totalDownloadedSize).isEqualTo(model1.fileSizeBytes + model2.fileSizeBytes)
        assertThat(storageInfo.availableSpace).isGreaterThan(0L)
    }
    
    @Test
    fun `cancelDownload stops ongoing download`() = runTest {
        // Given - Model in downloading state
        val model = WhisperModel.TINY
        model.status = ModelStatus.Downloading
        
        // Create temp file to simulate ongoing download
        val tempFile = File(modelsDir, "${model.id}.bin.tmp")
        tempFile.createNewFile()
        
        // When
        val result = modelManager.cancelDownload(model)
        
        // Then
        assertThat(result.isSuccess).isTrue()
        assertThat(model.status).isEqualTo(ModelStatus.NotDownloaded)
        assertThat(tempFile.exists()).isFalse()
        
        // Verify progress was cleared
        val progressMap = modelManager.downloadProgress.first()
        assertThat(progressMap).doesNotContainKey(model)
    }
    
    @Test
    fun `cancelDownload fails for non-downloading model`() = runTest {
        // Given - Model not in downloading state
        val model = WhisperModel.TINY
        model.status = ModelStatus.Available
        
        // When
        val result = modelManager.cancelDownload(model)
        
        // Then
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("not being downloaded")
    }
}
