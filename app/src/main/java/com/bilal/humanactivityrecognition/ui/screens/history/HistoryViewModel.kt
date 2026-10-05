package com.bilal.humanactivityrecognition.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilal.humanactivityrecognition.AppContainer
import com.bilal.humanactivityrecognition.data.model.ActivityClass
import com.bilal.humanactivityrecognition.domain.model.ActivitySegment
import com.bilal.humanactivityrecognition.utils.DateTimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HistoryViewModel(private val container: AppContainer) : ViewModel() {
    private val repository = container.activityRepository

    private val _filter = MutableStateFlow<ActivityClass?>(null)
    val filter: StateFlow<ActivityClass?> = _filter

    /** Segments grouped by day, newest first. */
    val groupedSegments: StateFlow<Map<LocalDate, List<ActivitySegment>>> =
        combine(repository.recentSegments(), _filter) { segments, filter ->
            segments
                .filter { filter == null || it.activity == filter }
                .groupBy { DateTimeUtils.toLocalDate(it.startTimeMs) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun setFilter(activity: ActivityClass?) {
        _filter.value = activity
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }
}
