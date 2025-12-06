package com.example.catlovercompose.core.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a comment on a post
 * TODO: Implement comment functionality later
 */
data class Comment(
    @DocumentId
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorProfileUrl: String? = null,
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Convert Comment to Map for Firestore
 */
fun Comment.toMap(): Map<String, Any?> = mapOf(
    "postId" to postId,
    "userId" to userId,
    "authorName" to authorName,
    "authorProfileUrl" to authorProfileUrl,
    "content" to content,
    "createdAt" to createdAt
)