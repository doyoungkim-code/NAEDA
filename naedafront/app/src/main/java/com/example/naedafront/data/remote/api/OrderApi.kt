package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.request.CreateOrderRequest
import com.example.naedafront.data.remote.response.OrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface OrderApi {

    @POST("/api/orders")
    suspend fun createOrder(
        @Query("userNo") userNo: Long,
        @Body request: CreateOrderRequest
    ): Response<OrderResponse>
}