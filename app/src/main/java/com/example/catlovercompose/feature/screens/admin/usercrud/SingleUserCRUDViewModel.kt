package com.example.catlovercompose.feature.screens.admin.usercrud

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.Post
import com.example.catlovercompose.core.repository.PostRepository
import com.example.catlovercompose.core.repository.UserRepository
import com.example.catlovercompose.core.util.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SingleUserCRUDViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SingleUserCRUDState())
    val state = _state.asStateFlow()

    private val currentAdminId = AuthState.getCurrentUser()?.uid ?: ""

    /**
     * Load user data and posts
     */
    fun loadUser(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                userId = userId,
                isLoading = true
            )

            // Load user profile
            userRepository.getUserProfile(userId)
                .onSuccess { profile ->
                    _state.value = _state.value.copy(
                        username = profile?.username ?: "",
                        bio = profile?.bio ?: "",
                        email = profile?.email ?: "",
                        profileImageUrl = profile?.profileImageUrl,
                        age = profile?.age,
                        gender = profile?.gender ?: 2,
                        postCount = profile?.postCount ?: 0,
                        followerCount = profile?.followerCount ?: 0,
                        followingCount = profile?.followingCount ?: 0,
                        role = profile?.role ?: 0,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    Log.e("SingleUserCRUDVM", "Error loading user: ${e.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load user: ${e.message}"
                    )
                }

            // Load user posts
            postRepository.getUserPosts(userId).collect { posts ->
                _state.value = _state.value.copy(userPosts = posts)
            }
        }
    }

    // ==================== EDIT USER ====================

    /**
     * Show edit user dialog
     */
    fun showEditUserDialog() {
        _state.value = _state.value.copy(
            showEditUserDialog = true,
            editUsername = _state.value.username,
            editBio = _state.value.bio,
            editAge = _state.value.age?.toString() ?: "",
            editGender = _state.value.gender
        )
    }

    /**
     * Hide edit user dialog
     */
    fun hideEditUserDialog() {
        _state.value = _state.value.copy(
            showEditUserDialog = false,
            editUsername = "",
            editBio = "",
            editAge = "",
            editGender = 2
        )
    }

    /**
     * Update edit form fields
     */
    fun updateEditUsername(value: String) {
        _state.value = _state.value.copy(editUsername = value)
    }

    fun updateEditBio(value: String) {
        _state.value = _state.value.copy(editBio = value)
    }

    fun updateEditAge(value: String) {
        _state.value = _state.value.copy(editAge = value)
    }

    fun updateEditGender(value: Int) {
        _state.value = _state.value.copy(editGender = value)
    }

    /**
     * Save edited user profile
     */
    fun saveUserEdit() {
        val userId = _state.value.userId
        val username = _state.value.editUsername
        val bio = _state.value.editBio
        val age = _state.value.editAge.toIntOrNull()
        val gender = _state.value.editGender

        if (username.isBlank()) {
            _state.value = _state.value.copy(error = "Username cannot be empty")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdatingUser = true)

            val updates = mapOf(
                "username" to username,
                "bio" to bio,
                "age" to age,
                "gender" to gender,
                "updatedAt" to System.currentTimeMillis()
            )

            userRepository.updateUserProfile(userId, updates)
                .onSuccess {
                    _state.value = _state.value.copy(
                        username = username,
                        bio = bio,
                        age = age,
                        gender = gender,
                        isUpdatingUser = false,
                        showEditUserDialog = false
                    )
                    Log.d("SingleUserCRUDVM", "User profile updated")
                }
                .onFailure { e ->
                    Log.e("SingleUserCRUDVM", "Error updating user: ${e.message}")
                    _state.value = _state.value.copy(
                        isUpdatingUser = false,
                        error = "Failed to update user: ${e.message}"
                    )
                }
        }
    }

    // ==================== DELETE USER ====================

    /**
     * Show delete user confirmation dialog
     */
    fun showDeleteUserDialog() {
        _state.value = _state.value.copy(
            showDeleteUserDialog = true,
            deleteConfirmUsername = ""
        )
    }

    /**
     * Hide delete user dialog
     */
    fun hideDeleteUserDialog() {
        _state.value = _state.value.copy(
            showDeleteUserDialog = false,
            deleteConfirmUsername = ""
        )
    }

    /**
     * Update delete confirmation input
     */
    fun updateDeleteConfirmUsername(value: String) {
        _state.value = _state.value.copy(deleteConfirmUsername = value)
    }

    /**
     * Delete user (with all posts and relationships)
     */
    fun deleteUser(onSuccess: () -> Unit) {
        val userId = _state.value.userId
        val confirmUsername = _state.value.deleteConfirmUsername
        val actualUsername = _state.value.username

        // Verify username matches
        if (confirmUsername != actualUsername) {
            _state.value = _state.value.copy(error = "Username doesn't match")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isDeletingUser = true)

            // First, delete all user posts
            postRepository.deleteAllUserPosts(userId)
                .onSuccess { deletedPostCount ->
                    Log.d("SingleUserCRUDVM", "Deleted $deletedPostCount posts")
                }
                .onFailure { e ->
                    Log.w("SingleUserCRUDVM", "Error deleting posts: ${e.message}")
                }

            // Then delete the user
            userRepository.deleteUser(userId)
                .onSuccess {
                    Log.d("SingleUserCRUDVM", "User deleted successfully")
                    _state.value = _state.value.copy(
                        isDeletingUser = false,
                        showDeleteUserDialog = false
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    Log.e("SingleUserCRUDVM", "Error deleting user: ${e.message}")
                    _state.value = _state.value.copy(
                        isDeletingUser = false,
                        error = "Failed to delete user: ${e.message}"
                    )
                }
        }
    }

    // ==================== POST ACTIONS ====================

    /**
     * Show post action modal (edit or delete)
     */
    fun showPostActionModal(post: Post) {
        _state.value = _state.value.copy(
            selectedPost = post,
            showPostActionModal = true
        )
    }

    /**
     * Hide post action modal
     */
    fun hidePostActionModal() {
        _state.value = _state.value.copy(
            selectedPost = null,
            showPostActionModal = false
        )
    }

    /**
     * Show edit post dialog
     */
    fun showEditPostDialog() {
        val post = _state.value.selectedPost ?: return
        _state.value = _state.value.copy(
            showEditPostDialog = true,
            showPostActionModal = false,
            editPostTitle = post.title,
            editPostContent = post.content,
            editPostImageUrl = post.imageUrl,  // ✅ ADDED
            editPostNewImageUri = null  // ✅ ADDED
        )
    }

    /**
     * Hide edit post dialog
     */
    fun hideEditPostDialog() {
        _state.value = _state.value.copy(
            showEditPostDialog = false,
            editPostTitle = "",
            editPostContent = "",
            editPostImageUrl = null,  // ✅ ADDED
            editPostNewImageUri = null  // ✅ ADDED
        )
    }

    /**
     * Update edit post fields
     */
    fun updateEditPostTitle(value: String) {
        _state.value = _state.value.copy(editPostTitle = value)
    }

    fun updateEditPostContent(value: String) {
        _state.value = _state.value.copy(editPostContent = value)
    }

    // ✅ NEW: Set new image for post edit
    fun setEditPostImage(imageUri: Uri?) {
        _state.value = _state.value.copy(editPostNewImageUri = imageUri)
    }

    // ✅ NEW: Remove image from post edit
    fun removeEditPostImage() {
        _state.value = _state.value.copy(
            editPostImageUrl = null,
            editPostNewImageUri = null
        )
    }

    /**
     * Save edited post with image support
     */
    fun savePostEdit() {
        val post = _state.value.selectedPost ?: return
        val title = _state.value.editPostTitle
        val content = _state.value.editPostContent
        val newImageUri = _state.value.editPostNewImageUri

        if (content.isBlank()) {
            _state.value = _state.value.copy(error = "Content cannot be empty")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdatingPost = true)

            try {
                // ✅ STEP 1: Upload new image if selected
                val finalImageUrl = if (newImageUri != null) {
                    val uploadResult = uploadPostImage(newImageUri)

                    uploadResult.getOrNull() ?: run {
                        _state.value = _state.value.copy(
                            isUpdatingPost = false,
                            error = "Failed to upload image"
                        )
                        return@launch
                    }
                } else {
                    // Keep existing image URL (or null if removed)
                    _state.value.editPostImageUrl
                }

                // ✅ STEP 2: Update post with image
                postRepository.updatePost(
                    postId = post.id,
                    title = title,
                    content = content,
                    imageUrl = finalImageUrl,
                    adminUserId = currentAdminId
                )
                    .onSuccess {
                        Log.d("SingleUserCRUDViewModel", "Post updated with image")
                        _state.value = _state.value.copy(
                            isUpdatingPost = false,
                            showEditPostDialog = false,
                            selectedPost = null,
                            editPostImageUrl = null,
                            editPostNewImageUri = null
                        )
                    }
                    .onFailure { e ->
                        Log.e("SingleUserCRUDVM", "Error updating post: ${e.message}")
                        _state.value = _state.value.copy(
                            isUpdatingPost = false,
                            error = "Failed to update post: ${e.message}"
                        )
                    }
            } catch (e: Exception) {
                Log.e("SingleUserCRUDVM", "Error in savePostEdit: ${e.message}")
                _state.value = _state.value.copy(
                    isUpdatingPost = false,
                    error = "Failed to update post: ${e.message}"
                )
            }
        }
    }

    // ✅ NEW: Upload post image helper
    private suspend fun uploadPostImage(imageUri: Uri): Result<String> {
        return try {
            val imageRef = com.google.firebase.storage.FirebaseStorage.getInstance()
                .reference.child("post_images/${System.currentTimeMillis()}.jpg")

            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("SingleUserCRUDVM", "Error uploading post image: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Show delete post confirmation
     */
    fun showDeletePostDialog() {
        _state.value = _state.value.copy(
            showDeletePostDialog = true,
            showPostActionModal = false
        )
    }

    /**
     * Hide delete post dialog
     */
    fun hideDeletePostDialog() {
        _state.value = _state.value.copy(
            showDeletePostDialog = false
        )
    }

    /**
     * Delete post
     */
    fun deletePost() {
        val post = _state.value.selectedPost ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isDeletingPost = true)

            postRepository.deletePost(post.id, post.userId)
                .onSuccess {
                    Log.d("SingleUserCRUDVM", "Post deleted")
                    _state.value = _state.value.copy(
                        isDeletingPost = false,
                        showDeletePostDialog = false,
                        selectedPost = null
                    )
                }
                .onFailure { e ->
                    Log.e("SingleUserCRUDVM", "Error deleting post: ${e.message}")
                    _state.value = _state.value.copy(
                        isDeletingPost = false,
                        error = "Failed to delete post: ${e.message}"
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

data class SingleUserCRUDState(
    val userId: String = "",
    val username: String = "",
    val bio: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val age: Int? = null,
    val gender: Int = 2,
    val role: Int = 0,
    val postCount: Int = 0,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val userPosts: List<Post> = emptyList(),

    // Loading states
    val isLoading: Boolean = false,
    val isUpdatingUser: Boolean = false,
    val isDeletingUser: Boolean = false,
    val isUpdatingPost: Boolean = false,
    val isDeletingPost: Boolean = false,

    // Edit user dialog
    val showEditUserDialog: Boolean = false,
    val editUsername: String = "",
    val editBio: String = "",
    val editAge: String = "",
    val editGender: Int = 2,

    // Delete user dialog
    val showDeleteUserDialog: Boolean = false,
    val deleteConfirmUsername: String = "",

    // Post actions
    val selectedPost: Post? = null,
    val showPostActionModal: Boolean = false,
    val showEditPostDialog: Boolean = false,
    val showDeletePostDialog: Boolean = false,
    val editPostTitle: String = "",
    val editPostContent: String = "",
    val editPostImageUrl: String? = null,  // ✅ ADDED
    val editPostNewImageUri: Uri? = null,  // ✅ ADDED

    val error: String? = null
)