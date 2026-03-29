package com.example.naedafront.ui.screen

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.material3.TextButton
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
import androidx.compose.material3.MaterialTheme
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                            val unreadNotifications = notifications
                                .filter { notification -> notification.isRead != true }
                                .sortedByDescending { notification -> notification.sent }
                            _uiState.update {
                                it.copy(
                                    notifications = unreadNotifications,
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

    fun markAsRead(notificationId: Long?) {
        if (notificationId == null) return
        if (_uiState.value.notifications.firstOrNull { it.notificationId == notificationId }?.isRead == true) return

        viewModelScope.launch {
            NotificationRepository.markAsRead(notificationId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.filterNot { notification ->
                                notification.notificationId == notificationId
                            },
                            unreadCount = (state.unreadCount - 1L).coerceAtLeast(0L)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "알림 읽음 처리에 실패했습니다.") }
                }
        }
    }

    fun markAllAsRead(context: Context) {
        val userNo = AuthPrefs.getUserNo(context) ?: return
        if (_uiState.value.unreadCount <= 0L) return

        viewModelScope.launch {
            NotificationRepository.markAllAsRead(userNo)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            notifications = emptyList(),
                            unreadCount = 0L
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message ?: "알림 전체 읽음 처리에 실패했습니다.") }
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "알림",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.markAllAsRead(context) },
                        enabled = uiState.unreadCount > 0L
                    ) {
                        Text(
                            text = "모두 읽음",
                            color = if (uiState.unreadCount > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        text = "읽지 않은 알림이 없습니다.",
                        style = NaedaTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        NotificationRow(
                            notification = notification,
                            onClick = { viewModel.markAsRead(notification.notificationId) }
                        )
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
private fun NotificationRow(
    notification: NotificationResponse,
    onClick: () -> Unit
) {
    val isRead = notification.isRead == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isRead) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface)
            .clickable(enabled = !isRead, onClick = onClick)
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
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
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
                    else -> MaterialTheme.colorScheme.primary
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
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = notification.sent?.formatSentTime() ?: "",
                    style = NaedaTypography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.body ?: "",
                style = NaedaTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.CenterVertically)
            )
        }
    }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp
    )
}

// ─────────────────────────────────────────────
// 시간 포맷
// ─────────────────────────────────────────────

private fun String.formatSentTime(): String {
    val dateTime = runCatching {
        OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }.getOrElse {
        runCatching { LocalDateTime.parse(this, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }.getOrNull()
    } ?: return this

    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
