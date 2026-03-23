package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.request.CreateOrderRequest
import com.example.naedafront.data.remote.response.OrderResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OrderApi {

    @POST("/api/orders")
    suspend fun createOrder(
        @Query("userNo") userNo: Long,
        @Body request: CreateOrderRequest
    ): Response<OrderResponse>

    @GET("/api/orders")
    suspend fun getOrders(
        @Query("userNo") userNo: Long,
        @Query("size") size: Int = 100
    ): Response<List<OrderResponse>>
}