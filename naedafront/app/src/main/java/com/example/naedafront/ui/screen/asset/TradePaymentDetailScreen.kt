package com.example.naedafront.ui.screen.asset

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.naedafront.data.remote.response.PaymentDetailResponse
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TradePaymentDetailScreen(
    item: TradeReportItem,
    detail: PaymentDetailResponse,
    onClose: () -> Unit,
) {
    val accentColor = Mint900
    val merchantName = item.storeName.ifBlank { item.title.ifBlank { "가맹점 정보 없음" } }
    val accountInfo = buildList {
        item.bankName.takeIf { it.isNotBlank() }?.let(::add)
        item.accountNumber.takeIf { it.isNotBlank() }?.let(::add)
    }.joinToString(" ").ifBlank { "계좌 정보 없음" }
    val detailFields = listOf(
        "결제 수단" to accountInfo,
        "결제 시간" to (detail.createdAt?.toTradeDetailDateTime().orEmpty().ifBlank { "-" }),
        "적립 포인트" to (detail.earnedPoints?.let { "${tradeDetailAmount(it)}P" } ?: "적립 없음"),
        "거래 번호" to (detail.ssafyTransactionId ?: detail.paymentId.toString()),
        "결제 장소" to merchantName,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F2F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = accentColor
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "결제 상세",
                        style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.size(42.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(34.dp),
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PAY",
                            style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "결제 장소",
                        style = NaedaTypography.bodyMedium,
                        color = OnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = merchantName,
                        style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = OnBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFFF7F3F6),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "총 결제 금액",
                                style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = OnSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "${tradeDetailAmount(detail.amount)}원",
                                style = NaedaTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            TradePaymentStatusChip(
                                text = detail.status.toTradePaymentStatusText(),
                                accentColor = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        detailFields.forEach { (label, value) ->
                            TradePaymentDetailField(
                                label = label,
                                value = value
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TradePaymentPrimaryButton(
                        text = "닫기",
                        onClick = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun TradePaymentStatusChip(
    text: String,
    accentColor: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = text,
            style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = accentColor
        )
    }
}

@Composable
private fun TradePaymentDetailField(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = NaedaTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = OnBackground
        )
    }
}

@Composable
private fun TradePaymentPrimaryButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Mint900)
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = NaedaTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

private fun tradeDetailAmount(amount: Long): String {
    return "%,d".format(amount)
}

private fun String?.toTradePaymentStatusText(): String {
    return when (this?.uppercase()) {
        "APPROVED", "SUCCESS", "COMPLETED" -> "결제 완료"
        "FAILED", "FAIL", "CANCELED", "CANCELLED" -> "결제 실패"
        else -> "처리 완료"
    }
}

private fun String.toTradeDetailDateTime(): String {
    val parsed = parseTradeDetailDate(this) ?: return this
    val calendar = Calendar.getInstance().apply { time = parsed }
    val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return "${month}월 ${day}일 ${"%02d".format(hour24)}:${"%02d".format(minute)}"
}

private fun parseTradeDetailDate(raw: String): java.util.Date? {
    val normalizedRaw = raw.trim().replace(
        Regex("""\.\d{1,9}(?=Z|[+-]\d{2}:?\d{2}|$)"""),
        ""
    )

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyyMMdd HHmmss",
        "yyyyMMdd HH:mm:ss",
        "yyyyMMdd HH:mm",
        "yyyy-MM-dd"
    )

    for (pattern in patterns) {
        val formatter = SimpleDateFormat(pattern, Locale.KOREA).apply {
            isLenient = false
        }
        val position = ParsePosition(0)
        val parsed = formatter.parse(normalizedRaw, position)
        if (parsed != null && position.index == normalizedRaw.length) {
            return parsed
        }
    }
    return null
}
