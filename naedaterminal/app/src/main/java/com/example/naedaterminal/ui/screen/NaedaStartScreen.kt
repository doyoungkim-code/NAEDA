package com.example.naedaterminal.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedaterminal.R
import com.example.naedaterminal.ui.theme.KronaOneFontFamily

private val BgColor = Color(0xFFFCFFFF)

@Composable
fun NaedaStartScreen(
    onPaymentStart: (amount: Long, merchant: String) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            // TODO: 실제로는 POS 웹에서 결제 요청 수신 시 자동 진입
            // 임시로 탭하면 진입 (테스트용 금액 15,000원)
            .clickable { onPaymentStart(15_000L, "전자 기기 상점 GUMI") }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(220.dp))

            Text(
                text = "NAEDA",
                fontSize = 64.sp,
                fontWeight = FontWeight.Normal,
                color = primary,
                letterSpacing = 5.sp,
                fontFamily = KronaOneFontFamily
            )

            Spacer(Modifier.weight(0.2f))

            Image(
                painter = painterResource(R.drawable.naeda_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f)
            )

            Spacer(Modifier.weight(1f))

            NaedaFooter(modifier = Modifier.padding(bottom = 36.dp))
        }
    }
}