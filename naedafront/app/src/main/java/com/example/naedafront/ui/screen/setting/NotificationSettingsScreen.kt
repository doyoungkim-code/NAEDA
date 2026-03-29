package com.example.naedafront.ui.screen.setting

import androidx.compose.foundation.background
import com.example.naedafront.ui.theme.Mint900
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.AuthPrefs
import androidx.compose.material3.MaterialTheme
import com.example.naedafront.data.remote.NotificationRepository
import com.example.naedafront.data.remote.NotificationSettingRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userNo = AuthPrefs.getUserNo(context) ?: return

    var isLoading by remember { mutableStateOf(true) }
    var paymentEnabled by remember { mutableStateOf(true) }
    var fdsEnabled by remember { mutableStateOf(true) }
    var festivalEnabled by remember { mutableStateOf(true) }
    var pointEnabled by remember { mutableStateOf(true) }
    var systemEnabled by remember { mutableStateOf(true) }

    fun allEnabled() = paymentEnabled && fdsEnabled && festivalEnabled && pointEnabled && systemEnabled

    fun updateSettings() {
        scope.launch {
            NotificationRepository.updateNotificationSettings(
                userNo = userNo,
                request = NotificationSettingRequest(
                    paymentEnabled = paymentEnabled,
                    fdsEnabled = fdsEnabled,
                    festivalEnabled = festivalEnabled,
                    pointEnabled = pointEnabled,
                    systemEnabled = systemEnabled
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        NotificationRepository.getNotificationSettings(userNo)
            .onSuccess { settings ->
                paymentEnabled = settings.paymentEnabled
                fdsEnabled = settings.fdsEnabled
                festivalEnabled = settings.festivalEnabled
                pointEnabled = settings.pointEnabled
                systemEnabled = settings.systemEnabled
            }
        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "알림 설정",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF0FA37F))
                }
            } else {
                // 전체 알림
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        NotificationToggleRow(
                            icon = Icons.Outlined.Notifications,
                            title = "전체 알림",
                            description = "모든 알림을 한번에 켜거나 끕니다",
                            checked = allEnabled(),
                            onCheckedChange = { enabled ->
                                paymentEnabled = enabled
                                fdsEnabled = enabled
                                festivalEnabled = enabled
                                pointEnabled = enabled
                                systemEnabled = enabled
                                updateSettings()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "알림 유형",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        NotificationToggleRow(
                            icon = Icons.Outlined.CreditCard,
                            title = "결제 알림",
                            description = "결제 완료 알림",
                            checked = paymentEnabled,
                            onCheckedChange = { enabled ->
                                paymentEnabled = enabled
                                updateSettings()
                            }
                        )

                        CardDivider()

                        NotificationToggleRow(
                            icon = Icons.Outlined.Celebration,
                            title = "축제 알림",
                            description = "구미 축제 안내",
                            checked = festivalEnabled,
                            onCheckedChange = { enabled ->
                                festivalEnabled = enabled
                                updateSettings()
                            }
                        )

                        CardDivider()

                        NotificationToggleRow(
                            icon = Icons.Outlined.Stars,
                            title = "포인트 알림",
                            description = "포인트 적립 알림",
                            checked = pointEnabled,
                            onCheckedChange = { enabled ->
                                pointEnabled = enabled
                                updateSettings()
                            }
                        )

                        CardDivider()

                        NotificationToggleRow(
                            icon = Icons.Outlined.Campaign,
                            title = "시스템 알림",
                            description = "공지사항 및 시스템 안내",
                            checked = systemEnabled,
                            onCheckedChange = { enabled ->
                                systemEnabled = enabled
                                updateSettings()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Mint900,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
