package com.example.naedafront.ui.screen.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.naedafront.R

@Composable
fun MapSelectScreen(
    onOkseongClick: () -> Unit = {},
    onDogaeClick: () -> Unit = {},
    onMueulClick: () -> Unit = {},
    onSeonsanClick: () -> Unit = {},
    onHaepyeongClick: () -> Unit = {},
    onSandongClick: () -> Unit = {},
    onJangcheonClick: () -> Unit = {},
    onGoaClick: () -> Unit = {},
    onWonpyeongClick: () -> Unit = {},
    onYangpoClick: () -> Unit = {},
    onIndongClick: () -> Unit = {},
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.gumi_map_select),
                contentDescription = "구미 지역 선택 지도",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // 옥성면
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.29f, y = maxHeight * 0.19f)
                    .size(width = maxWidth * 0.18f, height = maxHeight * 0.09f)
                    .clickable(onClick = onOkseongClick)
            )

            // 도개면
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.47f, y = maxHeight * 0.18f)
                    .size(width = maxWidth * 0.17f, height = maxHeight * 0.09f)
                    .clickable(onClick = onDogaeClick)
            )

            // 무을면
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.09f, y = maxHeight * 0.28f)
                    .size(width = maxWidth * 0.17f, height = maxHeight * 0.09f)
                    .clickable(onClick = onMueulClick)
            )

            // 선산읍
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.33f, y = maxHeight * 0.40f)
                    .size(width = maxWidth * 0.18f, height = maxHeight * 0.09f)
                    .clickable(onClick = onSeonsanClick)
            )

            // 해평면
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.55f, y = maxHeight * 0.47f)
                    .size(width = maxWidth * 0.17f, height = maxHeight * 0.09f)
                    .clickable(onClick = onHaepyeongClick)
            )

            // 산동면
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.69f, y = maxHeight * 0.60f)
                    .size(width = maxWidth * 0.17f, height = maxHeight * 0.09f)
                    .clickable(onClick = onSandongClick)
            )

            // 장천면
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.83f, y = maxHeight * 0.73f)
                    .size(width = maxWidth * 0.15f, height = maxHeight * 0.09f)
                    .clickable(onClick = onJangcheonClick)
            )

            // 고아읍
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.40f, y = maxHeight * 0.60f)
                    .size(width = maxWidth * 0.15f, height = maxHeight * 0.09f)
                    .clickable(onClick = onGoaClick)
            )

            // 원평동
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.46f, y = maxHeight * 0.70f)
                    .size(width = maxWidth * 0.14f, height = maxHeight * 0.08f)
                    .clickable(onClick = onWonpyeongClick)
            )

            // 양포동
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.58f, y = maxHeight * 0.78f)
                    .size(width = maxWidth * 0.14f, height = maxHeight * 0.08f)
                    .clickable(onClick = onYangpoClick)
            )

            // 인동동
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * 0.74f, y = maxHeight * 0.84f)
                    .size(width = maxWidth * 0.13f, height = maxHeight * 0.08f)
                    .clickable(onClick = onIndongClick)
            )
        }
    }
}