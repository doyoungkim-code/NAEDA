package com.example.naedafront.ui.screen.asset

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.OnBackground

private val TradePrimary = Color(0xFF00635A)
private val TradeSurfaceTint = Color(0xFFF5FBFA)
private val TradeIncome = Color(0xFF00897B)
private val TradeExpense = Color(0xFF1F2A37)
private val TradeChipBg = Color(0xFFE5F3F1)
private val TradeChipBorder = Color(0xFFB7D9D4)

data class TradeReportUiState(
    val accountName: String = "내다 대표계좌",
    val accountNumber: String = "123-456-789012",
    val balance: Long = 18_240_500L,
    val transactions: List<TradeReportItem> = emptyList()
)

data class TradeReportItem(
    val title: String,
    val subTitle: String,
    val amount: String,
    val isIncome: Boolean,
    val balanceAfter: String,
    val icon: ImageVector,
    val iconBg: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeReportScreen(
    uiState: TradeReportUiState = TradeReportUiState(),
    onBackClick: () -> Unit = {}
) {
    val displayItems = if (uiState.transactions.isEmpty()) {
        listOf(
            TradeReportItem(
                title = "전자 기기 상점",
                subTitle = "03월 18일 오후 2:45",
                amount = "-₩249,000",
                isIncome = false,
                balanceAfter = "잔액 ₩17,991,500",
                icon = Icons.Default.ShoppingBag,
                iconBg = Color(0xFFDCEBFF)
            ),
            TradeReportItem(
                title = "급여 입금",
                subTitle = "03월 17일 오전 9:10",
                amount = "+₩4,250,000",
                isIncome = true,
                balanceAfter = "잔액 ₩18,240,500",
                icon = Icons.Default.AccountBalance,
                iconBg = Color(0xFFDDF5EA)
            ),
            TradeReportItem(
                title = "스타벅스",
                subTitle = "03월 17일 오전 9:12",
                amount = "-₩6,500",
                isIncome = false,
                balanceAfter = "잔액 ₩13,990,500",
                icon = Icons.Default.LocalCafe,
                iconBg = Color(0xFFE3F4EA)
            ),
            TradeReportItem(
                title = "계좌이체",
                subTitle = "03월 16일 오후 6:22",
                amount = "-₩120,000",
                isIncome = false,
                balanceAfter = "잔액 ₩13,870,500",
                icon = Icons.Default.SwapHoriz,
                iconBg = Color(0xFFE7F3F1)
            )
        )
    } else {
        uiState.transactions
    }

    var selectedYear by remember { mutableIntStateOf(2026) }
    var selectedMonth by remember { mutableIntStateOf(3) }

    Scaffold(
        containerColor = Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "거래내역",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = OnBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TradeReportAccountSummaryCard(
                accountName = uiState.accountName,
                accountNumber = uiState.accountNumber,
                balance = uiState.balance
            )

            Spacer(modifier = Modifier.height(18.dp))

            TradeReportDateSection(
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                onYearChange = { selectedYear = it },
                onMonthChange = { selectedMonth = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "${selectedYear}년 ${selectedMonth}월 내역",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = OnBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 24.dp
                )
            ) {
                items(displayItems) { item ->
                    TradeReportRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun TradeReportAccountSummaryCard(
    accountName: String,
    accountNumber: String,
    balance: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = TradePrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = accountNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "현재 잔액",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.76f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "₩${"%,d".format(balance)}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                ),
                color = Color.White
            )
        }
    }
}

@Composable
private fun TradeReportDateSection(
    selectedYear: Int,
    selectedMonth: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit
) {
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    val years = listOf(2026, 2025, 2024)
    val months = (1..12).toList()

    Column(
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Text(
            text = "날짜",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = OnBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TradeSelectableChip(
                label = "${selectedYear}년",
                onClick = { yearExpanded = true }
            ) {
                DropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false }
                ) {
                    years.forEach { year ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${year}년",
                                    color = if (selectedYear == year) TradePrimary else OnBackground
                                )
                            },
                            onClick = {
                                onYearChange(year)
                                yearExpanded = false
                            },
                            colors = MenuDefaults.itemColors()
                        )
                    }
                }
            }

            TradeSelectableChip(
                label = "${selectedMonth}월",
                onClick = { monthExpanded = true }
            ) {
                DropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false }
                ) {
                    months.forEach { month ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${month}월",
                                    color = if (selectedMonth == month) TradePrimary else OnBackground
                                )
                            },
                            onClick = {
                                onMonthChange(month)
                                monthExpanded = false
                            },
                            colors = MenuDefaults.itemColors()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeSelectableChip(
    label: String,
    onClick: () -> Unit,
    menuContent: @Composable () -> Unit
) {
    Box {
        Surface(
            modifier = Modifier.clickable { onClick() },
            shape = RoundedCornerShape(999.dp),
            color = TradeChipBg,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TradePrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TradePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        menuContent()
    }
}

@Composable
private fun TradeReportRow(item: TradeReportItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TradeSurfaceTint)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(item.iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (item.isIncome) TradeIncome else TradeExpense,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = OnBackground
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.subTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnBackground.copy(alpha = 0.5f)
                    )
                }

                Text(
                    text = item.amount,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = if (item.isIncome) TradeIncome else TradeExpense
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(color = TradeChipBorder.copy(alpha = 0.45f))

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "거래 후 잔액",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnBackground.copy(alpha = 0.48f)
                )
                Text(
                    text = item.balanceAfter,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TradePrimary
                )
            }
        }
    }
}