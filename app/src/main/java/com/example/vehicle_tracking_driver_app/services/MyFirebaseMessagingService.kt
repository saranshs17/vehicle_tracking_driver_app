package com.example.vehicle_tracking_driver_app.services


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.vehicle_tracking_driver_app.R
import com.example.vehicle_tracking_driver_app.activities.HomeActivity
import com.example.vehicle_tracking_driver_app.models.TokenUpdateRequest
import com.example.vehicle_tracking_driver_app.network.ApiService
import com.example.vehicle_tracking_driver_app.network.RetrofitClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message received from: ${remoteMessage.from}")
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "Notification", it.body ?: "")
        }
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        Log.d("FCM", "New token: $newToken")
        updateTokenOnBackend(newToken)
    }

    private fun updateTokenOnBackend(newToken: String) {
        val prefs = getSharedPreferences("driver_prefs", MODE_PRIVATE)
        val authToken = prefs.getString("token", "") ?: ""
        if(authToken.isNotEmpty()){
            val apiService = RetrofitClient.instance.create(ApiService::class.java)
            val tokenUpdateRequest = TokenUpdateRequest(newToken)
            apiService.updateToken("Bearer $authToken", tokenUpdateRequest)
                .enqueue(object: Callback<com.example.vehicle_tracking_driver_app.models.GenericResponse> {
                    override fun onResponse(
                        call: Call<com.example.vehicle_tracking_driver_app.models.GenericResponse>,
                        response: Response<com.example.vehicle_tracking_driver_app.models.GenericResponse>
                    ) {
                        if(response.isSuccessful){
                            Log.d("FCM", "Token updated on backend successfully.")
                        } else {
                            Log.e("FCM", "Failed to update token: ${response.errorBody()?.string()}")
                        }
                    }
                    override fun onFailure(call: Call<com.example.vehicle_tracking_driver_app.models.GenericResponse>, t: Throwable) {
                        Log.e("FCM", "Error updating token: ${t.message}")
                    }
                })
        } else {
            Log.d("FCM", "No auth token found, skipping token update.")
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = "default_channel_id"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Ensure you have an icon in your drawable resources.
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(channelId, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}
