package com.example.catlovercompose.core.repository

import android.net.Uri
import android.util.Log
import com.example.catlovercompose.core.model.Post
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
class PostRepository @Inject constructor(
    private val userRepository: UserRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val postsCollection = firestore.collection("posts")
    private val usersCollection = firestore.collection("users")
    private val storageRef = storage.reference

    /**
     * Create a new post with optional image
     */
    suspend fun createPost(
        userId: String,
        title : String,
        content: String,
        imageUri: Uri?
    ): Result<String> {
        return try {
            // Get user info
            val userProfile = userRepository.getUserProfile(userId).getOrNull()
                ?: return Result.failure(Exception("User profile not found"))

            // Upload image if provided
            val imageUrl = if (imageUri != null) {
                uploadPostImage(imageUri).getOrNull()
            } else null

            // Create post document
            val post = Post(
                userId = userId,
                authorName = userProfile.username,
                authorProfileUrl = userProfile.profileImageUrl,
                title = title,
                content = content,
                imageUrl = imageUrl,
                likeCount = 0,
                commentCount = 0,
                likedBy = emptyList(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Add to Firestore
            val docRef = postsCollection.add(post.toMap()).await()

            // Update user's post count
            usersCollection.document(userId).update(
                mapOf(
                    "postCount" to com.google.firebase.firestore.FieldValue.increment(1)
                )
            ).await()

            Log.d("PostRepository", "Post created: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error creating post: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Upload post image to Firebase Storage
     */
    private suspend fun uploadPostImage(imageUri: Uri): Result<String> {
        return try {
            val imageRef = storageRef.child("post_images/${System.currentTimeMillis()}.jpg")
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error uploading image: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all posts (global feed) - real-time
     */
    fun getAllPosts(): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PostRepository", "Error listening to posts: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(posts)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get posts by specific user
     */
    fun getUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PostRepository", "Error listening to user posts: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(posts)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Like a post
     */
    suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)

            // ✅ FIXED: Use FieldValue operations instead of transaction
            postRef.update(
                mapOf(
                    "likedBy" to com.google.firebase.firestore.FieldValue.arrayUnion(userId),
                    "likeCount" to com.google.firebase.firestore.FieldValue.increment(1),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            Log.d("PostRepository", "Post liked: $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error liking post: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Unlike a post
     */
    suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)

            // ✅ FIXED: Use FieldValue operations instead of transaction
            postRef.update(
                mapOf(
                    "likedBy" to com.google.firebase.firestore.FieldValue.arrayRemove(userId),
                    "likeCount" to com.google.firebase.firestore.FieldValue.increment(-1),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            Log.d("PostRepository", "Post unliked: $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error unliking post: ${e.message}")
            Result.failure(e)
        }
    }
    /**
     * Delete a post (only by owner)
     */
    suspend fun deletePost(postId: String, userId: String): Result<Unit> {
        return try {
            val postSnapshot = postsCollection.document(postId).get().await()
            val post = postSnapshot.toObject(Post::class.java)

            // Check if user is the owner
            if (post?.userId != userId) {
                return Result.failure(Exception("Unauthorized: You can only delete your own posts"))
            }

            // Delete post image from Storage if exists
            post.imageUrl?.let { imageUrl ->
                try {
                    val imageRef = storage.getReferenceFromUrl(imageUrl)
                    imageRef.delete().await()
                } catch (e: Exception) {
                    Log.w("PostRepository", "Could not delete image: ${e.message}")
                }
            }

            // Delete post from Firestore
            postsCollection.document(postId).delete().await()

            // Decrement user's post count
            usersCollection.document(userId).update(
                mapOf(
                    "postCount" to com.google.firebase.firestore.FieldValue.increment(-1)
                )
            ).await()

            Log.d("PostRepository", "Post deleted: $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error deleting post: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get single post by ID
     */
    suspend fun getPost(postId: String): Result<Post?> {
        return try {
            val snapshot = postsCollection.document(postId).get().await()
            val post = snapshot.toObject(Post::class.java)?.copy(id = snapshot.id)
            Result.success(post)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error getting post: ${e.message}")
            Result.failure(e)
        }
    }

    //admin operations

    /**
     * Update post with image (admin/owner edit)
     */
    suspend fun updatePost(
        postId: String,
        title: String,
        content: String,
        imageUrl: String?,
        adminUserId: String
    ): Result<Unit> {
        return try {
            postsCollection.document(postId).update(
                mapOf(
                    "title" to title,
                    "content" to content,
                    "imageUrl" to imageUrl,  // ✅ Can be null to remove image
                    "lastEditedBy" to adminUserId,
                    "lastEditedAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            Log.d("PostRepository", "Post updated with image: $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error updating post: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteAllUserPosts(userId: String): Result<Int> {
        return try {
            val userPostsSnapshot = postsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            var deletedCount = 0

            userPostsSnapshot.documents.forEach { doc ->
                val post = doc.toObject(Post::class.java)

                // Delete post image from Storage if exists
                post?.imageUrl?.let { imageUrl ->
                    try {
                        val imageRef = storage.getReferenceFromUrl(imageUrl)
                        imageRef.delete().await()
                    } catch (e: Exception) {
                        Log.w("PostRepository", "Could not delete post image: ${e.message}")
                    }
                }

                // Delete post document
                doc.reference.delete().await()
                deletedCount++
            }

            // Reset user's post count to 0
            usersCollection.document(userId).update("postCount", 0).await()

            Log.d("PostRepository", "Deleted $deletedCount posts for user: $userId")
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e("PostRepository", "Error deleting user posts: ${e.message}")
            Result.failure(e)
        }
    }




}