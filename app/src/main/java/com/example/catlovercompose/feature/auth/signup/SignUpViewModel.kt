package com.example.catlovercompose.feature.auth.signup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.UserProfile
import com.example.catlovercompose.core.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SignUpState>(SignUpState.Nothing)
    val state = _state.asStateFlow()

    fun signUp(username: String, email: String, passwd: String) {
        _state.value = SignUpState.Loading

        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email, passwd)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    _state.value = SignUpState.Error
                    return@addOnCompleteListener
                }

                val user = task.result.user ?: run {
                    _state.value = SignUpState.Error
                    return@addOnCompleteListener
                }

                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()
                ).addOnCompleteListener {
                    addUserToFirestore(
                        uid = user.uid,
                        username = username,
                        email = email
                    )
                }
            }
    }

    private fun addUserToFirestore(uid: String, username: String, email: String) {
        viewModelScope.launch {
            val userProfile = UserProfile(
                uid = uid,
                username = username,
                email = email,
                profileImageUrl = null,
                age = null,
                gender = 2,
                role = 0,
                postCount = 0,

            )

            userRepository.createUserProfile(userProfile)
                .onSuccess {
                    Log.d("FIRESTORE", "User added successfully")
                    _state.value = SignUpState.Success
                }
                .onFailure { e ->
                    Log.e("FIRESTORE", "Failed to add user: ${e.message}")
                    _state.value = SignUpState.Error
                }
        }
    }
}

sealed class SignUpState {
    object Nothing : SignUpState()
    object Loading : SignUpState()
    object Success : SignUpState()
    object Error : SignUpState()
}