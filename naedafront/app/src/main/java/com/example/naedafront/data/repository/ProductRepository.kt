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
                Result.failure(Exception("상품 목록 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}