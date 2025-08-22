package com.app.whisper.presentation.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.whisper.domain.entity.WhisperModel
import com.app.whisper.presentation.permission.PermissionState
import com.app.whisper.presentation.ui.component.PermissionStatusIndicator

/**
 * Settings screen for configuring app preferences and permissions.
 * 
 * This screen provides access to model selection, audio settings,
 * permission management, and other app configurations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    permissionState: PermissionState,
    availableModels: List<WhisperModel>,
    selectedModel: WhisperModel?,
    audioSettings: AudioSettings,
    onModelSelected: (WhisperModel) -> Unit,
    onAudioSettingsChanged: (AudioSettings) -> Unit,
    onRequestPermission: () -> Unit,
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
                    text = "Settings",
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
        
        // Settings Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permissions Section
            item {
                SettingsSection(
                    title = "Permissions",
                    icon = Icons.Default.Security
                ) {
                    PermissionSettingsCard(
                        permissionState = permissionState,
                        onRequestPermission = onRequestPermission
                    )
                }
            }
            
            // Model Selection Section
            item {
                SettingsSection(
                    title = "Transcription Model",
                    icon = Icons.Default.Psychology
                ) {
                    ModelSelectionCard(
                        availableModels = availableModels,
                        selectedModel = selectedModel,
                        onModelSelected = onModelSelected
                    )
                }
            }
            
            // Audio Settings Section
            item {
                SettingsSection(
                    title = "Audio Settings",
                    icon = Icons.Default.AudioFile
                ) {
                    AudioSettingsCard(
                        audioSettings = audioSettings,
                        onSettingsChanged = onAudioSettingsChanged
                    )
                }
            }
            
            // About Section
            item {
                SettingsSection(
                    title = "About",
                    icon = Icons.Default.Info
                ) {
                    AboutCard()
                }
            }
        }
    }
}

/**
 * Settings section with title and icon.
 */
@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Section Content
        content()
    }
}

/**
 * Permission settings card.
 */
@Composable
private fun PermissionSettingsCard(
    permissionState: PermissionState,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            PermissionStatusIndicator(
                permissionState = permissionState,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (!permissionState.isGranted()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Microphone access is required for audio recording and transcription features.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

/**
 * Model selection card.
 */
@Composable
private fun ModelSelectionCard(
    availableModels: List<WhisperModel>,
    selectedModel: WhisperModel?,
    onModelSelected: (WhisperModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Transcription Model",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Choose the model that best fits your needs. Larger models provide better accuracy but require more storage and processing power.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Model Options
            availableModels.forEach { model ->
                ModelOption(
                    model = model,
                    isSelected = model == selectedModel,
                    onSelected = { onModelSelected(model) }
                )
                
                if (model != availableModels.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Individual model option.
 */
@Composable
private fun ModelOption(
    model: WhisperModel,
    isSelected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelected
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onSelected
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "${model.size.displayName} • ${model.getFileSizeFormatted()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Download status indicator
        when (model.status) {
            is com.app.whisper.domain.entity.ModelStatus.Available -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            is com.app.whisper.domain.entity.ModelStatus.Downloading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = "Not downloaded",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Audio settings card.
 */
@Composable
private fun AudioSettingsCard(
    audioSettings: AudioSettings,
    onSettingsChanged: (AudioSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Audio Configuration",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sample Rate Setting
            SettingRow(
                title = "Sample Rate",
                subtitle = "${audioSettings.sampleRate} Hz",
                icon = Icons.Default.GraphicEq
            ) {
                // Sample rate selector would go here
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Auto Language Detection
            SettingRow(
                title = "Auto Language Detection",
                subtitle = if (audioSettings.autoDetectLanguage) "Enabled" else "Disabled",
                icon = Icons.Default.Language
            ) {
                Switch(
                    checked = audioSettings.autoDetectLanguage,
                    onCheckedChange = { 
                        onSettingsChanged(audioSettings.copy(autoDetectLanguage = it))
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Noise Reduction
            SettingRow(
                title = "Noise Reduction",
                subtitle = if (audioSettings.noiseReduction) "Enabled" else "Disabled",
                icon = Icons.Default.FilterAlt
            ) {
                Switch(
                    checked = audioSettings.noiseReduction,
                    onCheckedChange = { 
                        onSettingsChanged(audioSettings.copy(noiseReduction = it))
                    }
                )
            }
        }
    }
}

/**
 * About card with app information.
 */
@Composable
private fun AboutCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Whisper Android",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "A privacy-focused speech-to-text app powered by OpenAI's Whisper model. All processing happens locally on your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Generic setting row component.
 */
@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        action()
    }
}

/**
 * Data class for audio settings.
 */
data class AudioSettings(
    val sampleRate: Int = 16000,
    val autoDetectLanguage: Boolean = true,
    val noiseReduction: Boolean = true,
    val channels: Int = 1,
    val bitRate: Int = 256
)
