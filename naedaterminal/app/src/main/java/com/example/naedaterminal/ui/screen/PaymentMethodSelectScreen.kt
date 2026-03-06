// File: app/src/main/java/com/example/naedaterminal/ui/screen/PaymentMethodSelectScreen.kt
package com.example.naedaterminal.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.naedaterminal.ui.theme.Mint50

enum class PaymentMethod {
    FACE_PAY,
    SAMSUNG_PAY,
    CARD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodSelectScreen(
    onBack: () -> Unit,
    onSelect: (PaymentMethod) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val container = cs.background
    val onBg = cs.onBackground

    Scaffold(
        containerColor = container,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "결제 수단 선택",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                        color = onBg
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge, color = onBg)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = container,
                    titleContentColor = onBg,
                    navigationIconContentColor = onBg
                )
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FacePayOptionCard(
                    title = "페이스페이",
                    subtitle = "얼굴 인증으로 빠르게 결제",
                    badgeText = "🙂",
                    onClick = { onSelect(PaymentMethod.FACE_PAY) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "삼성페이",
                        subtitle = "기기 등록 페이",
                        badgeText = "📶",
                        onClick = { onSelect(PaymentMethod.SAMSUNG_PAY) }
                    )
                    SecondaryOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "카드결제",
                        subtitle = "신용/체크카드",
                        badgeText = "💳",
                        onClick = { onSelect(PaymentMethod.CARD) }
                    )
                }

                FooterTip(text = "Tip: 뒤로 가려면 좌측 상단을 누르세요.")
            }
        }
    }
}

@Composable
private fun FacePayOptionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val onBg = cs.onBackground
    val primary = cs.primary
    val outlineSoft = cs.outline.copy(alpha = 0.35f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Mint50,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, outlineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp) // 고정 높이 유지
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp) // ✅ Secondary와 동일하게 맞춰서 오버플로우 여지 감소
                    .clip(RoundedCornerShape(16.dp))
                    .background(primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = primary
                )
            }

            Spacer(Modifier.height(6.dp)) // ✅ 10 -> 6으로 줄여 여유 확보

            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = onBg
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = subtitle,
                maxLines = 1, // ✅ 고정 높이에서 잘림 방지
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = onBg.copy(alpha = 0.60f)
            )
        }
    }
}

@Composable
private fun SecondaryOptionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val onBg = cs.onBackground
    val primary = cs.primary
    val outlineSoft = cs.outline.copy(alpha = 0.35f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Mint50,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, outlineSoft),
        modifier = modifier.height(132.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = primary
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = onBg
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = onBg.copy(alpha = 0.60f)
            )
        }
    }
}

@Composable
private fun FooterTip(text: String) {
    val onBg = MaterialTheme.colorScheme.onBackground

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ℹ️",
            color = onBg.copy(alpha = 0.40f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = onBg.copy(alpha = 0.45f)
        )
    }
}