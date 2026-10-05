package com.bilal.humanactivityrecognition.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilal.humanactivityrecognition.AppContainer
import com.bilal.humanactivityrecognition.data.repository.ActivityRepository
import com.bilal.humanactivityrecognition.data.repository.LiveRecognitionState
import com.bilal.humanactivityrecognition.domain.model.DailyActivitySummary
import com.bilal.humanactivityrecognition.domain.model.InputSource
import com.bilal.humanactivityrecognition.service.ActivityRecognitionService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    val live: StateFlow<LiveRecognitionState> = container.activityRepository.live

    val today: StateFlow<DailyActivitySummary> = container.activityRepository
        .dailySummaries(LocalDate.now(), 1)
        .map { it.first() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityRepository.summarize(LocalDate.now(), emptyList()))

    val inputSource: StateFlow<InputSource> = container.settingsRepository.settings
        .map { it.inputSource }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InputSource.PHONE_SENSORS)

    fun stop() = ActivityRecognitionService.stop(container.appContext)
}
