package com.example.naedafront.data.remote.api

import com.example.naedafront.data.remote.response.ReportResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ReportApi {
    @GET("/api/reports/latest")
    suspend fun getLatestReport(
        @Query("userNo") userNo: Long,
        @Query("periodType") periodType: String = "MONTHLY"
    ): Response<ReportResponse>

    @GET("/api/reports")
    suspend fun getReportHistory(
        @Query("userNo") userNo: Long,
        @Query("periodType") periodType: String
    ): Response<List<ReportResponse>>
}