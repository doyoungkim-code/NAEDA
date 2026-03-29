package com.example.naedafront.ui.screen.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.naedafront.data.remote.response.AddressResponse
import com.example.naedafront.data.remote.response.ProductResponse
import com.example.naedafront.data.remote.response.UserMeResponse
import com.example.naedafront.data.repository.AddressRepository
import com.example.naedafront.data.repository.OrderRepository
import com.example.naedafront.data.repository.ProductRepository
import com.example.naedafront.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderDetailViewModel(
    private val orderId: Long,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val addressRepository: AddressRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        loadOrderDetail()
    }

    fun loadOrderDetail() {
        viewModelScope.launch {
            _uiState.value = OrderDetailUiState(
                isLoading = true,
                order = null,
                product = null,
                address = null,
                user = null,
                errorMessage = null
            )

            orderRepository.getOrderDetail(orderId)
                .onSuccess { order ->

                    var productResult: ProductResponse? = null
                    var addressResult: AddressResponse? = null
                    var userResult: UserMeResponse? = null
                    val errors = mutableListOf<String>()

                    productRepository.getProductDetail(order.productId)
                        .onSuccess { productResult = it }
                        .onFailure { errors += (it.message ?: "상품 정보 조회 실패") }

                    addressRepository.getAddressDetail(
                        userNo = order.userNo,
                        addressId = order.addressId
                    )
                        .onSuccess { addressResult = it }
                        .onFailure { errors += (it.message ?: "배송지 정보 조회 실패") }

                    userRepository.getMyInfo(order.userNo)
                        .onSuccess { userResult = it }
                        .onFailure { errors += (it.message ?: "회원 정보 조회 실패") }

                    _uiState.value = OrderDetailUiState(
                        isLoading = false,
                        order = order,
                        product = productResult,
                        address = addressResult,
                        user = userResult,
                        errorMessage = errors.takeIf { it.isNotEmpty() }?.joinToString("\n")
                    )
                }
                .onFailure { throwable ->
                    _uiState.value = OrderDetailUiState(
                        isLoading = false,
                        order = null,
                        product = null,
                        address = null,
                        user = null,
                        errorMessage = throwable.message ?: "주문 상세 조회 중 오류가 발생했습니다."
                    )
                }
        }
    }

    companion object {
        fun factory(
            orderId: Long,
            orderRepository: OrderRepository,
            productRepository: ProductRepository,
            addressRepository: AddressRepository,
            userRepository: UserRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OrderDetailViewModel(
                        orderId = orderId,
                        orderRepository = orderRepository,
                        productRepository = productRepository,
                        addressRepository = addressRepository,
                        userRepository = userRepository
                    ) as T
                }
            }
        }
    }
}