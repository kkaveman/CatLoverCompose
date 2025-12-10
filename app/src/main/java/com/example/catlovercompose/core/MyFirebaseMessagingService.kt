package com.example.catlovercompose.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.catlovercompose.MainActivity
import com.example.catlovercompose.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "chat_notifications"
        private const val CHANNEL_NAME = "Chat Messages"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM token: $token")

        // Save token to Firestore when user is logged in
        val userId = com.example.catlovercompose.core.util.AuthState.getCurrentUser()?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM", "Token saved to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Failed to save token: ${e.message}")
                }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("FCM", "Message received from: ${message.from}")
        Log.d("FCM", "Message data: ${message.data}")

        // Get notification data
        val title = message.data["title"] ?: "New Message"
        val body = message.data["body"] ?: ""
        val channelId = message.data["channelId"]

        // Show notification
        showNotification(title, body, channelId)
    }

    private fun showNotification(title: String, messageText: String, channelId: String?) {
        createNotificationChannel()

        // Create intent to open chat when notification is clicked
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add channel ID so we can navigate to specific chat
            channelId?.let { putExtra("channelId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app icon
            .setContentTitle(title)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for chat messages"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}