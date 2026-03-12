package com.example.naedafront.data.remote.request

data class SignUpRequest(
    val userId: String,
    val password: String,
    val username: String,
    val residentNo: String,
    val phone: String,
    val institutionCode: String,
    val pin: String
)