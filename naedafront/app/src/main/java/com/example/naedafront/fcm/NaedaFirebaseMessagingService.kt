package com.example.naedafront.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.naedafront.MainActivity
import com.example.naedafront.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NaedaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: 서버에 토큰 전송
        // sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 알림 데이터 추출
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "내다"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: ""

        sendNotification(title, body)
    }

    private fun sendNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = CHANNEL_ID
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 이상 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "내다 앱 알림 채널"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        const val CHANNEL_ID   = "naeda_default_channel"
        const val CHANNEL_NAME = "내다 알림"
    }
}