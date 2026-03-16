package com.example.naedafront.data.remote

import com.example.naedafront.data.remote.request.SignUpRequest
import com.example.naedafront.data.remote.response.SignUpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("/api/auth/signup")
    suspend fun signUp(
        @Body request: SignUpRequest
    ): Response<SignUpResponse>
}