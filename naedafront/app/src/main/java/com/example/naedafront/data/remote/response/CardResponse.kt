package com.example.naedafront.data.remote.response

data class CardResponse(
    val cardId: Long,
    val cardNo: String,
    val cardUniqueNo: String,
    val cardIssuerCode: String,
    val cardIssuerName: String,
    val cardName: String,
    val cardExpiryDate: String,
    val isActive: Boolean,
    val accountId: Long,
    val cardType: String,
    val creditLimit: Long?,
    val billingDate: Int?
)