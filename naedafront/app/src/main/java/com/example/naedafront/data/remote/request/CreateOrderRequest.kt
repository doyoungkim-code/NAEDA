package com.example.naedafront.data.remote.request

data class CreateOrderRequest(
    val productId: Long,
    val addressId: Long
)