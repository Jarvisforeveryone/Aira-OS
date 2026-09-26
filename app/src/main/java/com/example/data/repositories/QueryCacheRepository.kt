package com.example.data.repositories

import com.example.data.QueryCache
import com.example.data.QueryCacheDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository interface providing cached access to frequent and historical AI queries.
 */
interface QueryCacheRepository {
    suspend fun getCachedResponse(query: String, maxAgeMs: Long = QueryCache.DEFAULT_TTL_MS): String?
    suspend fun cacheResponse(query: String, response: String, provider: String = "ai_provider")
    fun observeFrequentlyAskedQueries(limit: Int = 20): Flow<List<QueryCache>>
    suspend fun getTopQueries(limit: Int = 20): List<QueryCache>
    suspend fun removeQuery(query: String)
    suspend fun cleanExpired(maxAgeMs: Long = QueryCache.DEFAULT_TTL_MS)
    suspend fun clearAll()
}

/**
 * Room-backed implementation of QueryCacheRepository.
 */
class QueryCacheRepositoryImpl(
    private val queryCacheDao: QueryCacheDao
) : QueryCacheRepository {

    override suspend fun getCachedResponse(query: String, maxAgeMs: Long): String? = withContext(Dispatchers.IO) {
        val normalized = QueryCache.normalize(query)
        if (normalized.isBlank()) return@withContext null

        val entry = queryCacheDao.getCacheForQuery(normalized) ?: return@withContext null

        if (entry.isExpired(maxAgeMs)) {
            queryCacheDao.deleteCache(normalized)
            null
        } else {
            // Increment hit counter and update access timestamp
            queryCacheDao.incrementHitCount(normalized, System.currentTimeMillis())
            entry.response
        }
    }

    override suspend fun cacheResponse(query: String, response: String, provider: String) = withContext(Dispatchers.IO) {
        val normalized = QueryCache.normalize(query)
        if (normalized.isBlank() || response.isBlank()) return@withContext

        val existing = queryCacheDao.getCacheForQuery(normalized)
        val hitCount = (existing?.hitCount ?: 0) + 1

        val entry = QueryCache(
            normalizedQuery = normalized,
            originalQuery = query.trim(),
            response = response.trim(),
            provider = provider,
            hitCount = hitCount,
            timestamp = System.currentTimeMillis(),
            lastAccessed = System.currentTimeMillis()
        )
        queryCacheDao.insertCache(entry)
    }

    override fun observeFrequentlyAskedQueries(limit: Int): Flow<List<QueryCache>> {
        return queryCacheDao.observeFrequentlyAskedQueries(limit)
    }

    override suspend fun getTopQueries(limit: Int): List<QueryCache> = withContext(Dispatchers.IO) {
        queryCacheDao.getTopQueries(limit)
    }

    override suspend fun removeQuery(query: String) = withContext(Dispatchers.IO) {
        val normalized = QueryCache.normalize(query)
        queryCacheDao.deleteCache(normalized)
    }

    override suspend fun cleanExpired(maxAgeMs: Long) = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - maxAgeMs
        queryCacheDao.deleteExpired(threshold)
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        queryCacheDao.clearAll()
    }
}
