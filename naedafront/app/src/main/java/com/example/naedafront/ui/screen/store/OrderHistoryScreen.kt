package com.example.naedafront.ui.screen.store

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.data.remote.response.OrderResponse
import com.example.naedafront.ui.theme.Background
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OrderHistoryScreen(
    viewModel: OrderHistoryViewModel,
    onBackClick: () -> Unit,
    onOrderClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val hiddenOrderIds = remember { mutableStateListOf<Long>() }

    val visibleOrders = uiState.orders.filterNot { order ->
        hiddenOrderIds.contains(order.orderId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        OrderHistoryTopBar(onBackClick = onBackClick)

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "오류가 발생했습니다.",
                        color = Color(0xFFB00020),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            visibleOrders.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "주문 내역이 없습니다.",
                        color = Color(0xFF667085),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleOrders, key = { it.orderId }) { order ->
                        OrderHistoryBlock(
                            order = order,
                            imageUrl = uiState.productImageUrls[order.productId].orEmpty(),
                            onClick = { onOrderClick(order.orderId) },
                            onCancelClick = {
                                hiddenOrderIds.add(order.orderId)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderHistoryTopBar(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "배송 조회",
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "back"
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Background
            ),
            windowInsets = WindowInsets(0)
        )
    }
}

@Composable
private fun OrderHistoryBlock(
    order: OrderResponse,
    imageUrl: String,
    onClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(96.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF2F4F7)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = order.productName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF98A2B3),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalShipping,
                            contentDescription = null,
                            tint = Color(0xFF16A36A),
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "배송 준비중",
                            color = Color(0xFF16A36A),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = order.productName,
                        color = Color(0xFF111827),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoRow("주문번호", order.orderId.toString())
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("결제 포인트", formatPoint(order.pointPrice))
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("주문일시", formatOrderDate(order.orderAt))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFB42318)
                )
            ) {
                Text(
                    text = "배송취소",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFF98A2B3),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            color = Color(0xFF344054),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatPoint(point: Long): String {
    return "${NumberFormat.getNumberInstance(Locale.KOREA).format(point)}P"
}

private fun formatOrderDate(raw: String): String {
    return runCatching {
        raw.take(10).replace("-", ".")
    }.getOrElse { raw }
}
