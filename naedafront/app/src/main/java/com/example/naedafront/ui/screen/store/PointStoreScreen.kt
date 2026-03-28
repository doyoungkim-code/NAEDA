package com.example.naedafront.ui.screen.store

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

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
    onPurchaseClick: (StoreItem) -> Unit = {},
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
    var showInsufficientPointDialog by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

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
    }.sortedWith(
        compareBy<StoreItem> { it.title.trim() }
            .thenBy { it.brand.trim() }
    )

    val collapseFraction by remember {
        derivedStateOf {
            when {
                gridState.firstVisibleItemIndex > 0 -> 1f
                else -> (gridState.firstVisibleItemScrollOffset / 180f).coerceIn(0f, 1f)
            }
        }
    }
    val headerHeight by animateDpAsState(
        targetValue = lerp(236.dp, 18.dp, collapseFraction),
        label = "pointStoreHeaderHeight"
    )
    val categoryRowVerticalPadding by animateDpAsState(
        targetValue = if (collapseFraction > 0.5f) 8.dp else 10.dp,
        label = "pointStoreCategoryRowPadding"
    )
    val chipHorizontalPadding by animateDpAsState(
        targetValue = if (collapseFraction > 0.5f) 16.dp else 20.dp,
        label = "pointStoreCategoryChipHorizontalPadding"
    )
    val chipVerticalPadding by animateDpAsState(
        targetValue = if (collapseFraction > 0.5f) 9.dp else 12.dp,
        label = "pointStoreCategoryChipVerticalPadding"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .clipToBounds()
            ) {
                PointStoreHeader(
                    walletStatus = uiState.walletStatus,
                    point = uiState.pointBalance,
                    errorMessage = uiState.errorMessage,
                    onHistoryClick = onHistoryClick
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = categoryRowVerticalPadding),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.forEach { category ->
                    CategoryChip(
                        text = category.label,
                        selected = selectedCategory == category.id,
                        horizontalPadding = chipHorizontalPadding,
                        verticalPadding = chipVerticalPadding,
                        onClick = {
                            selectedCategory = category.id
                            coroutineScope.launch {
                                gridState.scrollToItem(0)
                            }
                        }
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
                            color = MaterialTheme.colorScheme.primary
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
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
                                onClick = {
                                    selectedItem = item
                                }
                            )
                        }
                    }
                }
            }
        }

        selectedItem?.let { item ->
            StoreItemDetailDialog(
                item = item,
                onDismiss = { selectedItem = null },
                onPurchaseClick = {
                    if (uiState.pointBalance < item.pricePoint) {
                        showInsufficientPointDialog = true
                    } else {
                        StoreOrderDraftStore.updateSelectedItem(item)
                        selectedItem = null
                        onPurchaseClick(item)
                    }
                }
            )
        }

        if (showInsufficientPointDialog) {
            AlertDialog(
                onDismissRequest = { showInsufficientPointDialog = false },
                confirmButton = {
                    TextButton(onClick = { showInsufficientPointDialog = false }) {
                        Text("확인")
                    }
                },
                title = {
                    Text("잔액 부족")
                },
                text = {
                    Text("포인트 잔액이 부족합니다.")
                }
            )
        }
    }
}

@Composable
private fun PointStoreHeader(
    walletStatus: PointWalletStatus,
    point: Long,
    errorMessage: String?,
    onHistoryClick: () -> Unit
) {
    val headerColor = MaterialTheme.colorScheme.primaryContainer
    val waveColor1 = Color(0xFF1F7D72)
    val waveColor2 = Color(0xFF0F645B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(236.dp)
            .background(headerColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
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
                    colors = listOf(waveColor1, waveColor2)
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "포인트 스토어",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                                color = MaterialTheme.colorScheme.primary
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
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "포인트 지갑을 생성하는 중입니다.",
                                color = MaterialTheme.colorScheme.onSurface,
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "%,d".format(point),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "P",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "내역보기",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable(onClick = onHistoryClick)
                            )
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
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = errorMessage ?: "잠시 후 다시 시도해 주세요.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 12.dp,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .wrapContentWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            ProductImage(
                imageUrl = item.imageUrl,
                thumbnailLabel = item.thumbnailLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(162.dp),
                badge = item.badge,
                requestWidth = 180.dp,
                requestHeight = 162.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = item.brand,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "%,d P".format(item.pricePoint),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun ProductImage(
    imageUrl: String,
    thumbnailLabel: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    imagePadding: Dp = 0.dp,
    requestWidth: Dp? = null,
    requestHeight: Dp? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val imageRequest = remember(imageUrl, requestWidth, requestHeight) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(false)
            .apply {
                if (requestWidth != null && requestHeight != null) {
                    size(
                        with(density) { requestWidth.roundToPx() },
                        with(density) { requestHeight.roundToPx() }
                    )
                }
            }
            .build()
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = thumbnailLabel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(imagePadding),
                contentScale = contentScale
            )
        } else {
            Text(
                text = thumbnailLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        badge?.let { badgeText ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE6F7F2))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color(0xFF0A8F72),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StoreItemDetailDialog(
    item: StoreItem,
    onDismiss: () -> Unit,
    onPurchaseClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "상품 상세",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ProductImage(
                    imageUrl = item.imageUrl,
                    thumbnailLabel = item.thumbnailLabel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Fit,
                    imagePadding = 12.dp,
                    requestWidth = 320.dp,
                    requestHeight = 320.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = item.brand,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = item.description.ifBlank { "상품 설명이 없습니다." },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoChip(label = "카테고리", value = item.category)
                    InfoChip(label = "재고", value = "${item.stockQuantity}개")
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "%,d P".format(item.pricePoint),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onPurchaseClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    enabled = item.stockQuantity > 0
                ) {
                    Text(
                        text = if (item.stockQuantity > 0) "구매하기" else "품절",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
