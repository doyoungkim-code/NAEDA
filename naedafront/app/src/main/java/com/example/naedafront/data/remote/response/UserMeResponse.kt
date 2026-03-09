package com.example.naedafront.data.remote.response

data class UserMeResponse(
    val userNo: Long,
    val userId: String,
    val username: String,
    val phone: String,
    val faceRegistered: Boolean,
    val secondaryAuthEnabled: Boolean
)