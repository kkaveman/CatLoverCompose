package com.example.catlovercompose.core.repository

import android.util.Log
import com.example.catlovercompose.core.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val userRepository: UserRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val channelsCollection = firestore.collection("channels")

    /**
     * Generate channel ID from two user IDs (alphabetically sorted for consistency)
     */
    private fun generateChannelId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
    }

    /**
     * Search for a user by email
     */
    suspend fun searchUserByEmail(email: String): Result<UserProfile?> {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()

            val user = snapshot.documents.firstOrNull()?.toObject(UserProfile::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error searching user: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Create or get existing channel between two users
     */
    suspend fun createOrGetChannel(currentUserId: String, otherUserId: String): Result<String> {
        return try {
            Log.d("ChatRepository", "=== CREATE CHANNEL DEBUG ===")
            Log.d("ChatRepository", "Current User ID: $currentUserId")
            Log.d("ChatRepository", "Other User ID: $otherUserId")

            val channelId = generateChannelId(currentUserId, otherUserId)
            Log.d("ChatRepository", "Generated Channel ID: $channelId")

            // Get both users' info
            val currentUser = userRepository.getUserProfile(currentUserId).getOrNull()
            val otherUser = userRepository.getUserProfile(otherUserId).getOrNull()

            Log.d("ChatRepository", "Current User: ${currentUser?.username} (${currentUser?.email})")
            Log.d("ChatRepository", "Other User: ${otherUser?.username} (${otherUser?.email})")

            if (currentUser == null || otherUser == null) {
                Log.e("ChatRepository", "Failed to load user profiles")
                return Result.failure(Exception("Failed to load user profiles"))
            }

            // Create participant data
            val participantData = mapOf(
                currentUserId to ParticipantInfo(
                    username = currentUser.username,
                    email = currentUser.email,
                    profileImageUrl = currentUser.profileImageUrl
                ),
                otherUserId to ParticipantInfo(
                    username = otherUser.username,
                    email = otherUser.email,
                    profileImageUrl = otherUser.profileImageUrl
                )
            )

            // Create new channel
            val channel = Channel(
                id = channelId,
                participantIds = listOf(currentUserId, otherUserId),
                participantData = participantData,
                lastMessage = "",
                lastMessageTime = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val channelMap = channel.toMap()
            Log.d("ChatRepository", "Channel Map: $channelMap")
            Log.d("ChatRepository", "ParticipantIds in map: ${channelMap["participantIds"]}")

            // Use set with merge option to create if doesn't exist or keep existing
            // This avoids the permission denied error when checking if document exists
            channelsCollection.document(channelId)
                .set(channelMap, com.google.firebase.firestore.SetOptions.merge())
                .await()

            Log.d("ChatRepository", "Channel created/updated successfully: $channelId")

            Result.success(channelId)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating channel: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get all channels for a user (real-time)
     */
    fun getUserChannels(userId: String): Flow<List<Channel>> = callbackFlow {
        val listener = channelsCollection
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error listening to channels: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val channels = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Channel::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(channels)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get a specific channel by ID
     */
    suspend fun getChannel(channelId: String): Result<Channel?> {
        return try {
            val snapshot = channelsCollection.document(channelId).get().await()
            val channel = snapshot.toObject(Channel::class.java)?.copy(id = snapshot.id)
            Result.success(channel)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting channel: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Send a message in a channel
     */
    suspend fun sendMessage(channelId: String, senderId: String, text: String): Result<Unit> {
        return try {
            val message = Message(
                channelId = channelId,
                senderId = senderId,
                text = text,
                timestamp = System.currentTimeMillis(),
                read = false
            )

            // Add message to subcollection under the channel document
            channelsCollection
                .document(channelId)
                .collection("messages")
                .add(message.toMap())
                .await()

            // Update channel's last message
            channelsCollection.document(channelId).update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageTime" to message.timestamp,
                    "updatedAt" to message.timestamp
                )
            ).await()

            Log.d("ChatRepository", "Message sent successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get messages for a channel (real-time)
     */
    fun getChannelMessages(channelId: String): Flow<List<Message>> = callbackFlow {
        val listener = channelsCollection
            .document(channelId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error listening to messages: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }
}