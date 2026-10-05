package com.bilal.humanactivityrecognition.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bilal.humanactivityrecognition.ui.screens.history.HistoryScreen
import com.bilal.humanactivityrecognition.ui.screens.home.HomeScreen
import com.bilal.humanactivityrecognition.ui.screens.recognition.RecognitionScreen
import com.bilal.humanactivityrecognition.ui.screens.settings.SettingsScreen
import com.bilal.humanactivityrecognition.ui.screens.statistics.StatisticsScreen

@Composable
fun AppNavigation(onStartRecognition: () -> Unit, navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    fun navigate(screen: Screen) = navController.navigate(screen.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.bottomBar.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = { navigate(screen) },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Screen.Home.route, modifier = Modifier.padding(padding)) {
            composable(Screen.Home.route) {
                HomeScreen(onStartRecognition = onStartRecognition, onOpenLive = { navigate(Screen.Recognition) })
            }
            composable(Screen.Recognition.route) { RecognitionScreen(onStartRecognition = onStartRecognition) }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Statistics.route) { StatisticsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
