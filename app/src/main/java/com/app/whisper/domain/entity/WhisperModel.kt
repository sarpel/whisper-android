package com.app.whisper.domain.entity

/**
 * Domain entity representing a Whisper model.
 * 
 * This entity encapsulates all information about a Whisper model including
 * its metadata, download status, file information, and capabilities.
 */
data class WhisperModel(
    val id: String,
    val name: String,
    val displayName: String,
    val description: String,
    val size: ModelSize,
    val fileSizeBytes: Long,
    val downloadUrl: String,
    val version: String,
    val checksum: String,
    val isMultilingual: Boolean,
    val supportedLanguages: List<String>,
    val status: ModelStatus = ModelStatus.NOT_DOWNLOADED,
    val localPath: String? = null,
    val downloadedAt: Long? = null,
    val lastUsedAt: Long? = null,
    val metadata: ModelMetadata = ModelMetadata()
) {
    
    /**
     * Check if the model is available for use.
     * 
     * @return true if model is downloaded and ready
     */
    fun isAvailable(): Boolean = status == ModelStatus.DOWNLOADED && localPath != null
    
    /**
     * Check if the model is currently being downloaded.
     * 
     * @return true if download is in progress
     */
    fun isDownloading(): Boolean = status == ModelStatus.DOWNLOADING
    
    /**
     * Check if the model supports a specific language.
     * 
     * @param languageCode Language code to check (e.g., "en", "tr")
     * @return true if language is supported
     */
    fun supportsLanguage(languageCode: String): Boolean {
        return isMultilingual || supportedLanguages.contains(languageCode.lowercase())
    }
    
    /**
     * Get the model size in a human-readable format.
     * 
     * @return Formatted size string (e.g., "39 MB", "1.5 GB")
     */
    fun getFormattedSize(): String {
        return when {
            fileSizeBytes < 1024 * 1024 -> "${fileSizeBytes / 1024} KB"
            fileSizeBytes < 1024 * 1024 * 1024 -> "${fileSizeBytes / (1024 * 1024)} MB"
            else -> String.format("%.1f GB", fileSizeBytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * Get the expected transcription quality level.
     * 
     * @return Quality level based on model size
     */
    fun getQualityLevel(): QualityLevel = when (size) {
        ModelSize.TINY -> QualityLevel.BASIC
        ModelSize.BASE -> QualityLevel.GOOD
        ModelSize.SMALL -> QualityLevel.BETTER
        ModelSize.MEDIUM -> QualityLevel.HIGH
        ModelSize.LARGE -> QualityLevel.EXCELLENT
    }
    
    /**
     * Get the expected processing speed relative to real-time.
     * 
     * @return Speed multiplier (e.g., 2.0 means 2x faster than real-time)
     */
    fun getExpectedSpeed(): Float = when (size) {
        ModelSize.TINY -> 8.0f
        ModelSize.BASE -> 4.0f
        ModelSize.SMALL -> 2.0f
        ModelSize.MEDIUM -> 1.0f
        ModelSize.LARGE -> 0.5f
    }
    
    /**
     * Check if this model is recommended for the current device.
     * 
     * @param availableMemoryMB Available device memory in MB
     * @param preferSpeed Whether to prefer speed over quality
     * @return true if model is recommended
     */
    fun isRecommendedFor(availableMemoryMB: Long, preferSpeed: Boolean = false): Boolean {
        val requiredMemoryMB = getRequiredMemoryMB()
        val hasEnoughMemory = availableMemoryMB >= requiredMemoryMB * 1.5 // 50% buffer
        
        return if (preferSpeed) {
            hasEnoughMemory && (size == ModelSize.TINY || size == ModelSize.BASE)
        } else {
            hasEnoughMemory
        }
    }
    
    /**
     * Get the estimated memory requirement for this model.
     * 
     * @return Required memory in MB
     */
    fun getRequiredMemoryMB(): Long = when (size) {
        ModelSize.TINY -> 64L
        ModelSize.BASE -> 128L
        ModelSize.SMALL -> 256L
        ModelSize.MEDIUM -> 512L
        ModelSize.LARGE -> 1024L
    }
    
    /**
     * Create a copy with updated status.
     * 
     * @param newStatus New model status
     * @param localPath Local file path (for downloaded models)
     * @return Updated model instance
     */
    fun withStatus(newStatus: ModelStatus, localPath: String? = null): WhisperModel = copy(
        status = newStatus,
        localPath = localPath,
        downloadedAt = if (newStatus == ModelStatus.DOWNLOADED) System.currentTimeMillis() else downloadedAt
    )
    
    /**
     * Create a copy with updated last used timestamp.
     * 
     * @return Updated model instance
     */
    fun withLastUsed(): WhisperModel = copy(lastUsedAt = System.currentTimeMillis())
    
    companion object {
        /**
         * Create a Whisper model instance for the tiny model.
         */
        fun tiny(): WhisperModel = WhisperModel(
            id = "whisper-tiny",
            name = "tiny",
            displayName = "Tiny",
            description = "Fastest model, basic quality. Good for real-time applications.",
            size = ModelSize.TINY,
            fileSizeBytes = 39 * 1024 * 1024, // ~39MB
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
            version = "1.0.0",
            checksum = "",
            isMultilingual = false,
            supportedLanguages = listOf("en")
        )
        
        /**
         * Create a Whisper model instance for the base model.
         */
        fun base(): WhisperModel = WhisperModel(
            id = "whisper-base",
            name = "base",
            displayName = "Base",
            description = "Balanced speed and quality. Recommended for most use cases.",
            size = ModelSize.BASE,
            fileSizeBytes = 142 * 1024 * 1024, // ~142MB
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
            version = "1.0.0",
            checksum = "",
            isMultilingual = true,
            supportedLanguages = listOf("en", "tr", "de", "fr", "es", "it", "pt", "ru", "ja", "ko", "zh")
        )
        
        /**
         * Create a Whisper model instance for the small model.
         */
        fun small(): WhisperModel = WhisperModel(
            id = "whisper-small",
            name = "small",
            displayName = "Small",
            description = "Better quality with moderate speed. Good balance for mobile devices.",
            size = ModelSize.SMALL,
            fileSizeBytes = 244 * 1024 * 1024, // ~244MB
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
            version = "1.0.0",
            checksum = "",
            isMultilingual = true,
            supportedLanguages = listOf("en", "tr", "de", "fr", "es", "it", "pt", "ru", "ja", "ko", "zh")
        )
        
        /**
         * Get all available model presets.
         * 
         * @return List of all available models
         */
        fun getAllModels(): List<WhisperModel> = listOf(tiny(), base(), small())
        
        /**
         * Get the recommended model for a device with specific constraints.
         * 
         * @param availableMemoryMB Available device memory in MB
         * @param preferSpeed Whether to prioritize speed over quality
         * @return Recommended model
         */
        fun getRecommended(availableMemoryMB: Long, preferSpeed: Boolean = false): WhisperModel {
            val models = getAllModels()
            return models.firstOrNull { it.isRecommendedFor(availableMemoryMB, preferSpeed) } ?: tiny()
        }
    }
}

/**
 * Enumeration of Whisper model sizes.
 */
enum class ModelSize {
    TINY,
    BASE,
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * Enumeration of model download/availability status.
 */
enum class ModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    CORRUPTED,
    OUTDATED
}

/**
 * Enumeration of transcription quality levels.
 */
enum class QualityLevel {
    BASIC,
    GOOD,
    BETTER,
    HIGH,
    EXCELLENT
}

/**
 * Additional metadata for a Whisper model.
 */
data class ModelMetadata(
    val architecture: String = "transformer",
    val parameters: Long = 0L,
    val vocabularySize: Int = 0,
    val contextLength: Int = 0,
    val releaseDate: String = "",
    val license: String = "MIT",
    val author: String = "OpenAI",
    val notes: String = ""
) {
    
    /**
     * Get a formatted parameter count string.
     * 
     * @return Formatted parameter count (e.g., "39M", "1.5B")
     */
    fun getFormattedParameters(): String = when {
        parameters < 1_000_000 -> "${parameters / 1000}K"
        parameters < 1_000_000_000 -> "${parameters / 1_000_000}M"
        else -> String.format("%.1fB", parameters / 1_000_000_000.0)
    }
}
