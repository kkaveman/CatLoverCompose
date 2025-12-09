package com.example.catlovercompose.core.model

import com.google.gson.annotations.SerializedName

// Cat Image Model
data class CatImage(
    val id: String,
    val url: String,
    val width: Int,
    val height: Int
)

// Cat Fact Model
data class CatFactResponse(
    val fact: String,
    val length: Int
)

// Cat Breed Model
data class CatBreedDetail(
    val id: String,
    val name: String,
    val description: String,
    val temperament: String,
    val origin: String,
    @SerializedName("life_span") val lifeSpan: String,
    @SerializedName("weight") val weight: CatWeight,
    @SerializedName("wikipedia_url") val wikipediaUrl: String?,
    val intelligence: Int?,
    @SerializedName("affection_level") val affectionLevel: Int?,
    @SerializedName("child_friendly") val childFriendly: Int?,
    @SerializedName("energy_level") val energyLevel: Int?
)

data class CatWeight(
    val imperial: String,
    val metric: String
)

// Cat Breed Image
data class CatBreedImage(
    val id: String,
    val url: String,
    val breeds: List<CatBreedDetail>?,
    val width: Int,
    val height: Int
)