package com.example.catlovercompose.feature.screens.admin.usercrud

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.UserProfile
import com.example.catlovercompose.core.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserCRUDViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UserCRUDState())
    val state = _state.asStateFlow()

    init {
        loadAllUsers()
    }

    /**
     * Load all users from Firestore
     */
    private fun loadAllUsers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            userRepository.getAllUsers().collect { users ->
                _state.value = _state.value.copy(
                    allUsers = users,
                    displayedUsers = if (_state.value.searchQuery.isBlank()) {
                        users
                    } else {
                        _state.value.displayedUsers
                    },
                    isLoading = false
                )
                Log.d("UserCRUDViewModel", "Loaded ${users.size} users")
            }
        }
    }

    /**
     * Update search query
     */
    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)

        // If query is empty, show all users
        if (query.isBlank()) {
            _state.value = _state.value.copy(
                displayedUsers = _state.value.allUsers,
                hasSearched = false
            )
        }
    }

    /**
     * Search users by username or email
     */
    fun searchUsers() {
        val query = _state.value.searchQuery.trim()

        if (query.isBlank()) {
            _state.value = _state.value.copy(
                displayedUsers = _state.value.allUsers,
                hasSearched = false
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true)

            userRepository.searchUsers(query)
                .onSuccess { results ->
                    _state.value = _state.value.copy(
                        displayedUsers = results,
                        isSearching = false,
                        hasSearched = true,
                        error = null
                    )
                    Log.d("UserCRUDViewModel", "Search found ${results.size} users")
                }
                .onFailure { e ->
                    Log.e("UserCRUDViewModel", "Search error: ${e.message}")
                    _state.value = _state.value.copy(
                        isSearching = false,
                        hasSearched = true,
                        error = "Search failed: ${e.message}"
                    )
                }
        }
    }

    /**
     * Clear search and show all users
     */
    fun clearSearch() {
        _state.value = _state.value.copy(
            searchQuery = "",
            displayedUsers = _state.value.allUsers,
            hasSearched = false,
            error = null
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

data class UserCRUDState(
    val allUsers: List<UserProfile> = emptyList(),
    val displayedUsers: List<UserProfile> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)