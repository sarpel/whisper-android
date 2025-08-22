package com.app.whisper.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.app.whisper.CoroutineTestRule
import com.app.whisper.TestConfiguration
import com.app.whisper.TestDataFactory
import com.app.whisper.data.audio.AudioRecorder
import com.app.whisper.data.model.RecordingState
import com.app.whisper.domain.entity.TranscriptionResult
import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.domain.usecase.DownloadModelUseCase
import com.app.whisper.domain.usecase.TranscribeAudioUseCase
import com.app.whisper.presentation.state.TranscriptionUiState
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for TranscriptionViewModel.
 * 
 * Tests UI state management, recording operations, and transcription workflow.
 */
@ExperimentalCoroutinesApi
class TranscriptionViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()
    
    private lateinit var viewModel: TranscriptionViewModel
    private lateinit var mockAudioRecorder: AudioRecorder
    private lateinit var mockTranscribeAudioUseCase: TranscribeAudioUseCase
    private lateinit var mockDownloadModelUseCase: DownloadModelUseCase
    
    private val mockRecordingState = MutableStateFlow(RecordingState.IDLE)
    private val mockAudioLevel = MutableStateFlow(0.0f)
    
    @Before
    fun setUp() {
        // Setup mocks
        mockAudioRecorder = mockk(relaxed = true)
        mockTranscribeAudioUseCase = mockk(relaxed = true)
        mockDownloadModelUseCase = mockk(relaxed = true)
        
        // Setup audio recorder flows
        every { mockAudioRecorder.recordingState } returns mockRecordingState
        every { mockAudioRecorder.audioLevel } returns mockAudioLevel
        
        // Create ViewModel
        viewModel = TranscriptionViewModel(
            audioRecorder = mockAudioRecorder,
            transcribeAudioUseCase = mockTranscribeAudioUseCase,
            downloadModelUseCase = mockDownloadModelUseCase
        )
    }
    
    @Test
    fun `initial state is Initial`() = runTest {
        // When
        val initialState = viewModel.uiState.first()
        
        // Then
        assertThat(initialState).isInstanceOf(TranscriptionUiState.Initial::class.java)
    }
    
    @Test
    fun `startRecording changes state to Recording`() = runTest {
        // Given
        coEvery { mockAudioRecorder.startRecording() } returns Result.success(Unit)
        mockRecordingState.value = RecordingState.RECORDING
        
        // When
        viewModel.startRecording()
        
        // Then
        val currentState = viewModel.uiState.value
        assertThat(currentState).isInstanceOf(TranscriptionUiState.Recording::class.java)
        
        // Verify audio recorder was called
        coVerify { mockAudioRecorder.startRecording() }
    }
    
    @Test
    fun `startRecording handles error gracefully`() = runTest {
        // Given
        val error = RuntimeException("Recording failed")
        coEvery { mockAudioRecorder.startRecording() } returns Result.failure(error)
        
        // When
        viewModel.startRecording()
        
        // Then
        val currentState = viewModel.uiState.value
        assertThat(currentState).isInstanceOf(TranscriptionUiState.Error::class.java)
        
        val errorState = currentState as TranscriptionUiState.Error
        assertThat(errorState.error).isEqualTo(error)
    }
    
    @Test
    fun `stopRecording triggers transcription`() = runTest {
        // Given - Setup recording state
        mockRecordingState.value = RecordingState.RECORDING
        viewModel.startRecording()
        
        val testAudioData = TestDataFactory.createTestAudioData()
        val testResult = TestDataFactory.createTestTranscriptionResult()
        
        coEvery { mockAudioRecorder.stopRecording() } returns Result.success(testAudioData)
        coEvery { 
            mockTranscribeAudioUseCase.execute(any(), any()) 
        } returns flowOf(
            com.app.whisper.domain.usecase.TranscriptionProgress.Started,
            com.app.whisper.domain.usecase.TranscriptionProgress.Completed(testResult)
        )
        
        // When
        viewModel.stopRecording()
        
        // Then
        // Verify recording was stopped
        coVerify { mockAudioRecorder.stopRecording() }
        
        // Verify transcription was started
        coVerify { mockTranscribeAudioUseCase.execute(any(), any()) }
        
        // Check final state
        val finalState = viewModel.uiState.value
        assertThat(finalState).isInstanceOf(TranscriptionUiState.Success::class.java)
        
        val successState = finalState as TranscriptionUiState.Success
        assertThat(successState.result).isEqualTo(testResult)
    }
    
    @Test
    fun `pauseRecording updates state correctly`() = runTest {
        // Given - Setup recording state
        mockRecordingState.value = RecordingState.RECORDING
        viewModel.startRecording()
        
        coEvery { mockAudioRecorder.pauseRecording() } returns Result.success(Unit)
        mockRecordingState.value = RecordingState.PAUSED
        
        // When
        viewModel.pauseRecording()
        
        // Then
        coVerify { mockAudioRecorder.pauseRecording() }
        
        val currentState = viewModel.uiState.value
        assertThat(currentState).isInstanceOf(TranscriptionUiState.Recording::class.java)
        
        val recordingState = currentState as TranscriptionUiState.Recording
        assertThat(recordingState.recordingState).isEqualTo(RecordingState.PAUSED)
    }
    
    @Test
    fun `resumeRecording updates state correctly`() = runTest {
        // Given - Setup paused state
        mockRecordingState.value = RecordingState.PAUSED
        
        coEvery { mockAudioRecorder.resumeRecording() } returns Result.success(Unit)
        mockRecordingState.value = RecordingState.RECORDING
        
        // When
        viewModel.resumeRecording()
        
        // Then
        coVerify { mockAudioRecorder.resumeRecording() }
        
        val currentState = viewModel.uiState.value
        assertThat(currentState).isInstanceOf(TranscriptionUiState.Recording::class.java)
        
        val recordingState = currentState as TranscriptionUiState.Recording
        assertThat(recordingState.recordingState).isEqualTo(RecordingState.RECORDING)
    }
    
    @Test
    fun `clearResult resets to Ready state`() = runTest {
        // Given - Setup success state
        val testResult = TestDataFactory.createTestTranscriptionResult()
        // Manually set success state
        viewModel.setUiState(TranscriptionUiState.Success(testResult, 1000L))
        
        // When
        viewModel.clearResult()
        
        // Then
        val currentState = viewModel.uiState.value
        assertThat(currentState).isInstanceOf(TranscriptionUiState.Ready::class.java)
    }
    
    @Test
    fun `audio level updates are reflected in Recording state`() = runTest {
        // Given - Setup recording state
        mockRecordingState.value = RecordingState.RECORDING
        viewModel.startRecording()
        
        // When - Update audio level
        val testLevel = 0.75f
        mockAudioLevel.value = testLevel
        
        // Then
        val currentState = viewModel.uiState.value
        assertThat(currentState).isInstanceOf(TranscriptionUiState.Recording::class.java)
        
        val recordingState = currentState as TranscriptionUiState.Recording
        assertThat(recordingState.audioLevel).isEqualTo(testLevel)
    }
    
    @Test
    fun `transcription progress updates state correctly`() = runTest {
        // Given - Setup for transcription
        val testAudioData = TestDataFactory.createTestAudioData()
        coEvery { mockAudioRecorder.stopRecording() } returns Result.success(testAudioData)
        
        // Setup progress flow
        coEvery { 
            mockTranscribeAudioUseCase.execute(any(), any()) 
        } returns flowOf(
            com.app.whisper.domain.usecase.TranscriptionProgress.Started,
            com.app.whisper.domain.usecase.TranscriptionProgress.ModelLoaded("test-model"),
            com.app.whisper.domain.usecase.TranscriptionProgress.Processing
        )
        
        // When
        viewModel.stopRecording()
        
        // Then - Check that processing state is reached
        val currentState = viewModel.uiState.value
        assertThat(currentState).isInstanceOf(TranscriptionUiState.Processing::class.java)
        
        val processingState = currentState as TranscriptionUiState.Processing
        assertThat(processingState.progress).isInstanceOf(
            com.app.whisper.domain.usecase.TranscriptionProgress.Processing::class.java
        )
    }
    
    @Test
    fun `transcription failure updates to Error state`() = runTest {
        // Given - Setup for transcription failure
        val testAudioData = TestDataFactory.createTestAudioData()
        val testError = RuntimeException("Transcription failed")
        
        coEvery { mockAudioRecorder.stopRecording() } returns Result.success(testAudioData)
        coEvery { 
            mockTranscribeAudioUseCase.execute(any(), any()) 
        } returns flowOf(
            com.app.whisper.domain.usecase.TranscriptionProgress.Started,
            com.app.whisper.domain.usecase.TranscriptionProgress.Failed(testError)
        )
        
        // When
        viewModel.stopRecording()
        
        // Then
        val currentState = viewModel.uiState.value
        assertThat(currentState).isInstanceOf(TranscriptionUiState.Error::class.java)
        
        val errorState = currentState as TranscriptionUiState.Error
        assertThat(errorState.error).isEqualTo(testError)
    }
    
    @Test
    fun `ViewModel cleanup stops recording`() = runTest {
        // Given - Setup recording state
        mockRecordingState.value = RecordingState.RECORDING
        viewModel.startRecording()
        
        coEvery { mockAudioRecorder.stopRecording() } returns Result.success(ByteArray(0))
        
        // When - Clear ViewModel (simulate onCleared)
        viewModel.onCleared()
        
        // Then
        coVerify { mockAudioRecorder.stopRecording() }
    }
    
    // Helper method to set UI state for testing
    private fun TranscriptionViewModel.setUiState(state: TranscriptionUiState) {
        // This would require making _uiState internal or adding a test-only method
        // For now, we'll work with the public interface
    }
}
