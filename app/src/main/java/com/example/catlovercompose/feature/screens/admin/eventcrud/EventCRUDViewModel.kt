package com.example.catlovercompose.feature.screens.admin.eventcrud

import android.net.Uri
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
class EventCRUDViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EventCRUDState())
    val state = _state.asStateFlow()

    private val currentUserId = AuthState.getCurrentUser()?.uid ?: ""

    init {
        loadEvents()
    }

    /**
     * Load all events (real-time)
     */
    private fun loadEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                eventRepository.getAllEvents().collect { events ->
                    _state.value = _state.value.copy(
                        events = events,
                        isLoading = false
                    )
                    Log.d("EventCRUDViewModel", "Loaded ${events.size} events")
                }
            } catch (e: Exception) {
                Log.e("EventCRUDViewModel", "Error loading events: ${e.message}")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load events"
                )
            }
        }
    }

    // ============ CREATE EVENT ============

    fun showCreateDialog() {
        _state.value = _state.value.copy(
            showCreateDialog = true,
            title = "",
            description = "",
            imageUri = null,
            startDate = 0L,
            endDate = 0L,
            error = null
        )
    }

    fun hideCreateDialog() {
        _state.value = _state.value.copy(showCreateDialog = false)
    }

    fun createEvent() {
        val title = _state.value.title.trim()
        val description = _state.value.description.trim()
        val imageUri = _state.value.imageUri
        val startDate = _state.value.startDate
        val endDate = _state.value.endDate

        // Validation
        if (title.isBlank()) {
            _state.value = _state.value.copy(error = "Title is required")
            return
        }

        if (description.isBlank()) {
            _state.value = _state.value.copy(error = "Description is required")
            return
        }

        if (startDate == 0L) {
            _state.value = _state.value.copy(error = "Start date is required")
            return
        }

        if (endDate == 0L) {
            _state.value = _state.value.copy(error = "End date is required")
            return
        }

        if (endDate < startDate) {
            _state.value = _state.value.copy(error = "End date must be after start date")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isCreating = true, error = null)

            eventRepository.createEvent(
                userId = currentUserId,
                title = title,
                description = description,
                imageUri = imageUri,
                startDate = startDate,
                endDate = endDate
            )
                .onSuccess { eventId ->
                    Log.d("EventCRUDViewModel", "Event created: $eventId")
                    _state.value = _state.value.copy(
                        isCreating = false,
                        showCreateDialog = false,
                        successMessage = "Event created successfully"
                    )
                }
                .onFailure { e ->
                    Log.e("EventCRUDViewModel", "Error creating event: ${e.message}")
                    _state.value = _state.value.copy(
                        isCreating = false,
                        error = "Failed to create event: ${e.message}"
                    )
                }
        }
    }

    // ============ EVENT DETAILS ============

    fun showDetailsDialog(event: Event) {
        _state.value = _state.value.copy(
            selectedEvent = event,
            showDetailsDialog = true
        )
    }

    fun hideDetailsDialog() {
        _state.value = _state.value.copy(
            selectedEvent = null,
            showDetailsDialog = false
        )
    }

    // ============ EDIT EVENT ============

    fun showEditDialog() {
        val event = _state.value.selectedEvent ?: return
        _state.value = _state.value.copy(
            showEditDialog = true,
            showDetailsDialog = false,
            title = event.title,
            description = event.description,
            imageUri = null, // Keep existing image unless user changes it
            startDate = event.startDate,
            endDate = event.endDate,
            error = null
        )
    }

    fun hideEditDialog() {
        _state.value = _state.value.copy(
            showEditDialog = false,
            showDetailsDialog = true // Go back to details
        )
    }

    fun updateEvent() {
        val event = _state.value.selectedEvent ?: return
        val title = _state.value.title.trim()
        val description = _state.value.description.trim()
        val imageUri = _state.value.imageUri
        val startDate = _state.value.startDate
        val endDate = _state.value.endDate

        // Validation
        if (title.isBlank()) {
            _state.value = _state.value.copy(error = "Title is required")
            return
        }

        if (description.isBlank()) {
            _state.value = _state.value.copy(error = "Description is required")
            return
        }

        if (startDate == 0L) {
            _state.value = _state.value.copy(error = "Start date is required")
            return
        }

        if (endDate == 0L) {
            _state.value = _state.value.copy(error = "End date is required")
            return
        }

        if (endDate < startDate) {
            _state.value = _state.value.copy(error = "End date must be after start date")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true, error = null)

            eventRepository.updateEvent(
                eventId = event.id,
                userId = currentUserId,
                title = title,
                description = description,
                imageUri = imageUri, // If null, keeps existing image
                startDate = startDate,
                endDate = endDate
            )
                .onSuccess {
                    Log.d("EventCRUDViewModel", "Event updated: ${event.id}")
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        showEditDialog = false,
                        showDetailsDialog = false,
                        selectedEvent = null,
                        successMessage = "Event updated successfully"
                    )
                }
                .onFailure { e ->
                    Log.e("EventCRUDViewModel", "Error updating event: ${e.message}")
                    _state.value = _state.value.copy(
                        isUpdating = false,
                        error = "Failed to update event: ${e.message}"
                    )
                }
        }
    }

    // ============ DELETE EVENT ============

    fun showDeleteDialog(eventId: String) {
        _state.value = _state.value.copy(
            eventToDelete = eventId,
            showDeleteDialog = true,
            showDetailsDialog = false
        )
    }

    fun hideDeleteDialog() {
        _state.value = _state.value.copy(
            eventToDelete = null,
            showDeleteDialog = false,
            showDetailsDialog = _state.value.selectedEvent != null // Go back to details if open
        )
    }

    fun deleteEvent() {
        val eventId = _state.value.eventToDelete ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true)

            eventRepository.deleteEvent(eventId, currentUserId)
                .onSuccess {
                    Log.d("EventCRUDViewModel", "Event deleted: $eventId")
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        showDeleteDialog = false,
                        showDetailsDialog = false,
                        selectedEvent = null,
                        eventToDelete = null,
                        successMessage = "Event deleted successfully"
                    )
                }
                .onFailure { e ->
                    Log.e("EventCRUDViewModel", "Error deleting event: ${e.message}")
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        error = "Failed to delete event: ${e.message}"
                    )
                }
        }
    }

    // ============ KICK PARTICIPANT ============

    fun kickParticipant(participantId: String) {
        val event = _state.value.selectedEvent ?: return

        viewModelScope.launch {
            eventRepository.leaveEvent(event.id, participantId)
                .onSuccess {
                    Log.d("EventCRUDViewModel", "Participant kicked: $participantId")
                    _state.value = _state.value.copy(
                        successMessage = "Participant removed"
                    )
                    // The event will auto-update via real-time listener
                }
                .onFailure { e ->
                    Log.e("EventCRUDViewModel", "Error kicking participant: ${e.message}")
                    _state.value = _state.value.copy(
                        error = "Failed to remove participant"
                    )
                }
        }
    }

    // ============ FORM FIELDS ============

    fun updateTitle(title: String) {
        _state.value = _state.value.copy(title = title, error = null)
    }

    fun updateDescription(description: String) {
        _state.value = _state.value.copy(description = description, error = null)
    }

    fun setImageUri(uri: Uri?) {
        _state.value = _state.value.copy(imageUri = uri, error = null)
    }

    fun removeImage() {
        _state.value = _state.value.copy(imageUri = null)
    }

    fun setStartDate(timestamp: Long) {
        _state.value = _state.value.copy(startDate = timestamp, error = null)
    }

    fun setEndDate(timestamp: Long) {
        _state.value = _state.value.copy(endDate = timestamp, error = null)
    }

    // ============ MESSAGES ============

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }
}

data class EventCRUDState(
    val events: List<Event> = emptyList(),
    val selectedEvent: Event? = null,

    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,

    val showCreateDialog: Boolean = false,
    val showDetailsDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,

    val eventToDelete: String? = null,

    // Form fields
    val title: String = "",
    val description: String = "",
    val imageUri: Uri? = null,
    val startDate: Long = 0L,
    val endDate: Long = 0L,

    val error: String? = null,
    val successMessage: String? = null
)