package com.example.naedaterminal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedaterminal.ui.theme.NaedaFontFamily
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush

private val BgColor = Color(0xFFFCFFFF)
private val TextPrimary = Color(0xFF0D3B35)

@Composable
fun PaymentMethodSelectScreen(
    amount: Long,
    merchant: String,
    onBack: () -> Unit,
    onFacePay: () -> Unit,
    onCard: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

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
            Spacer(Modifier.height(160.dp))

            Text(
                text = "결제 수단 선택",
                color = TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = NaedaFontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "결제 금액",
                color = primary,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp,
                fontFamily = NaedaFontFamily,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "%,d원".format(amount),
                color = TextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = NaedaFontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            PaymentOptionRow(
                icon = Icons.Default.Face,
                label = "페이스페이",
                onClick = onFacePay
            )
            Spacer(Modifier.height(12.dp))
            PaymentOptionRow(
                icon = Icons.Default.CreditCard,
                label = "카드결제",
                onClick = onCard
            )

            Spacer(Modifier.weight(1f))

            NaedaFooter(modifier = Modifier.padding(bottom = 36.dp))
        }
    }
}

@Composable
private fun PaymentOptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = primary.copy(alpha = 0.4f),
                spotColor = primary.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.85f),  // 위 — 밝게
                        primary,                       // 중간
                        primary.copy(red = 0f, green = 0.32f, blue = 0.29f) // 아래 — 어둡게
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NaedaFontFamily,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}