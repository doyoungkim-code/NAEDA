package com.example.naedafront.ui.screen.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.naedafront.ui.theme.Background
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.naedafront.ui.common.NaedaTopBar

@Composable
fun MyPageScreen(
    viewModel: MyPageViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onFaceReRegisterClick: () -> Unit = {},
    onSecondaryAuthClick: () -> Unit = {},
    onPinChangeClick: () -> Unit = {},
    onDeliveryAddressClick: () -> Unit = {},
    onOrderHistoryClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadInitialUserInfo(context)
        viewModel.fetchMyInfo(context)
    }

    val bgColor = Background

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = bgColor,
        topBar = {
            NaedaTopBar(
                title = "마이페이지",
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "설정",
                            tint = Color(0xFF67707E)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(18.dp))

                ProfileHeader(
                    userName = uiState.userName.ifBlank { "사용자" },
                    userEmail = uiState.userEmail.ifBlank { "-" },
                    isLogoutLoading = uiState.isLogoutLoading,
                    onLogoutClick = {
                        viewModel.logout(
                            context = context,
                            onLogoutSuccess = onLogoutClick
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                QuickOrderDeliveryCard(
                    onClick = onOrderHistoryClick
                )

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = uiState.errorMessage ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFFE53935)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                SectionTitle("보안 설정")
                Spacer(modifier = Modifier.height(12.dp))

                MenuSectionCard(
                    items = listOf(
                        MyPageMenuItemData(
                            title = "얼굴 재등록",
                            icon = Icons.Outlined.Face,
                            iconBg = Color(0xFFE8F7F1),
                            iconTint = Color(0xFF16A36A),
                            onClick = onFaceReRegisterClick
                        ),
                        MyPageMenuItemData(
                            title = "2차 암호 사용",
                            icon = Icons.Outlined.Edit,
                            iconBg = Color(0xFFFFF4E5),
                            iconTint = Color(0xFFF59E0B),
                            trailingType = MyPageMenuTrailing.Toggle(
                                checked = uiState.secondaryAuthEnabled,
                                enabled = !uiState.isUpdatingSecondaryAuth
                            ),
                            onClick = {
                                if (!uiState.faceRegistered) {
                                    Toast.makeText(context, "얼굴 등록 후 사용할 수 있습니다.", Toast.LENGTH_SHORT).show()
                                } else if (uiState.secondaryAuthEnabled) {
                                    viewModel.updateSecondaryAuth(
                                        context = context,
                                        enable = false,
                                        currentPin = null,
                                        onSuccess = {
                                            Toast.makeText(context, "2차 인증이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    onSecondaryAuthClick()
                                }
                            }
                        ),
                        MyPageMenuItemData(
                            title = "PIN 번호 변경",
                            icon = Icons.Outlined.LockReset,
                            iconBg = Color(0xFFEEF4FF),
                            iconTint = Color(0xFF2F6FED),
                            onClick = onPinChangeClick
                        )
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                SectionTitle("내 정보 관리")
                Spacer(modifier = Modifier.height(12.dp))

                MenuSectionCard(
                    items = listOf(
                        MyPageMenuItemData(
                            title = "배송지 관리",
                            icon = Icons.Outlined.LocationOn,
                            iconBg = Color(0xFFEEF4FF),
                            iconTint = Color(0xFF2F6FED),
                            onClick = onDeliveryAddressClick
                        ),
                        MyPageMenuItemData(
                            title = "주문 조회",
                            icon = Icons.Outlined.ReceiptLong,
                            iconBg = Color(0xFFFFF4E5),
                            iconTint = Color(0xFFF59E0B),
                            onClick = onOrderHistoryClick
                        )
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun QuickOrderDeliveryCard(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE8F7F1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalShipping,
                    contentDescription = "배송 조회",
                    tint = Color(0xFF16A36A),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "배송 조회",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "주문 내역과 배송 상태를 확인하세요",
                    fontSize = 13.sp,
                    color = Color(0xFF7B8494)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "이동",
                tint = Color(0xFFB8BEC8),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color(0xFFB0B7C3),
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ProfileHeader(
    userName: String,
    userEmail: String,
    isLogoutLoading: Boolean,
    onLogoutClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF6CFC3)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .border(
                                width = 1.dp,
                                color = Color(0xFFE4E4E7),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "카드",
                            fontSize = 7.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F8B72))
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "프로필 편집",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = userName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userEmail,
                    fontSize = 13.sp,
                    color = Color(0xFF7B8494)
                )
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFFFF1F1))
                .clickable(enabled = !isLogoutLoading, onClick = onLogoutClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = "로그아웃",
                tint = Color(0xFFFF4D4F),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isLogoutLoading) "로그아웃 중..." else "로그아웃",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFF4D4F)
            )
        }
    }
}

@Composable
private fun MenuSectionCard(
    items: List<MyPageMenuItemData>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            items.forEachIndexed { index, item ->
                MyPageMenuRow(item = item)

                if (index != items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = Color(0xFFF1F3F5),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun MyPageMenuRow(
    item: MyPageMenuItemData
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.enabled, onClick = item.onClick)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(item.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.title,
            fontSize = 16.sp,
            color = item.textColor,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        when (val trailing = item.trailingType) {
            MyPageMenuTrailing.Chevron -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "이동",
                    tint = Color(0xFFB8BEC8),
                    modifier = Modifier.size(22.dp)
                )
            }

            is MyPageMenuTrailing.Toggle -> {
                Switch(
                    checked = trailing.checked,
                    onCheckedChange = if (trailing.enabled) {
                        { item.onClick() }
                    } else {
                        null
                    },
                    enabled = trailing.enabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF16A36A),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFD5D9E0),
                        uncheckedBorderColor = Color(0xFFD5D9E0)
                    )
                )
            }
        }
    }
}

@Immutable
private sealed interface MyPageMenuTrailing {
    data object Chevron : MyPageMenuTrailing
    data class Toggle(
        val checked: Boolean,
        val enabled: Boolean
    ) : MyPageMenuTrailing
}

@Immutable
private data class MyPageMenuItemData(
    val title: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val textColor: Color = Color(0xFF1F2937),
    val enabled: Boolean = true,
    val trailingType: MyPageMenuTrailing = MyPageMenuTrailing.Chevron,
    val onClick: () -> Unit = {}
)
