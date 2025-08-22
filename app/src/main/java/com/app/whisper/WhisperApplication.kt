package com.app.whisper

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Whisper Android app.
 * 
 * This class serves as the entry point for the application and is responsible for:
 * - Initializing Hilt dependency injection
 * - Setting up global application state
 * - Handling application-wide configurations
 */
@HiltAndroidApp
class WhisperApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any global configurations here
        // For example: logging, crash reporting, etc.
    }
}
