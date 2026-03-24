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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

data class AccountItem(
    val id: String,
    val accountId: Long? = null,
    val paymentMethodId: Long? = null,
    val bankCode: String,
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val accountBalance: Long? = null,
    val isPrimary: Boolean = false,
    val bankColor: Color,
    val bankInitials: String
)

data class CardItem(
    val id: String,
    val paymentMethodId: Long? = null,
    val cardType: String,
    val cardIssuerName: String,
    val cardName: String,
    val cardNumber: String,
    val cardExpiryDate: String,
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
    val cardGradientStart: Color,
    val cardGradientEnd: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    initialTab: Int = 0,
    accounts: List<AccountItem>,
    cards: List<CardItem>,
    onBack: () -> Unit = {},
    showBackButton: Boolean = true,
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
    var showAccountDeleteDialog by remember { mutableStateOf(false) }
    var targetAccount by remember { mutableStateOf<AccountItem?>(null) }

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
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = OnBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface),
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0)
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
                    accounts = accounts,
                    expandedMenuId = expandedMenuId,
                    onMenuToggle = { id ->
                        expandedMenuId = if (expandedMenuId == id) null else id
                    },
                    onAccountClick = onAccountClick,
                    onSetPrimary = { account ->
                        expandedMenuId = null
                        onSetPrimary(account)
                    },
                    onDeleteRequest = { account ->
                        expandedMenuId = null
                        targetAccount = account
                        showAccountDeleteDialog = true
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

    if (showAccountDeleteDialog && targetAccount != null) {
        AccountDeleteDialog(
            onDismiss = {
                showAccountDeleteDialog = false
                targetAccount = null
            },
            onConfirm = {
                targetAccount?.let { onDeleteAccount(it) }
                showAccountDeleteDialog = false
                targetAccount = null
            }
        )
    }
}

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

@Composable
private fun AccountListContent(
    accounts: List<AccountItem>,
    expandedMenuId: String?,
    onMenuToggle: (String) -> Unit,
    onAccountClick: (AccountItem) -> Unit,
    onSetPrimary: (AccountItem) -> Unit,
    onDeleteRequest: (AccountItem) -> Unit,
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
                onSetPrimary = { onSetPrimary(account) },
                onDeleteRequest = { onDeleteRequest(account) }
            )
        }

        item { AccountInfoNotice() }
    }
}

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

@Composable
private fun AccountListItem(
    account: AccountItem,
    isMenuExpanded: Boolean,
    onMenuToggle: () -> Unit,
    onAccountClick: () -> Unit,
    onSetPrimary: () -> Unit,
    onDeleteRequest: () -> Unit
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

            if (account.paymentMethodId != null) {
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
                        if (!account.isPrimary) {
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
    }
}

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
            onDismiss = {
                showDeleteDialog = false
                targetCard = null
            },
            onConfirm = {
                targetCard?.let { onDeleteCard(it) }
                showDeleteDialog = false
                targetCard = null
            }
        )
    }
}

private fun isLightColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.5
}

@Composable
private fun CardListItem(
    card: CardItem,
    isMenuExpanded: Boolean = false,
    onMenuToggle: () -> Unit = {},
    onClick: () -> Unit = {},
    onSetPrimary: () -> Unit = {},
    onDeleteRequest: () -> Unit = {}
) {
    val isLight = isLightColor(card.cardGradientStart)
    val textPrimary = if (isLight) Color(0xFF1A1A1A) else Color.White
    val textSecondary = if (isLight) Color(0xFF1A1A1A).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.45f)
    val textBody = if (isLight) Color(0xFF1A1A1A).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.85f)
    val badgeBg = if (isLight) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f)
    val decoColor = if (isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)
    val decoColor2 = if (isLight) Color.Black.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.04f)

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
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = 160.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(decoColor)
        )
        Box(
            modifier = Modifier
                .size(130.dp)
                .offset(x = 200.dp, y = 60.dp)
                .clip(CircleShape)
                .background(decoColor2)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.cardIssuerName,
                        style = NaedaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                    if (card.isPrimary) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "대표",
                                style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (card.cardType == "CREDIT") "신용" else "체크",
                            style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary
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
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = onMenuToggle,
                            modifier = Modifier.background(Surface)
                        ) {
                            if (!card.isPrimary && card.paymentMethodId != null) {
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

            Text(
                text = card.cardName,
                style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = textBody,
                maxLines = 1
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = card.cardNumber,
                    style = NaedaTypography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    ),
                    color = textPrimary
                )
                Text(
                    text = "~ ${card.cardExpiryDate}",
                    style = NaedaTypography.bodySmall,
                    color = textSecondary
                )
            }
        }
    }
}

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