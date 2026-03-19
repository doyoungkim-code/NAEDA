package com.example.naedaterminal.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedaterminal.R
import com.example.naedaterminal.ui.theme.KronaOneFontFamily
import com.example.naedaterminal.ui.theme.NaedaFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

private val BgColor = Color(0xFFFCFFFF)

data class PayRequestInfo(
    val requestId: Long,
    val amount: Long,
    val storeId: Long,
    val status: String
)

@Composable
fun NaedaStartScreen(
    storeId: String,
    apiBaseUrl: String,
    onPaymentStart: (requestId: Long, amount: Long, merchant: String) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val client = remember { OkHttpClient() }
    var pollingStatus by remember { mutableStateOf("결제 대기 중...") }

    // 백엔드 폴링: PENDING 결제 요청 감지
    LaunchedEffect(storeId) {
        while (true) {
            val pending = withContext(Dispatchers.IO) {
                pollPendingRequest(client, apiBaseUrl, storeId)
            }
            if (pending != null) {
                pollingStatus = "결제 요청 수신!"
                onPaymentStart(pending.requestId, pending.amount, "매장 결제")
                break
            }
            delay(1500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(220.dp))

            Text(
                text = "NAEDA",
                fontSize = 64.sp,
                fontWeight = FontWeight.Normal,
                color = primary,
                letterSpacing = 5.sp,
                fontFamily = KronaOneFontFamily
            )

            Spacer(Modifier.weight(0.2f))

            Image(
                painter = painterResource(R.drawable.naeda_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = pollingStatus,
                color = primary.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontFamily = NaedaFontFamily
            )

            Spacer(Modifier.weight(1f))

            NaedaFooter(modifier = Modifier.padding(bottom = 36.dp))
        }
    }
}

private fun pollPendingRequest(
    client: OkHttpClient,
    apiBaseUrl: String,
    storeId: String
): PayRequestInfo? {
    return runCatching {
        val req = Request.Builder()
            .url("${apiBaseUrl.trimEnd('/')}/api/pay-requests?storeId=$storeId")
            .get()
            .build()

        client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@use null
            val raw = res.body?.string().orEmpty()
            val array = JSONArray(raw)

            // PENDING 상태인 첫 번째 요청 반환
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("status") == "PENDING") {
                    return@use PayRequestInfo(
                        requestId = obj.optLong("requestId"),
                        amount = obj.optLong("amount"),
                        storeId = obj.optLong("storeId"),
                        status = obj.optString("status")
                    )
                }
            }
            null
        }
    }.getOrNull()
}