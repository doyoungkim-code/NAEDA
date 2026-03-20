package com.example.naedafront.ui.screen.store

//C:\Users\SSAFY\Desktop\kimjongwoo\xmrghk\S14P21D103\naedafront\app\src\main\java\com\example\naedafront\ui\screen\store\AddressSearchViewModel.kt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.naedafront.data.remote.response.NaverGeocodeAddress
import com.example.naedafront.data.repository.NaverAddressSearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddressSearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<NaverGeocodeAddress> = emptyList(),
    val errorMessage: String? = null
)

class AddressSearchViewModel(
    private val repository: NaverAddressSearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddressSearchUiState())
    val uiState: StateFlow<AddressSearchUiState> = _uiState.asStateFlow()

    fun updateQuery(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                errorMessage = null
            )
        }
    }

    fun search() {
        val keyword = _uiState.value.query.trim()

        if (keyword.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "검색어를 입력해주세요.",
                    results = emptyList()
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    results = emptyList()
                )
            }

            repository.searchAddress(keyword)
                .onSuccess { addresses ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            results = addresses,
                            errorMessage = if (addresses.isEmpty()) "검색 결과가 없습니다." else null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            results = emptyList(),
                            errorMessage = throwable.message ?: "주소 검색에 실패했습니다."
                        )
                    }
                }
        }
    }

    fun clear() {
        _uiState.value = AddressSearchUiState()
    }

    companion object {
        fun factory(): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AddressSearchViewModel(
                        repository = NaverAddressSearchRepository()
                    ) as T
                }
            }
        }
    }
}