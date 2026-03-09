package com.example.naedafront.ui.screen.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.naedafront.R
import kotlin.math.roundToInt

data class MapRegion(
    val id: String,
    val label: String,
    val labelAnchor: Offset
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSelectScreen(
    onBack: () -> Unit,
    onRestaurantClick: (region: MapRegion, restaurantName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val regions = remember { gumiRegions() }

    val restaurantsByRegion = remember {
        mapOf(
            "muul" to listOf("무을식당", "무을국밥", "무을손칼국수"),
            "oksung" to listOf("옥성한우", "옥성손두부"),
            "dogae" to listOf("도개짬뽕", "도개순대국"),
            "haepyeong" to listOf("해평막국수", "해평갈비"),
            "seonsan" to listOf("선산곱창", "선산냉면", "선산국수"),
            "goa" to listOf("고아돈까스", "고아국밥", "고아초밥"),
            "sandong" to listOf("산동쭈꾸미", "산동샤브", "산동파스타"),
            "jangcheon" to listOf("장천오리", "장천백반"),
            "wonpyeong" to listOf("원평돼지국밥", "원평분식"),
            "jisan" to listOf("지산삼겹", "지산칼국수"),
            "doryang" to listOf("도량마라탕", "도량김밥"),
            "seonjuwonnam" to listOf("선주원남돈카츠", "선주원남국밥"),
            "songjeong" to listOf("송정초밥", "송정찌개"),
            "hyeonggok1" to listOf("형곡1동치킨", "형곡1동보쌈"),
            "hyeonggok2" to listOf("형곡2동국수", "형곡2동파전"),
            "gwangpyeong" to listOf("광평낙지", "광평쌀국수"),
            "sangmosagok" to listOf("상모사곡족발", "상모사곡카레"),
            "imo" to listOf("임오곰탕", "임오중식"),
            "bisan" to listOf("비산불고기", "비산냉삼"),
            "gongdan" to listOf("공단기사식당", "공단짬뽕"),
            "sinpyeong1" to listOf("신평1동백반", "신평1동칼국수"),
            "sinpyeong2" to listOf("신평2동곱창", "신평2동돈까스"),
            "yangpo" to listOf("양포쭈꾸미", "양포초밥"),
            "jinmi" to listOf("진미국밥", "진미삼겹"),
            "indong" to listOf("인동곱창", "인동마라탕", "인동파스타")
        )
    }

    var imageWidthPx by remember { mutableIntStateOf(0) }
    var imageHeightPx by remember { mutableIntStateOf(0) }
    var selectedRegion by remember { mutableStateOf<MapRegion?>(null) }

    val labelPositions = remember(regions, imageWidthPx, imageHeightPx) {
        calculateInnerLabelPositions(
            regions = regions,
            imageWidthPx = imageWidthPx,
            imageHeightPx = imageHeightPx
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF6F1E8))
    ) {
        Image(
            painter = painterResource(id = R.drawable.gumi_map_select),
            contentDescription = "구미 지역 선택 지도",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .onSizeChanged {
                    imageWidthPx = it.width
                    imageHeightPx = it.height
                }
        )

        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp)
                .zIndex(2f)
        ) {
            Text(
                text = "뒤로",
                color = Color(0xFF2E3B2F),
                fontWeight = FontWeight.Bold
            )
        }

        if (imageWidthPx > 0 && imageHeightPx > 0) {
            regions.forEach { region ->
                SmallRegionLabel(
                    region = region,
                    selected = selectedRegion?.id == region.id,
                    labelOffset = labelPositions[region.id] ?: IntOffset.Zero,
                    onClick = { selectedRegion = region }
                )
            }
        }
    }

    selectedRegion?.let { region ->
        val restaurants = restaurantsByRegion[region.id].orEmpty()

        ModalBottomSheet(
            onDismissRequest = { selectedRegion = null },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = region.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF223024)
                )

                Text(
                    text = "맛집 리스트",
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6A7468)
                )

                if (restaurants.isEmpty()) {
                    Text(
                        text = "등록된 맛집이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7A7A7A),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                } else {
                    restaurants.forEachIndexed { index, restaurant ->
                        RestaurantListItem(
                            name = restaurant,
                            onClick = {
                                onRestaurantClick(region, restaurant)
                                selectedRegion = null
                            }
                        )

                        if (index != restaurants.lastIndex) {
                            HorizontalDivider(color = Color(0xFFE8E3D8))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallRegionLabel(
    region: MapRegion,
    selected: Boolean,
    labelOffset: IntOffset,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .offset { labelOffset }
            .zIndex(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFF1FFF3) else Color(0xFFFFFBF2),
        shadowElevation = if (selected) 4.dp else 1.5.dp
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = if (selected) Color(0xFF2E8B57) else Color(0xFFC7B08A),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(
                        color = if (selected) Color(0xFF2E8B57) else Color(0xFFD4B483),
                        shape = CircleShape
                    )
            )

            Text(
                text = region.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E3B2F)
            )
        }
    }
}

@Composable
private fun RestaurantListItem(
    name: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    color = Color(0xFF4CAF50),
                    shape = CircleShape
                )
        )

        Text(
            text = name,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF223024)
        )
    }
}

private fun calculateInnerLabelPositions(
    regions: List<MapRegion>,
    imageWidthPx: Int,
    imageHeightPx: Int
): Map<String, IntOffset> {
    if (imageWidthPx == 0 || imageHeightPx == 0) return emptyMap()

    val result = mutableMapOf<String, IntOffset>()

    val labelWidth = 42
    val labelHeight = 18

    regions.forEach { region ->
        val x = (region.labelAnchor.x * imageWidthPx).roundToInt() - (labelWidth / 2)
        val y = (region.labelAnchor.y * imageHeightPx).roundToInt() - (labelHeight / 2)

        result[region.id] = IntOffset(
            x = x.coerceIn(0, imageWidthPx - labelWidth),
            y = y.coerceIn(0, imageHeightPx - labelHeight)
        )
    }

    return result
}

private fun gumiRegions(): List<MapRegion> = listOf(
    MapRegion("muul", "무을면", Offset(0.12f, 0.25f)),
    MapRegion("oksung", "옥성면", Offset(0.33f, 0.12f)),
    MapRegion("dogae", "도개면", Offset(0.54f, 0.14f)),
    MapRegion("haepyeong", "해평면", Offset(0.61f, 0.28f)),
    MapRegion("seonsan", "선산읍", Offset(0.29f, 0.29f)),
    MapRegion("goa", "고아읍", Offset(0.37f, 0.47f)),
    MapRegion("sandong", "산동읍", Offset(0.72f, 0.42f)),
    MapRegion("jangcheon", "장천면", Offset(0.81f, 0.58f)),
    MapRegion("wonpyeong", "원평동", Offset(0.28f, 0.59f)),
    MapRegion("jisan", "지산동", Offset(0.35f, 0.58f)),
    MapRegion("doryang", "도량동", Offset(0.43f, 0.59f)),
    MapRegion("seonjuwonnam", "선주원남동", Offset(0.29f, 0.68f)),
    MapRegion("songjeong", "송정동", Offset(0.37f, 0.67f)),
    MapRegion("hyeonggok1", "형곡1동", Offset(0.33f, 0.76f)),
    MapRegion("hyeonggok2", "형곡2동", Offset(0.42f, 0.75f)),
    MapRegion("gwangpyeong", "광평동", Offset(0.50f, 0.72f)),
    MapRegion("sangmosagok", "상모사곡동", Offset(0.41f, 0.85f)),
    MapRegion("imo", "임오동", Offset(0.54f, 0.85f)),
    MapRegion("bisan", "비산동", Offset(0.59f, 0.69f)),
    MapRegion("gongdan", "공단동", Offset(0.60f, 0.79f)),
    MapRegion("sinpyeong1", "신평1동", Offset(0.61f, 0.88f)),
    MapRegion("sinpyeong2", "신평2동", Offset(0.66f, 0.61f)),
    MapRegion("yangpo", "양포동", Offset(0.72f, 0.72f)),
    MapRegion("jinmi", "진미동", Offset(0.73f, 0.82f)),
    MapRegion("indong", "인동동", Offset(0.84f, 0.81f))
)