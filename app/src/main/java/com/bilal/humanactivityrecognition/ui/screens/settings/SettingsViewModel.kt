package com.bilal.humanactivityrecognition.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilal.humanactivityrecognition.AppContainer
import com.bilal.humanactivityrecognition.data.sensor.SensorStatus
import com.bilal.humanactivityrecognition.domain.model.AppSettings
import com.bilal.humanactivityrecognition.domain.model.InputSource
import com.bilal.humanactivityrecognition.domain.model.RecognitionFrequency
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.settingsRepository

    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val sensorStatus: StateFlow<SensorStatus> = container.sensorManager.status
    val modelInfo: Map<String, String> = container.modelInfo

    fun setFrequency(value: RecognitionFrequency) = viewModelScope.launch { repo.setFrequency(value) }
    fun setInputSource(value: InputSource) = viewModelScope.launch { repo.setInputSource(value) }
    fun setSmoothing(value: Int) = viewModelScope.launch { repo.setSmoothingWindow(value) }
    fun setNotifications(value: Boolean) = viewModelScope.launch { repo.setNotificationsEnabled(value) }
}
