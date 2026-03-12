package com.example.naedafront.ui.screen.signup

data class SignUpUiState(
    val userId: String = "",
    val password: String = "",
    val username: String = "",
    val residentNo: String = "",
    val phone: String = "",
    val institutionCode: String = "001",
    val pin: String = "",
    val isLoading: Boolean = false,
    val isSignUpSuccess: Boolean = false,
    val errorMessage: String? = null
)