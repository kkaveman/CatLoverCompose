package com.example.catlovercompose.feature.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.catlovercompose.core.model.Post
import com.example.catlovercompose.core.repository.PostRepository
import com.example.catlovercompose.core.repository.UserRepository
import com.example.catlovercompose.core.util.AuthState
import com.example.catlovercompose.navigation.NavDestinations
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init {
        loadUserData()
        loadUserPosts()
    }

    private fun loadUserData() {
        val user = AuthState.getCurrentUser() ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            // Now load from Firestore
            userRepository.getUserProfile(user.uid)
                .onSuccess { profile ->
                    _state.value = _state.value.copy(
                        username = profile?.username ?: user.displayName ?: "User",
                        bio = profile?.bio,
                        email = profile?.email ?: user.email ?: "No email",
                        profileImageUrl = profile?.profileImageUrl,
                        age = profile?.age,
                        gender = profile?.gender ?: 2,
                        postCount = profile?.postCount ?: 0,
                        isLoading = false
                    )
                }
                .onFailure { e ->
                    Log.e("PROFILE", "Failed to load profile: ${e.message}")
                    // Fallback to Firebase Auth data
                    _state.value = _state.value.copy(
                        username = user.displayName ?: user.email?.substringBefore("@") ?: "User",
                        bio = "",
                        email = user.email ?: "No email",
                        profileImageUrl = null,
                        age = null,
                        gender = 2,
                        postCount = 0,
                        isLoading = false,
                        error = "Failed to load profile"
                    )
                }
        }
    }

    private fun loadUserPosts() {
        val user = AuthState.getCurrentUser() ?: return

        viewModelScope.launch {
            postRepository.getUserPosts(user.uid).collect { posts ->
                _state.value = _state.value.copy(userPosts = posts)
                Log.d("PROFILE", "Loaded ${posts.size} user posts")
            }
        }
    }

    fun uploadProfilePicture(imageUri: Uri) {
        val user = AuthState.getCurrentUser() ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingImage = true, error = null)

            userRepository.uploadProfilePicture(user.uid, imageUri)
                .onSuccess { downloadUrl ->
                    Log.d("PROFILE", "Image uploaded: $downloadUrl")
                    _state.value = _state.value.copy(
                        profileImageUrl = downloadUrl,
                        isUploadingImage = false
                    )
                }
                .onFailure { e ->
                    Log.e("PROFILE", "Image upload failed: ${e.message}")
                    _state.value = _state.value.copy(
                        isUploadingImage = false,
                        error = "Failed to upload image: ${e.message}"
                    )
                }
        }
    }

    fun updateProfile(username: String, bio: String, age: Int?, gender: Int) {
        val user = AuthState.getCurrentUser() ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)

            // Update Firebase Auth display name
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(username)
                    .build()
            ).addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    Log.d("PROFILE", "Auth display name updated")
                }
            }

            // Update Firestore
            val updates = mapOf(
                "username" to username,
                "bio" to bio,
                "age" to age,
                "gender" to gender
            )

            userRepository.updateUserProfile(user.uid, updates)
                .onSuccess {
                    Log.d("PROFILE", "Profile updated successfully")
                    _state.value = _state.value.copy(
                        username = username,
                        bio = bio,
                        age = age,
                        gender = gender,
                        isSaving = false,
                        error = null
                    )
                }
                .onFailure { e ->
                    Log.e("PROFILE", "Profile update failed: ${e.message}")
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = "Failed to update profile: ${e.message}"
                    )
                }
        }
    }

    fun signOut(navController: NavController) {
        AuthState.signOut()
        navController.navigate(NavDestinations.SignIn.route) {
            popUpTo(0) { inclusive = true }
        }
    }
}

data class ProfileState(
    val username: String = "",
    val bio: String? = "",
    val email: String = "",
    val profileImageUrl: String? = null,
    val age: Int? = null,
    val gender: Int = 2, // 0=male, 1=female, 2=other
    val postCount: Int = 0,
    val userPosts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)