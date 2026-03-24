package com.example.naedafront.data.remote.response

data class PaymentDetailResponse(
    val paymentId: Long,
    val userNo: Long,
    val storeId: Long,
    val paymentMethodId: Long,
    val amount: Long,
    val status: String?,
    val authMethod: String?,
    val authLevel: String?,
    val faceDistance: Double?,
    val livenessPass: Boolean?,
    val pinVerified: Boolean?,
    val fdsScore: Int?,
    val fdsAction: String?,
    val earnedPoints: Long?,
    val ssafyTransactionId: String?,
    val failureReason: String?,
    val createdAt: String?
)