package com.app.whisper.presentation.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Custom recording button with animations and visual feedback.
 * 
 * This component provides an animated button for recording controls
 * with pulse effects, scaling animations, and customizable appearance.
 */
@Composable
fun RecordingButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
    size: ButtonSize = ButtonSize.Large
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Pulse animation for recording state
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Press animation
    val pressScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "press_scale"
    )
    
    val buttonColor = if (enabled) color else color.copy(alpha = 0.5f)
    val contentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.7f)
    
    when (size) {
        ButtonSize.Small -> SmallRecordingButton(
            onClick = onClick,
            enabled = enabled,
            icon = icon,
            text = text,
            color = buttonColor,
            contentColor = contentColor,
            scale = pressScale * pulseScale,
            interactionSource = interactionSource,
            modifier = modifier
        )
        
        ButtonSize.Medium -> MediumRecordingButton(
            onClick = onClick,
            enabled = enabled,
            icon = icon,
            text = text,
            color = buttonColor,
            contentColor = contentColor,
            scale = pressScale * pulseScale,
            interactionSource = interactionSource,
            modifier = modifier
        )
        
        ButtonSize.Large -> LargeRecordingButton(
            onClick = onClick,
            enabled = enabled,
            icon = icon,
            text = text,
            color = buttonColor,
            contentColor = contentColor,
            scale = pressScale * pulseScale,
            interactionSource = interactionSource,
            isRecording = isRecording,
            modifier = modifier
        )
    }
}

/**
 * Large recording button with prominent design.
 */
@Composable
private fun LargeRecordingButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    text: String,
    color: Color,
    contentColor: Color,
    scale: Float,
    interactionSource: MutableInteractionSource,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main circular button
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(color)
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(
                        bounded = true,
                        radius = 40.dp,
                        color = contentColor
                    ),
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Button label
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
        
        // Recording indicator
        if (isRecording) {
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = "REC",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Medium recording button for secondary actions.
 */
@Composable
private fun MediumRecordingButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    text: String,
    color: Color,
    contentColor: Color,
    scale: Float,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .scale(scale)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor,
            disabledContainerColor = color.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(24.dp),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Small recording button for compact layouts.
 */
@Composable
private fun SmallRecordingButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    text: String,
    color: Color,
    contentColor: Color,
    scale: Float,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .scale(scale)
            .size(40.dp)
            .clip(CircleShape)
            .background(color),
        interactionSource = interactionSource
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Floating action button style recording button.
 */
@Composable
fun FloatingRecordingButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_pulse_scale"
    )
    
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.scale(pulseScale),
        containerColor = color,
        contentColor = Color.White
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Button size variants.
 */
enum class ButtonSize {
    Small,
    Medium,
    Large
}
