package com.example.naedafront.data.repository

import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.data.remote.api.ProductApi
import com.example.naedafront.data.remote.response.ProductResponse

class ProductRepository {

    private val productApi: ProductApi =
        ApiConfig.retrofit.create(ProductApi::class.java)

    suspend fun getProducts(size: Int = 100): Result<List<ProductResponse>> {
        return try {
            val response = productApi.getProducts(size)
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(
                    IllegalStateException(
                        if (errorBody.isNotBlank()) {
                            "상품 목록 조회 실패: HTTP ${response.code()} / $errorBody"
                        } else {
                            "상품 목록 조회 실패: HTTP ${response.code()}"
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "상품 목록 조회 중 오류가 발생했습니다.",
                    e
                )
            )
        }
    }

    suspend fun getProductDetail(productId: Long): Result<ProductResponse> {
        return try {
            val response = productApi.getProductDetail(productId)
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(
                        IllegalStateException("상품 상세 응답 바디가 비어 있습니다.")
                    )
                Result.success(body)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(
                    IllegalStateException(
                        if (errorBody.isNotBlank()) {
                            "상품 상세 조회 실패: HTTP ${response.code()} / $errorBody"
                        } else {
                            "상품 상세 조회 실패: HTTP ${response.code()}"
                        }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "상품 상세 조회 중 오류가 발생했습니다.",
                    e
                )
            )
        }
    }
}