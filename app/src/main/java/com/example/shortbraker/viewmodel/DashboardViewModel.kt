package com.example.shortbraker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shortbraker.permission.PermissionChecker
import com.example.shortbraker.usage.UsageStatsTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardState(
    val youtubeUsageMinutes: Int = 0,
    val usageStatsGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val accessibilityGranted: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val permissionChecker = PermissionChecker(application)
    private val usageStatsTracker = UsageStatsTracker(application)

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val usageStatsGranted = permissionChecker.hasUsageStatsPermission()
            val youtubeMinutes = if (usageStatsGranted) {
                usageStatsTracker.getTodayYoutubeUsageMinutes()
            } else {
                0
            }
            _state.update {
                it.copy(
                    youtubeUsageMinutes = youtubeMinutes,
                    usageStatsGranted = usageStatsGranted,
                    overlayGranted = permissionChecker.hasOverlayPermission(),
                    accessibilityGranted = permissionChecker.hasAccessibilityPermission()
                )
            }
        }
    }
}
