package com.example.catlovercompose.core.model

import com.google.firebase.firestore.DocumentId

data class Event(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val participantCount: Int = 0,
    val participants: List<String> = emptyList(), // List of user IDs who joined
    val createdBy: String = "", // User ID of creator (admin)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Convert Event to Map for Firestore
 */
fun Event.toMap(): Map<String, Any?> = mapOf(
    "title" to title,
    "description" to description,
    "imageUrl" to imageUrl,
    "startDate" to startDate,
    "endDate" to endDate,
    "participantCount" to participantCount,
    "participants" to participants,
    "createdBy" to createdBy,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

/**
 * Check if user has joined this event
 */
fun Event.isJoinedBy(userId: String): Boolean {
    return participants.contains(userId)
}

/**
 * Check if event is ongoing
 */
fun Event.isOngoing(): Boolean {
    val now = System.currentTimeMillis()
    return now >= startDate && now <= endDate
}

/**
 * Check if event has ended
 */
fun Event.hasEnded(): Boolean {
    return System.currentTimeMillis() > endDate
}

/**
 * Check if event hasn't started yet
 */
fun Event.isUpcoming(): Boolean {
    return System.currentTimeMillis() < startDate
}