// File: app/src/main/java/com/example/naedaterminal/ui/screen/PaymentDoneScreen.kt
package com.example.naedaterminal.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.naedaterminal.ui.theme.Mint50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDoneScreen(
    onDone: () -> Unit,
    onReceipt: () -> Unit = {},
    merchantName: String = "SSAFY 편의점",
    orderName: String = "결제 상품",
    amountWon: Long = 4500,
    paidMethodLabel: String = "FACE PAY",
    approvedAt: String = "2026-03-05 14:30",
    approvalNo: String = "A-20260305-0001",
) {
    val cs = MaterialTheme.colorScheme
    val container = cs.background
    val onBg = cs.onBackground
    val primary = cs.primary
    val outlineSoft = cs.outline.copy(alpha = 0.35f)

    Scaffold(
        containerColor = container,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("결제 완료", fontWeight = FontWeight.Bold, color = onBg) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = container,
                    titleContentColor = onBg
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(10.dp))

            Text(
                text = "✅",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "결제가 완료되었습니다",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = onBg,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Mint50,
                border = BorderStroke(1.dp, outlineSoft),
                tonalElevation = 0.dp,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("가맹점", color = onBg.copy(alpha = 0.65f))
                        Text(merchantName, fontWeight = FontWeight.Bold, color = onBg)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("상품", color = onBg.copy(alpha = 0.65f))
                        Text(orderName, fontWeight = FontWeight.Bold, color = onBg)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("결제수단", color = onBg.copy(alpha = 0.65f))
                        Text(paidMethodLabel, fontWeight = FontWeight.Bold, color = onBg)
                    }

                    Divider(color = outlineSoft)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("결제금액", color = onBg.copy(alpha = 0.65f))
                        Text(
                            text = "${amountWon}원",
                            fontWeight = FontWeight.ExtraBold,
                            color = primary
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("승인시각", color = onBg.copy(alpha = 0.65f))
                        Text(approvedAt, fontWeight = FontWeight.SemiBold, color = onBg)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("승인번호", color = onBg.copy(alpha = 0.65f))
                        Text(approvalNo, fontWeight = FontWeight.SemiBold, color = onBg)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ✅ 버튼도 카드 스타일로 통일: Mint50 + outline + primary 텍스트
                Surface(
                    onClick = onReceipt,
                    shape = RoundedCornerShape(999.dp),
                    color = Mint50,
                    tonalElevation = 0.dp,
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, outlineSoft),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "영수증 보기",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = primary
                        )
                    }
                }

                Surface(
                    onClick = onDone,
                    shape = RoundedCornerShape(999.dp),
                    color = Mint50,
                    tonalElevation = 0.dp,
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, outlineSoft),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "완료",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = primary
                        )
                    }
                }
            }
        }
    }
}