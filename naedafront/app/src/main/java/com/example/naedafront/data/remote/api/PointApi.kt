package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.response.PointHistoryResponse
import com.example.naedafront.data.remote.response.PointWalletResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PointApi {
    @GET("/api/points/wallet/{userNo}")
    suspend fun getPointWallet(
        @Path("userNo") userNo: Long
    ): Response<PointWalletResponse>

    @POST("/api/points/wallet/{userNo}")
    suspend fun createPointWallet(
        @Path("userNo") userNo: Long
    ): Response<PointWalletResponse>

    @GET("/api/points/wallet/{userNo}/histories")
    suspend fun getPointHistories(
        @Path("userNo") userNo: Long,
        @Query("size") size: Int = 100
    ): Response<List<PointHistoryResponse>>
}