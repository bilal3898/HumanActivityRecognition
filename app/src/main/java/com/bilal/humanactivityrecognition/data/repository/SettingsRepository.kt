package com.bilal.humanactivityrecognition.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bilal.humanactivityrecognition.domain.model.AppSettings
import com.bilal.humanactivityrecognition.domain.model.InputSource
import com.bilal.humanactivityrecognition.domain.model.RecognitionFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val FREQUENCY = stringPreferencesKey("frequency")
        val SOURCE = stringPreferencesKey("input_source")
        val SMOOTHING = intPreferencesKey("smoothing_window")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            frequency = prefs[Keys.FREQUENCY]?.let { runCatching { RecognitionFrequency.valueOf(it) }.getOrNull() }
                ?: RecognitionFrequency.NORMAL,
            inputSource = prefs[Keys.SOURCE]?.let { runCatching { InputSource.valueOf(it) }.getOrNull() }
                ?: InputSource.PHONE_SENSORS,
            smoothingWindow = prefs[Keys.SMOOTHING] ?: 3,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
        )
    }

    suspend fun setFrequency(value: RecognitionFrequency) = context.dataStore.edit { it[Keys.FREQUENCY] = value.name }
    suspend fun setInputSource(value: InputSource) = context.dataStore.edit { it[Keys.SOURCE] = value.name }
    suspend fun setSmoothingWindow(value: Int) = context.dataStore.edit { it[Keys.SMOOTHING] = value }
    suspend fun setNotificationsEnabled(value: Boolean) = context.dataStore.edit { it[Keys.NOTIFICATIONS] = value }
}
