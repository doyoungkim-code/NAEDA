package com.example.naedafront.ui.screen.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.naedafront.data.remote.request.CreateAddressRequest
import com.example.naedafront.data.remote.response.AddressResponse
import com.example.naedafront.data.repository.AddressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeliveryAddressUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val addresses: List<AddressResponse> = emptyList(),
    val selectedAddress: AddressResponse? = null,
    val errorMessage: String? = null
)

class DeliveryAddressViewModel(
    private val repository: AddressRepository = AddressRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryAddressUiState())
    val uiState: StateFlow<DeliveryAddressUiState> = _uiState.asStateFlow()

    fun loadAddresses(userNo: Long) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            repository.getAddresses(userNo)
                .onSuccess { addresses ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            addresses = addresses,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            addresses = emptyList(),
                            errorMessage = throwable.message ?: "배송지 목록을 불러오지 못했습니다."
                        )
                    }
                }
        }
    }

    fun getAddressDetail(
        userNo: Long,
        addressId: Long,
        onSuccess: (AddressResponse) -> Unit
    ) {
        viewModelScope.launch {
            repository.getAddressDetail(userNo, addressId)
                .onSuccess { address ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            selectedAddress = address,
                            errorMessage = null
                        )
                    }
                    onSuccess(address)
                }
                .onFailure { throwable ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            errorMessage = throwable.message ?: "배송지 정보를 불러오지 못했습니다."
                        )
                    }
                }
        }
    }

    fun deleteAddress(userNo: Long, addressId: Long) {
        viewModelScope.launch {
            repository.deleteAddress(userNo, addressId)
                .onSuccess {
                    loadAddresses(userNo)
                }
                .onFailure { throwable ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            errorMessage = throwable.message ?: "배송지를 삭제하지 못했습니다."
                        )
                    }
                }
        }
    }

    fun createAddress(
        userNo: Long,
        addressName: String,
        recipientName: String,
        phone: String,
        postCode: String,
        address: String,
        detailAddress: String,
        saveAsDefault: Boolean,
        onSuccess: (AddressResponse) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    isSubmitting = true,
                    errorMessage = null
                )
            }

            val resolvedAddressName = addressName.trim().ifBlank {
                if (saveAsDefault) "기본 배송지" else recipientName.trim().ifBlank { "배송지" }
            }

            val request = CreateAddressRequest(
                addressName = resolvedAddressName,
                recipient = recipientName,
                phone = phone,
                roadAddress = address,
                numberAddress = "",
                detailAddress = detailAddress,
                zipCode = postCode,
                isDefault = saveAsDefault
            )

            repository.createAddress(userNo, request)
                .onSuccess { created ->
                    if (saveAsDefault) {
                        repository.setDefaultAddress(userNo, created.addressId)
                            .onSuccess { defaultAddress ->
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        isSubmitting = false,
                                        selectedAddress = defaultAddress,
                                        errorMessage = null
                                    )
                                }
                                loadAddresses(userNo)
                                onSuccess(defaultAddress)
                            }
                            .onFailure {
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        isSubmitting = false,
                                        selectedAddress = created,
                                        errorMessage = null
                                    )
                                }
                                loadAddresses(userNo)
                                onSuccess(created)
                            }
                    } else {
                        _uiState.update { currentState ->
                            currentState.copy(
                                isSubmitting = false,
                                selectedAddress = created,
                                errorMessage = null
                            )
                        }
                        loadAddresses(userNo)
                        onSuccess(created)
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isSubmitting = false,
                            errorMessage = throwable.message ?: "배송지 생성에 실패했습니다."
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(
            repository: AddressRepository = AddressRepository()
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(DeliveryAddressViewModel::class.java)) {
                        return DeliveryAddressViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
