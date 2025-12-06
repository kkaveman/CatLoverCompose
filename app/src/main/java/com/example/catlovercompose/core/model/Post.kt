package com.example.catlovercompose.core.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a post in the community feed
 */
data class Post(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorProfileUrl: String? = null,
    val content: String = "",
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedBy: List<String> = emptyList(), // List of user IDs who liked this post
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Convert Post to Map for Firestore
 */
fun Post.toMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "authorName" to authorName,
    "authorProfileUrl" to authorProfileUrl,
    "content" to content,
    "imageUrl" to imageUrl,
    "likeCount" to likeCount,
    "commentCount" to commentCount,
    "likedBy" to likedBy,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)