package com.example.naedafront.data.remote.request

data class CreateAddressRequest(
    val addressName: String,
    val recipient: String,
    val phone: String,
    val roadAddress: String,
    val numberAddress: String,
    val detailAddress: String,
    val zipCode: String,
    val isDefault: Boolean
)