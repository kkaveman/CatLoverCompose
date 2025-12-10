package com.example.catlovercompose.feature.screens.chatsection.channel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.Channel
import com.example.catlovercompose.core.model.UserProfile
import com.example.catlovercompose.core.repository.ChatRepository
import com.example.catlovercompose.core.repository.UserRepository
import com.example.catlovercompose.core.util.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChannelState())
    val state = _state.asStateFlow()

    private val currentUserId = AuthState.getCurrentUser()?.uid ?: ""
    private val userProfileCache = mutableMapOf<String, UserProfile>()

    init {
        if (currentUserId.isNotEmpty()) {
            loadChannels()
        }
    }

    /**
     * Load all channels for current user (real-time)
     */
    private fun loadChannels() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            chatRepository.getUserChannels(currentUserId).collect { channels ->
                // ✅ Fetch fresh user data for all participants
                val channelsWithFreshData = channels.map { channel ->
                    val otherUserId = channel.participantIds.firstOrNull { it != currentUserId }
                    if (otherUserId != null && !userProfileCache.containsKey(otherUserId)) {
                        // Fetch user profile if not cached
                        userRepository.getUserProfile(otherUserId)
                            .onSuccess { profile ->
                                profile?.let { userProfileCache[otherUserId] = it }
                            }
                    }
                    channel
                }

                _state.value = _state.value.copy(
                    channels = channelsWithFreshData,
                    filteredChannels = channelsWithFreshData,
                    isLoading = false,
                    userProfiles = userProfileCache.toMap() // ✅ Store profiles
                )
                Log.d("ChannelViewModel", "Loaded ${channels.size} channels")
            }
        }
    }
    /**
     * Search channels by username
     */
    fun searchChannels(query: String) {
        val filtered = if (query.isBlank()) {
            _state.value.channels
        } else {
            _state.value.channels.filter { channel ->
                val otherParticipant = channel.participantData.values.firstOrNull {
                    it.username != AuthState.getCurrentUser()?.displayName
                }
                otherParticipant?.username?.contains(query, ignoreCase = true) == true
            }
        }
        _state.value = _state.value.copy(
            filteredChannels = filtered,
            searchQuery = query
        )
    }

    /**
     * Search for user by email to create new channel
     */
    fun searchUserByEmail(email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSearchingUser = true, userSearchError = null)

            chatRepository.searchUserByEmail(email)
                .onSuccess { user ->
                    if (user == null) {
                        _state.value = _state.value.copy(
                            isSearchingUser = false,
                            userSearchError = "No user found with email: $email"
                        )
                    } else if (user.uid == currentUserId) {
                        _state.value = _state.value.copy(
                            isSearchingUser = false,
                            userSearchError = "You cannot message yourself"
                        )
                    } else {
                        _state.value = _state.value.copy(
                            searchedUser = user,
                            isSearchingUser = false
                        )
                    }
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isSearchingUser = false,
                        userSearchError = "Error searching user: ${e.message}"
                    )
                }
        }
    }

    /**
     * Create a new channel with the searched user
     */
    fun createChannel(otherUserId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            // Verify auth state
            val authUser = AuthState.getCurrentUser()
            Log.d("ChannelViewModel", "Auth UID: ${authUser?.uid}")
            Log.d("ChannelViewModel", "Auth Email: ${authUser?.email}")
            Log.d("ChannelViewModel", "Is Authenticated: ${authUser != null}")

            if (authUser == null) {
                _state.value = _state.value.copy(
                    isCreatingChannel = false,
                    userSearchError = "Not authenticated. Please sign in again."
                )
                return@launch
            }

            _state.value = _state.value.copy(isCreatingChannel = true)

            chatRepository.createOrGetChannel(currentUserId, otherUserId)
                .onSuccess { channelId ->
                    _state.value = _state.value.copy(
                        isCreatingChannel = false,
                        searchedUser = null,
                        showAddChannelDialog = false
                    )
                    onSuccess(channelId)
                    Log.d("ChannelViewModel", "Channel created/retrieved: $channelId")
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isCreatingChannel = false,
                        userSearchError = "Failed to create channel: ${e.message}"
                    )
                    Log.e("ChannelViewModel", "Error creating channel: ${e.message}", e)
                }
        }
    }
    /**
     * Show/hide add channel dialog
     */
    fun toggleAddChannelDialog(show: Boolean) {
        _state.value = _state.value.copy(
            showAddChannelDialog = show,
            searchedUser = null,
            userSearchError = null
        )
    }

    /**
     * Clear searched user
     */
    fun clearSearchedUser() {
        _state.value = _state.value.copy(
            searchedUser = null,
            userSearchError = null
        )
    }
}

data class ChannelState(
    val channels: List<Channel> = emptyList(),
    val filteredChannels: List<Channel> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val showAddChannelDialog: Boolean = false,
    val searchedUser: UserProfile? = null,
    val isSearchingUser: Boolean = false,
    val isCreatingChannel: Boolean = false,
    val userSearchError: String? = null,
    val userProfiles: Map<String, UserProfile> = emptyMap()
)