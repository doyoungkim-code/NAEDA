package com.example.naedaterminal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

data class MatchedUserInfo(
    val userId: String,
    val userName: String,
    val userNo: Long?,
    val requiresAdditionalAuth: Boolean,
    val authReason: String?   // "AMBIGUOUS", "HIGH_AMOUNT", "USER_SETTING" 등
)

@Composable
fun FaceMatchUserScreen(
    userInfo: MatchedUserInfo,
    amount: Long,
    merchant: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            // 얼굴 인식 성공 아이콘
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // 인증 성공 배지
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Primary.copy(alpha = 0.1f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "얼굴 인식 성공",
                    color = Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = NaedaFontFamily
                )
            }

            Spacer(Modifier.height(16.dp))

            // 사용자 이름
            Text(
                text = "${userInfo.userName} 님",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = NaedaFontFamily
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "본인 확인이 완료되었습니다",
                color = TextPrimary.copy(alpha = 0.45f),
                fontSize = 13.sp,
                fontFamily = NaedaFontFamily
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
                    fontSize = 32.sp,
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

            // 결제하기 버튼
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    text = if (userInfo.requiresAdditionalAuth) "본인 인증 후 결제하기" else "결제하기",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NaedaFontFamily
                )
            }

            Spacer(Modifier.height(10.dp))

            TextButton(
                onClick = onCancel,
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
