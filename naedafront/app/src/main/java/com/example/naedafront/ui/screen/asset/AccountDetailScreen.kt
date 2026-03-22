package com.example.naedafront.ui.screen.asset

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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import com.example.naedafront.ui.theme.OutlineVariant
import com.example.naedafront.ui.theme.Surface as SurfaceColor
import com.example.naedafront.ui.theme.SurfaceVariant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class TransactionItem(
    val id: String,
    val transactionType: String,
    val counterpart: String,
    val memo: String,
    val category: String,
    val amount: Long,
    val balanceAfter: Long,
    val transacted: String,
) {
    val date: String get() = transacted.take(10)
    val time: String get() = if (transacted.length >= 16) transacted.takeLast(5) else ""
}

private val periodList = listOf("1주일", "1개월", "3개월", "6개월", "직접 설정")

@Composable
fun AccountDetailScreen(
    account: AccountItem,
    balance: Long,
    transactions: List<TransactionItem>,
    onBack: () -> Unit = {},
) {
    var selectedPeriod by rememberSaveable { mutableStateOf("1개월") }
    var selectedCategory by rememberSaveable { mutableStateOf("전체") }
    var showPeriodDialog by remember { mutableStateOf(false) }

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
    val filteredTransactions = remember(periodFiltered, selectedCategory) {
        if (selectedCategory == "전체") {
            periodFiltered
        } else {
            periodFiltered.filter { it.category == selectedCategory }
        }
    }
    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.date }.toSortedMap(reverseOrder())
    }

    Scaffold(containerColor = Background, contentWindowInsets = WindowInsets(0)) { innerPadding ->
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
                    onBack = onBack
                )
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
                            text = "거래내역이 없어요",
                            style = NaedaTypography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            groupedTransactions.forEach { (date, txList) ->
                item {
                    TransactionDateHeader(date = date)
                }
                items(txList, key = { it.id }) { tx ->
                    TransactionRow(item = tx)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
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
}

@Composable
private fun AccountDetailHeader(
    bankName: String,
    accountName: String,
    accountNumber: String,
    balance: Long,
    onBack: () -> Unit,
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
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "더보기",
                        tint = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = bankName,
                    style = NaedaTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = accountNumber,
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = accountName,
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "잔액",
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
private fun PeriodFilterRow(
    selectedPeriod: String,
    onPeriodClick: () -> Unit,
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
private fun TransactionRow(item: TransactionItem) {
    val isDeposit = item.transactionType.equals("DEPOSIT", ignoreCase = true)
    val title = item.counterpart.ifBlank {
        item.memo.ifBlank { item.category.ifBlank { "거래내역" } }
    }
    val subtitle = listOfNotNull(
        item.category.takeIf { it.isNotBlank() },
        item.time.takeIf { it.isNotBlank() },
        item.memo.takeIf { it.isNotBlank() && it != title }
    ).joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isDeposit) Color(0xFFDFF7E8) else Color(0xFFDCEBFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDeposit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = if (isDeposit) Color(0xFF1F8F5F) else Mint900,
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
                text = "잔액 ${"%,d".format(item.balanceAfter)}원",
                style = NaedaTypography.labelSmall,
                color = OnSurfaceVariant
            )
        }

        Text(
            text = if (isDeposit) "+${"%,d".format(item.amount)}원" else "-${"%,d".format(item.amount)}원",
            style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isDeposit) Color(0xFF1F8F5F) else OnBackground
        )
    }

    HorizontalDivider(
        color = OutlineVariant,
        thickness = 0.5.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
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