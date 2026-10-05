package com.bilal.humanactivityrecognition.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilal.humanactivityrecognition.AppContainer
import com.bilal.humanactivityrecognition.domain.model.DailyActivitySummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class StatisticsViewModel(container: AppContainer) : ViewModel() {
    /** Last 7 days (oldest first); the last element is today. */
    val week: StateFlow<List<DailyActivitySummary>> = container.activityRepository
        .dailySummaries(LocalDate.now(), 7)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
