package com.example.naedafront.data.repository

import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.data.remote.api.UserApi
import com.example.naedafront.data.remote.response.UserMeResponse

class UserRepository {

    private val userApi: UserApi =
        ApiConfig.retrofit.create(UserApi::class.java)

    suspend fun getMyInfo(
        userNo: Long
    ): Result<UserMeResponse> {
        return try {
            Result.success(userApi.getMyInfo(userNo))
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "회원 정보 조회 중 오류가 발생했습니다.",
                    e
                )
            )
        }
    }
}