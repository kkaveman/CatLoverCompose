package com.example.catlovercompose.feature.screens.community.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.CatBreedDetail
import com.example.catlovercompose.core.model.CatImage
import com.example.catlovercompose.core.repository.CatapiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InfoTab {
    RANDOM_CATS,
    BREEDS,
    FACTS
}

data class InformationUiState(
    val selectedTab: InfoTab = InfoTab.RANDOM_CATS,
    val randomImages: List<CatImage> = emptyList(),
    val breeds: List<CatBreedDetail> = emptyList(),
    val currentFact: String? = null,
    val selectedBreed: CatBreedDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class InformationViewModel @Inject constructor(
    private val repository: CatapiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InformationUiState())
    val uiState: StateFlow<InformationUiState> = _uiState.asStateFlow()

    init {
        loadRandomImages()
        loadRandomFact()
    }

    fun selectTab(tab: InfoTab) {
        _uiState.update { it.copy(selectedTab = tab) }

        when (tab) {
            InfoTab.RANDOM_CATS -> {
                if (_uiState.value.randomImages.isEmpty()) {
                    loadRandomImages()
                }
            }
            InfoTab.BREEDS -> {
                if (_uiState.value.breeds.isEmpty()) {
                    loadBreeds()
                }
            }
            InfoTab.FACTS -> {
                if (_uiState.value.currentFact == null) {
                    loadRandomFact()
                }
            }
        }
    }

    fun loadRandomImages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getRandomCatImages(10).fold(
                onSuccess = { images ->
                    _uiState.update {
                        it.copy(
                            randomImages = images,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load images: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun loadBreeds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getAllBreeds().fold(
                onSuccess = { breeds ->
                    _uiState.update {
                        it.copy(
                            breeds = breeds,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to load breeds: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun loadRandomFact() {
        viewModelScope.launch {
            repository.getRandomCatFact().fold(
                onSuccess = { fact ->
                    _uiState.update { it.copy(currentFact = fact) }
                },
                onFailure = { /* Silently fail for facts */ }
            )
        }
    }

    fun selectBreed(breed: CatBreedDetail) {
        _uiState.update { it.copy(selectedBreed = breed) }
    }

    fun clearSelectedBreed() {
        _uiState.update { it.copy(selectedBreed = null) }
    }

    fun refreshCurrentTab() {
        when (_uiState.value.selectedTab) {
            InfoTab.RANDOM_CATS -> loadRandomImages()
            InfoTab.BREEDS -> loadBreeds()
            InfoTab.FACTS -> loadRandomFact()
        }
    }
}