package com.example.naedafront.ui.screen.mypage

data class MyPageUiState(
    val isLoading: Boolean = false,
    val isLogoutLoading: Boolean = false,
    val isUpdatingSecondaryAuth: Boolean = false,
    val userNo: Long? = null,
    val userName: String = "",
    val userEmail: String = "",
    val phone: String = "",
    val faceRegistered: Boolean = false,
    val secondaryAuthEnabled: Boolean = false,
    val errorMessage: String? = null
)
