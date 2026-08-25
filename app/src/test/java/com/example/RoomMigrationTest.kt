package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigrationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addMigrations(*DatabaseSchema.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAllMigrationsArrayPresent() {
        assertEquals(9, DatabaseSchema.ALL_MIGRATIONS.size)
        assertEquals(1, DatabaseSchema.ALL_MIGRATIONS[0].startVersion)
        assertEquals(2, DatabaseSchema.ALL_MIGRATIONS[0].endVersion)
        assertEquals(9, DatabaseSchema.ALL_MIGRATIONS.last().startVersion)
        assertEquals(10, DatabaseSchema.ALL_MIGRATIONS.last().endVersion)
    }

    @Test
    fun testChatMessageEntityOperations() = runBlocking {
        val chatDao = db.chatMessageDao()
        val message = ChatMessage(
            id = 1,
            sender = "user",
            message = "Test message for Room migration",
            timestamp = 1700000000000L,
            isOffline = false
        )
        chatDao.insertMessage(message)

        val retrievedList = chatDao.getAllMessagesList()
        assertTrue(retrievedList.isNotEmpty())
        val retrieved = retrievedList.firstOrNull { it.id == 1L }
        assertNotNull(retrieved)
        assertEquals("user", retrieved?.sender)
        assertEquals("Test message for Room migration", retrieved?.message)
        assertEquals(1700000000000L, retrieved?.timestamp)
    }

    @Test
    fun testMemoryEntityOperations() = runBlocking {
        val memoryDao = db.memoryDao()
        val memory = Memory(
            id = 10,
            factText = "User lives in Seattle",
            source = "voice_command",
            createdAt = 1700000001000L,
            category = "Personal",
            isImportant = true
        )
        memoryDao.insertMemory(memory)

        val memories = memoryDao.getAllMemoriesList()
        assertTrue(memories.any { it.factText == "User lives in Seattle" && it.isImportant })
    }

    @Test
    fun testReminderEntityOperations() = runBlocking {
        val reminderDao = db.reminderDao()
        val reminder = Reminder(
            id = 5,
            title = "Doctor Appointment",
            timeLabel = "Tomorrow 10 AM",
            timestamp = 1700005000000L,
            isCompleted = false
        )
        val rowId = reminderDao.insertReminder(reminder)
        assertTrue(rowId > 0)
    }

    @Test
    fun testQueryCacheEntityOperations() = runBlocking {
        val cacheDao = db.queryCacheDao()
        val cacheEntry = QueryCache(
            normalizedQuery = "what is quantum computing",
            originalQuery = "What is quantum computing?",
            response = "Quantum computing is computation using quantum mechanics.",
            provider = "gemini",
            hitCount = 3,
            timestamp = 1700000000000L,
            lastAccessed = 1700000100000L
        )
        cacheDao.insertCache(cacheEntry)

        val cached = cacheDao.getCacheForQuery("what is quantum computing")
        assertNotNull(cached)
        assertEquals("Quantum computing is computation using quantum mechanics.", cached?.response)
        assertEquals(3, cached?.hitCount)
    }

    @Test
    fun testMacroTemplateEntityOperations() = runBlocking {
        val macroDao = db.macroDao()
        val macro = MacroEntity(
            id = "macro_morning_routine",
            trigger = "good morning",
            actionsJson = """[{"action":"say","param":"Good morning sir"}]""",
            description = "Morning assistant routine"
        )
        macroDao.insertMacro(macro)

        val retrieved = macroDao.getMacroByTrigger("good morning")
        assertNotNull(retrieved)
        assertEquals("good morning", retrieved?.trigger)
    }

    @Test
    fun testVoiceCommandLogEntityOperations() = runBlocking {
        val logDao = db.voiceCommandLogDao()
        val log = VoiceCommandLogEntity(
            id = "log_001",
            command = "turn on flashlight",
            matchedTrigger = "flashlight on",
            timestamp = "2026-08-25 10:00:00",
            status = "SUCCESS",
            details = "Torch activated"
        )
        logDao.insertLog(log)

        val allLogs = logDao.getRecentLogs()
        assertTrue(allLogs.any { it.id == "log_001" && it.status == "SUCCESS" })
    }

    @Test
    fun testWeatherCacheEntityOperations() = runBlocking {
        val weatherDao = db.weatherCacheDao()
        val weather = WeatherCache(
            locationKey = "loc_tokyo",
            locationName = "Tokyo",
            country = "Japan",
            latitude = 35.6762,
            longitude = 139.6503,
            temperatureC = 22.5,
            windSpeedKmH = 12.0,
            windDirectionDeg = 180,
            weatherCode = 800,
            conditionDescription = "Clear sky",
            isDaytime = true,
            isGpsLocation = false,
            formattedText = "22.5°C in Tokyo, Clear sky",
            forecastStr = "Sunny throughout the day",
            timestamp = 1700000000000L
        )
        weatherDao.insertWeather(weather)

        val retrieved = weatherDao.getWeatherByLocation("loc_tokyo")
        assertNotNull(retrieved)
        assertEquals(22.5, retrieved?.temperatureC ?: 0.0, 0.01)
    }

    @Test
    fun testResponseFeedbackEntityOperations() = runBlocking {
        val feedbackDao = db.responseFeedbackDao()
        val feedback = ResponseFeedback(
            id = 1,
            messageId = 10,
            query = "What is the capital of France?",
            response = "The capital of France is Paris.",
            feedbackType = "THUMBS_UP",
            comment = "Accurate and fast",
            timestamp = 1700000000000L
        )
        feedbackDao.insertFeedback(feedback)

        val retrieved = feedbackDao.getFeedbackForMessage(10)
        assertNotNull(retrieved)
        assertEquals("THUMBS_UP", retrieved?.feedbackType)
    }
}
