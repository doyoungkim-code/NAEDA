package com.example.naedafront.ui.screen.store

import androidx.compose.material.icons.filled.Add
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private data class StoreCategory(
    val id: String,
    val label: String
)

data class StoreItem(
    val id: Long,
    val brand: String,
    val title: String,
    val description: String,
    val category: String,
    val pricePoint: Long,
    val stockQuantity: Int,
    val status: String,
    val imageUrl: String,
    val badge: String? = null,
    val thumbnailLabel: String
)

@Composable
fun PointStoreScreen(
    onHistoryClick: () -> Unit = {},
    onPurchaseClick: (StoreItem, Int) -> Unit = { _, _ -> },
    viewModel: StoreViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStoreData(context)
    }

    val categories = remember {
        listOf(
            StoreCategory("all", "전체"),
            StoreCategory("voucher", "구미 바우처"),
            StoreCategory("food", "식음료"),
            StoreCategory("digital", "디지털"),
            StoreCategory("ticket", "티켓")
        )
    }

    var selectedCategory by remember { mutableStateOf("all") }
    var selectedItem by remember { mutableStateOf<StoreItem?>(null) }

    val saleItems = uiState.items.filter { it.status == "ON_SALE" }

    val filteredItems = when (selectedCategory) {
        "voucher" -> saleItems.filter {
            it.category.contains("바우처") ||
                    it.category.contains("상품권") ||
                    it.title.contains("상품권")
        }

        "food" -> saleItems.filter {
            it.category.contains("식음료") ||
                    it.category.contains("음료") ||
                    it.category.contains("커피") ||
                    it.title.contains("커피") ||
                    it.title.contains("음료")
        }

        "digital" -> saleItems.filter {
            it.category.contains("디지털") ||
                    it.category.contains("금액권") ||
                    it.title.contains("금액권")
        }

        "ticket" -> saleItems.filter {
            it.category.contains("티켓") ||
                    it.title.contains("티켓")
        }

        else -> saleItems
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F5F7))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            PointStoreHeader(
                walletStatus = uiState.walletStatus,
                point = uiState.pointBalance,
                errorMessage = uiState.errorMessage,
                onHistoryClick = onHistoryClick,
                onCreateWalletClick = {
                    viewModel.createWallet(context)
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.forEach { category ->
                    CategoryChip(
                        text = category.label,
                        selected = selectedCategory == category.id,
                        onClick = { selectedCategory = category.id }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF006B60)
                        )
                    }
                }

                uiState.errorMessage != null &&
                        filteredItems.isEmpty() &&
                        uiState.walletStatus == PointWalletStatus.ERROR -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "데이터를 불러오지 못했습니다.",
                            color = Color(0xFF667085),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                filteredItems.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "등록된 상품이 없습니다.",
                            color = Color(0xFF98A2B3),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 0.dp,
                            bottom = 120.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredItems) { item ->
                            ProductCard(
                                item = item,
                                onClick = { selectedItem = item }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        PurchaseBottomSheet(
            item = item,
            onDismiss = { selectedItem = null },
            onPurchaseClick = { quantity ->
                onPurchaseClick(item, quantity)
                selectedItem = null
            }
        )
    }
}

@Composable
private fun PointStoreHeader(
    walletStatus: PointWalletStatus,
    point: Long,
    errorMessage: String?,
    onHistoryClick: () -> Unit,
    onCreateWalletClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFF005E54))
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
                    colors = listOf(
                        Color(0xFF1F7D72),
                        Color(0xFF0F645B)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "포인트 스토어",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F2F4)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                when (walletStatus) {
                    PointWalletStatus.LOADING -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF006B60)
                            )
                        }
                    }

                    PointWalletStatus.CREATING -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF006B60)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "포인트 지갑을 생성하는 중입니다.",
                                color = Color(0xFF344054),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    PointWalletStatus.EXISTS -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 22.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "사용 가능한 포인트",
                                    color = Color(0xFF98A2B3),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "%,d".format(point),
                                        color = Color(0xFF006B60),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "P",
                                        color = Color(0xFF006B60),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                modifier = Modifier.clickable(onClick = onHistoryClick),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "history",
                                    tint = Color(0xFF667085),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "내역보기",
                                    color = Color(0xFF667085),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    PointWalletStatus.NOT_CREATED -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 22.dp)
                        ) {
                            Text(
                                text = "포인트 지갑이 아직 없습니다.",
                                color = Color(0xFF101828),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "포인트를 적립하고 사용하려면 먼저 포인트 지갑을 생성해야 합니다.",
                                color = Color(0xFF667085),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = onCreateWalletClick,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF006B60),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "포인트 지갑 생성하기",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    PointWalletStatus.ERROR -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 22.dp)
                        ) {
                            Text(
                                text = "포인트 정보를 불러오지 못했습니다.",
                                color = Color(0xFF101828),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = errorMessage ?: "잠시 후 다시 시도해 주세요.",
                                color = Color(0xFF667085),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) Color(0xFF006B60) else Color(0xFFECEEF1)
    val textColor = if (selected) Color.White else Color(0xFF7B8794)

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProductCard(
    item: StoreItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(162.dp)
                    .background(Color(0xFFF2F4F7)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.thumbnailLabel,
                    color = Color(0xFF98A2B3),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                item.badge?.let { badge ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE6F7F2))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color(0xFF0A8F72),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = item.brand,
                    color = Color(0xFF98A2B3),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.title,
                    color = Color(0xFF101828),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "%,d P".format(item.pricePoint),
                    color = Color(0xFF006B60),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseBottomSheet(
    item: StoreItem,
    onDismiss: () -> Unit,
    onPurchaseClick: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantity by remember(item.id) { mutableIntStateOf(1) }
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF7F7F8),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(52.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFD1D5DB))
            )
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "close",
                            tint = Color(0xFF98A2B3)
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        ProductPreviewSection(item = item)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 22.dp)
                        ) {
                            Text(
                                text = "상품 정보",
                                color = Color(0xFF111827),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            InfoRow(
                                label = "유효기간",
                                value = "발행일로부터 5년"
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            InfoRow(
                                label = "사용처",
                                value = if (item.title.contains("상품권")) {
                                    "구미시 내 가맹점"
                                } else {
                                    "해당 제휴처 사용 가능"
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "수량",
                                    color = Color(0xFF98A2B3),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                QuantitySelector(
                                    quantity = quantity,
                                    onDecrease = {
                                        if (quantity > 1) quantity--
                                    },
                                    onIncrease = {
                                        quantity++
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            Text(
                                text = "유의사항",
                                color = Color(0xFF111827),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (item.title.contains("상품권")) {
                                NoticeText("본 상품권은 실물 상품권 또는 모바일 상품권 형태로 제공될 수 있습니다.")
                                NoticeText("포인트 구매 후 변심에 의한 환불은 불가합니다.")
                                NoticeText("일부 매장에서는 사용이 제한될 수 있으니 미리 확인해 주세요.")
                                NoticeText("해당 지자체 정책에 따라 사용 범위가 변경될 수 있습니다.")
                            } else {
                                NoticeText("구매 후 변심에 의한 환불은 불가합니다.")
                                NoticeText("사용 가능 매장 및 조건은 제휴처 정책에 따릅니다.")
                                NoticeText("유효기간 경과 시 사용이 제한될 수 있습니다.")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F7F8))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { onPurchaseClick(quantity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00695C),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "포인트로 구매하기",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductPreviewSection(
    item: StoreItem
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF7F7F8))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF5EFE7))
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(146.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.thumbnailLabel,
                        color = Color(0xFF9CA3AF),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item.badge?.let { badge ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F7F2))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = badge,
                            color = Color(0xFF0C8C72),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = item.brand,
                color = Color(0xFF00695C),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                color = Color(0xFF111827),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "%,d P".format(item.pricePoint),
                color = Color(0xFF111827),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFFEAECEF)
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF98A2B3),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            color = Color(0xFF344054),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QuantitySelector(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircleActionButton(
            icon = Icons.Default.Remove,
            enabled = quantity > 1,
            onClick = onDecrease
        )

        Text(
            text = quantity.toString(),
            color = Color(0xFF111827),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        CircleActionButton(
            icon = Icons.Default.Add,
            enabled = true,
            onClick = onIncrease
        )
    }
}

@Composable
private fun CircleActionButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (enabled) Color(0xFFF3F4F6) else Color(0xFFF7F7F8))
            .border(
                width = 1.dp,
                color = if (enabled) Color(0xFFD0D5DD) else Color(0xFFE5E7EB),
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color(0xFF667085) else Color(0xFFC7CDD4),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun NoticeText(
    text: String
) {
    Text(
        text = text,
        color = Color(0xFF667085),
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}