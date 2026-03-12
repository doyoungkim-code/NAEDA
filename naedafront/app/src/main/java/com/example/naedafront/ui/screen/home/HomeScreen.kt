package com.example.naedafront.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.ui.theme.*
import kotlinx.coroutines.delay

// ────────────────────────────────────────
// 데이터 모델 (임시 — 나중에 data/model 로 이동)
// ────────────────────────────────────────

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
    val tag: String,       // 예: "축제", "공지", "이벤트"
    val tagColor: Color,
    val title: String,
    val date: String
)

// ────────────────────────────────────────
// HomeUiState
// ────────────────────────────────────────

data class HomeUiState(
    val userName: String = "사용자",
    val isAccountLinked: Boolean = true,       // 계좌 연결 여부 ← 핵심 분기
    val isFaceRegistered: Boolean = false,     // 얼굴 등록 여부 ← 페이스페이 배너 분기
    val totalBalance: Long = 18_240_500L,
    val recentTransactions: List<TransactionItem> = emptyList(),
    val spendingCategories: List<SpendingCategory> = emptyList(),
    val topSpendingCategory: String = "식비",
    val topSpendingAmount: Long = 842_500L,
    val notices: List<NoticeItem> = emptyList()
)

// ────────────────────────────────────────
// HomeScreen
// ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState = HomeUiState(),
    onTransferClick: () -> Unit = {},
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
            // 인사말
            GreetingSection(
                userName = uiState.userName,
                onSecretFaceMatchTestClick = onSecretFaceMatchTestClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ★ 계좌 연결 여부에 따른 분기
            if (uiState.isAccountLinked) {
                // 계좌 연결 O → 총 잔액 카드
                BalanceCard(
                    totalBalance = uiState.totalBalance,
                    onTransferClick = onTransferClick,
                    onTransactionClick = onTransactionClick
                )
            } else {
                // 계좌 연결 X → 계좌 등록 유도 카드
                LinkAccountCard(onLinkAccountClick = onLinkAccountClick)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 페이스페이 배너 (등록 여부에 따라 분기)
            if (uiState.isFaceRegistered) {
                FacePayBenefitCard(onReRegisterClick = onFacePaySettingClick)
            } else {
                FacePayBannerCard(onFacePaySettingClick = onFacePaySettingClick)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 구미시 공지사항
            NoticeCard(notices = uiState.notices)

            Spacer(modifier = Modifier.height(16.dp))

            // 이번 달 소비 분석
            SpendingAnalysisCard(
                topCategory = uiState.topSpendingCategory,
                topAmount = uiState.topSpendingAmount,
                categories = uiState.spendingCategories
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 최근 거래 내역
            RecentTransactionsSection(
                transactions = uiState.recentTransactions,
                onViewAllClick = onViewAllTransactionsClick
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ────────────────────────────────────────
// Preview
// ────────────────────────────────────────

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
