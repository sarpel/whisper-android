package com.app.whisper.data.local.database

// Temporarily disabled Room for build fix
// import androidx.room.Database
// import androidx.room.Room
// import androidx.room.RoomDatabase
// import androidx.room.TypeConverters
// import androidx.room.migration.Migration
// import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
// import com.app.whisper.data.local.database.converter.Converters
// import com.app.whisper.data.local.database.dao.ModelDao
// import com.app.whisper.data.local.database.dao.TranscriptionDao
// import com.app.whisper.data.local.database.entity.TranscriptionEntity
// import com.app.whisper.data.local.database.entity.TranscriptionSessionEntity
// import com.app.whisper.data.local.database.entity.WhisperModelEntity

/**
 * Room database for the Whisper Android application.
 *
 * This database stores transcription results, sessions, and model information
 * locally on the device using SQLite.
 *
 * Temporarily disabled for build fix - will be re-enabled after Room setup is complete.
 */
/*
@Database(
    entities = [
        TranscriptionEntity::class,
        TranscriptionSessionEntity::class,
        WhisperModelEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WhisperDatabase : RoomDatabase() {

    abstract fun transcriptionDao(): TranscriptionDao
    abstract fun modelDao(): ModelDao

    companion object {
        private const val DATABASE_NAME = "whisper_database"

        @Volatile
        private var INSTANCE: WhisperDatabase? = null

        /**
         * Get the singleton database instance.
         *
         * @param context Application context
         * @return WhisperDatabase instance
         */
        fun getDatabase(context: Context): WhisperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhisperDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(*getAllMigrations())
                    .addCallback(DatabaseCallback())
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Get all database migrations.
         *
         * @return Array of migrations
         */
        private fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                // Future migrations will be added here
            )
        }

        /**
         * Clear the database instance (for testing).
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }

    /**
     * Database callback for initialization and other events.
     */
    private class DatabaseCallback : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Database created for the first time
            // Add any initial setup here if needed
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Database opened
            // Add any setup that should run every time the database is opened
        }
    }
}

/**
 * Database migration from version 1 to 2 (example for future use).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Example migration - add new column to transcriptions table
        // database.execSQL("ALTER TABLE transcriptions ADD COLUMN new_column TEXT")
    }
}

/**
 * Database migration from version 2 to 3 (example for future use).
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Example migration - create new table
        // database.execSQL("CREATE TABLE new_table (id TEXT PRIMARY KEY NOT NULL, data TEXT)")
    }
}
*/

// Placeholder class for build fix
class WhisperDatabase {
    companion object {
        fun getDatabase(context: Context): WhisperDatabase = WhisperDatabase()
    }
}
