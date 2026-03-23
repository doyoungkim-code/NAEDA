package com.example.naedafront.ui.screen.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.naedafront.R
import com.example.naedafront.data.remote.MapStoreResponseDto
import com.example.naedafront.data.remote.StoreMapRepository
import com.example.naedafront.ui.common.NaedaButton
import com.example.naedafront.ui.common.NaedaButtonType
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint50
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Navy900
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import com.example.naedafront.ui.theme.OutlineVariant
import kotlin.math.roundToInt

data class Restaurant(
    val name: String,
    val category: String,
    val menu: String,
    val desc: String,
    val tags: List<String>
)

data class MapRegion(
    val label: String,
    val points: List<Offset>,
    val color: Color,
    val restaurants: List<Restaurant>
)

private data class StoreMapFilterState(
    val selectedCategory: String? = null,
    val facePayOnly: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = selectedCategory != null || facePayOnly

    val activeFilterCount: Int
        get() = (if (selectedCategory != null) 1 else 0) + (if (facePayOnly) 1 else 0)
}

private fun parsePoints(raw: String): List<Offset> =
    raw.trim().split(" ").map {
        val (x, y) = it.split(",")
        Offset(x.toFloat(), y.toFloat())
    }

private val REGIONS: List<MapRegion> = listOf(
    MapRegion(
        label = "무을면",
        points = parsePoints("89,195 110,182 132,174 174,169 172,185 178,200 191,213 207,211 227,207 214,223 219,241 219,249 219,263 212,271 202,276 185,261 183,245 152,241 143,237 132,238 130,210 118,217 101,210 90,200"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("무을손두부", "한식", "순두부찌개", "직접 만든 두부로 끓인 구수한 시골 순두부", listOf("FACE PAY", "도보 5분")),
            Restaurant("황토가마솥", "한식", "가마솥밥 정식", "황토가마에 지은 밥과 15가지 반찬", listOf("인기", "도보 8분")),
            Restaurant("무을냉면", "분식", "물냉면·비빔냉면", "40년 전통 냉면집, 여름 필수 코스", listOf("노포", "도보 10분"))
        )
    ),
    MapRegion(
        label = "도개면",
        points = parsePoints("237,117 236,135 238,150 243,166 280,189 292,208 301,216 304,222 322,225 337,222 361,225 372,218 377,205 367,189 353,178 342,164 321,142 313,126 285,112 267,128 253,136 243,120"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("도개막걸리촌", "한식", "파전·두부김치", "낙동강 뷰 맛집, 막걸리와 찰떡궁합", listOf("강변", "도보 6분")),
            Restaurant("도개한우마을", "한식", "한우구이", "도개면 직영 한우 저렴하게 즐기기", listOf("한우", "도보 9분")),
            Restaurant("낙동강쉼터", "분식", "어묵국물·떡볶이", "강변 드라이브 후 들르기 좋은 간식집", listOf("간식", "도보 12분"))
        )
    ),
    MapRegion(
        label = "옥성면",
        points = parsePoints("178,166 189,151 196,127 211,109 232,110 232,135 235,164 245,169 276,188 287,201 283,227 271,241 243,259 235,260 222,235 217,213 193,195 187,192 179,170"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("옥성오리훈제", "한식", "훈제오리구이", "직화 훈제 오리, 쌈채소 무한 리필", listOf("인기", "도보 4분")),
            Restaurant("산골밥상", "한식", "산채비빔밥", "옥성면 직접 채취 나물로 만든 비빔밥", listOf("건강식", "도보 7분")),
            Restaurant("옥성짬뽕집", "중식", "짬뽕·볶음밥", "시원한 국물 짬뽕 동네 소문난 집", listOf("중식", "도보 10분"))
        )
    ),
    MapRegion(
        label = "선산읍",
        points = parsePoints("193,277 219,263 220,238 231,244 236,255 245,255 274,245 281,238 283,228 308,219 314,228 319,252 327,281 337,313 336,321 324,326 295,324 277,324 260,310 247,314 250,322 250,331 242,331 228,317 222,304 225,295 214,287 205,285 193,279"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("선산곰탕", "한식", "곰탕·수육", "50년 전통 선산 대표 곰탕 노포", listOf("FACE PAY", "도보 3분")),
            Restaurant("선산닭갈비", "한식", "춘천식 닭갈비", "불 맛 살아있는 철판 닭갈비", listOf("매콤", "도보 8분")),
            Restaurant("읍내칼국수", "한식", "칼국수·만두", "손으로 민 칼국수, 점심 줄 서는 집", listOf("노포", "도보 11분"))
        )
    ),
    MapRegion(
        label = "해평면",
        points = parsePoints("312,224 355,223 367,225 382,243 396,250 405,263 403,280 403,297 394,307 386,325 383,332 373,337 362,348 363,363 363,373 358,375 348,357 331,335 331,319"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("해평어죽집", "한식", "어죽·매운탕", "낙동강 민물고기 어죽 원조집", listOf("대표", "도보 5분")),
            Restaurant("해평쌈밥", "한식", "쌈밥정식", "제철 채소 20가지 쌈채소 무한제공", listOf("인기", "도보 9분")),
            Restaurant("강변숯불갈비", "한식", "숯불갈비", "해평 낙동강 강변 뷰 숯불갈비", listOf("뷰", "도보 13분"))
        )
    ),
    MapRegion(
        label = "고아읍",
        points = parsePoints("228,375 254,373 274,380 290,380 327,380 338,376 358,373 345,347 341,331 305,319 268,320 258,308 252,313 251,329 251,337 247,341 239,359 247,372 230,376"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("고아돼지국밥", "한식", "돼지국밥", "진한 사골 육수 고아읍 명물 국밥", listOf("아침", "도보 5분")),
            Restaurant("고아족발보쌈", "한식", "족발·보쌈", "수제 족발 1위, 모임 단골집", listOf("모임", "도보 9분")),
            Restaurant("고아중화요리", "중식", "짜장·탕수육", "3대 운영 중, 탕수육은 꼭 주문", listOf("중식", "도보 14분"))
        )
    ),
    MapRegion(
        label = "시내동지구",
        points = parsePoints("221,373 303,373 345,375 369,375 399,365 415,375 429,393 426,413 418,435 391,436 368,429 357,422 335,437 330,454 318,451 282,423 276,410 258,412 246,405 235,396 223,380"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("구미역전곱창", "한식", "곱창·막창구이", "구미 원조 곱창골목 대표 맛집", listOf("야식", "도보 5분")),
            Restaurant("원조부대찌개", "한식", "부대찌개", "30년 전통, 라면사리 무한 추가", listOf("FACE PAY", "도보 8분")),
            Restaurant("시내일식", "일식", "초밥·라멘", "구미 시내 가성비 일식당", listOf("일식", "도보 6분"))
        )
    ),
    MapRegion(
        label = "산동면",
        points = parsePoints("364,361 364,342 380,326 405,301 402,257 428,253 441,269 453,289 464,296 468,313 454,328 444,334 436,345 422,372 419,374 410,367 396,361 385,367 377,368 370,365 367,361"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("산동 고기집 본점", "한식", "숙성 삼겹살", "숙성육과 깔끔한 반찬 구성이 강점인 인기 매장", listOf("FACE PAY", "도보 5분")),
            Restaurant("원평동 파스타 마켓", "양식", "파스타·리조또", "편한 분위기의 캐주얼 파스타 매장", listOf("도보 12분")),
            Restaurant("산동숯불닭갈비", "한식", "숯불닭갈비", "양 많고 저렴해 재방문이 많은 곳", listOf("가성비", "도보 9분"))
        )
    ),
    MapRegion(
        label = "장천면",
        points = parsePoints("467,290 485,288 502,308 500,321 483,330 490,355 492,370 499,396 452,391 438,391 418,376 423,360 437,342 466,332 469,313 462,296 460,286"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("장천순대국", "한식", "순대국밥", "직접 만든 수제 순대, 국물 진함", listOf("수제", "도보 4분")),
            Restaurant("장천갈비탕", "한식", "갈비탕·갈비찜", "뚝배기 갈비탕 한 그릇에 든든하게", listOf("보양식", "도보 7분")),
            Restaurant("장천두부전골", "한식", "두부전골·삼겹살", "직접 만든 두부로 끓인 전골", listOf("전골", "도보 10분"))
        )
    )
)

private fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val xi = polygon[i].x
        val yi = polygon[i].y
        val xj = polygon[j].x
        val yj = polygon[j].y

        if ((yi > point.y) != (yj > point.y) &&
            point.x < (xj - xi) * (point.y - yi) / (yj - yi) + xi
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

@Composable
fun MapSelectScreen(
    onBack: () -> Unit,
    onRestaurantClick: (region: MapRegion, restaurantName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedRegion by remember { mutableStateOf<MapRegion?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isSheetExpanded by remember { mutableStateOf(true) }
    var selectedStoreCluster by remember { mutableStateOf<List<MapStoreResponseDto>>(emptyList()) }
    var selectedStoreDetail by remember { mutableStateOf<MapStoreResponseDto?>(null) }
    var isStoreSheetExpanded by remember { mutableStateOf(true) }
    var mapStores by remember { mutableStateOf<List<MapStoreResponseDto>>(emptyList()) }
    var mapStoresReloadKey by remember { mutableIntStateOf(0) }
    var isMapStoresLoading by remember { mutableStateOf(true) }
    var mapStoresError by remember { mutableStateOf<String?>(null) }
    var currentLocationRequestKey by remember { mutableIntStateOf(0) }
    var hasLocationPermission by remember {
        mutableStateOf(hasLocationPermission(context))
    }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var showStoreFilterDialog by remember { mutableStateOf(false) }
    var storeFilterState by remember { mutableStateOf(StoreMapFilterState()) }

    val availableCategories = remember(mapStores) {
        mapStores.mapNotNull { store ->
            store.categoryName?.takeIf { category -> category.isNotBlank() }
        }.distinct().sorted()
    }
    val filteredMapStores = remember(mapStores, storeFilterState) {
        mapStores.filter { store ->
            val categoryMatches = storeFilterState.selectedCategory == null ||
                store.categoryName == storeFilterState.selectedCategory
            val facePayMatches = !storeFilterState.facePayOnly || store.facePayEnabled
            categoryMatches && facePayMatches
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        hasLocationPermission = granted
        if (granted) {
            currentLocationRequestKey += 1
        }
    }

    LaunchedEffect(mapStoresReloadKey) {
        isMapStoresLoading = true
        mapStoresError = null
        runCatching {
            StoreMapRepository.getMapStores()
        }.onSuccess { stores ->
            mapStores = stores
        }.onFailure { throwable ->
            mapStoresError = throwable.message ?: "식당 정보를 불러오지 못했습니다."
        }
        isMapStoresLoading = false
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                hasLocationPermission = hasLocationPermission(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(filteredMapStores) {
        val visibleStoreIds = filteredMapStores.map { it.storeId }.toSet()
        if (selectedStoreCluster.isNotEmpty()) {
            selectedStoreCluster = selectedStoreCluster.filter { it.storeId in visibleStoreIds }
        }
        selectedStoreDetail = selectedStoreDetail?.takeIf { it.storeId in visibleStoreIds }
    }

    LaunchedEffect(selectedTabIndex, hasLocationPermission) {
        if (selectedTabIndex == 0) {
            if (!hasLocationPermission) {
                showLocationPermissionDialog = true
            } else {
                currentLocationRequestKey += 1
            }
        } else {
            showLocationPermissionDialog = false
            selectedStoreCluster = emptyList()
            selectedStoreDetail = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {
        NaverRestaurantMapScreen(
            stores = filteredMapStores,
            hasLocationPermission = hasLocationPermission,
            currentLocationRequestKey = currentLocationRequestKey,
            onStoreClusterSelected = { stores ->
                selectedStoreCluster = stores
                selectedStoreDetail = null
                isStoreSheetExpanded = true
            },
            onMapTap = {
                selectedStoreCluster = emptyList()
                selectedStoreDetail = null
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (selectedTabIndex == 0) 1f else 0f)
        )

        if (selectedTabIndex == 0 && !hasLocationPermission) {
            LocationPermissionHintCard(
                onActionClick = { showLocationPermissionDialog = true },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            )
        }

        if (selectedTabIndex == 0 && isMapStoresLoading) {
            StoreMapStatusOverlay(
                title = "식당 지도를 불러오는 중이에요",
                message = "구미 매장 정보를 지도에 배치하고 있습니다.",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            )
        }

        if (selectedTabIndex == 0 && !isMapStoresLoading && !mapStoresError.isNullOrBlank()) {
            StoreMapStatusOverlay(
                title = "식당 정보를 불러오지 못했어요",
                message = mapStoresError.orEmpty(),
                actionText = "다시 시도",
                onActionClick = { mapStoresReloadKey += 1 },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            )
        }

        if (
            selectedTabIndex == 0 &&
            !isMapStoresLoading &&
            mapStoresError.isNullOrBlank()
        ) {
            StoreMapFilterButton(
                activeFilterCount = storeFilterState.activeFilterCount,
                onClick = { showStoreFilterDialog = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 84.dp, end = 20.dp)
            )
        }

        if (selectedTabIndex == 0) {
            CurrentLocationFab(
                onClick = {
                    if (hasLocationPermission) {
                        currentLocationRequestKey += 1
                    } else {
                        showLocationPermissionDialog = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 28.dp)
            )
        }

        if (selectedTabIndex == 0 && selectedStoreCluster.isNotEmpty()) {
            StoreClusterBottomSheet(
                stores = selectedStoreCluster,
                expanded = isStoreSheetExpanded,
                onToggleExpanded = { isStoreSheetExpanded = !isStoreSheetExpanded },
                onStoreClick = { selectedStoreDetail = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }

        if (selectedTabIndex == 1) {
            PopularRestaurantMapTab(
                selectedRegion = selectedRegion,
                isSheetExpanded = isSheetExpanded,
                onRegionSelected = {
                    selectedRegion = it
                    isSheetExpanded = true
                },
                onRegionCleared = { selectedRegion = null },
                onToggleExpanded = { isSheetExpanded = !isSheetExpanded },
                onRestaurantClick = { region, restaurantName ->
                    onRestaurantClick(region, restaurantName)
                }
            )
        }

        TopMapHeader(
            selectedTabIndex = selectedTabIndex,
            onBack = onBack,
            onTabSelected = { selectedTabIndex = it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )

        if (showLocationPermissionDialog) {
            LocationPermissionDialog(
                onDismiss = { showLocationPermissionDialog = false },
                onConfirm = {
                    showLocationPermissionDialog = false
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )
        }

        if (showStoreFilterDialog) {
            StoreMapFilterDialog(
                currentState = storeFilterState,
                categories = availableCategories,
                onDismiss = { showStoreFilterDialog = false },
                onApply = { nextState ->
                    storeFilterState = nextState
                    showStoreFilterDialog = false
                    selectedStoreCluster = emptyList()
                    selectedStoreDetail = null
                },
                onReset = {
                    storeFilterState = StoreMapFilterState()
                    showStoreFilterDialog = false
                    selectedStoreCluster = emptyList()
                    selectedStoreDetail = null
                }
            )
        }

        selectedStoreDetail?.let { store ->
            StoreDetailDialog(
                store = store,
                onDismiss = { selectedStoreDetail = null }
            )
        }
    }
}

@Composable
private fun TopMapHeader(
    selectedTabIndex: Int,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBack,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = Color(0xFF30384A)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        SegmentedTabs(
            selectedTabIndex = selectedTabIndex,
            items = listOf("식당", "맛집"),
            onSelected = onTabSelected
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun LocationPermissionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Background,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Mint50),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = null,
                        tint = Mint500,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "내 주변 식당을 바로 찾을게요",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "현재 위치를 기준으로 식당 지도를 보여주려면 위치 권한이 필요합니다. 허용하면 내 주변 식당으로 지도가 바로 이동합니다.",
                    color = OnSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NaedaButton(
                        text = "나중에",
                        onClick = onDismiss,
                        type = NaedaButtonType.OUTLINED,
                        modifier = Modifier.weight(1f)
                    )
                    NaedaButton(
                        text = "권한 허용",
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationPermissionHintCard(
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Background.copy(alpha = 0.96f),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Mint50),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = Mint500
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "위치 권한이 필요합니다",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = OnBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "권한을 허용하면 지금 위치를 기준으로 식당 지도를 바로 보여드립니다.",
                color = OnSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            NaedaButton(
                text = "위치 권한 허용하기",
                onClick = onActionClick
            )
        }
    }
}


@Composable
private fun StoreMapStatusOverlay(
    title: String,
    message: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Background.copy(alpha = 0.96f),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Mint500,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = OnBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                color = OnSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (!actionText.isNullOrBlank() && onActionClick != null) {
                Spacer(modifier = Modifier.height(18.dp))
                NaedaButton(
                    text = actionText,
                    onClick = onActionClick
                )
            }
        }
    }
}
@Composable
private fun CurrentLocationFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        shape = CircleShape,
        color = Background.copy(alpha = 0.96f),
        shadowElevation = 12.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.GpsFixed,
                contentDescription = "현재 위치로 이동",
                tint = Mint500,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun StoreMapFilterButton(
    activeFilterCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = activeFilterCount > 0

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = if (isActive) Mint500 else Background.copy(alpha = 0.96f),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = "지도 필터",
                tint = if (isActive) Color.White else Navy900,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (isActive) "필터 $activeFilterCount" else "필터",
                color = if (isActive) Color.White else Navy900,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StoreMapFilterDialog(
    currentState: StoreMapFilterState,
    categories: List<String>,
    onDismiss: () -> Unit,
    onApply: (StoreMapFilterState) -> Unit,
    onReset: () -> Unit
) {
    var selectedCategory by remember(currentState) { mutableStateOf(currentState.selectedCategory) }
    var facePayOnly by remember(currentState) { mutableStateOf(currentState.facePayOnly) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Background,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "지도 필터",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "카테고리",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("전체") }
                    )
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = OutlineVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "페이스페이 가능 매장만 보기",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "페이스페이 결제가 가능한 매장만 지도에 표시합니다.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = OnSurfaceVariant
                        )
                    }

                    Switch(
                        checked = facePayOnly,
                        onCheckedChange = { facePayOnly = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NaedaButton(
                        text = "초기화",
                        onClick = onReset,
                        type = NaedaButtonType.OUTLINED,
                        modifier = Modifier.weight(1f)
                    )
                    NaedaButton(
                        text = "적용",
                        onClick = {
                            onApply(
                                StoreMapFilterState(
                                    selectedCategory = selectedCategory,
                                    facePayOnly = facePayOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedTabs(
    selectedTabIndex: Int,
    items: List<String>,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(24.dp))
            .padding(4.dp)
    ) {
        items.forEachIndexed { index, label ->
            val selected = index == selectedTabIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Color(0xFF152341) else Color.Transparent)
                    .clickable { onSelected(index) }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.White else Color(0xFF8A94A6),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BottomStoreSheet(
    region: MapRegion,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = region.restaurants.size * 43

    Surface(
        modifier = modifier.offset(y = (-6).dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = Color(0xFFF7F7F8),
        shadowElevation = 14.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
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
                        .background(Color(0xFFD1D5DB))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${region.label} 맛집 리스트",
                        color = Color(0xFF1F2937),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "인기순 ⌄",
                        color = Color(0xFF4F74FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "총 ${totalCount}개",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 4 }) togetherWith fadeOut()
                },
                label = "bottom_sheet_expand"
            ) { isExpanded ->
                if (isExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))

                        LazyColumn(
                            modifier = Modifier.heightIn(max = 250.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(region.restaurants) { restaurant ->
                                StoreListRow(
                                    restaurant = restaurant,
                                    onClick = { onRestaurantClick(restaurant.name) }
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
private fun StoreListRow(
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFE8E2D9)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.gumi_map_select),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFB8A48B).copy(alpha = 0.35f))
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = restaurant.name,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222B45),
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
                        text = "4.8",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = " (1,204)",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "· ${restaurant.category}",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    restaurant.tags.forEach { tag ->
                        if (tag == "FACE PAY") {
                            FacePayChip()
                        } else {
                            Text(
                                text = tag,
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = "찜",
                tint = Color(0xFFCBD5E1),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
    }
}

@Composable
private fun FacePayChip() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8F0FF))
            .border(1.dp, Color(0xFFC9D9FF), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "FACE PAY",
            color = Color(0xFF4F74FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PopularRestaurantMapTab(
    selectedRegion: MapRegion?,
    isSheetExpanded: Boolean,
    onRegionSelected: (MapRegion) -> Unit,
    onRegionCleared: () -> Unit,
    onToggleExpanded: () -> Unit,
    onRestaurantClick: (MapRegion, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = ImageBitmap.imageResource(context.resources, R.drawable.gumi_map_select)
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    fun scalePoint(point: Offset): Offset {
        if (canvasSize == Size.Zero) return point
        return Offset(
            point.x * canvasSize.width / 600f,
            point.y * canvasSize.height / 600f
        )
    }

    fun scaledPolygon(region: MapRegion): List<Offset> = region.points.map(::scalePoint)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.92f)
                    .onSizeChanged {
                        canvasSize = Size(it.width.toFloat(), it.height.toFloat())
                    }
                    .pointerInput(canvasSize) {
                        detectTapGestures { tapOffset ->
                            val tappedRegion = REGIONS.firstOrNull { region ->
                                pointInPolygon(tapOffset, scaledPolygon(region))
                            }

                            if (tappedRegion != null) {
                                onRegionSelected(tappedRegion)
                            } else {
                                onRegionCleared()
                            }
                        }
                    }
            ) {
                drawImage(
                    image = bitmap,
                    dstSize = IntSize(
                        width = size.width.roundToInt(),
                        height = size.height.roundToInt()
                    )
                )

                REGIONS.forEach { region ->
                    val scaled = scaledPolygon(region)
                    if (scaled.isEmpty()) return@forEach

                    val isSelected = selectedRegion?.label == region.label
                    val cx = scaled.map { it.x }.average().toFloat()
                    val cy = scaled.map { it.y }.average().toFloat()

                    drawContext.canvas.nativeCanvas.drawText(
                        region.label,
                        cx,
                        cy + 4f,
                        Paint().apply {
                            color = if (isSelected) {
                                android.graphics.Color.parseColor("#5B5CEB")
                            } else {
                                android.graphics.Color.WHITE
                            }
                            textSize = if (isSelected) 30f else 28f
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = true
                            setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
                        }
                    )
                }
            }
        }

        if (selectedRegion != null) {
            BottomStoreSheet(
                region = selectedRegion,
                expanded = isSheetExpanded,
                onToggleExpanded = onToggleExpanded,
                onRestaurantClick = { restaurantName ->
                    onRestaurantClick(selectedRegion, restaurantName)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED



