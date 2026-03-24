package com.example.naedafront.data.remote

import com.example.naedafront.data.remote.request.LogoutRequest
import com.example.naedafront.data.remote.request.SignUpRequest
import com.example.naedafront.data.remote.response.SignUpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {

    @POST("/api/auth/signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response<SignUpResponse>

    @GET("/api/auth/check-email")
    suspend fun checkEmail(
        @Query("email") email: String
    ): Response<Unit>

    @GET("/api/auth/check-phone")
    suspend fun checkPhone(
        @Query("phone") phone: String
    ): Response<Unit>

    @POST("/api/auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest
    ): Response<Unit>
}