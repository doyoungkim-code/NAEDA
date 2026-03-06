package com.example.naedaterminal.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.naedaterminal.R
import com.example.naedaterminal.ui.theme.Mint50
import com.example.naedaterminal.ui.theme.OnSurfaceVariant

@Composable
fun NaedaStartScreen(
    onStart: () -> Unit,
    onTerminalMode: () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val bg = cs.background
    val onBg = cs.onBackground
    val primary = cs.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .clickable(onClick = onStart)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // ✅ 기존 상단 배너 Surface 블럭 삭제하고 로고로 교체
            Image(
                painter = painterResource(id = R.drawable.naeda_logo),
                contentDescription = "Naeda Logo",
                contentScale = ContentScale.FillWidth, // 가로를 꽉 채움
                modifier = Modifier
                    .fillMaxWidth()
                    // "세로는 적당히" (원하면 max만 조절)
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 8.dp,
                        vertical = 70.dp)
            )

            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "내다",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = onBg
                )
                Text(
                    text = "(NAEDA)",
                    modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onBg
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "당신을 위한 가장 똑똑한 결제 시스템",
                style = MaterialTheme.typography.titleMedium,
                color = onBg.copy(alpha = 0.70f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            // 기존 OutlinedButton 블럭 삭제하고 아래로 교체

            Surface(
                onClick = onTerminalMode,
                shape = RoundedCornerShape(999.dp),
                color = Mint50, // ✅ 블럭 배경
                tonalElevation = 0.dp,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "STORE TERMINAL MODE",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = onBg // ✅ 텍스트 검은색 계열
                    )
                }
            }
        }
    }
}