package com.example.catlovercompose.core.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AuthState {
    private val firestore = FirebaseFirestore.getInstance()

    fun getCurrentUser(): FirebaseUser? = FirebaseAuth.getInstance().currentUser

    fun isUserSignedIn(): Boolean = getCurrentUser() != null

    suspend fun isAdmin(): Boolean {
        val uid = getCurrentUser()?.uid ?: return false

        return try {
            val snapshot = firestore
                .collection("users")
                .document(uid)
                .get()
                .await()

            val roleInt = snapshot.getLong("role")?.toInt() ?: 0
            roleInt == 1
        } catch (e: Exception) {
            false
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}