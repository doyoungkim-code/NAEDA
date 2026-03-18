package com.example.naedafront.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val icon: ImageVector
)

data class SpendingCategory(
    val label: String,
    val ratio: Float,
    val color: Color
)

data class NoticeItem(
    val tag: String,
    val tagColor: Color,
    val title: String,
    val date: String
)

data class HomeUiState(
    val userName: String = "사용자",
    val isAccountLinked: Boolean = true,
    val isFaceRegistered: Boolean = false,
    val totalBalance: Long = 18_240_500L,
    val recentTransactions: List<TransactionItem> = emptyList(),
    val spendingCategories: List<SpendingCategory> = emptyList(),
    val topSpendingCategory: String = "식비",
    val topSpendingAmount: Long = 842_500L,
    val notices: List<NoticeItem> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState = HomeUiState(),
    onTransactionClick: () -> Unit = {},
    onFacePaySettingClick: () -> Unit = {},
    onLinkAccountClick: () -> Unit = {},
    onViewAllTransactionsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAlarmClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSecretFaceMatchTestClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            NaedaHomeTopBar(
                onSearchClick = onSearchClick,
                onAlarmClick = onAlarmClick,
                onProfileClick = onProfileClick
            )
        },
        containerColor = Background
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

            if (uiState.isAccountLinked) {
                BalanceCard(
                    totalBalance = uiState.totalBalance,
                    onTransactionClick = onTransactionClick
                )
            } else {
                LinkAccountCard(onLinkAccountClick = onLinkAccountClick)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isFaceRegistered) {
                FacePayBenefitCard(onReRegisterClick = onFacePaySettingClick)
            } else {
                FacePayBannerCard(onFacePaySettingClick = onFacePaySettingClick)
            }

            Spacer(modifier = Modifier.height(16.dp))

            NoticeCard(notices = uiState.notices)

            Spacer(modifier = Modifier.height(16.dp))

            SpendingAnalysisCard(
                topCategory = uiState.topSpendingCategory,
                topAmount = uiState.topSpendingAmount,
                categories = uiState.spendingCategories
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
    onProfileClick: () -> Unit
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
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "검색", tint = OnBackground)
            }
            IconButton(onClick = onAlarmClick) {
                Icon(Icons.Default.Notifications, contentDescription = "알림", tint = OnBackground)
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
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
            text = "안녕하세요, ${userName}님",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
private fun BalanceCard(
    totalBalance: Long,
    onTransactionClick: () -> Unit
) {
    val balanceCardColor = Color(0xFF00635A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = balanceCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "총 잔액",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "₩${"%,d".format(totalBalance)}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.28f)
                )
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
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "웃으며 결제하세요",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = OnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "내다의 혁신적인 얼굴 인식\n결제로 더 안전하고 편리하게.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackground.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onFacePaySettingClick,
                    modifier = Modifier.height(36.dp),
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

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Mint900.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "😊", fontSize = 36.sp)
            }
        }
    }
}

@Composable
private fun FacePayBenefitCard(onReRegisterClick: () -> Unit = {}) {
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
                    text = "내다 페이스페이로 결제하면\n결제 금액의 5%가 포인트로 적립돼요.",
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

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Mint900.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎉", fontSize = 36.sp)
            }
        }
    }
}

@Composable
private fun NoticeCard(notices: List<NoticeItem>) {
    val displayNotices = if (notices.isEmpty()) {
        listOf(
            NoticeItem(
                tag = "축제",
                tagColor = Color(0xFFE91E63),
                title = "2025 구미 낙동강 세계 물 축제",
                date = "03.15 ~ 03.20"
            ),
            NoticeItem(
                tag = "공지",
                tagColor = Color(0xFF1976D2),
                title = "구미시 청년 창업 지원금 신청 안내",
                date = "03.10 마감"
            ),
            NoticeItem(
                tag = "이벤트",
                tagColor = Color(0xFF388E3C),
                title = "구미 사랑 상품권 10% 추가 할인",
                date = "03.01 ~ 03.31"
            )
        )
    } else notices

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
                    modifier = Modifier.clickable { }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            displayNotices.forEachIndexed { index, notice ->
                NoticeRow(notice = notice)
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

@Composable
private fun NoticeRow(notice: NoticeItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
    topCategory: String,
    topAmount: Long,
    categories: List<SpendingCategory>
) {
    val displayCategories = if (categories.isEmpty()) {
        listOf(
            SpendingCategory("식비 45%", 0.45f, Color(0xFFFF6B35)),
            SpendingCategory("쇼핑 25%", 0.25f, Color(0xFF4A90D9)),
            SpendingCategory("교통 20%", 0.20f, Color(0xFF44E3D3)),
            SpendingCategory("기타", 0.10f, Color(0xFFBDBDBD))
        )
    } else categories

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

            SpendingProgressBar(categories = displayCategories)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                displayCategories.forEach { cat ->
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
    val displayItems = if (transactions.isEmpty()) {
        listOf(
            TransactionItem(
                title = "전자 기기 상점",
                subTitle = "오늘 오후 2:45",
                amount = "-₩249,000",
                isIncome = false,
                iconBg = Color(0xFF5C6BC0).copy(alpha = 0.15f),
                icon = Icons.Default.ShoppingBag
            ),
            TransactionItem(
                title = "급여 입금",
                subTitle = "어제",
                amount = "+₩4,250,000",
                isIncome = true,
                iconBg = Success.copy(alpha = 0.12f),
                icon = Icons.Default.AccountBalance
            ),
            TransactionItem(
                title = "스타벅스",
                subTitle = "10월 24일 오전 9:12",
                amount = "-₩6,500",
                isIncome = false,
                iconBg = Color(0xFF00704A).copy(alpha = 0.12f),
                icon = Icons.Default.LocalCafe
            )
        )
    } else transactions

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
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

        displayItems.forEach { item ->
            TransactionRow(item = item)
            Spacer(modifier = Modifier.height(4.dp))
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
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = OnBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subTitle,
                style = MaterialTheme.typography.bodySmall,
                color = OnBackground.copy(alpha = 0.45f)
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenLinkedPreview() {
    NaedaTheme {
        HomeScreen(
            uiState = HomeUiState(isAccountLinked = true)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenUnlinkedPreview() {
    NaedaTheme {
        HomeScreen(
            uiState = HomeUiState(isAccountLinked = false)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenFaceRegisteredPreview() {
    NaedaTheme {
        HomeScreen(
            uiState = HomeUiState(isFaceRegistered = true)
        )
    }
}