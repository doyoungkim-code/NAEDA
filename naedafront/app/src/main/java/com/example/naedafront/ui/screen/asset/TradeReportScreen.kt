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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.AssetRepository
import com.example.naedafront.data.remote.PaymentResponse
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.OnBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val TradePrimary = Color(0xFF00635A)
private val TradeSurfaceTint = Color(0xFFF5FBFA)
private val TradeIncome = Color(0xFF00897B)
private val TradeExpense = Color(0xFF1F2A37)
private val TradeChipBg = Color(0xFFE5F3F1)
private val TradeChipBorder = Color(0xFFB7D9D4)

data class TradeReportItem(
    val title: String,
    val subTitle: String,
    val amount: String,
    val isIncome: Boolean,
    val balanceAfter: String,
    val balanceLabel: String = "적립 포인트",
    val icon: ImageVector,
    val iconBg: Color
)

data class TradeReportUiState(
    val accountName: String = "",
    val accountNumber: String = "",
    val balance: Long = 0L,
    val transactions: List<TradeReportItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class TradeReportViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TradeReportUiState())
    val uiState: StateFlow<TradeReportUiState> = _uiState.asStateFlow()

    fun loadData(context: Context, year: Int, month: Int) {
        val userNo = AuthPrefs.getUserNo(context) ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            runCatching {
                AssetRepository.getWalletAssets(userNo).accounts.firstOrNull()
            }.onSuccess { account ->
                _uiState.update {
                    it.copy(
                        accountName = account?.accountName ?: "대표계좌",
                        accountNumber = account?.accountNo ?: "",
                        balance = account?.accountBalance ?: 0L
                    )
                }
            }

            val from = buildMonthStart(year, month)
            val to = buildMonthEnd(year, month)

            AssetRepository.getPayments(userNo, from, to)
                .onSuccess { payments ->
                    _uiState.update {
                        it.copy(
                            transactions = payments.map { payment -> payment.toUiItem() },
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
        }
    }
}

private fun buildMonthStart(year: Int, month: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return apiDateTimeFormat().format(calendar.time)
}

private fun buildMonthEnd(year: Int, month: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 0)
    }
    return apiDateTimeFormat().format(calendar.time)
}

private fun apiDateTimeFormat(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA)
}

private fun PaymentResponse.toUiItem(): TradeReportItem {
    val isSuccess = status?.uppercase() in listOf("APPROVED", "SUCCESS", "COMPLETED")
    val pointsText = earnedPoints
        ?.takeIf { it > 0 }
        ?.let { "${"%,d".format(it)}P 적립" }
        ?: "—"

    return TradeReportItem(
        title = when {
            !isSuccess -> "결제 실패"
            authMethod?.uppercase() == "FACE" -> "내다페이 (얼굴인증)"
            authMethod?.uppercase() == "PIN" -> "내다페이 (PIN인증)"
            else -> "내다페이 결제"
        },
        subTitle = createdAt?.formatCreatedAt() ?: "",
        amount = if (isSuccess) "-₩${"%,d".format(amount ?: 0L)}" else "실패",
        isIncome = false,
        balanceAfter = pointsText,
        balanceLabel = "적립 포인트",
        icon = Icons.Default.ShoppingBag,
        iconBg = if (isSuccess) Color(0xFFDCEBFF) else Color(0xFFFFEBEE)
    )
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

private fun TradeReportItem.matches(query: String): Boolean {
    if (query.isBlank()) return true
    val keyword = query.trim().lowercase()

    return listOf(
        title,
        subTitle,
        amount,
        balanceAfter,
        balanceLabel
    ).any { it.lowercase().contains(keyword) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeReportScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: TradeReportViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val currentCalendar = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(currentCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(currentCalendar.get(Calendar.MONTH) + 1) }

    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(selectedYear, selectedMonth) {
        viewModel.loadData(context, selectedYear, selectedMonth)
    }

    val filteredTransactions = remember(uiState.transactions, searchQuery) {
        uiState.transactions.filter { it.matches(searchQuery) }
    }

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
                actions = {
                    IconButton(
                        onClick = {
                            if (isSearchMode) {
                                searchQuery = ""
                                isSearchMode = false
                            } else {
                                isSearchMode = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSearchMode) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchMode) "검색 닫기" else "거래내역 검색",
                            tint = OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Background
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSearchMode) {
                TradeSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            TradeReportAccountSummaryCard(
                accountName = uiState.accountName.ifBlank { "대표계좌" },
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
                text = if (searchQuery.isBlank()) {
                    "${selectedYear}년 ${selectedMonth}월 내역"
                } else {
                    "\"$searchQuery\" 검색 결과 ${filteredTransactions.size}건"
                },
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = OnBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TradePrimary)
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "데이터를 불러오지 못했습니다.\n${uiState.error}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnBackground.copy(alpha = 0.5f)
                        )
                    }
                }

                uiState.transactions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "이 기간에 거래 내역이 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnBackground.copy(alpha = 0.5f)
                        )
                    }
                }

                filteredTransactions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "검색 결과가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnBackground.copy(alpha = 0.5f)
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 24.dp
                        )
                    ) {
                        items(filteredTransactions) { item ->
                            TradeReportRow(item = item)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            .padding(horizontal = 20.dp),
        singleLine = true,
        placeholder = {
            Text(
                text = "결제내역 검색",
                color = OnBackground.copy(alpha = 0.45f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TradePrimary
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "검색어 지우기",
                        tint = OnBackground.copy(alpha = 0.7f)
                    )
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TradePrimary,
            unfocusedBorderColor = TradeChipBorder,
            focusedTextColor = OnBackground,
            unfocusedTextColor = OnBackground,
            cursorColor = TradePrimary,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
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

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val years = remember { (currentYear downTo currentYear - 2).toList() }
    val months = (1..12).toList()

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "날짜",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = OnBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    text = item.balanceLabel,
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