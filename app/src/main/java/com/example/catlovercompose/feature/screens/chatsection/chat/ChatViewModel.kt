package com.example.catlovercompose.feature.screens.chatsection.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.Channel
import com.example.catlovercompose.core.model.Message
import com.example.catlovercompose.core.model.getOtherParticipant
import com.example.catlovercompose.core.repository.ChatRepository
import com.example.catlovercompose.core.repository.UserRepository

import com.example.catlovercompose.core.util.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    private val currentUserId = AuthState.getCurrentUser()?.uid ?: ""
    private var currentChannelId: String = ""

    /**
     * Initialize chat with channel ID
     */
    fun initializeChat(channelId: String) {
        currentChannelId = channelId
        loadChannel()
        loadMessages()
    }

    /**
     * Load channel info
     */
    private fun loadChannel() {
        viewModelScope.launch {
            chatRepository.getChannel(currentChannelId)
                .onSuccess { channel ->
                    channel?.let {
                        val otherUserId = it.participantIds.firstOrNull { id -> id != currentUserId }

                        // ✅ Fetch fresh user profile
                        if (otherUserId != null) {
                            userRepository.getUserProfile(otherUserId)
                                .onSuccess { profile ->
                                    _state.value = _state.value.copy(
                                        channel = it,
                                        otherUserName = profile?.username ?: "Unknown",
                                        otherUserProfileUrl = profile?.profileImageUrl // ✅ Fresh data
                                    )
                                }
                                .onFailure { e ->
                                    // Fallback to cached data
                                    val otherParticipant = it.getOtherParticipant(currentUserId)
                                    _state.value = _state.value.copy(
                                        channel = it,
                                        otherUserName = otherParticipant?.username ?: "Unknown",
                                        otherUserProfileUrl = otherParticipant?.profileImageUrl
                                    )
                                }
                        }
                    }
                }
                .onFailure { e ->
                    Log.e("ChatViewModel", "Error loading channel: ${e.message}")
                    _state.value = _state.value.copy(
                        error = "Failed to load chat: ${e.message}"
                    )
                }
        }
    }
    /**
     * Load messages (real-time)
     */
    private fun loadMessages() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            chatRepository.getChannelMessages(currentChannelId).collect { messages ->
                _state.value = _state.value.copy(
                    messages = messages,
                    isLoading = false
                )
                Log.d("ChatViewModel", "Loaded ${messages.size} messages")
            }
        }
    }

    /**
     * Send a message
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true)

            chatRepository.sendMessage(currentChannelId, currentUserId, text.trim())
                .onSuccess {
                    _state.value = _state.value.copy(
                        isSending = false,
                        messageInput = ""
                    )
                    Log.d("ChatViewModel", "Message sent")
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isSending = false,
                        error = "Failed to send message: ${e.message}"
                    )
                    Log.e("ChatViewModel", "Error sending message: ${e.message}")
                }
        }
    }

    /**
     * Update message input text
     */
    fun updateMessageInput(text: String) {
        _state.value = _state.value.copy(messageInput = text)
    }

    /**
     * Clear error
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

data class ChatState(
    val channel: Channel? = null,
    val messages: List<Message> = emptyList(),
    val messageInput: String = "",
    val otherUserName: String = "",
    val otherUserProfileUrl: String? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)