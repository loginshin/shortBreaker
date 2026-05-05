package com.example.shortbraker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserSettingsRepository(private val context: Context) {

    companion object {
        val KEY_DAILY_LIMIT_MINUTES = intPreferencesKey("daily_limit_minutes")
        val KEY_BLOCK_MODE = stringPreferencesKey("block_mode")
        val KEY_BEDTIME_MODE_ENABLED = booleanPreferencesKey("bedtime_mode_enabled")
        val KEY_BEDTIME_START = stringPreferencesKey("bedtime_start")
        val KEY_BEDTIME_END = stringPreferencesKey("bedtime_end")
        val KEY_REDIRECT_APP = stringPreferencesKey("redirect_app_package")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            dailyLimitMinutes = prefs[KEY_DAILY_LIMIT_MINUTES] ?: 30,
            blockMode = prefs[KEY_BLOCK_MODE] ?: "NORMAL",
            bedtimeModeEnabled = prefs[KEY_BEDTIME_MODE_ENABLED] ?: false,
            bedtimeStart = prefs[KEY_BEDTIME_START] ?: "23:30",
            bedtimeEnd = prefs[KEY_BEDTIME_END] ?: "07:00",
            redirectAppPackage = prefs[KEY_REDIRECT_APP] ?: ""
        )
    }

    suspend fun updateDailyLimitMinutes(minutes: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_DAILY_LIMIT_MINUTES] = minutes }
    }

    suspend fun updateBlockMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[KEY_BLOCK_MODE] = mode }
    }

    suspend fun updateBedtimeModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_BEDTIME_MODE_ENABLED] = enabled }
    }

    suspend fun updateBedtimeStart(time: String) {
        context.dataStore.edit { prefs -> prefs[KEY_BEDTIME_START] = time }
    }

    suspend fun updateBedtimeEnd(time: String) {
        context.dataStore.edit { prefs -> prefs[KEY_BEDTIME_END] = time }
    }

    suspend fun updateRedirectApp(packageName: String) {
        context.dataStore.edit { prefs -> prefs[KEY_REDIRECT_APP] = packageName }
    }
}
