package com.example.catlovercompose.feature.screens.community.postsection

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.repository.PostRepository
import com.example.catlovercompose.core.util.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddPostViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddPostState())
    val state = _state.asStateFlow()

    private val currentUserId = AuthState.getCurrentUser()?.uid ?: ""

    /**
     * Update post content text
     */
    fun updateContent(content: String) {
        _state.value = _state.value.copy(
            content = content,
            error = null
        )
    }

    /**
     * Set selected image URI
     */
    fun setImageUri(uri: Uri?) {
        _state.value = _state.value.copy(
            imageUri = uri,
            error = null
        )
    }

    /**
     * Remove selected image
     */
    fun removeImage() {
        _state.value = _state.value.copy(imageUri = null)
    }

    /**
     * Create and publish post
     */
    fun createPost(onSuccess: () -> Unit) {
        val content = _state.value.content.trim()
        val imageUri = _state.value.imageUri

        // Validation
        if (content.isBlank() && imageUri == null) {
            _state.value = _state.value.copy(
                error = "Post must have content or an image"
            )
            return
        }

        if (content.length > 500) {
            _state.value = _state.value.copy(
                error = "Content must be 500 characters or less"
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isPosting = true, error = null)

            postRepository.createPost(
                userId = currentUserId,
                content = content,
                imageUri = imageUri
            )
                .onSuccess { postId ->
                    Log.d("AddPostViewModel", "Post created: $postId")
                    _state.value = AddPostState() // Reset state
                    onSuccess()
                }
                .onFailure { e ->
                    Log.e("AddPostViewModel", "Error creating post: ${e.message}")
                    _state.value = _state.value.copy(
                        isPosting = false,
                        error = "Failed to create post: ${e.message}"
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

data class AddPostState(
    val content: String = "",
    val imageUri: Uri? = null,
    val isPosting: Boolean = false,
    val error: String? = null
)