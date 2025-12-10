package com.example.catlovercompose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.catlovercompose.core.util.AuthState
import com.example.catlovercompose.core.util.SettingsDatastore
import com.example.catlovercompose.ui.theme.CatLoverComposeTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Request notification permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
            saveFCMToken()
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission
        requestNotificationPermission()

        setContent {
            val darkModeEnabled by SettingsDatastore.getDarkModeEnabled(this)
                .collectAsState(initial = false)

            CatLoverComposeTheme(
                darkTheme = darkModeEnabled
            ) {
                MainApp()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Permission already granted")
                    saveFCMToken()
                }
                else -> {
                    Log.d("MainActivity", "Requesting permission")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // No permission needed for Android 12 and below
            saveFCMToken()
        }
    }

    private fun saveFCMToken() {
        val userId = AuthState.getCurrentUser()?.uid
        if (userId == null) {
            Log.d("MainActivity", "No user logged in, not saving token")
            return
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("MainActivity", "FCM Token: $token")

            // Save token to Firestore
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("MainActivity", "FCM token saved successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Failed to save FCM token: ${e.message}")
                }
        }.addOnFailureListener { e ->
            Log.e("MainActivity", "Failed to get FCM token: ${e.message}")
        }
    }
}