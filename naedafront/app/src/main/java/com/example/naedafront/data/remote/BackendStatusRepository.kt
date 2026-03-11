package com.example.naedafront.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

data class BackendConnectionStatus(
    val isConnected: Boolean,
    val message: String
)

private interface BackendStatusService {
    @GET("api-docs")
    suspend fun getBackendStatus(): Response<ResponseBody>
}

object BackendStatusRepository {
    private val service = ApiConfig.retrofit.create(BackendStatusService::class.java)

    suspend fun fetchStatus(): BackendConnectionStatus {
        return runCatching {
            val response = service.getBackendStatus()
            if (response.isSuccessful) {
                BackendConnectionStatus(
                    isConnected = true,
                    message = "배포 서버와 정상 연결되었습니다."
                )
            } else {
                BackendConnectionStatus(
                    isConnected = false,
                    message = "서버 응답을 확인하지 못했습니다. (${response.code()})"
                )
            }
        }.getOrElse { error ->
            BackendConnectionStatus(
                isConnected = false,
                message = error.message ?: "서버 연결에 실패했습니다."
            )
        }
    }
}
