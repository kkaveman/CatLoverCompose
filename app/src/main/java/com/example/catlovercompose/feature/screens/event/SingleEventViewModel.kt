package com.example.catlovercompose.feature.screens.event

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.Event
import com.example.catlovercompose.core.repository.EventRepository
import com.example.catlovercompose.core.util.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SingleEventViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SingleEventState())
    val state = _state.asStateFlow()

    private val currentUserId = AuthState.getCurrentUser()?.uid ?: ""

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            eventRepository.getEvent(eventId)
                .onSuccess { event ->
                    _state.value = _state.value.copy(
                        event = event,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    Log.e("SingleEventViewModel", "Error loading event: ${e.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load event"
                    )
                }
        }
    }

    fun toggleJoinEvent() {
        val event = _state.value.event ?: return
        val isJoined = event.participants.contains(currentUserId)

        if (currentUserId.isEmpty()) {
            _state.value = _state.value.copy(error = "Please sign in to join events")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isJoining = true)

            if (isJoined) {
                eventRepository.leaveEvent(event.id, currentUserId)
                    .onSuccess {
                        _state.value = _state.value.copy(isJoining = false)
                        Log.d("SingleEventViewModel", "Left event")
                    }
                    .onFailure { e ->
                        _state.value = _state.value.copy(
                            isJoining = false,
                            error = "Failed to leave event"
                        )
                    }
            } else {
                eventRepository.joinEvent(event.id, currentUserId)
                    .onSuccess {
                        _state.value = _state.value.copy(isJoining = false)
                        Log.d("SingleEventViewModel", "Joined event")
                    }
                    .onFailure { e ->
                        _state.value = _state.value.copy(
                            isJoining = false,
                            error = "Failed to join event"
                        )
                    }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun getCurrentUserId(): String = currentUserId
    fun isUserSignedIn(): Boolean = currentUserId.isNotEmpty()
}

data class SingleEventState(
    val event: Event? = null,
    val isLoading: Boolean = false,
    val isJoining: Boolean = false,
    val error: String? = null
)