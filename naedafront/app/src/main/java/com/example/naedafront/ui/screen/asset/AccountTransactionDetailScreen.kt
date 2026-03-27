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
import com.example.naedafront.data.remote.response.PaymentDetailResponse
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaTypography
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun AccountTransactionDetailScreen(
    transaction: TransactionItem,
    paymentDetail: PaymentDetailResponse? = null,
    storeName: String = "",
    onClose: () -> Unit,
) {
    val isPayment = paymentDetail != null
    val isDeposit = transaction.transactionType.equals("DEPOSIT", ignoreCase = true)
    val accentColor = if (isDeposit) Color(0xFF1F8F5F) else Mint900

    val title = if (isPayment) "Payment Details" else "Transaction Details"
    val badgeText = when {
        isPayment -> "PAY"
        isDeposit -> "IN"
        else -> "OUT"
    }
    val headlineLabel = if (isPayment) "Merchant" else "Counterpart"
    val headlineValue = when {
        isPayment -> storeName.ifBlank { paymentDetail?.storeId?.toString().orEmpty() }
        transaction.counterpart.isNotBlank() -> transaction.counterpart
        transaction.memo.isNotBlank() -> transaction.memo
        else -> "Transaction Info"
    }
    val amountText = "${"%,d".format(paymentDetail?.amount ?: transaction.amount)} KRW"
    val statusText = when {
        isPayment -> paymentDetail?.status.toPaymentStatusLabel()
        isDeposit -> "Deposit Complete"
        else -> "Withdrawal Complete"
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
                        text = title,
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
                                text = badgeText,
                                style = NaedaTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = accentColor
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = headlineLabel,
                            style = NaedaTypography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = headlineValue,
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
                                    text = amountText,
                                    style = NaedaTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                DetailChip(text = statusText, accentColor = accentColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        if (isPayment) {
                            DetailField("Payment Method", paymentDetail?.authMethod.toPaymentMethodLabel())
                            DetailField("Date & Time", paymentDetail?.createdAt?.formatCreatedAt() ?: "-")
                            DetailField("Reward Points", paymentDetail?.earnedPoints?.let { "${"%,d".format(it)}P" } ?: "0P")
                            DetailField("Payment ID", paymentDetail?.ssafyTransactionId ?: paymentDetail?.paymentId?.toString().orEmpty())
                            DetailField("Merchant", storeName.ifBlank { paymentDetail?.storeId?.toString().orEmpty() })
                        } else {
                            DetailField("Type", if (isDeposit) "Deposit" else "Withdrawal")
                            DetailField("Date & Time", transaction.transacted)
                            DetailField("Memo", transaction.memo.ifBlank { "-" })
                            DetailField("Category", transaction.category.ifBlank { "-" })
                            if (transaction.estimatedPoints > 0L) {
                                DetailField("Reward Points", "${"%,d".format(transaction.estimatedPoints)}P")
                            }
                            DetailField("Balance After", "${"%,d".format(transaction.balanceAfter)} KRW")
                            DetailField("Transaction ID", transaction.ssafyTransactionId.ifBlank { transaction.id })
                        }
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

private fun String?.toPaymentMethodLabel(): String {
    return when (this?.uppercase()) {
        "FACE" -> "Naeda Pay (Face Pay)"
        "PIN" -> "Naeda Pay (PIN)"
        else -> "Naeda Pay"
    }
}

private fun String?.toPaymentStatusLabel(): String {
    return when (this?.uppercase()) {
        "COMPLETED", "SUCCESS", "PAID" -> "Payment Complete"
        "CANCELED", "CANCELLED" -> "Payment Cancelled"
        "FAILED" -> "Payment Failed"
        else -> "Processed"
    }
}

private fun String.formatCreatedAt(): String {
    val parsePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss"
    )

    for (pattern in parsePatterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.KOREA)
            if (pattern.contains("X") || pattern.contains("'Z'")) {
                parser.timeZone = TimeZone.getTimeZone("UTC")
            }

            val date = parser.parse(this) ?: continue
            val calendar = Calendar.getInstance().apply { time = date }
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            return "$month/$day ${"%02d".format(hour)}:${"%02d".format(minute)}"
        } catch (_: Exception) {
        }
    }

    return this
}
