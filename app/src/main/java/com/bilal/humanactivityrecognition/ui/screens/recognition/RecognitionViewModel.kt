package com.bilal.humanactivityrecognition.ui.screens.recognition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bilal.humanactivityrecognition.AppContainer
import com.bilal.humanactivityrecognition.data.repository.LiveRecognitionState
import com.bilal.humanactivityrecognition.service.ActivityRecognitionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class RecognitionViewModel(private val container: AppContainer) : ViewModel() {
    val live: StateFlow<LiveRecognitionState> = container.activityRepository.live

    /** Ticks once per second to drive the recognition timer. */
    val now: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(1_000), System.currentTimeMillis())

    fun stop() = ActivityRecognitionService.stop(container.appContext)
}
