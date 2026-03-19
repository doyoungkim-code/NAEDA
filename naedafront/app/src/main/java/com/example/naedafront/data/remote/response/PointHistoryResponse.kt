package com.example.naedafront.data.remote.response

data class PointHistoryResponse(
    val historyId: Long,
    val type: String,
    val amount: Long,
    val balanceAfter: Long,
    val description: String,
    val created: String
)