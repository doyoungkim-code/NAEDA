package com.example.naedafront.data.repository

import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.data.remote.api.ReportApi
import com.example.naedafront.data.remote.response.ReportResponse

class ReportRepository {

    private val reportApi: ReportApi =
        ApiConfig.retrofit.create(ReportApi::class.java)

    suspend fun getLatestMonthlyReport(userNo: Long): Result<ReportResponse> {
        return try {
            val response = reportApi.getLatestReport(
                userNo = userNo,
                periodType = "MONTHLY"
            )

            if (response.isSuccessful) {
                val body: ReportResponse? = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure<ReportResponse>(
                        Exception("최신 소비 리포트 응답이 비어 있습니다.")
                    )
                }
            } else {
                Result.failure<ReportResponse>(
                    Exception("최신 소비 리포트 조회 실패: ${response.code()}")
                )
            }
        } catch (e: Exception) {
            Result.failure<ReportResponse>(e)
        }
    }
}