package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.response.UserMeResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface UserApi {

    @GET("/api/users/me")
    suspend fun getMyInfo(
        @Query("userNo") userNo: Long
    ): UserMeResponse
}