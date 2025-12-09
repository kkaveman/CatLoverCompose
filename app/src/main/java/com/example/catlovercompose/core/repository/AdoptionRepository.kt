package com.example.catlovercompose.core.repository

import android.util.Log
import com.example.catlovercompose.core.model.PetShop
import com.example.catlovercompose.core.model.PetShopType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class AdoptionRepository @Inject constructor() {

    companion object {
        private const val TAG = "AdoptionRepository"
        private const val OVERPASS_API_URL = "https://overpass-api.de/api/interpreter"
        private const val DEFAULT_RADIUS = 5000 // 5km
    }

    /**
     * Search for pet-related locations near a given location
     */
    suspend fun searchNearbyPetShops(
        location: GeoPoint,
        radiusMeters: Int = DEFAULT_RADIUS
    ): Result<List<PetShop>> = withContext(Dispatchers.IO) {
        try {
            val lat = location.latitude
            val lon = location.longitude

            // Overpass API query for pet-related locations
            val query = """
                [out:json][timeout:25];
                (
                  node["shop"="pet"](around:$radiusMeters,$lat,$lon);
                  node["shop"="pet_grooming"](around:$radiusMeters,$lat,$lon);
                  node["amenity"="veterinary"](around:$radiusMeters,$lat,$lon);
                  node["amenity"="animal_shelter"](around:$radiusMeters,$lat,$lon);
                  way["shop"="pet"](around:$radiusMeters,$lat,$lon);
                  way["shop"="pet_grooming"](around:$radiusMeters,$lat,$lon);
                  way["amenity"="veterinary"](around:$radiusMeters,$lat,$lon);
                  way["amenity"="animal_shelter"](around:$radiusMeters,$lat,$lon);
                );
                out center;
            """.trimIndent()

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "$OVERPASS_API_URL?data=$encodedQuery"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "CatLoverCompose/1.0")

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val petShops = parseOverpassResponse(response, location)
            Result.success(petShops)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching pet shops", e)
            Result.failure(e)
        }
    }

    /**
     * Parse Overpass API response
     */
    private fun parseOverpassResponse(jsonResponse: String, userLocation: GeoPoint): List<PetShop> {
        val petShops = mutableListOf<PetShop>()

        try {
            val jsonObject = JSONObject(jsonResponse)
            val elements = jsonObject.getJSONArray("elements")

            for (i in 0 until elements.length()) {
                val element = elements.getJSONObject(i)

                val lat: Double
                val lon: Double

                if (element.has("lat") && element.has("lon")) {
                    lat = element.getDouble("lat")
                    lon = element.getDouble("lon")
                } else if (element.has("center")) {
                    val center = element.getJSONObject("center")
                    lat = center.getDouble("lat")
                    lon = center.getDouble("lon")
                } else {
                    continue
                }

                val tags = if (element.has("tags")) element.getJSONObject("tags") else null
                val shopLocation = GeoPoint(lat, lon)

                val type = determinePetShopType(tags)
                val name = tags?.optString("name", type.displayName) ?: type.displayName
                val address = buildAddress(tags)
                val phone = tags?.optString("phone")
                val website = tags?.optString("website")
                val distance = calculateDistance(userLocation, shopLocation)

                petShops.add(
                    PetShop(
                        id = element.optLong("id", 0).toString(),
                        name = name,
                        type = type,
                        location = shopLocation,
                        address = address,
                        phone = phone?.takeIf { it.isNotEmpty() },
                        website = website?.takeIf { it.isNotEmpty() },
                        distance = distance
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
        }

        // Sort by distance
        return petShops.sortedBy { it.distance ?: Float.MAX_VALUE }
    }

    /**
     * Determine the type of pet shop from OSM tags
     */
    private fun determinePetShopType(tags: JSONObject?): PetShopType {
        if (tags == null) return PetShopType.UNKNOWN

        return when {
            tags.optString("shop") == "pet" -> PetShopType.PET_SHOP
            tags.optString("shop") == "pet_grooming" -> PetShopType.PET_GROOMING
            tags.optString("amenity") == "veterinary" -> PetShopType.VETERINARY
            tags.optString("amenity") == "animal_shelter" -> PetShopType.ANIMAL_SHELTER
            else -> PetShopType.UNKNOWN
        }
    }

    /**
     * Build address string from OSM tags
     */
    private fun buildAddress(tags: JSONObject?): String {
        if (tags == null) return "Address not available"

        val parts = mutableListOf<String>()

        tags.optString("addr:housenumber")?.let { if (it.isNotEmpty()) parts.add(it) }
        tags.optString("addr:street")?.let { if (it.isNotEmpty()) parts.add(it) }
        tags.optString("addr:city")?.let { if (it.isNotEmpty()) parts.add(it) }
        tags.optString("addr:postcode")?.let { if (it.isNotEmpty()) parts.add(it) }

        return if (parts.isEmpty()) "Address not available" else parts.joinToString(", ")
    }

    /**
     * Calculate distance between two points in kilometers
     */
    private fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Float {
        val earthRadius = 6371.0 // km

        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (earthRadius * c).toFloat()
    }
}