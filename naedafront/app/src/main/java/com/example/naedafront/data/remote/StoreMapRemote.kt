package com.example.naedafront.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

data class MapStoreResponseDto(
    val storeId: Long,
    val ssafyMerchantId: Long? = null,
    val userNo: Long? = null,
    val storeName: String,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val roadAddress: String? = null,
    val numberAddress: String? = null,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val isLocalBusiness: Boolean = false,
    val facePayEnabled: Boolean = false,
    val rating: Double = 0.0,
    val imageUrl: String? = null,
    val description: String? = null,
    val sourceType: String? = null,
    val isActive: Boolean = true
)

private interface StoreMapApiService {
    @GET("api/stores/map")
    suspend fun getMapStores(): List<MapStoreResponseDto>

    @GET("api/stores/{storeId}")
    suspend fun getStoreDetail(@Path("storeId") storeId: Long): MapStoreResponseDto
}

object StoreMapRepository {
    private val service = ApiConfig.retrofit.create(StoreMapApiService::class.java)

    suspend fun getMapStores(): List<MapStoreResponseDto> {
        return runCatching {
            service.getMapStores()
                .filter { it.isActive }
        }.getOrElse { throwable ->
            throw IllegalStateException(
                throwable.message ?: "식당 지도 데이터를 불러오지 못했습니다.",
                throwable
            )
        }
    }

    suspend fun getStoreDetail(storeId: Long): MapStoreResponseDto {
        return runCatching {
            service.getStoreDetail(storeId)
        }.getOrElse { throwable ->
            throw IllegalStateException(
                throwable.message ?: "가게 상세 정보를 불러오지 못했습니다.",
                throwable
            )
        }
    }
}
