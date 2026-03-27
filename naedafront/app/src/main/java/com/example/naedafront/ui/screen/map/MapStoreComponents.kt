package com.example.naedafront.ui.screen.map

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.naedafront.R
import com.example.naedafront.data.remote.MapStoreResponseDto
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Navy900
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun StoreClusterBottomSheet(
    stores: List<MapStoreResponseDto>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onStoreClick: (MapStoreResponseDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedStores = remember(stores) {
        stores.sortedWith(
            compareByDescending<MapStoreResponseDto> { it.rating }
                .thenBy { it.storeName }
        )
    }

    // 수정: modifier.offset(y = (-6).dp)를 제거하여 하단에 밀착시킴
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color(0xFFF7F7F8),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(48.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.outline)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (sortedStores.size == 1) "식당 정보" else "주변 식당 리스트",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (sortedStores.size == 1) {
                            sortedStores.first().storeName
                        } else {
                            "마커 위치 기준 모아보기"
                        },
                        color = Color(0xFF4F74FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "총 ${sortedStores.size}개",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 4 }) togetherWith fadeOut()
                },
                label = "store_bottom_sheet_expand"
            ) { isExpanded ->
                if (isExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))

                        LazyColumn(
                            modifier = Modifier.heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(sortedStores, key = { it.storeId }) { store ->
                                StoreMapListRow(
                                    store = store,
                                    onClick = { onStoreClick(store) }
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
fun StoreDetailBottomSheet(
    store: MapStoreResponseDto,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color(0xFFF7F7F8),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(48.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.outline)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            StoreImageHero(
                imageUrl = store.imageUrl,
                categoryName = toMapCategoryLabel(store.categoryName),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = store.storeName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                toMapCategoryLabel(store.categoryName)?.let { categoryName ->
                    StoreMetaChip(categoryName)
                }
                if (store.facePayEnabled) {
                    StoreMetaChip("FACE PAY", background = Color(0xFFE8F0FF), content = Color(0xFF4F74FF))
                }
                if (store.isLocalBusiness) {
                    StoreMetaChip("구미 로컬", background = MaterialTheme.colorScheme.primaryContainer, content = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = store.description?.takeIf { it.isNotBlank() } ?: "등록된 설명이 없습니다.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(18.dp))

            StoreDetailLine("주소", store.roadAddress ?: store.numberAddress ?: "주소 정보 없음")
            StoreDetailLine("전화", store.phone ?: "전화번호 정보 없음")
            StoreDetailLine(
                "평점",
                if (store.rating > 0.0) String.format("%.1f", store.rating) else "평점 없음"
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                StoreMetaChip(
                    text = "닫기",
                    background = Color(0xFF152341),
                    content = Color.White,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }
        }
    }
}

@Composable
private fun StoreMapListRow(
    store: MapStoreResponseDto,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            StoreThumbnail(
                imageUrl = store.imageUrl,
                categoryName = toMapCategoryLabel(store.categoryName),
                modifier = Modifier.size(width = 96.dp, height = 96.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = store.storeName,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (store.rating > 0.0) String.format("%.1f", store.rating) else "평점 없음",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    toMapCategoryLabel(store.categoryName)?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "· $it",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = store.roadAddress ?: store.numberAddress ?: "주소 정보 없음",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (store.facePayEnabled || store.isLocalBusiness) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (store.facePayEnabled) {
                            StoreMetaChip("FACE PAY", background = Color(0xFFE8F0FF), content = Color(0xFF4F74FF))
                        }
                        if (store.isLocalBusiness) {
                            StoreMetaChip("구미 로컬", background = MaterialTheme.colorScheme.primaryContainer, content = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
    }
}

@Composable
private fun StoreImageHero(
    imageUrl: String?,
    categoryName: String?,
    modifier: Modifier = Modifier
) {
    val resolvedImageUrl = toUsableMapImageUrl(imageUrl)
    val imageBitmap by rememberNetworkImage(url = resolvedImageUrl)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFE8E2D9)),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (resolvedImageUrl == null) {
            StoreNoImageFallback()
        } else {
            PlaceholderStoreGraphic(categoryName = categoryName)
        }
    }
}

@Composable
private fun StoreThumbnail(
    imageUrl: String?,
    categoryName: String?,
    modifier: Modifier = Modifier
) {
    val resolvedImageUrl = toUsableMapImageUrl(imageUrl)
    val imageBitmap by rememberNetworkImage(url = resolvedImageUrl)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFE8E2D9)),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (resolvedImageUrl == null) {
            StoreNoImageFallback()
        } else {
            PlaceholderStoreGraphic(categoryName = categoryName)
        }
    }
}

@Composable
private fun StoreNoImageFallback() {
    Image(
        painter = painterResource(id = R.drawable.no_image),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PlaceholderStoreGraphic(categoryName: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if ((categoryName ?: "").contains("제과")) "BAKERY" else "STORE",
            color = Color(0xFF7C6752),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = categoryName ?: "식당",
            color = Color(0xFF5F4B38),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StoreMetaChip(
    text: String,
    background: Color = Color(0xFFF1F5F9),
    content: Color = Color(0xFF475569),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = content,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StoreDetailLine(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun rememberNetworkImage(url: String?): androidx.compose.runtime.State<ImageBitmap?> {
    return produceState<ImageBitmap?>(initialValue = null, url) {
        if (url.isNullOrBlank()) {
            value = null
            return@produceState
        }

        value = runCatching {
            withContext(Dispatchers.IO) {
                URL(url).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }
        }.getOrNull()
    }
}
