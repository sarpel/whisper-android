package com.app.whisper.presentation.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles audio recording permissions for the Whisper app.
 * 
 * This class manages microphone permission requests, tracks permission state,
 * and provides a clean interface for permission-related operations.
 */
class PermissionHandler(private val activity: ComponentActivity) {
    
    // Permission state
    private val _permissionState = MutableStateFlow(getInitialPermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()
    
    // Permission launcher
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    
    init {
        setupPermissionLauncher()
    }
    
    /**
     * Setup the permission request launcher.
     */
    private fun setupPermissionLauncher() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            updatePermissionState(isGranted)
        }
    }
    
    /**
     * Request microphone permission.
     */
    fun requestMicrophonePermission() {
        when {
            hasPermission() -> {
                _permissionState.value = PermissionState.Granted
            }
            
            activity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                _permissionState.value = PermissionState.ShouldShowRationale
            }
            
            else -> {
                _permissionState.value = PermissionState.Requesting
                permissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    /**
     * Check if microphone permission is granted.
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Update permission state after request result.
     */
    private fun updatePermissionState(isGranted: Boolean) {
        _permissionState.value = if (isGranted) {
            PermissionState.Granted
        } else {
            if (activity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                PermissionState.Denied
            } else {
                PermissionState.PermanentlyDenied
            }
        }
    }
    
    /**
     * Get initial permission state.
     */
    private fun getInitialPermissionState(): PermissionState {
        return if (hasPermission()) {
            PermissionState.Granted
        } else {
            PermissionState.NotRequested
        }
    }
    
    /**
     * Refresh permission state (call when returning from settings).
     */
    fun refreshPermissionState() {
        _permissionState.value = getInitialPermissionState()
    }
    
    companion object {
        /**
         * Check if microphone permission is granted for a given context.
         */
        fun hasAudioPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

/**
 * Sealed class representing different permission states.
 */
sealed class PermissionState {
    object NotRequested : PermissionState()
    object Requesting : PermissionState()
    object Granted : PermissionState()
    object Denied : PermissionState()
    object ShouldShowRationale : PermissionState()
    object PermanentlyDenied : PermissionState()
    
    /**
     * Check if permission is granted.
     */
    fun isGranted(): Boolean = this is Granted
    
    /**
     * Check if permission is denied.
     */
    fun isDenied(): Boolean = this is Denied || this is PermanentlyDenied
    
    /**
     * Check if we should show rationale.
     */
    fun shouldShowRationale(): Boolean = this is ShouldShowRationale
    
    /**
     * Check if permission is permanently denied.
     */
    fun isPermanentlyDenied(): Boolean = this is PermanentlyDenied
    
    /**
     * Get human-readable description of the permission state.
     */
    fun getDescription(): String = when (this) {
        is NotRequested -> "Permission not requested"
        is Requesting -> "Requesting permission..."
        is Granted -> "Permission granted"
        is Denied -> "Permission denied"
        is ShouldShowRationale -> "Permission rationale needed"
        is PermanentlyDenied -> "Permission permanently denied"
    }
}
