package com.example.naedafront.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.R
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Mint900

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 상단 민트 배경 블롭 (장식)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Mint500.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 로고 이미지 — 화면 중앙보다 위 + 글로우 효과
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val glowAnim = remember { Animatable(-1.5f) }
                LaunchedEffect(Unit) {
                    glowAnim.animateTo(
                        targetValue = 1.5f,
                        animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                    )
                }

                Box(
                    modifier = Modifier.size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.chatbot_image),
                        contentDescription = "내다 로고",
                        modifier = Modifier.size(280.dp)
                    )

                    // 글로우 오버레이 (사선)
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clipToBounds()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 120.dp, height = 400.dp)
                                .offset(x = (280 * glowAnim.value).dp)
                                .rotate(-20f)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.4f),
                                            Color.Transparent
                                        ),
                                        start = Offset.Zero,
                                        end = Offset(120f, 0f)
                                    )
                                )
                        )
                    }
                }
            }

            // 슬라이드업 + 페이드인 애니메이션
            val slideAnim = remember { Animatable(150f) }
            val alphaAnim = remember { Animatable(0f) }

            LaunchedEffect(Unit) {
                slideAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                )
            }
            LaunchedEffect(Unit) {
                alphaAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 600)
                )
            }

            // 하단 텍스트 + 버튼 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = slideAnim.value.dp)
                    .alpha(alphaAnim.value)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "얼굴 하나로\n모든 결제를 내다",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 42.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "신속하고 안전한 금융 서비스,\n지금 바로 시작해보세요.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Mint900
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        text = "시작하기",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "이미 계정이 있으신가요?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "로그인",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Mint900,
                        modifier = Modifier.clickable { onLoginClick() }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WelcomeScreenPreview() {
    MaterialTheme {
        WelcomeScreen()
    }
}