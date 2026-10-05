package com.bilal.humanactivityrecognition.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Filled.Home)
    data object Recognition : Screen("recognition", "Live", Icons.Filled.Sensors)
    data object History : Screen("history", "History", Icons.Filled.History)
    data object Statistics : Screen("statistics", "Stats", Icons.Filled.BarChart)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val bottomBar = listOf(Home, Recognition, History, Statistics, Settings)
    }
}
