package com.example.naedafront.ui.screen.home

import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.AssetRepository
import com.example.naedafront.data.remote.PaymentResponse
import com.example.naedafront.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HomeViewModel : ViewModel() {

    private val reportRepository = ReportRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadHomeData(
        context: Context,
        userName: String,
        isFaceRegistered: Boolean
    ) {
        val userNo = AuthPrefs.getUserNo(context)

        Log.d("HomeViewModel", "▶ loadHomeData / userNo=$userNo / userName=$userName")

        _uiState.update {
            it.copy(
                userName = userName,
                isFaceRegistered = isFaceRegistered
            )
        }

        if (userNo == null || userNo <= 0L) {
            Log.w("HomeViewModel", "⚠ userNo가 null 또는 0 → 로그인 시 saveLoginSession() 호출 확인 필요")
            _uiState.update {
                it.copy(
                    isLoadingAccount = false,
                    account = null,
                    spendingCategories = emptyList(),
                    topSpendingCategory = null,
                    topSpendingAmount = 0L
                )
            }
            return
        }

        viewModelScope.launch { loadAccount(userNo) }
        viewModelScope.launch { loadRecentTransactions(userNo) }
        viewModelScope.launch { loadSpendingReport(userNo) }
    }

    private suspend fun loadAccount(userNo: Long) {
        Log.d("HomeViewModel", "▶ loadAccount 시작 / userNo=$userNo")
        _uiState.update { it.copy(isLoadingAccount = true) }

        runCatching {
            AssetRepository.getWalletAssets(userNo).accounts.firstOrNull()
        }.onSuccess { account ->
            Log.d("HomeViewModel", "✅ 계좌 로드 성공: bank=${account?.bankName}, balance=${account?.accountBalance}")
            _uiState.update {
                it.copy(
                    account = account,
                    isLoadingAccount = false,
                    accountError = null
                )
            }
        }.onFailure { e ->
            Log.e("HomeViewModel", "❌ 계좌 로드 실패: ${e.message}", e)
            _uiState.update {
                it.copy(
                    account = null,
                    isLoadingAccount = false,
                    accountError = e.message
                )
            }
        }
    }

    private suspend fun loadRecentTransactions(userNo: Long) {
        AssetRepository.getPayments(userNo)
            .onSuccess { payments ->
                val items = payments
                    .sortedByDescending { it.createdAt }
                    .take(3)
                    .map { it.toTransactionItem() }
                _uiState.update { it.copy(recentTransactions = items) }
            }
            .onFailure { e ->
                Log.e("HomeViewModel", "❌ 최근거래 로드 실패: ${e.message}", e)
                _uiState.update { it.copy(recentTransactions = emptyList()) }
            }
    }

    private suspend fun loadSpendingReport(userNo: Long) {
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

                _uiState.update {
                    it.copy(
                        spendingCategories = mappedCategories,
                        topSpendingCategory = sortedCategories.firstOrNull()?.key,
                        topSpendingAmount = sortedCategories.firstOrNull()?.value ?: 0L
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

private fun PaymentResponse.toTransactionItem(): TransactionItem {
    val isSuccess = status?.uppercase() in listOf("APPROVED", "SUCCESS", "COMPLETED")

    return TransactionItem(
        title = when {
            !isSuccess -> "결제 실패"
            authMethod?.uppercase() == "FACE" -> "내다페이 (얼굴인증)"
            authMethod?.uppercase() == "PIN" -> "내다페이 (PIN인증)"
            else -> "내다페이 결제"
        },
        subTitle = createdAt?.formatDateTime() ?: "",
        amount = if (isSuccess) "-₩${"%,d".format(amount ?: 0L)}" else "실패",
        isIncome = false,
        iconBg = if (isSuccess) Color(0xFFDCEBFF) else Color(0xFFFFEBEE),
        icon = Icons.Default.ShoppingBag
    )
}

private fun String.formatDateTime(): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = inputFormat.parse(this) ?: return this
        val cal = Calendar.getInstance().apply { time = date }

        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val ampm = if (hour24 < 12) "오전" else "오후"
        val hour12 = hour24 % 12
        val displayHour = if (hour12 == 0) 12 else hour12

        "${month}월 ${day}일 $ampm $displayHour:${"%02d".format(minute)}"
    } catch (e: Exception) {
        this
    }
}