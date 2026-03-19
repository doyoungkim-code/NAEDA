package com.example.naedafront.ui.screen.asset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.AssetAccountResponse
import com.example.naedafront.data.remote.AssetCardResponse
import com.example.naedafront.data.remote.AssetRepository
import com.example.naedafront.ui.common.LoadingIndicator
import com.example.naedafront.ui.theme.Background

private sealed interface AssetListUiState {
    data object Loading : AssetListUiState
    data class Success(
        val accounts: List<AccountItem>,
        val cards: List<CardItem>
    ) : AssetListUiState
    data class Error(val message: String) : AssetListUiState
}

@Composable
fun AccountListRoute(
    initialTab: Int = 0,
    onBack: () -> Unit = {},
    onRegisterNewAccount: () -> Unit = {},
    onRegisterNewCard: () -> Unit = {},
    onAccountClick: (AccountItem) -> Unit = {},
    onCardClick: (CardItem) -> Unit = {},
    onDeleteAccount: (AccountItem) -> Unit = {},
    onSetPrimary: (AccountItem) -> Unit = {},
    onSetPrimaryCard: (CardItem) -> Unit = {},
    onDeleteCard: (CardItem) -> Unit = {}
) {
    val context = LocalContext.current
    val userNo = remember(context) { AuthPrefs.getUserNo(context) }
    var reloadTick by remember { mutableIntStateOf(0) }
    var uiState by remember { mutableStateOf<AssetListUiState>(AssetListUiState.Loading) }

    LaunchedEffect(userNo, reloadTick) {
        if (userNo == null) {
            uiState = AssetListUiState.Error("로그인 정보가 없어 자산 목록을 불러올 수 없어요.")
            return@LaunchedEffect
        }

        uiState = AssetListUiState.Loading
        uiState = runCatching {
            val assets = AssetRepository.getWalletAssets(userNo)
            AssetListUiState.Success(
                accounts = assets.accounts.mapIndexed { index, account -> account.toUi(index) },
                cards = assets.cards
                    .filter { it.isActive != false }
                    .mapIndexed { index, card -> card.toUi(index) }
            )
        }.getOrElse { throwable ->
            AssetListUiState.Error(throwable.message ?: "계좌와 카드 정보를 불러오지 못했습니다.")
        }
    }

    when (val state = uiState) {
        AssetListUiState.Loading -> {
            LoadingIndicator(message = "계좌와 카드를 불러오는 중...")
        }

        is AssetListUiState.Error -> {
            Scaffold(containerColor = Background) { innerPadding ->
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

        is AssetListUiState.Success -> {
            AccountListScreen(
                initialTab = initialTab,
                accounts = state.accounts,
                cards = state.cards,
                onBack = onBack,
                onRegisterNewAccount = onRegisterNewAccount,
                onRegisterNewCard = onRegisterNewCard,
                onAccountClick = onAccountClick,
                onCardClick = onCardClick,
                onDeleteAccount = onDeleteAccount,
                onSetPrimary = onSetPrimary,
                onSetPrimaryCard = onSetPrimaryCard,
                onDeleteCard = onDeleteCard
            )
        }
    }
}

private fun AssetAccountResponse.toUi(index: Int): AccountItem {
    val style = resolveBankStyle(bankCode, bankName)
    val resolvedAccountNumber = accountNo.orEmpty().ifBlank { "계좌번호 없음" }
    val resolvedAccountName = accountName.orEmpty().ifBlank { bankName.orEmpty().ifBlank { "내 계좌" } }

    return AccountItem(
        id = accountId?.toString() ?: resolvedAccountNumber.ifBlank { "account-$index" },
        bankCode = bankCode.orEmpty(),
        bankName = bankName.orEmpty().ifBlank { style.displayName },
        accountName = resolvedAccountName,
        accountNumber = resolvedAccountNumber,
        isPrimary = index == 0,
        bankColor = style.color,
        bankInitials = style.initials
    )
}

private fun AssetCardResponse.toUi(index: Int): CardItem {
    val style = resolveCardStyle(cardIssuerCode, cardIssuerName)
    return CardItem(
        id = cardId?.toString() ?: cardUniqueNo.orEmpty().ifBlank { "card-$index" },
        cardType = cardType.orEmpty().uppercase().ifBlank { "DEBIT" },
        cardIssuerName = cardIssuerName.orEmpty().ifBlank { style.displayName },
        cardName = cardName.orEmpty().ifBlank { "등록 카드" },
        cardNumber = formatCardNumber(cardNo),
        cardExpiryDate = formatExpiryDate(cardExpiryDate),
        isPrimary = index == 0,
        isActive = isActive != false,
        cardGradientStart = style.start,
        cardGradientEnd = style.end
    )
}

private data class BankStyle(
    val color: Color,
    val initials: String,
    val displayName: String
)

private fun resolveBankStyle(bankCode: String?, bankName: String?): BankStyle {
    val code = bankCode.orEmpty()
    val name = bankName.orEmpty()
    return when {
        code == "004" || name.contains("국민") || name.contains("KB") -> BankStyle(Color(0xFFFFB800), "KB", "KB국민은행")
        code == "088" || name.contains("신한") -> BankStyle(Color(0xFF0046FF), "SH", "신한은행")
        code == "090" || name.contains("카카오") -> BankStyle(Color(0xFFFFE400), "KA", "카카오뱅크")
        code == "092" || name.contains("토스") -> BankStyle(Color(0xFF0064FF), "TO", "토스뱅크")
        code == "081" || name.contains("하나") -> BankStyle(Color(0xFF0F9D58), "HN", "하나은행")
        code == "020" || name.contains("우리") -> BankStyle(Color(0xFF1E88E5), "WR", "우리은행")
        code == "011" || name.contains("농협") || name.contains("NH") -> BankStyle(Color(0xFF2E7D32), "NH", "농협은행")
        code == "003" || name.contains("기업") || name.contains("IBK") -> BankStyle(Color(0xFF1565C0), "IB", "IBK기업은행")
        else -> BankStyle(Color(0xFF18A77C), (name.take(2).ifBlank { "BK" }).uppercase(), name.ifBlank { "은행" })
    }
}

private data class CardStyle(
    val start: Color,
    val end: Color,
    val displayName: String
)

private fun resolveCardStyle(cardIssuerCode: String?, cardIssuerName: String?): CardStyle {
    val code = cardIssuerCode.orEmpty()
    val name = cardIssuerName.orEmpty()
    return when {
        code == "1005" || name.contains("신한") -> CardStyle(Color(0xFF0046FF), Color(0xFF0088FF), "신한카드")
        code == "1006" || name.contains("삼성") -> CardStyle(Color(0xFF1A1A2E), Color(0xFF16213E), "삼성카드")
        code == "1007" || name.contains("현대") -> CardStyle(Color(0xFF2D2D2D), Color(0xFF555555), "현대카드")
        code == "1004" || name.contains("국민") || name.contains("KB") -> CardStyle(Color(0xFFFFB800), Color(0xFFFF8C00), "KB국민카드")
        code == "1003" || name.contains("롯데") -> CardStyle(Color(0xFFE53935), Color(0xFFFF7043), "롯데카드")
        name.contains("카카오") -> CardStyle(Color(0xFFFFE400), Color(0xFFFFC000), "카카오뱅크")
        name.contains("하나") -> CardStyle(Color(0xFF0F9D58), Color(0xFF34A853), "하나카드")
        name.contains("우리") -> CardStyle(Color(0xFF1565C0), Color(0xFF42A5F5), "우리카드")
        else -> CardStyle(Color(0xFF264653), Color(0xFF2A9D8F), name.ifBlank { "등록 카드" })
    }
}

private fun formatCardNumber(raw: String?): String {
    val value = raw.orEmpty().trim()
    if (value.isBlank()) return "카드번호 없음"

    val normalized = value.replace("-", "")
    return if (normalized.length == 12 && normalized.contains("****")) {
        val first = normalized.take(4)
        val last = normalized.takeLast(4)
        "$first-****-****-$last"
    } else if (normalized.length >= 16 && normalized.all { it.isDigit() || it == '*' }) {
        normalized.chunked(4).joinToString("-")
    } else {
        value
    }
}

private fun formatExpiryDate(raw: String?): String {
    val value = raw.orEmpty().trim()
    if (value.length == 8 && value.all { it.isDigit() }) {
        return value.substring(2, 4) + "/" + value.substring(4, 6)
    }
    return value.ifBlank { "-" }
}
