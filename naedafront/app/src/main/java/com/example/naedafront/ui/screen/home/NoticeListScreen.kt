package com.example.naedafront.ui.screen.home

import android.util.Log
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.naedafront.data.repository.NoticeRepository
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeListScreen(
    onBackClick: () -> Unit,
    onItemClick: (type: String, id: Long) -> Unit
) {
    var notices by remember { mutableStateOf<List<NoticeItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val items = mutableListOf<NoticeItem>()

        NoticeRepository.getAllFestivals()
            .onSuccess { festivals ->
                festivals.forEach { f ->
                    val startDate = f.startDate?.substring(5)?.replace("-", ".") ?: ""
                    val endDate = f.endDate?.substring(5)?.replace("-", ".") ?: ""
                    items.add(
                        NoticeItem(
                            id = f.festivalId ?: 0L,
                            type = "festival",
                            tag = "축제",
                            tagColor = Color(0xFFE91E63),
                            title = f.title ?: "",
                            content = f.description ?: "",
                            date = "$startDate ~ $endDate"
                        )
                    )
                }
            }
            .onFailure { Log.e("NoticeList", "축제 로드 실패: ${it.message}") }

        NoticeRepository.getAllNotices()
            .onSuccess { noticeList ->
                noticeList.forEach { n ->
                    val created = n.created?.substring(5, 10)?.replace("-", ".") ?: ""
                    items.add(
                        NoticeItem(
                            id = n.noticeId ?: 0L,
                            type = "notice",
                            tag = "공지",
                            tagColor = Color(0xFF1976D2),
                            title = n.title ?: "",
                            content = n.content ?: "",
                            date = created
                        )
                    )
                }
            }
            .onFailure { Log.e("NoticeList", "공지사항 로드 실패: ${it.message}") }

        notices = items
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Mint900,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "구미시 소식",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
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
        } else if (notices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "등록된 소식이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(notices) { notice ->
                    NoticeListItem(
                        notice = notice,
                        onClick = { onItemClick(notice.type, notice.id) }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun NoticeListItem(
    notice: NoticeItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(notice.tagColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = notice.tag,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = notice.tagColor
                    )
                }

                Text(
                    text = notice.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnBackground.copy(alpha = 0.45f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notice.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = OnBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (notice.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notice.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackground.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
