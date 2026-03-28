package com.example.naedafront.ui.screen.asset

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.window.Dialog
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.AssetRepository
import com.example.naedafront.data.remote.PaymentResponse
import com.example.naedafront.data.remote.response.PaymentDetailResponse
import androidx.compose.material3.MaterialTheme
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class TransactionItem(
    val id: String,
    val transactionType: String,
    val counterpart: String,
    val memo: String,
    val category: String,
    val amount: Long,
    val balanceAfter: Long,
    val ssafyTransactionId: String = "",
    val transacted: String,
) {
    val date: String get() = transacted.take(10)
    val time: String get() = if (transacted.length >= 16) transacted.substring(11, 16) else ""
    val estimatedPoints: Long
        get() = if (
            transactionType.equals("DEPOSIT", ignoreCase = true) ||
            ssafyTransactionId.isBlank()
        ) {
            0L
        } else {
            amount * 5 / 100
        }
}

private val periodList = listOf("전체", "1주일", "1개월", "3개월", "6개월")

@Composable
fun AccountDetailScreen(
    account: AccountItem,
    balance: Long,
    transactions: List<TransactionItem>,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedPeriod by rememberSaveable { mutableStateOf("전체") }
    var selectedCategory by rememberSaveable { mutableStateOf("전체") }
    var showPeriodDialog by remember { mutableStateOf(false) }
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTransaction by remember { mutableStateOf<TransactionItem?>(null) }
    var selectedPaymentDetail by remember { mutableStateOf<PaymentDetailResponse?>(null) }
    var selectedPaymentSummary by remember { mutableStateOf<PaymentResponse?>(null) }
    var isDetailLoading by remember { mutableStateOf(false) }
    var detailError by remember { mutableStateOf<String?>(null) }

    val availableCategories = remember(transactions) {
        listOf("전체") + transactions.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    LaunchedEffect(availableCategories) {
        if (selectedCategory !in availableCategories) {
            selectedCategory = "전체"
        }
    }

    val periodFiltered = remember(transactions, selectedPeriod) {
        filterTransactionsByPeriod(transactions, selectedPeriod)
    }

    val categoryFiltered = remember(periodFiltered, selectedCategory) {
        if (selectedCategory == "전체") {
            periodFiltered
        } else {
            periodFiltered.filter { it.category == selectedCategory }
        }
    }

    val filteredTransactions = remember(categoryFiltered, searchQuery) {
        if (searchQuery.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { it.matches(searchQuery) }
        }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.date }.toSortedMap(reverseOrder())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0)) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                AccountDetailHeader(
                    bankName = account.bankName,
                    accountName = account.accountName,
                    accountNumber = account.accountNumber,
                    balance = balance,
                    onBack = onBack,
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
                    AccountSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )
                }
            }

            item {
                PeriodFilterRow(
                    selectedPeriod = selectedPeriod,
                    onPeriodClick = { showPeriodDialog = true }
                )
            }

            if (availableCategories.size > 1) {
                item {
                    CategoryFilterRow(
                        categories = availableCategories,
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )
                }
            }

            if (groupedTransactions.isEmpty()) {
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

            groupedTransactions.forEach { (date, txList) ->
                item {
                    TransactionDateHeader(date = date)
                }
                items(txList, key = { it.id }) { tx ->
                    TransactionRow(
                        item = tx,
                        onClick = {
                            selectedTransaction = tx
                            selectedPaymentDetail = null
                            selectedPaymentSummary = null
                            detailError = null

                            val userNo = AuthPrefs.getUserNo(context)
                            if (userNo == null || tx.ssafyTransactionId.isBlank()) {
                                isDetailLoading = false
                            } else {
                                isDetailLoading = true
                                coroutineScope.launch {
                                    val matchedPayment = AssetRepository.getPayments(userNo)
                                        .getOrNull()
                                        ?.firstOrNull { payment ->
                                            payment.ssafyTransactionId == tx.ssafyTransactionId
                                        }
                                    selectedPaymentSummary = matchedPayment
                                    val matchedPaymentId = matchedPayment?.paymentId

                                    if (matchedPaymentId != null && matchedPaymentId > 0L) {
                                        AssetRepository.getPaymentDetail(userNo, matchedPaymentId)
                                            .onSuccess { detail ->
                                                selectedPaymentDetail = detail
                                            }
                                            .onFailure { throwable ->
                                                detailError = throwable.message
                                            }
                                    }

                                    isDetailLoading = false
                                }
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

        if (isDetailLoading && selectedTransaction != null) {
            AccountDetailLoadingOverlay(
                onDismiss = {
                    selectedTransaction = null
                    selectedPaymentDetail = null
                    selectedPaymentSummary = null
                    isDetailLoading = false
                    detailError = null
                }
            )
        }

        detailError?.let { message ->
            AccountDetailErrorOverlay(
                message = message,
                onDismiss = {
                    selectedTransaction = null
                    selectedPaymentDetail = null
                    selectedPaymentSummary = null
                    isDetailLoading = false
                    detailError = null
                }
            )
        }

        selectedPaymentDetail?.let { detail ->
            AccountTransactionDetailScreen(
                transaction = selectedTransaction ?: return@let,
                paymentDetail = detail,
                storeName = selectedPaymentSummary?.storeName.orEmpty(),
                bankName = account.bankName,
                accountNumber = account.accountNumber,
                onClose = {
                    selectedTransaction = null
                    selectedPaymentDetail = null
                    selectedPaymentSummary = null
                    isDetailLoading = false
                    detailError = null
                }
            )
        }

        if (!isDetailLoading && detailError == null && selectedPaymentDetail == null) {
            selectedTransaction?.let { transaction ->
                AccountTransactionDetailScreen(
                    transaction = transaction,
                    bankName = account.bankName,
                    accountNumber = account.accountNumber,
                    onClose = {
                        selectedTransaction = null
                        selectedPaymentDetail = null
                        selectedPaymentSummary = null
                        isDetailLoading = false
                        detailError = null
                    }
                )
            }
        }
    }

    if (showPeriodDialog) {
        PeriodPickerDialog(
            selected = selectedPeriod,
            onSelect = {
                selectedPeriod = it
                showPeriodDialog = false
            },
            onDismiss = { showPeriodDialog = false }
        )
    }

    if (false && isDetailLoading) {
        AlertDialog(
            onDismissRequest = {
                selectedTransaction = null
                selectedPaymentDetail = null
                selectedPaymentSummary = null
                isDetailLoading = false
                detailError = null
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

    detailError.takeIf { false }?.let { message ->
        AlertDialog(
            onDismissRequest = {
                selectedTransaction = null
                selectedPaymentDetail = null
                selectedPaymentSummary = null
                isDetailLoading = false
                detailError = null
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTransaction = null
                        selectedPaymentDetail = null
                        selectedPaymentSummary = null
                        isDetailLoading = false
                        detailError = null
                    }
                ) {
                    Text("확인")
                }
            },
            title = { Text("오류") },
            text = { Text(message) }
        )
    }

    selectedPaymentDetail.takeIf { false }?.let { detail ->
        PaymentDetailDialog(
            detail = detail,
            storeName = selectedPaymentSummary?.storeName.orEmpty(),
            onDismiss = {
                selectedTransaction = null
                selectedPaymentDetail = null
                selectedPaymentSummary = null
                isDetailLoading = false
                detailError = null
            }
        )
    }

    if (false && !isDetailLoading && detailError == null && selectedPaymentDetail == null) {
        selectedTransaction?.let { transaction ->
            AccountTransactionDetailDialog(
                transaction = transaction,
                onDismiss = {
                    selectedTransaction = null
                    selectedPaymentDetail = null
                    selectedPaymentSummary = null
                    isDetailLoading = false
                    detailError = null
                }
            )
        }
    }
}

@Composable
private fun AccountDetailHeader(
    bankName: String,
    accountName: String,
    accountNumber: String,
    balance: Long,
    onBack: () -> Unit,
    isSearchMode: Boolean,
    onSearchToggle: () -> Unit,
) {
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
                Text(
                    text = accountNumber.maskAccountNumber(),
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOf(bankName, accountName)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                        .ifBlank { "기본 계좌" },
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
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

@Composable
private fun AccountSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
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
private fun PeriodFilterRow(
    selectedPeriod: String,
    onPeriodClick: () -> Unit,
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
private fun CategoryFilterRow(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 12.dp),
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

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
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
private fun TransactionRow(
    item: TransactionItem,
    onClick: () -> Unit,
) {
    val isDeposit = item.transactionType.equals("DEPOSIT", ignoreCase = true)
    val transactionColor = if (isDeposit) Color(0xFF307CBF) else Color(0xFFF2522E)

    val title = when {
        item.memo.isNotBlank() -> item.memo.replace(Regex("\\s*(페이스페이|카드)\\s*결제$"), "")
        item.counterpart.isNotBlank() && !item.counterpart.all { it.isDigit() } -> item.counterpart
        else -> "계좌 거래"
    }

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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
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
                text = if (item.estimatedPoints > 0L) {
                    "적립 포인트 ${"%,d".format(item.estimatedPoints)}P"
                } else {
                    "거래 후 잔액 ${"%,d".format(item.balanceAfter)}원"
                },
                style = NaedaTypography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = if (isDeposit) "+${"%,d".format(item.amount)}원" else "-${"%,d".format(item.amount)}원",
            style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = transactionColor
        )
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun AccountTransactionDetailDialog(
    transaction: TransactionItem,
    onDismiss: () -> Unit,
) {
    val isDeposit = transaction.transactionType.equals("DEPOSIT", ignoreCase = true)

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
                DetailRow("거래 유형", if (isDeposit) "입금" else "출금")
                DetailRow("거래 금액", "${"%,d".format(transaction.amount)}원")
                DetailRow("거래 시간", transaction.transacted)
                DetailRow("거래 상대", transaction.counterpart.ifBlank { "-" })
                DetailRow("메모", transaction.memo.ifBlank { "-" })
                DetailRow("카테고리", transaction.category.ifBlank { "-" })
                if (transaction.estimatedPoints > 0L) {
                    DetailRow("적립 포인트", "${"%,d".format(transaction.estimatedPoints)}P")
                }
                DetailRow("거래 후 잔액", "${"%,d".format(transaction.balanceAfter)}원")
                DetailRow("거래 번호", transaction.ssafyTransactionId.ifBlank { transaction.id })
            }
        }
    )
}

@Composable
private fun PaymentDetailDialog(
    detail: PaymentDetailResponse,
    storeName: String,
    onDismiss: () -> Unit,
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
    value: String,
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

@Composable
private fun AccountTransactionDetailFullScreen(
    transaction: TransactionItem,
    onDismiss: () -> Unit,
) {
    val isDeposit = transaction.transactionType.equals("DEPOSIT", ignoreCase = true)
    val accentColor = if (isDeposit) Color(0xFF1F8F5F) else Mint900

    AccountDetailFullScreenLayout(
        title = "거래 상세",
        badgeText = if (isDeposit) "입" else "출",
        headlineLabel = if (isDeposit) "입금 상대" else "거래 상대",
        headlineValue = transaction.counterpart.ifBlank { transaction.memo.ifBlank { "거래 정보" } },
        amountText = "${"%,d".format(transaction.amount)}원",
        statusText = if (isDeposit) "입금 완료" else "출금 완료",
        accentColor = accentColor,
        onDismiss = onDismiss
    ) {
        AccountDetailField("거래 유형", if (isDeposit) "입금" else "출금")
        AccountDetailField("거래 시간", transaction.transacted)
        AccountDetailField("메모", transaction.memo.ifBlank { "-" })
        AccountDetailField("카테고리", transaction.category.ifBlank { "-" })
        if (transaction.estimatedPoints > 0L) {
            AccountDetailField("적립 포인트", "${"%,d".format(transaction.estimatedPoints)}P")
        }
        AccountDetailField("거래 후 잔액", "${"%,d".format(transaction.balanceAfter)}원")
        AccountDetailField("거래 번호", transaction.ssafyTransactionId.ifBlank { transaction.id })
    }
}

@Composable
private fun PaymentDetailFullScreen(
    detail: PaymentDetailResponse,
    storeName: String,
    onDismiss: () -> Unit,
) {
    AccountDetailFullScreenLayout(
        title = "결제 상세",
        badgeText = "결",
        headlineLabel = "결제 장소",
        headlineValue = storeName.ifBlank { detail.storeId.toString() },
        amountText = "${"%,d".format(detail.amount)}원",
        statusText = detail.status.toPaymentStatusLabel(),
        accentColor = Mint900,
        onDismiss = onDismiss
    ) {
        AccountDetailField("결제 방식", detail.authMethod.toPaymentMethodLabel())
        AccountDetailField("결제 시간", detail.createdAt?.formatCreatedAt() ?: "-")
        AccountDetailField(
            "적립 포인트",
            detail.earnedPoints?.let { "${"%,d".format(it)}P" } ?: "0P"
        )
        AccountDetailField("결제 번호", detail.ssafyTransactionId ?: detail.paymentId.toString())
        AccountDetailField("결제 장소", storeName.ifBlank { detail.storeId.toString() })
    }
}

@Composable
private fun AccountDetailFullScreenLayout(
    title: String,
    badgeText: String,
    headlineLabel: String,
    headlineValue: String,
    amountText: String,
    statusText: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
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
                Surface(
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = headlineValue,
                            style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Surface(
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = amountText,
                                    style = NaedaTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                AccountDetailStatusChip(
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

                AccountDetailPrimaryButton(
                    text = "닫기",
                    onClick = onDismiss
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AccountDetailStatusChip(
    text: String,
    accentColor: Color,
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
private fun AccountDetailField(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = NaedaTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun AccountDetailPrimaryButton(
    text: String,
    onClick: () -> Unit,
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
private fun AccountDetailLoadingOverlay(
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F2F5)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Mint900)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "상세 정보를 불러오고 있습니다.",
                    style = NaedaTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(onClick = onDismiss) {
                    Text(text = "닫기", color = Mint900)
                }
            }
        }
    }
}

@Composable
private fun AccountDetailErrorOverlay(
    message: String,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F2F5)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "상세 정보를 불러오지 못했습니다.",
                    style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = NaedaTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(onClick = onDismiss) {
                    Text(text = "닫기", color = Mint900)
                }
            }
        }
    }
}

@Composable
private fun PeriodPickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
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

                periodList.forEach { period ->
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

private fun filterTransactionsByPeriod(
    transactions: List<TransactionItem>,
    selectedPeriod: String,
): List<TransactionItem> {
    if (transactions.isEmpty()) {
        return emptyList()
    }

    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    val threshold = when (selectedPeriod) {
        "전체" -> null
        "1주일" -> now.minusWeeks(1)
        "1개월" -> now.minusMonths(1)
        "3개월" -> now.minusMonths(3)
        "6개월" -> now.minusMonths(6)
        else -> null
    }

    if (threshold == null) {
        return transactions
    }

    return transactions.filter { tx ->
        runCatching { LocalDateTime.parse(tx.transacted, formatter) }
            .getOrNull()
            ?.let { !it.isBefore(threshold) }
            ?: true
    }
}

private fun TransactionItem.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val keyword = query.trim().lowercase()

    return listOf(
        id,
        transactionType,
        counterpart,
        memo,
        category,
        amount.toString(),
        balanceAfter.toString(),
        date,
        time
    ).any { it.lowercase().contains(keyword) }
}

private fun String?.toPaymentMethodLabel(): String {
    return when (this?.uppercase()) {
        "FACE" -> "내다페이(페이스페이)"
        "PIN" -> "내다페이(PIN인증)"
        else -> "내다페이"
    }
}

private fun String?.toPaymentStatusLabel(): String {
    return when (this?.uppercase()) {
        "COMPLETED", "SUCCESS", "PAID" -> "결제 완료"
        "CANCELED", "CANCELLED" -> "결제 취소"
        "FAILED" -> "결제 실패"
        else -> "처리 완료"
    }
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

private fun String.maskAccountNumber(): String {
    val digits = replace("-", "").replace(" ", "")
    return when {
        digits.isBlank() -> "-"
        digits.length <= 7 -> this
        else -> "${digits.take(3)}${"*".repeat(digits.length - 7)}${digits.takeLast(4)}"
    }
}
