package com.example.naedafront.data.remote.response

data class OrderResponse(
    val orderId: Long,
    val userNo: Long,
    val productId: Long,
    val productName: String,
    val pointPrice: Long,
    val addressId: Long,
    val orderAt: String
)