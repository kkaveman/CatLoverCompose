package com.example.catlovercompose.core.repository

import android.util.Log
import com.example.catlovercompose.core.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@Singleton
class CatapiRepository @Inject constructor() {

    private val gson = Gson()
    private val TAG = "CatApiRepository"

    // ⭐ Your actual API key from The Cat API
    private val apiKey = "live_eFn5q8ZawwKj1td1fnxbQaQd4gbAAu"

    /**
     * Get random cat images with API key
     */
    suspend fun getRandomCatImages(limit: Int = 10): Result<List<CatImage>> = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.thecatapi.com/v1/images/search?limit=$limit"
            Log.d(TAG, "Fetching images from: $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("x-api-key", apiKey)  // ⭐ Add API key header
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            Log.d(TAG, "Images response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                Log.d(TAG, "Images response: ${response.take(200)}...")

                val images = gson.fromJson<List<CatImage>>(response, object : TypeToken<List<CatImage>>() {}.type)
                Log.d(TAG, "Parsed ${images.size} images")
                Result.success(images)
            } else {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "Images error: $responseCode - $error")
                Result.failure(Exception("HTTP Error: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading images", e)
            Result.failure(Exception("Failed to load images: ${e.message}"))
        }
    }

    /**
     * Get random cat fact - Free API, no key needed
     */
    suspend fun getRandomCatFact(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://catfact.ninja/fact"
            Log.d(TAG, "Fetching fact from: $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d(TAG, "Fact response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }

                val fact = gson.fromJson(response, CatFactResponse::class.java)
                Log.d(TAG, "Got fact: ${fact.fact.take(50)}...")
                Result.success(fact.fact)
            } else {
                Result.failure(Exception("HTTP Error: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading fact", e)
            Result.failure(Exception("Failed to load fact: ${e.message}"))
        }
    }

    /**
     * Get all cat breeds with API key
     */
    suspend fun getAllBreeds(): Result<List<CatBreedDetail>> = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.thecatapi.com/v1/breeds"
            Log.d(TAG, "Fetching breeds from: $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("x-api-key", apiKey)  // ⭐ Add API key header
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            Log.d(TAG, "Breeds response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }
                Log.d(TAG, "Breeds response length: ${response.length}")

                val breeds = gson.fromJson<List<CatBreedDetail>>(response, object : TypeToken<List<CatBreedDetail>>() {}.type)
                Log.d(TAG, "Parsed ${breeds.size} breeds")
                Result.success(breeds)
            } else {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "Breeds error: $responseCode - $error")

                // Return sample breeds as fallback
                Log.d(TAG, "Using fallback breeds")
                Result.success(getSampleBreeds())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading breeds", e)
            // Return sample breeds as fallback
            Result.success(getSampleBreeds())
        }
    }

    /**
     * Fallback sample breeds in case API fails
     */
    private fun getSampleBreeds(): List<CatBreedDetail> {
        return listOf(
            CatBreedDetail(
                id = "abys",
                name = "Abyssinian",
                description = "The Abyssinian is easy to care for, and a joy to have in your home. They're affectionate cats and love both people and other animals.",
                temperament = "Active, Energetic, Independent, Intelligent, Gentle",
                origin = "Egypt",
                lifeSpan = "14 - 15",
                weight = CatWeight(imperial = "7 - 10", metric = "3 - 5"),
                wikipediaUrl = null,
                intelligence = 5,
                affectionLevel = 5,
                childFriendly = 3,
                energyLevel = 5
            ),
            CatBreedDetail(
                id = "pers",
                name = "Persian",
                description = "Persians are quiet, gentle, and love to lounge around. They're perfect for a calm household.",
                temperament = "Calm, Gentle, Sweet, Quiet",
                origin = "Iran",
                lifeSpan = "12 - 17",
                weight = CatWeight(imperial = "7 - 12", metric = "3 - 5"),
                wikipediaUrl = null,
                intelligence = 3,
                affectionLevel = 5,
                childFriendly = 4,
                energyLevel = 1
            ),
            CatBreedDetail(
                id = "siam",
                name = "Siamese",
                description = "Siamese cats are vocal, intelligent, and very social. They form strong bonds with their owners.",
                temperament = "Active, Agile, Clever, Sociable, Loving, Vocal",
                origin = "Thailand",
                lifeSpan = "12 - 15",
                weight = CatWeight(imperial = "6 - 14", metric = "3 - 6"),
                wikipediaUrl = null,
                intelligence = 5,
                affectionLevel = 5,
                childFriendly = 4,
                energyLevel = 5
            ),
            CatBreedDetail(
                id = "maine",
                name = "Maine Coon",
                description = "Maine Coons are gentle giants. They're friendly, playful, and get along with everyone.",
                temperament = "Adaptable, Friendly, Gentle, Intelligent, Playful",
                origin = "United States",
                lifeSpan = "12 - 15",
                weight = CatWeight(imperial = "12 - 18", metric = "5 - 8"),
                wikipediaUrl = null,
                intelligence = 5,
                affectionLevel = 5,
                childFriendly = 5,
                energyLevel = 3
            ),
            CatBreedDetail(
                id = "rag",
                name = "Ragdoll",
                description = "Ragdolls are docile, calm, and affectionate. They love to be held and cuddled.",
                temperament = "Affectionate, Calm, Gentle, Relaxed, Social",
                origin = "United States",
                lifeSpan = "12 - 17",
                weight = CatWeight(imperial = "10 - 20", metric = "5 - 9"),
                wikipediaUrl = null,
                intelligence = 3,
                affectionLevel = 5,
                childFriendly = 5,
                energyLevel = 2
            ),
            CatBreedDetail(
                id = "beng",
                name = "Bengal",
                description = "Bengals are active, playful cats with wild-looking markings. They love to climb and play.",
                temperament = "Alert, Agile, Energetic, Demanding, Intelligent",
                origin = "United States",
                lifeSpan = "12 - 16",
                weight = CatWeight(imperial = "8 - 15", metric = "4 - 7"),
                wikipediaUrl = null,
                intelligence = 5,
                affectionLevel = 4,
                childFriendly = 4,
                energyLevel = 5
            ),
            CatBreedDetail(
                id = "brit",
                name = "British Shorthair",
                description = "British Shorthairs are easygoing, calm cats with a teddy bear appearance.",
                temperament = "Affectionate, Easy Going, Gentle, Loyal, Patient, calm",
                origin = "United Kingdom",
                lifeSpan = "12 - 17",
                weight = CatWeight(imperial = "9 - 18", metric = "4 - 8"),
                wikipediaUrl = null,
                intelligence = 3,
                affectionLevel = 4,
                childFriendly = 5,
                energyLevel = 2
            ),
            CatBreedDetail(
                id = "sphy",
                name = "Sphynx",
                description = "The Sphynx is a hairless cat known for being energetic, affectionate, and attention-seeking.",
                temperament = "Active, Curious, Friendly, Gentle, Intelligent, Loyal",
                origin = "Canada",
                lifeSpan = "12 - 14",
                weight = CatWeight(imperial = "6 - 12", metric = "3 - 5"),
                wikipediaUrl = null,
                intelligence = 5,
                affectionLevel = 5,
                childFriendly = 4,
                energyLevel = 5
            )
        )
    }

    /**
     * Get images for a specific breed with API key
     */
    suspend fun getBreedImages(breedId: String, limit: Int = 5): Result<List<CatBreedImage>> = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.thecatapi.com/v1/images/search?breed_ids=$breedId&limit=$limit"
            Log.d(TAG, "Fetching breed images from: $urlString")

            val url = URL(urlString)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("x-api-key", apiKey)  // ⭐ Add API key header
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.use { it.readText() }

                val images = gson.fromJson<List<CatBreedImage>>(response, object : TypeToken<List<CatBreedImage>>() {}.type)
                Result.success(images)
            } else {
                Result.failure(Exception("HTTP Error: $responseCode"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading breed images", e)
            Result.failure(Exception("Failed to load breed images: ${e.message}"))
        }
    }
}