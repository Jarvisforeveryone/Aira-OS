package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room Entity for local caching of frequently asked queries and AI responses.
 * Tracks execution frequency (hitCount) and timestamps to prioritize high-frequency
 * queries and eliminate redundant network LLM calls.
 */
@Entity(
    tableName = "query_cache",
    indices = [
        Index(value = ["normalized_query"], unique = true),
        Index(value = ["hit_count"]),
        Index(value = ["last_accessed"]),
        Index(value = ["timestamp"])
    ]
)
data class QueryCache(
    @PrimaryKey
    @ColumnInfo(name = "normalized_query")
    val normalizedQuery: String,

    @ColumnInfo(name = "original_query")
    val originalQuery: String,

    @ColumnInfo(name = "response")
    val response: String,

    @ColumnInfo(name = "provider")
    val provider: String = "ai_provider",

    @ColumnInfo(name = "hit_count")
    val hitCount: Int = 1,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_accessed")
    val lastAccessed: Long = System.currentTimeMillis()
) {
    fun isExpired(ttlMs: Long = DEFAULT_TTL_MS): Boolean {
        return (System.currentTimeMillis() - timestamp) > ttlMs
    }

    companion object {
        const val DEFAULT_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

        fun normalize(query: String): String {
            return query.trim().lowercase().replace(Regex("\\s+"), " ")
        }
    }
}

/**
 * Data Access Object for local Query Cache operations in Room.
 */
@Dao
interface QueryCacheDao {

    @Query("SELECT * FROM query_cache WHERE normalized_query = :normalizedQuery LIMIT 1")
    suspend fun getCacheForQuery(normalizedQuery: String): QueryCache?

    @Query("SELECT * FROM query_cache ORDER BY hit_count DESC, last_accessed DESC LIMIT :limit")
    fun observeFrequentlyAskedQueries(limit: Int = 20): Flow<List<QueryCache>>

    @Query("SELECT * FROM query_cache ORDER BY hit_count DESC, last_accessed DESC LIMIT :limit")
    suspend fun getTopQueries(limit: Int = 20): List<QueryCache>

    @Query("SELECT * FROM query_cache ORDER BY last_accessed DESC LIMIT :limit")
    fun observeRecentQueries(limit: Int = 20): Flow<List<QueryCache>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: QueryCache)

    @Query("UPDATE query_cache SET hit_count = hit_count + 1, last_accessed = :accessTime WHERE normalized_query = :normalizedQuery")
    suspend fun incrementHitCount(normalizedQuery: String, accessTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM query_cache WHERE normalized_query = :normalizedQuery")
    suspend fun deleteCache(normalizedQuery: String)

    @Query("DELETE FROM query_cache WHERE timestamp < :expireThreshold")
    suspend fun deleteExpired(expireThreshold: Long)

    @Query("DELETE FROM query_cache")
    suspend fun clearAll()
}
