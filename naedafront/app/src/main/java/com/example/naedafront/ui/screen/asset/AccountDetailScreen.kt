package com.example.naedafront.ui.screen.asset

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.naedafront.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ─────────────────────────────────────────────
// 샘플 데이터
// ─────────────────────────────────────────────

data class TransactionItem(
    val id: String,                   // log_id
    val transactionType: String,      // transaction_type: DEPOSIT / WITHDRAW / TRANSFER
    val counterpart: String,          // counterpart (상대방, 거래처명)
    val memo: String = "",            // memo (AI 분류 태그)
    val category: String,             // category
    val amount: Long,                 // amount (항상 양수, transactionType으로 방향 결정)
    val balanceAfter: Long,           // balance_after
    val transacted: String,           // transacted → "2026.03.12 14:32" 파싱해서 사용
) {
    val isIncome get() = transactionType == "DEPOSIT"
    val date get() = transacted.take(10)   // "2026.03.12"
    val time get() = if (transacted.length >= 16) transacted.takeLast(5) else ""
}

val sampleTransactions = listOf(
    TransactionItem("1", "WITHDRAW",  "스타벅스 구미 인동점", "카페",      "카페",   6500,       1_243_500, "2026.03.12 14:32"),
    TransactionItem("2", "DEPOSIT",   "급여",               "급여",      "급여",   3_000_000,  1_250_000, "2026.03.10 09:00"),
    TransactionItem("3", "WITHDRAW",  "GS25 구미공단점",     "편의점",    "편의점",  3200,       1_246_800, "2026.03.09 22:10"),
    TransactionItem("4", "TRANSFER",  "카카오페이 송금",      "이체",      "이체",   50_000,     1_246_800, "2026.03.08 18:45"),
    TransactionItem("5", "WITHDRAW",  "구미시 버스",          "교통",      "교통",   1500,       1_296_800, "2026.03.08 08:12"),
    TransactionItem("6", "WITHDRAW",  "이마트 구미점",        "쇼핑",      "쇼핑",   43_200,     1_298_300, "2026.03.07 16:30"),
    TransactionItem("7", "DEPOSIT",   "부모님 송금",          "이체",      "이체",   200_000,    1_341_500, "2026.03.05 11:20"),
    TransactionItem("8", "WITHDRAW",  "올리브영 구미",        "쇼핑",      "쇼핑",   28_900,     1_141_500, "2026.03.04 15:00"),
    TransactionItem("9", "WITHDRAW",  "넷플릭스",             "구독",      "구독",   17_000,     1_170_400, "2026.03.01 00:00"),
    TransactionItem("10", "WITHDRAW", "CGV 구미",            "여가",      "여가",   14_000,     1_187_400, "2026.02.28 19:30"),
)

val categoryList = listOf("전체", "카페", "편의점", "이체", "교통", "쇼핑", "급여", "구독", "여가")

val periodList = listOf("1주일", "1개월", "3개월", "6개월", "직접 설정")

// ─────────────────────────────────────────────
// 메인 화면
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    account: AccountItem = sampleAccounts.first(),
    balance: Long = account.accountBalance ?: 0L,
    transactions: List<TransactionItem> = sampleTransactions,
    onBack: () -> Unit = {}
) {
    var selectedPeriod by remember { mutableStateOf("1개월") }
    var selectedCategory by remember { mutableStateOf("전체") }
    var showPeriodDialog by remember { mutableStateOf(false) }
    val availableCategories = remember(transactions) {
        listOf("전체") + transactions.map { it.category }.filter { it.isNotBlank() }.distinct()
    }

    LaunchedEffect(availableCategories, selectedCategory) {
        if (selectedCategory !in availableCategories) {
            selectedCategory = "전체"
        }
    }

    val periodFiltered = remember(transactions, selectedPeriod) {
        filterTransactionsByPeriod(transactions, selectedPeriod)
    }

    val filtered = periodFiltered.filter { tx ->
        selectedCategory == "전체" || tx.category == selectedCategory
    }

    // 날짜별 그룹핑
    val grouped = filtered.groupBy { it.date }.toSortedMap(reverseOrder())

    Scaffold(containerColor = Background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 민트 헤더 카드 ──────────────────────
            item {
                AccountDetailHeader(
                    account = account,
                    balance = balance,
                    onBack = onBack
                )
            }

            // ── 기간 필터 ───────────────────────────
            item {
                PeriodFilterRow(
                    selectedPeriod = selectedPeriod,
                    onPeriodClick = { showPeriodDialog = true }
                )
            }

            // ── 카테고리 필터 ────────────────────────
            item {
                CategoryFilterRow(
                    categories = availableCategories,
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it }
                )
            }

            // ── 거래 내역 없음 ───────────────────────
            if (grouped.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "거래내역이 없어요",
                            style = NaedaTypography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // ── 날짜별 거래 그룹 ─────────────────────
            grouped.forEach { (date, txList) ->
                item {
                    TransactionDateHeader(date = date)
                }
                items(txList, key = { it.id }) { tx ->
                    TransactionRow(tx = tx)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    // 기간 선택 다이얼로그
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
}

// ─────────────────────────────────────────────
// 민트 헤더 카드 (토스 스타일)
// ─────────────────────────────────────────────

@Composable
private fun AccountDetailHeader(
    account: AccountItem,
    balance: Long,
    onBack: () -> Unit
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
            // 상단 바 (뒤로가기 + 더보기)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "더보기",
                        tint = Color.White
                    )
                }
            }

            // 은행명 + 계좌번호
            Column(
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = account.bankName,
                    style = NaedaTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = account.accountNumber,
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 잔액
                Text(
                    text = "잔액",
                    style = NaedaTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatAmount(balance) + "원",
                    style = NaedaTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

            }
        }
    }
}

// ─────────────────────────────────────────────
// 기간 필터 행
// ─────────────────────────────────────────────

@Composable
private fun PeriodFilterRow(
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
}

// ─────────────────────────────────────────────
// 카테고리 필터 칩 행
// ─────────────────────────────────────────────

@Composable
private fun CategoryFilterRow(
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

// ─────────────────────────────────────────────
// 날짜 헤더
// ─────────────────────────────────────────────

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

// ─────────────────────────────────────────────
// 거래 내역 행
// ─────────────────────────────────────────────

@Composable
private fun TransactionRow(tx: TransactionItem) {
    val isIncome = tx.isIncome
    val amountColor = if (isIncome) Mint700 else OnBackground
    val amountPrefix = if (isIncome) "+" else "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isIncome) Mint50 else SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = if (isIncome) Mint900 else OnSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.counterpart,
                style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = OnBackground,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tx.time,
                style = NaedaTypography.labelSmall,
                color = OnSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix${formatAmount(tx.amount)}원",
                style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = amountColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "잔액 ${formatAmount(tx.balanceAfter)}원",
                style = NaedaTypography.labelSmall,
                color = OnSurfaceVariant
            )
        }
    }

    HorizontalDivider(
        color = OutlineVariant,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

// ─────────────────────────────────────────────
// 기간 선택 다이얼로그
// ─────────────────────────────────────────────

@Composable
private fun PeriodPickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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

// ─────────────────────────────────────────────
// 포맷 유틸
// ─────────────────────────────────────────────

private fun formatAmount(amount: Long): String {
    val abs = Math.abs(amount)
    return "%,d".format(abs)
}

private fun filterTransactionsByPeriod(
    transactions: List<TransactionItem>,
    selectedPeriod: String
): List<TransactionItem> {
    if (transactions.isEmpty()) {
        return emptyList()
    }

    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    val threshold = when (selectedPeriod) {
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
