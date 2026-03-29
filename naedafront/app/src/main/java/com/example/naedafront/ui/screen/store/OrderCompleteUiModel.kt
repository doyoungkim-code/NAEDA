package com.example.naedafront.ui.screen.store

data class OrderCompleteUiModel(
    val productName: String,
    val imageUrl: String = "",
    val thumbnailLabel: String = "",
    val orderNumber: String,
    val recipientName: String,
    val phone: String,
    val zipCode: String,
    val address: String,
    val detailAddress: String,
    val deliveryRequest: String,
    val totalPaymentAmount: Int
)
