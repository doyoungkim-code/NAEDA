package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.response.PaymentDetailResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface PaymentApi {

    @GET("/api/pay/{id}")
    suspend fun getPaymentDetail(
        @Header("X-User-No") userNo: Long,
        @Path("id") id: Long
    ): Response<PaymentDetailResponse>
}