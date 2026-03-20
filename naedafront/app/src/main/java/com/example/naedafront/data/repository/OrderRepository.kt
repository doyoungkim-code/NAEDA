package com.example.naedafront.data.repository

import com.example.naedafront.data.remote.RetrofitClient
import com.example.naedafront.data.remote.request.CreateOrderRequest
import com.example.naedafront.data.remote.response.OrderResponse

class OrderRepository {

    suspend fun createOrder(
        userNo: Long,
        productId: Long,
        addressId: Long
    ): Result<OrderResponse> {
        return try {
            val response = RetrofitClient.orderApi.createOrder(
                userNo = userNo,
                request = CreateOrderRequest(
                    productId = productId,
                    addressId = addressId
                )
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(
                    IllegalStateException(
                        if (errorBody.isNotBlank()) {
                            "주문 생성 실패: HTTP ${response.code()} / $errorBody"
                        } else {
                            "주문 생성 실패: HTTP ${response.code()}"
                        }
                    )
                )
            } else {
                val body = response.body()
                    ?: return Result.failure(
                        IllegalStateException("주문 응답 바디가 비어 있습니다.")
                    )

                Result.success(body)
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    e.message ?: "주문 생성 중 오류가 발생했습니다.",
                    e
                )
            )
        }
    }
}