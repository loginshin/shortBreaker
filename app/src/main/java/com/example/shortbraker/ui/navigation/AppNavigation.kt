package com.example.shortbraker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.shortbraker.ui.screen.DashboardScreen
import com.example.shortbraker.ui.screen.HistoryScreen
import com.example.shortbraker.ui.screen.PermissionScreen
import com.example.shortbraker.ui.screen.SettingsScreen
import com.example.shortbraker.viewmodel.SettingsViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val PERMISSION = "permission"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                navController = navController,
                settingsViewModel = settingsViewModel
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                navController = navController,
                settingsViewModel = settingsViewModel
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(navController = navController)
        }
        composable(Routes.PERMISSION) {
            PermissionScreen(navController = navController)
        }
    }
}
