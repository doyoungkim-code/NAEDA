package com.example.naedafront.ui.screen.asset

import android.content.Context
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.response.CardResponse
import com.example.naedafront.data.repository.CardRepository
import com.example.naedafront.data.repository.CardTransactionItemData
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint50
import com.example.naedafront.ui.theme.Mint700
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import com.example.naedafront.ui.theme.OutlineVariant
import com.example.naedafront.ui.theme.Surface
import com.example.naedafront.ui.theme.SurfaceVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class CardTransactionItem(
    val id: String,
    val merchantName: String,
    val category: String,
    val amount: Long,
    val isCanceled: Boolean = false,
    val transacted: String,
    val approvalNumber: String? = null,
    val cardNo: String? = null,
    val installment: String? = null,
    val raw: CardTransactionItemData
) {
    val date: String
        get() = transacted.toDateKey()

    val time: String
        get() = transacted.toTimeOnly()
}

data class CardDetailUiState(
    val isLoading: Boolean = false,
    val isDetailLoading: Boolean = false,
    val error: String? = null,
    val cards: List<CardResponse> = emptyList(),
    val selectedCard: CardResponse? = null,
    val selectedPeriod: String = "전체",
    val selectedCategory: String = "전체",
    val transactions: List<CardTransactionItem> = emptyList(),
    val selectedTransaction: CardTransactionItem? = null
) {
    val categoryList: List<String>
        get() = listOf("전체") + transactions.map { it.category }.distinct().sorted()

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

    fun loadInitial(context: Context, initialCardId: Long? = null) {
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
            _uiState.update { it.copy(isLoading = true, error = null) }

            CardRepository.getCards(userNo)
                .onSuccess { cards ->
                    val selected = when {
                        cards.isEmpty() -> null
                        initialCardId != null -> cards.firstOrNull { it.cardId == initialCardId } ?: cards.first()
                        else -> cards.first()
                    }

                    _uiState.update {
                        it.copy(
                            cards = cards,
                            selectedCard = selected
                        )
                    }

                    if (selected != null) {
                        loadTransactions(context, selected.cardId, _uiState.value.selectedPeriod)
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "조회된 카드가 없습니다."
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "카드 목록을 불러오지 못했습니다."
                        )
                    }
                }
        }
    }

    fun selectPeriod(context: Context, period: String) {
        val cardId = _uiState.value.selectedCard?.cardId ?: return
        _uiState.update {
            it.copy(
                selectedPeriod = period,
                selectedTransaction = null
            )
        }
        loadTransactions(context, cardId, period)
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun clearSelectedTransaction() {
        _uiState.update {
            it.copy(
                selectedTransaction = null,
                isDetailLoading = false
            )
        }
    }

    fun loadTransactionDetail(context: Context, transaction: CardTransactionItem) {
        val userNo = AuthPrefs.getUserNo(context) ?: return
        val cardId = _uiState.value.selectedCard?.cardId ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDetailLoading = true,
                    selectedTransaction = null
                )
            }

            CardRepository.getCardTransactions(
                userNo = userNo,
                cardId = cardId,
                period = _uiState.value.selectedPeriod,
                transactionId = transaction.id
            ).onSuccess { result ->
                val detail = result.firstOrNull {
                    it.transactionId == transaction.id
                }?.toUiItem() ?: transaction

                _uiState.update {
                    it.copy(
                        isDetailLoading = false,
                        selectedTransaction = detail
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isDetailLoading = false,
                        selectedTransaction = transaction
                    )
                }
            }
        }
    }

    private fun loadTransactions(
        context: Context,
        cardId: Long,
        period: String
    ) {
        val userNo = AuthPrefs.getUserNo(context) ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    transactions = emptyList(),
                    selectedCategory = "전체"
                )
            }

            CardRepository.getCardTransactions(
                userNo = userNo,
                cardId = cardId,
                period = period
            ).onSuccess { items ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        transactions = items.map { response -> response.toUiItem() }
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "거래내역을 불러오지 못했습니다."
                    )
                }
            }
        }
    }
}

private fun CardTransactionItemData.toUiItem(): CardTransactionItem {
    return CardTransactionItem(
        id = transactionId,
        merchantName = merchantName,
        category = category,
        amount = amount,
        isCanceled = isCanceled,
        transacted = transactedAt,
        approvalNumber = approvalNumber,
        cardNo = cardNo,
        installment = installment,
        raw = this
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
    onBack: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: CardDetailViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showPeriodDialog by rememberSaveable { mutableStateOf(false) }
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(cardId) {
        viewModel.loadInitial(context, cardId)
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
        onTransactionClick = { viewModel.loadTransactionDetail(context, it) },
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
    onDismissDetail: () -> Unit
) {
    val selectedCard = uiState.selectedCard

    val searchedTransactions = remember(uiState.filteredTransactions, searchQuery) {
        uiState.filteredTransactions.filter { it.matches(searchQuery) }
    }

    val grouped = remember(searchedTransactions) {
        searchedTransactions.groupBy { it.date }.toSortedMap(reverseOrder())
    }

    val thisMonthTotal = remember(uiState.transactions) {
        uiState.transactions
            .filter { !it.isCanceled && it.date.startsWith(currentYearMonth()) }
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
            if (selectedCard != null) {
                item {
                    CardDetailHeader(
                        card = selectedCard,
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
                    CardPeriodFilterRow(
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
                                text = uiState.error ?: "오류가 발생했습니다.",
                                style = NaedaTypography.bodyMedium,
                                color = OnSurfaceVariant
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

    if (uiState.isDetailLoading) {
        AlertDialog(
            onDismissRequest = onDismissDetail,
            confirmButton = {},
            title = { Text("거래 상세 조회 중") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Mint900)
                }
            }
        )
    }

    uiState.selectedTransaction?.let { tx ->
        CardTransactionDetailDialog(
            transaction = tx,
            card = uiState.selectedCard,
            onDismiss = onDismissDetail
        )
    }
}

@Composable
private fun CardDetailHeader(
    card: CardResponse,
    thisMonthTotal: Long,
    onBack: () -> Unit,
    isSearchMode: Boolean,
    onSearchToggle: () -> Unit
) {
    val gradient = card.cardGradient()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(gradient.first, gradient.second)
                )
            )
            .padding(bottom = 28.dp)
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 220.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )

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
                    text = card.cardIssuerName,
                    style = NaedaTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = card.cardName,
                    style = NaedaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = card.cardNo.maskCardNumber(),
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "이번달 결제금액",
                    style = NaedaTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%,d원".format(thisMonthTotal),
                    style = NaedaTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = card.cardType.toCardTypeLabel(),
                        style = NaedaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        fontSize = 11.sp
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
                text = "결제내역 검색",
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
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface
        )
    )
}

@Composable
private fun CardPeriodFilterRow(
    selectedPeriod: String,
    onPeriodClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
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
            .background(Surface)
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
    tx: CardTransactionItem,
    onClick: () -> Unit
) {
    val title = when {
        tx.isCanceled -> "결제 취소"
        tx.merchantName.isNotBlank() -> tx.merchantName
        else -> "내다페이 결제"
    }

    val subtitle = listOfNotNull(
        tx.time.takeIf { it.isNotBlank() },
        tx.category.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
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

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
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
                text = "거래 ID ${tx.id}",
                style = NaedaTypography.labelSmall,
                color = OnSurfaceVariant
            )
        }

        Text(
            text = if (tx.isCanceled) "+${"%,d".format(tx.amount)}원" else "-${"%,d".format(tx.amount)}원",
            style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = OnBackground
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
    card: CardResponse?,
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
                text = "거래 상세",
                style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow("가맹점", transaction.merchantName)
                DetailRow("결제 금액", "${"%,d".format(transaction.amount)}원")
                DetailRow("거래 시간", transaction.transacted.toDisplayDateTime())
                DetailRow("카테고리", transaction.category)
                DetailRow("거래 상태", if (transaction.isCanceled) "취소" else "승인")
                DetailRow("승인 번호", transaction.approvalNumber ?: "-")
                DetailRow("카드 번호", transaction.cardNo ?: card?.cardNo?.maskCardNumber().orEmpty().ifBlank { "-" })
                DetailRow("할부", transaction.installment ?: "일시불")
                DetailRow("거래 ID", transaction.id)
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
private fun CardPeriodPickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val periods = listOf("전체", "1주일", "1개월", "3개월", "6개월")

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Surface,
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

private fun CardResponse.cardGradient(): Pair<Color, Color> {
    val issuer = cardIssuerName.lowercase()
    return when {
        "shinhan" in issuer || "신한" in issuer -> Color(0xFF1E3A8A) to Color(0xFF2563EB)
        "kb" in issuer || "국민" in issuer -> Color(0xFF6D4C41) to Color(0xFFA1887F)
        "hana" in issuer || "하나" in issuer -> Color(0xFF00695C) to Color(0xFF26A69A)
        "woori" in issuer || "우리" in issuer -> Color(0xFF0D47A1) to Color(0xFF42A5F5)
        "hyundai" in issuer || "현대" in issuer -> Color(0xFF263238) to Color(0xFF546E7A)
        else -> Mint900 to Color(0xFF4DB6AC)
    }
}

private fun String.toCardTypeLabel(): String {
    return when (uppercase()) {
        "CREDIT" -> "신용카드"
        "CHECK", "DEBIT" -> "체크카드"
        else -> this
    }
}

private fun String.maskCardNumber(): String {
    val digits = replace("-", "").replace(" ", "")
    return if (digits.length >= 16) {
        "${digits.substring(0, 4)}-****-****-${digits.takeLast(4)}"
    } else {
        this
    }
}

private fun String.toDateKey(): String {
    val parsed = parseFlexibleDate(this) ?: return take(10)
    return SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(parsed)
}

private fun String.toTimeOnly(): String {
    val parsed = parseFlexibleDate(this) ?: return ""
    return SimpleDateFormat("HH:mm", Locale.KOREA).format(parsed)
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