package com.bilal.humanactivityrecognition.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.bilal.humanactivityrecognition.AppContainer
import com.bilal.humanactivityrecognition.HARApplication
import com.bilal.humanactivityrecognition.ui.screens.history.HistoryViewModel
import com.bilal.humanactivityrecognition.ui.screens.home.HomeViewModel
import com.bilal.humanactivityrecognition.ui.screens.recognition.RecognitionViewModel
import com.bilal.humanactivityrecognition.ui.screens.settings.SettingsViewModel
import com.bilal.humanactivityrecognition.ui.screens.statistics.StatisticsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { HomeViewModel(container()) }
        initializer { RecognitionViewModel(container()) }
        initializer { HistoryViewModel(container()) }
        initializer { StatisticsViewModel(container()) }
        initializer { SettingsViewModel(container()) }
    }
}

private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as HARApplication).container
