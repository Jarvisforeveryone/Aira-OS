package com.example.presentation.device

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceUiState(
    val isFlashlightOn: Boolean = false,
    val isWifiOn: Boolean = true,
    val isBluetoothOn: Boolean = false,
    val volumeLevel: Int = 50,
    val brightnessLevel: Int = 70,
    val batteryLevel: Int = 100,
    val statusMessage: String = "Ready"
)

sealed class DeviceIntent {
    object ToggleFlashlight : DeviceIntent()
    data class SetVolume(val level: Int) : DeviceIntent()
    data class SetBrightness(val level: Int) : DeviceIntent()
    object RefreshStatus : DeviceIntent()
}

class DeviceViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceUiState())
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    fun processIntent(intent: DeviceIntent) {
        viewModelScope.launch {
            when (intent) {
                is DeviceIntent.ToggleFlashlight -> {
                    val newState = !_uiState.value.isFlashlightOn
                    _uiState.value = _uiState.value.copy(
                        isFlashlightOn = newState,
                        statusMessage = if (newState) "Flashlight turned on" else "Flashlight turned off"
                    )
                }
                is DeviceIntent.SetVolume -> {
                    _uiState.value = _uiState.value.copy(
                        volumeLevel = intent.level,
                        statusMessage = "Volume set to ${intent.level}%"
                    )
                }
                is DeviceIntent.SetBrightness -> {
                    _uiState.value = _uiState.value.copy(
                        brightnessLevel = intent.level,
                        statusMessage = "Brightness set to ${intent.level}%"
                    )
                }
                is DeviceIntent.RefreshStatus -> {
                    _uiState.value = _uiState.value.copy(statusMessage = "Device status refreshed")
                }
            }
        }
    }
}
