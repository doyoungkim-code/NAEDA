package com.example.naedafront.data.remote.response

data class CardTransactionResponse(
    val logId: Long,
    val transactionUniqueNo: String,
    val categoryName: String?,
    val aiCategory: String?,
    val merchantName: String?,
    val transactionDate: String?,
    val transactionTime: String?,
    val amount: Long,
    val cardStatus: String?
)