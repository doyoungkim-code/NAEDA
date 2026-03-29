package com.example.naedafront.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
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

data class NotificationSettingResponse(
    val settingId: Long?,
    val paymentEnabled: Boolean,
    val fdsEnabled: Boolean,
    val festivalEnabled: Boolean,
    val pointEnabled: Boolean,
    val systemEnabled: Boolean
)

data class NotificationSettingRequest(
    val paymentEnabled: Boolean,
    val fdsEnabled: Boolean,
    val festivalEnabled: Boolean,
    val pointEnabled: Boolean,
    val systemEnabled: Boolean
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

    @PATCH("api/notifications/{notificationId}/read")
    suspend fun markAsRead(
        @Path("notificationId") notificationId: Long
    )

    @PATCH("api/notifications/read-all")
    suspend fun markAllAsRead(
        @Query("userNo") userNo: Long
    )

    @GET("api/notification-settings/{userNo}")
    suspend fun getNotificationSettings(
        @Path("userNo") userNo: Long
    ): NotificationSettingResponse

    @POST("api/notification-settings/{userNo}")
    suspend fun createNotificationSettings(
        @Path("userNo") userNo: Long
    ): NotificationSettingResponse

    @PUT("api/notification-settings/{userNo}")
    suspend fun updateNotificationSettings(
        @Path("userNo") userNo: Long,
        @Body request: NotificationSettingRequest
    ): NotificationSettingResponse
}

object NotificationRepository {
    private val api = ApiConfig.retrofit.create(NotificationApi::class.java)

    suspend fun getNotifications(userNo: Long): Result<List<NotificationResponse>> = runCatching {
        api.getNotifications(userNo)
    }

    suspend fun getUnreadCount(userNo: Long): Result<Long> = runCatching {
        api.getUnreadCount(userNo).values.firstOrNull() ?: 0L
    }

    suspend fun markAsRead(notificationId: Long): Result<Unit> = runCatching {
        api.markAsRead(notificationId)
    }

    suspend fun markAllAsRead(userNo: Long): Result<Unit> = runCatching {
        api.markAllAsRead(userNo)
    }

    suspend fun getNotificationSettings(userNo: Long): Result<NotificationSettingResponse> = runCatching {
        try {
            api.getNotificationSettings(userNo)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) {
                api.createNotificationSettings(userNo)
            } else {
                throw e
            }
        }
    }

    suspend fun updateNotificationSettings(
        userNo: Long,
        request: NotificationSettingRequest
    ): Result<NotificationSettingResponse> = runCatching {
        api.updateNotificationSettings(userNo, request)
    }
}
