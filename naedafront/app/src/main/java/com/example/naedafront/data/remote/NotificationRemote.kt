package com.example.naedafront.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class NotificationResponse(
    val notificationId: Long?,
    val userNo: Long?,
    val type: String?,
    val title: String?,
    val body: String?,
    val referenceId: Long?,
    val referenceType: String?,
    val isRead: Boolean?,
    val sent: String?
)

interface NotificationApi {
    @GET("api/notifications")
    suspend fun getNotifications(
        @Query("userNo") userNo: Long
    ): List<NotificationResponse>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(
        @Query("userNo") userNo: Long
    ): Map<String, Long>
}

object NotificationRepository {
    private val api = ApiConfig.retrofit.create(NotificationApi::class.java)

    suspend fun getNotifications(userNo: Long): Result<List<NotificationResponse>> = runCatching {
        api.getNotifications(userNo)
    }

    suspend fun getUnreadCount(userNo: Long): Result<Long> = runCatching {
        api.getUnreadCount(userNo).values.firstOrNull() ?: 0L
    }
}