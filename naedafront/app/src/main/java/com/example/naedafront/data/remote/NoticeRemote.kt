package com.example.naedafront.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

data class NoticeApiResponse(
    val noticeId: Long?,
    val title: String?,
    val content: String?,
    val fcmNotified: Boolean?,
    val created: String?,
    val modified: String?
)

data class FestivalApiResponse(
    val festivalId: Long?,
    val title: String?,
    val description: String?,
    val location: String?,
    val roadAddress: String?,
    val numberAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val linkUrl: String?,
    val imageUrl: String?,
    val startDate: String?,
    val endDate: String?,
    val fcmNotified: Boolean?,
    val created: String?
)

interface NoticeApi {
    @GET("api/notices")
    suspend fun getAllNotices(): List<NoticeApiResponse>

    @GET("api/notices/{noticeId}")
    suspend fun getNotice(@Path("noticeId") noticeId: Long): NoticeApiResponse

    @GET("api/festivals")
    suspend fun getAllFestivals(): List<FestivalApiResponse>

    @GET("api/festivals/{festivalId}")
    suspend fun getFestival(@Path("festivalId") festivalId: Long): FestivalApiResponse
}
