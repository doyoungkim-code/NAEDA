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

                var currentAmount by remember { mutableStateOf(0L) }
                var currentMerchant by remember { mutableStateOf("전자 기기 상점 GUMI") }
                var currentMethod by remember { mutableStateOf("페이스페이") }
                var matchedUserInfo by remember { mutableStateOf<MatchedUserInfo?>(null) }
                var rbaAuthSteps by remember { mutableStateOf<List<RbaAuthType>>(emptyList()) }

                when (route) {

                    Route.PosKey -> PosKeyScreen(
                        onConnected = { posKey ->
                            savePosKey(context, posKey)
                            route = Route.Waiting
                        }
                    )

                    Route.Waiting -> NaedaStartScreen(
                        onPaymentStart = { amount, merchant ->
                            currentAmount = amount
                            currentMerchant = merchant
                            route = Route.PaymentSelect
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
                        apiBaseUrl = "http://10.0.2.2:8080",
                        topK = 3,
                        onBack = { route = Route.PaymentSelect },
                        onAuthed = { userId, similarity ->
                            // TODO: 실제 사용자 정보 백엔드에서 받아오기
                            val mockUser = MatchedUserInfo(
                                userId = userId,
                                userName = "홍길동",
                                requiresPin = false, // TODO: 백엔드 응답값으로 교체
                                linkedAccounts = listOf(
                                    LinkedAccount("acc1", "국민은행", "123-456-789012", 1_250_000L, true),
                                    LinkedAccount("acc2", "신한은행", "987-654-321098", 350_000L, false),
                                    LinkedAccount("acc3", "카카오뱅크", "333-444-555666", 80_000L, false)
                                )
                            )

                            val steps = buildList {
                                when {
                                    // PIN 2차 인증 설정한 사용자 → 무조건 PIN
                                    mockUser.requiresPin -> add(RbaAuthType.Pin)
                                    // 유사도 낮음 → PIN or 전화번호 가운데 4자리 랜덤
                                    similarity < 0.55 -> {
                                        if ((0..1).random() == 0) add(RbaAuthType.Pin)
                                        else add(RbaAuthType.PhoneMiddleFour)
                                    }
                                }
                            }

                            matchedUserInfo = mockUser
                            rbaAuthSteps = steps
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
                                    route = if (rbaAuthSteps.isEmpty()) Route.PaymentDone
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
                        onAuthComplete = { route = Route.PaymentDone },
                        onAuthCancel = { route = Route.FaceMatchUser }
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

private sealed interface Route {
    data object PosKey : Route
    data object Waiting : Route
    data object PaymentSelect : Route
    data object FacePay : Route
    data object FaceMatchUser : Route
    data object Rba : Route
    data object PaymentDone : Route
}