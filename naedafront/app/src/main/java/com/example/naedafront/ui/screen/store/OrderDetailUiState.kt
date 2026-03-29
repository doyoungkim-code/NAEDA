package com.example.naedafront.ui.screen.store

import com.example.naedafront.data.remote.response.AddressResponse
import com.example.naedafront.data.remote.response.OrderResponse
import com.example.naedafront.data.remote.response.ProductResponse
import com.example.naedafront.data.remote.response.UserMeResponse

data class OrderDetailUiState(
    val isLoading: Boolean = true,
    val order: OrderResponse? = null,
    val product: ProductResponse? = null,
    val address: AddressResponse? = null,
    val user: UserMeResponse? = null,
    val errorMessage: String? = null
)