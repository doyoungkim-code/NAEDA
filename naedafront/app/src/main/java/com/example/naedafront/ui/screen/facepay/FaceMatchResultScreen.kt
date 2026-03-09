package com.example.naedafront.ui.screen.facepay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.naedafront.data.remote.AccountResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceMatchResultScreen(
    onBack: () -> Unit
) {
    val result = FaceMatchSessionStore.latestResult

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("페이스 매칭 결과") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (result == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("표시할 결과가 없습니다.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ResponseSummaryCard(result)
            }

            result.topCandidate?.let { topCandidate ->
                item {
                    CandidateSection(
                        title = "가장 높은 후보",
                        description = "현재 프레임에서 가장 높은 유사도 계정입니다.",
                        candidates = listOf(topCandidate)
                    )
                }
            }

            if (result.matchedCandidates.isNotEmpty()) {
                item {
                    CandidateSection(
                        title = "Threshold 통과 후보",
                        description = "매칭 기준값을 넘은 계정들입니다.",
                        candidates = result.matchedCandidates
                    )
                }
            }

            if (result.ambiguousCandidates.isNotEmpty()) {
                item {
                    CandidateSection(
                        title = "애매한 후보",
                        description = "매칭 기준은 넘지 못했지만 임계값 근처에 있는 후보들입니다.",
                        candidates = result.ambiguousCandidates
                    )
                }
            }

            if (result.belowThresholdCandidates.isNotEmpty()) {
                item {
                    CandidateSection(
                        title = "참고 후보",
                        description = "임계값 아래 후보들입니다.",
                        candidates = result.belowThresholdCandidates
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponseSummaryCard(result: FaceMatchResultSnapshot) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "검색 요약",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            SummaryRow("status", result.searchResponse.status ?: "-")
            SummaryRow("bestUserId", result.searchResponse.bestUserId ?: "-")
            SummaryRow("matchedUserNo", result.searchResponse.matchedUserNo?.toString() ?: "-")
            SummaryRow("similarity", "%.4f".format(result.searchResponse.similarity))
            SummaryRow("matchThreshold", "%.4f".format(result.searchResponse.matchThreshold))
            SummaryRow("ambiguousThreshold", "%.4f".format(result.searchResponse.ambiguousThreshold))
            SummaryRow("nextAction", result.searchResponse.nextAction ?: "-")
        }
    }
}

@Composable
private fun CandidateSection(
    title: String,
    description: String,
    candidates: List<FaceMatchCandidateResult>
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            candidates.forEachIndexed { index, candidate ->
                CandidateCard(candidate = candidate)
                if (index < candidates.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CandidateCard(candidate: FaceMatchCandidateResult) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SummaryRow("userId", candidate.userId)
        SummaryRow("userNo", candidate.userNo?.toString() ?: "-")
        SummaryRow("pose", candidate.pose)
        SummaryRow("similarity", "%.4f".format(candidate.similarity))
        if (candidate.accounts.isEmpty()) {
            Text(
                text = "연결된 계좌 정보가 없습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            candidate.accounts.forEach { account ->
                AccountItem(account)
            }
        }
    }
}

@Composable
private fun AccountItem(account: AccountResponse) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = account.accountName ?: "계좌명 없음",
                fontWeight = FontWeight.SemiBold
            )
            Text("은행: ${account.bankName ?: account.bankCode ?: "-"}")
            Text("계좌번호: ${account.accountNo ?: "-"}")
            Text("잔액: ${account.accountBalance ?: 0L} ${account.currency ?: ""}")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label:",
            modifier = Modifier.width(140.dp),
            fontWeight = FontWeight.SemiBold
        )
        Text(text = value)
    }
}
