package com.example.naedafront.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SpeechBubbleShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val r = size.minDimension * 0.22f
    val tailW = w * 0.22f
    val tailH = h * 0.12f
    val tailX = w * 0.18f
    val bodyH = h - tailH

    addRoundRect(
        RoundRect(
            left = 0f,
            top = 0f,
            right = w,
            bottom = bodyH,
            cornerRadius = CornerRadius(r, r)
        )
    )

    moveTo(tailX, bodyH)
    lineTo(tailX - tailW * 0.8f, bodyH + tailH)
    lineTo(tailX + tailW, bodyH)
    close()
}

@Composable
fun NaedaChatFab(onClick: () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    val springScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_appear"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) { appeared = true }

    Box(
        modifier = Modifier
            .scale(springScale * pulseScale)
            .size(width = 64.dp, height = 62.dp)
            .clip(SpeechBubbleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00897B),
                        Color(0xFF00635A)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(64f, 62f)
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .offset(y = (-4).dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "AI 챗봇",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
