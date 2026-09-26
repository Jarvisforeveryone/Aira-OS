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
 * Room Entity representing locally cached weather observation and forecast.
 * Indexed by locationKey and timestamp to enable zero-latency offline lookups
 * and rapid expiration cleanup without full table scans.
 */
@Entity(
    tableName = "weather_cache",
    indices = [
        Index(value = ["locationKey"], unique = true),
        Index(value = ["timestamp"])
    ]
)
data class WeatherCache(
    @PrimaryKey 
    val locationKey: String,
    
    @ColumnInfo(name = "location_name")
    val locationName: String,
    
    @ColumnInfo(name = "country")
    val country: String = "",
    
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    
    @ColumnInfo(name = "temperature_c")
    val temperatureC: Double,
    
    @ColumnInfo(name = "wind_speed_kmh")
    val windSpeedKmH: Double,
    
    @ColumnInfo(name = "wind_direction_deg")
    val windDirectionDeg: Int = 0,
    
    @ColumnInfo(name = "weather_code")
    val weatherCode: Int = 0,
    
    @ColumnInfo(name = "condition_description")
    val conditionDescription: String = "",
    
    @ColumnInfo(name = "is_daytime")
    val isDaytime: Boolean = true,
    
    @ColumnInfo(name = "is_gps_location")
    val isGpsLocation: Boolean = false,
    
    @ColumnInfo(name = "formatted_text")
    val formattedText: String,
    
    @ColumnInfo(name = "forecast_str")
    val forecastStr: String = "",
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Checks if the cached weather is older than the given TTL in milliseconds.
     * Default TTL: 20 minutes (1,200,000 ms).
     */
    fun isExpired(ttlMs: Long = DEFAULT_TTL_MS): Boolean {
        return (System.currentTimeMillis() - timestamp) > ttlMs
    }

    companion object {
        const val DEFAULT_TTL_MS = 20 * 60 * 1000L // 20 minutes
    }
}

/**
 * Data Access Object for local Weather Cache operations in Room.
 */
@Dao
interface WeatherCacheDao {

    @Query("SELECT * FROM weather_cache WHERE LOWER(locationKey) = LOWER(:locationKey) LIMIT 1")
    suspend fun getWeatherByLocation(locationKey: String): WeatherCache?

    @Query("SELECT * FROM weather_cache ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestWeather(): WeatherCache?

    @Query("SELECT * FROM weather_cache ORDER BY timestamp DESC LIMIT 1")
    fun observeLatestWeather(): Flow<WeatherCache?>

    @Query("SELECT * FROM weather_cache WHERE LOWER(locationKey) = LOWER(:locationKey) LIMIT 1")
    fun observeWeatherByLocation(locationKey: String): Flow<WeatherCache?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(cache: WeatherCache)

    @Query("DELETE FROM weather_cache WHERE LOWER(locationKey) = LOWER(:locationKey)")
    suspend fun deleteWeatherByLocation(locationKey: String)

    @Query("DELETE FROM weather_cache WHERE timestamp < :expireThreshold")
    suspend fun deleteExpiredWeather(expireThreshold: Long)

    @Query("DELETE FROM weather_cache")
    suspend fun clearAll()
}
