package com.example.naedafront.ui.screen.store

import com.example.naedafront.data.remote.response.OrderResponse

data class OrderHistoryUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderResponse> = emptyList(),
    val errorMessage: String? = null
)