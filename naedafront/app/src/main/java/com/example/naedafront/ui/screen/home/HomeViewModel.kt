package com.example.naedafront.ui.screen.home

import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.AssetPayMethodResponse
import com.example.naedafront.data.remote.AssetRepository
import com.example.naedafront.data.remote.NotificationRepository
import com.example.naedafront.data.remote.PaymentResponse
import com.example.naedafront.data.repository.NoticeRepository
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
                    topSpendingAmount = 0L,
                    spendingInsight = null,
                    unreadNotificationCount = 0L,
                    facePayEnabled = false,
                    facePayMethodId = null,
                    defaultPaymentMethodId = null,
                    isUpdatingFacePay = false
                )
            }
            return
        }

        viewModelScope.launch { loadWalletSummary(userNo) }
        viewModelScope.launch { loadRecentTransactions(userNo) }
        viewModelScope.launch { loadCurrentMonthSpendingAnalysis(userNo) }
        viewModelScope.launch { loadNotices() }
        viewModelScope.launch { loadUnreadNotificationCount(userNo) }
    }

    fun refreshUnreadNotificationCount(context: Context) {
        val userNo = AuthPrefs.getUserNo(context) ?: return
        viewModelScope.launch { loadUnreadNotificationCount(userNo) }
    }

    fun toggleFacePay(context: Context) {
        val userNo = AuthPrefs.getUserNo(context) ?: return
        val currentState = _uiState.value
        val targetPaymentMethodId = if (currentState.facePayEnabled) {
            currentState.facePayMethodId
        } else {
            currentState.defaultPaymentMethodId
        }

        if (targetPaymentMethodId == null) {
            Log.w("HomeViewModel", "⚠ 토글할 결제수단이 없습니다.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingFacePay = true) }
            runCatching {
                AssetRepository.setFacePayPaymentMethod(
                    userNo = userNo,
                    paymentMethodId = targetPaymentMethodId,
                    enabled = !currentState.facePayEnabled
                )
            }.onSuccess {
                loadWalletSummary(userNo)
            }.onFailure { e ->
                Log.e("HomeViewModel", "❌ 페이스페이 사용 토글 실패: ${e.message}", e)
                _uiState.update { it.copy(isUpdatingFacePay = false) }
            }
        }
    }

    private suspend fun loadWalletSummary(userNo: Long) {
        Log.d("HomeViewModel", "▶ loadWalletSummary 시작 / userNo=$userNo")
        _uiState.update { it.copy(isLoadingAccount = true) }

        runCatching {
            AssetRepository.getWalletAssets(userNo)
        }.onSuccess { wallet ->
            val activeMethods = wallet.payMethods.filter { it.isActive != false }
            val facePayMethod = activeMethods.firstOrNull { it.isFacePay == true }
            val defaultMethod = activeMethods.firstOrNull { it.isDefault == true }

            Log.d("HomeViewModel", "✅ 지갑 로드 성공: account=${wallet.accounts.firstOrNull()?.bankName}, facePay=${facePayMethod?.paymentMethodId}, default=${defaultMethod?.paymentMethodId}")
            _uiState.update {
                it.copy(
                    account = wallet.accounts.firstOrNull(),
                    cards = wallet.cards.filter { card -> card.isActive != false },
                    isLoadingAccount = false,
                    accountError = null,
                    facePayEnabled = facePayMethod != null,
                    facePayMethodId = facePayMethod?.paymentMethodId,
                    defaultPaymentMethodId = defaultMethod?.paymentMethodId,
                    isUpdatingFacePay = false
                )
            }
        }.onFailure { e ->
            Log.e("HomeViewModel", "❌ 지갑 로드 실패: ${e.message}", e)
            _uiState.update {
                it.copy(
                    account = null,
                    cards = emptyList(),
                    isLoadingAccount = false,
                    accountError = e.message,
                    facePayEnabled = false,
                    facePayMethodId = null,
                    defaultPaymentMethodId = null,
                    isUpdatingFacePay = false
                )
            }
        }
    }

    private suspend fun loadRecentTransactions(userNo: Long) {
        AssetRepository.getPayments(userNo)
            .onSuccess { payments ->
                val items = payments
                    .sortedByDescending { it.createdAt.toEpochMillis() }
                    .take(3)
                    .map { it.toTransactionItem() }
                _uiState.update { it.copy(recentTransactions = items) }
            }
            .onFailure { e ->
                Log.e("HomeViewModel", "❌ 최근거래 로드 실패: ${e.message}", e)
                _uiState.update { it.copy(recentTransactions = emptyList()) }
            }
    }

    private suspend fun loadNotices() {
        val noticeItems = mutableListOf<NoticeItem>()

        NoticeRepository.getAllFestivals()
            .onSuccess { festivals ->
                festivals.forEach { f ->
                    val startDate = f.startDate?.substring(5)?.replace("-", ".") ?: ""
                    val endDate = f.endDate?.substring(5)?.replace("-", ".") ?: ""
                    noticeItems.add(
                        NoticeItem(
                            id = f.festivalId ?: 0L,
                            type = "festival",
                            tag = "축제",
                            tagColor = Color(0xFFE91E63),
                            title = f.title ?: "",
                            content = f.description ?: "",
                            date = "$startDate ~ $endDate",
                            createdRaw = f.created ?: "",
                            scheduleStartRaw = f.startDate ?: "",
                            imageUrl = f.imageUrl
                        )
                    )
                }
            }
            .onFailure { e ->
                Log.e("HomeViewModel", "축제 로드 실패: ${e.message}", e)
            }

        NoticeRepository.getAllNotices()
            .onSuccess { notices ->
                notices.forEach { n ->
                    val created = n.created?.substring(5, 10)?.replace("-", ".") ?: ""
                    noticeItems.add(
                        NoticeItem(
                            id = n.noticeId ?: 0L,
                            type = "notice",
                            tag = "공지",
                            tagColor = Color(0xFF1976D2),
                            title = n.title ?: "",
                            content = n.content ?: "",
                            date = created,
                            createdRaw = n.modified ?: n.created ?: "",
                            scheduleStartRaw = "",
                            imageUrl = null
                        )
                    )
                }
            }
            .onFailure { e ->
                Log.e("HomeViewModel", "공지사항 로드 실패: ${e.message}", e)
            }

        val sorted = noticeItems.sortedByDescending { it.createdRaw }
        _uiState.update { it.copy(notices = sorted.take(3)) }
    }

    private suspend fun loadCurrentMonthSpendingAnalysis(userNo: Long) {
        AssetRepository.getCurrentMonthSpendingAnalysis(userNo)
            .onSuccess { analysis ->
                val totalSpending = analysis.totalSpending ?: 0L
                val sortedCategories = analysis.categoryBreakdown.entries
                    .filter { it.value > 0L }
                    .sortedByDescending { it.value }

                if (sortedCategories.isEmpty() || totalSpending <= 0L) {
                    _uiState.update {
                        it.copy(
                            spendingCategories = emptyList(),
                            topSpendingCategory = null,
                            topSpendingAmount = 0L,
                            spendingInsight = analysis.insights.firstOrNull()
                        )
                    }
                    return@onSuccess
                }

                val total = totalSpending.toFloat()
                val categoryColors = listOf(
                    Color(0xFFFF6B35),
                    Color(0xFF4A90D9),
                    Color(0xFF44E3D3),
                    Color(0xFF9C27B0),
                    Color(0xFFFFB300),
                    Color(0xFFBDBDBD)
                )

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
                        topSpendingCategory = analysis.topCategory ?: sortedCategories.firstOrNull()?.key,
                        topSpendingAmount = analysis.topAmount ?: sortedCategories.firstOrNull()?.value ?: 0L,
                        spendingInsight = analysis.insights.firstOrNull()
                    )
                }
            }
            .onFailure { e ->
                Log.e("HomeViewModel", "❌ 이번 달 소비 분석 로드 실패: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        spendingCategories = emptyList(),
                        topSpendingCategory = null,
                        topSpendingAmount = 0L,
                        spendingInsight = null
                    )
                }
            }
    }

    private suspend fun loadUnreadNotificationCount(userNo: Long) {
        NotificationRepository.getUnreadCount(userNo)
            .onSuccess { count ->
                _uiState.update { it.copy(unreadNotificationCount = count) }
            }
            .onFailure { e ->
                Log.e("HomeViewModel", "❌ 안 읽은 알림 수 로드 실패: ${e.message}", e)
                _uiState.update { it.copy(unreadNotificationCount = 0L) }
            }
    }
}

private fun PaymentResponse.toTransactionItem(): TransactionItem {
    val isSuccess = status?.uppercase() in listOf("APPROVED", "SUCCESS", "COMPLETED")
    val storeLabel = storeName?.takeIf { it.isNotBlank() }
        ?: if (isSuccess) "매장 정보 없음" else "결제 실패"
    val subtitle = listOfNotNull(
        categoryName?.takeIf { it.isNotBlank() },
        createdAt?.formatDateTime()?.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
    val isFacePayTransaction = facePay == true || authLevel?.equals("FACE_PAY", ignoreCase = true) == true

    return TransactionItem(
        title = storeLabel,
        subTitle = subtitle.ifBlank { createdAt?.formatDateTime().orEmpty() },
        amount = if (isSuccess) "-₩${"%,d".format(amount ?: 0L)}" else "실패",
        isIncome = false,
        iconBg = if (isSuccess) Color(0xFFDCEBFF) else Color(0xFFFFEBEE),
        icon = Icons.Default.ShoppingBag,
        badgeText = if (isSuccess && isFacePayTransaction) "FACE PAY" else null
    )
}

private fun String.formatDateTime(): String {
    return try {
        // 마이크로초 제거: "2026-03-20T17:17:41.841531" → "2026-03-20T17:17:41"
        val trimmed = this.substringBefore(".").let {
            if (it.length >= 19) it.substring(0, 19) else it
        }
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(trimmed) ?: return this
        val cal = Calendar.getInstance().apply { time = date }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        "${year}.${"%02d".format(month)}.${"%02d".format(day)} ${"%02d".format(hour24)}:${"%02d".format(minute)}"
    } catch (e: Exception) {
        this
    }
}

private fun String?.toEpochMillis(): Long {
    if (this.isNullOrBlank()) return Long.MIN_VALUE

    return try {
        val trimmed = this
            .substringBefore(".")
            .substringBefore("Z")
            .let {
                val plusIndex = it.indexOf('+')
                if (plusIndex >= 0) it.substring(0, plusIndex) else it
            }
            .let {
                if (it.length >= 19) it.substring(0, 19) else it
            }
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        inputFormat.parse(trimmed)?.time ?: Long.MIN_VALUE
    } catch (e: Exception) {
        Long.MIN_VALUE
    }
}
