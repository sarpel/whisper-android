package com.app.whisper.domain.entity

import java.text.DecimalFormat

/**
 * Enum representing available Whisper models with their characteristics.
 *
 * Each model has different accuracy, speed, and size trade-offs. Larger models provide better
 * accuracy but require more storage and processing power.
 */
enum class WhisperModel(
        val id: String,
        val displayName: String,
        val description: String,
        val size: ModelSize,
        val fileSizeBytes: Long,
        val downloadUrl: String,
        val version: String,
        val checksum: String,
        val isMultilingual: Boolean,
        val supportedLanguages: List<String>,
        val recommendedUseCase: String
) {
    TINY(
            id = "tiny",
            displayName = "Tiny",
            description = "Fastest model with basic accuracy. Good for real-time transcription.",
            size = ModelSize.TINY,
            fileSizeBytes = 39_000_000L, // ~39MB
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
            version = "1.0.0",
            checksum = "bd577a113a864445d4c299885e0cb97d4ba92b5f",
            isMultilingual = false,
            supportedLanguages = listOf("en"),
            recommendedUseCase = "Quick transcription, real-time use"
    ),
    BASE(
            id = "base",
            displayName = "Base",
            description = "Balanced model with good accuracy and reasonable speed.",
            size = ModelSize.BASE,
            fileSizeBytes = 142_000_000L, // ~142MB
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
            version = "1.0.0",
            checksum = "465707469ff3a37a2b9b8d8f89f2f99de7299dac",
            isMultilingual = true,
            supportedLanguages =
                    listOf(
                            "en",
                            "zh",
                            "de",
                            "es",
                            "ru",
                            "ko",
                            "fr",
                            "ja",
                            "pt",
                            "tr",
                            "pl",
                            "ca",
                            "nl",
                            "ar",
                            "sv",
                            "it",
                            "id",
                            "hi",
                            "fi",
                            "vi",
                            "he",
                            "uk",
                            "el",
                            "ms",
                            "cs",
                            "ro",
                            "da",
                            "hu",
                            "ta",
                            "no",
                            "th",
                            "ur",
                            "hr",
                            "bg",
                            "lt",
                            "la",
                            "mi",
                            "ml",
                            "cy",
                            "sk",
                            "te",
                            "fa",
                            "lv",
                            "bn",
                            "sr",
                            "az",
                            "sl",
                            "kn",
                            "et",
                            "mk",
                            "br",
                            "eu",
                            "is",
                            "hy",
                            "ne",
                            "mn",
                            "bs",
                            "kk",
                            "sq",
                            "sw",
                            "gl",
                            "mr",
                            "pa",
                            "si",
                            "km",
                            "sn",
                            "yo",
                            "so",
                            "af",
                            "oc",
                            "ka",
                            "be",
                            "tg",
                            "sd",
                            "gu",
                            "am",
                            "yi",
                            "lo",
                            "uz",
                            "fo",
                            "ht",
                            "ps",
                            "tk",
                            "nn",
                            "mt",
                            "sa",
                            "lb",
                            "my",
                            "bo",
                            "tl",
                            "mg",
                            "as",
                            "tt",
                            "haw",
                            "ln",
                            "ha",
                            "ba",
                            "jw",
                            "su"
                    ),
            recommendedUseCase = "General purpose transcription"
    ),
    SMALL(
            id = "small",
            displayName = "Small",
            description =
                    "Higher accuracy model with moderate speed. Good balance for most use cases.",
            size = ModelSize.SMALL,
            fileSizeBytes = 466_000_000L, // ~466MB
            downloadUrl =
                    "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
            version = "1.0.0",
            checksum = "55356645c2b361a969dfd0ef2c5a50d530afd8d5",
            isMultilingual = true,
            supportedLanguages =
                    listOf(
                            "en",
                            "zh",
                            "de",
                            "es",
                            "ru",
                            "ko",
                            "fr",
                            "ja",
                            "pt",
                            "tr",
                            "pl",
                            "ca",
                            "nl",
                            "ar",
                            "sv",
                            "it",
                            "id",
                            "hi",
                            "fi",
                            "vi",
                            "he",
                            "uk",
                            "el",
                            "ms",
                            "cs",
                            "ro",
                            "da",
                            "hu",
                            "ta",
                            "no",
                            "th",
                            "ur",
                            "hr",
                            "bg",
                            "lt",
                            "la",
                            "mi",
                            "ml",
                            "cy",
                            "sk",
                            "te",
                            "fa",
                            "lv",
                            "bn",
                            "sr",
                            "az",
                            "sl",
                            "kn",
                            "et",
                            "mk",
                            "br",
                            "eu",
                            "is",
                            "hy",
                            "ne",
                            "mn",
                            "bs",
                            "kk",
                            "sq",
                            "sw",
                            "gl",
                            "mr",
                            "pa",
                            "si",
                            "km",
                            "sn",
                            "yo",
                            "so",
                            "af",
                            "oc",
                            "ka",
                            "be",
                            "tg",
                            "sd",
                            "gu",
                            "am",
                            "yi",
                            "lo",
                            "uz",
                            "fo",
                            "ht",
                            "ps",
                            "tk",
                            "nn",
                            "mt",
                            "sa",
                            "lb",
                            "my",
                            "bo",
                            "tl",
                            "mg",
                            "as",
                            "tt",
                            "haw",
                            "ln",
                            "ha",
                            "ba",
                            "jw",
                            "su"
                    ),
            recommendedUseCase = "High-quality multilingual transcription"
    ),
    MEDIUM(
            id = "medium",
            displayName = "Medium",
            description =
                    "High accuracy model with slower processing. Best for quality-focused use cases.",
            size = ModelSize.MEDIUM,
            fileSizeBytes = 1_420_000_000L, // ~1.42GB
            downloadUrl =
                    "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-medium.bin",
            version = "1.0.0",
            checksum = "fd9727b6e1217c2f614f9b698455c4ffd82463b4",
            isMultilingual = true,
            supportedLanguages =
                    listOf(
                            "en",
                            "zh",
                            "de",
                            "es",
                            "ru",
                            "ko",
                            "fr",
                            "ja",
                            "pt",
                            "tr",
                            "pl",
                            "ca",
                            "nl",
                            "ar",
                            "sv",
                            "it",
                            "id",
                            "hi",
                            "fi",
                            "vi",
                            "he",
                            "uk",
                            "el",
                            "ms",
                            "cs",
                            "ro",
                            "da",
                            "hu",
                            "ta",
                            "no",
                            "th",
                            "ur",
                            "hr",
                            "bg",
                            "lt",
                            "la",
                            "mi",
                            "ml",
                            "cy",
                            "sk",
                            "te",
                            "fa",
                            "lv",
                            "bn",
                            "sr",
                            "az",
                            "sl",
                            "kn",
                            "et",
                            "mk",
                            "br",
                            "eu",
                            "is",
                            "hy",
                            "ne",
                            "mn",
                            "bs",
                            "kk",
                            "sq",
                            "sw",
                            "gl",
                            "mr",
                            "pa",
                            "si",
                            "km",
                            "sn",
                            "yo",
                            "so",
                            "af",
                            "oc",
                            "ka",
                            "be",
                            "tg",
                            "sd",
                            "gu",
                            "am",
                            "yi",
                            "lo",
                            "uz",
                            "fo",
                            "ht",
                            "ps",
                            "tk",
                            "nn",
                            "mt",
                            "sa",
                            "lb",
                            "my",
                            "bo",
                            "tl",
                            "mg",
                            "as",
                            "tt",
                            "haw",
                            "ln",
                            "ha",
                            "ba",
                            "jw",
                            "su"
                    ),
            recommendedUseCase = "Professional multilingual transcription"
    ),
    LARGE(
            id = "large-v2",
            displayName = "Large",
            description =
                    "Highest accuracy model with slowest processing. Best for critical applications.",
            size = ModelSize.LARGE,
            fileSizeBytes = 2_900_000_000L, // ~2.9GB
            downloadUrl =
                    "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-large-v2.bin",
            version = "2.0.0",
            checksum = "0f4c8e34f21cf1a914c59d8b3ce882345ad349d6",
            isMultilingual = true,
            supportedLanguages =
                    listOf(
                            "en",
                            "zh",
                            "de",
                            "es",
                            "ru",
                            "ko",
                            "fr",
                            "ja",
                            "pt",
                            "tr",
                            "pl",
                            "ca",
                            "nl",
                            "ar",
                            "sv",
                            "it",
                            "id",
                            "hi",
                            "fi",
                            "vi",
                            "he",
                            "uk",
                            "el",
                            "ms",
                            "cs",
                            "ro",
                            "da",
                            "hu",
                            "ta",
                            "no",
                            "th",
                            "ur",
                            "hr",
                            "bg",
                            "lt",
                            "la",
                            "mi",
                            "ml",
                            "cy",
                            "sk",
                            "te",
                            "fa",
                            "lv",
                            "bn",
                            "sr",
                            "az",
                            "sl",
                            "kn",
                            "et",
                            "mk",
                            "br",
                            "eu",
                            "is",
                            "hy",
                            "ne",
                            "mn",
                            "bs",
                            "kk",
                            "sq",
                            "sw",
                            "gl",
                            "mr",
                            "pa",
                            "si",
                            "km",
                            "sn",
                            "yo",
                            "so",
                            "af",
                            "oc",
                            "ka",
                            "be",
                            "tg",
                            "sd",
                            "gu",
                            "am",
                            "yi",
                            "lo",
                            "uz",
                            "fo",
                            "ht",
                            "ps",
                            "tk",
                            "nn",
                            "mt",
                            "sa",
                            "lb",
                            "my",
                            "bo",
                            "tl",
                            "mg",
                            "as",
                            "tt",
                            "haw",
                            "ln",
                            "ha",
                            "ba",
                            "jw",
                            "su"
                    ),
            recommendedUseCase = "State-of-the-art multilingual transcription"
    );

    /** Get formatted file size string. */
    fun getFileSizeFormatted(): String {
        val df = DecimalFormat("#.#")
        return when {
            fileSizeBytes >= 1_000_000_000 -> "${df.format(fileSizeBytes / 1_000_000_000.0)} GB"
            fileSizeBytes >= 1_000_000 -> "${df.format(fileSizeBytes / 1_000_000.0)} MB"
            fileSizeBytes >= 1_000 -> "${df.format(fileSizeBytes / 1_000.0)} KB"
            else -> "$fileSizeBytes B"
        }
    }

    /** Get model status (will be set by ModelManager). */
    var status: ModelStatus = ModelStatus.NotDownloaded
        internal set

    /** Get local file path (will be set by ModelManager). */
    var localPath: String? = null
        internal set

    /** Get download timestamp (will be set by ModelManager). */
    var downloadedAt: Long? = null
        internal set

    /** Get last used timestamp (will be set by ModelManager). */
    var lastUsedAt: Long? = null
        internal set

    /** Get model metadata (will be set by ModelManager). */
    var metadata: ModelMetadata? = null
        internal set

    /** Check if model is currently selected. */
    var isCurrent: Boolean = false
        internal set

    /** Check if model supports a specific language. */
    fun supportsLanguage(languageCode: String): Boolean {
        return supportedLanguages.contains(languageCode.lowercase())
    }

    /** Check if model is available for use. */
    fun isAvailable(): Boolean {
        return status == ModelStatus.Available && localPath != null
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
    fun getQualityLevel(): QualityLevel =
            when (size) {
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
    fun getExpectedSpeed(): Float =
            when (size) {
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
    fun getRequiredMemoryMB(): Long =
            when (size) {
                ModelSize.TINY -> 64L
                ModelSize.BASE -> 128L
                ModelSize.SMALL -> 256L
                ModelSize.MEDIUM -> 512L
                ModelSize.LARGE -> 1024L
            }

    /**
     * Update status (modifies the enum instance).
     *
     * @param newStatus New model status
     * @param localPath Local file path (for downloaded models)
     */
    fun updateStatus(newStatus: ModelStatus, localPath: String? = null) {
        this.status = newStatus
        this.localPath = localPath
        if (newStatus == ModelStatus.Available) {
            this.downloadedAt = System.currentTimeMillis()
        }
    }

    /** Update last used timestamp (modifies the enum instance). */
    fun updateLastUsed() {
        this.lastUsedAt = System.currentTimeMillis()
    }

    companion object {
        /**
         * Get all available model presets.
         *
         * @return List of all available models
         */
        fun getAllModels(): List<WhisperModel> = listOf(TINY, BASE, SMALL, MEDIUM, LARGE)

        /**
         * Get the recommended model for a device with specific constraints.
         *
         * @param availableMemoryMB Available device memory in MB
         * @param preferSpeed Whether to prioritize speed over quality
         * @return Recommended model
         */
        fun getRecommended(availableMemoryMB: Long, preferSpeed: Boolean = false): WhisperModel {
            val models = getAllModels()
            return models.firstOrNull { it.isRecommendedFor(availableMemoryMB, preferSpeed) }
                    ?: TINY
        }

        /**
         * Get model by ID.
         *
         * @param id Model ID to search for
         * @return Model if found, null otherwise
         */
        fun getById(id: String): WhisperModel? {
            return getAllModels().find { it.id == id }
        }
    }
}

/** Enumeration of Whisper model sizes. */
enum class ModelSize {
    TINY,
    BASE,
    SMALL,
    MEDIUM,
    LARGE
}

/** Enumeration of model download/availability status. */
enum class ModelStatus {
    NotDownloaded,
    Downloading,
    Available,
    Error,
    Corrupted,
    Outdated
}

/** Enumeration of transcription quality levels. */
enum class QualityLevel {
    BASIC,
    GOOD,
    BETTER,
    HIGH,
    EXCELLENT
}

/** Additional metadata for a Whisper model. */
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
    fun getFormattedParameters(): String =
            when {
                parameters < 1_000_000 -> "${parameters / 1000}K"
                parameters < 1_000_000_000 -> "${parameters / 1_000_000}M"
                else -> String.format("%.1fB", parameters / 1_000_000_000.0)
            }
}
