package com.app.whisper.presentation.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.whisper.presentation.state.TranscriptionUiState

/**
 * Status card component that displays the current state of the transcription system.
 * 
 * This component provides visual feedback about the current operation,
 * model status, and system readiness with appropriate colors and animations.
 */
@Composable
fun StatusCard(
    uiState: TranscriptionUiState,
    modifier: Modifier = Modifier
) {
    val statusInfo = getStatusInfo(uiState)
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusInfo.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon with animation
            StatusIcon(
                icon = statusInfo.icon,
                color = statusInfo.iconColor,
                isAnimated = statusInfo.isAnimated
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Status content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = statusInfo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusInfo.textColor
                )
                
                if (statusInfo.subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusInfo.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusInfo.textColor.copy(alpha = 0.8f)
                    )
                }
                
                if (statusInfo.details.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusInfo.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusInfo.textColor.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Progress indicator for processing states
            if (statusInfo.showProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = statusInfo.iconColor,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

/**
 * Animated status icon component.
 */
@Composable
private fun StatusIcon(
    icon: ImageVector,
    color: Color,
    isAnimated: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status_icon_animation")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimated) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "icon_rotation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAnimated) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )
    
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (isAnimated) {
                        Modifier
                            .graphicsLayer {
                                rotationZ = rotation
                                scaleX = scale
                                scaleY = scale
                            }
                    } else {
                        Modifier
                    }
                )
        )
    }
}

/**
 * Get status information based on UI state.
 */
@Composable
private fun getStatusInfo(uiState: TranscriptionUiState): StatusInfo {
    return when (uiState) {
        is TranscriptionUiState.Initial -> StatusInfo(
            title = "Initializing",
            subtitle = "Setting up transcription system...",
            details = "",
            icon = Icons.Default.Refresh,
            iconColor = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surface,
            isAnimated = true,
            showProgress = false
        )
        
        is TranscriptionUiState.Loading -> StatusInfo(
            title = "Loading",
            subtitle = "Preparing transcription engine...",
            details = "",
            icon = Icons.Default.Refresh,
            iconColor = MaterialTheme.colorScheme.primary,
            textColor = MaterialTheme.colorScheme.onSurface,
            backgroundColor = MaterialTheme.colorScheme.surface,
            isAnimated = true,
            showProgress = true
        )
        
        is TranscriptionUiState.Ready -> {
            val modelName = uiState.currentModel?.displayName ?: "No model"
            val canRecord = uiState.canRecord
            
            StatusInfo(
                title = if (canRecord) "Ready to Record" else "Not Ready",
                subtitle = "Model: $modelName",
                details = if (canRecord) "Tap the microphone to start recording" else "Please select a model and grant microphone permission",
                icon = if (canRecord) Icons.Default.CheckCircle else Icons.Default.Warning,
                iconColor = if (canRecord) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.onSurface,
                backgroundColor = MaterialTheme.colorScheme.surface,
                isAnimated = false,
                showProgress = false
            )
        }
        
        is TranscriptionUiState.Recording -> {
            val duration = uiState.duration / 1000
            val minutes = duration / 60
            val seconds = duration % 60
            
            StatusInfo(
                title = "Recording",
                subtitle = String.format("Duration: %02d:%02d", minutes, seconds),
                details = "Speak clearly into the microphone",
                icon = Icons.Default.Mic,
                iconColor = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.onErrorContainer,
                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                isAnimated = true,
                showProgress = false
            )
        }
        
        is TranscriptionUiState.Processing -> {
            val progressText = uiState.progress.getDescription()
            
            StatusInfo(
                title = "Processing",
                subtitle = progressText,
                details = "Converting speech to text...",
                icon = Icons.Default.Psychology,
                iconColor = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                isAnimated = true,
                showProgress = true
            )
        }
        
        is TranscriptionUiState.Success -> {
            val confidence = (uiState.result.confidence * 100).toInt()
            val processingTime = uiState.processingTimeMs / 1000.0
            
            StatusInfo(
                title = "Transcription Complete",
                subtitle = "Confidence: ${confidence}%",
                details = String.format("Processed in %.1fs", processingTime),
                icon = Icons.Default.Done,
                iconColor = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                isAnimated = false,
                showProgress = false
            )
        }
        
        is TranscriptionUiState.Error -> {
            val errorMessage = uiState.error.message ?: "Unknown error"
            
            StatusInfo(
                title = "Error",
                subtitle = errorMessage,
                details = if (uiState.canRetry) "Tap retry to try again" else "",
                icon = Icons.Default.Error,
                iconColor = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.onErrorContainer,
                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                isAnimated = false,
                showProgress = false
            )
        }
    }
}

/**
 * Data class containing status display information.
 */
private data class StatusInfo(
    val title: String,
    val subtitle: String,
    val details: String,
    val icon: ImageVector,
    val iconColor: Color,
    val textColor: Color,
    val backgroundColor: Color,
    val isAnimated: Boolean,
    val showProgress: Boolean
)
