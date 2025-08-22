package com.app.whisper.presentation.ui.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.whisper.TestDataFactory
import com.app.whisper.data.model.RecordingState
import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.presentation.permission.PermissionState
import com.app.whisper.presentation.state.TranscriptionUiState
import com.app.whisper.presentation.theme.WhisperAndroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for RecordingScreen Compose UI.
 * 
 * Tests user interactions, UI state changes, and accessibility features.
 */
@RunWith(AndroidJUnit4::class)
class RecordingScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun recordingScreen_initialState_displaysCorrectly() {
        // Given
        val initialState = TranscriptionUiState.Initial
        
        // When
        composeTestRule.setContent {
            WhisperAndroidTheme {
                RecordingScreen(
                    uiState = initialState,
                    onStartRecording = { },
                    onStopRecording = { },
                    onPauseRecording = { },
                    onResumeRecording = { },
                    onClearResult = { }
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Whisper Transcription")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Initializing")
            .assertIsDisplayed()
    }
    
    @Test
    fun recordingScreen_readyState_showsRecordButton() {
        // Given
        val readyState = TranscriptionUiState.Ready(
            currentModel = WhisperModel.TINY,
            recordingState = RecordingState.IDLE,
            canRecord = true
        )
        
        // When
        composeTestRule.setContent {
            WhisperAndroidTheme {
                RecordingScreen(
                    uiState = readyState,
                    onStartRecording = { },
                    onStopRecording = { },
                    onPauseRecording = { },
                    onResumeRecording = { },
                    onClearResult = { }
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Start Recording")
            .assertIsDisplayed()
            .assertIsEnabled()
        
        composeTestRule
            .onNodeWithText("Ready to Record")
            .assertIsDisplayed()
    }
    
    @Test
    fun recordingScreen_recordingState_showsStopAndPauseButtons() {
        // Given
        val recordingState = TranscriptionUiState.Recording(
            recordingState = RecordingState.RECORDING,
            duration = 5000L,
            audioLevel = 0.5f,
            waveformData = null,
            canPause = true,
            canStop = true
        )
        
        // When
        composeTestRule.setContent {
            WhisperAndroidTheme {
                RecordingScreen(
                    uiState = recordingState,
                    onStartRecording = { },
                    onStopRecording = { },
                    onPauseRecording = { },
                    onResumeRecording = { },
                    onClearResult = { }
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Pause")
            .assertIsDisplayed()
            .assertIsEnabled()
        
        composeTestRule
            .onNodeWithText("Stop")
            .assertIsDisplayed()
            .assertIsEnabled()
        
        composeTestRule
            .onNodeWithText("Recording")
            .assertIsDisplayed()
    }
    
    @Test
    fun recordingScreen_processingState_showsProgressIndicator() {
        // Given
        val processingState = TranscriptionUiState.Processing(
            progress = com.app.whisper.domain.usecase.TranscriptionProgress.Processing,
            audioData = null
        )
        
        // When
        composeTestRule.setContent {
            WhisperAndroidTheme {
                RecordingScreen(
                    uiState = processingState,
                    onStartRecording = { },
                    onStopRecording = { },
                    onPauseRecording = { },
                    onResumeRecording = { },
                    onClearResult = { }
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Processing")
            .assertIsDisplayed()
        
        // Check for progress indicator
        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
    }
    
    @Test
    fun recordingScreen_successState_showsTranscriptionResult() {
        // Given
        val testResult = TestDataFactory.createTestTranscriptionResult(
            text = "This is a test transcription result."
        )
        val successState = TranscriptionUiState.Success(
            result = testResult,
            processingTimeMs = 2000L
        )
        
        // When
        composeTestRule.setContent {
            WhisperAndroidTheme {
                RecordingScreen(
                    uiState = successState,
                    onStartRecording = { },
                    onStopRecording = { },
                    onPauseRecording = { },
                    onResumeRecording = { },
                    onClearResult = { }
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Transcription Result")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("This is a test transcription result.")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Clear")
            .assertIsDisplayed()
            .assertIsEnabled()
        
        composeTestRule
            .onNodeWithText("Language: en")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Confidence: 95.0%")
            .assertIsDisplayed()
    }
    
    @Test
    fun recordingScreen_errorState_showsErrorMessage() {
        // Given
        val testError = RuntimeException("Test error message")
        val errorState = TranscriptionUiState.Error(
            error = testError,
            canRetry = true
        )
        
        // When
        composeTestRule.setContent {
            WhisperAndroidTheme {
                RecordingScreen(
                    uiState = errorState,
                    onStartRecording = { },
                    onStopRecording = { },
                    onPauseRecording = { },
                    onResumeRecording = { },
                    onClearResult = { }
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Error")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Test error message")
            .assertIsDisplayed()
    }
    
    @Test
    fun recordingScreen_permissionDenied_showsPermissionRequest() {
        // Given
        val readyState = TranscriptionUiState.Ready(
            currentModel = WhisperModel.TINY,
            recordingState = RecordingState.IDLE,
            canRecord = false
        )
        val permissionState = PermissionState.Denied
        
        // When
        composeTestRule.setContent {
            WhisperAndroidTheme {
                RecordingScreen(
                    uiState = readyState,
                    onStartRecording = { },
                    onStopRecording = { },
                    onPauseRecording = { },
                    onResumeRecording = { },
                    onClearResult = { },
                    permissionState = permissionState,
                    onRequestPermission = { }
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Microphone Access Required")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Allow")
            .assertIsDisplayed()
            .assertIsEnabled()
    }
    
    @Test
    fun recordingScreen_startRecordingButton_triggersCallback() {
        // Given
        var startRecordingCalled = false
        val readyState = TranscriptionUiState.Ready(
            currentModel = WhisperModel.TINY,
            recordingState = RecordingState.IDLE,
            canRecord = true
        )
        
        // When
        composeTestRule.setContent {
            WhisperAndroidTheme {
                RecordingScreen(
                    uiState = readyState,
                    onStartRecording = { startRecordingCalled = true },
                    onStopRecording = { },
                    onPauseRecording = { },
                    onResumeRecording = { },
                    onClearResult = { }
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Start Recording")
            .performClick()
        
        assert(startRecordingCalled) { "Start recording callback should be called" }
    }
    
    @Test
    fun recordingScreen_clearResultButton_triggersCallback() {
        // Given
        var clearResultCalled = false
        val testResult = TestDataFactory.createTestTranscriptionResult()
        val successState = TranscriptionUiState.Success(
            result = testResult,
            processingTimeMs = 2000L
        )
        
        // When
        composeTestRule.setContent {
            WhisperAndroidTheme {
                RecordingScreen(
                    uiState = successState,
                    onStartRecording = { },
                    onStopRecording = { },
                    onPauseRecording = { },
                    onResumeRecording = { },
                    onClearResult = { clearResultCalled = true }
                )
            }
        }
        
        // Then
        composeTestRule
            .onNodeWithText("Clear")
            .performClick()
        
        assert(clearResultCalled) { "Clear result callback should be called" }
    }
    
    @Test
    fun recordingScreen_accessibility_hasCorrectContentDescriptions() {
        // Given
        val readyState = TranscriptionUiState.Ready(
            currentModel = WhisperModel.TINY,
            recordingState = RecordingState.IDLE,
            canRecord = true
        )
        
        // When
        composeTestRule.setContent {
            WhisperAndroidTheme {
                RecordingScreen(
                    uiState = readyState,
                    onStartRecording = { },
                    onStopRecording = { },
                    onPauseRecording = { },
                    onResumeRecording = { },
                    onClearResult = { }
                )
            }
        }
        
        // Then - Check that important elements have content descriptions
        composeTestRule
            .onNodeWithContentDescription("Start Recording")
            .assertExists()
    }
}
