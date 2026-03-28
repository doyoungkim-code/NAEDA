package com.example.naedafront.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class RecommendResponseDto(
    val storeId: Long,
    val storeName: String,
    val categoryName: String? = null,
    val roadAddress: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Double = 0.0,
    val imageUrl: String? = null,
    val description: String? = null,
    val visitCount: Long = 0,
    val score: Double = 0.0
)

private interface RecommendApiService {
    @GET("api/recommend/stores")
    suspend fun getRecommendStores(
        @Query("dong") dong: String? = null,
        @Query("category") category: String? = null,
        @Query("sort") sort: String? = null
    ): List<RecommendResponseDto>

    @GET("api/recommend/dongs")
    suspend fun getDongs(): List<String>
}

object RecommendRepository {
    private val service = ApiConfig.retrofit.create(RecommendApiService::class.java)

    suspend fun getRecommendStores(
        dong: String? = null,
        category: String? = null,
        sort: String? = null
    ): List<RecommendResponseDto> {
        return runCatching {
            service.getRecommendStores(dong, category, sort)
        }.getOrElse { throwable ->
            throw IllegalStateException(
                throwable.message ?: "맛집 추천 정보를 불러오지 못했습니다.",
                throwable
            )
        }
    }

    suspend fun getDongs(): List<String> {
        return runCatching {
            service.getDongs()
        }.getOrElse { throwable ->
            throw IllegalStateException(
                throwable.message ?: "동 목록을 불러오지 못했습니다.",
                throwable
            )
        }
    }
}
