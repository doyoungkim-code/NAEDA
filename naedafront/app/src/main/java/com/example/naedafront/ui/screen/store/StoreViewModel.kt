package com.example.naedafront.ui.screen.store

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.repository.PointRepository
import com.example.naedafront.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PointWalletStatus {
    LOADING,
    EXISTS,
    NOT_CREATED,
    CREATING,
    ERROR
}

data class StoreUiState(
    val isLoading: Boolean = false,
    val pointBalance: Long = 0L,
    val items: List<StoreItem> = emptyList(),
    val walletStatus: PointWalletStatus = PointWalletStatus.LOADING,
    val errorMessage: String? = null
)

class StoreViewModel : ViewModel() {

    private val productRepository = ProductRepository()
    private val pointRepository = PointRepository()

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    fun loadStoreData(context: Context) {
        val userNo = AuthPrefs.getUserNo(context) ?: 0L

        if (userNo <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    walletStatus = PointWalletStatus.ERROR,
                    errorMessage = "사용자 정보가 없습니다."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    walletStatus = PointWalletStatus.LOADING
                )
            }

            loadPointBalance(userNo)
            loadProducts()
        }
    }

    fun createWallet(context: Context) {
        val userNo = AuthPrefs.getUserNo(context) ?: 0L

        if (userNo <= 0L) {
            _uiState.update {
                it.copy(
                    walletStatus = PointWalletStatus.ERROR,
                    errorMessage = "사용자 정보가 없습니다."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    walletStatus = PointWalletStatus.CREATING,
                    errorMessage = null
                )
            }

            pointRepository.createPointWallet(userNo)
                .onSuccess { wallet ->
                    _uiState.update {
                        it.copy(
                            pointBalance = wallet.balance,
                            walletStatus = PointWalletStatus.EXISTS
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            walletStatus = PointWalletStatus.ERROR,
                            errorMessage = throwable.message ?: "포인트 지갑 생성에 실패했습니다."
                        )
                    }
                }
        }
    }

    private suspend fun loadPointBalance(userNo: Long) {
        pointRepository.getPointWallet(userNo)
            .onSuccess { wallet ->
                _uiState.update {
                    it.copy(
                        pointBalance = wallet.balance,
                        walletStatus = PointWalletStatus.EXISTS
                    )
                }
            }
            .onFailure { throwable ->
                val message = throwable.message.orEmpty()

                if (message.contains("404")) {
                    _uiState.update {
                        it.copy(
                            pointBalance = 0L,
                            walletStatus = PointWalletStatus.NOT_CREATED
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            pointBalance = 0L,
                            walletStatus = PointWalletStatus.ERROR,
                            errorMessage = if (message.isNotBlank()) {
                                message
                            } else {
                                "포인트 지갑을 불러오지 못했습니다."
                            }
                        )
                    }
                }
            }
    }

    private suspend fun loadProducts() {
        productRepository.getProducts(size = 100)
            .onSuccess { products ->
                val mappedItems = products.map { product ->
                    StoreItem(
                        id = product.productId,
                        brand = product.category,
                        title = product.productName,
                        description = product.description,
                        category = product.category,
                        pricePoint = product.pointPrice,
                        stockQuantity = product.stockQuantity,
                        status = product.status,
                        imageUrl = product.imageUrl,
                        badge = when {
                            product.stockQuantity <= 0 -> "품절"
                            product.status == "ON_SALE" -> null
                            else -> product.status
                        },
                        thumbnailLabel = product.category
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = mappedItems
                    )
                }
            }
            .onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "상품을 불러오지 못했습니다."
                    )
                }
            }
    }
}