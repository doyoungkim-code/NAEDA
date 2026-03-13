package com.example.naedafront.ui.screen.mypage

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.HeadsetMic
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MyPageScreen(
    modifier: Modifier = Modifier,
    userName: String = "김종우님",
    userEmail: String = "kjw_naeda@email.com",
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onFaceReRegisterClick: () -> Unit = {},
    onPinChangeClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onContactManageClick: () -> Unit = {},
    onCustomerCenterClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onFabClick: () -> Unit = {},
    selectedBottomTab: MyPageBottomTab = MyPageBottomTab.Settings,
    onBottomTabClick: (MyPageBottomTab) -> Unit = {},
) {
    val bgColor = Color(0xFFF5F6F8)
    val cardColor = Color.White
    val titleColor = Color(0xFF1F2937)
    val bodyColor = Color(0xFF6B7280)
    val sectionColor = Color(0xFFB0B7C3)
    val dividerColor = Color(0xFFF0F1F3)
    val fabColor = Color(0xFF0FA37F)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
        containerColor = bgColor,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = fabColor,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Outlined.Badge,
                    contentDescription = "직원 호출",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        bottomBar = {
            MyPageBottomBar(
                selected = selectedBottomTab,
                onTabClick = onBottomTabClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(innerPadding)
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "마이페이지",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = "알림",
                    tint = Color(0xFF67707E),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onNotificationClick)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "설정",
                    tint = Color(0xFF67707E),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onSettingsClick)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            ProfileHeader(
                userName = userName,
                userEmail = userEmail
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "보안 설정",
                fontSize = 14.sp,
                color = sectionColor,
                fontWeight = FontWeight.Medium
            )

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
                        title = "PIN 번호 변경",
                        icon = Icons.Outlined.LockReset,
                        iconBg = Color(0xFFEEF4FF),
                        iconTint = Color(0xFF2F6FED),
                        onClick = onPinChangeClick
                    )
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "내 정보 관리",
                fontSize = 14.sp,
                color = sectionColor,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuSectionCard(
                items = listOf(
                    MyPageMenuItemData(
                        title = "프로필 정보 수정",
                        icon = Icons.Outlined.PersonOutline,
                        iconBg = Color(0xFFF3F4F6),
                        iconTint = Color(0xFF5B6472),
                        onClick = onEditProfileClick
                    ),
                    MyPageMenuItemData(
                        title = "연락처 관리",
                        icon = Icons.Outlined.ContactPhone,
                        iconBg = Color(0xFFF3F4F6),
                        iconTint = Color(0xFF5B6472),
                        onClick = onContactManageClick
                    )
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "서비스 지원",
                fontSize = 14.sp,
                color = sectionColor,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuSectionCard(
                items = listOf(
                    MyPageMenuItemData(
                        title = "고객센터",
                        icon = Icons.Outlined.HeadsetMic,
                        iconBg = Color(0xFFF3F4F6),
                        iconTint = Color(0xFF5B6472),
                        onClick = onCustomerCenterClick
                    ),
                    MyPageMenuItemData(
                        title = "로그아웃",
                        icon = Icons.Outlined.Logout,
                        iconBg = Color(0xFFFFF1F1),
                        iconTint = Color(0xFFFF4D4F),
                        textColor = Color(0xFFFF4D4F),
                        onClick = onLogoutClick
                    )
                )
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    userName: String,
    userEmail: String,
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
                    Divider(
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
            .clickable(onClick = item.onClick)
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

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "이동",
            tint = Color(0xFFB8BEC8),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun MyPageBottomBar(
    selected: MyPageBottomTab,
    onTabClick: (MyPageBottomTab) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        MyPageBottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onTabClick(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp
                    )
                }
            )
        }
    }
}

@Immutable
private data class MyPageMenuItemData(
    val title: String,
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val textColor: Color = Color(0xFF1F2937),
    val onClick: () -> Unit = {}
)

enum class MyPageBottomTab(
    val label: String,
    val icon: ImageVector
) {
    Home("홈", Icons.Outlined.Home),
    Point("포인트", Icons.Outlined.Campaign),
    Store("가맹점", Icons.Outlined.Storefront),
    MyTime("내 시간", Icons.Outlined.AccessTime),
    Settings("설정", Icons.Outlined.Settings)
}