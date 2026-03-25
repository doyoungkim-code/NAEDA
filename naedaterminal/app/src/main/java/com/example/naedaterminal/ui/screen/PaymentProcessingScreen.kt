package com.example.naedaterminal.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedaterminal.ui.theme.NaedaFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

private val Primary = Color(0xFF00635A)
private val BgColor = Color(0xFFFCFFFF)
private val TextPrimary = Color(0xFF0D3B35)

data class PaymentResult(
    val paymentId: Long?,
    val status: String,
    val pinVerified: Boolean,
    val earnedPoints: Long,
    val failureReason: String?
)

@Composable
fun PaymentProcessingScreen(
    apiBaseUrl: String,
    requestId: Long,
    userNo: Long?,
    pin: String?,
    phoneMiddleDigits: String?,
    faceStatus: String?,
    selectedAuthMethod: String?,
    signatureConfirmed: Boolean,
    amount: Long,
    merchant: String,
    onSuccess: (PaymentResult) -> Unit,
    onFailure: (String) -> Unit
) {
    val client = remember { OkHttpClient() }

    val transition = rememberInfiniteTransition(label = "processing")
    val progress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Restart),
        label = "progress"
    )

    // 결제 API 호출
    LaunchedEffect(requestId) {
        val result = withContext(Dispatchers.IO) {
            processPaymentRequest(
                client = client,
                apiBaseUrl = apiBaseUrl,
                requestId = requestId,
                userNo = userNo,
                pin = pin,
                phoneMiddleDigits = phoneMiddleDigits,
                faceStatus = faceStatus,
                selectedAuthMethod = selectedAuthMethod,
                signatureConfirmed = signatureConfirmed
            )
        }
        android.util.Log.d("PayProcess", "result=$result")
        if (result != null && result.status == "SUCCESS") {
            onSuccess(result)
        } else {
            onFailure(result?.failureReason ?: "결제 처리에 실패했습니다")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "결제 처리 중...",
                color = Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NaedaFontFamily
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "%,d원".format(amount),
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = NaedaFontFamily
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = merchant,
                color = TextPrimary.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontFamily = NaedaFontFamily
            )
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(200.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.12f)
            )
        }
    }
}

private fun processPaymentRequest(
    client: OkHttpClient,
    apiBaseUrl: String,
    requestId: Long,
    userNo: Long?,
    pin: String?,
    phoneMiddleDigits: String?,
    faceStatus: String?,
    selectedAuthMethod: String?,
    signatureConfirmed: Boolean
): PaymentResult? {
    return runCatching {
        val requestJson = JSONObject().apply {
            put("userNo", userNo)
            put("idempotencyKey", UUID.randomUUID().toString())
            if (!pin.isNullOrBlank()) put("pin", pin)
            if (!phoneMiddleDigits.isNullOrBlank()) put("phoneMiddleDigits", phoneMiddleDigits)
            if (!faceStatus.isNullOrBlank()) put("faceStatus", faceStatus)
            if (!selectedAuthMethod.isNullOrBlank()) put("selectedAuthMethod", selectedAuthMethod)
            put("signatureConfirmed", signatureConfirmed)
        }

        val req = Request.Builder()
            .url("${apiBaseUrl.trimEnd('/')}/api/pay-requests/$requestId/process")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { res ->
            val raw = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                val errorJson = runCatching { JSONObject(raw) }.getOrNull()
                val message = errorJson?.optString("message") ?: "HTTP ${res.code}"
                return@use PaymentResult(
                    paymentId = null,
                    status = "FAILED",
                    pinVerified = false,
                    earnedPoints = 0L,
                    failureReason = message
                )
            }
            val json = JSONObject(raw)
            PaymentResult(
                paymentId = json.optLong("paymentId"),
                status = json.optString("status", "UNKNOWN"),
                pinVerified = json.optBoolean("pinVerified", false),
                earnedPoints = json.optLong("earnedPoints", 0L),
                failureReason = json.optString("failureReason").takeIf { it.isNotBlank() }
            )
        }
    }.getOrNull()
}
