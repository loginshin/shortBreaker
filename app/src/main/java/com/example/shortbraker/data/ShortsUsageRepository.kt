package com.example.shortbraker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.shortsDataStore: DataStore<Preferences> by preferencesDataStore(name = "shorts_usage")

data class ShortsUsage(
    val usageSeconds: Long = 0L,
    val openCount: Int = 0
) {
    val usageMinutes: Int get() = (usageSeconds / 60L).toInt()
}

class ShortsUsageRepository(private val context: Context) {

    companion object {
        val KEY_USAGE_DATE = stringPreferencesKey("shorts_usage_date")
        val KEY_USAGE_SECONDS = longPreferencesKey("shorts_usage_seconds")
        val KEY_OPEN_COUNT = intPreferencesKey("shorts_open_count")
    }

    private fun todayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val todayUsage: Flow<ShortsUsage> = context.shortsDataStore.data.map { prefs ->
        val savedDate = prefs[KEY_USAGE_DATE] ?: ""
        if (savedDate != todayDate()) {
            ShortsUsage()
        } else {
            ShortsUsage(
                usageSeconds = prefs[KEY_USAGE_SECONDS] ?: 0L,
                openCount = prefs[KEY_OPEN_COUNT] ?: 0
            )
        }
    }

    suspend fun addUsageSeconds(seconds: Long) {
        if (seconds <= 0) return
        val today = todayDate()
        context.shortsDataStore.edit { prefs ->
            if ((prefs[KEY_USAGE_DATE] ?: "") != today) {
                prefs[KEY_USAGE_DATE] = today
                prefs[KEY_USAGE_SECONDS] = seconds
                prefs[KEY_OPEN_COUNT] = 0
            } else {
                prefs[KEY_USAGE_SECONDS] = (prefs[KEY_USAGE_SECONDS] ?: 0L) + seconds
            }
        }
    }

    suspend fun incrementOpenCount() {
        val today = todayDate()
        context.shortsDataStore.edit { prefs ->
            if ((prefs[KEY_USAGE_DATE] ?: "") != today) {
                prefs[KEY_USAGE_DATE] = today
                prefs[KEY_USAGE_SECONDS] = 0L
                prefs[KEY_OPEN_COUNT] = 1
            } else {
                prefs[KEY_OPEN_COUNT] = (prefs[KEY_OPEN_COUNT] ?: 0) + 1
            }
        }
    }

    suspend fun getTodayUsageSnapshot(): ShortsUsage {
        val prefs = context.shortsDataStore.data.first()
        val savedDate = prefs[KEY_USAGE_DATE] ?: ""
        return if (savedDate != todayDate()) {
            ShortsUsage()
        } else {
            ShortsUsage(
                usageSeconds = prefs[KEY_USAGE_SECONDS] ?: 0L,
                openCount = prefs[KEY_OPEN_COUNT] ?: 0
            )
        }
    }
}
