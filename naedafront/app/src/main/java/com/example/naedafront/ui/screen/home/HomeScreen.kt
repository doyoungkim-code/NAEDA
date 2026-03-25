package com.example.naedafront.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.naedafront.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.AssetAccountResponse
import com.example.naedafront.data.remote.AssetCardResponse
import com.example.naedafront.data.repository.CardRepository
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.KronaOneFontFamily
import com.example.naedafront.ui.theme.Mint100
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTheme
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.Success
import com.example.naedafront.ui.theme.Surface
import kotlinx.coroutines.delay

data class TransactionItem(
    val title: String,
    val subTitle: String,
    val amount: String,
    val isIncome: Boolean,
    val iconBg: Color,
    val icon: ImageVector,
    val badgeText: String? = null
)


data class SpendingCategory(
    val label: String,
    val ratio: Float,
    val color: Color
)

data class NoticeItem(
    val id: Long,
    val type: String, // "notice" or "festival"
    val tag: String,
    val tagColor: Color,
    val title: String,
    val content: String = "",
    val date: String,
    val createdRaw: String = ""
)

data class HomeUiState(
    val userName: String = "사용자",
    val isFaceRegistered: Boolean = false,
    val unreadNotificationCount: Long = 0L,
    val recentTransactions: List<TransactionItem> = emptyList(),
    val spendingCategories: List<SpendingCategory> = emptyList(),
    val topSpendingCategory: String? = null,
    val topSpendingAmount: Long = 0L,
    val spendingInsight: String? = null,
    val notices: List<NoticeItem> = emptyList(),

    val account: AssetAccountResponse? = null,
    val cards: List<AssetCardResponse> = emptyList(),
    val isLoadingAccount: Boolean = true,
    val accountError: String? = null,
    val facePayEnabled: Boolean = false,
    val facePayMethodId: Long? = null,
    val defaultPaymentMethodId: Long? = null,
    val isUpdatingFacePay: Boolean = false
) {
    val isAccountLinked: Boolean get() = account != null

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState = HomeUiState(),
    onTransactionClick: () -> Unit = {},
    onCardTransactionClick: (AssetCardResponse) -> Unit = {},
    onFacePaySettingClick: () -> Unit = {},
    onLinkAccountClick: () -> Unit = {},
    onViewAllTransactionsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAlarmClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSecretFaceMatchTestClick: () -> Unit = {},
    onRegisterCardClick: () -> Unit = {},
    onNoticeItemClick: (NoticeItem) -> Unit = {},
    onNoticeMoreClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var cardPaymentAmounts by remember(uiState.cards) { mutableStateOf<Map<Long, Long>>(emptyMap()) }

    LaunchedEffect(uiState.cards) {
        val userNo = AuthPrefs.getUserNo(context)
        if (userNo == null || uiState.cards.isEmpty()) {
            cardPaymentAmounts = emptyMap()
            return@LaunchedEffect
        }

        val totals = mutableMapOf<Long, Long>()

        uiState.cards.forEach { card ->
            val cardId = card.cardId ?: return@forEach
            val totalAmount = CardRepository.getCardTransactions(
                userNo = userNo,
                cardId = cardId,
                period = "전체"
            ).getOrDefault(emptyList())
                .filter { !it.isCanceled }
                .sumOf { it.amount }

            totals[cardId] = totalAmount
        }

        cardPaymentAmounts = totals
    }

    Scaffold(
        topBar = {
            NaedaHomeTopBar(
                onSearchClick = onSearchClick,
                onAlarmClick = onAlarmClick,
                onProfileClick = onProfileClick,
                unreadNotificationCount = uiState.unreadNotificationCount
            )
        },
        containerColor = Background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            GreetingSection(
                userName = uiState.userName,
                onSecretFaceMatchTestClick = onSecretFaceMatchTestClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoadingAccount -> BalanceCardSkeleton()
                uiState.isAccountLinked -> AssetCardPager(
                    account = uiState.account!!,
                    cards = uiState.cards,
                    cardPaymentAmounts = cardPaymentAmounts,
                    onTransactionClick = onTransactionClick,
                    onCardTransactionClick = onCardTransactionClick,
                    onRegisterCardClick = onRegisterCardClick
                )
                else -> LinkAccountCard(onLinkAccountClick = onLinkAccountClick)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isFaceRegistered) {
                FacePayBenefitCard(
                    onReRegisterClick = onFacePaySettingClick
                )
            } else {
                FacePayBannerCard(onFacePaySettingClick = onFacePaySettingClick)
            }

            Spacer(modifier = Modifier.height(16.dp))

            NoticeCard(
                notices = uiState.notices,
                onItemClick = onNoticeItemClick,
                onMoreClick = onNoticeMoreClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            SpendingAnalysisCard(
                topCategory = uiState.topSpendingCategory,
                topAmount = uiState.topSpendingAmount,
                categories = uiState.spendingCategories,
                insight = uiState.spendingInsight
            )

            Spacer(modifier = Modifier.height(16.dp))

            RecentTransactionsSection(
                transactions = uiState.recentTransactions,
                onViewAllClick = onViewAllTransactionsClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NaedaHomeTopBar(
    onSearchClick: () -> Unit,
    onAlarmClick: () -> Unit,
    onProfileClick: () -> Unit,
    unreadNotificationCount: Long
) {
    TopAppBar(
        title = {
            Text(
                text = "NAEDA",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = KronaOneFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 32.sp,
                    letterSpacing = 1.sp
                ),
                color = Mint900
            )
        },
        actions = {
            Box {
                IconButton(onClick = onAlarmClick) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "알림", tint = OnBackground)
                }
                if (unreadNotificationCount > 0L) {
                    val badgeText = if (unreadNotificationCount > 99L) "99+" else unreadNotificationCount.toString()
                    val badgeSize = when {
                        unreadNotificationCount > 99L -> 22.dp
                        unreadNotificationCount > 9L -> 18.dp
                        else -> 16.dp
                    }
                    val badgeFontSize = when {
                        unreadNotificationCount > 99L -> 6.sp
                        unreadNotificationCount > 9L -> 8.sp
                        else -> 9.sp
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 4.dp)
                            .size(badgeSize)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = badgeFontSize,
                                lineHeight = badgeFontSize,
                                fontWeight = FontWeight.Bold,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
            IconButton(onClick = onProfileClick) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Mint100.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "프로필",
                        tint = Mint900,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        windowInsets = WindowInsets(0)
    )
}

@Composable
private fun GreetingSection(
    userName: String,
    onSecretFaceMatchTestClick: () -> Unit
) {
    var holding by remember { mutableStateOf(false) }

    LaunchedEffect(holding) {
        if (holding) {
            delay(3000)
            if (holding) {
                holding = false
                onSecretFaceMatchTestClick()
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .pointerInput(onSecretFaceMatchTestClick) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            holding = false
                        }
                    }
                )
            }
    ) {
        Text(
            text = "반가워요, ${userName}님 👋",  // 여기만 바꾸면 됨
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}

private val CARD_HEIGHT = 200.dp

@Composable
private fun BalanceCard(
    account: AssetAccountResponse,
    onTransactionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(CARD_HEIGHT)
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF00635A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = account.bankName ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            text = account.accountNo?.maskAccountNo() ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "계좌",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = account.accountName ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "₩${"%,d".format(account.accountBalance ?: 0L)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp
                    ),
                    color = Color.White
                )
            }

            OutlinedButton(
                onClick = onTransactionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
            ) {
                Text(
                    "거래내역",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AssetCardPager(
    account: AssetAccountResponse,
    cards: List<AssetCardResponse>,
    cardPaymentAmounts: Map<Long, Long>,
    onTransactionClick: () -> Unit,
    onCardTransactionClick: (AssetCardResponse) -> Unit,
    onRegisterCardClick: () -> Unit
) {
    // 계좌(1) + 카드(N) + 카드없으면 등록카드(1)
    val pages = mutableListOf<@Composable () -> Unit>()
    pages.add { BalanceCard(account = account, onTransactionClick = onTransactionClick) }

    if (cards.isEmpty()) {
        pages.add { RegisterCardPrompt(onClick = onRegisterCardClick) }
    } else {
        cards.forEach { card ->
            pages.add {
                CardInfoCard(
                    card = card,
                    amount = card.cardId?.let { cardPaymentAmounts[it] } ?: 0L,
                    onTransactionClick = { onCardTransactionClick(card) }
                )
            }
        }
    }

    val actualPageCount = pages.size
    // 순환 스와이프: 충분히 큰 페이지 수로 설정
    val loopPageCount = if (actualPageCount > 1) actualPageCount * 1000 else actualPageCount
    val startPage = if (actualPageCount > 1) (loopPageCount / 2) - ((loopPageCount / 2) % actualPageCount) else 0
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { loopPageCount })

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val actualPage = page % actualPageCount
            pages[actualPage]()
        }

        // 인디케이터 (2페이지 이상일 때만)
        if (actualPageCount > 1) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val currentActualPage = pagerState.currentPage % actualPageCount
                repeat(actualPageCount) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (currentActualPage == index) 8.dp else 6.dp)
                            .background(
                                color = if (currentActualPage == index) Mint900 else Mint900.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

private fun isLightColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.5
}

private fun resolveCardGradient(cardIssuerCode: String?, cardIssuerName: String?): Pair<Color, Color> {
    val code = cardIssuerCode.orEmpty()
    val name = cardIssuerName.orEmpty()
    return when {
        code == "1005" || name.contains("신한") -> Color(0xFF0046FF) to Color(0xFF0088FF)
        code == "1006" || name.contains("삼성") -> Color(0xFF1A1A2E) to Color(0xFF16213E)
        code == "1007" || name.contains("현대") -> Color(0xFF2D2D2D) to Color(0xFF555555)
        code == "1004" || name.contains("국민") || name.contains("KB") -> Color(0xFFFFB800) to Color(0xFFFF8C00)
        code == "1003" || name.contains("롯데") -> Color(0xFFE53935) to Color(0xFFFF7043)
        name.contains("카카오") -> Color(0xFFFFE400) to Color(0xFFFFC000)
        name.contains("하나") -> Color(0xFF0F9D58) to Color(0xFF34A853)
        name.contains("우리") -> Color(0xFF1565C0) to Color(0xFF42A5F5)
        else -> Color(0xFF264653) to Color(0xFF2A9D8F)
    }
}

@Composable
private fun CardInfoCard(
    card: AssetCardResponse,
    amount: Long,
    onTransactionClick: () -> Unit = {}
) {
    val (gradientStart, gradientEnd) = resolveCardGradient(card.cardIssuerCode, card.cardIssuerName)
    val isLight = isLightColor(gradientStart)
    val textPrimary = if (isLight) Color(0xFF1A1A1A) else Color.White
    val textSecondary = if (isLight) Color(0xFF1A1A1A).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.65f)
    val textTertiary = if (isLight) Color(0xFF1A1A1A).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.45f)
    val textBody = if (isLight) Color(0xFF1A1A1A).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
    val badgeBg = if (isLight) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)
    val decoColor = if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)
    val decoColor2 = if (isLight) Color.Black.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.04f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CARD_HEIGHT)
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(gradientStart, gradientEnd)
                )
            )
    ) {
        // 배경 원형 장식
        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(decoColor)
        )
        Box(
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(decoColor2)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = card.cardIssuerName ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = card.cardNo?.maskCardNumber() ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = textTertiary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (card.cardType?.uppercase() == "CREDIT") "신용" else "체크",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary
                        )
                    }
                }
            }

            // 중단: 카드 상품명
            Text(
                text = card.cardName ?: "등록 카드",
                style = MaterialTheme.typography.bodySmall,
                color = textBody,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "₩${"%,d".format(amount)}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 30.sp
                    ),
                    color = textPrimary
                )

                Text(
                    text = "유효기간 ${card.cardExpiryDate ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // 하단: 거래내역 버튼
            Column {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onTransactionClick,
                    enabled = card.cardId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = textPrimary,
                        disabledContainerColor = Color.White.copy(alpha = 0.05f),
                        disabledContentColor = textPrimary.copy(alpha = 0.45f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                ) {
                    Text(
                        "거래내역",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (card.cardId != null) textPrimary else textPrimary.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisterCardPrompt(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(CARD_HEIGHT)
            .padding(horizontal = 20.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Mint900.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "카드 등록",
                tint = Mint900,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "카드 등록하러 가기",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Mint900
            )
        }
    }
}

@Composable
private fun BalanceCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF00635A).copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {}
}

@Composable
private fun LinkAccountCard(onLinkAccountClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Mint100.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = Mint900,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "연결된 계좌가 없어요",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = OnBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "계좌를 연결하면 잔액과 거래내역을\n한눈에 확인할 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = OnBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLinkAccountClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint900)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "계좌 등록하기",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun FacePayBannerCard(onFacePaySettingClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Mint900.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "얼굴 등록 한 번으로 결제 끝!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = OnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "카드 없이도 어디서든 빠르게.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackground.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onFacePaySettingClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint900)
                ) {
                    Text(
                        "페이스 페이 설정",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Image(
                painter = painterResource(id = R.drawable.face_before),
                contentDescription = "페이스페이 등록 전",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    }
}

@Composable
private fun FacePayBenefitCard(
    onReRegisterClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Mint900.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Mint900),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "페이스페이 등록 완료",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Mint900
                        )
                    }

                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "페이스페이 결제 시 5% 적립",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = OnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "얼굴 등록이 완료되었어요. 필요한 경우 아래에서 얼굴 정보를 다시 등록할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackground.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "얼굴 재등록",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Mint900.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { onReRegisterClick() }
                )
            }

            Image(
                painter = painterResource(id = R.drawable.face_after),
                contentDescription = "페이스페이 등록 완료",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
    }
}

@Composable
private fun NoticeCard(
    notices: List<NoticeItem>,
    onItemClick: (NoticeItem) -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    val displayNotices = notices

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Mint900,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "구미시 소식",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = OnBackground
                    )
                }
                Text(
                    text = "더보기",
                    style = MaterialTheme.typography.labelMedium,
                    color = Mint900,
                    modifier = Modifier.clickable { onMoreClick() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (displayNotices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "공지사항이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                displayNotices.forEachIndexed { index, notice ->
                    NoticeRow(notice = notice, onClick = { onItemClick(notice) })
                    if (index < displayNotices.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = OnBackground.copy(alpha = 0.06f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeRow(notice: NoticeItem, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(notice.tagColor.copy(alpha = 0.12f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = notice.tag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = notice.tagColor
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = notice.title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = OnBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = notice.date,
            style = MaterialTheme.typography.labelSmall,
            color = OnBackground.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun SpendingAnalysisCard(
    topCategory: String?,
    topAmount: Long,
    categories: List<SpendingCategory>,
    insight: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "이번 달 소비 분석",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = OnBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (categories.isEmpty() || topCategory.isNullOrBlank() || topAmount <= 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = insight ?: "이번 달 결제 기록이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = OnBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF6B35).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍽️", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${topCategory}에 가장 많이 썼어요",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBackground.copy(alpha = 0.55f)
                        )
                        Text(
                            text = "₩${"%,d".format(topAmount)}원",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = OnBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SpendingProgressBar(categories = categories)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    categories.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(cat.color)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = OnBackground.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (!insight.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Mint100.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = insight,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp
                            ),
                            color = OnBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingProgressBar(categories: List<SpendingCategory>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        categories.forEachIndexed { index, cat ->
            val shape = when {
                index == 0 -> RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                index == categories.lastIndex -> RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                else -> RoundedCornerShape(0.dp)
            }
            Box(
                modifier = Modifier
                    .weight(cat.ratio)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(cat.color)
            )
        }
    }
}

@Composable
private fun RecentTransactionsSection(
    transactions: List<TransactionItem>,
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "최근 거래 내역",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = OnBackground
                )
                Text(
                    text = "전체보기",
                    style = MaterialTheme.typography.labelMedium,
                    color = Mint900,
                    modifier = Modifier.clickable { onViewAllClick() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "최근 거래 내역이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                transactions.forEach { item ->
                    TransactionRow(item = item)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
@Composable
private fun TransactionRow(item: TransactionItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(item.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = if (item.isIncome) Success else OnBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = OnBackground,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.badgeText?.let { badgeText ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Mint100)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Mint900
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subTitle,
                style = MaterialTheme.typography.bodySmall,
                color = OnBackground.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = item.amount,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (item.isIncome) Success else OnBackground
        )
    }
}

private fun String.maskAccountNo(): String {
    val digits = replace("-", "").replace(" ", "")
    return when {
        digits.isBlank() -> "-"
        digits.length <= 7 -> this
        else -> "${digits.take(3)}${"*".repeat(digits.length - 7)}${digits.takeLast(4)}"
    }
}

private fun String.maskCardNumber(): String {
    val digits = replace("-", "").replace(" ", "")
    return if (digits.length >= 16) {
        "${digits.substring(0, 4)}-****-****-${digits.takeLast(4)}"
    } else if (isBlank()) {
        "-"
    } else {
        this
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenLinkedPreview() {
    NaedaTheme {
        HomeScreen(
            uiState = HomeUiState(
                account = AssetAccountResponse(
                    accountId = 1L,
                    bankCode = "088",
                    bankName = "신한은행",
                    accountNo = "110-123-456789",
                    accountName = "입출금통장",
                    accountBalance = 18_240_500L,
                    currency = "KRW"
                )
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenUnlinkedPreview() {
    NaedaTheme {
        HomeScreen(uiState = HomeUiState(account = null))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenFaceRegisteredPreview() {
    NaedaTheme {
        HomeScreen(
            uiState = HomeUiState(
                isFaceRegistered = true,
                account = AssetAccountResponse(
                    accountId = 1L,
                    bankCode = "088",
                    bankName = "신한은행",
                    accountNo = "110-123-456789",
                    accountName = "입출금통장",
                    accountBalance = 18_240_500L,
                    currency = "KRW"
                )
            )
        )
    }
}
