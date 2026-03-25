package com.example.naedaterminal

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.naedaterminal.ui.screen.*
import com.example.naedaterminal.ui.screen.payment.AmbiguousAuthChoiceScreen
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

        // 상태바 색상을 검은색으로
        window.statusBarColor = android.graphics.Color.BLACK

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
                var postChoiceAuthSteps by remember { mutableStateOf<List<RbaAuthType>>(emptyList()) }
                var enteredPin by remember { mutableStateOf<String?>(null) }
                var enteredPhoneDigits by remember { mutableStateOf<String?>(null) }
                var currentFaceStatus by remember { mutableStateOf<String?>(null) }
                var selectedAuthMethod by remember { mutableStateOf<String?>(null) }
                var signatureConfirmed by remember { mutableStateOf(false) }
                var paymentFailureReason by remember { mutableStateOf<String?>(null) }

                // 결제 진행 중 POS 취소 감지 폴링
                val isInPaymentFlow = route in listOf(
                    Route.PaymentSelect, Route.FacePay,
                    Route.AuthChoice, Route.Rba, Route.Processing
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

                // 상단 검은색 바 (카메라 영역 가림) + 앱 콘텐츠
                Column(modifier = Modifier.fillMaxSize()) {
                    // 상태바 아래 추가 검은색 영역 (카메라 가림용)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color.Black)
                    )

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
                            currentMethod = "페이스페이"
                            paymentFailureReason = null
                            matchedUserInfo = null
                            rbaAuthSteps = emptyList()
                            postChoiceAuthSteps = emptyList()
                            enteredPin = null
                            enteredPhoneDigits = null
                            currentFaceStatus = null
                            selectedAuthMethod = null
                            signatureConfirmed = false
                            route = Route.FacePay
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
                        onResolved = { faceResult ->
                            val methods = faceResult.requiredMethods
                            val isAmbiguous = faceResult.status == "AMBIGUOUS"
                            val nonFaceMethods = methods - "FACE"
                            val tailSteps = buildList {
                                if ("SIGNATURE" in methods) add(RbaAuthType.Signature)
                            }
                            val directSteps = buildList {
                                if ("PIN" in methods && !isAmbiguous) add(RbaAuthType.Pin)
                                if ("PHONE" in methods && !isAmbiguous) add(RbaAuthType.PhoneMiddleFour)
                                addAll(tailSteps)
                            }

                            matchedUserInfo = MatchedUserInfo(
                                userId = faceResult.bestUserId ?: "",
                                userName = faceResult.username ?: faceResult.bestUserId ?: "알 수 없음",
                                userNo = faceResult.userNo ?: faceResult.matchedUserNo,
                                requiresAdditionalAuth = nonFaceMethods.isNotEmpty(),
                                authReason = when {
                                    isAmbiguous -> "AMBIGUOUS"
                                    "PIN" in methods -> "USER_SETTING"
                                    "SIGNATURE" in methods -> "HIGH_AMOUNT"
                                    else -> null
                                }
                            )

                            currentFaceStatus = faceResult.status
                            selectedAuthMethod = null
                            enteredPin = null
                            enteredPhoneDigits = null
                            signatureConfirmed = false

                            if (isAmbiguous && "PIN" in methods && "PHONE" in methods) {
                                postChoiceAuthSteps = tailSteps
                                rbaAuthSteps = emptyList()
                                route = Route.AuthChoice
                            } else {
                                postChoiceAuthSteps = emptyList()
                                rbaAuthSteps = directSteps
                                route = if (directSteps.isEmpty()) Route.Processing else Route.Rba
                            }
                        },
                        onNotMatched = { reason ->
                            paymentFailureReason = reason ?: "얼굴을 인식하지 못했습니다."
                            route = Route.PaymentFailed
                        }
                    )

                    Route.AuthChoice -> AmbiguousAuthChoiceScreen(
                        paymentAmount = currentAmount,
                        merchantName = currentMerchant,
                        onChoosePin = {
                            selectedAuthMethod = "PIN"
                            enteredPin = null
                            enteredPhoneDigits = null
                            rbaAuthSteps = buildList {
                                add(RbaAuthType.Pin)
                                addAll(postChoiceAuthSteps)
                            }
                            route = Route.Rba
                        },
                        onChoosePhone = {
                            selectedAuthMethod = "PHONE"
                            enteredPin = null
                            enteredPhoneDigits = null
                            rbaAuthSteps = buildList {
                                add(RbaAuthType.PhoneMiddleFour)
                                addAll(postChoiceAuthSteps)
                            }
                            route = Route.Rba
                        },
                        onCancel = {
                            selectedAuthMethod = null
                            signatureConfirmed = false
                            route = Route.FacePay
                        }
                    )

                    Route.Rba -> RbaAuthContainer(
                        authSteps = rbaAuthSteps,
                        paymentAmount = currentAmount,
                        merchantName = currentMerchant,
                        apiBaseUrl = apiBaseUrl,
                        userNo = matchedUserInfo?.userNo,
                        onAuthComplete = { route = Route.Processing },
                        onAuthCancel = {
                            enteredPin = null
                            enteredPhoneDigits = null
                            selectedAuthMethod = null
                            signatureConfirmed = false
                            route = Route.FacePay
                        },
                        onPinEntered = { pin ->
                            enteredPin = pin
                            selectedAuthMethod = "PIN"
                        },
                        onPhoneEntered = { digits ->
                            enteredPhoneDigits = digits
                            selectedAuthMethod = "PHONE"
                        },
                        onSignatureConfirmed = {
                            signatureConfirmed = true
                        }
                    )

                    Route.Processing -> PaymentProcessingScreen(
                        apiBaseUrl = apiBaseUrl,
                        requestId = currentRequestId,
                        userNo = matchedUserInfo?.userNo,
                        pin = enteredPin,
                        phoneMiddleDigits = enteredPhoneDigits,
                        faceStatus = currentFaceStatus,
                        selectedAuthMethod = selectedAuthMethod,
                        signatureConfirmed = signatureConfirmed,
                        amount = currentAmount,
                        merchant = currentMerchant,
                        onSuccess = { result ->
                            route = Route.PaymentDone
                        },
                        onFailure = { reason ->
                            currentRequestId = 0L
                            paymentFailureReason = reason
                            route = Route.PaymentFailed
                        }
                    )

                    Route.Cancelled -> PaymentCancelledScreen(
                        onDone = { route = Route.Waiting }
                    )

                    Route.PaymentFailed -> PaymentFailedScreen(
                        reason = paymentFailureReason,
                        onDone = { route = Route.Waiting }
                    )

                    Route.PaymentDone -> PaymentDoneScreen(
                        amount = currentAmount,
                        merchant = currentMerchant,
                        method = currentMethod,
                        userName = matchedUserInfo?.userName,
                        onDone = { route = Route.Waiting }
                    )
                }
                } // Column 끝
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
    data object AuthChoice : Route
    data object Rba : Route
    data object Processing : Route
    data object PaymentDone : Route
    data object Cancelled : Route
    data object PaymentFailed : Route
}
