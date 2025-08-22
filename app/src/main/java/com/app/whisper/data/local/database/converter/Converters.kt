package com.app.whisper.data.local.database.converter

import androidx.room.TypeConverter
import com.app.whisper.domain.entity.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters for complex data types.
 * 
 * This class provides conversion methods for complex objects that need to be
 * stored in the SQLite database as JSON strings.
 */
class Converters {
    
    private val gson = Gson()
    
    // String List Converters
    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    // TranscriptionSegment List Converters
    @TypeConverter
    fun fromTranscriptionSegmentList(value: List<TranscriptionSegment>): String = gson.toJson(value)
    
    @TypeConverter
    fun toTranscriptionSegmentList(value: String): List<TranscriptionSegment> {
        val listType = object : TypeToken<List<TranscriptionSegment>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    // TranscriptionWord List Converters
    @TypeConverter
    fun fromTranscriptionWordList(value: List<TranscriptionWord>): String = gson.toJson(value)
    
    @TypeConverter
    fun toTranscriptionWordList(value: String): List<TranscriptionWord> {
        val listType = object : TypeToken<List<TranscriptionWord>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    // TranscriptionMetadata Converters
    @TypeConverter
    fun fromTranscriptionMetadata(value: TranscriptionMetadata): String = gson.toJson(value)
    
    @TypeConverter
    fun toTranscriptionMetadata(value: String): TranscriptionMetadata = 
        gson.fromJson(value, TranscriptionMetadata::class.java)
    
    // AudioMetadata Converters
    @TypeConverter
    fun fromAudioMetadata(value: AudioMetadata): String = gson.toJson(value)
    
    @TypeConverter
    fun toAudioMetadata(value: String): AudioMetadata = 
        gson.fromJson(value, AudioMetadata::class.java)
    
    // ProcessingParameters Converters
    @TypeConverter
    fun fromProcessingParameters(value: ProcessingParameters): String = gson.toJson(value)
    
    @TypeConverter
    fun toProcessingParameters(value: String): ProcessingParameters = 
        gson.fromJson(value, ProcessingParameters::class.java)
    
    // ModelMetadata Converters
    @TypeConverter
    fun fromModelMetadata(value: ModelMetadata): String = gson.toJson(value)
    
    @TypeConverter
    fun toModelMetadata(value: String): ModelMetadata = 
        gson.fromJson(value, ModelMetadata::class.java)
    
    // Enum Converters
    @TypeConverter
    fun fromSessionStatus(value: SessionStatus): String = value.name
    
    @TypeConverter
    fun toSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)
    
    @TypeConverter
    fun fromModelStatus(value: ModelStatus): String = value.name
    
    @TypeConverter
    fun toModelStatus(value: String): ModelStatus = ModelStatus.valueOf(value)
    
    @TypeConverter
    fun fromModelSize(value: ModelSize): String = value.name
    
    @TypeConverter
    fun toModelSize(value: String): ModelSize = ModelSize.valueOf(value)
    
    @TypeConverter
    fun fromQualityLevel(value: QualityLevel): String = value.name
    
    @TypeConverter
    fun toQualityLevel(value: String): QualityLevel = QualityLevel.valueOf(value)
}
