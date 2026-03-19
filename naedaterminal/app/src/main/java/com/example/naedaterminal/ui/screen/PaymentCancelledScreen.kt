package com.example.naedaterminal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

private val CancelColor = Color(0xFFD32F2F)
private val CancelLight = Color(0xFFEF5350)
private val CancelBright = Color(0xFFEF9A9A)
private val BgColor = Color(0xFFFCFFFF)
private val TextPrimary = Color(0xFF0D3B35)

@Composable
fun PaymentCancelledScreen(
    onDone: () -> Unit
) {
    // 3초 후 자동으로 대기 화면으로 이동
    LaunchedEffect(Unit) {
        delay(3000)
        onDone()
    }

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
                text = "결제 취소",
                color = TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = NaedaFontFamily
            )

            Spacer(Modifier.height(40.dp))

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .drawBehind {
                        drawCircle(
                            color = CancelColor.copy(alpha = 0.2f),
                            radius = size.width * 0.54f,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        )
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colorStops = arrayOf(
                                0.0f to CancelColor,
                                0.5f to CancelLight,
                                1.0f to CancelBright
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "결제가 취소되었습니다",
                color = CancelColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NaedaFontFamily,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "POS에서 결제 요청이 취소되었습니다",
                color = TextPrimary.copy(alpha = 0.45f),
                fontSize = 14.sp,
                fontFamily = NaedaFontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CancelColor)
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
