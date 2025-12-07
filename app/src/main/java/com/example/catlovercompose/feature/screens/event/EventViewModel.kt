package com.example.catlovercompose.feature.event

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
import kotlinx.coroutines.CancellationException  // ✅ ADD THIS
import javax.inject.Inject

@HiltViewModel
class EventViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventState())
    val state = _state.asStateFlow()

    private val currentUserId = AuthState.getCurrentUser()?.uid ?: ""

    init {
        loadEvents()
    }

    /**
     * Load all events from Firestore (real-time)
     * This will automatically update when events change, including participant updates
     */
    private fun loadEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                eventRepository.getAllEvents().collect { events ->
                    // Sort events: ongoing first, then upcoming, then ended
                    val sortedEvents = events.sortedWith(
                        compareBy<Event> { event ->
                            when {
                                event.hasEnded() -> 3
                                event.isOngoing() -> 1
                                else -> 2 // upcoming
                            }
                        }.thenBy { it.startDate }
                    )

                    _state.value = _state.value.copy(
                        events = sortedEvents,
                        isLoading = false
                    )
                    Log.d("EventViewModel", "Loaded ${events.size} events")
                }
            } catch (e: CancellationException) {
                // ✅ ADD THIS: Don't log cancellation as error - it's expected
                Log.d("EventViewModel", "Event loading cancelled (navigation)")
                throw e // Re-throw to properly cancel the coroutine
            } catch (e: Exception) {
                Log.e("EventViewModel", "Error loading events: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load events"
                )
            }
        }
    }

    /**
     * Toggle join/leave event
     */
    fun toggleJoinEvent(eventId: String, isCurrentlyJoined: Boolean) {
        if (currentUserId.isEmpty()) {
            _state.value = _state.value.copy(
                error = "Please sign in to join events"
            )
            return
        }

        viewModelScope.launch {
            try {  // ✅ ADD TRY-CATCH HERE TOO
                if (isCurrentlyJoined) {
                    eventRepository.leaveEvent(eventId, currentUserId)
                        .onSuccess {
                            Log.d("EventViewModel", "Left event: $eventId")
                        }
                        .onFailure { e ->
                            Log.e("EventViewModel", "Error leaving event: ${e.message}")
                            _state.value = _state.value.copy(
                                error = "Failed to leave event"
                            )
                        }
                } else {
                    eventRepository.joinEvent(eventId, currentUserId)
                        .onSuccess {
                            Log.d("EventViewModel", "Joined event: $eventId")
                        }
                        .onFailure { e ->
                            Log.e("EventViewModel", "Error joining event: ${e.message}")
                            _state.value = _state.value.copy(
                                error = "Failed to join event"
                            )
                        }
                }
            } catch (e: CancellationException) {
                Log.d("EventViewModel", "Join/leave cancelled")
                throw e
            } catch (e: Exception) {
                Log.e("EventViewModel", "Unexpected error: ${e.message}")
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String = currentUserId

    /**
     * Check if user is signed in
     */
    fun isUserSignedIn(): Boolean = currentUserId.isNotEmpty()

    /**
     * Get count of joined events
     */
    fun getJoinedEventsCount(): Int {
        return _state.value.events.count { event ->
            event.participants.contains(currentUserId)
        }
    }
}

data class EventState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Extension functions for Event status checking
private fun Event.hasEnded(): Boolean {
    return System.currentTimeMillis() > endDate
}

private fun Event.isOngoing(): Boolean {
    val now = System.currentTimeMillis()
    return now in startDate..endDate
}

private fun Event.isUpcoming(): Boolean {
    return System.currentTimeMillis() < startDate
}