package com.example.naedafront.ui.screen.store

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.naedafront.ui.theme.Background
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val StoreGreen = Color(0xFF005E54)

private enum class HistoryFilterType(val label: String) {
    ALL("전체"),
    EARN("적립"),
    USE("사용")
}

@Composable
fun PointHistoryScreen(
    onBackClick: () -> Unit = {},
    onGiftClick: () -> Unit = {},
    viewModel: PointHistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load(context)
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    var showPeriodSheet by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(HistoryFilterType.ALL) }

    val filteredHistoryItems = remember(
        uiState.items,
        uiState.selectedYear,
        uiState.selectedMonth,
        selectedFilter
    ) {
        uiState.items
            .filter { it.year == uiState.selectedYear }
            .filter { uiState.selectedMonth == null || it.month == uiState.selectedMonth }
            .filter {
                when (selectedFilter) {
                    HistoryFilterType.ALL -> true
                    HistoryFilterType.EARN -> it.positive
                    HistoryFilterType.USE -> !it.positive
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StoreGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                item {
                    PointHistoryHeader(
                        point = uiState.pointBalance,
                        onBackClick = onBackClick
                    )
                }

                item {
                    ProgressSection(
                        point = uiState.pointBalance
                    )
                }

                item {
                    PeriodSection(
                        selectedYear = uiState.selectedYear,
                        selectedMonth = uiState.selectedMonth,
                        onPeriodClick = { showPeriodSheet = true },
                        onFilterClick = { showFilterSheet = true }
                    )
                }

                item {
                    SummaryCard(
                        totalEarned = uiState.totalEarned,
                        totalUsed = uiState.totalUsed,
                        selectedYear = uiState.selectedYear,
                        selectedMonth = uiState.selectedMonth
                    )
                }

                item {
                    HistoryTitleRow()
                }

                if (filteredHistoryItems.isEmpty()) {
                    item {
                        EmptyHistorySection(
                            message = if (uiState.errorMessage != null) {
                                uiState.errorMessage ?: "기록이 없습니다."
                            } else {
                                "기록이 없습니다."
                            }
                        )
                    }
                } else {
                    items(filteredHistoryItems) { item ->
                        HistoryRow(item = item)
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            selectedFilter = selectedFilter,
            onSelectFilter = { selectedFilter = it },
            onDismiss = { showFilterSheet = false },
            onApply = { showFilterSheet = false }
        )
    }

    if (showPeriodSheet) {
        PeriodBottomSheet(
            selectedYear = uiState.selectedYear,
            selectedMonth = uiState.selectedMonth,
            onSelectYear = { viewModel.setYear(it) },
            onSelectMonth = { viewModel.setMonth(it) },
            onDismiss = { showPeriodSheet = false },
            onApply = { showPeriodSheet = false }
        )
    }
}

@Composable
private fun PointHistoryHeader(
    point: Long,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(540.dp)
            .background(StoreGreen)
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
                    fontSize = 11.sp,
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
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "%,d".format(point),
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "P",
                                color = Color.White,
                                fontSize = 16.sp,
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
                    fontSize = 10.sp,
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProgressSection(
    point: Long
) {
    val progress = (point.coerceAtMost(10_000L).toFloat() / 10_000f)
    val progressPercent = (progress * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Text(
            text = "${progressPercent}% 완료",
            modifier = Modifier.align(Alignment.End),
            color = StoreGreen,
            fontSize = 12.sp,
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
                labelBottom = "%,d".format(point),
                selected = true,
                faded = false
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(if (point >= 10_000L) StoreGreen else Color(0xFFD8DEE6))
            )

            ProgressNode(
                labelTop = "권",
                labelBottom = "10,000 P",
                selected = point >= 10_000L,
                faded = point < 10_000L
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = labelBottom,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PeriodSection(
    selectedYear: Int,
    selectedMonth: Int?,
    onPeriodClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .clickable(onClick = onPeriodClick),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(StoreGreen)
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            ) {
                Text(
                    text = selectedYear.toString(),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(StoreGreen)
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            ) {
                Text(
                    text = selectedMonth?.let { "${it}월" } ?: "전체",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F2F5))
                .clickable(onClick = onFilterClick),
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
private fun SummaryCard(
    totalEarned: Long,
    totalUsed: Long,
    selectedYear: Int,
    selectedMonth: Int?
) {
    val periodLabel = if (selectedMonth != null) {
        "${selectedYear}년 ${selectedMonth}월"
    } else {
        "${selectedYear}년 전체"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(
                text = periodLabel,
                color = Color(0xFF98A2B3),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "총 적립",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+%,d P".format(totalEarned),
                        color = StoreGreen,
                        fontSize = 15.sp,
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
                        text = "총 사용",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "-%,d P".format(totalUsed),
                        color = Color(0xFF344054),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
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
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.weight(1f))
        
    }
}

@Composable
private fun HistoryRow(
    item: PointHistoryItemUi
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0F2F5)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.iconText,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    color = if (item.positive) Color(0xFF111827) else Color(0xFF98A2B3),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row {
                    Text(
                        text = item.dateText,
                        color = Color(0xFF98A2B3),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (item.detailText.isNotBlank()) {
                        Text(
                            text = " · ${item.detailText}",
                            color = Color(0xFF98A2B3),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = item.pointText,
                color = if (item.positive) StoreGreen else Color(0xFF98A2B3),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFEEF1F4)
        )
    }
}

@Composable
private fun EmptyHistorySection(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color(0xFF98A2B3),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    selectedFilter: HistoryFilterType,
    onSelectFilter: (HistoryFilterType) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF7F7F7),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(52.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFD1D5DB))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "필터 설정",
                color = Color(0xFF111827),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(22.dp))

            FilterOptionRow(
                title = "전체",
                selected = selectedFilter == HistoryFilterType.ALL,
                onClick = { onSelectFilter(HistoryFilterType.ALL) }
            )

            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)

            FilterOptionRow(
                title = "적립",
                selected = selectedFilter == HistoryFilterType.EARN,
                onClick = { onSelectFilter(HistoryFilterType.EARN) }
            )

            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)

            FilterOptionRow(
                title = "사용",
                selected = selectedFilter == HistoryFilterType.USE,
                onClick = { onSelectFilter(HistoryFilterType.USE) }
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00695C),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "적용하기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodBottomSheet(
    selectedYear: Int,
    selectedMonth: Int?,
    onSelectYear: (Int) -> Unit,
    onSelectMonth: (Int?) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val years = listOf(2024, 2025, 2026)
    val months = (1..12).toList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF7F7F7),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(52.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFD1D5DB))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "기간 설정",
                color = Color(0xFF111827),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "연도",
                color = Color(0xFF6B7280),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                years.forEach { year ->
                    SelectChip(
                        text = year.toString(),
                        selected = selectedYear == year,
                        onClick = { onSelectYear(year) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "월",
                color = Color(0xFF6B7280),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectChip(
                    text = "전체",
                    selected = selectedMonth == null,
                    onClick = { onSelectMonth(null) }
                )

                months.forEach { month ->
                    SelectChip(
                        text = "${month}월",
                        selected = selectedMonth == month,
                        onClick = { onSelectMonth(month) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00695C),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "적용하기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) StoreGreen else Color(0xFFEFF2F6))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF64748B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FilterOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = if (selected) Color(0xFF00695C) else Color(0xFF6B7280),
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircleOutline,
                contentDescription = "selected",
                tint = Color(0xFF00695C),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}