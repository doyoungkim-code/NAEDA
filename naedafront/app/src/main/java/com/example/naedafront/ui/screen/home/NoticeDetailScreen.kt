package com.example.naedafront.ui.screen.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.naedafront.data.remote.FestivalApiResponse
import com.example.naedafront.data.remote.NoticeApiResponse
import com.example.naedafront.data.repository.NoticeRepository
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeDetailScreen(
    type: String,
    id: Long,
    onBackClick: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var tagColor by remember { mutableStateOf(Color(0xFF1976D2)) }
    var dateInfo by remember { mutableStateOf("") }
    var location by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(type, id) {
        if (type == "festival") {
            NoticeRepository.getFestival(id)
                .onSuccess { f ->
                    title = f.title ?: ""
                    content = f.description ?: ""
                    tag = "축제"
                    tagColor = Color(0xFFE91E63)
                    dateInfo = "${f.startDate ?: ""} ~ ${f.endDate ?: ""}"
                    location = f.location ?: f.roadAddress
                }
                .onFailure { Log.e("NoticeDetail", "축제 상세 로드 실패: ${it.message}") }
        } else {
            NoticeRepository.getNotice(id)
                .onSuccess { n ->
                    title = n.title ?: ""
                    content = n.content ?: ""
                    tag = "공지"
                    tagColor = Color(0xFF1976D2)
                    dateInfo = n.created?.replace("T", " ")?.take(16) ?: ""
                }
                .onFailure { Log.e("NoticeDetail", "공지 상세 로드 실패: ${it.message}") }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (type == "festival") "축제 상세" else "공지사항 상세",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Background
                ),
                windowInsets = WindowInsets(0)
            )
        },
        containerColor = Background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "불러오는 중...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(tagColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = tagColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Title
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = OnBackground
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Date info
                        if (dateInfo.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = OnBackground.copy(alpha = 0.45f),
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = dateInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnBackground.copy(alpha = 0.55f)
                                )
                            }
                        }

                        // Location (festival only)
                        if (!location.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Mint900,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = location!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnBackground.copy(alpha = 0.55f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = OnBackground.copy(alpha = 0.08f))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Content
                        Text(
                            text = content.ifBlank { "내용이 없습니다." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnBackground.copy(alpha = 0.8f),
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                    }
                }
            }
        }
    }
}
