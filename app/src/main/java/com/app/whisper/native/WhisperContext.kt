package com.app.whisper.native

/**
 * Data class representing the state and configuration of a Whisper context.
 * 
 * This class encapsulates all the information about a loaded Whisper model
 * and its current configuration, providing a clean interface for context
 * management and state tracking.
 */
data class WhisperContext(
    val modelPath: String,
    val threadCount: Int,
    val isInitialized: Boolean = false,
    val isMultilingual: Boolean = false,
    val modelInfo: String? = null,
    val contextPtr: Long = 0L
) {
    
    /**
     * Check if the context is ready for transcription.
     * 
     * @return true if context is properly initialized and ready
     */
    fun isReady(): Boolean = isInitialized && contextPtr != 0L
    
    /**
     * Get a human-readable description of the context state.
     * 
     * @return String describing the current state
     */
    fun getStateDescription(): String = when {
        !isInitialized -> "Not initialized"
        contextPtr == 0L -> "Invalid context pointer"
        else -> "Ready for transcription"
    }
    
    /**
     * Get model file name from the full path.
     * 
     * @return Model file name
     */
    fun getModelFileName(): String = modelPath.substringAfterLast('/')
    
    /**
     * Get estimated model size category based on file name.
     * 
     * @return Model size category (tiny, base, small, medium, large)
     */
    fun getModelSizeCategory(): String = when {
        modelPath.contains("tiny", ignoreCase = true) -> "tiny"
        modelPath.contains("base", ignoreCase = true) -> "base"
        modelPath.contains("small", ignoreCase = true) -> "small"
        modelPath.contains("medium", ignoreCase = true) -> "medium"
        modelPath.contains("large", ignoreCase = true) -> "large"
        else -> "unknown"
    }
    
    /**
     * Create a copy of this context with updated initialization state.
     * 
     * @param initialized New initialization state
     * @param contextPtr New context pointer
     * @param multilingual Whether model supports multilingual
     * @param info Model information string
     * @return Updated context instance
     */
    fun withInitialization(
        initialized: Boolean,
        contextPtr: Long = 0L,
        multilingual: Boolean = false,
        info: String? = null
    ): WhisperContext = copy(
        isInitialized = initialized,
        contextPtr = contextPtr,
        isMultilingual = multilingual,
        modelInfo = info
    )
    
    companion object {
        /**
         * Create a new uninitialized context.
         * 
         * @param modelPath Path to the model file
         * @param threadCount Number of threads to use
         * @return New WhisperContext instance
         */
        fun create(modelPath: String, threadCount: Int): WhisperContext =
            WhisperContext(
                modelPath = modelPath,
                threadCount = threadCount,
                isInitialized = false
            )
    }
}
