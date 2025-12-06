package com.example.catlovercompose.feature.screens.community.postsection.postlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.Post
import com.example.catlovercompose.core.repository.PostRepository
import com.example.catlovercompose.core.util.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostListViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PostListState())
    val state = _state.asStateFlow()

    private val currentUserId = AuthState.getCurrentUser()?.uid ?: ""

    init {
        loadPosts()
    }

    /**
     * Load all posts from Firestore (real-time)
     */
    private fun loadPosts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            postRepository.getAllPosts().collect { posts ->
                _state.value = _state.value.copy(
                    posts = posts,
                    isLoading = false
                )
                Log.d("PostListViewModel", "Loaded ${posts.size} posts")
            }
        }
    }

    /**
     * Toggle like on a post
     */
    fun toggleLike(postId: String, isCurrentlyLiked: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyLiked) {
                postRepository.unlikePost(postId, currentUserId)
                    .onSuccess {
                        Log.d("PostListViewModel", "Post unliked: $postId")
                    }
                    .onFailure { e ->
                        Log.e("PostListViewModel", "Error unliking post: ${e.message}")
                        _state.value = _state.value.copy(
                            error = "Failed to unlike post"
                        )
                    }
            } else {
                postRepository.likePost(postId, currentUserId)
                    .onSuccess {
                        Log.d("PostListViewModel", "Post liked: $postId")
                    }
                    .onFailure { e ->
                        Log.e("PostListViewModel", "Error liking post: ${e.message}")
                        _state.value = _state.value.copy(
                            error = "Failed to like post"
                        )
                    }
            }
        }
    }

    /**
     * Delete a post (only owner can delete)
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeleting = true)

            postRepository.deletePost(postId, currentUserId)
                .onSuccess {
                    Log.d("PostListViewModel", "Post deleted: $postId")
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        error = null
                    )
                }
                .onFailure { e ->
                    Log.e("PostListViewModel", "Error deleting post: ${e.message}")
                    _state.value = _state.value.copy(
                        isDeleting = false,
                        error = "Failed to delete post: ${e.message}"
                    )
                }
        }
    }

    /**
     * Show delete confirmation dialog
     */
    fun showDeleteDialog(postId: String) {
        _state.value = _state.value.copy(
            postToDelete = postId,
            showDeleteDialog = true
        )
    }

    /**
     * Hide delete confirmation dialog
     */
    fun hideDeleteDialog() {
        _state.value = _state.value.copy(
            postToDelete = null,
            showDeleteDialog = false
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Get current user ID for checking post ownership
     */
    fun getCurrentUserId(): String = currentUserId

}

data class PostListState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val postToDelete: String? = null,
    val error: String? = null
)