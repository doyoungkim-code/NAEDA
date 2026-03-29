package com.example.naedaterminal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.StoreMallDirectory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedaterminal.ui.theme.NaedaFontFamily

private val Primary = Color(0xFF00635A)
private val BgColor = Color(0xFFFCFFFF)
private val BgCard = Color(0xFFECF8F7)
private val TextPrimary = Color(0xFF0D3B35)

@Composable
fun PaymentMethodSelectScreen(
    amount: Long,
    merchant: String,
    onBack: () -> Unit,
    onFacePay: () -> Unit,
    onCard: () -> Unit
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
            Spacer(Modifier.height(80.dp))

            // 페이스페이 아이콘
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "페이스페이 결제",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = NaedaFontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "얼굴 인식으로 간편하게 결제합니다",
                color = TextPrimary.copy(alpha = 0.45f),
                fontSize = 13.sp,
                fontFamily = NaedaFontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // 결제 금액 카드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "결제 금액",
                    color = TextPrimary.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontFamily = NaedaFontFamily
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "%,d원".format(amount),
                    color = TextPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = NaedaFontFamily
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.StoreMallDirectory,
                        contentDescription = null,
                        tint = TextPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = merchant,
                        color = TextPrimary.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontFamily = NaedaFontFamily
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 얼굴 인식 시작 버튼
            Button(
                onClick = onFacePay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "얼굴 인식 시작",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NaedaFontFamily
                )
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "취소",
                    color = TextPrimary.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    fontFamily = NaedaFontFamily
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
