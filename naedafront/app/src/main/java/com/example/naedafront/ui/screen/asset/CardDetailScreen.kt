// File: app/src/main/java/com/example/naedafront/ui/screen/asset/CardDetailScreen.kt
package com.example.naedafront.ui.screen.asset

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface as MaterialSurface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.repository.CardRepository
import com.example.naedafront.data.repository.CardTransactionItemData
import com.example.naedafront.ui.theme.Background
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

private const val TAG = "CardDetailScreen"

data class CardHeaderUi(
    val cardId: Long,
    val cardName: String = "",
    val cardNo: String = ""
)

data class CardTransactionItem(
    val id: String,
    val merchantName: String,
    val category: String,
    val amount: Long,
    val isCanceled: Boolean = false,
    val transacted: String
) {
    val dateLabel: String
        get() = transactedAt.toDateLabel()

    val timeLabel: String
        get() = transactedAt.toTimeLabel()
}

data class CardDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedCardId: Long? = null,
    val selectedCard: CardHeaderUi? = null,
    val selectedPeriod: String = "전체",
    val selectedCategory: String = "전체",
    val transactions: List<CardTransactionItem> = emptyList(),
    val selectedTransaction: CardTransactionItem? = null
) {
    val categoryList: List<String>
        get() = listOf("전체") + transactions
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()

    val filteredTransactions: List<CardTransactionItem>
        get() = if (selectedCategory == "전체") {
            transactions
        } else {
            transactions.filter { it.category == selectedCategory }
        }
}

class CardDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState: StateFlow<CardDetailUiState> = _uiState.asStateFlow()

    fun loadInitial(
        context: Context,
        initialCardId: Long? = null,
        initialCardName: String = "",
        initialCardNo: String = ""
    ) {
        val userNo = AuthPrefs.getUserNo(context)

        if (userNo == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "로그인 사용자 정보(userNo)가 없습니다."
                )
            }
            return
        }

        if (initialCardId == null || initialCardId <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "유효한 카드 정보가 없습니다."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                selectedCardId = initialCardId,
                selectedCard = CardHeaderUi(
                    cardId = initialCardId,
                    cardName = initialCardName,
                    cardNo = initialCardNo
                ),
                selectedTransaction = null,
                selectedCategory = "전체"
            )
        }

        loadTransactions(
            context = context,
            cardId = initialCardId,
            period = _uiState.value.selectedPeriod
        )
    }

    fun selectPeriod(context: Context, period: String) {
        val cardId = _uiState.value.selectedCardId ?: return

        _uiState.update {
            it.copy(
                selectedPeriod = period,
                selectedCategory = "전체",
                selectedTransaction = null,
                error = null
            )
        }

        loadTransactions(context = context, cardId = cardId, period = period)
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun clearSelectedTransaction() {
        _uiState.update { it.copy(selectedTransaction = null) }
    }

    fun selectTransaction(transaction: CardTransactionItem) {
        _uiState.update { it.copy(selectedTransaction = transaction) }
    }

    private fun loadTransactions(
        context: Context,
        cardId: Long,
        period: String
    ) {
        val userNo = AuthPrefs.getUserNo(context)

        if (userNo == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "로그인 사용자 정보(userNo)가 없습니다."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    selectedTransaction = null
                )
            }

            Log.d(TAG, "loadTransactions | userNo=$userNo cardId=$cardId period=$period")

            CardRepository.getCardTransactions(
                userNo = userNo,
                cardId = cardId,
                period = period
            ).onSuccess { items ->
                val mapped = items.map { it.toUiModel() }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        transactions = mapped
                    )
                }
            }.onFailure { throwable ->
                Log.e(TAG, "loadTransactions failed", throwable)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "거래내역 조회에 실패했습니다."
                    )
                }
            }
        }
    }
}

private fun CardTransactionItemData.toUiModel(): CardTransactionItem {
    return CardTransactionItem(
        id = transactionId,
        merchantName = merchantName,
        category = category,
        amount = amount,
        isCanceled = isCanceled,
        transacted = transactedAt
    )
}

private fun CardTransactionItem.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val keyword = query.trim().lowercase()

    return listOf(
        merchantName,
        category,
        amount.toString(),
        date,
        time,
        id
    ).any { it.lowercase().contains(keyword) }
}

@Composable
fun CardDetailRoute(
    cardId: Long? = null,
    cardName: String = "",
    cardNo: String = "",
    onBack: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: CardDetailViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showPeriodDialog by rememberSaveable { mutableStateOf(false) }
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(cardId, cardName, cardNo) {
        viewModel.loadInitial(
            context = context,
            initialCardId = cardId,
            initialCardName = cardName,
            initialCardNo = cardNo
        )
    }

    CardDetailScreen(
        uiState = uiState,
        isSearchMode = isSearchMode,
        searchQuery = searchQuery,
        onSearchToggle = {
            if (isSearchMode) {
                searchQuery = ""
                isSearchMode = false
            } else {
                isSearchMode = true
            }
        },
        onSearchQueryChange = { searchQuery = it },
        onBack = onBack,
        onPeriodClick = { showPeriodDialog = true },
        onCategorySelect = { viewModel.selectCategory(it) },
        onTransactionClick = { viewModel.selectTransaction(it) },
        onDismissDetail = { viewModel.clearSelectedTransaction() }
    )

    if (showPeriodDialog) {
        CardPeriodPickerDialog(
            selected = uiState.selectedPeriod,
            onSelect = {
                showPeriodDialog = false
                viewModel.selectPeriod(context, it)
            },
            onDismiss = { showPeriodDialog = false }
        )
    }
}

@Composable
fun CardDetailScreen(
    uiState: CardDetailUiState,
    isSearchMode: Boolean,
    searchQuery: String,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onPeriodClick: () -> Unit,
    onCategorySelect: (String) -> Unit,
    onTransactionClick: (CardTransactionItem) -> Unit,
    onDismissDetail: () -> Unit,
    onDismissError: () -> Unit
) {
    val filteredBySearch = remember(uiState.filteredTransactions, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.filteredTransactions
        } else {
            val keyword = searchQuery.trim().lowercase()
            uiState.filteredTransactions.filter { transaction ->
                transaction.merchantName.lowercase().contains(keyword) ||
                        transaction.category.lowercase().contains(keyword) ||
                        transaction.amount.toString().contains(keyword) ||
                        transaction.transactedAt.lowercase().contains(keyword)
            }
        }
    }

    val groupedTransactions = remember(filteredBySearch) {
        filteredBySearch.groupBy { it.dateLabel }
    }

    val thisMonthTotal = remember(uiState.transactions) {
        uiState.transactions
            .filter { !it.isCanceled }
            .filter { it.transactedAt.toYearMonth() == currentYearMonth() }
            .sumOf { it.amount }
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    CardDetailHeader(
                        card = uiState.selectedCard,
                        thisMonthTotal = thisMonthTotal,
                        isSearchMode = isSearchMode,
                        onBack = onBack,
                        onSearchToggle = onSearchToggle
                    )
                }

                if (isSearchMode) {
                    item {
                        CardSearchBar(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange
                        )
                    }
                }

                item {
                    CardFilterSection(
                        selectedPeriod = uiState.selectedPeriod,
                        categories = uiState.categoryList,
                        selectedCategory = uiState.selectedCategory,
                        onPeriodClick = onPeriodClick,
                        onCategorySelect = onCategorySelect
                    )
                }

                if (!uiState.isLoading && filteredBySearch.isEmpty()) {
                    item {
                        EmptyTransactionView(
                            message = if (uiState.error.isNullOrBlank()) {
                                "거래내역이 없습니다."
                            } else {
                                "거래내역을 불러오지 못했습니다."
                            }
                        )
                    }
                } else {
                    groupedTransactions.forEach { (date, itemsForDate) ->
                        item(key = "header_$date") {
                            TransactionDateHeader(date = date)
                        }

                        items(
                            items = itemsForDate,
                            key = { it.id }
                        ) { item ->
                            CardTransactionRow(
                                item = item,
                                onClick = { onTransactionClick(item) }
                            )
                        }
                    }
                }

                searchedTransactions.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "결제내역이 없어요" else "검색 결과가 없습니다.",
                                style = NaedaTypography.bodyMedium,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    grouped.forEach { (date, txList) ->
                        item {
                            Text(
                                text = date,
                                style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = OnSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Background)
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }

                        items(txList, key = { it.id }) { tx ->
                            CardTransactionRow(
                                tx = tx,
                                onClick = { onTransactionClick(tx) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    uiState.selectedTransaction?.let { transaction ->
        CardTransactionDetailDialog(
            transaction = transaction,
            isLoading = uiState.isDetailLoading,
            onDismiss = onDismissDetail
        )
    }
}

@Composable
private fun CardDetailHeader(
    card: CardHeaderUi?,
    thisMonthTotal: Long,
    isSearchMode: Boolean,
    onBack: () -> Unit,
    onSearchToggle: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(Mint700, Mint900)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(top = 12.dp, start = 20.dp, end = 20.dp, bottom = 28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "검색",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = card?.cardName?.takeIf { it.isNotBlank() } ?: "카드 상세",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!card?.cardNo.isNullOrBlank()) {
            Text(
                text = maskCardNumber(card?.cardNo.orEmpty()),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }

        Text(
            text = "이번 달 사용금액",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${formatAmount(thisMonthTotal)}원",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CardSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        placeholder = { Text("가맹점명, 카테고리, 금액 검색") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Mint900,
            unfocusedBorderColor = OutlineVariant,
            focusedTextColor = OnBackground,
            unfocusedTextColor = OnBackground,
            cursorColor = Mint900,
            focusedContainerColor = SurfaceColor,
            unfocusedContainerColor = SurfaceColor
        )
    )
}

@Composable
private fun CardFilterSection(
    selectedPeriod: String,
    onPeriodClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "결제내역",
            style = NaedaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = OnBackground
        )

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
}

@Composable
private fun CardCategoryFilterRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(bottom = 12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = selected == category
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Mint900 else SurfaceVariant)
                    .clickable { onSelect(category) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = category,
                    style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) Color.White else OnSurfaceVariant
                )
            }
        }
    }

    HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
}

@Composable
private fun CardTransactionRow(
    item: CardTransactionItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (tx.isCanceled) Color(0xFFFFEBEE) else Color(0xFFDCEBFF)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (tx.isCanceled) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = if (tx.isCanceled) Color(0xFFD32F2F) else Mint900,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.merchantName.ifBlank { "가맹점 정보 없음" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${item.category} · ${item.timeLabel}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = if (item.isCanceled) {
                    "-${formatAmount(item.amount)}원"
                } else {
                    "${formatAmount(item.amount)}원"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        HorizontalDivider(color = Color(0xFFF0F0F0))
    }
}

@Composable
private fun EmptyTransactionView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 72.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun CardTransactionDetailDialog(
    transaction: CardTransactionItem,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = transaction.merchantName.ifBlank { "거래 상세" },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow("가맹점", transaction.displayMerchantName())
                DetailRow("결제 금액", "${"%,d".format(transaction.amount)}원")
                DetailRow("거래 시간", transaction.transacted.toDisplayDateTime())
                DetailRow("카테고리", transaction.category)
                DetailRow("거래 상태", if (transaction.isCanceled) "취소" else "승인")
                DetailRow("카드명", card?.cardName.orEmpty().ifBlank { "-" })
                DetailRow("카드 번호", card?.cardNo?.maskCardNumber().orEmpty().ifBlank { "-" })
                DetailRow("거래 ID", transaction.id)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = value,
            color = Color.Black,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CardPeriodPickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val periods = listOf("전체", "1주일", "1개월", "3개월", "6개월")

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        MaterialSurface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceColor,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = "기간 선택",
                    style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnBackground,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                periods.forEach { period ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(period) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = period,
                            color = if (period == selected) Mint900 else Color.Black,
                            fontWeight = if (period == selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

private fun formatAmount(amount: Long): String {
    return "%,d".format(amount)
}

private fun currentYearMonth(): String {
    return SimpleDateFormat("yyyyMM", Locale.KOREA).format(Calendar.getInstance().time)
}

private fun String.toYearMonth(): String {
    val digits = filter { it.isDigit() }
    return if (digits.length >= 6) digits.substring(0, 6) else ""
}

private fun String.toDateLabel(): String {
    val digits = filter { it.isDigit() }

    return if (digits.length >= 8) {
        val yyyy = digits.substring(0, 4)
        val mm = digits.substring(4, 6)
        val dd = digits.substring(6, 8)
        "$yyyy.$mm.$dd"
    } else {
        ifBlank { "날짜 없음" }
    }
}

private fun String.toTimeLabel(): String {
    val digits = filter { it.isDigit() }

    return if (digits.length >= 12) {
        val hh = digits.substring(8, 10)
        val mm = digits.substring(10, 12)
        "$hh:$mm"
    } else {
        ""
    }
}

private fun String.toDisplayDateTime(): String {
    val parsed = parseFlexibleDate(this) ?: return this
    return SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(parsed)
}

private fun parseFlexibleDate(raw: String): java.util.Date? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy.MM.dd HH:mm",
        "yyyy-MM-dd"
    )

    for (pattern in patterns) {
        runCatching {
            return SimpleDateFormat(pattern, Locale.KOREA).parse(raw)
        }
    }
    return null
}

private fun currentYearMonth(): String {
    return SimpleDateFormat("yyyy.MM", Locale.KOREA).format(java.util.Date())
}

private fun CardTransactionItem.displayMerchantName(): String {
    return merchantName.ifBlank { "가맹점 정보 없음" }
}
