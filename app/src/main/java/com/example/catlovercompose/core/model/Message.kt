package com.example.catlovercompose.core.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a single message in a chat
 */
data class Message(
    @DocumentId
    val id: String = "",
    val channelId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

/**
 * Convert Message to Map for Firestore
 */
fun Message.toMap(): Map<String, Any?> = mapOf(
    "channelId" to channelId,
    "senderId" to senderId,
    "text" to text,
    "timestamp" to timestamp,
    "read" to read
)