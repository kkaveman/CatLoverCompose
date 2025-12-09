package com.example.catlovercompose.feature.screens.community.adoption

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catlovercompose.core.model.PetShop
import com.example.catlovercompose.core.model.PetShopType
import com.example.catlovercompose.core.repository.AdoptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

data class AdoptionUiState(
    val petShops: List<PetShop> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val userLocation: GeoPoint? = null,
    val selectedPetShop: PetShop? = null,
    val filterType: PetShopType? = null,
    val searchRadius: Int = 5000, // meters
    val mapCenter: GeoPoint = GeoPoint(-6.2088, 106.8456), // Default: Jakarta
    val mapZoom: Double = 15.0
)

@HiltViewModel
class AdoptionViewModel @Inject constructor(
    private val adoptionRepository: AdoptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdoptionUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Update user location and search for nearby pet shops
     */
    fun updateUserLocation(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        _uiState.update {
            it.copy(
                userLocation = geoPoint,
                mapCenter = geoPoint
            )
        }
        searchNearbyPetShops(geoPoint)
    }

    /**
     * Search for pet shops near a location
     */
    fun searchNearbyPetShops(location: GeoPoint = _uiState.value.userLocation ?: _uiState.value.mapCenter) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = adoptionRepository.searchNearbyPetShops(
                location = location,
                radiusMeters = _uiState.value.searchRadius
            )

            result.fold(
                onSuccess = { petShops ->
                    _uiState.update {
                        it.copy(
                            petShops = applyFilter(petShops),
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load pet shops"
                        )
                    }
                }
            )
        }
    }

    /**
     * Select a pet shop to show details
     */
    fun selectPetShop(petShop: PetShop?) {
        _uiState.update {
            it.copy(
                selectedPetShop = petShop,
                mapCenter = petShop?.location ?: it.mapCenter
            )
        }
    }

    /**
     * Filter pet shops by type
     */
    fun setFilter(type: PetShopType?) {
        _uiState.update { state ->
            state.copy(
                filterType = type,
                petShops = applyFilter(state.petShops)
            )
        }
    }

    /**
     * Apply filter to pet shops list
     */
    private fun applyFilter(petShops: List<PetShop>): List<PetShop> {
        val filterType = _uiState.value.filterType ?: return petShops
        return petShops.filter { it.type == filterType }
    }

    /**
     * Update search radius
     */
    fun updateSearchRadius(radiusKm: Int) {
        _uiState.update { it.copy(searchRadius = radiusKm * 1000) }
        _uiState.value.userLocation?.let { location ->
            searchNearbyPetShops(location)
        }
    }

    /**
     * Update map center
     */
    fun updateMapCenter(geoPoint: GeoPoint) {
        _uiState.update { it.copy(mapCenter = geoPoint) }
    }

    /**
     * Update map zoom
     */
    fun updateMapZoom(zoom: Double) {
        _uiState.update { it.copy(mapZoom = zoom) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Retry search
     */
    fun retry() {
        _uiState.value.userLocation?.let { location ->
            searchNearbyPetShops(location)
        }
    }
}