package com.example.catlovercompose.core.repository

import android.net.Uri
import android.util.Log
import com.example.catlovercompose.core.model.UserProfile
import com.example.catlovercompose.core.model.toMap
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    suspend fun searchUsersByUsername(query: String): Result<List<UserProfile>> {
        return try {
            if (query.isBlank()) {
                return Result.success(emptyList())
            }

            // Firestore doesn't support case-insensitive queries directly
            // So we fetch and filter on client side
            // For better performance, consider using Algolia or storing lowercase username field
            val snapshot = usersCollection
                .orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserProfile::class.java)
            }

            Result.success(users)
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

    //Admin Only Operations

    fun getAllUsers(): Flow<List<UserProfile>> = callbackFlow {
        val listener = usersCollection
            .orderBy("username")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Error fetching users: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserProfile::class.java)
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Search for users by username or email
     */

    suspend fun searchUsers(query: String): Result<List<UserProfile>> {
        return try {
            if (query.isBlank()) {
                return Result.success(emptyList())
            }

            // Search by username
            val usernameResults = usersCollection
                .orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .await()

            // Search by email
            val emailResults = usersCollection
                .orderBy("email")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .await()

            // Combine and deduplicate results
            val allUsers = (usernameResults.documents + emailResults.documents)
                .distinctBy { it.id }
                .mapNotNull { doc -> doc.toObject(UserProfile::class.java) }

            Result.success(allUsers)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error searching users: ${e.message}")
            Result.failure(e)
        }
    }


    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            // 1. Get user profile to check for profile picture
            val userProfile = getUserProfile(userId).getOrNull()

            // 2. Delete profile picture from Storage if exists
            userProfile?.profileImageUrl?.let { imageUrl ->
                try {
                    val imageRef = storage.getReferenceFromUrl(imageUrl)
                    imageRef.delete().await()
                    Log.d("UserRepository", "Deleted profile picture for user: $userId")
                } catch (e: Exception) {
                    Log.w("UserRepository", "Could not delete profile picture: ${e.message}")
                }
            }

            // 3. Remove user from all followers lists
            removeUserFromAllFollowers(userId).getOrThrow()

            // 4. Remove user from all following lists
            removeUserFromAllFollowing(userId).getOrThrow()

            // 5. Remove user's likes from all posts
            removeUserLikesFromPosts(userId).getOrThrow()

            // 6. Delete user document from Firestore
            usersCollection.document(userId).delete().await()
            Log.d("UserRepository", "Deleted user document: $userId")

            // 7. Delete user from Firebase Auth
            try {
                // Note: This requires Admin SDK on backend
                // For now, we'll just log it
                // In production, call a Cloud Function to delete auth user
                Log.w("UserRepository", "Auth deletion requires backend Cloud Function")
                // FirebaseAuth.getInstance().deleteUser(userId) // Requires Admin SDK
            } catch (e: Exception) {
                Log.w("UserRepository", "Could not delete auth user: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting user: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Remove user from all followers lists
     */
    private suspend fun removeUserFromAllFollowers(userId: String): Result<Unit> {
        return try {
            // Get all users who follow this user
            val followersSnapshot = usersCollection
                .document(userId)
                .collection("followers")
                .get()
                .await()

            // For each follower, remove this user from their "following" list
            followersSnapshot.documents.forEach { doc ->
                val followerId = doc.id
                usersCollection
                    .document(followerId)
                    .collection("following")
                    .document(userId)
                    .delete()
                    .await()

                // Decrement follower's following count
                usersCollection
                    .document(followerId)
                    .update("followingCount", FieldValue.increment(-1))
                    .await()
            }

            // Delete user's followers subcollection
            followersSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Log.d("UserRepository", "Removed user from all followers lists: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error removing user from followers: ${e.message}")
            Result.failure(e)
        }
    }


    /**
     * Remove user from all following lists
     */
    private suspend fun removeUserFromAllFollowing(userId: String): Result<Unit> {
        return try {
            // Get all users this user follows
            val followingSnapshot = usersCollection
                .document(userId)
                .collection("following")
                .get()
                .await()

            // For each followed user, remove this user from their "followers" list
            followingSnapshot.documents.forEach { doc ->
                val followedUserId = doc.id
                usersCollection
                    .document(followedUserId)
                    .collection("followers")
                    .document(userId)
                    .delete()
                    .await()

                // Decrement followed user's follower count
                usersCollection
                    .document(followedUserId)
                    .update("followerCount", FieldValue.increment(-1))
                    .await()
            }

            // Delete user's following subcollection
            followingSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Log.d("UserRepository", "Removed user from all following lists: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error removing user from following: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Remove user's likes from all posts
     */
    private suspend fun removeUserLikesFromPosts(userId: String): Result<Unit> {
        return try {
            val postsCollection = firestore.collection("posts")

            // Find all posts liked by this user
            val likedPostsSnapshot = postsCollection
                .whereArrayContains("likedBy", userId)
                .get()
                .await()

            // Remove user from likedBy array and decrement like count
            likedPostsSnapshot.documents.forEach { doc ->
                doc.reference.update(
                    mapOf(
                        "likedBy" to FieldValue.arrayRemove(userId),
                        "likeCount" to FieldValue.increment(-1)
                    )
                ).await()
            }

            Log.d("UserRepository", "Removed user likes from ${likedPostsSnapshot.size()} posts")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error removing user likes: ${e.message}")
            Result.failure(e)
        }
    }

    // Commented out for future development
// /**
//  * Ban user (disable account without deletion)
//  */
// suspend fun banUser(userId: String): Result<Unit> {
//     return try {
//         usersCollection.document(userId).update("isBanned", true).await()
//         Result.success(Unit)
//     } catch (e: Exception) {
//         Result.failure(e)
//     }
// }


}

