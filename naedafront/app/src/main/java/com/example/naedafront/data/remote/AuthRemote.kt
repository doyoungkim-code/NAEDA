package com.example.naedafront.data.remote

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequestBody(
    val userId: String,
    val password: String
)

data class LoginResponseDto(
    val userNo: Long,
    val userId: String,
    val username: String,
    val userKey: String,
    val accessToken: String,
    val refreshToken: String,
    val faceRegistered: Boolean = false,
    val secondaryAuthEnabled: Boolean = false
)

private data class ErrorResponseDto(
    val code: String? = null,
    val message: String? = null,
    val detail: String? = null
)

private interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestBody): Response<LoginResponseDto>
}

sealed interface LoginResult {
    data class Success(val response: LoginResponseDto) : LoginResult
    data class Failure(val message: String) : LoginResult
}

object AuthRepository {
    private val service = ApiConfig.retrofit.create(AuthApiService::class.java)
    private val gson = Gson()

    suspend fun login(userId: String, password: String): LoginResult {
        return runCatching {
            service.login(
                LoginRequestBody(
                    userId = userId.trim(),
                    password = password
                )
            )
        }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    LoginResult.Success(body)
                } else {
                    LoginResult.Failure(parseErrorMessage(response))
                }
            },
            onFailure = { error ->
                LoginResult.Failure(error.message ?: "로그인 요청에 실패했습니다.")
            }
        )
    }

    private fun parseErrorMessage(response: Response<*>): String {
        val raw = response.errorBody()?.string().orEmpty()
        if (raw.isBlank()) {
            return when (response.code()) {
                401 -> "아이디 또는 비밀번호가 일치하지 않습니다."
                502 -> "금융망 사용자 검증에 실패했습니다."
                else -> "로그인에 실패했습니다. (${response.code()})"
            }
        }

        return try {
            gson.fromJson(raw, ErrorResponseDto::class.java)?.message
                ?: "로그인에 실패했습니다. (${response.code()})"
        } catch (_: JsonSyntaxException) {
            "로그인에 실패했습니다. (${response.code()})"
        }
    }
}

