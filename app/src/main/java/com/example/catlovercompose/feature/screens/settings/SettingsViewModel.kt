package com.example.catlovercompose.feature.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.util.SettingsDatastore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Load settings from DataStore
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Collect notifications setting
                SettingsDatastore.getNotificationsEnabled(context).collect { enabled ->
                    _state.value = _state.value.copy(
                        notificationsEnabled = enabled,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load settings: ${e.message}"
                )
            }
        }

        viewModelScope.launch {
            try {
                // Collect dark mode setting
                SettingsDatastore.getDarkModeEnabled(context).collect { enabled ->
                    _state.value = _state.value.copy(darkModeEnabled = enabled)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to load dark mode: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggle notifications
     */
    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                SettingsDatastore.setNotificationsEnabled(context, enabled)
                _state.value = _state.value.copy(notificationsEnabled = enabled)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to update notifications: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggle dark mode
     */
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                SettingsDatastore.setDarkModeEnabled(context, enabled)
                _state.value = _state.value.copy(darkModeEnabled = enabled)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to update dark mode: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

data class SettingsState(
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)