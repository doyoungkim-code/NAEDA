package com.example.naedafront.data.remote.response

data class PointWalletResponse(
    val walletId: Long,
    val userNo: Long,
    val balance: Long,
    val totalEarned: Long,
    val totalUsed: Long
)