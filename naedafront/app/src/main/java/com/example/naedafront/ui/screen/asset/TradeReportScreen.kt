// File: app/src/main/java/com/example/naedafront/ui/screen/asset/TradeReportScreen.kt
package com.example.naedafront.ui.screen.asset

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.AssetAccountResponse
import com.example.naedafront.data.remote.AssetCardResponse
import com.example.naedafront.data.remote.AssetPayMethodResponse
import com.example.naedafront.data.remote.AssetRepository
import com.example.naedafront.data.remote.AssetTransactionResponse
import com.example.naedafront.data.remote.PaymentResponse
import com.example.naedafront.data.remote.response.PaymentDetailResponse
import androidx.compose.material3.MaterialTheme
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import com.example.naedafront.ui.theme.OutlineVariant
import com.example.naedafront.ui.theme.Surface as SurfaceColor
import com.example.naedafront.ui.theme.SurfaceVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class TradeReportItem(
    val paymentId: Long,
    val title: String,
    val subTitle: String,
    val category: String = "",
    val storeName: String = "",
    val amount: String,
    val amountValue: Long,
    val isIncome: Boolean,
    val bankName: String = "",
    val accountNumber: String = "",
    val cardNumber: String = "",
    val balanceAfter: String,
    val balanceLabel: String = "적립 포인트",
    val icon: ImageVector,
    val iconBg: Color,
    val createdAtRaw: String = "",
    val transaction: TransactionItem? = null
) {
    val date: String
        get() = createdAtRaw.toDateKey()

    val time: String
        get() = createdAtRaw.toTimeOnly()
}

data class TradeReportUiState(
    val accountName: String = "",
    val accountNumber: String = "",
    val balance: Long = 0L,
    val incomeTotal: Long = 0L,
    val expenseTotal: Long = 0L,
    val transactions: List<TradeReportItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedPaymentDetail: PaymentDetailResponse? = null,
    val isDetailLoading: Boolean = false,
    val detailError: String? = null,
    val selectedPeriod: String = "전체",
    val selectedCategory: String = "전체"
) {
    val categoryList: List<String>
        get() = listOf("전체") + transactions
            .map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

    val filteredTransactions: List<TradeReportItem>
        get() = if (selectedCategory == "전체") {
            transactions
        } else {
            transactions.filter { it.category == selectedCategory }
        }
}

private data class TradeWalletData(
    val defaultAccount: AssetAccountResponse?,
    val accountById: Map<Long, AssetAccountResponse>,
    val payMethodById: Map<Long, AssetPayMethodResponse>,
    val cardById: Map<Long, AssetCardResponse>
)

private suspend fun loadTradeWalletData(userNo: Long): TradeWalletData {
    val wallet = AssetRepository.getWalletAssets(userNo)
    return TradeWalletData(
        defaultAccount = wallet.accounts.firstOrNull(),
        accountById = wallet.accounts.mapNotNull { account ->
            val accountId = account.accountId ?: return@mapNotNull null
            accountId to account
        }.toMap(),
        payMethodById = wallet.payMethods.mapNotNull { payMethod ->
            val paymentMethodId = payMethod.paymentMethodId ?: return@mapNotNull null
            paymentMethodId to payMethod
        }.toMap(),
        cardById = wallet.cards.mapNotNull { card ->
            val cardId = card.cardId ?: return@mapNotNull null
            cardId to card
        }.toMap()
    )
}

class TradeReportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TradeReportUiState())
    val uiState: StateFlow<TradeReportUiState> = _uiState.asStateFlow()
    private var detailRequestToken = 0L
    private var currentDetailPaymentId: Long? = null
    private var listRequestToken = 0L

    fun updatePeriod(period: String) {
        _uiState.update {
            it.copy(
                selectedPeriod = period,
                selectedCategory = "전체"
            )
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun loadPaymentDetail(context: Context, paymentId: Long) {
        val userNo = AuthPrefs.getUserNo(context) ?: return
        if (_uiState.value.isDetailLoading && currentDetailPaymentId == paymentId) return

        val requestToken = ++detailRequestToken
        currentDetailPaymentId = paymentId

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDetailLoading = true,
                    detailError = null,
                    selectedPaymentDetail = null
                )
            }

            val detailResult = AssetRepository.getPaymentDetail(userNo, paymentId)
            if (requestToken != detailRequestToken || currentDetailPaymentId != paymentId) return@launch

            detailResult
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            isDetailLoading = false,
                            selectedPaymentDetail = detail
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isDetailLoading = false,
                            detailError = e.message
                        )
                    }
                }
        }
    }

    fun clearPaymentDetail() {
        detailRequestToken++
        currentDetailPaymentId = null
        _uiState.update {
            it.copy(
                selectedPaymentDetail = null,
                isDetailLoading = false,
                detailError = null
            )
        }
    }

    fun loadData(context: Context, period: String) {
        val userNo = AuthPrefs.getUserNo(context) ?: return
        val requestToken = ++listRequestToken

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    transactions = emptyList(),
                    selectedCategory = "전체"
                )
            }

            val walletData = runCatching { loadTradeWalletData(userNo) }.getOrNull()
            if (requestToken != listRequestToken) return@launch
            walletData?.defaultAccount?.let { account ->
                _uiState.update {
                    it.copy(
                        accountName = account.accountName.orEmpty().ifBlank { "내 계좌" },
                        accountNumber = account.accountNo.orEmpty(),
                        balance = account.accountBalance ?: 0L
                    )
                }
            }

            val accountResult = runCatching {
                AssetRepository.getWalletAssets(userNo).accounts.firstOrNull()
            }
            if (requestToken != listRequestToken) return@launch
            accountResult.onSuccess { account ->
                _uiState.update {
                    it.copy(
                        accountName = account?.accountName ?: "대표계좌",
                        accountNumber = account?.accountNo ?: "",
                        balance = account?.accountBalance ?: 0L
                    )
                }
            }

            val range = buildPeriodRange(period)
            AssetRepository.getPayments(userNo, range.first, range.second)
                .onSuccess { payments ->
                    if (requestToken != listRequestToken) return@onSuccess

                    val transactions = payments
                        .filter { payment -> payment.status?.uppercase() in listOf("APPROVED", "SUCCESS", "COMPLETED") }
                        .map { payment ->
                            payment.toRecentTradeItem(
                                defaultAccount = walletData?.defaultAccount,
                                accountById = walletData?.accountById.orEmpty(),
                                payMethodById = walletData?.payMethodById.orEmpty(),
                                cardById = walletData?.cardById.orEmpty()
                            )
                        }
                        .sortedByDescending { item ->
                            item.createdAtRaw.toTradeReportEpochMillis() ?: Long.MIN_VALUE
                        }

                    _uiState.update {
                        it.copy(
                            transactions = transactions,
                            incomeTotal = 0L,
                            expenseTotal = transactions.sumOf { item -> item.amountValue },
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { e ->
                    if (requestToken != listRequestToken) return@onFailure

                    _uiState.update {
                        it.copy(
                            transactions = emptyList(),
                            incomeTotal = 0L,
                            expenseTotal = 0L,
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
        }
    }
}

private fun buildPeriodRange(period: String): Pair<String, String> {
    val end = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 0)
    }

    val start = Calendar.getInstance().apply {
        when (period) {
            "1주일" -> add(Calendar.DAY_OF_MONTH, -7)
            "1개월" -> add(Calendar.MONTH, -1)
            "3개월" -> add(Calendar.MONTH, -3)
            "6개월" -> add(Calendar.MONTH, -6)
            "전체" -> add(Calendar.YEAR, -10)
            else -> add(Calendar.YEAR, -10)
        }
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return apiDateTimeFormat().format(start.time) to apiDateTimeFormat().format(end.time)
}

private fun apiDateTimeFormat(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA)
}

private fun PaymentResponse.toUiItem(
    defaultAccount: AssetAccountResponse?,
    accountById: Map<Long, AssetAccountResponse>,
    payMethodById: Map<Long, AssetPayMethodResponse>,
    cardById: Map<Long, AssetCardResponse>
): TradeReportItem {
    val isSuccess = status?.uppercase() in listOf("APPROVED", "SUCCESS", "COMPLETED")
    val pointsText = earnedPoints
        ?.takeIf { it > 0 }
        ?.let { "${"%,d".format(it)}P 적립" }
        ?: "—"

    val rawAmount = amount ?: 0L
    val rawCategory = categoryName?.trim().orEmpty()
    val rawStoreName = storeName?.trim().orEmpty()
    val payMethod = paymentMethodId?.let { payMethodById[it] }
    val account = payMethod?.accountId?.let { accountById[it] } ?: defaultAccount
    val cardId = payMethod?.debitCardId ?: payMethod?.creditCardId
    val maskedCardNumber = cardId
        ?.let { cardById[it]?.cardNo }
        .orEmpty()
        .maskCardNumber()
        .takeIf { it.isNotBlank() && it != "-" }
        .orEmpty()

    return TradeReportItem(
        paymentId = paymentId ?: -1L,
        title = when {
            !isSuccess -> "결제 실패"
            rawStoreName.isNotBlank() -> rawStoreName
            else -> "내다페이 결제"
        },
        subTitle = createdAt?.formatCreatedAt() ?: "",
        category = rawCategory,
        storeName = rawStoreName,
        amount = if (isSuccess) "-${"%,d".format(rawAmount)}원" else "실패",
        amountValue = rawAmount,
        isIncome = false,
        bankName = account?.bankName.orEmpty(),
        accountNumber = account?.accountNo.orEmpty().maskAccountNumber(),
        cardNumber = maskedCardNumber,
        balanceAfter = pointsText,
        balanceLabel = "적립 포인트",
        icon = Icons.Default.ArrowUpward,
        iconBg = if (isSuccess) Color(0xFFDCEBFF) else Color(0xFFFFEBEE),
        createdAtRaw = createdAt ?: ""
    )
}

private fun PaymentResponse.toRecentTradeItem(
    defaultAccount: AssetAccountResponse?,
    accountById: Map<Long, AssetAccountResponse>,
    payMethodById: Map<Long, AssetPayMethodResponse>,
    cardById: Map<Long, AssetCardResponse>
): TradeReportItem {
    val rawAmount = amount ?: 0L
    val rawCategory = categoryName?.trim().orEmpty()
    val rawStoreName = storeName?.trim().orEmpty()
    val payMethod = paymentMethodId?.let { payMethodById[it] }
    val account = payMethod?.accountId?.let { accountById[it] } ?: defaultAccount
    val cardId = payMethod?.debitCardId ?: payMethod?.creditCardId
    val maskedCardNumber = cardId
        ?.let { cardById[it]?.cardNo }
        .orEmpty()
        .maskCardNumber()
        .takeIf { it.isNotBlank() && it != "-" }
        .orEmpty()
    val pointsText = earnedPoints
        ?.takeIf { it > 0 }
        ?.let { "${"%,d".format(it)}P 적립" }
        ?: "적립 없음"

    return TradeReportItem(
        paymentId = paymentId ?: -1L,
        title = rawStoreName.ifBlank { "내다페이 결제" },
        subTitle = createdAt?.formatCreatedAt() ?: "",
        category = rawCategory,
        storeName = rawStoreName,
        amount = "-${"%,d".format(rawAmount)}원",
        amountValue = rawAmount,
        isIncome = false,
        bankName = account?.bankName.orEmpty(),
        accountNumber = account?.accountNo.orEmpty().maskAccountNumber(),
        cardNumber = maskedCardNumber,
        balanceAfter = pointsText,
        balanceLabel = "적립 포인트",
        icon = Icons.Default.ArrowUpward,
        iconBg = Color(0xFFDCEBFF),
        createdAtRaw = createdAt ?: ""
    )
}

private fun AssetTransactionResponse.toRecentTradeItem(
    defaultAccount: AssetAccountResponse?,
    linkedPayment: PaymentResponse?
): TradeReportItem {
    val isDeposit = transactionType.equals("DEPOSIT", ignoreCase = true)
    val rawAmount = amount ?: 0L
    val rawTransacted = transacted.orEmpty()
    val rawMemo = memo.orEmpty()
    val rawCounterpart = counterpart.orEmpty()
    val rawStoreName = linkedPayment?.storeName?.trim().orEmpty()
    val rawCategory = linkedPayment?.categoryName?.trim().orEmpty().ifBlank {
        aiCategory.orEmpty().ifBlank { category.orEmpty() }
    }
    val cleanedMemo = rawMemo.replace(
        Regex("\\s*(\\uD398\\uC774\\uC2A4\\uD398\\uC774|\\uCE74\\uB4DC)\\s*\\uACB0\\uC81C$"),
        ""
    )
    val pointsText = linkedPayment?.earnedPoints
        ?.takeIf { it > 0 }
        ?.let { "${"%,d".format(it)}P" }
        ?: "0P"
    val title = when {
        rawStoreName.isNotBlank() -> rawStoreName
        cleanedMemo.isNotBlank() -> cleanedMemo
        rawCounterpart.isNotBlank() &&
            !rawCounterpart.all { it.isDigit() } &&
            !rawCounterpart.contains("@") -> rawCounterpart
        else -> if (isDeposit) "\uC785\uAE08" else "\uCD9C\uAE08"
    }
    val subtitle = rawTransacted.formatCreatedAt().ifBlank { rawTransacted }
    val syntheticIdSeed = rawTransacted.hashCode().toLong()
    val syntheticId = when {
        linkedPayment?.paymentId != null -> linkedPayment.paymentId
        logId != null && logId > 0L -> -logId
        syntheticIdSeed <= -1L -> syntheticIdSeed
        else -> -(syntheticIdSeed + 1L)
    }

    return TradeReportItem(
        paymentId = syntheticId ?: -1L,
        title = title,
        subTitle = subtitle,
        category = rawCategory,
        storeName = rawStoreName,
        amount = if (isDeposit) "+${"%,d".format(rawAmount)}\uC6D0" else "-${"%,d".format(rawAmount)}\uC6D0",
        amountValue = rawAmount,
        isIncome = isDeposit,
        bankName = defaultAccount?.bankName.orEmpty(),
        accountNumber = defaultAccount?.accountNo.orEmpty().maskAccountNumber(),
        balanceAfter = if (linkedPayment != null) {
            pointsText
        } else {
            "${"%,d".format(balanceAfter ?: 0L)}\uC6D0"
        },
        balanceLabel = if (linkedPayment != null) "\uC801\uB9BD \uD3EC\uC778\uD2B8" else "\uAC70\uB798 \uD6C4 \uC794\uC561",
        icon = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
        iconBg = if (isDeposit) Color(0xFFDFF7E8) else Color(0xFFDCEBFF),
        createdAtRaw = rawTransacted,
        transaction = TransactionItem(
            id = logId?.toString() ?: ssafyTransactionId.orEmpty().ifBlank { "trade-${title.hashCode()}-${rawTransacted.hashCode()}" },
            transactionType = transactionType.orEmpty(),
            counterpart = rawCounterpart,
            memo = rawMemo,
            category = rawCategory,
            amount = rawAmount,
            balanceAfter = balanceAfter ?: 0L,
            ssafyTransactionId = ssafyTransactionId.orEmpty(),
            transacted = rawTransacted
        )
    )
}

private fun List<AssetTransactionResponse>.filterByPeriod(period: String): List<AssetTransactionResponse> {
    val threshold = Calendar.getInstance().apply {
        when (period) {
            "1äºŒì‡±ì”ª" -> add(Calendar.DAY_OF_MONTH, -7)
            "1åª›ì’–ì¡" -> add(Calendar.MONTH, -1)
            "3åª›ì’–ì¡" -> add(Calendar.MONTH, -3)
            "6åª›ì’–ì¡" -> add(Calendar.MONTH, -6)
            else -> return this@filterByPeriod
        }
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    return filter { transaction ->
        val epochMillis = transaction.transacted.toTradeReportEpochMillis()
        epochMillis == null || epochMillis >= threshold
    }
}

private fun List<AssetTransactionResponse>.filterByThreshold(threshold: Long?): List<AssetTransactionResponse> {
    if (threshold == null) return this
    return filter { transaction ->
        val epochMillis = transaction.transacted.toTradeReportEpochMillis()
        epochMillis == null || epochMillis >= threshold
    }
}

private fun String?.toTradeReportEpochMillis(): Long? {
    if (this.isNullOrBlank()) return null

    val normalizedRaw = trim().replace(
        Regex("""\.\d{1,9}(?=Z|[+-]\d{2}:?\d{2}|$)"""),
        ""
    )
    val patterns = listOf(
        "yyyyMMdd HHmmss",
        "yyyyMMdd HH:mm:ss",
        "yyyyMMdd HH:mm",
        "yyyyMMdd",
        "yyyy-M-d HH:mm:ss",
        "yyyy-M-d HH:mm",
        "yyyy-M-d",
        "yyyy.M.d HH:mm:ss",
        "yyyy.M.d HH:mm",
        "yyyy.M.d",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy.MM.dd HH:mm",
        "yyyy-MM-dd"
    )

    for (pattern in patterns) {
        val formatter = SimpleDateFormat(pattern, Locale.KOREA).apply {
            isLenient = false
            if (pattern.contains("X")) {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        val parsed = runCatching { formatter.parse(normalizedRaw) }.getOrNull() ?: continue
        return parsed.time
    }

    return null
}

private fun String.formatCreatedAt(): String {
    val parsePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss"
    )

    for (pattern in parsePatterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.KOREA)
            if (pattern.contains("X") || pattern.contains("'Z'")) {
                parser.timeZone = TimeZone.getTimeZone("UTC")
            }

            val date = parser.parse(this) ?: continue
            val calendar = Calendar.getInstance().apply { time = date }

            val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val ampm = if (hour24 < 12) "오전" else "오후"
            val hour12 = when (hour24 % 12) {
                0 -> 12
                else -> hour24 % 12
            }

            return "${month}월 ${day}일 $ampm $hour12:${"%02d".format(minute)}"
        } catch (_: Exception) {
        }
    }

    return this
}

private fun String.toDateKey(): String {
    val parsePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss"
    )

    for (pattern in parsePatterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.KOREA)
            if (pattern.contains("X") || pattern.contains("'Z'")) {
                parser.timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(this) ?: continue
            return SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(date)
        } catch (_: Exception) {
        }
    }

    return take(10)
}

private fun String.toTimeOnly(): String {
    val parsePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss"
    )

    for (pattern in parsePatterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.KOREA)
            if (pattern.contains("X") || pattern.contains("'Z'")) {
                parser.timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = parser.parse(this) ?: continue
            return SimpleDateFormat("HH:mm", Locale.KOREA).format(date)
        } catch (_: Exception) {
        }
    }

    return ""
}

private fun String?.toPaymentMethodLabel(): String {
    return when (this?.uppercase()) {
        "FACE" -> "내다페이(페이스페이)"
        "PIN" -> "내다페이(PIN인증)"
        else -> "내다페이"
    }
}

private fun TradeReportItem.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val keyword = query.trim().lowercase()

    return listOf(
        title,
        subTitle,
        storeName,
        category,
        amount,
        balanceAfter,
        balanceLabel,
        bankName,
        accountNumber,
        cardNumber,
        date,
        time
    ).any { it.lowercase().contains(keyword) }
}

private fun String.maskAccountNumber(): String {
    val digits = replace("-", "").replace(" ", "")
    return when {
        digits.isBlank() -> "-"
        digits.length <= 7 -> this
        else -> "${digits.take(3)}${"*".repeat(digits.length - 7)}${digits.takeLast(4)}"
    }
}

private fun String.maskCardNumber(): String {
    val digits = replace("-", "").replace(" ", "")
    return when {
        digits.isBlank() -> "-"
        digits.length < 8 -> this
        else -> "${digits.take(4)} •••• •••• ${digits.takeLast(4)}"
    }
}

@Composable
fun TradeReportScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: TradeReportViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showPeriodDialog by rememberSaveable { mutableStateOf(false) }
    var selectedFlow by rememberSaveable { mutableStateOf("전체") }
    var selectedPaymentItem by remember { mutableStateOf<TradeReportItem?>(null) }
    val normalizedSelectedFlow = when (selectedFlow) {
        "입금" -> "입금"
        "출금" -> "출금"
        else -> "전체"
    }

    LaunchedEffect(uiState.selectedPeriod) {
        viewModel.loadData(context, uiState.selectedPeriod)
    }

    val filteredTransactions = remember(uiState.filteredTransactions, searchQuery, selectedFlow) {
        uiState.filteredTransactions
            .filter { item ->
                when (normalizedSelectedFlow) {
                    "입금" -> item.isIncome
                    "출금" -> !item.isIncome
                    else -> true
                }
            }
            .filter { it.matches(searchQuery) }
            .filter { item ->
                when (normalizedSelectedFlow) {
                    "입금" -> item.isIncome
                    "출금" -> !item.isIncome
                    else -> true
                }
            }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.date }.toSortedMap(reverseOrder())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                RecentTradeReportHeaderV2(
                    accountName = uiState.accountName.ifBlank { "대표계좌" },
                    accountNumber = uiState.accountNumber,
                    incomeTotal = uiState.incomeTotal,
                    expenseTotal = uiState.expenseTotal,
                    onBack = onBackClick,
                    isSearchMode = isSearchMode,
                    onSearchToggle = {
                        if (isSearchMode) {
                            searchQuery = ""
                            isSearchMode = false
                        } else {
                            isSearchMode = true
                        }
                    }
                )
            }

            if (isSearchMode) {
                item {
                    TradeSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )
                }
            }

            item {
                TradeFilterRowDropdown(
                    selectedPeriod = uiState.selectedPeriod,
                    selected = normalizedSelectedFlow,
                    onSelect = { selectedFlow = it },
                    onPeriodClick = { showPeriodDialog = true }
                )
            }

            if (uiState.categoryList.size > 1) {
                item {
                    TradeCategoryFilterRow(
                        categories = uiState.categoryList,
                        selected = uiState.selectedCategory,
                        onSelect = { viewModel.selectCategory(it) }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                uiState.error != null -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "데이터를 불러오지 못했습니다.\n${uiState.error}",
                                style = NaedaTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                filteredTransactions.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "거래내역이 없어요" else "검색 결과가 없습니다.",
                                style = NaedaTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    groupedTransactions.forEach { (date, txList) ->
                        item {
                            TransactionDateHeader(date = date)
                        }
                        items(txList, key = { "${it.paymentId}:${it.createdAtRaw}" }) { item ->
                            RecentTradeTransactionRow(
                                item = item,
                                onClick = {
                                    if (item.paymentId > 0L) {
                                        selectedPaymentItem = item
                                        viewModel.loadPaymentDetail(context, item.paymentId)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
        val paymentItem = selectedPaymentItem
        val paymentDetail = uiState.selectedPaymentDetail
        if (paymentItem != null && paymentDetail != null) {
            TradePaymentDetailScreen(
                item = paymentItem,
                detail = paymentDetail,
                onClose = {
                    selectedPaymentItem = null
                    viewModel.clearPaymentDetail()
                }
            )
        }
    }

    if (showPeriodDialog) {
        TradePeriodPickerDialog(
            selected = uiState.selectedPeriod,
            onSelect = {
                viewModel.updatePeriod(it)
                showPeriodDialog = false
            },
            onDismiss = { showPeriodDialog = false }
        )
    }

    if (uiState.isDetailLoading) {
        AlertDialog(
            onDismissRequest = {
                selectedPaymentItem = null
                viewModel.clearPaymentDetail()
            },
            confirmButton = {},
            title = { Text("결제 상세 조회 중") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    uiState.detailError?.let { message ->
        AlertDialog(
            onDismissRequest = {
                selectedPaymentItem = null
                viewModel.clearPaymentDetail()
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedPaymentItem = null
                    viewModel.clearPaymentDetail()
                }) {
                    Text("확인")
                }
            },
            title = { Text("오류") },
            text = { Text(message) }
        )
    }

}

@Composable
private fun RecentTradeReportHeaderV2(
    accountName: String,
    accountNumber: String,
    incomeTotal: Long,
    expenseTotal: Long,
    onBack: () -> Unit,
    isSearchMode: Boolean,
    onSearchToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Mint900, Mint500)
                )
            )
            .padding(bottom = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.White
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uCD5C\uADFC \uAC70\uB798\uB0B4\uC5ED",
                        style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchMode) "검색 닫기" else "검색",
                        tint = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "입금",
                            style = NaedaTypography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+${"%,d".format(incomeTotal)}원",
                            style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFDCEBFF)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "출금",
                            style = NaedaTypography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-${"%,d".format(expenseTotal)}원",
                            style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFD9D0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTradeReportHeader(
    accountName: String,
    accountNumber: String,
    incomeTotal: Long,
    expenseTotal: Long,
    onBack: () -> Unit,
    isSearchMode: Boolean,
    onSearchToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Mint900, Mint500)
                )
            )
            .padding(bottom = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchMode) "검색 닫기" else "검색",
                        tint = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "수입",
                            style = NaedaTypography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+${"%,d".format(incomeTotal)}원",
                            style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFDCEBFF)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "지출",
                            style = NaedaTypography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-${"%,d".format(expenseTotal)}원",
                            style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFD9D0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeReportHeader(
    accountName: String,
    accountNumber: String,
    incomeTotal: Long,
    expenseTotal: Long,
    onBack: () -> Unit,
    isSearchMode: Boolean,
    onSearchToggle: () -> Unit
) {
    val balance = expenseTotal

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
                )
            )
            .padding(bottom = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.White
                    )
                }

                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchMode) "검색 닫기" else "검색",
                        tint = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "거래내역",
                    style = NaedaTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "수입",
                            style = NaedaTypography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+${"%,d".format(incomeTotal)}원",
                            style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFDCEBFF)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "지출",
                            style = NaedaTypography.labelMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-${"%,d".format(expenseTotal)}원",
                            style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFD9D0)
                        )
                    }
                }
                if (false) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "현재 잔액",
                    style = NaedaTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${"%,d".format(balance)}원",
                    style = NaedaTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                }
            }
        }
    }
}

@Composable
private fun TradeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        singleLine = true,
        placeholder = {
            Text(
                text = "결제내역 검색",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "검색어 지우기",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun TradeFilterRowDropdown(
    selectedPeriod: String,
    selected: String,
    onSelect: (String) -> Unit,
    onPeriodClick: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariant)
                    .clickable { showSortMenu = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "정렬",
                    style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Mint900
                )
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = Mint900,
                    modifier = Modifier.size(12.dp)
                )
            }

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                listOf("전체", "입금", "출금").forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (selected == option) Mint900 else OnBackground
                            )
                        },
                        onClick = {
                            onSelect(option)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceVariant)
                .clickable { onPeriodClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = selectedPeriod,
                style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Mint900
            )
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = Mint900,
                modifier = Modifier.size(12.dp)
            )
        }
    }

    HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
}

@Composable
private fun TradeFilterRow(
    selectedPeriod: String,
    onPeriodClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "거래내역",
            style = NaedaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onPeriodClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = selectedPeriod,
                style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
}

@Composable
private fun TradeFlowFilterRow(
    selected: String,
    onSelect: (String) -> Unit
) {
    val options = listOf("전체", "입금", "출금")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(top = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            val isSelected = selected == option

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Mint900 else SurfaceVariant)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = option,
                    style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) Color.White else OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TradeCategoryFilterRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 10.dp, bottom = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selected == category

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(category) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = category,
                    style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TradePeriodPickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val periods = listOf("전체", "1주일", "1개월", "3개월", "6개월")

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = "기간 선택",
                    style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                periods.forEach { period ->
                    val isSelected = selected == period

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(period) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = period,
                            style = NaedaTypography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "취소",
                        style = NaedaTypography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDateHeader(date: String) {
    Text(
        text = date,
        style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun RecentTradeTransactionRow(
    item: TradeReportItem,
    onClick: () -> Unit
) {
    val amountText = if (item.isIncome) {
        "+${"%,d".format(item.amountValue)}원"
    } else {
        "-${"%,d".format(item.amountValue)}원"
    }
    val amountColor = if (item.isIncome) Color(0xFF307CBF) else Color(0xFFF2522E)
    val subtitle = listOfNotNull(
        item.time.takeIf { it.isNotBlank() },
        item.category.takeIf { it.isNotBlank() }
    ).joinToString(" · ")
    val paymentInfo = listOfNotNull(
        item.bankName.takeIf { it.isNotBlank() },
        item.accountNumber.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
        .ifBlank { "-" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = NaedaTypography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = paymentInfo,
                style = NaedaTypography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Text(
            text = amountText,
            style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = amountColor
        )
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun TradeTransactionRow(
    item: TradeReportItem,
    onClick: () -> Unit
) {
    val amountColor = if (item.isIncome) Color(0xFF1F8F5F) else MaterialTheme.colorScheme.onBackground

    val subtitle = listOfNotNull(
        item.time.takeIf { it.isNotBlank() },
        item.category.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (item.isIncome) Color(0xFFDFF7E8)
                    else if (item.title == "결제 실패") Color(0xFFFFEBEE)
                    else Color(0xFFDCEBFF)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = if (item.isIncome) Color(0xFF1F8F5F) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = NaedaTypography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

        }

        Text(
            text = item.amount,
            style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = amountColor
        )
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun PaymentDetailDialog(
    detail: PaymentDetailResponse,
    storeName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        },
        title = {
            Text(
                text = "결제 상세",
                style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("결제 금액", "${"%,d".format(detail.amount)}원")
                DetailRow("결제 방식", detail.authMethod.toPaymentMethodLabel())
                DetailRow("결제 시간", detail.createdAt?.formatCreatedAt() ?: "-")
                DetailRow(
                    "적립 포인트",
                    detail.earnedPoints?.let { "${"%,d".format(it)}P" } ?: "—"
                )
                DetailRow("결제 번호", detail.ssafyTransactionId ?: detail.paymentId.toString())
                DetailRow("결제 장소", storeName.ifBlank { detail.storeId.toString() })
            }
        }
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = NaedaTypography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = NaedaTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
