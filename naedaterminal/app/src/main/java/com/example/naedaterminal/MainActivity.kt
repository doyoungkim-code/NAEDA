package com.example.naedaterminal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.naedaterminal.ui.screen.*
import com.example.naedaterminal.ui.screen.payment.RbaAuthContainer
import com.example.naedaterminal.ui.screen.payment.RbaAuthType
import com.example.naedaterminal.ui.theme.NaedaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NaedaTheme {
                val context = this

                var route by remember {
                    val hasPosKey = getPosKey(context) != null
                    mutableStateOf<Route>(if (hasPosKey) Route.Waiting else Route.PosKey)
                }

                val apiBaseUrl = "https://j14d103.p.ssafy.io"
                val pollClient = remember { OkHttpClient() }

                var currentRequestId by remember { mutableStateOf(0L) }
                var currentAmount by remember { mutableStateOf(0L) }
                var currentMerchant by remember { mutableStateOf("전자 기기 상점 GUMI") }
                var currentMethod by remember { mutableStateOf("페이스페이") }
                var matchedUserInfo by remember { mutableStateOf<MatchedUserInfo?>(null) }
                var rbaAuthSteps by remember { mutableStateOf<List<RbaAuthType>>(emptyList()) }
                var enteredPin by remember { mutableStateOf<String?>(null) }
                var enteredPhoneDigits by remember { mutableStateOf<String?>(null) }

                // 결제 진행 중 POS 취소 감지 폴링
                val isInPaymentFlow = route in listOf(
                    Route.PaymentSelect, Route.FacePay,
                    Route.FaceMatchUser, Route.Rba
                )
                LaunchedEffect(isInPaymentFlow, currentRequestId) {
                    if (!isInPaymentFlow || currentRequestId == 0L) return@LaunchedEffect
                    while (true) {
                        delay(2000)
                        val cancelled = withContext(Dispatchers.IO) {
                            runCatching {
                                val req = Request.Builder()
                                    .url("$apiBaseUrl/api/pay-requests/$currentRequestId")
                                    .get().build()
                                pollClient.newCall(req).execute().use { res ->
                                    if (!res.isSuccessful || res.code == 404) return@use true
                                    val raw = res.body?.string().orEmpty()
                                    val status = org.json.JSONObject(raw).optString("status")
                                    // PENDING이 아니고 SUCCESS도 아니면 취소/만료/실패
                                    status != "PENDING" && status != "PROCESSING" && status != "SUCCESS"
                                }
                            }.getOrDefault(true) // 네트워크 에러 시에도 취소 처리
                        }
                        if (cancelled) {
                            route = Route.Cancelled
                            break
                        }
                    }
                }

                when (route) {

                    Route.PosKey -> PosKeyScreen(
                        onConnected = { posKey ->
                            savePosKey(context, posKey)
                            route = Route.Waiting
                        }
                    )

                    Route.Waiting -> NaedaStartScreen(
                        storeId = getPosKey(context) ?: "",
                        apiBaseUrl = apiBaseUrl,
                        onPaymentStart = { requestId, amount, merchant ->
                            currentRequestId = requestId
                            currentAmount = amount
                            currentMerchant = merchant
                            route = Route.PaymentSelect
                        },
                        onLogout = {
                            clearPosKey(context)
                            route = Route.PosKey
                        }
                    )

                    Route.PaymentSelect -> PaymentMethodSelectScreen(
                        amount = currentAmount,
                        merchant = currentMerchant,
                        onBack = { route = Route.Waiting },
                        onFacePay = {
                            currentMethod = "페이스페이"
                            route = Route.FacePay
                        },
                        onCard = {
                            currentMethod = "카드결제"
                            route = Route.PaymentDone
                        }
                    )

                    Route.FacePay -> FacePayAuthScreen(
                        amount = currentAmount,
                        merchant = currentMerchant,
                        apiBaseUrl = apiBaseUrl,
                        topK = 3,
                        onBack = { route = Route.PaymentSelect },
                        onAuthed = { faceResult ->
                            // 백엔드 RBA 결정 사용
                            val steps = buildList {
                                val methods = faceResult.requiredMethods
                                if ("PIN" in methods) add(RbaAuthType.Pin)
                                if ("PHONE" in methods) add(RbaAuthType.PhoneMiddleFour)
                            }

                            matchedUserInfo = MatchedUserInfo(
                                userId = faceResult.bestUserId ?: "",
                                userName = faceResult.bestUserId ?: "알 수 없음",
                                requiresPin = "PIN" in faceResult.requiredMethods,
                                linkedAccounts = emptyList()
                            )

                            rbaAuthSteps = steps
                            enteredPin = null
                            enteredPhoneDigits = null
                            route = Route.FaceMatchUser
                        },
                        onNotMatched = { route = Route.PaymentSelect }
                    )

                    Route.FaceMatchUser -> {
                        val userInfo = matchedUserInfo
                        if (userInfo != null) {
                            FaceMatchUserScreen(
                                userInfo = userInfo,
                                amount = currentAmount,
                                merchant = currentMerchant,
                                onConfirm = {
                                    route = if (rbaAuthSteps.isEmpty()) Route.Processing
                                    else Route.Rba
                                },
                                onCancel = { route = Route.Waiting }
                            )
                        }
                    }

                    Route.Rba -> RbaAuthContainer(
                        authSteps = rbaAuthSteps,
                        paymentAmount = currentAmount,
                        merchantName = currentMerchant,
                        onAuthComplete = { route = Route.Processing },
                        onAuthCancel = { route = Route.FaceMatchUser },
                        onPinEntered = { pin -> enteredPin = pin },
                        onPhoneEntered = { digits -> enteredPhoneDigits = digits }
                    )

                    Route.Processing -> PaymentProcessingScreen(
                        apiBaseUrl = apiBaseUrl,
                        requestId = currentRequestId,
                        pin = enteredPin,
                        amount = currentAmount,
                        merchant = currentMerchant,
                        onSuccess = { result ->
                            route = Route.PaymentDone
                        },
                        onFailure = { reason ->
                            // 결제 실패 시 대기 화면으로
                            route = Route.Waiting
                        }
                    )

                    Route.Cancelled -> PaymentCancelledScreen(
                        onDone = { route = Route.Waiting }
                    )

                    Route.PaymentDone -> PaymentDoneScreen(
                        amount = currentAmount,
                        merchant = currentMerchant,
                        method = currentMethod,
                        onDone = { route = Route.Waiting }
                    )
                }
            }
        }
    }
}

private fun savePosKey(context: Context, key: String) {
    context.getSharedPreferences("naeda_prefs", Context.MODE_PRIVATE)
        .edit().putString("pos_key", key).apply()
}

private fun getPosKey(context: Context): String? {
    return context.getSharedPreferences("naeda_prefs", Context.MODE_PRIVATE)
        .getString("pos_key", null)
}

private fun clearPosKey(context: Context) {
    context.getSharedPreferences("naeda_prefs", Context.MODE_PRIVATE)
        .edit().remove("pos_key").apply()
}

private sealed interface Route {
    data object PosKey : Route
    data object Waiting : Route
    data object PaymentSelect : Route
    data object FacePay : Route
    data object FaceMatchUser : Route
    data object Rba : Route
    data object Processing : Route
    data object PaymentDone : Route
    data object Cancelled : Route
}