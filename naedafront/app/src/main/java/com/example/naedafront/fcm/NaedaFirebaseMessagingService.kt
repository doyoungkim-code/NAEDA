package com.example.naedafront.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.naedafront.AuthPrefs
import com.example.naedafront.MainActivity
import com.example.naedafront.R
import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.data.remote.api.UserApi
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NaedaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "새 FCM 토큰 발급: $token")
        sendTokenToServer(token)
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

    private fun sendTokenToServer(token: String) {
        val userNo = AuthPrefs.getUserNo(applicationContext) ?: run {
            Log.w(TAG, "userNo 없음 — 로그인 후 토큰 전송 예정")
            return
        }

        val userApi = ApiConfig.retrofit.create(UserApi::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                userApi.updateFcmToken(userNo, mapOf("fcmToken" to token))
                Log.i(TAG, "FCM 토큰 서버 전송 성공: userNo=$userNo")
            } catch (e: Exception) {
                Log.e(TAG, "FCM 토큰 서버 전송 실패: ${e.message}")
            }
        }
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
        private const val TAG = "FCM_SERVICE"
        const val CHANNEL_ID   = "naeda_default_channel"
        const val CHANNEL_NAME = "내다 알림"

        /**
         * 앱 시작 시 또는 로그인 직후 호출하여 현재 토큰을 서버에 전송
         */
        fun registerCurrentToken(context: Context) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    val userNo = AuthPrefs.getUserNo(context) ?: run {
                        Log.w(TAG, "registerCurrentToken: userNo 없음")
                        return@addOnSuccessListener
                    }

                    val userApi = ApiConfig.retrofit.create(UserApi::class.java)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            userApi.updateFcmToken(userNo, mapOf("fcmToken" to token))
                            Log.i(TAG, "FCM 토큰 서버 등록 성공: userNo=$userNo, token=${token.take(10)}...")
                        } catch (e: Exception) {
                            Log.e(TAG, "FCM 토큰 서버 등록 실패: ${e.message}")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FCM 토큰 조회 실패: ${e.message}")
                }
        }
    }
}
