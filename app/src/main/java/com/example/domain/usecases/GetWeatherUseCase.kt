package com.example.domain.usecases

import com.example.data.WeatherCache
import com.example.data.repositories.WeatherCacheRepository
import com.example.domain.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class GetWeatherUseCase(
    private val client: OkHttpClient = OkHttpClient(),
    private val weatherCacheRepository: WeatherCacheRepository? = null
) {
    suspend operator fun invoke(location: String, forceRefresh: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        val queryLocation = location.ifBlank { "San Francisco" }
        val locationKey = queryLocation.trim().lowercase()

        // 1. Check local Room cache first if forceRefresh is false
        if (!forceRefresh && weatherCacheRepository != null) {
            try {
                val cached = weatherCacheRepository.getCachedWeather(locationKey)
                    ?: if (locationKey == "local" || locationKey == "current") weatherCacheRepository.getLatestCachedWeather() else null
                if (cached != null) {
                    return@withContext Result.Success(cached.formattedText)
                }
            } catch (e: Exception) {
                // Ignore cache read errors and fall through to network
            }
        }

        try {
            var lat = 37.7749
            var lon = -122.4194
            var resolvedName = queryLocation

            if (queryLocation.equals("Local", ignoreCase = true) || queryLocation.equals("Current", ignoreCase = true)) {
                resolvedName = "Current Location"
            } else {
                val geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(queryLocation, "UTF-8")}&count=1&language=en&format=json"
                val geoRequest = Request.Builder().url(geocodingUrl).build()
                client.newCall(geoRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val json = JSONObject(body)
                            val results = json.optJSONArray("results")
                            if (results != null && results.length() > 0) {
                                val first = results.getJSONObject(0)
                                lat = first.getDouble("latitude")
                                lon = first.getDouble("longitude")
                                resolvedName = first.optString("name", queryLocation)
                            }
                        }
                    }
                }
            }

            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
            val request = Request.Builder().url(weatherUrl).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        val json = JSONObject(body)
                        val current = json.getJSONObject("current_weather")
                        val temp = current.getDouble("temperature")
                        val wind = current.getDouble("windspeed")
                        val weatherCode = current.optInt("weathercode", 0)
                        val condition = mapWeatherCode(weatherCode)

                        var forecastStr = ""
                        val daily = json.optJSONObject("daily")
                        if (daily != null) {
                            val maxTemps = daily.optJSONArray("temperature_2m_max")
                            val minTemps = daily.optJSONArray("temperature_2m_min")
                            if (maxTemps != null && minTemps != null && maxTemps.length() > 0 && minTemps.length() > 0) {
                                val max = maxTemps.getDouble(0)
                                val min = minTemps.getDouble(0)
                                forecastStr = " • High ${max.toInt()}°C / Low ${min.toInt()}°C"
                            }
                        }

                        val resultText = "$resolvedName: ${temp.toInt()}°C, $condition • Wind ${wind.toInt()} km/h$forecastStr"

                        // 2. Persist fresh network observation to Room cache
                        weatherCacheRepository?.let { repo ->
                            try {
                                val cacheEntry = WeatherCache(
                                    locationKey = locationKey,
                                    locationName = resolvedName,
                                    latitude = lat,
                                    longitude = lon,
                                    temperatureC = temp,
                                    windSpeedKmH = wind,
                                    weatherCode = weatherCode,
                                    conditionDescription = condition,
                                    formattedText = resultText,
                                    forecastStr = forecastStr,
                                    timestamp = System.currentTimeMillis()
                                )
                                repo.saveWeather(cacheEntry)
                            } catch (e: Exception) {
                                // Non-fatal cache write failure
                            }
                        }

                        return@withContext Result.Success(resultText)
                    }
                }
            }

            // Fallback to cached value if network response was not successful
            weatherCacheRepository?.getCachedWeather(locationKey)?.let { staleCache ->
                return@withContext Result.Success(staleCache.formattedText)
            }

            Result.Success("$resolvedName: 20°C, Clear skies")
        } catch (e: Exception) {
            // If offline, attempt to serve last known cache before returning error
            val lastKnown = weatherCacheRepository?.getLatestCachedWeather(maxAgeMs = Long.MAX_VALUE)
            if (lastKnown != null) {
                Result.Success(lastKnown.formattedText)
            } else {
                Result.Error(e, "Failed to fetch weather data: ${e.localizedMessage}")
            }
        }
    }

    private fun mapWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            80, 81, 82 -> "Rain Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Partly Cloudy"
        }
    }
}

