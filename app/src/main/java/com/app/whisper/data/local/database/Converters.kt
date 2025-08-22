package com.app.whisper.data.local.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room type converters for complex data types.
 * 
 * This class provides conversion methods for storing complex objects
 * as JSON strings in the SQLite database.
 */
class Converters {
    
    private val gson = Gson()
    
    /**
     * Convert List<String> to JSON string.
     * 
     * @param value List of strings
     * @return JSON string representation
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return if (value == null) null else gson.toJson(value)
    }
    
    /**
     * Convert JSON string to List<String>.
     * 
     * @param value JSON string
     * @return List of strings
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return if (value == null) {
            null
        } else {
            try {
                val listType = object : TypeToken<List<String>>() {}.type
                gson.fromJson(value, listType)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Convert Map<String, String> to JSON string.
     * 
     * @param value Map of string key-value pairs
     * @return JSON string representation
     */
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return if (value == null) null else gson.toJson(value)
    }
    
    /**
     * Convert JSON string to Map<String, String>.
     * 
     * @param value JSON string
     * @return Map of string key-value pairs
     */
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return if (value == null) {
            null
        } else {
            try {
                val mapType = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson(value, mapType)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Convert List<Float> to JSON string.
     * 
     * @param value List of floats
     * @return JSON string representation
     */
    @TypeConverter
    fun fromFloatList(value: List<Float>?): String? {
        return if (value == null) null else gson.toJson(value)
    }
    
    /**
     * Convert JSON string to List<Float>.
     * 
     * @param value JSON string
     * @return List of floats
     */
    @TypeConverter
    fun toFloatList(value: String?): List<Float>? {
        return if (value == null) {
            null
        } else {
            try {
                val listType = object : TypeToken<List<Float>>() {}.type
                gson.fromJson(value, listType)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Convert FloatArray to JSON string.
     * 
     * @param value Float array
     * @return JSON string representation
     */
    @TypeConverter
    fun fromFloatArray(value: FloatArray?): String? {
        return if (value == null) null else gson.toJson(value.toList())
    }
    
    /**
     * Convert JSON string to FloatArray.
     * 
     * @param value JSON string
     * @return Float array
     */
    @TypeConverter
    fun toFloatArray(value: String?): FloatArray? {
        return if (value == null) {
            null
        } else {
            try {
                val listType = object : TypeToken<List<Float>>() {}.type
                val list: List<Float> = gson.fromJson(value, listType)
                list.toFloatArray()
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Convert Map<String, Any> to JSON string.
     * 
     * @param value Map with string keys and any values
     * @return JSON string representation
     */
    @TypeConverter
    fun fromAnyMap(value: Map<String, Any>?): String? {
        return if (value == null) null else gson.toJson(value)
    }
    
    /**
     * Convert JSON string to Map<String, Any>.
     * 
     * @param value JSON string
     * @return Map with string keys and any values
     */
    @TypeConverter
    fun toAnyMap(value: String?): Map<String, Any>? {
        return if (value == null) {
            null
        } else {
            try {
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson(value, mapType)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Convert List<Int> to JSON string.
     * 
     * @param value List of integers
     * @return JSON string representation
     */
    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return if (value == null) null else gson.toJson(value)
    }
    
    /**
     * Convert JSON string to List<Int>.
     * 
     * @param value JSON string
     * @return List of integers
     */
    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        return if (value == null) {
            null
        } else {
            try {
                val listType = object : TypeToken<List<Int>>() {}.type
                gson.fromJson(value, listType)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Convert List<Long> to JSON string.
     * 
     * @param value List of longs
     * @return JSON string representation
     */
    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return if (value == null) null else gson.toJson(value)
    }
    
    /**
     * Convert JSON string to List<Long>.
     * 
     * @param value JSON string
     * @return List of longs
     */
    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        return if (value == null) {
            null
        } else {
            try {
                val listType = object : TypeToken<List<Long>>() {}.type
                gson.fromJson(value, listType)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Convert ByteArray to Base64 string.
     * 
     * @param value Byte array
     * @return Base64 encoded string
     */
    @TypeConverter
    fun fromByteArray(value: ByteArray?): String? {
        return if (value == null) {
            null
        } else {
            android.util.Base64.encodeToString(value, android.util.Base64.DEFAULT)
        }
    }
    
    /**
     * Convert Base64 string to ByteArray.
     * 
     * @param value Base64 encoded string
     * @return Byte array
     */
    @TypeConverter
    fun toByteArray(value: String?): ByteArray? {
        return if (value == null) {
            null
        } else {
            try {
                android.util.Base64.decode(value, android.util.Base64.DEFAULT)
            } catch (e: Exception) {
                null
            }
        }
    }
}
