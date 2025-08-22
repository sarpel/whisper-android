package com.app.whisper.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.whisper.data.manager.StorageInfo
import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.domain.repository.DownloadProgress
import com.app.whisper.presentation.ui.component.ModelDownloadCard

/**
 * Screen for managing Whisper models - downloading, deleting, and selecting models.
 * 
 * This screen provides a comprehensive interface for model management including
 * storage information, download progress, and model selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagementScreen(
    models: List<WhisperModel>,
    downloadProgress: Map<WhisperModel, DownloadProgress>,
    storageInfo: StorageInfo,
    onDownloadModel: (WhisperModel) -> Unit,
    onDeleteModel: (WhisperModel) -> Unit,
    onSelectModel: (WhisperModel) -> Unit,
    onCancelDownload: (WhisperModel) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Model Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage information
            item {
                StorageInfoCard(
                    storageInfo = storageInfo,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Quick actions
            item {
                QuickActionsCard(
                    models = models,
                    onDownloadRecommended = { 
                        // Download recommended model based on device capabilities
                        val recommended = WhisperModel.getRecommendedModel(
                            availableStorageBytes = storageInfo.availableSpace,
                            preferSpeed = false,
                            requireMultilingual = false
                        )
                        onDownloadModel(recommended)
                    },
                    onDeleteAll = {
                        // Delete all downloaded models
                        models.filter { it.isAvailable() }.forEach { model ->
                            onDeleteModel(model)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Model categories
            item {
                Text(
                    text = "Available Models",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Model list
            items(models) { model ->
                ModelDownloadCard(
                    model = model,
                    downloadProgress = downloadProgress[model],
                    onDownload = onDownloadModel,
                    onDelete = onDeleteModel,
                    onSelect = onSelectModel,
                    onCancel = onCancelDownload,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Storage information card.
 */
@Composable
private fun StorageInfoCard(
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = "Storage Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Storage stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StorageStatItem(
                    label = "Downloaded",
                    value = "${storageInfo.downloadedModelsCount}/${storageInfo.totalModelsCount}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                StorageStatItem(
                    label = "Used Space",
                    value = storageInfo.getFormattedTotalSize(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                StorageStatItem(
                    label = "Available",
                    value = storageInfo.getFormattedAvailableSpace(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Storage stat item.
 */
@Composable
private fun StorageStatItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color.copy(alpha = 0.8f)
        )
    }
}

/**
 * Quick actions card.
 */
@Composable
private fun QuickActionsCard(
    models: List<WhisperModel>,
    onDownloadRecommended: () -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Download recommended
                Button(
                    onClick = onDownloadRecommended,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Recommend,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Get Recommended")
                }
                
                // Delete all (only show if there are downloaded models)
                if (models.any { it.isAvailable() }) {
                    OutlinedButton(
                        onClick = onDeleteAll,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All")
                    }
                }
            }
        }
    }
}

/**
 * Model category header.
 */
@Composable
private fun ModelCategoryHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Empty state for when no models are available.
 */
@Composable
private fun EmptyModelsState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Models Available",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Check your internet connection and try again",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Loading state for model list.
 */
@Composable
private fun ModelsLoadingState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Loading Models...",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
