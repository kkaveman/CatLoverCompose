package com.example.catlovercompose.feature.screens.finduser.otherprofile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.Follower
import com.example.catlovercompose.core.model.Post
import com.example.catlovercompose.core.repository.FollowerRepository
import com.example.catlovercompose.core.repository.PostRepository
import com.example.catlovercompose.core.repository.UserRepository
import com.example.catlovercompose.core.util.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtherProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val followerRepository: FollowerRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OtherProfileState())
    val state = _state.asStateFlow()

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, userId = userId)

            // Load user profile
            userRepository.getUserProfile(userId)
                .onSuccess { profile ->
                    _state.value = _state.value.copy(
                        username = profile?.username ?: "User",
                        bio = profile?.bio,
                        profileImageUrl = profile?.profileImageUrl,
                        followerCount = profile?.followerCount ?: 0,
                        followingCount = profile?.followingCount ?: 0,
                        postCount = profile?.postCount ?: 0,
                        isLoading = false
                    )

                    // Check if current user is following this user
                    checkFollowStatus(userId)

                    // Load user's posts
                    loadUserPosts(userId)
                }
                .onFailure { e ->
                    Log.e("OTHER_PROFILE", "Failed to load profile: ${e.message}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to load profile"
                    )
                }
        }
    }

    private fun checkFollowStatus(targetUserId: String) {
        val currentUserId = AuthState.getCurrentUser()?.uid ?: return

        viewModelScope.launch {
            followerRepository.isFollowing(currentUserId, targetUserId)
                .onSuccess { isFollowing ->
                    _state.value = _state.value.copy(isFollowing = isFollowing)
                }
        }
    }

    private fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            postRepository.getUserPosts(userId).collect { posts ->
                _state.value = _state.value.copy(userPosts = posts)
            }
        }
    }

    fun followUser() {
        val currentUser = AuthState.getCurrentUser() ?: return
        val targetUserId = _state.value.userId

        viewModelScope.launch {
            _state.value = _state.value.copy(isFollowActionLoading = true)

            // Get current user's profile
            userRepository.getUserProfile(currentUser.uid)
                .onSuccess { currentUserProfile ->
                    val currentUserInfo = Follower(
                        uid = currentUser.uid,
                        username = currentUserProfile?.username ?: "User",
                        profileImageUrl = currentUserProfile?.profileImageUrl
                    )

                    val targetUserInfo = Follower(
                        uid = targetUserId,
                        username = _state.value.username,
                        profileImageUrl = _state.value.profileImageUrl
                    )

                    followerRepository.followUser(
                        currentUserId = currentUser.uid,
                        targetUserId = targetUserId,
                        currentUserInfo = currentUserInfo,
                        targetUserInfo = targetUserInfo
                    ).onSuccess {
                        _state.value = _state.value.copy(
                            isFollowing = true,
                            isFollowActionLoading = false,
                            followerCount = _state.value.followerCount + 1
                        )
                        Log.d("OTHER_PROFILE", "Successfully followed user")
                    }.onFailure { e ->
                        Log.e("OTHER_PROFILE", "Failed to follow: ${e.message}")
                        _state.value = _state.value.copy(
                            isFollowActionLoading = false,
                            error = "Failed to follow user"
                        )
                    }
                }
        }
    }

    fun unfollowUser() {
        val currentUserId = AuthState.getCurrentUser()?.uid ?: return
        val targetUserId = _state.value.userId

        viewModelScope.launch {
            _state.value = _state.value.copy(isFollowActionLoading = true)

            followerRepository.unfollowUser(currentUserId, targetUserId)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isFollowing = false,
                        isFollowActionLoading = false,
                        followerCount = _state.value.followerCount - 1
                    )
                    Log.d("OTHER_PROFILE", "Successfully unfollowed user")
                }
                .onFailure { e ->
                    Log.e("OTHER_PROFILE", "Failed to unfollow: ${e.message}")
                    _state.value = _state.value.copy(
                        isFollowActionLoading = false,
                        error = "Failed to unfollow user"
                    )
                }
        }
    }

    fun showFollowersDialog() {
        // Will implement in Phase 4
    }

    fun showFollowingDialog() {
        // Will implement in Phase 4
    }
}

data class OtherProfileState(
    val userId: String = "",
    val username: String = "",
    val bio: String? = null,
    val profileImageUrl: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postCount: Int = 0,
    val userPosts: List<Post> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoading: Boolean = false,
    val isFollowActionLoading: Boolean = false,
    val error: String? = null
)