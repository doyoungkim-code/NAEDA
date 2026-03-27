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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CardTransactionDetailScreen(
    transaction: CardTransactionItem,
    card: CardHeaderUi?,
    onClose: () -> Unit,
) {
    val accentColor = if (transaction.isCanceled) Color(0xFF1F8F5F) else Mint900
    val paymentMethod = buildString {
        append(card?.cardName?.ifBlank { "Card Payment" } ?: "Card Payment")
        val maskedNo = card?.cardNo?.maskCardNumber().orEmpty()
        if (maskedNo.isNotBlank() && maskedNo != "-") {
            append(" ")
            append(maskedNo)
        }
    }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F2F5))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = accentColor
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Payment Details",
                        style = NaedaTypography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.size(42.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(36.dp),
                    color = Color.White,
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
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

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Merchant",
                            style = NaedaTypography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = transaction.merchantName.ifBlank { "No Merchant Info" },
                            style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = OnBackground
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(28.dp),
                            color = Color(0xFFF7F3F6),
                            shadowElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "TOTAL TRANSACTION",
                                    style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = OnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "${formatAmount(transaction.amount)} KRW",
                                    style = NaedaTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                DetailChip(
                                    text = if (transaction.isCanceled) "Payment Cancelled" else "Payment Complete",
                                    accentColor = accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        DetailField("Payment Method", paymentMethod)
                        DetailField("Date & Time", transaction.transactedRaw.toDisplayDateTime().ifBlank { "-" })
                        DetailField("Reward Points", "${formatAmount(transaction.estimatedPoints)}P")
                        DetailField("Payment ID", transaction.transactionId)
                        DetailField("Merchant", transaction.merchantName.ifBlank { "No Merchant Info" })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Mint900)
                        .clickable { onClose() }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Close",
                        style = NaedaTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DetailChip(
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
private fun DetailField(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = NaedaTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = NaedaTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun formatAmount(amount: Long): String {
    return "%,d".format(amount)
}

private fun String.toDisplayDateTime(): String {
    val parsed = parseFlexibleDate(this) ?: return this
    val calendar = Calendar.getInstance().apply { time = parsed }
    val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return "$month/$day ${"%02d".format(hour24)}:${"%02d".format(minute)}"
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
        }
        val position = ParsePosition(0)
        val parsed = formatter.parse(normalizedRaw, position)
        if (parsed != null && position.index == normalizedRaw.length) {
            return parsed
        }
    }
    return null
}

private fun String.maskCardNumber(): String {
    val digits = replace("-", "").replace(" ", "")
    return if (digits.length >= 16) {
        "${digits.substring(0, 4)}-****-****-${digits.takeLast(4)}"
    } else if (isBlank()) {
        "-"
    } else {
        this
    }
}
