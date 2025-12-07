package com.example.catlovercompose.core.repository

import android.util.Log
import com.example.catlovercompose.core.model.Follower
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.example.catlovercompose.core.model.toMap
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowerRepository @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    /**
     * Follow a user
     * @param currentUserId - The user who is following
     * @param targetUserId - The user being followed
     */
    suspend fun followUser(
        currentUserId: String,
        targetUserId: String,
        currentUserInfo: Follower,
        targetUserInfo: Follower
    ): Result<Unit> {
        return try {
            // Use Firestore batch for atomic operations
            val batch = firestore.batch()

            // 1. Add target user to current user's "following" subcollection
            val followingRef = usersCollection
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)
            batch.set(followingRef, targetUserInfo.toMap())

            // 2. Add current user to target user's "followers" subcollection
            val followersRef = usersCollection
                .document(targetUserId)
                .collection("followers")
                .document(currentUserId)
            batch.set(followersRef, currentUserInfo.toMap())

            // 3. Increment current user's followingCount
            val currentUserRef = usersCollection.document(currentUserId)
            batch.update(currentUserRef, "followingCount", FieldValue.increment(1))

            // 4. Increment target user's followerCount
            val targetUserRef = usersCollection.document(targetUserId)
            batch.update(targetUserRef, "followerCount", FieldValue.increment(1))

            // Commit the batch
            batch.commit().await()

            Log.d("FOLLOWER_REPO", "Successfully followed user: $targetUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FOLLOWER_REPO", "Error following user: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Unfollow a user
     * @param currentUserId - The user who is unfollowing
     * @param targetUserId - The user being unfollowed
     */
    suspend fun unfollowUser(
        currentUserId: String,
        targetUserId: String
    ): Result<Unit> {
        return try {
            val batch = firestore.batch()

            // 1. Remove target user from current user's "following" subcollection
            val followingRef = usersCollection
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)
            batch.delete(followingRef)

            // 2. Remove current user from target user's "followers" subcollection
            val followersRef = usersCollection
                .document(targetUserId)
                .collection("followers")
                .document(currentUserId)
            batch.delete(followersRef)

            // 3. Decrement current user's followingCount
            val currentUserRef = usersCollection.document(currentUserId)
            batch.update(currentUserRef, "followingCount", FieldValue.increment(-1))

            // 4. Decrement target user's followerCount
            val targetUserRef = usersCollection.document(targetUserId)
            batch.update(targetUserRef, "followerCount", FieldValue.increment(-1))

            // Commit the batch
            batch.commit().await()

            Log.d("FOLLOWER_REPO", "Successfully unfollowed user: $targetUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FOLLOWER_REPO", "Error unfollowing user: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Check if current user is following target user
     */
    suspend fun isFollowing(currentUserId: String, targetUserId: String): Result<Boolean> {
        return try {
            val doc = usersCollection
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)
                .get()
                .await()

            Result.success(doc.exists())
        } catch (e: Exception) {
            Log.e("FOLLOWER_REPO", "Error checking follow status: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get list of followers for a user (real-time)
     */
    fun getFollowers(userId: String): Flow<List<Follower>> = callbackFlow {
        val listener = usersCollection
            .document(userId)
            .collection("followers")
            .orderBy("followedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FOLLOWER_REPO", "Error fetching followers: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val followers = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Follower::class.java)?.copy(uid = doc.id)
                } ?: emptyList()

                trySend(followers)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get list of following for a user (real-time)
     */
    fun getFollowing(userId: String): Flow<List<Follower>> = callbackFlow {
        val listener = usersCollection
            .document(userId)
            .collection("following")
            .orderBy("followedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FOLLOWER_REPO", "Error fetching following: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val following = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Follower::class.java)?.copy(uid = doc.id)
                } ?: emptyList()

                trySend(following)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get follower count (one-time fetch)
     */
    suspend fun getFollowerCount(userId: String): Result<Int> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val count = userDoc.getLong("followerCount")?.toInt() ?: 0
            Result.success(count)
        } catch (e: Exception) {
            Log.e("FOLLOWER_REPO", "Error getting follower count: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get following count (one-time fetch)
     */
    suspend fun getFollowingCount(userId: String): Result<Int> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val count = userDoc.getLong("followingCount")?.toInt() ?: 0
            Result.success(count)
        } catch (e: Exception) {
            Log.e("FOLLOWER_REPO", "Error getting following count: ${e.message}")
            Result.failure(e)
        }
    }
}