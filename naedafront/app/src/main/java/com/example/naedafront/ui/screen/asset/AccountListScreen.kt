// File: app/src/main/java/com/example/naedafront/ui/screen/asset/AccountListScreen.kt
package com.example.naedafront.ui.screen.asset

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.MaterialTheme
import com.example.naedafront.ui.theme.Error
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography

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
    val cardId: Long? = null,
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "계좌 및 카드 관리",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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
    MaterialSurface(color = MaterialTheme.colorScheme.background, shadowElevation = 0.dp) {
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
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
            AccountPassbookCardLikeListItem(
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "총 ${count}개",
                style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onRegisterNew() }
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "계좌 추가",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "추가하기",
                style = NaedaTypography.labelMedium,
                color = MaterialTheme.colorScheme.primary
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
    MaterialSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onAccountClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
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
                        color = MaterialTheme.colorScheme.onBackground,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = onMenuToggle,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (!account.isPrimary) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "대표계좌 변경",
                                        style = NaedaTypography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground
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
private fun AccountPassbookListItem(
    account: AccountItem,
    isMenuExpanded: Boolean,
    onMenuToggle: () -> Unit,
    onAccountClick: () -> Unit,
    onSetPrimary: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val gradientEnd = account.bankColor.blendTowardWhite(0.42f)
    val isLight = isLightColor(gradientEnd)
    val textPrimary = if (isLight) Color(0xFF1A1A1A) else Color.White
    val textSecondary = if (isLight) Color(0xFF1A1A1A).copy(alpha = 0.62f) else Color.White.copy(alpha = 0.68f)
    val badgeBg = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.18f)
    val decoColor = if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.10f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onAccountClick() }
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(account.bankColor, gradientEnd)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = 210.dp, y = (-18).dp)
                .clip(CircleShape)
                .background(decoColor)
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 240.dp, y = 118.dp)
                .clip(CircleShape)
                .background(decoColor.copy(alpha = decoColor.alpha * 0.9f))
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = account.bankName,
                        style = NaedaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary,
                        maxLines = 1
                    )
                    if (account.isPrimary) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "대표",
                                style = NaedaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "계좌",
                            style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary
                        )
                    }

                    if (account.paymentMethodId != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box {
                            IconButton(onClick = onMenuToggle) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "더보기",
                                    tint = textPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = onMenuToggle,
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                if (!account.isPrimary) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "대표계좌로 설정",
                                                style = NaedaTypography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onBackground
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

            Column {
                Text(
                    text = account.accountName,
                    style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = account.accountNumber,
                    style = NaedaTypography.bodyLarge,
                    color = textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "현재 잔액",
                        style = NaedaTypography.labelMedium,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${"%,d".format(account.accountBalance ?: 0L)}원",
                        style = NaedaTypography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = textPrimary,
                        maxLines = 1
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BankIcon(
                        initials = account.bankInitials,
                        color = account.bankColor
                    )
                    Box(
                        modifier = Modifier
                            .width(112.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(textPrimary.copy(alpha = 0.18f))
                    )
                    Box(
                        modifier = Modifier
                            .width(86.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(textPrimary.copy(alpha = 0.14f))
                    )
                }
            }
        }
    }
}

private fun Color.blendTowardWhite(fraction: Float): Color {
    return Color(
        red = red + (1f - red) * fraction,
        green = green + (1f - green) * fraction,
        blue = blue + (1f - blue) * fraction,
        alpha = 1f
    )
}

@Composable
private fun AccountPassbookCardLikeListItem(
    account: AccountItem,
    isMenuExpanded: Boolean,
    onMenuToggle: () -> Unit,
    onAccountClick: () -> Unit,
    onSetPrimary: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val paperColor = Color(0xFFFFFCF5)
    val paperBorder = Color(0xFFE7DED0)
    val textPrimary = Color(0xFF2E261C)
    val textSecondary = Color(0xFF7A7065)
    val textBody = Color(0xFF4A4035)
    val badgeBg = Color(0xFFF2E6D8)
    val maskedAccountNumber = account.accountNumber.maskAccountNumberForList()

    MaterialSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable { onAccountClick() }
            .border(1.dp, paperBorder, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = paperColor,
        shadowElevation = 3.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(18.dp)
                    .background(account.bankColor)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.78f))
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = account.bankName,
                            style = NaedaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (account.isPrimary) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "대표",
                                    style = NaedaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = textPrimary
                                )
                            }
                        }
                    }

                    if (account.paymentMethodId != null) {
                        Box {
                            IconButton(onClick = onMenuToggle) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "더보기",
                                    tint = textPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = onMenuToggle,
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                if (!account.isPrimary) {
                                    DropdownMenuItem(
                                        text = { Text("대표계좌로 설정") },
                                        onClick = onSetPrimary
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("삭제", color = Error) },
                                    onClick = onDeleteRequest
                                )
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = account.accountName,
                        style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = maskedAccountNumber,
                        style = NaedaTypography.bodyLarge,
                        color = textBody,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(badgeBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "계좌",
                                style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = textBody
                            )
                        }
                        Text(
                            text = formatWon(account.accountBalance ?: 0L),
                            style = NaedaTypography.labelMedium,
                            color = textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PassbookStamp(initials: String, color: Color) {
    val stampColor = color.copy(alpha = 0.88f)
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .border(2.dp, stampColor.copy(alpha = 0.28f), CircleShape)
            .background(stampColor.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = NaedaTypography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = stampColor
        )
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
        Text(text = "ⓘ", style = NaedaTypography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "계좌를 해지하시려면 해당 금융사 앱 또는 영업점을 이용해 주세요.\n등록된 정보는 안전한 보안 통신을 통해 관리되며, 서비스 이용 이외의 목적으로 사용되지 않습니다.",
            style = NaedaTypography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "총 ${cards.size}개",
                        style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onRegisterNew() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "카드 추가",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "추가하기",
                        style = NaedaTypography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
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
                Text(text = "ⓘ", style = NaedaTypography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "카드를 해지하시려면 해당 카드사 앱 또는 고객센터를 이용해 주세요.\n등록된 정보는 안전한 보안 통신을 통해 관리되며, 서비스 이용 이외의 목적으로 사용되지 않습니다.",
                    style = NaedaTypography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val cardTypeLabel = card.cardType.toCardTypeShortLabel()

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
                                style = NaedaTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = textPrimary
                            )
                        }
                    }
                }

                if (card.paymentMethodId != null) {
                    Box {
                        IconButton(onClick = onMenuToggle) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "더보기",
                                tint = textPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = onMenuToggle,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (!card.isPrimary) {
                                DropdownMenuItem(
                                    text = { Text("대표카드 변경") },
                                    onClick = onSetPrimary
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("삭제", color = Error) },
                                onClick = onDeleteRequest
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = card.cardName,
                    style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = card.cardNumber.maskCardNumberForList(),
                    style = NaedaTypography.bodyLarge,
                    color = textBody
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(badgeBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = cardTypeLabel,
                            style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = textBody
                        )
                    }
                    Text(
                        text = card.cardExpiryDate,
                        style = NaedaTypography.labelMedium,
                        color = textSecondary
                    )
                }
            }
        }
    }
}

private fun formatWon(amount: Long): String {
    return "%,d원".format(amount)
}

private fun String.maskAccountNumberForList(): String {
    val digits = replace("-", "").replace(" ", "")
    return when {
        digits.isBlank() -> "-"
        digits.length <= 7 -> this
        else -> "${digits.take(3)}${"*".repeat(digits.length - 7)}${digits.takeLast(4)}"
    }
}

private fun String.maskCardNumberForList(): String {
    val digits = replace("-", "").replace(" ", "")
    return if (digits.length >= 16) {
        "${digits.substring(0, 4)}-****-****-${digits.takeLast(4)}"
    } else if (isBlank()) {
        "-"
    } else {
        this
    }
}

private fun String?.toCardTypeShortLabel(): String {
    return when (this?.uppercase()) {
        "CREDIT" -> "신용"
        "CHECK", "DEBIT" -> "체크"
        else -> this?.ifBlank { "-" } ?: "-"
    }
}

@Composable
private fun AccountDeleteDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        MaterialSurface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "계좌를 목록에서 삭제할까요?",
                    style = NaedaTypography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "삭제 후에도 실제 계좌는 해지되지 않아요.",
                    style = NaedaTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                    ) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Text("삭제")
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
        MaterialSurface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "카드를 목록에서 삭제할까요?",
                    style = NaedaTypography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "삭제 후에도 실제 카드는 해지되지 않아요.",
                    style = NaedaTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                    ) {
                        Text("취소")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Text("삭제")
                    }
                }
            }
        }
    }
}
