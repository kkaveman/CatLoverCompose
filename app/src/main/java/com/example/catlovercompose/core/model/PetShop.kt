package com.example.catlovercompose.core.model

import org.osmdroid.util.GeoPoint

/**
 * Represents a pet-related location (shop, veterinary, grooming)
 */
data class PetShop(
    val id: String = "",
    val name: String,
    val type: PetShopType,
    val location: GeoPoint,
    val address: String,
    val phone: String? = null,
    val website: String? = null,
    val rating: Float? = null,
    val distance: Float? = null // in kilometers
)

enum class PetShopType(val displayName: String) {
    PET_SHOP("Pet Shop"),
    VETERINARY("Veterinary"),
    PET_GROOMING("Pet Grooming"),
    ANIMAL_SHELTER("Animal Shelter"),
    UNKNOWN("Location")
}