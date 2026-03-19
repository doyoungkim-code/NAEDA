package com.example.naedafront.ui.screen.asset

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Error
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import com.example.naedafront.ui.theme.Outline
import com.example.naedafront.ui.theme.Surface

// ─────────────────────────────────────────────
// 데이터 모델
// ─────────────────────────────────────────────

data class AccountItem(
    val id: String,           // account_id
    val bankCode: String,     // bank_code (SSAFY 은행코드)
    val bankName: String,     // bank_name
    val accountName: String,  // account_name (SSAFY 계좌명 = 자산 별칭)
    val accountNumber: String,// account_no
    val isPrimary: Boolean = false,
    // UI 전용 (서버에서 bankCode 기반으로 결정)
    val bankColor: Color,
    val bankInitials: String
)

// ─────────────────────────────────────────────
// 카드 데이터 모델
// ─────────────────────────────────────────────

data class CardItem(
    val id: String,               // debit/credit card id
    val cardType: String,         // "CREDIT" or "DEBIT"
    val cardIssuerName: String,   // card_issuer_name
    val cardName: String,         // card_name (상품명)
    val cardNumber: String,       // card_no (마스킹 표시용)
    val cardExpiryDate: String,   // card_expiry_date
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
    val cardGradientStart: Color,
    val cardGradientEnd: Color
)

val sampleCards = listOf(
    CardItem(
        id = "1",
        cardType = "CREDIT",
        cardIssuerName = "신한카드",
        cardName = "신한 Deep Dream 카드",
        cardNumber = "1234-****-****-5678",
        cardExpiryDate = "26/08",
        isPrimary = true,
        cardGradientStart = Color(0xFF0046FF),
        cardGradientEnd = Color(0xFF0088FF)
    ),
    CardItem(
        id = "2",
        cardType = "DEBIT",
        cardIssuerName = "삼성카드",
        cardName = "삼성 taptap O카드",
        cardNumber = "9876-****-****-4321",
        cardExpiryDate = "25/12",
        cardGradientStart = Color(0xFF1A1A2E),
        cardGradientEnd = Color(0xFF16213E)
    ),
    CardItem(
        id = "3",
        cardType = "CREDIT",
        cardIssuerName = "현대카드",
        cardName = "현대카드 ZERO Edition3",
        cardNumber = "5555-****-****-1111",
        cardExpiryDate = "27/03",
        cardGradientStart = Color(0xFF2D2D2D),
        cardGradientEnd = Color(0xFF555555)
    ),
    CardItem(
        id = "4",
        cardType = "CREDIT",
        cardIssuerName = "KB국민카드",
        cardName = "KB 청춘대로 톡톡카드",
        cardNumber = "4444-****-****-2222",
        cardExpiryDate = "26/05",
        cardGradientStart = Color(0xFFFFB800),
        cardGradientEnd = Color(0xFFFF8C00)
    ),
    CardItem(
        id = "5",
        cardType = "DEBIT",
        cardIssuerName = "카카오뱅크",
        cardName = "카카오뱅크 체크카드",
        cardNumber = "3333-****-****-9999",
        cardExpiryDate = "28/01",
        cardGradientStart = Color(0xFFFFE400),
        cardGradientEnd = Color(0xFFFFC000)
    )
)

val sampleAccounts = listOf(
    AccountItem(
        id = "1",
        bankCode = "004",
        bankName = "KB국민은행",
        accountName = "생활비 통장",
        accountNumber = "123-45-67890",
        isPrimary = true,
        bankColor = Color(0xFFFFB800),
        bankInitials = "KB"
    ),
    AccountItem(
        id = "2",
        bankCode = "088",
        bankName = "신한은행",
        accountName = "신한 카드",
        accountNumber = "987-65-43210",
        bankColor = Color(0xFF0046FF),
        bankInitials = "SH"
    ),
    AccountItem(
        id = "3",
        bankCode = "090",
        bankName = "카카오뱅크",
        accountName = "입출금통장",
        accountNumber = "3333-01-23456",
        bankColor = Color(0xFFFFE400),
        bankInitials = "KA"
    ),
    AccountItem(
        id = "4",
        bankCode = "092",
        bankName = "토스뱅크",
        accountName = "토스뱅크 통장",
        accountNumber = "1000-432-1234",
        bankColor = Color(0xFF0064FF),
        bankInitials = "TO"
    )
)

// ─────────────────────────────────────────────
// 메인 화면
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    initialTab: Int = 0,
    accounts: List<AccountItem> = sampleAccounts,
    cards: List<CardItem> = sampleCards,
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
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }
    val tabs = listOf("계좌", "카드")
    var expandedMenuId by remember { mutableStateOf<String?>(null) }
    val accountIdsKey = remember(accounts) { accounts.joinToString("|") { it.id } }
    var selectedPrimaryAccountId by rememberSaveable(accountIdsKey) { mutableStateOf<String?>(null) }
    val displayedAccounts = remember(accounts, selectedPrimaryAccountId) {
        accounts.map { account ->
            account.copy(isPrimary = selectedPrimaryAccountId == account.id)
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "계좌 및 카드 관리",
                        style = NaedaTypography.titleMedium,
                        color = OnBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = OnBackground
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AccountTabRow(
                selectedTab = selectedTab,
                tabs = tabs,
                onTabSelected = { selectedTab = it }
            )

            if (selectedTab == 0) {
                AccountListContent(
                    accounts = displayedAccounts,
                    expandedMenuId = expandedMenuId,
                    onMenuToggle = { id ->
                        expandedMenuId = if (expandedMenuId == id) null else id
                    },
                    onAccountClick = onAccountClick,
                    onSetPrimary = { account ->
                        expandedMenuId = null
                        selectedPrimaryAccountId = account.id
                        onSetPrimary(account)
                    },
                    onRegisterNew = onRegisterNewAccount
                )
            } else {
                CardListContent(
                    cards = cards,
                    onRegisterNew = onRegisterNewCard,
                    onCardClick = onCardClick,
                    onSetPrimaryCard = onSetPrimaryCard,
                    onDeleteCard = onDeleteCard
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 탭 바
// ─────────────────────────────────────────────

@Composable
private fun AccountTabRow(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Surface(color = Surface, shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        style = NaedaTypography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) Mint900 else OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isSelected) Mint900 else Color.Transparent)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 계좌 목록 콘텐츠
// ─────────────────────────────────────────────

@Composable
private fun AccountListContent(
    accounts: List<AccountItem>,
    expandedMenuId: String?,
    onMenuToggle: (String) -> Unit,
    onAccountClick: (AccountItem) -> Unit,
    onSetPrimary: (AccountItem) -> Unit,
    onRegisterNew: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { AccountListHeader(count = accounts.size, onRegisterNew = onRegisterNew) }

        items(accounts, key = { it.id }) { account ->
            AccountListItem(
                account = account,
                isMenuExpanded = expandedMenuId == account.id,
                onMenuToggle = { onMenuToggle(account.id) },
                onAccountClick = { onAccountClick(account) },
                onSetPrimary = { onSetPrimary(account) }
            )
        }

        item { AccountInfoNotice() }
    }
}

// ─────────────────────────────────────────────
// 계좌 수 헤더
// ─────────────────────────────────────────────

@Composable
private fun AccountListHeader(count: Int, onRegisterNew: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "등록된 계좌",
                style = NaedaTypography.labelMedium,
                color = OnSurfaceVariant
            )
            Text(
                text = "총 ${count}개",
                style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = OnBackground
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onRegisterNew() }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "계좌 추가",
                tint = Mint900,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "추가하기",
                style = NaedaTypography.labelMedium,
                color = Mint900
            )
        }
    }
}

// ─────────────────────────────────────────────
// 계좌 아이템
// ─────────────────────────────────────────────

@Composable
private fun AccountListItem(
    account: AccountItem,
    isMenuExpanded: Boolean,
    onMenuToggle: () -> Unit,
    onAccountClick: () -> Unit,
    onSetPrimary: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onAccountClick() },
        shape = RoundedCornerShape(14.dp),
        color = Surface,
        shadowElevation = 1.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BankIcon(initials = account.bankInitials, color = account.bankColor)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.bankName,
                        style = NaedaTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OnBackground,
                        maxLines = 1
                    )
                    if (account.isPrimary) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "대표",
                            style = NaedaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Error,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${account.accountName} · ${account.accountNumber}",
                    style = NaedaTypography.labelMedium,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!account.isPrimary) {
                Box {
                    IconButton(
                        onClick = onMenuToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = onMenuToggle,
                        modifier = Modifier.background(Surface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "대표계좌 변경",
                                    style = NaedaTypography.bodyMedium,
                                    color = OnBackground
                                )
                            },
                            onClick = onSetPrimary
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 은행 이니셜 아이콘
// ─────────────────────────────────────────────

@Composable
private fun BankIcon(initials: String, color: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = NaedaTypography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            color = Color.White
        )
    }
}

// ─────────────────────────────────────────────
// 안내 문구
// ─────────────────────────────────────────────

@Composable
private fun AccountInfoNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(text = "ⓘ", style = NaedaTypography.labelSmall, color = OnSurfaceVariant)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "계좌를 해지하시려면 해당 금융사 앱 또는 영업점을 이용해 주세요.\n등록된 정보는 안전한 보안 통신을 통해 관리되며, 서비스 이용 이외의 목적으로 사용되지 않습니다.",
            style = NaedaTypography.labelSmall,
            color = OnSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

// ─────────────────────────────────────────────
// 카드 목록 콘텐츠
// ─────────────────────────────────────────────

@Composable
private fun CardListContent(
    cards: List<CardItem>,
    onRegisterNew: () -> Unit,
    onCardClick: (CardItem) -> Unit = {},
    onSetPrimaryCard: (CardItem) -> Unit = {},
    onDeleteCard: (CardItem) -> Unit = {}
) {
    var expandedMenuId by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var targetCard by remember { mutableStateOf<CardItem?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "등록된 카드",
                        style = NaedaTypography.labelMedium,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = "총 ${cards.size}개",
                        style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnBackground
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onRegisterNew() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "카드 추가",
                        tint = Mint900,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "추가하기",
                        style = NaedaTypography.labelMedium,
                        color = Mint900
                    )
                }
            }
        }

        items(cards, key = { it.id }) { card ->
            CardListItem(
                card = card,
                isMenuExpanded = expandedMenuId == card.id,
                onMenuToggle = { expandedMenuId = if (expandedMenuId == card.id) null else card.id },
                onClick = { onCardClick(card) },
                onSetPrimary = {
                    expandedMenuId = null
                    onSetPrimaryCard(card)
                },
                onDeleteRequest = {
                    expandedMenuId = null
                    targetCard = card
                    showDeleteDialog = true
                }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(text = "ⓘ", style = NaedaTypography.labelSmall, color = OnSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "카드를 해지하시려면 해당 카드사 앱 또는 고객센터를 이용해 주세요.\n등록된 정보는 안전한 보안 통신을 통해 관리되며, 서비스 이용 이외의 목적으로 사용되지 않습니다.",
                    style = NaedaTypography.labelSmall,
                    color = OnSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }

    if (showDeleteDialog && targetCard != null) {
        CardDeleteDialog(
            onDismiss = { showDeleteDialog = false; targetCard = null },
            onConfirm = {
                targetCard?.let { onDeleteCard(it) }
                showDeleteDialog = false
                targetCard = null
            }
        )
    }
}

// ─────────────────────────────────────────────
// 카드 아이템 (실제 카드 모양)
// ─────────────────────────────────────────────

@Composable
private fun CardListItem(
    card: CardItem,
    isMenuExpanded: Boolean = false,
    onMenuToggle: () -> Unit = {},
    onClick: () -> Unit = {},
    onSetPrimary: () -> Unit = {},
    onDeleteRequest: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(card.cardGradientStart, card.cardGradientEnd)
                )
            )
    ) {
        // 배경 원형 장식
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = 160.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            modifier = Modifier
                .size(130.dp)
                .offset(x = 200.dp, y = 60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단: 카드사명 + 카드 타입 + ... 메뉴
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.cardIssuerName,
                        style = NaedaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    if (card.isPrimary) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "대표",
                                style = NaedaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (card.cardType == "CREDIT") "신용" else "체크",
                            style = NaedaTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box {
                        IconButton(
                            onClick = onMenuToggle,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "더보기",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = onMenuToggle,
                            modifier = Modifier.background(Surface)
                        ) {
                            if (!card.isPrimary) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "대표 카드로 설정",
                                            style = NaedaTypography.bodyMedium,
                                            color = OnBackground
                                        )
                                    },
                                    onClick = onSetPrimary
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "삭제",
                                        style = NaedaTypography.bodyMedium,
                                        color = Error
                                    )
                                },
                                onClick = onDeleteRequest
                            )
                        }
                    }
                }
            }

            // 중단: 카드 상품명
            Text(
                text = card.cardName,
                style = NaedaTypography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )

            // 하단: 카드번호 + 유효기간
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = card.cardNumber,
                    style = NaedaTypography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = "~ ${card.cardExpiryDate}",
                    style = NaedaTypography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 삭제 확인 다이얼로그
// ─────────────────────────────────────────────

@Composable
private fun AccountDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "계좌를 삭제하시겠습니까?",
                    style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "삭제 시 해당 계좌의 정보가\n앱에서 제거됩니다.",
                    style = NaedaTypography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = OnSurfaceVariant
                        )
                    ) {
                        Text(text = "취소", style = NaedaTypography.labelLarge)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint900)
                    ) {
                        Text(
                            text = "삭제하기",
                            style = NaedaTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 카드 삭제 확인 다이얼로그
// ─────────────────────────────────────────────

@Composable
private fun CardDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "카드를 삭제하시겠습니까?",
                    style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "삭제 시 해당 카드의 정보가\n앱에서 제거됩니다.",
                    style = NaedaTypography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = OnSurfaceVariant
                        )
                    ) {
                        Text(text = "취소", style = NaedaTypography.labelLarge)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint900)
                    ) {
                        Text(
                            text = "삭제하기",
                            style = NaedaTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
