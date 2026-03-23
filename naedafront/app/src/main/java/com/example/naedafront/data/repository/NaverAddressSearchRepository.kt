package com.example.naedafront.data.repository

import com.example.naedafront.data.remote.NaverMapApiConfig
import com.example.naedafront.data.remote.response.NaverGeocodeAddress

class NaverAddressSearchRepository {

    suspend fun searchAddress(query: String): Result<List<NaverGeocodeAddress>> {
        return try {
            val response = NaverMapApiConfig.geocodingApi.searchAddress(
                query = query,
                language = "kor",
                page = 1,
                count = 20
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                return Result.failure(
                    IllegalStateException(
                        if (errorBody.isNotBlank()) {
                            "주소 검색 실패: HTTP ${response.code()} / $errorBody"
                        } else {
                            "주소 검색 실패: HTTP ${response.code()}"
                        }
                    )
                )
            }

            val body = response.body()
                ?: return Result.failure(IllegalStateException("응답 바디가 비어 있습니다."))

            if (body.status != "OK") {
                return Result.failure(
                    IllegalStateException(
                        body.errorMessage.ifBlank { "주소 검색에 실패했습니다." }
                    )
                )
            }

            Result.success(body.addresses)
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "주소 검색 중 알 수 없는 오류가 발생했습니다.",
                    e
                )
            )
        }
    }
}