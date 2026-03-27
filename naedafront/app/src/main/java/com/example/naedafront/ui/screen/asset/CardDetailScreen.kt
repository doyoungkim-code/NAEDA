package com.example.naedafront.ui.screen.asset

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.response.CardResponse
import com.example.naedafront.data.repository.CardRepository
import com.example.naedafront.data.repository.CardTransactionItemData
import com.example.naedafront.ui.theme.Background
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
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CardHeaderUi(
    val cardId: Long,
    val cardIssuerName: String = "",
    val cardName: String = "",
    val cardNo: String = "",
    val cardType: String = ""
)

data class CardTransactionItem(
    val transactionId: String,
    val merchantName: String,
    val category: String = "",
    val amount: Long,
    val isCanceled: Boolean,
    val transactedRaw: String = ""
) {
    val date: String
        get() = transactedRaw.toDateKey()

    val time: String
        get() = transactedRaw.toTimeOnly()

    val estimatedPoints: Long
        get() = if (isCanceled) 0L else amount * 5 / 100
}

data class CardDetailUiState(
    val selectedCard: CardHeaderUi? = null,
    val transactions: List<CardTransactionItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTransaction: CardTransactionItem? = null,
    val selectedPeriod: String = "전체",
    val selectedCategory: String = "전체"
) {
    val categoryList: List<String>
        get() = listOf("전체") + transactions
            .map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

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

    fun showTransactionDetail(transaction: CardTransactionItem) {
        _uiState.update { it.copy(selectedTransaction = transaction) }
    }

    fun clearSelectedTransaction() {
        _uiState.update { it.copy(selectedTransaction = null) }
    }

    fun loadData(
        context: Context,
        cardId: Long,
        fallbackCardName: String = "",
        fallbackCardNo: String = ""
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

        if (cardId <= 0L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "카드 정보를 찾을 수 없습니다."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    transactions = emptyList(),
                    selectedTransaction = null,
                    selectedCategory = "전체",
                    selectedCard = if (it.selectedCard?.cardId == cardId) {
                        it.selectedCard
                    } else {
                        CardHeaderUi(
                            cardId = cardId,
                            cardName = fallbackCardName,
                            cardNo = fallbackCardNo
                        )
                    }
                )
            }

            var resolvedCard = _uiState.value.selectedCard

            CardRepository.getCards(userNo)
                .onSuccess { cards ->
                    resolvedCard = cards.firstOrNull { it.cardId == cardId }?.toHeaderUi()
                        ?: resolvedCard
                }

            CardRepository.getCardTransactions(
                userNo = userNo,
                cardId = cardId,
                period = _uiState.value.selectedPeriod
            ).onSuccess { items ->
                _uiState.update {
                    it.copy(
                        selectedCard = resolvedCard,
                        transactions = items.map { item -> item.toUiItem() },
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        selectedCard = resolvedCard,
                        isLoading = false,
                        error = throwable.message ?: "카드 거래내역을 불러오지 못했습니다."
                    )
                }
            }
        }
    }
}

private fun CardResponse.toHeaderUi(): CardHeaderUi {
    return CardHeaderUi(
        cardId = cardId,
        cardIssuerName = cardIssuerName,
        cardName = cardName,
        cardNo = cardNo,
        cardType = cardType
    )
}

private fun CardTransactionItemData.toUiItem(): CardTransactionItem {
    return CardTransactionItem(
        transactionId = transactionId,
        merchantName = merchantName,
        category = category,
        amount = amount,
        isCanceled = isCanceled,
        transactedRaw = transactedAt
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
        transactionId
    ).any { value -> value.lowercase().contains(keyword) }
}

@Composable
fun CardDetailRoute(
    cardId: Long? = null,
    cardName: String = "",
    cardNo: String = "",
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: CardDetailViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showPeriodDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(cardId, cardName, cardNo, uiState.selectedPeriod) {
        viewModel.loadData(
            context = context,
            cardId = cardId ?: -1L,
            fallbackCardName = cardName,
            fallbackCardNo = cardNo
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
        onTransactionClick = { viewModel.showTransactionDetail(it) },
        onDismissDetail = { viewModel.clearSelectedTransaction() }
    )

    if (showPeriodDialog) {
        CardPeriodPickerDialog(
            selected = uiState.selectedPeriod,
            onSelect = {
                viewModel.updatePeriod(it)
                showPeriodDialog = false
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
    onDismissDetail: () -> Unit
) {
    val filteredTransactions = remember(uiState.filteredTransactions, searchQuery) {
        uiState.filteredTransactions.filter { it.matches(searchQuery) }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.date }.toSortedMap(reverseOrder())
    }

    val thisMonthTotal = remember(uiState.transactions) {
        uiState.transactions
            .filter { !it.isCanceled }
            .filter { it.transactedRaw.toYearMonthKey() == currentYearMonthKey() }
            .sumOf { it.amount }
    }

    Scaffold(
        containerColor = Background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                CardDetailHeader(
                    card = uiState.selectedCard,
                    thisMonthTotal = thisMonthTotal,
                    onBack = onBack,
                    isSearchMode = isSearchMode,
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
                CardFilterRow(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodClick = onPeriodClick
                )
            }

            if (uiState.categoryList.size > 1) {
                item {
                    CardCategoryFilterRow(
                        categories = uiState.categoryList,
                        selected = uiState.selectedCategory,
                        onSelect = onCategorySelect
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
                            CircularProgressIndicator(color = Mint900)
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
                                text = uiState.error,
                                style = NaedaTypography.bodyMedium,
                                color = OnSurfaceVariant
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
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    groupedTransactions.forEach { (date, txList) ->
                        item {
                            TransactionDateHeader(date = date)
                        }
                        items(txList, key = { it.transactionId }) { item ->
                            CardTransactionRow(
                                item = item,
                                onClick = { onTransactionClick(item) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    uiState.selectedTransaction?.let { transaction ->
        CardTransactionDetailScreen(
            transaction = transaction,
            card = uiState.selectedCard,
            onClose = onDismissDetail
        )
    }
}

@Composable
private fun CardDetailHeader(
    card: CardHeaderUi?,
    thisMonthTotal: Long,
    onBack: () -> Unit,
    isSearchMode: Boolean,
    onSearchToggle: () -> Unit
) {
    val gradientColors = card?.cardGradient() ?: (Mint900 to Mint500)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(gradientColors.first, gradientColors.second)
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
                Text(
                    text = card?.cardNo.orEmpty().maskCardNumber(),
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card?.cardName.orEmpty().ifBlank {
                        card?.cardIssuerName.orEmpty().ifBlank { "등록 카드" }
                    },
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "이번 달 결제 금액",
                    style = NaedaTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatAmount(thisMonthTotal)}원",
                    style = NaedaTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                if (!card?.cardType.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = card?.cardType.toCardTypeLabel(),
                        style = NaedaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
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
            .padding(horizontal = 20.dp, vertical = 14.dp),
        singleLine = true,
        placeholder = {
            Text(
                text = "거래내역 검색",
                color = OnSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Mint900
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "검색어 지우기",
                        tint = OnSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
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
private fun CardFilterRow(
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
            text = "거래내역",
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

    HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
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
private fun TransactionDateHeader(date: String) {
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

@Composable
private fun CardTransactionRow(
    item: CardTransactionItem,
    onClick: () -> Unit
) {
    val amountText = if (item.isCanceled) {
        "+${formatAmount(item.amount)}원"
    } else {
        "-${formatAmount(item.amount)}원"
    }
    val amountColor = if (item.isCanceled) Color(0xFF1F8F5F) else OnBackground
    val iconBackground = if (item.isCanceled) Color(0xFFDFF7E8) else Color(0xFFDCEBFF)
    val iconTint = if (item.isCanceled) Color(0xFF1F8F5F) else Mint900
    val subtitle = listOfNotNull(
        item.time.takeIf { it.isNotBlank() },
        item.category.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    Row(
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
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.isCanceled) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.merchantName.ifBlank { "가맹점 정보 없음" },
                style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = OnBackground,
                maxLines = 1
            )

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = NaedaTypography.labelSmall,
                    color = OnSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "적립 포인트 ${formatAmount(item.estimatedPoints)}P",
                style = NaedaTypography.labelSmall,
                color = OnSurfaceVariant
            )
        }

        Text(
            text = amountText,
            style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = amountColor
        )
    }

    HorizontalDivider(
        color = OutlineVariant,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun CardTransactionDetailDialog(
    transaction: CardTransactionItem,
    card: CardHeaderUi?,
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow("결제 금액", "${formatAmount(transaction.amount)}원")
                DetailRow("결제 방식", "카드 결제")
                DetailRow(
                    "결제 시간",
                    transaction.transactedRaw.toDisplayDateTime().ifBlank { "-" }
                )
                DetailRow("적립 포인트", "${formatAmount(transaction.estimatedPoints)}P")
                DetailRow("결제 번호", transaction.transactionId)
                DetailRow("결제 장소", transaction.merchantName.ifBlank { "가맹점 정보 없음" })
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
            color = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = NaedaTypography.bodyMedium,
            color = OnBackground
        )
    }
}

@Composable
private fun CardTransactionDetailFullScreen(
    transaction: CardTransactionItem,
    card: CardHeaderUi?,
    onDismiss: () -> Unit
) {
    val statusText = if (transaction.isCanceled) "결제 취소" else "결제 완료"
    val accentColor = if (transaction.isCanceled) Color(0xFF1F8F5F) else Mint900
    val paymentMethod = buildString {
        append(card?.cardName?.ifBlank { "카드 결제" } ?: "카드 결제")
        val maskedNo = card?.cardNo?.maskCardNumber().orEmpty()
        if (maskedNo.isNotBlank() && maskedNo != "-") {
            append(" ")
            append(maskedNo)
        }
    }

    CardDetailFullScreenLayout(
        title = "결제 상세",
        badgeText = "결",
        headlineLabel = "결제 장소",
        headlineValue = transaction.merchantName.ifBlank { "가맹점 정보 없음" },
        amountText = "${formatAmount(transaction.amount)}원",
        statusText = statusText,
        accentColor = accentColor,
        onDismiss = onDismiss
    ) {
        CardDetailField("결제 방식", paymentMethod)
        CardDetailField(
            "결제 시간",
            transaction.transactedRaw.toDisplayDateTime().ifBlank { "-" }
        )
        CardDetailField("적립 포인트", "${formatAmount(transaction.estimatedPoints)}P")
        CardDetailField("결제 번호", transaction.transactionId)
        CardDetailField("결제 장소", transaction.merchantName.ifBlank { "가맹점 정보 없음" })
    }
}

@Composable
private fun CardDetailFullScreenLayout(
    title: String,
    badgeText: String,
    headlineLabel: String,
    headlineValue: String,
    amountText: String,
    statusText: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F2F5))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = accentColor
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.size(42.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MaterialSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(36.dp),
                    color = Color.White,
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeText,
                                style = NaedaTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = headlineLabel,
                            style = NaedaTypography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = headlineValue,
                            style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OnBackground
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        MaterialSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            color = Color(0xFFF7F3F6),
                            shadowElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TOTAL TRANSACTION",
                                    style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = amountText,
                                    style = NaedaTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                CardDetailStatusChip(
                                    text = statusText,
                                    accentColor = accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            content = content
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                CardDetailPrimaryButton(
                    text = "닫기",
                    onClick = onDismiss
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CardDetailStatusChip(
    text: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = text,
            style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = accentColor
        )
    }
}

@Composable
private fun CardDetailField(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = NaedaTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = OnBackground
        )
    }
}

@Composable
private fun CardDetailPrimaryButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Mint900)
            .clickable { onClick() }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = NaedaTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
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
                            color = if (isSelected) Mint900 else OnBackground
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Mint900)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = OutlineVariant,
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
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatAmount(amount: Long): String {
    return "%,d".format(amount)
}

private fun currentYearMonthKey(): String {
    return SimpleDateFormat("yyyyMM", Locale.KOREA).format(Calendar.getInstance().time)
}

private fun String.toYearMonthKey(): String {
    val parsed = parseFlexibleDate(this)
    return if (parsed != null) {
        SimpleDateFormat("yyyyMM", Locale.KOREA).format(parsed)
    } else {
        filter { it.isDigit() }.take(6)
    }
}

private fun String.toDateKey(): String {
    val parsed = parseFlexibleDate(this)
    return if (parsed != null) {
        SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(parsed)
    } else {
        take(10)
    }
}

private fun String.toTimeOnly(): String {
    val parsed = parseFlexibleDate(this) ?: return ""
    return SimpleDateFormat("HH:mm", Locale.KOREA).format(parsed)
}

private fun String.toDisplayDateTime(): String {
    val parsed = parseFlexibleDate(this) ?: return this
    val calendar = Calendar.getInstance().apply { time = parsed }
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
}

private fun parseFlexibleDate(raw: String): java.util.Date? {
    val normalizedRaw = raw.trim().replace(
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
        }
        val position = ParsePosition(0)
        val parsed = formatter.parse(normalizedRaw, position)
        if (parsed != null && position.index == normalizedRaw.length) {
            return parsed
        }
    }
    return null
}

private fun String.maskCardNumber(): String {
    val digits = replace("-", "").replace(" ", "")
    return if (digits.length >= 16) {
        "${digits.substring(0, 4)}-****-****-${digits.takeLast(4)}"
    } else if (isBlank()) {
        "-"
    } else {
        this
    }
}

private fun String?.toCardTypeLabel(): String {
    return when (this?.uppercase()) {
        "CREDIT" -> "신용카드"
        "CHECK", "DEBIT" -> "체크카드"
        else -> this ?: "-"
    }
}

private fun CardHeaderUi.cardGradient(): Pair<Color, Color> {
    val issuer = cardIssuerName.lowercase()
    return when {
        "shinhan" in issuer || "신한" in issuer -> Color(0xFF1E3A8A) to Color(0xFF2563EB)
        "kb" in issuer || "국민" in issuer -> Color(0xFF6D4C41) to Color(0xFFA1887F)
        "hana" in issuer || "하나" in issuer -> Color(0xFF00695C) to Color(0xFF26A69A)
        "woori" in issuer || "우리" in issuer -> Color(0xFF0D47A1) to Color(0xFF42A5F5)
        "hyundai" in issuer || "현대" in issuer -> Color(0xFF263238) to Color(0xFF546E7A)
        else -> Mint900 to Mint500
    }
}
