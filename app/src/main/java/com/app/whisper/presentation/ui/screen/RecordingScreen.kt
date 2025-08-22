package com.app.whisper.presentation.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.whisper.data.model.RecordingState
import com.app.whisper.data.model.WaveformData
import com.app.whisper.presentation.state.TranscriptionUiState
import com.app.whisper.presentation.ui.component.WaveformVisualizer
import com.app.whisper.presentation.ui.component.RecordingButton
import com.app.whisper.presentation.ui.component.StatusCard

/**
 * Main recording screen for audio capture and transcription.
 * 
 * This screen provides the primary interface for recording audio,
 * visualizing waveforms, and displaying transcription results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    uiState: TranscriptionUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Whisper Transcription",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Status Card
        StatusCard(
            uiState = uiState,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Waveform Visualizer
        WaveformSection(
            uiState = uiState,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Recording Controls
        RecordingControls(
            uiState = uiState,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onPauseRecording = onPauseRecording,
            onResumeRecording = onResumeRecording,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Transcription Result
        TranscriptionResultSection(
            uiState = uiState,
            onClearResult = onClearResult,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

/**
 * Waveform visualization section.
 */
@Composable
private fun WaveformSection(
    uiState: TranscriptionUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is TranscriptionUiState.Recording -> {
                    WaveformVisualizer(
                        waveformData = uiState.waveformData,
                        isActive = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is TranscriptionUiState.Processing -> {
                    WaveformVisualizer(
                        waveformData = uiState.audioData?.let { 
                            WaveformData.fromAudioData(it) 
                        },
                        isActive = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Text(
                        text = "Audio waveform will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Recording control buttons section.
 */
@Composable
private fun RecordingControls(
    uiState: TranscriptionUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (uiState) {
            is TranscriptionUiState.Ready -> {
                RecordingButton(
                    onClick = onStartRecording,
                    enabled = uiState.canRecord,
                    icon = Icons.Default.Mic,
                    text = "Start Recording",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            is TranscriptionUiState.Recording -> {
                // Pause button
                RecordingButton(
                    onClick = onPauseRecording,
                    enabled = uiState.canPause,
                    icon = Icons.Default.Pause,
                    text = "Pause",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(end = 16.dp)
                )
                
                // Stop button
                RecordingButton(
                    onClick = onStopRecording,
                    enabled = uiState.canStop,
                    icon = Icons.Default.Stop,
                    text = "Stop",
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            is TranscriptionUiState.Processing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            else -> {
                // Show disabled record button
                RecordingButton(
                    onClick = { },
                    enabled = false,
                    icon = Icons.Default.Mic,
                    text = "Record",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Transcription result display section.
 */
@Composable
private fun TranscriptionResultSection(
    uiState: TranscriptionUiState,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is TranscriptionUiState.Success -> {
            Card(
                modifier = modifier,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transcription Result",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        TextButton(onClick = onClearResult) {
                            Text("Clear")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Result text
                    Text(
                        text = uiState.result.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Metadata
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Language: ${uiState.result.language}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        
                        Text(
                            text = "Confidence: ${String.format("%.1f%%", uiState.result.confidence * 100)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        is TranscriptionUiState.Error -> {
            Card(
                modifier = modifier,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = uiState.error.message ?: "Unknown error occurred",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        else -> {
            // Empty state
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Transcription results will appear here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
