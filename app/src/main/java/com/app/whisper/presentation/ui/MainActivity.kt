package com.app.whisper.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.whisper.presentation.theme.WhisperAndroidTheme
import com.app.whisper.presentation.ui.screen.RecordingScreen
import com.app.whisper.presentation.viewmodel.TranscriptionViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the Whisper Android application.
 *
 * This activity serves as the entry point for the UI and hosts the main
 * transcription screen using Jetpack Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val transcriptionViewModel: TranscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WhisperAndroidTheme {
                val uiState by transcriptionViewModel.uiState.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RecordingScreen(
                        uiState = uiState,
                        onStartRecording = { transcriptionViewModel.startRecording() },
                        onStopRecording = { transcriptionViewModel.stopRecording() },
                        onPauseRecording = { transcriptionViewModel.pauseRecording() },
                        onResumeRecording = { transcriptionViewModel.resumeRecording() },
                        onClearResult = { transcriptionViewModel.clearResult() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        // Placeholder content - will be replaced with actual TranscriptionScreen
        androidx.compose.material3.Text(
            text = "Whisper Android - Coming Soon!",
            modifier = Modifier.padding(paddingValues),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    WhisperAndroidTheme {
        PlaceholderScreen()
    }
}
