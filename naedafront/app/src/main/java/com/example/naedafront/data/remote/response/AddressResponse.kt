package com.example.naedafront.data.remote.response

data class AddressResponse(
    val addressId: Long,
    val addressName: String,
    val recipient: String,
    val phone: String,
    val roadAddress: String,
    val numberAddress: String,
    val detailAddress: String,
    val zipCode: String,
    val isDefault: Boolean
)