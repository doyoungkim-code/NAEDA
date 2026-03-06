package com.example.naedafront.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── 색상 정의 ──
private val MintLight = Color(0xFFB2DFDB)
private val MintDark = Color(0xFF4DB6AC)
private val ButtonGreen = Color(0xFF2E7D6E)
private val SubTextGray = Color(0xFF9E9E9E)
private val LinkGreen = Color(0xFF2E7D6E)

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit = {},
    onLoginClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MintLight.copy(alpha = 0.4f),
                        Color.White
                    ),
                    startY = 0f,
                    endY = 800f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // ── 타이틀 텍스트 ──
            Text(
                text = "내다를 시작하려면\n본인인증을 해주세요",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                lineHeight = 36.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 서브 텍스트 ──
            Text(
                text = "신속하고 안전한 금융 서비스를 시작합니다.",
                fontSize = 14.sp,
                color = SubTextGray,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // ── 로고 카드 ──
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                // 방법 1: 벡터 드로어블 사용 시
                // Image(
                //     painter = painterResource(id = R.drawable.ic_naeda_logo),
                //     contentDescription = "내다 로고",
                //     modifier = Modifier.size(100.dp)
                // )

                // 방법 2: 임시 텍스트 로고 (드로어블 준비 전)
                NaedaLogoPlaceholder()
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── 시작하기 버튼 ──
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonGreen
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Text(
                    text = "시작하기",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 로그인 링크 ──
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Text(
                    text = "이미 계정이 있으신가요?",
                    fontSize = 14.sp,
                    color = SubTextGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "로그인",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LinkGreen,
                    modifier = Modifier.clickable { onLoginClick() }
                )
            }
        }
    }
}

/**
 * 로고 드로어블 준비 전 임시 플레이스홀더
 * 실제 로고 이미지(ic_naeda_logo)가 준비되면
 * 위 로고 카드 섹션의 Image() 주석을 해제하고 이 함수를 제거
 */
@Composable
private fun NaedaLogoPlaceholder() {
    Canvas(modifier = Modifier.size(100.dp)) {
        val cornerRadius = 20.dp.toPx()

        // 민트색 둥근 사각형 테두리
        drawRoundRect(
            color = Color(0xFF4DB6AC),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )

        // 왼쪽 눈
        drawCircle(
            color = Color(0xFF333333),
            radius = 5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                x = size.width * 0.35f,
                y = size.height * 0.4f
            )
        )

        // 오른쪽 눈
        drawCircle(
            color = Color(0xFF333333),
            radius = 5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                x = size.width * 0.65f,
                y = size.height * 0.4f
            )
        )

        // 미소 (반원 아크)
        drawArc(
            color = Color(0xFF333333),
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(
                x = size.width * 0.25f,
                y = size.height * 0.35f
            ),
            size = androidx.compose.ui.geometry.Size(
                width = size.width * 0.5f,
                height = size.height * 0.35f
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun WelcomeScreenPreview() {
    MaterialTheme {
        WelcomeScreen()
    }
}