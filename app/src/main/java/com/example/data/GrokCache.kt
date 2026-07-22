package com.example.data

import androidx.room.*

@Entity(tableName = "grok_caches")
data class GrokCache(
    @PrimaryKey val query: String,
    @ColumnInfo(name = "response") val response: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface GrokCacheDao {
    @Query("SELECT * FROM grok_caches WHERE query = :query LIMIT 1")
    suspend fun getCacheForQuery(query: String): GrokCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: GrokCache)

    @Query("DELETE FROM grok_caches WHERE query = :query")
    suspend fun deleteCache(query: String)

    @Query("DELETE FROM grok_caches WHERE timestamp < :expireTime")
    suspend fun clearExpiredCaches(expireTime: Long)
}
