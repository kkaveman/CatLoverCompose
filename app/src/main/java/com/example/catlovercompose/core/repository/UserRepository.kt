package com.example.catlovercompose.core.repository

import android.net.Uri
import com.example.catlovercompose.core.model.UserProfile
import com.example.catlovercompose.core.model.toMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val usersCollection = firestore.collection("users")
    private val storageRef = storage.reference

    /**
     * Create a new user profile in Firestore
     */
    suspend fun createUserProfile(userProfile: UserProfile): Result<Unit> {
        return try {
            usersCollection
                .document(userProfile.uid)
                .set(userProfile.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user profile from Firestore
     */
    suspend fun getUserProfile(uid: String): Result<UserProfile?> {
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            val profile = snapshot.toObject(UserProfile::class.java)
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile in Firestore
     */
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            val updatesWithTimestamp = updates.toMutableMap().apply {
                put("updatedAt", System.currentTimeMillis())
            }
            usersCollection.document(uid).update(updatesWithTimestamp).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload profile picture to Firebase Storage and return download URL
     */
    suspend fun uploadProfilePicture(uid: String, imageUri: Uri): Result<String> {
        return try {
            val profileImageRef = storageRef.child("profile_pictures/$uid.jpg")

            // Upload the file
            profileImageRef.putFile(imageUri).await()

            // Get the download URL
            val downloadUrl = profileImageRef.downloadUrl.await().toString()

            // Update Firestore with the new image URL
            updateUserProfile(uid, mapOf("profileImageUrl" to downloadUrl))

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete profile picture from Storage (optional, for cleanup)
     */
    suspend fun deleteProfilePicture(uid: String): Result<Unit> {
        return try {
            val profileImageRef = storageRef.child("profile_pictures/$uid.jpg")
            profileImageRef.delete().await()

            // Remove URL from Firestore
            updateUserProfile(uid, mapOf("profileImageUrl" to null))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user is admin
     */
    suspend fun isAdmin(uid: String): Boolean {
        return try {
            val profile = getUserProfile(uid).getOrNull()
            profile?.role == 1
        } catch (e: Exception) {
            false
        }
    }
}