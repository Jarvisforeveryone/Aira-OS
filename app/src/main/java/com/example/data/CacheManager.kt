package com.example.data

import android.content.Context
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified CacheManager for AIRA OS.
 * Combines high-speed in-memory LRU cache with SQLite database persistence
 * for LLM queries, offline weather, and system answers.
 */
class CacheManager private constructor(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val memoryCache = LruCache<String, String>(100) // 100 recent responses

    companion object {
        @Volatile
        private var instance: CacheManager? = null

        fun getInstance(context: Context): CacheManager {
            return instance ?: synchronized(this) {
                instance ?: CacheManager(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun getCachedAiResponse(prompt: String): String? = withContext(Dispatchers.IO) {
        val normalized = QueryCache.normalize(prompt)
        // 1. Check in-memory cache
        memoryCache.get(normalized)?.let { return@withContext it }

        // 2. Check disk / Room cache
        try {
            val cached = db.queryCacheDao().getCacheForQuery(normalized)
            if (cached != null && !cached.isExpired()) {
                db.queryCacheDao().incrementHitCount(normalized)
                memoryCache.put(normalized, cached.response)
                return@withContext cached.response
            }
        } catch (e: Exception) {
            // Ignore cache read failures
        }
        null
    }

    suspend fun putCachedAiResponse(prompt: String, response: String, provider: String = "ai_provider") = withContext(Dispatchers.IO) {
        if (response.isBlank()) return@withContext
        val normalized = QueryCache.normalize(prompt)
        memoryCache.put(normalized, response)
        try {
            db.queryCacheDao().insertCache(
                QueryCache(
                    normalizedQuery = normalized,
                    originalQuery = prompt.trim(),
                    response = response,
                    provider = provider,
                    hitCount = 1,
                    timestamp = System.currentTimeMillis(),
                    lastAccessed = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            // Ignore cache write failures
        }
    }

    fun clearMemoryCache() {
        memoryCache.evictAll()
    }
}
