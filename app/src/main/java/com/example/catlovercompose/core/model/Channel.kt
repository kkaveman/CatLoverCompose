package com.example.catlovercompose.core.model

import com.google.firebase.firestore.DocumentId

/**
 * Represents a chat channel between two users
 */
data class Channel(
    @DocumentId
    val id: String = "",
    val participantIds: List<String> = emptyList(), // [userId1, userId2]
    val participantData: Map<String, ParticipantInfo> = emptyMap(), // userId -> user info
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * User info stored in channel for quick display
 */
data class ParticipantInfo(
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String? = null
)

/**
 * Helper function to get the other participant's info
 */
fun Channel.getOtherParticipant(currentUserId: String): ParticipantInfo? {
    val otherUserId = participantIds.firstOrNull { it != currentUserId }
    return otherUserId?.let { participantData[it] }
}

/**
 * Convert Channel to Map for Firestore
 */
fun Channel.toMap(): Map<String, Any?> = mapOf(
    "participantIds" to participantIds,
    "participantData" to participantData.mapValues { (_, info) ->
        mapOf(
            "username" to info.username,
            "email" to info.email,
            "profileImageUrl" to info.profileImageUrl
        )
    },
    "lastMessage" to lastMessage,
    "lastMessageTime" to lastMessageTime,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)