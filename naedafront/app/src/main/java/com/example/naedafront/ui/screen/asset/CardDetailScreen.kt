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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.naedafront.ui.theme.*

// ─────────────────────────────────────────────
// 결제 내역 데이터 모델
// ─────────────────────────────────────────────

data class CardTransactionItem(
    val id: String,
    val merchantName: String,       // 가맹점명
    val category: String,
    val amount: Long,               // 항상 양수 (결제 취소는 isCanceled로 구분)
    val isCanceled: Boolean = false,
    val transacted: String,         // "2026.03.12 14:32"
) {
    val date get() = transacted.take(10)
    val time get() = if (transacted.length >= 16) transacted.takeLast(5) else ""
}

val sampleCardTransactions = listOf(
    CardTransactionItem("1",  "스타벅스 구미 인동점",  "카페",   6500,  false, "2026.03.12 14:32"),
    CardTransactionItem("2",  "GS25 구미공단점",       "편의점", 3200,  false, "2026.03.11 22:10"),
    CardTransactionItem("3",  "구미시 버스",            "교통",   1500,  false, "2026.03.10 08:12"),
    CardTransactionItem("4",  "이마트 구미점",          "쇼핑",   43200, false, "2026.03.09 16:30"),
    CardTransactionItem("5",  "올리브영 구미",          "쇼핑",   28900, false, "2026.03.08 15:00"),
    CardTransactionItem("6",  "넷플릭스",               "구독",   17000, false, "2026.03.07 00:00"),
    CardTransactionItem("7",  "CGV 구미",               "여가",   14000, false, "2026.03.06 19:30"),
    CardTransactionItem("8",  "스타벅스 구미 인동점",  "카페",   6500,  true,  "2026.03.05 10:00"),
    CardTransactionItem("9",  "배달의민족",             "배달",   32000, false, "2026.03.04 20:15"),
    CardTransactionItem("10", "쿠팡",                   "쇼핑",   15900, false, "2026.03.03 13:45"),
)

val cardCategoryList = listOf("전체", "카페", "편의점", "교통", "쇼핑", "구독", "여가", "배달")

// ─────────────────────────────────────────────
// 메인 화면
// ─────────────────────────────────────────────

@Composable
fun CardDetailScreen(
    cardId: String = "1",
    transactions: List<CardTransactionItem> = sampleCardTransactions,
    onBack: () -> Unit = {}
) {
    val card = sampleCards.find { it.id == cardId } ?: sampleCards.first()

    var selectedCategory by remember { mutableStateOf("전체") }
    var showPeriodDialog by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf("1개월") }

    val filtered = transactions.filter { tx ->
        selectedCategory == "전체" || tx.category == selectedCategory
    }

    val grouped = filtered.groupBy { it.date }.toSortedMap(reverseOrder())

    // 이번달 결제 금액 (취소 제외)
    val thisMonthTotal = transactions
        .filter { !it.isCanceled && it.date.startsWith("2026.03") }
        .sumOf { it.amount }

    Scaffold(containerColor = Background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 카드 헤더
            item {
                CardDetailHeader(
                    card = card,
                    thisMonthTotal = thisMonthTotal,
                    onBack = onBack
                )
            }

            // ── 기간 필터
            item {
                CardPeriodFilterRow(
                    selectedPeriod = selectedPeriod,
                    onPeriodClick = { showPeriodDialog = true }
                )
            }

            // ── 카테고리 필터
            item {
                CardCategoryFilterRow(
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it }
                )
            }

            // ── 결제 내역 없음
            if (grouped.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "결제내역이 없어요",
                            style = NaedaTypography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // ── 날짜별 결제 그룹
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
                    CardTransactionRow(tx = tx)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showPeriodDialog) {
        CardPeriodPickerDialog(
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
// 카드 헤더 (그라데이션)
// ─────────────────────────────────────────────

@Composable
private fun CardDetailHeader(
    card: CardItem,
    thisMonthTotal: Long,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(card.cardGradientStart, card.cardGradientEnd)
                )
            )
            .padding(bottom = 28.dp)
    ) {
        // 배경 장식
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 220.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // 상단 바
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

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // 카드사명
                Text(
                    text = card.cardIssuerName,
                    style = NaedaTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                // 카드 상품명
                Text(
                    text = card.cardName,
                    style = NaedaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 카드번호
                Text(
                    text = card.cardNumber,
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 이번달 결제금액
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

                // 신용/체크 뱃지
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (card.cardType == "CREDIT") "신용카드" else "체크카드",
                        style = NaedaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 기간 필터 행
// ─────────────────────────────────────────────

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

// ─────────────────────────────────────────────
// 카테고리 필터 칩
// ─────────────────────────────────────────────

@Composable
private fun CardCategoryFilterRow(
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
        items(cardCategoryList) { category ->
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
// 결제 내역 행
// ─────────────────────────────────────────────

@Composable
private fun CardTransactionRow(tx: CardTransactionItem) {
    val amountColor = if (tx.isCanceled) Mint700 else OnBackground
    val amountPrefix = if (tx.isCanceled) "+" else "-"

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
                .background(if (tx.isCanceled) Mint50 else SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (tx.isCanceled) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = if (tx.isCanceled) Mint900 else OnSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tx.merchantName,
                    style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = OnBackground,
                    maxLines = 1
                )
                if (tx.isCanceled) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Mint50)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "취소",
                            style = NaedaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Mint900,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = tx.time,
                style = NaedaTypography.labelSmall,
                color = OnSurfaceVariant
            )
        }

        Text(
            text = "$amountPrefix%,d원".format(tx.amount),
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

// ─────────────────────────────────────────────
// 기간 선택 다이얼로그
// ─────────────────────────────────────────────

@Composable
private fun CardPeriodPickerDialog(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val periods = listOf("1주일", "1개월", "3개월", "6개월", "직접 설정")
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