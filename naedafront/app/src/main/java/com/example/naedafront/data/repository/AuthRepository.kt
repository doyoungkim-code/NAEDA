package com.example.naedafront.data.repository

import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.data.remote.AuthApi
import com.example.naedafront.data.remote.request.SignUpRequest
import com.example.naedafront.data.remote.response.SignUpResponse
import retrofit2.Response

class AuthRepository {

    private val authApi: AuthApi = ApiConfig.retrofit.create(AuthApi::class.java)

    suspend fun signUp(request: SignUpRequest): Response<SignUpResponse> {
        return authApi.signUp(request)
    }

    suspend fun checkEmail(email: String): Response<Unit> {
        return authApi.checkEmail(email)
    }

    suspend fun checkPhone(phone: String): Response<Unit> {
        return authApi.checkPhone(phone)
    }
}