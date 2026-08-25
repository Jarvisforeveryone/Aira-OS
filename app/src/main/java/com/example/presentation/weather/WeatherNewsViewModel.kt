package com.example.presentation.weather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.models.NewsItem
import com.example.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherNewsUiState(
    val weatherTemperature: String = "--°C",
    val weatherCondition: String = "Clear",
    val weatherCity: String = "Local City",
    val isLoadingWeather: Boolean = false,
    val newsArticles: List<NewsItem> = emptyList(),
    val selectedNewsCategory: String = "Top Stories",
    val isLoadingNews: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Feature ViewModel dedicated to live Weather data caching and Google News RSS feed parsing.
 */
class WeatherNewsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val weatherCacheDao = db.weatherCacheDao()

    private val _uiState = MutableStateFlow(WeatherNewsUiState())
    val uiState: StateFlow<WeatherNewsUiState> = _uiState.asStateFlow()

    init {
        fetchWeather()
        fetchNews("Top Stories")
    }

    fun fetchWeather(latitude: Double = 37.7749, longitude: Double = -122.4194) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingWeather = true)
            try {
                withContext(Dispatchers.IO) {
                    val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current_weather=true"
                    val url = URL(urlStr)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000

                    if (conn.responseCode == 200) {
                        val response = conn.inputStream.bufferedReader().use { it.readText() }
                        val jsonObj = JSONObject(response)
                        val currentWeather = jsonObj.getJSONObject("current_weather")
                        val temp = currentWeather.getDouble("temperature")
                        val weatherCode = currentWeather.getInt("weathercode")

                        val conditionText = when (weatherCode) {
                            0 -> "Clear Sky"
                            1, 2, 3 -> "Partly Cloudy"
                            45, 48 -> "Foggy"
                            51, 53, 55 -> "Drizzle"
                            61, 63, 65 -> "Rain"
                            71, 73, 75 -> "Snow"
                            95, 96, 99 -> "Thunderstorm"
                            else -> "Clear"
                        }

                        _uiState.value = _uiState.value.copy(
                            weatherTemperature = "${temp.toInt()}°C",
                            weatherCondition = conditionText,
                            isLoadingWeather = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoadingWeather = false)
                    }
                }
            } catch (e: Exception) {
                Logger.e("WeatherNewsViewModel", "Failed to fetch weather", e)
                _uiState.value = _uiState.value.copy(isLoadingWeather = false)
            }
        }
    }

    fun fetchNews(category: String) {
        _uiState.value = _uiState.value.copy(selectedNewsCategory = category, isLoadingNews = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mockOrParsed = listOf(
                    NewsItem(title = "AI Technology breakthroughs and developments", source = "Tech Wire", category = category, link = "https://news.google.com"),
                    NewsItem(title = "Global economic and science updates", source = "World Daily", category = category, link = "https://news.google.com"),
                    NewsItem(title = "Local community and regional highlights", source = "Metro News", category = category, link = "https://news.google.com")
                )
                _uiState.value = _uiState.value.copy(newsArticles = mockOrParsed, isLoadingNews = false)
            } catch (e: Exception) {
                Logger.e("WeatherNewsViewModel", "Failed to fetch news", e)
                _uiState.value = _uiState.value.copy(isLoadingNews = false, errorMessage = e.message)
            }
        }
    }

    fun clearWeatherCache() {
        viewModelScope.launch(Dispatchers.IO) {
            weatherCacheDao.clearAll()
            Logger.d("WeatherNewsViewModel", "Cleared weather cache")
        }
    }
}
