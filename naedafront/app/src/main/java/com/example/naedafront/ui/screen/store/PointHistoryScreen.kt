package com.example.naedafront.ui.screen.store

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val StoreGreen = Color(0xFF005E54)

private data class MonthTab(
    val label: String,
    val selected: Boolean
)

private data class PointHistoryItem(
    val id: Int,
    val title: String,
    val dateText: String,
    val detailText: String,
    val pointText: String,
    val positive: Boolean,
    val iconText: String
)

@Composable
fun PointHistoryScreen(
    onBackClick: () -> Unit = {},
    onGiftClick: () -> Unit = {}
) {
    val months = remember {
        listOf(
            MonthTab("12월", true),
            MonthTab("11월", false),
            MonthTab("10월", false),
            MonthTab("9월", false)
        )
    }

    val historyItems = remember {
        listOf(
            PointHistoryItem(
                id = 1,
                title = "전자 기기 상점",
                dateText = "12월 24일 14:45",
                detailText = "결제 249,000원",
                pointText = "+12,450 P",
                positive = true,
                iconText = "가"
            ),
            PointHistoryItem(
                id = 2,
                title = "구미 로컬 카페",
                dateText = "12월 23일 09:12",
                detailText = "결제 6,500원",
                pointText = "+325 P",
                positive = true,
                iconText = "카"
            ),
            PointHistoryItem(
                id = 3,
                title = "상품권 전환",
                dateText = "12월 20일 18:30",
                detailText = "",
                pointText = "-10,000 P",
                positive = false,
                iconText = "전"
            ),
            PointHistoryItem(
                id = 4,
                title = "금오산 맛집",
                dateText = "12월 18일",
                detailText = "결제 32,000원",
                pointText = "+1,600 P",
                positive = true,
                iconText = "맛"
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F5F7))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            item {
                PointHistoryHeader(
                    point = 8500,
                    onBackClick = onBackClick
                )
            }

            item {
                ProgressSection()
            }

            item {
                MonthSection(months = months)
            }

            item {
                SummaryCard()
            }

            item {
                HistoryTitleRow()
            }

            items(historyItems) { item ->
                HistoryRow(item = item)
            }
        }

        FloatingActionButton(
            onClick = onGiftClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 92.dp),
            containerColor = Color(0xFF08A37A),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = "gift"
            )
        }
    }
}

@Composable
private fun PointHistoryHeader(
    point: Int,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(540.dp)
            .background(StoreGreen)
            .statusBarsPadding()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .align(Alignment.BottomCenter)
        ) {
            val w = size.width
            val h = size.height

            val path = Path().apply {
                moveTo(0f, h * 0.45f)
                cubicTo(
                    w * 0.22f, h * 0.10f,
                    w * 0.38f, h * 0.78f,
                    w * 0.60f, h * 0.52f
                )
                cubicTo(
                    w * 0.77f, h * 0.32f,
                    w * 0.90f, h * 0.78f,
                    w, h * 0.34f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(StoreGreen, StoreGreen)
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "back",
                        tint = Color.White
                    )
                }
            }



            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "info",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "페이스페이로 결제 시 적립됩니다.",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(230.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                )

                Box(
                    modifier = Modifier
                        .size(208.dp)
                        .clip(CircleShape)
                        .border(
                            width = 11.dp,
                            color = Color.White,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "현재 포인트",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "%,d".format(point),
                                color = Color.White,
                                fontSize = 50.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "P",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF202C49))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "10,000 P 이상부터 전환 가능합니다",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE9EEF0))
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "lock",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "구미사랑상품권으로 전환하기",
                    color = Color(0xFF94A3B8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProgressSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Text(
            text = "85% 완료",
            modifier = Modifier.align(Alignment.End),
            color = StoreGreen,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressNode(
                labelTop = "✓",
                labelBottom = "START",
                selected = true,
                faded = false
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(StoreGreen)
            )

            ProgressNode(
                labelTop = "○",
                labelBottom = "8.5K",
                selected = true,
                faded = false
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(Color(0xFFD8DEE6))
            )

            ProgressNode(
                labelTop = "권",
                labelBottom = "10,000 P",
                selected = false,
                faded = true
            )
        }
    }
}

@Composable
private fun ProgressNode(
    labelTop: String,
    labelBottom: String,
    selected: Boolean,
    faded: Boolean
) {
    val circleBg = when {
        faded -> Color(0xFFF0F3F6)
        selected -> StoreGreen
        else -> Color.White
    }

    val textColor = when {
        faded -> Color(0xFFB8C3D1)
        selected -> StoreGreen
        else -> Color(0xFF667085)
    }

    val topTextColor = if (faded) Color(0xFFB8C3D1) else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(circleBg)
                .border(
                    width = if (selected || faded) 0.dp else 1.dp,
                    color = Color(0xFFD0D5DD),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = labelTop,
                color = topTextColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = labelBottom,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MonthSection(
    months: List<MonthTab>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            months.forEach { month ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (month.selected) StoreGreen else Color.Transparent
                        )
                        .clickable { }
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = month.label,
                        color = if (month.selected) Color.White else Color(0xFF64748B),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F2F5)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "filter",
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SummaryCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "적립한 포인트",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "+12,450 P",
                    color = StoreGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(42.dp)
                    .background(Color(0xFFEAECEF))
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "12월 총 전환",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "-10,000 P",
                    color = Color(0xFF344054),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun HistoryTitleRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "상세 내역",
            color = Color(0xFF101828),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "SORTED BY DATE",
            color = Color(0xFFB0BAC8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HistoryRow(
    item: PointHistoryItem
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F2F5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.iconText,
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    color = if (item.positive) Color(0xFF111827) else Color(0xFF98A2B3),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row {
                    Text(
                        text = item.dateText,
                        color = Color(0xFF98A2B3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (item.detailText.isNotBlank()) {
                        Text(
                            text = " · ${item.detailText}",
                            color = Color(0xFF98A2B3),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = item.pointText,
                color = if (item.positive) StoreGreen else Color(0xFF98A2B3),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFEEF1F4)
        )
    }
}