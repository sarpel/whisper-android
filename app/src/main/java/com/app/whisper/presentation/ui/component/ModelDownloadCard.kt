package com.app.whisper.presentation.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.whisper.domain.entity.ModelStatus
import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.domain.repository.DownloadProgress

/**
 * Card component for displaying model information and download controls.
 * 
 * This component shows model details, download status, and provides
 * actions for downloading, deleting, or selecting models.
 */
@Composable
fun ModelDownloadCard(
    model: WhisperModel,
    downloadProgress: DownloadProgress?,
    onDownload: (WhisperModel) -> Unit,
    onDelete: (WhisperModel) -> Unit,
    onSelect: (WhisperModel) -> Unit,
    onCancel: (WhisperModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (model.isCurrent) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with model info
            ModelHeader(
                model = model,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = model.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Model details
            ModelDetails(
                model = model,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Download progress (if downloading)
            downloadProgress?.let { progress ->
                ModelDownloadProgress(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Action buttons
            ModelActions(
                model = model,
                downloadProgress = downloadProgress,
                onDownload = onDownload,
                onDelete = onDelete,
                onSelect = onSelect,
                onCancel = onCancel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Model header with name and status indicator.
 */
@Composable
private fun ModelHeader(
    model: WhisperModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (model.isCurrent) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            Text(
                text = model.recommendedUseCase,
                style = MaterialTheme.typography.bodySmall,
                color = if (model.isCurrent) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        // Status indicator
        ModelStatusIndicator(
            status = model.status,
            isCurrent = model.isCurrent
        )
    }
}

/**
 * Model details row with size and language info.
 */
@Composable
private fun ModelDetails(
    model: WhisperModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Size info
        DetailChip(
            icon = Icons.Default.Storage,
            text = model.getFileSizeFormatted(),
            color = MaterialTheme.colorScheme.outline
        )
        
        // Language info
        DetailChip(
            icon = Icons.Default.Language,
            text = if (model.isMultilingual) "Multilingual" else "English",
            color = MaterialTheme.colorScheme.outline
        )
        
        // Speed indicator
        DetailChip(
            icon = Icons.Default.Speed,
            text = getSpeedText(model.getSpeedFactor()),
            color = MaterialTheme.colorScheme.outline
        )
    }
}

/**
 * Small detail chip component.
 */
@Composable
private fun DetailChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

/**
 * Model status indicator.
 */
@Composable
private fun ModelStatusIndicator(
    status: ModelStatus,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when {
        isCurrent -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        status == ModelStatus.Available -> Icons.Default.CloudDone to MaterialTheme.colorScheme.primary
        status == ModelStatus.Downloading -> Icons.Default.CloudDownload to MaterialTheme.colorScheme.secondary
        status == ModelStatus.Error -> Icons.Default.Error to MaterialTheme.colorScheme.error
        else -> Icons.Default.CloudDownload to MaterialTheme.colorScheme.outline
    }
    
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = color,
        modifier = modifier.size(20.dp)
    )
}

/**
 * Download progress indicator.
 */
@Composable
private fun ModelDownloadProgress(
    progress: DownloadProgress,
    modifier: Modifier = Modifier
) {
    when (progress) {
        is DownloadProgress.InProgress -> {
            Column(modifier = modifier) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloading...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = if (progress.progress >= 0) {
                            "${(progress.progress * 100).toInt()}%"
                        } else {
                            "..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (progress.progress >= 0) {
                    LinearProgressIndicator(
                        progress = progress.progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        is DownloadProgress.Completed -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                
                Text(
                    text = "Download completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        is DownloadProgress.Failed -> {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                
                Text(
                    text = "Download failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Model action buttons.
 */
@Composable
private fun ModelActions(
    model: WhisperModel,
    downloadProgress: DownloadProgress?,
    onDownload: (WhisperModel) -> Unit,
    onDelete: (WhisperModel) -> Unit,
    onSelect: (WhisperModel) -> Unit,
    onCancel: (WhisperModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            model.status == ModelStatus.Downloading -> {
                // Cancel button
                OutlinedButton(
                    onClick = { onCancel(model) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
            }
            
            model.isAvailable() -> {
                // Select/Use button
                if (!model.isCurrent) {
                    Button(
                        onClick = { onSelect(model) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Use")
                    }
                } else {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Current")
                    }
                }
                
                // Delete button
                OutlinedButton(
                    onClick = { onDelete(model) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            else -> {
                // Download button
                Button(
                    onClick = { onDownload(model) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download")
                }
            }
        }
    }
}

/**
 * Get speed text from speed factor.
 */
private fun getSpeedText(speedFactor: Float): String {
    return when {
        speedFactor <= 0.2f -> "Very Fast"
        speedFactor <= 0.4f -> "Fast"
        speedFactor <= 0.8f -> "Medium"
        speedFactor <= 1.2f -> "Slow"
        else -> "Very Slow"
    }
}
