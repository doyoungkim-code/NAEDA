package com.example.naedaterminal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedaterminal.ui.theme.NaedaFontFamily

private val Primary = Color(0xFF00635A)
private val BgColor = Color(0xFFFCFFFF)
private val TextPrimary = Color(0xFF0D3B35)

@Composable
fun PaymentDoneScreen(
    amount: Long,
    merchant: String,
    method: String,
    userName: String?,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = "결제 완료 !",
                color = TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = NaedaFontFamily
            )

            Spacer(Modifier.height(40.dp))

            // ── 원형 체크마크 ──
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .drawBehind {
                        // 바깥 점선 링
                        drawCircle(
                            color = Primary.copy(alpha = 0.2f),
                            radius = size.width * 0.54f,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        )
                    }
                    .clip(CircleShape)
                    .background(
                        // ✅ 좌상단 어둡고 → 우하단 밝아지는 그라디언트
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFF00635A),  // 좌상단 — 딥그린
                                0.5f to Color(0xFF009688),  // 중간 — 미디엄 민트
                                1.0f to Color(0xFF4DB6AC)   // 우하단 — 밝은 민트
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "결제가 완료되었습니다",
                color = Primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NaedaFontFamily,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "정상적으로 승인되었습니다",
                color = TextPrimary.copy(alpha = 0.45f),
                fontSize = 14.sp,
                fontFamily = NaedaFontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // ── 결제 상세 카드 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFECF8F7))
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                userName?.takeIf { it.isNotBlank() }?.let { name ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "결제 사용자",
                            color = TextPrimary.copy(alpha = 0.55f),
                            fontSize = 14.sp,
                            fontFamily = NaedaFontFamily
                        )
                        Text(
                            text = name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = NaedaFontFamily
                        )
                    }

                    HorizontalDivider(color = Primary.copy(alpha = 0.12f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "결제 수단",
                        color = TextPrimary.copy(alpha = 0.55f),
                        fontSize = 14.sp,
                        fontFamily = NaedaFontFamily
                    )
                    // ✅ 🙂 → Icons.Default.Face
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = method,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = NaedaFontFamily
                        )
                    }
                }

                HorizontalDivider(color = Primary.copy(alpha = 0.12f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "결제 금액",
                        color = TextPrimary.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontFamily = NaedaFontFamily
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "%,d원".format(amount),
                        color = Primary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = NaedaFontFamily
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = "확인",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NaedaFontFamily
                )
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}
