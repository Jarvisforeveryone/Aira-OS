package com.example.presentation.wakeword

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TrainedWakeWord
import com.example.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WakeWordTrainerUiState(
    val isRecordingSample: Boolean = false,
    val recordedSamplesCount: Int = 0,
    val targetSamplesCount: Int = 5,
    val liveAmplitude: Float = 0f,
    val trainingStatusMessage: String = "Ready to record wake word samples",
    val testingResultText: String? = null,
    val isTesting: Boolean = false
)

/**
 * Feature ViewModel dedicated to custom wake word recording, amplitude monitoring,
 * neural training sample accumulation, and keyword detection testing.
 */
class WakeWordTrainerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val wakeWordDao = db.trainedWakeWordDao()

    val trainedWakeWords: StateFlow<List<TrainedWakeWord>> = wakeWordDao.getAllTrainedWakeWordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(WakeWordTrainerUiState())
    val uiState: StateFlow<WakeWordTrainerUiState> = _uiState.asStateFlow()

    fun startRecordingSample() {
        _uiState.value = _uiState.value.copy(
            isRecordingSample = true,
            trainingStatusMessage = "Listening... Say your custom wake word clearly."
        )
    }

    fun stopRecordingSample() {
        val currentCount = _uiState.value.recordedSamplesCount + 1
        _uiState.value = _uiState.value.copy(
            isRecordingSample = false,
            recordedSamplesCount = currentCount,
            trainingStatusMessage = if (currentCount >= _uiState.value.targetSamplesCount) {
                "Collected $currentCount samples! Ready to save profile."
            } else {
                "Sample $currentCount recorded. Please record ${ _uiState.value.targetSamplesCount - currentCount } more."
            }
        )
    }

    fun updateLiveAmplitude(amplitude: Float) {
        _uiState.value = _uiState.value.copy(liveAmplitude = amplitude)
    }

    fun saveWakeWordProfile(wordName: String) {
        if (wordName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val entity = TrainedWakeWord(
                word = wordName.trim().uppercase(),
                quality = "Good",
                attemptsJson = "[]",
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
            wakeWordDao.insertTrainedWakeWord(entity)
            Logger.d("WakeWordTrainerViewModel", "Saved custom wake word: $wordName")
            _uiState.value = _uiState.value.copy(
                recordedSamplesCount = 0,
                trainingStatusMessage = "Wake word '$wordName' profile saved successfully! ✅"
            )
        }
    }

    fun resetTraining() {
        _uiState.value = _uiState.value.copy(
            recordedSamplesCount = 0,
            isRecordingSample = false,
            trainingStatusMessage = "Training reset. Ready to record."
        )
    }

    fun deleteWakeWord(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            wakeWordDao.deleteById(id)
            Logger.d("WakeWordTrainerViewModel", "Deleted wake word profile $id")
        }
    }
}
