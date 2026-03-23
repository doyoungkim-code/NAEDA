package com.example.naedafront.ui.screen.store

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderHistoryViewModel(
    private val authPrefs: AuthPrefs,
    private val orderRepository: OrderRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryUiState(isLoading = true))
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            val userNo = authPrefs.getUserNo(context)

            if (userNo == null || userNo <= 0L) {
                _uiState.value = OrderHistoryUiState(
                    isLoading = false,
                    errorMessage = "사용자 정보를 찾을 수 없습니다."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            orderRepository.getOrders(userNo = userNo)
                .onSuccess { orders ->
                    _uiState.value = OrderHistoryUiState(
                        isLoading = false,
                        orders = orders.sortedByDescending { it.orderAt }
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = OrderHistoryUiState(
                        isLoading = false,
                        errorMessage = throwable.message ?: "주문 내역을 불러오지 못했습니다."
                    )
                }
        }
    }

    companion object {
        fun factory(
            authPrefs: AuthPrefs,
            orderRepository: OrderRepository,
            context: Context
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OrderHistoryViewModel(
                        authPrefs = authPrefs,
                        orderRepository = orderRepository,
                        context = context.applicationContext
                    ) as T
                }
            }
        }
    }
}