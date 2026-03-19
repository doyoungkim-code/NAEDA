package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.response.ProductResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ProductApi {
    @GET("/api/products")
    suspend fun getProducts(
        @Query("size") size: Int = 100
    ): Response<List<ProductResponse>>
}