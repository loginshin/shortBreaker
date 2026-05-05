package com.example.shortbraker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shortbraker.data.ShortsDebugRepository
import com.example.shortbraker.data.ShortsDebugState
import com.example.shortbraker.data.ShortsUsageRepository
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
    val shortsUsageMinutes: Int = 0,
    val shortsOpenCount: Int = 0,
    val usageStatsGranted: Boolean = false,
    val overlayGranted: Boolean = false,
    val accessibilityGranted: Boolean = false,
    val debugState: ShortsDebugState = ShortsDebugState()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val permissionChecker = PermissionChecker(application)
    private val usageStatsTracker = UsageStatsTracker(application)
    private val shortsUsageRepository = ShortsUsageRepository(application)
    private val shortsDebugRepository = ShortsDebugRepository(application)

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            shortsUsageRepository.todayUsage.collect { shortsUsage ->
                _state.update {
                    it.copy(
                        shortsUsageMinutes = shortsUsage.usageMinutes,
                        shortsOpenCount = shortsUsage.openCount
                    )
                }
            }
        }
        viewModelScope.launch {
            shortsDebugRepository.debugState.collect { debugState ->
                _state.update { it.copy(debugState = debugState) }
            }
        }
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
