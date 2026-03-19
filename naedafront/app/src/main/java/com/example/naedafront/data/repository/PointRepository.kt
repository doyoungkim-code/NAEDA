package com.example.naedafront.data.repository

import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.data.remote.api.PointApi
import com.example.naedafront.data.remote.response.PointHistoryResponse
import com.example.naedafront.data.remote.response.PointWalletResponse

class PointRepository {

    private val pointApi: PointApi =
        ApiConfig.retrofit.create(PointApi::class.java)

    suspend fun getPointWallet(userNo: Long): Result<PointWalletResponse> {
        return try {
            val response = pointApi.getPointWallet(userNo)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("포인트 지갑 응답이 비어 있습니다."))
                }
            } else {
                Result.failure(Exception("포인트 지갑 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPointWallet(userNo: Long): Result<PointWalletResponse> {
        return try {
            val response = pointApi.createPointWallet(userNo)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("포인트 지갑 생성 응답이 비어 있습니다."))
                }
            } else {
                Result.failure(Exception("포인트 지갑 생성 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPointHistories(userNo: Long, size: Int = 100): Result<List<PointHistoryResponse>> {
        return try {
            val response = pointApi.getPointHistories(userNo, size)
            if (response.isSuccessful) {
                Result.success(response.body().orEmpty())
            } else {
                Result.failure(Exception("포인트 이력 조회 실패: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}