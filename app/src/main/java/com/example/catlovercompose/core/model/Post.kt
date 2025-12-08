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
    val title: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val likedBy: List<String> = emptyList(),

    // ✅ NEW: Edit tracking for admin
    val lastEditedBy: String? = null,  // Admin user ID who last edited
    val lastEditedAt: Long? = null,     // Timestamp of last edit

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
    "title" to title,
    "content" to content,
    "imageUrl" to imageUrl,
    "likeCount" to likeCount,
    "commentCount" to commentCount,
    "likedBy" to likedBy,


    "lastEditedBy" to lastEditedBy,
    "lastEditedAt" to lastEditedAt,

    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)