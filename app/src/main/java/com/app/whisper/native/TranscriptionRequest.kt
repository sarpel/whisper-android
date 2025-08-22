package com.app.whisper.native

/**
 * Data class representing a transcription request with all necessary parameters.
 * 
 * This class encapsulates all the parameters needed for a Whisper transcription
 * operation, providing type safety and validation for transcription requests.
 */
data class TranscriptionRequest(
    val audioData: FloatArray,
    val language: String = "auto",
    val translate: Boolean = false,
    val sampleRate: Int = 16000,
    val enableTimestamps: Boolean = false,
    val enableWordTimestamps: Boolean = false,
    val maxTokens: Int = -1, // -1 means no limit
    val temperature: Float = 0.0f,
    val compressionRatioThreshold: Float = 2.4f,
    val logProbThreshold: Float = -1.0f,
    val noSpeechThreshold: Float = 0.6f
) {
    
    /**
     * Validate the transcription request parameters.
     * 
     * @return Result indicating validation success or failure with details
     */
    fun validate(): Result<Unit> {
        return when {
            audioData.isEmpty() -> 
                Result.failure(IllegalArgumentException("Audio data is empty"))
            
            audioData.size < 1600 -> // 0.1 seconds at 16kHz
                Result.failure(IllegalArgumentException(
                    "Audio too short: ${audioData.size} samples (minimum: 1600)"
                ))
            
            audioData.size > 480000 -> // 30 seconds at 16kHz
                Result.failure(IllegalArgumentException(
                    "Audio too long: ${audioData.size} samples (maximum: 480000)"
                ))
            
            sampleRate <= 0 -> 
                Result.failure(IllegalArgumentException("Invalid sample rate: $sampleRate"))
            
            temperature < 0.0f || temperature > 1.0f -> 
                Result.failure(IllegalArgumentException(
                    "Temperature must be between 0.0 and 1.0: $temperature"
                ))
            
            compressionRatioThreshold < 1.0f -> 
                Result.failure(IllegalArgumentException(
                    "Compression ratio threshold must be >= 1.0: $compressionRatioThreshold"
                ))
            
            noSpeechThreshold < 0.0f || noSpeechThreshold > 1.0f -> 
                Result.failure(IllegalArgumentException(
                    "No speech threshold must be between 0.0 and 1.0: $noSpeechThreshold"
                ))
            
            audioData.all { it == 0.0f } -> 
                Result.failure(IllegalArgumentException("Audio data contains only silence"))
            
            audioData.any { it.isNaN() || it.isInfinite() } -> 
                Result.failure(IllegalArgumentException("Audio data contains invalid values"))
            
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Get the duration of the audio in seconds.
     * 
     * @return Audio duration in seconds
     */
    fun getDurationSeconds(): Float = audioData.size.toFloat() / sampleRate
    
    /**
     * Get a summary of the request parameters.
     * 
     * @return Human-readable summary string
     */
    fun getSummary(): String = buildString {
        append("TranscriptionRequest(")
        append("duration=${String.format("%.2f", getDurationSeconds())}s, ")
        append("samples=${audioData.size}, ")
        append("sampleRate=${sampleRate}Hz, ")
        append("language=$language")
        if (translate) append(", translate=true")
        if (enableTimestamps) append(", timestamps=true")
        if (enableWordTimestamps) append(", wordTimestamps=true")
        append(")")
    }
    
    /**
     * Check if the request is for a supported language.
     * 
     * @return true if language is supported
     */
    fun isSupportedLanguage(): Boolean = when (language.lowercase()) {
        "auto", "en", "tr", "de", "fr", "es", "it", "pt", "ru", "ja", "ko", "zh" -> true
        else -> false
    }
    
    /**
     * Create a copy with modified parameters.
     * 
     * @param language New language code
     * @param translate New translate flag
     * @param enableTimestamps New timestamps flag
     * @return Modified copy of the request
     */
    fun withOptions(
        language: String = this.language,
        translate: Boolean = this.translate,
        enableTimestamps: Boolean = this.enableTimestamps
    ): TranscriptionRequest = copy(
        language = language,
        translate = translate,
        enableTimestamps = enableTimestamps
    )
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as TranscriptionRequest
        
        if (!audioData.contentEquals(other.audioData)) return false
        if (language != other.language) return false
        if (translate != other.translate) return false
        if (sampleRate != other.sampleRate) return false
        if (enableTimestamps != other.enableTimestamps) return false
        if (enableWordTimestamps != other.enableWordTimestamps) return false
        if (maxTokens != other.maxTokens) return false
        if (temperature != other.temperature) return false
        if (compressionRatioThreshold != other.compressionRatioThreshold) return false
        if (logProbThreshold != other.logProbThreshold) return false
        if (noSpeechThreshold != other.noSpeechThreshold) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = audioData.contentHashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + translate.hashCode()
        result = 31 * result + sampleRate
        result = 31 * result + enableTimestamps.hashCode()
        result = 31 * result + enableWordTimestamps.hashCode()
        result = 31 * result + maxTokens
        result = 31 * result + temperature.hashCode()
        result = 31 * result + compressionRatioThreshold.hashCode()
        result = 31 * result + logProbThreshold.hashCode()
        result = 31 * result + noSpeechThreshold.hashCode()
        return result
    }
    
    companion object {
        /**
         * Create a simple transcription request with minimal parameters.
         * 
         * @param audioData Audio samples
         * @param language Language code (default: "auto")
         * @return TranscriptionRequest instance
         */
        fun simple(
            audioData: FloatArray,
            language: String = "auto"
        ): TranscriptionRequest = TranscriptionRequest(
            audioData = audioData,
            language = language
        )
        
        /**
         * Create a transcription request with translation enabled.
         * 
         * @param audioData Audio samples
         * @param sourceLanguage Source language code
         * @return TranscriptionRequest instance with translation enabled
         */
        fun withTranslation(
            audioData: FloatArray,
            sourceLanguage: String = "auto"
        ): TranscriptionRequest = TranscriptionRequest(
            audioData = audioData,
            language = sourceLanguage,
            translate = true
        )
        
        /**
         * Create a transcription request with timestamps enabled.
         * 
         * @param audioData Audio samples
         * @param language Language code
         * @param wordLevel Whether to enable word-level timestamps
         * @return TranscriptionRequest instance with timestamps enabled
         */
        fun withTimestamps(
            audioData: FloatArray,
            language: String = "auto",
            wordLevel: Boolean = false
        ): TranscriptionRequest = TranscriptionRequest(
            audioData = audioData,
            language = language,
            enableTimestamps = true,
            enableWordTimestamps = wordLevel
        )
    }
}
