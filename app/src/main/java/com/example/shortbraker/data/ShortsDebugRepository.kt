package com.example.shortbraker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.debugDataStore: DataStore<Preferences> by preferencesDataStore(name = "shorts_debug")

data class ShortsDebugState(
    val lastDetectedPackageName: String = "",
    val lastEventType: String = "",
    val isCurrentlyShorts: Boolean = false,
    val lastConfidence: Float = 0f,
    val lastDetectionReasons: String = "",
    val lastDetectionTimeMillis: Long = 0L,
    val currentSessionStartedAt: Long = 0L
)

class ShortsDebugRepository(private val context: Context) {

    companion object {
        val KEY_PACKAGE = stringPreferencesKey("debug_package")
        val KEY_EVENT_TYPE = stringPreferencesKey("debug_event_type")
        val KEY_IS_SHORTS = booleanPreferencesKey("debug_is_shorts")
        val KEY_CONFIDENCE = floatPreferencesKey("debug_confidence")
        val KEY_REASONS = stringPreferencesKey("debug_reasons")
        val KEY_DETECTION_TIME = longPreferencesKey("debug_detection_time")
        val KEY_SESSION_START = longPreferencesKey("debug_session_start")
    }

    val debugState: Flow<ShortsDebugState> = context.debugDataStore.data.map { prefs ->
        ShortsDebugState(
            lastDetectedPackageName = prefs[KEY_PACKAGE] ?: "",
            lastEventType = prefs[KEY_EVENT_TYPE] ?: "",
            isCurrentlyShorts = prefs[KEY_IS_SHORTS] ?: false,
            lastConfidence = prefs[KEY_CONFIDENCE] ?: 0f,
            lastDetectionReasons = prefs[KEY_REASONS] ?: "",
            lastDetectionTimeMillis = prefs[KEY_DETECTION_TIME] ?: 0L,
            currentSessionStartedAt = prefs[KEY_SESSION_START] ?: 0L
        )
    }

    suspend fun saveDebugState(state: ShortsDebugState) {
        context.debugDataStore.edit { prefs ->
            prefs[KEY_PACKAGE] = state.lastDetectedPackageName
            prefs[KEY_EVENT_TYPE] = state.lastEventType
            prefs[KEY_IS_SHORTS] = state.isCurrentlyShorts
            prefs[KEY_CONFIDENCE] = state.lastConfidence
            prefs[KEY_REASONS] = state.lastDetectionReasons
            prefs[KEY_DETECTION_TIME] = state.lastDetectionTimeMillis
            prefs[KEY_SESSION_START] = state.currentSessionStartedAt
        }
    }
}
