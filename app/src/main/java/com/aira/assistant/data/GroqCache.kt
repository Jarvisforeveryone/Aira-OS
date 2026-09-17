package com.aira.assistant.data

import androidx.room.*

@Entity(tableName = "groq_cache")
data class GroqCache(
    @PrimaryKey val query: String,
    @ColumnInfo(name = "response") val response: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface GroqCacheDao {
    @Query("SELECT * FROM groq_cache WHERE query = :query LIMIT 1")
    suspend fun getCacheForQuery(query: String): GroqCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: GroqCache)

    @Query("DELETE FROM groq_cache WHERE query = :query")
    suspend fun deleteCache(query: String)

    @Query("DELETE FROM groq_cache WHERE timestamp < :expireTime")
    suspend fun clearExpiredCaches(expireTime: Long)
}
