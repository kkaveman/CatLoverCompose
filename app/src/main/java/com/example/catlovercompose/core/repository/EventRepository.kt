package com.example.catlovercompose.core.repository

import android.net.Uri
import android.util.Log
import com.example.catlovercompose.core.model.Event
import com.example.catlovercompose.core.model.toMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val userRepository: UserRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val eventsCollection = firestore.collection("events")
    private val storageRef = storage.reference

    /**
     * Create a new event
     */
    suspend fun createEvent(
        userId: String,
        title: String,
        description: String,
        imageUri: Uri?,
        startDate: Long,
        endDate: Long
    ): Result<String> {
        return try {
            // Upload image if provided
            val imageUrl = if (imageUri != null) {
                uploadEventImage(imageUri).getOrNull()
            } else null

            // Create event document
            val event = Event(
                title = title,
                description = description,
                imageUrl = imageUrl,
                startDate = startDate,
                endDate = endDate,
                participantCount = 0,
                participants = emptyList(),
                createdBy = userId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Add to Firestore
            val docRef = eventsCollection.add(event.toMap()).await()

            Log.d("EventRepository", "Event created: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error creating event: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Upload event image to Firebase Storage
     */
    private suspend fun uploadEventImage(imageUri: Uri): Result<String> {
        return try {
            val imageRef = storageRef.child("event_images/${System.currentTimeMillis()}.jpg")
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error uploading image: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all events (real-time)
     */
    fun getAllEvents(): Flow<List<Event>> = callbackFlow {
        val listener = eventsCollection
            .orderBy("startDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EventRepository", "Error listening to events: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(events)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get single event by ID
     */
    suspend fun getEvent(eventId: String): Result<Event?> {
        return try {
            val snapshot = eventsCollection.document(eventId).get().await()
            val event = snapshot.toObject(Event::class.java)?.copy(id = snapshot.id)
            Result.success(event)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error getting event: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get events that user has participated in (real-time)
     */
    fun getUserParticipatedEvents(userId: String): Flow<List<Event>> = callbackFlow {
        val listener = eventsCollection
            .whereArrayContains("participants", userId)
            .orderBy("startDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EventRepository", "Error listening to user events: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Event::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(events)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Join an event
     */
    suspend fun joinEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            val eventRef = eventsCollection.document(eventId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val event = snapshot.toObject(Event::class.java)

                if (event != null && !event.participants.contains(userId)) {
                    val updatedParticipants = event.participants + userId
                    transaction.update(
                        eventRef,
                        mapOf(
                            "participants" to updatedParticipants,
                            "participantCount" to updatedParticipants.size,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                }
            }.await()

            Log.d("EventRepository", "User joined event: $eventId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error joining event: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Leave an event
     */
    suspend fun leaveEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            val eventRef = eventsCollection.document(eventId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(eventRef)
                val event = snapshot.toObject(Event::class.java)

                if (event != null && event.participants.contains(userId)) {
                    val updatedParticipants = event.participants - userId
                    transaction.update(
                        eventRef,
                        mapOf(
                            "participants" to updatedParticipants,
                            "participantCount" to updatedParticipants.size,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                }
            }.await()

            Log.d("EventRepository", "User left event: $eventId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error leaving event: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete an event (only by creator or admin)
     */
    suspend fun deleteEvent(eventId: String, userId: String): Result<Unit> {
        return try {
            val eventSnapshot = eventsCollection.document(eventId).get().await()
            val event = eventSnapshot.toObject(Event::class.java)

            // Check if user is the creator
            if (event?.createdBy != userId) {
                // Check if user is admin
                val isAdmin = userRepository.isAdmin(userId)
                if (!isAdmin) {
                    return Result.failure(Exception("Unauthorized: Only event creator or admin can delete"))
                }
            }

            // Delete event image from Storage if exists
            event?.imageUrl?.let { imageUrl ->
                try {
                    val imageRef = storage.getReferenceFromUrl(imageUrl)
                    imageRef.delete().await()
                } catch (e: Exception) {
                    Log.w("EventRepository", "Could not delete image: ${e.message}")
                }
            }

            // Delete event from Firestore
            eventsCollection.document(eventId).delete().await()

            Log.d("EventRepository", "Event deleted: $eventId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EventRepository", "Error deleting event: ${e.message}")
            Result.failure(e)
        }
    }
}