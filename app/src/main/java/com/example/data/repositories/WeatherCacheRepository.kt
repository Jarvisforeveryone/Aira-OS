package com.example.data.repositories

import com.example.data.WeatherCache
import com.example.data.WeatherCacheDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository interface providing cached and reactive access to local weather data.
 */
interface WeatherCacheRepository {
    suspend fun getCachedWeather(locationKey: String, maxAgeMs: Long = WeatherCache.DEFAULT_TTL_MS): WeatherCache?
    suspend fun getLatestCachedWeather(maxAgeMs: Long = WeatherCache.DEFAULT_TTL_MS): WeatherCache?
    fun observeLatestWeather(): Flow<WeatherCache?>
    fun observeWeather(locationKey: String): Flow<WeatherCache?>
    suspend fun saveWeather(weather: WeatherCache)
    suspend fun cleanExpired(maxAgeMs: Long = WeatherCache.DEFAULT_TTL_MS)
    suspend fun clearAll()
}

/**
 * Implementation of WeatherCacheRepository backed by Room Database.
 */
class WeatherCacheRepositoryImpl(
    private val weatherCacheDao: WeatherCacheDao
) : WeatherCacheRepository {

    override suspend fun getCachedWeather(locationKey: String, maxAgeMs: Long): WeatherCache? = withContext(Dispatchers.IO) {
        val cached = weatherCacheDao.getWeatherByLocation(locationKey)
        if (cached != null && !cached.isExpired(maxAgeMs)) {
            cached
        } else {
            null
        }
    }

    override suspend fun getLatestCachedWeather(maxAgeMs: Long): WeatherCache? = withContext(Dispatchers.IO) {
        val cached = weatherCacheDao.getLatestWeather()
        if (cached != null && !cached.isExpired(maxAgeMs)) {
            cached
        } else {
            null
        }
    }

    override fun observeLatestWeather(): Flow<WeatherCache?> {
        return weatherCacheDao.observeLatestWeather()
    }

    override fun observeWeather(locationKey: String): Flow<WeatherCache?> {
        return weatherCacheDao.observeWeatherByLocation(locationKey)
    }

    override suspend fun saveWeather(weather: WeatherCache) = withContext(Dispatchers.IO) {
        weatherCacheDao.insertWeather(weather)
    }

    override suspend fun cleanExpired(maxAgeMs: Long) = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - maxAgeMs
        weatherCacheDao.deleteExpiredWeather(threshold)
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        weatherCacheDao.clearAll()
    }
}
