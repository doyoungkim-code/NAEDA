package com.example.naedafront.ui.screen.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Shadow
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
import com.example.naedafront.data.remote.RecommendRepository
import com.example.naedafront.data.remote.RecommendResponseDto
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer

private const val MIN_REGION_RECOMMEND_STORE_COUNT = 5

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
    val restaurants: List<Restaurant>,
    val labelCenter: Offset? = null,
    val pinX: Float = 0f,  // 이미지 너비 기준 % (0~1)
    val pinY: Float = 0f   // 이미지 높이 기준 % (0~1)
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

private fun MapStoreResponseDto.toRecommendFallback(): RecommendResponseDto {
    return RecommendResponseDto(
        storeId = storeId,
        storeName = storeName,
        categoryName = categoryName,
        roadAddress = roadAddress,
        latitude = latitude,
        longitude = longitude,
        rating = rating,
        imageUrl = imageUrl,
        description = description,
        visitCount = 0,
        score = rating
    )
}

private fun mergeWithFeaturedFallbackStores(
    regionLabel: String,
    stores: List<RecommendResponseDto>,
    featuredStores: List<MapStoreResponseDto>
): List<RecommendResponseDto> {
    if (stores.size >= MIN_REGION_RECOMMEND_STORE_COUNT || featuredStores.isEmpty()) {
        return stores
    }

    val existingStoreIds = stores.map { it.storeId }.toSet()
    val existingNames = stores.map { it.storeName }.toSet()
    val shortage = MIN_REGION_RECOMMEND_STORE_COUNT - stores.size
    val startIndex = (regionLabel.hashCode() and Int.MAX_VALUE) % featuredStores.size
    val fallbackStores = mutableListOf<RecommendResponseDto>()

    for (offset in featuredStores.indices) {
        if (fallbackStores.size >= shortage) {
            break
        }

        val candidate = featuredStores[(startIndex + offset) % featuredStores.size]
        if (candidate.storeId in existingStoreIds || candidate.storeName in existingNames) {
            continue
        }
        if (fallbackStores.any { it.storeId == candidate.storeId || it.storeName == candidate.storeName }) {
            continue
        }

        fallbackStores += candidate.toRecommendFallback()
    }

    return stores + fallbackStores
}

private val REGIONS: List<MapRegion> = listOf(
    MapRegion(
        label = "무을면",
        labelCenter = Offset(150f, 230f),
        pinX = 0.20f, pinY = 0.20f,
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
        labelCenter = Offset(320f, 155f),
        pinX = 0.67f, pinY = 0.26f,
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
        labelCenter = Offset(210f, 165f),
        pinX = 0.42f, pinY = 0.20f,
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
        labelCenter = Offset(260f, 280f),
        pinX = 0.40f, pinY = 0.34f,
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
        labelCenter = Offset(370f, 290f),
        pinX = 0.63f, pinY = 0.50f,
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
        labelCenter = Offset(270f, 345f),
        pinX = 0.40f, pinY = 0.53f,
        points = parsePoints("228,375 254,373 274,380 290,380 327,380 338,376 358,373 345,347 341,331 305,319 268,320 258,308 252,313 251,329 251,337 247,341 239,359 247,372 230,376"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("고아돼지국밥", "한식", "돼지국밥", "진한 사골 육수 고아읍 명물 국밥", listOf("아침", "도보 5분")),
            Restaurant("고아족발보쌈", "한식", "족발·보쌈", "수제 족발 1위, 모임 단골집", listOf("모임", "도보 9분")),
            Restaurant("고아중화요리", "중식", "짜장·탕수육", "3대 운영 중, 탕수육은 꼭 주문", listOf("중식", "도보 14분"))
        )
    ),
    MapRegion(
        label = "선주원남동",
        pinX = 0.23f, pinY = 0.70f,
        points = parsePoints("221,373 303,373 345,375 369,375 399,365 415,375 429,393 426,413 418,435 391,436 368,429 357,422 335,437 330,454 318,451 282,423 276,410 258,412 246,405 235,396 223,380"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("구미역전곱창", "한식", "곱창·막창구이", "구미 원조 곱창골목 대표 맛집", listOf("야식", "도보 5분")),
            Restaurant("원조부대찌개", "한식", "부대찌개", "30년 전통, 라면사리 무한 추가", listOf("FACE PAY", "도보 8분")),
            Restaurant("선주원남 칼국수", "한식", "칼국수·만두", "직접 뽑은 면, 점심 줄 서는 집", listOf("노포", "도보 5분"))
        )
    ),
    MapRegion(
        label = "지산동",
        pinX = 0.41f, pinY = 0.69f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("지산동 돈까스", "일식", "돈까스·우동", "두꺼운 수제 돈까스 인기 맛집", listOf("인기", "도보 3분")),
            Restaurant("지산 커피하우스", "카페", "핸드드립·라떼", "조용한 분위기의 로스터리 카페", listOf("카페", "도보 5분"))
        )
    ),
    MapRegion(
        label = "송정동",
        pinX = 0.38f, pinY = 0.72f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("송정동 치킨집", "한식", "후라이드·양념", "바삭한 치킨으로 동네 소문난 집", listOf("야식", "도보 4분")),
            Restaurant("송정 분식당", "분식", "떡볶이·순대", "학생들이 줄 서는 분식 맛집", listOf("가성비", "도보 6분"))
        )
    ),
    MapRegion(
        label = "원평동",
        pinX = 0.35f, pinY = 0.69f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("원평동 파스타", "양식", "파스타·리조또", "편한 분위기의 캐주얼 파스타 매장", listOf("데이트", "도보 7분")),
            Restaurant("원평 곱창골목", "한식", "곱창·막창", "구미 대표 곱창 골목 원조", listOf("야식", "도보 3분"))
        )
    ),
    MapRegion(
        label = "광평동",
        pinX = 0.38f, pinY = 0.77f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("광평 삼겹살", "한식", "삼겹살·목살", "숙성 삼겹살 맛집, 회식 단골", listOf("모임", "도보 5분")),
            Restaurant("광평동 국밥", "한식", "순대국밥", "진한 국물 순대국 아침 든든하게", listOf("아침", "도보 8분"))
        )
    ),
    MapRegion(
        label = "공단동",
        pinX = 0.43f, pinY = 0.805f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("공단 백반집", "한식", "백반·찌개", "직장인 점심 단골 가성비 백반", listOf("가성비", "도보 3분")),
            Restaurant("공단동 중화요리", "중식", "짜장·짬뽕", "빠른 배달, 푸짐한 양의 중식당", listOf("중식", "도보 6분"))
        )
    ),
    MapRegion(
        label = "상모사곡동",
        pinX = 0.31f, pinY = 0.80f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("상모 감자탕", "한식", "감자탕·뼈해장국", "뼈 푹 고은 감자탕 전문점", listOf("보양식", "도보 5분")),
            Restaurant("사곡동 횟집", "한식", "회·매운탕", "싱싱한 활어회 전문", listOf("횟집", "도보 10분"))
        )
    ),
    MapRegion(
        label = "임오동",
        pinX = 0.34f, pinY = 0.86f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("임오동 닭갈비", "한식", "닭갈비·볶음밥", "매콤한 철판 닭갈비 맛집", listOf("매콤", "도보 4분")),
            Restaurant("임오 카페거리", "카페", "디저트·음료", "감성 카페 모여있는 거리", listOf("카페", "도보 7분"))
        )
    ),
    MapRegion(
        label = "양포동",
        pinX = 0.56f, pinY = 0.75f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("양포동 족발", "한식", "족발·보쌈", "쫄깃한 족발 야식 맛집", listOf("야식", "도보 5분")),
            Restaurant("양포 버거", "양식", "수제버거", "두꺼운 패티의 수제 버거 전문점", listOf("양식", "도보 8분"))
        )
    ),
    MapRegion(
        label = "진미동",
        pinX = 0.50f, pinY = 0.84f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("진미동 해장국", "한식", "해장국·선지국", "새벽부터 여는 해장 맛집", listOf("아침", "도보 3분")),
            Restaurant("진미 일식", "일식", "초밥·라멘", "구미 시내 가성비 일식당", listOf("일식", "도보 6분"))
        )
    ),
    MapRegion(
        label = "산동면",
        labelCenter = Offset(435f, 330f),
        pinX = 0.69f, pinY = 0.69f,
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
        labelCenter = Offset(470f, 365f),
        pinX = 0.83f, pinY = 0.84f,
        points = parsePoints("467,290 485,288 502,308 500,321 483,330 490,355 492,370 499,396 452,391 438,391 418,376 423,360 437,342 466,332 469,313 462,296 460,286"),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("장천순대국", "한식", "순대국밥", "직접 만든 수제 순대, 국물 진함", listOf("수제", "도보 4분")),
            Restaurant("장천갈비탕", "한식", "갈비탕·갈비찜", "뚝배기 갈비탕 한 그릇에 든든하게", listOf("보양식", "도보 7분")),
            Restaurant("장천두부전골", "한식", "두부전골·삼겹살", "직접 만든 두부로 끓인 전골", listOf("전골", "도보 10분"))
        )
    ),
    MapRegion(
        label = "신평동",
        pinX = 0.41f, pinY = 0.74f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("신평 감자탕", "한식", "감자탕·뼈해장국", "뼈 푹 고은 감자탕 전문점", listOf("보양식", "도보 5분")),
            Restaurant("신평동 칼국수", "한식", "칼국수·수제비", "손 반죽 칼국수 동네 맛집", listOf("노포", "도보 7분"))
        )
    ),
    MapRegion(
        label = "비산동",
        pinX = 0.44f, pinY = 0.72f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("비산동 삼겹살", "한식", "삼겹살·목살", "숙성 삼겹살 구이 전문점", listOf("모임", "도보 4분")),
            Restaurant("비산 커피숍", "카페", "핸드드립·라떼", "조용한 분위기의 감성 카페", listOf("카페", "도보 6분"))
        )
    ),
    MapRegion(
        label = "도량동",
        pinX = 0.35f, pinY = 0.65f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("도량동 국밥", "한식", "돼지국밥·순대국", "진한 사골 국물 든든한 국밥집", listOf("아침", "도보 3분")),
            Restaurant("도량 분식", "분식", "떡볶이·순대", "학생들 사이 인기 분식 맛집", listOf("가성비", "도보 5분"))
        )
    ),
    MapRegion(
        label = "형곡동",
        pinX = 0.315f, pinY = 0.75f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("형곡동 치킨", "한식", "후라이드·양념", "바삭한 치킨 배달 맛집", listOf("야식", "도보 4분")),
            Restaurant("형곡 파스타", "양식", "파스타·스테이크", "캐주얼 분위기의 양식 레스토랑", listOf("데이트", "도보 8분"))
        )
    ),
    MapRegion(
        label = "인동동",
        pinX = 0.58f, pinY = 0.87f,
        points = emptyList(),
        color = Color(0xFF5B5CEB),
        restaurants = listOf(
            Restaurant("인동 곱창", "한식", "곱창·막창", "불 맛 살린 철판 곱창 맛집", listOf("야식", "도보 5분")),
            Restaurant("인동동 초밥", "일식", "초밥·사시미", "신선한 재료의 가성비 초밥집", listOf("일식", "도보 7분"))
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
    showBackButton: Boolean = true,
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
            toMapCategoryLabel(store.categoryName)
        }.distinct().sorted()
    }
    val filteredMapStores = remember(mapStores, storeFilterState) {
        mapStores.filter { store ->
            val categoryMatches = storeFilterState.selectedCategory == null ||
                toMapCategoryLabel(store.categoryName) == storeFilterState.selectedCategory
            val facePayMatches = !storeFilterState.facePayOnly || store.facePayEnabled
            categoryMatches && facePayMatches
        }
    }
    val featuredRecommendFallbackStores = remember(mapStores) {
        mapStores
            .asSequence()
            .filter { it.facePayEnabled }
            .filter { toUsableMapImageUrl(it.imageUrl) != null }
            .sortedWith(
                compareByDescending<MapStoreResponseDto> { it.rating }
                    .thenBy { it.storeName }
            )
            .toList()
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

        if (selectedTabIndex == 0 && selectedStoreCluster.isNotEmpty() && selectedStoreDetail == null) {
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
                featuredFallbackStores = featuredRecommendFallbackStores,
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
            showBackButton = showBackButton,
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
            StoreDetailBottomSheet(
                store = store,
                onDismiss = { selectedStoreDetail = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TopMapHeader(
    selectedTabIndex: Int,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
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
            } else {
                Spacer(modifier = Modifier.size(40.dp))
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
}

@Composable
private fun MapRegionGuideHint(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "지역명을 터치해 맛집을 알아보세요!",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "확대하면 글씨와 지도를 크게 볼 수 있습니다.",
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF)
        )
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
    val density = LocalDensity.current
    var selectedCategory by remember(currentState) { mutableStateOf(currentState.selectedCategory) }
    var facePayOnly by remember(currentState) { mutableStateOf(currentState.facePayOnly) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var categoryFieldWidth by remember { mutableIntStateOf(0) }

    val pendingState = StoreMapFilterState(
        selectedCategory = selectedCategory,
        facePayOnly = facePayOnly
    )
    val selectedCategoryLabel = toMapCategoryLabel(selectedCategory) ?: "전체"
    val facePayFilterLabel = if (facePayOnly) "페이스페이 가능 매장만" else "전체 매장"

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.92f),
                shape = RoundedCornerShape(28.dp),
                color = Background,
                shadowElevation = 16.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "지도 필터",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )
                            }
                            Surface(
                                onClick = { onApply(pendingState) },
                                shape = RoundedCornerShape(16.dp),
                                color = Mint500
                            ) {
                                Text(
                                    text = "확인",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StoreMapFilterSummaryChip(
                                text = "카테고리 · $selectedCategoryLabel",
                                background = Color(0xFFF3F4FF),
                                contentColor = Color(0xFF4C51BF)
                            )
                            StoreMapFilterSummaryChip(
                                text = facePayFilterLabel,
                                background = if (facePayOnly) Color(0xFFE7F6EF) else Color(0xFFF8FAFC),
                                contentColor = if (facePayOnly) Mint500 else Navy900
                            )
                            if (pendingState != StoreMapFilterState()) {
                                StoreMapFilterActionChip(
                                    text = "초기화",
                                    onClick = onReset
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "카테고리",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OnBackground
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onSizeChanged { categoryFieldWidth = it.width },
                                        onClick = { categoryDropdownExpanded = !categoryDropdownExpanded },
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Mint500)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "카테고리 선택",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = OnSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = selectedCategoryLabel,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Navy900
                                                )
                                            }

                                            Text(
                                                text = if (categoryDropdownExpanded) "▴" else "▾",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Mint500
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = categoryDropdownExpanded,
                                        onDismissRequest = { categoryDropdownExpanded = false },
                                        modifier = Modifier
                                            .heightIn(max = 288.dp)
                                            .width(with(density) { categoryFieldWidth.toDp() })
                                            .background(Color.White)
                                    ) {
                                        StoreMapFilterDropdownItem(
                                            label = "전체",
                                            selected = selectedCategory == null,
                                            onClick = {
                                                selectedCategory = null
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                        categories.forEach { category ->
                                            StoreMapFilterDropdownItem(
                                                label = category,
                                                selected = selectedCategory == category,
                                                onClick = {
                                                    selectedCategory = category
                                                    categoryDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "페이스페이 매장만 보기",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OnBackground
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Switch(
                                        checked = facePayOnly,
                                        onCheckedChange = { facePayOnly = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreMapFilterSummaryChip(
    text: String,
    background: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StoreMapFilterActionChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            color = Navy900,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StoreMapFilterDropdownItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        modifier = Modifier.background(if (selected) Mint50 else Color.White),
        text = {
            Text(
                text = label,
                color = if (selected) Mint500 else Navy900,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        onClick = onClick
    )
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
        modifier = modifier,
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
private fun RecommendBottomSheet(
    regionLabel: String,
    stores: List<RecommendResponseDto>,
    isLoading: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onStoreClick: (RecommendResponseDto) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(stores) {
        stores.mapNotNull { toMapCategoryLabel(it.categoryName) }.distinct()
    }

    val filteredStores = remember(stores, selectedCategory) {
        if (selectedCategory == null) stores
        else stores.filter { toMapCategoryLabel(it.categoryName) == selectedCategory }
    }

    Surface(
        modifier = modifier,
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
                        text = "$regionLabel 맛집 리스트",
                        color = Color(0xFF1F2937),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "추천순",
                        color = Color(0xFF4F74FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "총 ${filteredStores.size}개",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (categories.size > 1) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryFilterChip(
                        label = "전체",
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                    categories.forEach { category ->
                        CategoryFilterChip(
                            label = category,
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            }
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    (fadeIn() + slideInVertically { it / 4 }) togetherWith fadeOut()
                },
                label = "recommend_sheet_expand"
            ) { isExpanded ->
                if (isExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color(0xFF5B5CEB)
                                )
                            }
                        } else if (filteredStores.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedCategory != null) "해당 카테고리의 맛집이 없습니다."
                                           else "이 지역에 등록된 맛집이 없습니다.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 250.dp),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                items(filteredStores) { store ->
                                    RecommendStoreRow(
                                        store = store,
                                        onClick = { onStoreClick(store) }
                                    )
                                }
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
private fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF5B5CEB) else Color.White)
            .border(1.dp, if (selected) Color(0xFF5B5CEB) else Color(0xFFD1D5DB), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF4B5563)
        )
    }
}

@Composable
private fun RecommendStoreRow(
    store: RecommendResponseDto,
    onClick: () -> Unit
) {
    val resolvedImageUrl = toUsableMapImageUrl(store.imageUrl)

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
                if (resolvedImageUrl != null) {
                    coil.compose.AsyncImage(
                        model = resolvedImageUrl,
                        contentDescription = store.storeName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.no_image),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = store.storeName,
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
                        text = String.format("%.1f", store.rating),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    if (!store.categoryName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "· ${toMapCategoryLabel(store.categoryName)}",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (store.visitCount > 0) {
                        Text(
                            text = "방문 ${store.visitCount}회",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    if (!store.roadAddress.isNullOrBlank()) {
                        Text(
                            text = store.roadAddress,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
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
    featuredFallbackStores: List<MapStoreResponseDto>,
    isSheetExpanded: Boolean,
    onRegionSelected: (MapRegion) -> Unit,
    onRegionCleared: () -> Unit,
    onToggleExpanded: () -> Unit,
    onRestaurantClick: (MapRegion, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var recommendStores by remember { mutableStateOf<List<RecommendResponseDto>>(emptyList()) }
    var isRecommendLoading by remember { mutableStateOf(false) }
    var selectedStoreDetail by remember { mutableStateOf<MapStoreResponseDto?>(null) }
    var isDetailLoading by remember { mutableStateOf(false) }
    val displayedRecommendStores = remember(selectedRegion?.label, recommendStores, featuredFallbackStores) {
        val regionLabel = selectedRegion?.label ?: return@remember emptyList()
        mergeWithFeaturedFallbackStores(regionLabel, recommendStores, featuredFallbackStores)
    }

    LaunchedEffect(selectedRegion?.label) {
        if (selectedRegion != null) {
            isRecommendLoading = true
            runCatching {
                RecommendRepository.getRecommendStores(dong = selectedRegion.label)
            }.onSuccess { stores ->
                recommendStores = stores
            }.onFailure {
                recommendStores = emptyList()
            }
            isRecommendLoading = false
        } else {
            recommendStores = emptyList()
        }
    }

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF3F4F6))
                .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onRegionCleared() }
    ) {
        var boxSize by remember { mutableStateOf(IntSize.Zero) }
        val density = LocalDensity.current

        var scale by remember { mutableStateOf(1f) }
        var panOffsetX by remember { mutableStateOf(0f) }
        var panOffsetY by remember { mutableStateOf(0f) }

        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 4f)
            val maxX = (scale - 1f) * boxSize.width / 2f
            val maxY = (scale - 1f) * boxSize.height / 2f
            panOffsetX = (panOffsetX + panChange.x).coerceIn(-maxX, maxX)
            panOffsetY = (panOffsetY + panChange.y).coerceIn(-maxY, maxY)
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
                .onSizeChanged { boxSize = it }
                .transformable(state = transformableState)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = panOffsetX,
                    translationY = panOffsetY
                )
        ) {
            if (boxSize.width > 0) {
                val imgWidth = with(density) { boxSize.width.toDp() }
                val imgHeight = imgWidth * (1527f / 1247f)

                Box(
                    modifier = Modifier
                        .width(imgWidth)
                        .height(imgHeight)
                        .align(Alignment.Center)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.gumi_map_select),
                        contentDescription = "구미시 지도",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    val majorRegions = listOf("무을면", "옥성면", "도개면", "선산읍", "해평면", "고아읍", "산동면", "장천면")

                    REGIONS.forEach { region ->
                        val isSelected = selectedRegion?.label == region.label
                        val isMajor = region.label in majorRegions
                        val labelOffsetX = imgWidth * region.pinX
                        val labelOffsetY = imgHeight * region.pinY

                        Text(
                            text = region.label,
                            color = if (isSelected) Color(0xFF5B5CEB) else Color.White,
                            fontSize = when {
                                isSelected && isMajor -> 15.sp
                                isMajor -> 13.sp
                                isSelected -> 11.sp
                                else -> 9.sp
                            },
                            fontWeight = if (isMajor) FontWeight.Bold else FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium.copy(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = if (isMajor) 0.7f else 0.5f),
                                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                    blurRadius = if (isMajor) 4f else 3f
                                )
                            ),
                            modifier = Modifier
                                .offset(
                                    x = labelOffsetX - if (isMajor) 24.dp else 20.dp,
                                    y = labelOffsetY - if (isMajor) 8.dp else 6.dp
                                )
                                .clickable { onRegionSelected(region) }
                                .padding(if (isMajor) 4.dp else 2.dp)
                        )
                    }
                }
            }
        }

        if (selectedStoreDetail == null) {
            MapRegionGuideHint(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 88.dp)
            )
        }

        AnimatedVisibility(
            visible = selectedRegion != null,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 250),
                initialOffsetY = { it }
            ) + fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 200),
                targetOffsetY = { it }
            ) + fadeOut(animationSpec = tween(durationMillis = 150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            if (selectedRegion != null) {
                RecommendBottomSheet(
                    regionLabel = selectedRegion.label,
                    stores = displayedRecommendStores,
                    isLoading = isRecommendLoading,
                    expanded = isSheetExpanded,
                    onToggleExpanded = onToggleExpanded,
                    onStoreClick = { store ->
                        coroutineScope.launch {
                            isDetailLoading = true
                            runCatching {
                                StoreMapRepository.getStoreDetail(store.storeId)
                            }.onSuccess { detail ->
                                selectedStoreDetail = detail
                            }
                            isDetailLoading = false
                        }
                    }
                )
            }
        }

        if (isDetailLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = Color(0xFF5B5CEB)
                )
            }
        }

        selectedStoreDetail?.let { store ->
            StoreDetailBottomSheet(
                store = store,
                onDismiss = { selectedStoreDetail = null },
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
