package com.example.domain.usecases

import com.example.domain.Result

class GetWeatherUseCase {
    suspend operator fun invoke(location: String): Result<String> {
        return try {
            val weatherInfo = "Current weather in $location: 72°F, Clear skies"
            Result.Success(weatherInfo)
        } catch (e: Exception) {
            Result.Error(e, "Failed to fetch weather data")
        }
    }
}
