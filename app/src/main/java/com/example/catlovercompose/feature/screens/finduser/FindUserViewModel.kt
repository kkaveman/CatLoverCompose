package com.example.catlovercompose.feature.screens.finduser


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.UserProfile
import com.example.catlovercompose.core.repository.UserRepository
import com.example.catlovercompose.core.util.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FindUserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FindUserState())
    val state = _state.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun searchUsers() {
        val query = _state.value.searchQuery.trim()

        if (query.isBlank()) {
            _state.value = _state.value.copy(
                searchResults = emptyList(),
                isSearching = false,
                error = null
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true, error = null)

            userRepository.searchUsersByUsername(query)
                .onSuccess { users ->
                    // Filter out current user from results
                    val currentUserId = AuthState.getCurrentUser()?.uid
                    val filteredUsers = users.filter { it.uid != currentUserId }

                    _state.value = _state.value.copy(
                        searchResults = filteredUsers,
                        isSearching = false,
                        hasSearched = true
                    )
                    Log.d("FIND_USER", "Found ${filteredUsers.size} users")
                }
                .onFailure { e ->
                    Log.e("FIND_USER", "Search failed: ${e.message}")
                    _state.value = _state.value.copy(
                        isSearching = false,
                        error = "Failed to search users: ${e.message}"
                    )
                }
        }
    }

    fun clearSearch() {
        _state.value = FindUserState()
    }
}

data class FindUserState(
    val searchQuery: String = "",
    val searchResults: List<UserProfile> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)