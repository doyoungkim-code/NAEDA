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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.sp
import com.example.naedafront.data.remote.response.PaymentDetailResponse
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun AccountTransactionDetailScreen(
    transaction: TransactionItem,
    paymentDetail: PaymentDetailResponse? = null,
    storeName: String = "",
    bankName: String = "",
    accountNumber: String = "",
    onClose: () -> Unit,
) {
    val isPayment = paymentDetail != null
    val isDeposit = transaction.transactionType.equals("DEPOSIT", ignoreCase = true)
    val headerColor = Mint900
    val amountColor = if (isDeposit) Color(0xFF307CBF) else Color(0xFFF2522E)
    val amountValue = paymentDetail?.amount ?: transaction.amount
    val amountText = if (isDeposit) {
        "+${formatAmount(amountValue)}원"
    } else {
        "-${formatAmount(amountValue)}원"
    }
    val isPaymentTransaction = isPayment || transaction.isPaymentTransaction()
    val headlineLabel = "결제처"
    val headlineValue = when {
        isPayment && storeName.isNotBlank() -> storeName
        transaction.counterpart.isNotBlank() -> transaction.counterpart
        transaction.memo.isNotBlank() -> transaction.memo
        else -> "거래 정보 없음"
    }
    val badgeText = if (isDeposit) "입금" else "출금"
    val paymentMethodTitle = "결제 방식"
    val paymentMethod = if (isPaymentTransaction) {
        transaction.toPaymentMethodLabel(paymentDetail?.authMethod)
    } else {
        if (isDeposit) "입금" else "출금"
    }
    val transactedText = if (isPayment) {
        paymentDetail?.createdAt?.formatCreatedAt() ?: transaction.transacted.toDisplayDateTime()
    } else {
        transaction.transacted.toDisplayDateTime()
    }
    val pointsText = "${formatAmount((paymentDetail?.earnedPoints ?: transaction.estimatedPoints).coerceAtLeast(0L))}P"
    val transactionIdText = if (isPayment) {
        (
            paymentDetail?.ssafyTransactionId
                ?: paymentDetail?.paymentId?.toString().orEmpty()
            ).ifBlank { transaction.ssafyTransactionId.ifBlank { transaction.id } }
    } else {
        transaction.ssafyTransactionId.ifBlank { transaction.id }
    }
    val categoryText = transaction.category.takeIf { it.isNotBlank() } ?: "-"
    val detailFields = buildList {
        if (!isDeposit) {
            add(paymentMethodTitle to paymentMethod)
        }
        add("결제 유형" to if (isDeposit) "입금" else "출금")
        add("거래 시간" to transactedText)
        add("카테고리" to categoryText)
        if (!isDeposit) {
            add("적립 포인트" to pointsText)
        }
        add("결제 번호" to transactionIdText)
        add(headlineLabel to headlineValue)
    }

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
                        tint = headerColor
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "결제 상세",
                        style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = headerColor
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
                            .background(headerColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = headerColor
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = headlineLabel,
                        style = NaedaTypography.bodyMedium,
                        color = OnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = headlineValue,
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
                                text = amountText,
                                style = NaedaTypography.displayLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 42.sp
                                ),
                                color = amountColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            detailFields.forEach { (label, value) ->
                                AccountDetailField(
                                    label = label,
                                    value = value
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AccountDetailPrimaryButton(
                        text = "닫기",
                        onClick = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountDetailStatusChip(
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
private fun AccountDetailField(
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
private fun AccountDetailPrimaryButton(
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

private fun formatAmount(amount: Long): String {
    return "%,d".format(amount)
}

private fun TransactionItem.isPaymentTransaction(): Boolean {
    return memo.contains("페이스페이 결제") ||
        memo.contains("카드 결제") ||
        estimatedPoints > 0L
}

private fun TransactionItem.toPaymentMethodLabel(authMethod: String?): String {
    return "페이스페이"
}

private fun String.toAuthLevelLabel(): String {
    return when (uppercase()) {
        "FACE_ONLY" -> "얼굴 인증"
        "FACE_PIN" -> "얼굴 + PIN"
        "FACE_PHONE" -> "얼굴 + 휴대폰"
        "FACE_SIGNATURE" -> "얼굴 + 서명"
        "FACE_PIN_SIGNATURE" -> "얼굴 + PIN + 서명"
        else -> this
    }
}

private fun String.toFdsActionLabel(): String {
    return when (uppercase()) {
        "NONE" -> "이상 없음"
        "PAUSE" -> "추가 확인"
        "BLOCK" -> "차단"
        else -> this
    }
}

private fun String?.toPaymentStatusLabel(): String {
    return when (this?.uppercase()) {
        "COMPLETED", "SUCCESS", "PAID" -> "결제 완료"
        "CANCELED", "CANCELLED" -> "결제 취소"
        "FAILED" -> "결제 실패"
        else -> "처리 완료"
    }
}

private fun String.formatCreatedAt(): String {
    val parsed = parseFlexibleDate(this) ?: return this
    val calendar = Calendar.getInstance().apply { time = parsed }
    val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return "${month}월 ${day}일 ${"%02d".format(hour24)}:${"%02d".format(minute)}"
}

private fun String.toDisplayDateTime(): String {
    val parsed = parseFlexibleDate(this) ?: return this
    val calendar = Calendar.getInstance().apply { time = parsed }
    val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return "${month}월 ${day}일 ${"%02d".format(hour24)}:${"%02d".format(minute)}"
}

private fun parseFlexibleDate(raw: String): java.util.Date? {
    val normalizedRaw = raw.trim().replace(
        Regex("""\.\d{1,9}(?=Z|[+-]\d{2}:?\d{2}|$)"""),
        ""
    )

    val patterns = listOf(
        "yyyyMMdd HHmmss",
        "yyyyMMdd HH:mm:ss",
        "yyyyMMdd HH:mm",
        "yyyyMMdd",
        "yyyy-M-d HH:mm:ss",
        "yyyy-M-d HH:mm",
        "yyyy-M-d",
        "yyyy.M.d HH:mm:ss",
        "yyyy.M.d HH:mm",
        "yyyy.M.d",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy.MM.dd HH:mm",
        "yyyy-MM-dd"
    )

    for (pattern in patterns) {
        val formatter = SimpleDateFormat(pattern, Locale.KOREA).apply {
            isLenient = false
            if (pattern.contains("X")) {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        val position = ParsePosition(0)
        val parsed = formatter.parse(normalizedRaw, position)
        if (parsed != null && position.index == normalizedRaw.length) {
            return parsed
        }
    }
    return null
}
