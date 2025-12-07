package com.example.catlovercompose.core.model


import com.google.firebase.firestore.DocumentId

/**
 * Represents a follower/following relationship
 * Stored in subcollections: users/{uid}/followers and users/{uid}/following
 */
data class Follower(
    @DocumentId
    val uid: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val followedAt: Long = System.currentTimeMillis()
)

fun Follower.toMap(): Map<String, Any?> = mapOf(
    "username" to username,
    "profileImageUrl" to profileImageUrl,
    "followedAt" to followedAt
)