package com.example.shortbraker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shortbraker.data.UserSettings
import com.example.shortbraker.data.UserSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserSettingsRepository(application)

    val settings: StateFlow<UserSettings> = repository.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserSettings()
        )

    fun updateDailyLimitMinutes(minutes: Int) {
        viewModelScope.launch { repository.updateDailyLimitMinutes(minutes) }
    }

    fun updateBlockMode(mode: String) {
        viewModelScope.launch { repository.updateBlockMode(mode) }
    }

    fun updateBedtimeModeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateBedtimeModeEnabled(enabled) }
    }

    fun updateBedtimeStart(time: String) {
        viewModelScope.launch { repository.updateBedtimeStart(time) }
    }

    fun updateBedtimeEnd(time: String) {
        viewModelScope.launch { repository.updateBedtimeEnd(time) }
    }

    fun updateRedirectApp(packageName: String) {
        viewModelScope.launch { repository.updateRedirectApp(packageName) }
    }

    fun updateBlockYoutubeApp(enabled: Boolean) {
        viewModelScope.launch { repository.updateBlockYoutubeApp(enabled) }
    }
}
