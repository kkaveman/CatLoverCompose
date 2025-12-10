package com.example.catlovercompose.core.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Single DataStore instance for the entire app
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

object SettingsDatastore {

    // Preference keys
    object Keys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
    }

    /**
     * Get notifications enabled flow
     */
    fun getNotificationsEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.NOTIFICATIONS_ENABLED] ?: true
        }
    }

    /**
     * Get dark mode enabled flow
     */
    fun getDarkModeEnabled(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.DARK_MODE_ENABLED] ?: false
        }
    }

    /**
     * Set notifications enabled
     */
    suspend fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Set dark mode enabled
     */
    suspend fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DARK_MODE_ENABLED] = enabled
        }
    }
}