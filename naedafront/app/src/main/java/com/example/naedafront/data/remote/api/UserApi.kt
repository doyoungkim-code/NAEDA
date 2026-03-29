package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.response.UserMeResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

interface UserApi {

    @GET("/api/users/me")
    suspend fun getMyInfo(
        @Query("userNo") userNo: Long
    ): UserMeResponse

    @PUT("/api/users/me/fcm-token")
    suspend fun updateFcmToken(
        @Query("userNo") userNo: Long,
        @Body request: Map<String, String>
    )
}