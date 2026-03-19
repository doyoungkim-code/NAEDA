package com.example.naedafront.ui.screen.home

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val reportRepository = ReportRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHomeData(
        context: Context,
        userName: String,
        isFaceRegistered: Boolean
    ) {
        val userNo = AuthPrefs.getUserNo(context) ?: 0L

        _uiState.update {
            it.copy(
                userName = userName,
                isFaceRegistered = isFaceRegistered
            )
        }

        if (userNo <= 0L) {
            _uiState.update {
                it.copy(
                    spendingCategories = emptyList(),
                    topSpendingCategory = null,
                    topSpendingAmount = 0L
                )
            }
            return
        }

        viewModelScope.launch {
            reportRepository.getLatestMonthlyReport(userNo)
                .onSuccess { latestReport ->
                    if (latestReport.categoryBreakdown.isEmpty() || latestReport.totalSpending <= 0L) {
                        _uiState.update {
                            it.copy(
                                spendingCategories = emptyList(),
                                topSpendingCategory = null,
                                topSpendingAmount = 0L
                            )
                        }
                        return@onSuccess
                    }

                    val total = latestReport.totalSpending.toFloat()

                    val categoryColors = listOf(
                        Color(0xFFFF6B35),
                        Color(0xFF4A90D9),
                        Color(0xFF44E3D3),
                        Color(0xFF9C27B0),
                        Color(0xFFFFB300),
                        Color(0xFFBDBDBD)
                    )

                    val sortedCategories = latestReport.categoryBreakdown.entries
                        .sortedByDescending { it.value }

                    val mappedCategories = sortedCategories.mapIndexed { index, entry ->
                        val ratio = (entry.value / total).coerceIn(0f, 1f)

                        SpendingCategory(
                            label = "${entry.key} ${(ratio * 100).toInt()}%",
                            ratio = ratio,
                            color = categoryColors[index % categoryColors.size]
                        )
                    }

                    val topCategory = sortedCategories.firstOrNull()?.key
                    val topAmount = sortedCategories.firstOrNull()?.value ?: 0L

                    _uiState.update {
                        it.copy(
                            spendingCategories = mappedCategories,
                            topSpendingCategory = topCategory,
                            topSpendingAmount = topAmount
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            spendingCategories = emptyList(),
                            topSpendingCategory = null,
                            topSpendingAmount = 0L
                        )
                    }
                }
        }
    }
}