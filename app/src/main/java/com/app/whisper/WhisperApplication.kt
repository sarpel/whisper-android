package com.app.whisper

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Main application class for Whisper Android.
 *
 * This class serves as the entry point for the application and is responsible for:
 * - Initializing Hilt dependency injection
 * - Setting up logging with Timber
 * - Configuring global application settings
 * - Handling application lifecycle events
 */
@HiltAndroidApp
class WhisperApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize logging
        initializeLogging()

        // Log application startup
        Timber.i("WhisperApplication started")

        // Initialize crash reporting (if needed)
        initializeCrashReporting()

        // Set up global exception handler
        setupGlobalExceptionHandler()

        Timber.d("Application initialization completed")
    }

    /**
     * Initialize Timber logging based on build configuration.
     */
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            // Debug build: Plant debug tree with detailed logging
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    // Include line numbers in debug logs
                    return "${super.createStackElementTag(element)}:${element.lineNumber}"
                }

                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // Add thread information to debug logs
                    val threadName = Thread.currentThread().name
                    val enhancedMessage = "[$threadName] $message"
                    super.log(priority, tag, enhancedMessage, t)
                }
            })

            Timber.d("Debug logging initialized")
        } else {
            // Release build: Plant release tree with minimal logging
            Timber.plant(ReleaseTree())
            Timber.i("Release logging initialized")
        }
    }

    /**
     * Initialize crash reporting for production builds.
     */
    private fun initializeCrashReporting() {
        if (!BuildConfig.DEBUG) {
            // In a real app, you would initialize crash reporting here
            // For example: Firebase Crashlytics, Bugsnag, etc.
            Timber.i("Crash reporting would be initialized here in production")
        }
    }

    /**
     * Set up global exception handler for uncaught exceptions.
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Timber.e(exception, "Uncaught exception in thread: ${thread.name}")

            // Log additional context
            Timber.e("Application state: foreground=${isInForeground()}")
            Timber.e("Available memory: ${getAvailableMemory()} MB")

            // Call the default handler to ensure proper crash handling
            defaultHandler?.uncaughtException(thread, exception)
        }
    }

    /**
     * Check if the application is currently in the foreground.
     */
    private fun isInForeground(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses ?: return false

        return runningAppProcesses.any { processInfo ->
            processInfo.processName == packageName &&
            processInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }
    }

    /**
     * Get available memory in MB.
     */
    private fun getAvailableMemory(): Long {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024) // Convert to MB
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("Application received low memory warning")

        // Perform memory cleanup if needed
        performMemoryCleanup()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        val levelName = when (level) {
            TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
            TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
            TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
            TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            TRIM_MEMORY_MODERATE -> "MODERATE"
            TRIM_MEMORY_COMPLETE -> "COMPLETE"
            else -> "UNKNOWN($level)"
        }

        Timber.d("Memory trim requested: $levelName")

        // Perform appropriate memory cleanup based on level
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                performMemoryCleanup()
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                performAggressiveMemoryCleanup()
            }
        }
    }

    /**
     * Perform basic memory cleanup.
     */
    private fun performMemoryCleanup() {
        Timber.d("Performing memory cleanup")

        // Clear image caches, temporary files, etc.
        // This would be implemented based on your app's specific needs
        System.gc() // Suggest garbage collection
    }

    /**
     * Perform aggressive memory cleanup for background/low memory situations.
     */
    private fun performAggressiveMemoryCleanup() {
        Timber.d("Performing aggressive memory cleanup")

        // Clear all non-essential caches
        // Release heavy resources
        // This would be implemented based on your app's specific needs
        performMemoryCleanup()
    }
}

/**
 * Custom Timber tree for release builds.
 *
 * This tree filters out debug and verbose logs in production,
 * and can be extended to send error logs to crash reporting services.
 */
private class ReleaseTree : Timber.Tree() {

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        // Only log warnings, errors, and info in release builds
        return priority >= Log.INFO
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isLoggable(tag, priority)) return

        // In a real app, you might want to send error logs to a crash reporting service
        when (priority) {
            Log.ERROR -> {
                // Send to crash reporting service
                // Example: Crashlytics.log(message)
                if (t != null) {
                    // Example: Crashlytics.recordException(t)
                }
            }
            Log.WARN -> {
                // Optionally send warnings to analytics
                // Example: Analytics.logEvent("warning", message)
            }
        }

        // Still log to system log for debugging on device
        when (priority) {
            Log.INFO -> Log.i(tag, message, t)
            Log.WARN -> Log.w(tag, message, t)
            Log.ERROR -> Log.e(tag, message, t)
        }
    }
}
