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

@Composable
fun TradePaymentDetailScreen(
    item: TradeReportItem,
    detail: PaymentDetailResponse? = null,
    onClose: () -> Unit,
) {
    val accentColor = Mint900
    val isPayment = detail != null
    val isIncome = item.isIncome
    val transaction = item.transaction
    val amountValue = item.amountValue.takeIf { it > 0L } ?: detail?.amount ?: transaction?.amount ?: 0L
    val amountColor = if (isIncome) Color(0xFF307CBF) else Color(0xFFF2522E)
    val amountText = if (isIncome) {
        "+${tradeDetailAmount(amountValue)}\uC6D0"
    } else {
        "-${tradeDetailAmount(amountValue)}\uC6D0"
    }
    val merchantName = item.storeName.ifBlank {
        item.title.ifBlank { "\uAC00\uB9F9\uC810 \uC815\uBCF4 \uC5C6\uC74C" }
    }
    val isPaymentTransaction = item.isPaymentTransaction() || isPayment
    val headlineLabel = "\uACB0\uC81C\uCC98"
    val headlineValue = when {
        isPayment && merchantName.isNotBlank() -> merchantName
        isIncome && item.title.isNotBlank() && item.title != "\uC785\uAE08" -> item.title
        transaction?.counterpart?.isNotBlank() == true -> transaction.counterpart
        transaction?.memo?.isNotBlank() == true -> transaction.memo
        else -> "\uAC70\uB798 \uC815\uBCF4 \uC5C6\uC74C"
    }
    val badgeText = if (isIncome) "\uC785\uAE08" else "\uCD9C\uAE08"
    val paymentMethodTitle = "\uACB0\uC81C \uBC29\uC2DD"
    val paymentMethod = item.toTradePaymentMethodLabel(detail?.authMethod)
    val paymentInstrumentText = when {
        item.cardNumber.isNotBlank() -> item.cardNumber
        item.bankName.isNotBlank() || item.accountNumber.isNotBlank() -> listOf(
            item.bankName.takeIf { it.isNotBlank() },
            item.accountNumber.takeIf { it.isNotBlank() }
        ).joinToString(" ")
        else -> "-"
    }
    val transactedText = detail?.createdAt
        ?.toTradeDetailDateTime()
        .orEmpty()
        .ifBlank {
            transaction?.transacted?.toTradeDetailDateTime()
                ?: item.createdAtRaw.toTradeDetailDateTime()
        }
        .ifBlank { "-" }
    val pointsText = "${tradeDetailAmount((detail?.earnedPoints ?: transaction?.estimatedPoints ?: 0L).coerceAtLeast(0L))}P"
    val transactionIdText = detail?.ssafyTransactionId
        ?.takeIf { it.isNotBlank() }
        ?: transaction?.ssafyTransactionId?.takeIf { it.isNotBlank() }
        ?: transaction?.id?.takeIf { it.isNotBlank() }
        ?: detail?.paymentId?.toString()
        ?: item.paymentId.takeIf { it > 0L }?.toString()
        ?: "-"
    val categoryText = transaction?.category?.takeIf { it.isNotBlank() }
        ?: item.category.takeIf { it.isNotBlank() }
        ?: "-"
    val detailFields = buildList {
        if (!isIncome) {
            add(paymentMethodTitle to paymentMethod)
            add("\uACB0\uC81C \uC218\uB2E8" to paymentInstrumentText)
        }
        add("\uACB0\uC81C \uC720\uD615" to if (isIncome) "\uC785\uAE08" else "\uCD9C\uAE08")
        add("\uAC70\uB798 \uC2DC\uAC04" to transactedText)
        add("\uCE74\uD14C\uACE0\uB9AC" to categoryText)
        if (!isIncome) {
            add("\uC801\uB9BD \uD3EC\uC778\uD2B8" to pointsText)
        }
        add("\uACB0\uC81C \uBC88\uD638" to transactionIdText)
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
                        contentDescription = "\uB2EB\uAE30",
                        tint = accentColor
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\uACB0\uC81C \uC0C1\uC138",
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
                            text = badgeText,
                            style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
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
                                TradePaymentDetailField(
                                    label = label,
                                    value = value
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    TradePaymentPrimaryButton(
                        text = "\uB2EB\uAE30",
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

private fun TradeReportItem.isPaymentTransaction(): Boolean {
    return paymentId > 0L || (!isIncome && transaction?.ssafyTransactionId?.isNotBlank() == true)
}

private fun TradeReportItem.toTradePaymentMethodLabel(authMethod: String?): String {
    if (isIncome) return "\uC785\uAE08"

    val usesFacePay = authMethod?.contains("FACE", ignoreCase = true) == true

    return when {
        usesFacePay -> "\uD398\uC774\uC2A4\uD398\uC774"
        isPaymentTransaction() -> "\uCE74\uB4DC"
        else -> "\uCD9C\uAE08"
    }
}

private fun String.toTradeDetailDateTime(): String {
    val parsed = parseTradeDetailDate(this) ?: return this
    val calendar = Calendar.getInstance().apply { time = parsed }
    val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return "${month}\uC6D4 ${day}\uC77C ${"%02d".format(hour24)}:${"%02d".format(minute)}"
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
