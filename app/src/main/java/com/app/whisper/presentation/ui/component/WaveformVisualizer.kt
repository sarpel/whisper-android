package com.app.whisper.presentation.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.app.whisper.data.model.WaveformData
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Waveform visualizer component for displaying audio waveforms.
 * 
 * This component renders audio waveform data with smooth animations
 * and supports both active (recording) and static (playback) modes.
 */
@Composable
fun WaveformVisualizer(
    waveformData: WaveformData?,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    barWidth: Float = 3f,
    barSpacing: Float = 2f,
    maxBars: Int = 100
) {
    val density = LocalDensity.current
    
    // Animation for active waveform
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_animation")
    val animatedPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform_phase"
    )
    
    // Animation for bar heights
    val animatedAmplitudes = remember { mutableStateListOf<Float>() }
    
    // Update amplitudes when waveform data changes
    LaunchedEffect(waveformData) {
        if (waveformData != null) {
            val normalizedAmplitudes = waveformData.getNormalizedAmplitudes()
            val targetAmplitudes = if (normalizedAmplitudes.size > maxBars) {
                // Downsample if too many bars
                val step = normalizedAmplitudes.size.toFloat() / maxBars
                (0 until maxBars).map { i ->
                    val index = (i * step).toInt().coerceIn(0, normalizedAmplitudes.size - 1)
                    normalizedAmplitudes[index]
                }
            } else {
                normalizedAmplitudes.toList()
            }
            
            // Initialize or update animated amplitudes
            if (animatedAmplitudes.isEmpty()) {
                animatedAmplitudes.addAll(targetAmplitudes)
            } else {
                // Smooth transition to new amplitudes
                targetAmplitudes.forEachIndexed { index, amplitude ->
                    if (index < animatedAmplitudes.size) {
                        animatedAmplitudes[index] = amplitude
                    } else {
                        animatedAmplitudes.add(amplitude)
                    }
                }
            }
        } else if (isActive) {
            // Generate random amplitudes for active recording without data
            val randomAmplitudes = (0 until maxBars).map { 
                (0.1f + Math.random().toFloat() * 0.9f) * if (Math.random() > 0.7) 1f else 0.3f
            }
            animatedAmplitudes.clear()
            animatedAmplitudes.addAll(randomAmplitudes)
        }
    }
    
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        
        if (animatedAmplitudes.isEmpty()) {
            // Draw empty state
            drawEmptyWaveform(
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                centerY = centerY,
                color = backgroundColor,
                barWidth = barWidth,
                barSpacing = barSpacing,
                maxBars = maxBars
            )
            return@Canvas
        }
        
        val totalBarWidth = barWidth + barSpacing
        val totalWidth = animatedAmplitudes.size * totalBarWidth - barSpacing
        val startX = (canvasWidth - totalWidth) / 2f
        
        animatedAmplitudes.forEachIndexed { index, amplitude ->
            val x = startX + index * totalBarWidth
            val barHeight = amplitude * canvasHeight * 0.8f // 80% of canvas height
            
            val actualAmplitude = if (isActive) {
                // Add some animation to active waveform
                val phase = (animatedPhase + index * 0.1f) % 1f
                amplitude * (0.7f + 0.3f * kotlin.math.sin(phase * 2 * kotlin.math.PI).toFloat())
            } else {
                amplitude
            }
            
            val actualBarHeight = actualAmplitude * canvasHeight * 0.8f
            
            // Draw the bar
            drawLine(
                color = if (isActive) color else color.copy(alpha = 0.7f),
                start = Offset(x, centerY - actualBarHeight / 2f),
                end = Offset(x, centerY + actualBarHeight / 2f),
                strokeWidth = barWidth
            )
            
            // Add glow effect for active recording
            if (isActive && actualAmplitude > 0.5f) {
                drawLine(
                    color = color.copy(alpha = 0.3f),
                    start = Offset(x, centerY - actualBarHeight / 2f - 2f),
                    end = Offset(x, centerY + actualBarHeight / 2f + 2f),
                    strokeWidth = barWidth + 2f
                )
            }
        }
        
        // Draw center line
        drawLine(
            color = backgroundColor,
            start = Offset(0f, centerY),
            end = Offset(canvasWidth, centerY),
            strokeWidth = 1f
        )
    }
}

/**
 * Draw empty waveform state.
 */
private fun DrawScope.drawEmptyWaveform(
    canvasWidth: Float,
    canvasHeight: Float,
    centerY: Float,
    color: Color,
    barWidth: Float,
    barSpacing: Float,
    maxBars: Int
) {
    val totalBarWidth = barWidth + barSpacing
    val totalWidth = maxBars * totalBarWidth - barSpacing
    val startX = (canvasWidth - totalWidth) / 2f
    
    repeat(maxBars) { index ->
        val x = startX + index * totalBarWidth
        val barHeight = canvasHeight * 0.1f // Small height for empty state
        
        drawLine(
            color = color.copy(alpha = 0.3f),
            start = Offset(x, centerY - barHeight / 2f),
            end = Offset(x, centerY + barHeight / 2f),
            strokeWidth = barWidth
        )
    }
    
    // Draw center line
    drawLine(
        color = color.copy(alpha = 0.5f),
        start = Offset(0f, centerY),
        end = Offset(canvasWidth, centerY),
        strokeWidth = 1f
    )
}

/**
 * Animated waveform visualizer with pulse effect.
 */
@Composable
fun AnimatedWaveformVisualizer(
    isRecording: Boolean,
    audioLevel: Float = 0f,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    barCount: Int = 50
) {
    val infiniteTransition = rememberInfiniteTransition(label = "animated_waveform")
    
    val animatedValues = (0 until barCount).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = if (isRecording) 1f else 0.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 800 + (index * 50) % 400,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }
    
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        
        val barWidth = 3.dp.toPx()
        val barSpacing = 2.dp.toPx()
        val totalBarWidth = barWidth + barSpacing
        val totalWidth = barCount * totalBarWidth - barSpacing
        val startX = (canvasWidth - totalWidth) / 2f
        
        animatedValues.forEachIndexed { index, animatedValue ->
            val x = startX + index * totalBarWidth
            val amplitude = animatedValue.value * audioLevel.coerceIn(0.1f, 1f)
            val barHeight = amplitude * canvasHeight * 0.6f
            
            drawLine(
                color = color.copy(alpha = if (isRecording) 1f else 0.5f),
                start = Offset(x, centerY - barHeight / 2f),
                end = Offset(x, centerY + barHeight / 2f),
                strokeWidth = barWidth
            )
        }
    }
}

/**
 * Simple static waveform for displaying processed audio.
 */
@Composable
fun StaticWaveform(
    amplitudes: FloatArray,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        
        if (amplitudes.isEmpty()) return@Canvas
        
        val stepX = canvasWidth / amplitudes.size
        
        // Draw waveform path
        var previousX = 0f
        var previousY = centerY
        
        amplitudes.forEachIndexed { index, amplitude ->
            val x = index * stepX
            val y = centerY - (amplitude * canvasHeight * 0.4f)
            
            if (index > 0) {
                drawLine(
                    color = color,
                    start = Offset(previousX, previousY),
                    end = Offset(x, y),
                    strokeWidth = 2f
                )
            }
            
            previousX = x
            previousY = y
        }
        
        // Draw center line
        drawLine(
            color = backgroundColor,
            start = Offset(0f, centerY),
            end = Offset(canvasWidth, centerY),
            strokeWidth = 1f
        )
    }
}
