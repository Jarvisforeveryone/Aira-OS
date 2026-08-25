package com.example.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Single source of truth for Room Database schema documentation, table names,
 * and migration definitions in AIRA OS.
 */
object DatabaseSchema {

    const val DATABASE_NAME = "aira_database"
    const val DATABASE_VERSION = 10

    object Tables {
        const val CHAT_MESSAGES = "chat_messages"
        const val REMINDERS = "reminders"
        const val GROK_CACHE = "grok_caches"
        const val ACTIONS = "actions"
        const val COMMANDS = "commands"
        const val MEMORIES = "Memory"
        const val TRAINED_WAKE_WORDS = "trained_wake_words"
        const val RESPONSE_FEEDBACK = "response_feedback"
        const val VOICE_COMMAND_LOGS = "voice_command_logs"
        const val MACRO_TEMPLATES = "macro_templates"
        const val WEATHER_CACHE = "weather_cache"
        const val QUERY_CACHE = "query_cache"
    }

    /**
     * Helper to create all database tables if missing
     */
    private fun createTablesIfNotExist(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.CHAT_MESSAGES}` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sender` TEXT NOT NULL,
                `message` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `is_offline` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.REMINDERS}` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `time_label` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `is_completed` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.GROK_CACHE}` (
                `query` TEXT PRIMARY KEY NOT NULL,
                `response` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.ACTIONS}` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `params_json` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.COMMANDS}` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `trigger_phrase` TEXT NOT NULL,
                `action_ids_json` TEXT NOT NULL,
                `priority` INTEGER NOT NULL DEFAULT 1,
                `conditions_json` TEXT NOT NULL DEFAULT '',
                `use_count` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.MEMORIES}` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `factText` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `category` TEXT NOT NULL DEFAULT 'Personal',
                `isImportant` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.TRAINED_WAKE_WORDS}` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `word` TEXT NOT NULL,
                `quality` TEXT NOT NULL,
                `attempts_json` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL DEFAULT 0,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.RESPONSE_FEEDBACK}` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `message_id` INTEGER,
                `query` TEXT NOT NULL,
                `response` TEXT NOT NULL,
                `feedback_type` TEXT NOT NULL,
                `comment` TEXT,
                `timestamp` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.VOICE_COMMAND_LOGS}` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `command` TEXT NOT NULL,
                `matchedTrigger` TEXT,
                `timestamp` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `details` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.MACRO_TEMPLATES}` (
                `id` TEXT PRIMARY KEY NOT NULL,
                `trigger` TEXT NOT NULL,
                `actionsJson` TEXT NOT NULL,
                `description` TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.WEATHER_CACHE}` (
                `locationKey` TEXT PRIMARY KEY NOT NULL,
                `location_name` TEXT NOT NULL,
                `country` TEXT NOT NULL DEFAULT '',
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `temperature_c` REAL NOT NULL,
                `wind_speed_kmh` REAL NOT NULL,
                `wind_direction_deg` INTEGER NOT NULL DEFAULT 0,
                `weather_code` INTEGER NOT NULL DEFAULT 0,
                `condition_description` TEXT NOT NULL DEFAULT '',
                `is_daytime` INTEGER NOT NULL DEFAULT 1,
                `is_gps_location` INTEGER NOT NULL DEFAULT 0,
                `formatted_text` TEXT NOT NULL,
                `forecast_str` TEXT NOT NULL DEFAULT '',
                `timestamp` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_weather_cache_locationKey` ON `${Tables.WEATHER_CACHE}` (`locationKey`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_weather_cache_timestamp` ON `${Tables.WEATHER_CACHE}` (`timestamp`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `${Tables.QUERY_CACHE}` (
                `normalized_query` TEXT PRIMARY KEY NOT NULL,
                `original_query` TEXT NOT NULL,
                `response` TEXT NOT NULL,
                `provider` TEXT NOT NULL DEFAULT 'ai_provider',
                `hit_count` INTEGER NOT NULL DEFAULT 1,
                `timestamp` INTEGER NOT NULL,
                `last_accessed` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_query_cache_normalized_query` ON `${Tables.QUERY_CACHE}` (`normalized_query`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_query_cache_hit_count` ON `${Tables.QUERY_CACHE}` (`hit_count`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_query_cache_last_accessed` ON `${Tables.QUERY_CACHE}` (`last_accessed`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_query_cache_timestamp` ON `${Tables.QUERY_CACHE}` (`timestamp`)")
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createTablesIfNotExist(db)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createTablesIfNotExist(db)
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createTablesIfNotExist(db)
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createTablesIfNotExist(db)
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createTablesIfNotExist(db)
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createTablesIfNotExist(db)
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createTablesIfNotExist(db)
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createTablesIfNotExist(db)
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createTablesIfNotExist(db)
        }
    }

    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10
    )
}
