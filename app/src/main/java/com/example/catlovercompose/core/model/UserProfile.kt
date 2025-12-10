package com.example.catlovercompose.core.model

import com.google.firebase.firestore.DocumentId

data class UserProfile(
    @DocumentId
    val uid: String = "",
    val username: String = "",
    val bio: String = "",
    val email: String = "",
    val profileImageUrl: String? = null,

    val age: Int? = null,
    val gender: Int = 2, // 0=male, 1=female, 2=other/unspecified
    val role: Int = 0, // 0=user, 1=admin
    val postCount: Int = 0,

    // ✅ NEW: Follower/Following counts
    val followerCount: Int = 0,
    val followingCount: Int = 0,

    val fcmToken: String? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// For Firestore, we need a map representation
fun UserProfile.toMap(): Map<String, Any?> = mapOf(
    "username" to username,
    "bio" to bio,
    "email" to email,
    "profileImageUrl" to profileImageUrl,
    "age" to age,
    "gender" to gender,
    "role" to role,
    "postCount" to postCount,

    // ✅ NEW: Include counts in map
    "followerCount" to followerCount,
    "followingCount" to followingCount,

    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)