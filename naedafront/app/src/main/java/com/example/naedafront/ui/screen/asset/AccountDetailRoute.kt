package com.example.naedafront.ui.screen.asset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.AssetAccountResponse
import com.example.naedafront.data.remote.AssetRepository
import com.example.naedafront.data.remote.AssetTransactionResponse
import com.example.naedafront.ui.common.LoadingIndicator
import com.example.naedafront.ui.theme.Background
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private sealed interface AccountDetailUiState {
    data object Loading : AccountDetailUiState
    data class Success(
        val account: AccountItem,
        val balance: Long,
        val transactions: List<TransactionItem>
    ) : AccountDetailUiState
    data class Error(val message: String) : AccountDetailUiState
}

@Composable
fun AccountDetailRoute(
    accountId: String,
    accountNo: String,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userNo = remember(context) { AuthPrefs.getUserNo(context) }
    var reloadTick by remember { mutableIntStateOf(0) }
    var uiState by remember { mutableStateOf<AccountDetailUiState>(AccountDetailUiState.Loading) }
    var hasResumedOnce by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (hasResumedOnce) {
                    reloadTick++
                } else {
                    hasResumedOnce = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(userNo, accountId, accountNo, reloadTick) {
        if (userNo == null) {
            uiState = AccountDetailUiState.Error("로그인 정보가 없어 계좌 상세를 불러올 수 없어요.")
            return@LaunchedEffect
        }

        uiState = AccountDetailUiState.Loading
        uiState = runCatching {
            val detail = AssetRepository.getAccount(userNo, accountNo)
            val resolvedAccountId = accountId.toLongOrNull()?.takeIf { it > 0 } ?: detail.accountId
            val transactions = resolvedAccountId?.let { resolvedId ->
                runCatching { AssetRepository.getTransactions(userNo, resolvedId) }
                    .getOrElse { emptyList() }
            }.orEmpty()
                .sortedByDescending { transaction -> transaction.transacted.orEmpty() }
            val latestBalance = detail.accountBalance ?: transactions.firstOrNull()?.balanceAfter ?: 0L

            AccountDetailUiState.Success(
                account = detail.toDetailUi(resolvedAccountId),
                balance = latestBalance,
                transactions = transactions.mapIndexed { index, transaction ->
                    transaction.toUi(index)
                }
            )
        }.getOrElse { throwable ->
            AccountDetailUiState.Error(throwable.message ?: "계좌 상세 정보를 불러오지 못했습니다.")
        }
    }

    when (val state = uiState) {
        AccountDetailUiState.Loading -> {
            LoadingIndicator(message = "계좌 정보를 불러오는 중...")
        }

        is AccountDetailUiState.Error -> {
            Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0)) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { reloadTick++ }) {
                            Text(text = "다시 시도")
                        }
                    }
                }
            }
        }

        is AccountDetailUiState.Success -> {
            AccountDetailScreen(
                account = state.account,
                balance = state.balance,
                transactions = state.transactions,
                onBack = onBack
            )
        }
    }
}

private fun AssetAccountResponse.toDetailUi(resolvedAccountId: Long?): AccountItem {
    val style = resolveDetailBankStyle(bankCode, bankName)
    return AccountItem(
        id = resolvedAccountId?.toString() ?: (accountNo ?: "account-detail"),
        accountId = resolvedAccountId,
        bankCode = bankCode.orEmpty(),
        bankName = bankName.orEmpty().ifBlank { style.displayName },
        accountName = accountName.orEmpty().ifBlank { bankName.orEmpty().ifBlank { "내 계좌" } },
        accountNumber = accountNo.orEmpty().ifBlank { "계좌번호 없음" },
        accountBalance = accountBalance,
        bankColor = style.color,
        bankInitials = style.initials
    )
}

private fun AssetTransactionResponse.toUi(index: Int): TransactionItem {
    return TransactionItem(
        id = logId?.toString() ?: ssafyTransactionId.orEmpty().ifBlank { "tx-$index" },
        transactionType = transactionType.orEmpty(),
        counterpart = counterpart.orEmpty().ifBlank { "거래처 없음" },
        memo = memo.orEmpty(),
        category = aiCategory.orEmpty().ifBlank {
            category.orEmpty().ifBlank { "기타" }
        },
        amount = amount ?: 0L,
        balanceAfter = balanceAfter ?: 0L,
        ssafyTransactionId = ssafyTransactionId.orEmpty(),
        transacted = formatTransactionDateTime(transacted)
    )
}

private data class DetailBankStyle(
    val color: Color,
    val initials: String,
    val displayName: String
)

private fun resolveDetailBankStyle(bankCode: String?, bankName: String?): DetailBankStyle {
    val code = bankCode.orEmpty()
    val name = bankName.orEmpty()
    return when {
        code == "004" || name.contains("국민") || name.contains("KB") -> DetailBankStyle(Color(0xFFFFB800), "KB", "KB국민은행")
        code == "088" || name.contains("신한") -> DetailBankStyle(Color(0xFF0046FF), "SH", "신한은행")
        code == "090" || name.contains("카카오") -> DetailBankStyle(Color(0xFFFFE400), "KA", "카카오뱅크")
        code == "092" || name.contains("토스") -> DetailBankStyle(Color(0xFF0064FF), "TO", "토스뱅크")
        code == "081" || name.contains("하나") -> DetailBankStyle(Color(0xFF0F9D58), "HN", "하나은행")
        code == "020" || name.contains("우리") -> DetailBankStyle(Color(0xFF1E88E5), "WR", "우리은행")
        code == "011" || name.contains("농협") || name.contains("NH") -> DetailBankStyle(Color(0xFF2E7D32), "NH", "농협은행")
        code == "003" || name.contains("기업") || name.contains("IBK") -> DetailBankStyle(Color(0xFF1565C0), "IB", "IBK기업은행")
        else -> DetailBankStyle(Color(0xFF18A77C), (name.take(2).ifBlank { "BK" }).uppercase(), name.ifBlank { "은행" })
    }
}

private fun formatTransactionDateTime(raw: String?): String {
    if (raw.isNullOrBlank()) {
        return "-"
    }

    return runCatching {
        LocalDateTime.parse(raw).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
    }.getOrElse {
        raw.replace('T', ' ').replace('-', '.').take(16)
    }
}
