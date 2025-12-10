package com.example.catlovercompose.feature.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.Event
import com.example.catlovercompose.core.model.Post
import com.example.catlovercompose.core.model.hasEnded
import com.example.catlovercompose.core.repository.EventRepository
import com.example.catlovercompose.core.repository.PostRepository
import com.example.catlovercompose.core.repository.UserRepository
import com.example.catlovercompose.core.util.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PostFilter {
    RECENT, POPULAR
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private val currentUserId = AuthState.getCurrentUser()?.uid ?: ""

    init {
        loadUserProfile()
        loadEvents()
        loadPosts()
    }

    /**
     * Load current user profile for profile picture
     */
    private fun loadUserProfile() {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            userRepository.getUserProfile(currentUserId)
                .onSuccess { profile ->
                    _state.value = _state.value.copy(
                        userProfileImageUrl = profile?.profileImageUrl,
                        username = profile?.username ?: ""
                    )
                }
                .onFailure { e ->
                    Log.e("HomeViewModel", "Error loading profile: ${e.message}")
                }
        }
    }

    /**
     * Load all events (real-time)
     */
    private fun loadEvents() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingEvents = true)

            try {
                eventRepository.getAllEvents().collect { events ->
                    // Filter out ended events and sort by start date
                    val activeEvents = events
                        .filter { !it.hasEnded() }
                        .sortedBy { it.startDate }

                    _state.value = _state.value.copy(
                        events = activeEvents,
                        isLoadingEvents = false
                    )
                    Log.d("HomeViewModel", "Loaded ${activeEvents.size} active events")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading events: ${e.message}")
                _state.value = _state.value.copy(
                    isLoadingEvents = false,
                    error = "Failed to load events"
                )
            }
        }
    }

    /**
     * Load all posts (real-time)
     */
    private fun loadPosts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingPosts = true)

            try {
                postRepository.getAllPosts().collect { posts ->
                    _state.value = _state.value.copy(
                        allPosts = posts,
                        isLoadingPosts = false
                    )
                    applyPostFilter()
                    Log.d("HomeViewModel", "Loaded ${posts.size} posts")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading posts: ${e.message}")
                _state.value = _state.value.copy(
                    isLoadingPosts = false,
                    error = "Failed to load posts"
                )
            }
        }
    }

    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    /**
     * Toggle post filter between Recent and Popular
     */
    fun togglePostFilter(filter: PostFilter) {
        _state.value = _state.value.copy(postFilter = filter)
        applyPostFilter()
    }

    /**
     * Apply post filter based on current selection
     */
    private fun applyPostFilter() {
        val filtered = when (_state.value.postFilter) {
            PostFilter.RECENT -> {
                _state.value.allPosts.sortedByDescending { it.createdAt }
            }
            PostFilter.POPULAR -> {
                _state.value.allPosts.sortedByDescending { it.likeCount }
            }
        }

        _state.value = _state.value.copy(filteredPosts = filtered)
    }

    /**
     * Toggle like on a post
     */
    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        if (currentUserId.isEmpty()) {
            _state.value = _state.value.copy(error = "Please sign in to like posts")
            return
        }

        viewModelScope.launch {
            if (isCurrentlyLiked) {
                postRepository.unlikePost(postId, currentUserId)
                    .onSuccess {
                        Log.d("HomeViewModel", "Post unliked: $postId")
                    }
                    .onFailure { e ->
                        Log.e("HomeViewModel", "Error unliking post: ${e.message}")
                        _state.value = _state.value.copy(error = "Failed to unlike post")
                    }
            } else {
                postRepository.likePost(postId, currentUserId)
                    .onSuccess {
                        Log.d("HomeViewModel", "Post liked: $postId")
                    }
                    .onFailure { e ->
                        Log.e("HomeViewModel", "Error liking post: ${e.message}")
                        _state.value = _state.value.copy(error = "Failed to like post")
                    }
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
}

data class HomeState(
    val userProfileImageUrl: String? = null,
    val username: String = "",
    val searchQuery: String = "",
    val events: List<Event> = emptyList(),
    val allPosts: List<Post> = emptyList(),
    val filteredPosts: List<Post> = emptyList(),
    val postFilter: PostFilter = PostFilter.RECENT,
    val isLoadingEvents: Boolean = false,
    val isLoadingPosts: Boolean = false,
    val error: String? = null
)