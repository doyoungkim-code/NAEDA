package com.example.naedafront.data.remote.response

data class ProductResponse(
    val productId: Long,
    val productName: String,
    val description: String,
    val category: String,
    val imageUrl: String,
    val pointPrice: Long,
    val stockQuantity: Int,
    val status: String,
    val startsAt: String,
    val endsAt: String
)