package com.example.naedafront.ui.screen

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.NotificationRepository
import com.example.naedafront.data.remote.NotificationResponse
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import com.example.naedafront.ui.theme.OutlineVariant
import com.example.naedafront.ui.theme.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// ─────────────────────────────────────────────
// UiState
// ─────────────────────────────────────────────

data class NotificationUiState(
    val notifications: List<NotificationResponse> = emptyList(),
    val unreadCount: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null
)

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

class NotificationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    fun loadNotifications(context: Context) {
        val userNo = AuthPrefs.getUserNo(context) ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 알림 목록 + 안읽은 수 병렬 로드
            launch {
                NotificationRepository.getNotifications(userNo)
                    .onSuccess { notifications ->
                        _uiState.update {
                            it.copy(
                                notifications = notifications.sortedByDescending { it.sent },
                                isLoading = false
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(isLoading = false, error = e.message)
                        }
                    }
            }

            launch {
                NotificationRepository.getUnreadCount(userNo)
                    .onSuccess { count ->
                        _uiState.update { it.copy(unreadCount = count) }
                    }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 메인 화면
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: NotificationViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications(context)
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "알림",
                        style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = OnBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = OnBackground
                        )
                    }
                },
                actions = {
                    // 안읽은 알림 수 뱃지
                    if (uiState.unreadCount > 0) {
                        Box(
                            modifier = Modifier.padding(end = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Mint900)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (uiState.unreadCount > 99) "99+" else "${uiState.unreadCount}",
                                    style = NaedaTypography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Background
                )
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Mint900)
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "알림을 불러오지 못했습니다.",
                        style = NaedaTypography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            }

            uiState.notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "알림이 없습니다.",
                        style = NaedaTypography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    items(uiState.notifications) { notification ->
                        NotificationRow(notification = notification)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 알림 행
// ─────────────────────────────────────────────

@Composable
private fun NotificationRow(notification: NotificationResponse) {
    val isRead = notification.isRead == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isRead) Background else Surface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when (notification.type?.uppercase()) {
                        "PAYMENT" -> Color(0xFFDCEBFF)
                        "SECURITY" -> Color(0xFFFFEBEE)
                        else -> Mint900.copy(alpha = 0.1f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = when (notification.type?.uppercase()) {
                    "PAYMENT" -> Color(0xFF1565C0)
                    "SECURITY" -> Color(0xFFE53935)
                    else -> Mint900
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title ?: "",
                    style = NaedaTypography.bodyMedium.copy(
                        fontWeight = if (isRead) FontWeight.Normal else FontWeight.SemiBold
                    ),
                    color = OnBackground,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = notification.sent?.formatSentTime() ?: "",
                    style = NaedaTypography.labelSmall,
                    color = OnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.body ?: "",
                style = NaedaTypography.bodySmall,
                color = OnSurfaceVariant
            )
        }

        if (!isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Mint900)
                    .align(Alignment.CenterVertically)
            )
        }
    }

    HorizontalDivider(
        color = OutlineVariant,
        thickness = 0.5.dp
    )
}

// ─────────────────────────────────────────────
// 시간 포맷
// ─────────────────────────────────────────────

private fun String.formatSentTime(): String {
    val inputPatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS" to false,
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to true,
        "yyyy-MM-dd'T'HH:mm:ss.SSS" to false,
        "yyyy-MM-dd'T'HH:mm:ss" to false,
        "yyyy-MM-dd HH:mm:ss" to false
    )

    for ((pattern, isUtc) in inputPatterns) {
        try {
            val inputFormat = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                timeZone = if (isUtc) TimeZone.getTimeZone("UTC") else TimeZone.getDefault()
            }
            val date = inputFormat.parse(this) ?: continue

            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            val formatted = outputFormat.format(date)

            val now = Calendar.getInstance()
            val cal = Calendar.getInstance().apply { time = date }
            val diffMs = now.timeInMillis - cal.timeInMillis
            val diffMin = diffMs / (1000 * 60)
            val diffHour = diffMin / 60
            val diffDay = diffHour / 24

            return when {
                diffMin < 1 -> "방금"
                diffMin < 60 -> "${diffMin}분 전"
                diffHour < 24 -> "${diffHour}시간 전"
                else -> formatted
            }
        } catch (_: Exception) {
            continue
        }
    }
    return this
}