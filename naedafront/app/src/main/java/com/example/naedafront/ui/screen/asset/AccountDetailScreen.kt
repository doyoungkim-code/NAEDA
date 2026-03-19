package com.example.naedafront.ui.screen.asset

import android.content.Context
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
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.naedafront.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// ─────────────────────────────────────────────
// UiState
// ─────────────────────────────────────────────

data class AccountDetailUiState(
    val accountName: String = "",
    val accountNumber: String = "",
    val bankName: String = "",
    val balance: Long = 0L,
    val transactions: List<PaymentUiItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class PaymentUiItem(
    val paymentId: Long,
    val title: String,
    val date: String,
    val time: String,
    val amount: Long,
    val isSuccess: Boolean,
    val earnedPoints: Long,
    val status: String
)

val periodList = listOf("1주일", "1개월", "3개월", "직접 설정")

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

class AccountDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AccountDetailUiState())
    val uiState: StateFlow<AccountDetailUiState> = _uiState.asStateFlow()

    fun loadData(context: Context, accountId: String) {
        val userNo = AuthPrefs.getUserNo(context) ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 계좌 정보
            runCatching {
                AssetRepository.getWalletAssets(userNo).accounts
                    .find { it.accountId?.toString() == accountId }
                    ?: AssetRepository.getWalletAssets(userNo).accounts.firstOrNull()
            }.onSuccess { account ->
                _uiState.update {
                    it.copy(
                        accountName = account?.accountName ?: "",
                        accountNumber = account?.accountNo ?: "",
                        bankName = account?.bankName ?: "",
                        balance = account?.accountBalance ?: 0L
                    )
                }
            }

            // 결제 내역
            AssetRepository.getPayments(userNo)
                .onSuccess { payments ->
                    _uiState.update {
                        it.copy(
                            transactions = payments.map { it.toUiItem() },
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message)
                    }
                }
        }
    }
}

// ─────────────────────────────────────────────
// PaymentResponse → PaymentUiItem 변환
// ─────────────────────────────────────────────

private fun PaymentResponse.toUiItem(): PaymentUiItem {
    val isSuccess = status?.uppercase() in listOf("APPROVED", "SUCCESS", "COMPLETED")
    val parsed = createdAt?.parseDateTime()

    return PaymentUiItem(
        paymentId = paymentId ?: 0L,
        title = when {
            !isSuccess -> "결제 실패"
            authMethod?.uppercase() == "FACE" -> "내다페이 (얼굴인증)"
            authMethod?.uppercase() == "PIN" -> "내다페이 (PIN인증)"
            else -> "내다페이 결제"
        },
        date = parsed?.first ?: "",
        time = parsed?.second ?: "",
        amount = amount ?: 0L,
        isSuccess = isSuccess,
        earnedPoints = earnedPoints ?: 0L,
        status = status ?: ""
    )
}

private fun String.parseDateTime(): Pair<String, String>? {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = inputFormat.parse(this) ?: return null
        val cal = Calendar.getInstance().apply { time = date }

        val year = cal.get(Calendar.YEAR)
        val month = "%02d".format(cal.get(Calendar.MONTH) + 1)
        val day = "%02d".format(cal.get(Calendar.DAY_OF_MONTH))
        val hour = "%02d".format(cal.get(Calendar.HOUR_OF_DAY))
        val minute = "%02d".format(cal.get(Calendar.MINUTE))

        Pair("$year.$month.$day", "$hour:$minute")
    } catch (e: Exception) {
        null
    }
}

// ─────────────────────────────────────────────
// 메인 화면
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String = "",
    onBack: () -> Unit = {},
    onTransferClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: AccountDetailViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var selectedPeriod by remember { mutableStateOf("1개월") }
    var showPeriodDialog by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        viewModel.loadData(context, accountId)
    }

    // 날짜별 그룹핑
    val grouped = uiState.transactions
        .groupBy { it.date }
        .toSortedMap(reverseOrder())

    Scaffold(containerColor = Background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 헤더
            item {
                AccountDetailHeader(
                    bankName = uiState.bankName,
                    accountName = uiState.accountName,
                    accountNumber = uiState.accountNumber,
                    balance = uiState.balance,
                    onBack = onBack,
                    onTransferClick = onTransferClick
                )
            }

            // 기간 필터
            item {
                PeriodFilterRow(
                    selectedPeriod = selectedPeriod,
                    onPeriodClick = { showPeriodDialog = true }
                )
            }

            // 로딩
            if (uiState.isLoading) {
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

            // 에러
            if (uiState.error != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "데이터를 불러오지 못했습니다.",
                            style = NaedaTypography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // 거래 없음
            if (!uiState.isLoading && uiState.error == null && grouped.isEmpty()) {
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

            // 날짜별 거래 그룹
            grouped.forEach { (date, txList) ->
                item {
                    TransactionDateHeader(date = date)
                }
                items(txList, key = { it.paymentId }) { tx ->
                    PaymentRow(item = tx)
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

// ─────────────────────────────────────────────
// 헤더 카드
// ─────────────────────────────────────────────

@Composable
private fun AccountDetailHeader(
    bankName: String,
    accountName: String,
    accountNumber: String,
    balance: Long,
    onBack: () -> Unit,
    onTransferClick: () -> Unit
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
                Text(
                    text = bankName,
                    style = NaedaTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = accountNumber,
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = accountName,
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "잔액",
                    style = NaedaTypography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%,d".format(balance) + "원",
                    style = NaedaTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onTransferClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = "이체하기",
                        style = NaedaTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
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
// 결제 내역 행
// ─────────────────────────────────────────────

@Composable
private fun PaymentRow(item: PaymentUiItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (item.isSuccess) Color(0xFFDCEBFF) else Color(0xFFFFEBEE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = if (item.isSuccess) Mint900 else OnSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = OnBackground,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.time,
                style = NaedaTypography.labelSmall,
                color = OnSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (item.isSuccess) "-${"%,d".format(item.amount)}원" else "실패",
                style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (item.isSuccess) OnBackground else Color(0xFFE53935)
            )
            if (item.earnedPoints > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "+${"%,d".format(item.earnedPoints)}P",
                    style = NaedaTypography.labelSmall,
                    color = Mint900
                )
            }
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