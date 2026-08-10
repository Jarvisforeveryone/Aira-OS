package com.example.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.di.AppModule
import com.example.domain.Result
import com.example.domain.models.Memory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val memories: List<Memory> = emptyList(),
    val weatherInfo: String = "Clear, 72°F",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appModule = AppModule(application)
    private val memoryRepository = appModule.memoryRepository
    private val getWeatherUseCase = appModule.getWeatherUseCase

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            launch {
                memoryRepository.getMemories().collect { memList ->
                    _uiState.value = _uiState.value.copy(
                        memories = memList,
                        isLoading = false
                    )
                }
            }

            launch {
                when (val weatherResult = getWeatherUseCase("Local")) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(weatherInfo = weatherResult.data)
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(errorMessage = weatherResult.message)
                    }
                    Result.Loading -> {}
                }
            }
        }
    }
}
