package com.example.naedaterminal

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
                var route by remember { mutableStateOf<Route>(Route.PosKey) }

                // 결제 흐름 공유 상태
                var currentAmount by remember { mutableStateOf(0L) }
                var currentMerchant by remember { mutableStateOf("전자 기기 상점 GUMI") }
                var currentMethod by remember { mutableStateOf("페이스페이") }

                // RBA 관련 상태
                var rbaUserId by remember { mutableStateOf("") }
                var rbaAuthSteps by remember { mutableStateOf<List<RbaAuthType>>(emptyList()) }

                when (route) {

                    // ── 1. POS Key 입력 ──────────────────────────
                    Route.PosKey -> PosKeyScreen(
                        onConnected = { route = Route.Waiting }
                    )

                    // ── 2. 대기 화면 (금액 입력 포함) ────────────
                    Route.Waiting -> NaedaStartScreen(
                        onPaymentStart = { amount, merchant ->
                            currentAmount = amount
                            currentMerchant = merchant
                            route = Route.PaymentSelect
                        }
                    )

                    // ── 3. 결제 수단 선택 ─────────────────────────
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

                    // ── 4. 얼굴 인증 ──────────────────────────────
                    Route.FacePay -> FacePayAuthScreen(
                        amount = currentAmount,
                        merchant = currentMerchant,
                        apiBaseUrl = "http://10.0.2.2:8080",
                        topK = 3,
                        onBack = { route = Route.PaymentSelect },
                        onAuthed = { userId, similarity ->
                            val steps = buildList {
                                if (similarity < 0.55) {
                                    add(RbaAuthType.PhoneLastFour)
                                }
                                if (currentAmount >= 50_000L) {
                                    add(RbaAuthType.Signature)
                                }
                            }

                            rbaUserId = userId
                            rbaAuthSteps = steps

                            route = if (steps.isEmpty()) Route.PaymentDone else Route.Rba
                        },
                        onNotMatched = { route = Route.PaymentSelect }
                    )

                    // ── 5. RBA 추가 인증 ──────────────────────────
                    Route.Rba -> RbaAuthContainer(
                        authSteps = rbaAuthSteps,
                        paymentAmount = currentAmount,
                        merchantName = currentMerchant,
                        onAuthComplete = { route = Route.PaymentDone },
                        onAuthCancel = { route = Route.FacePay }
                    )

                    // ── 6. 결제 완료 ──────────────────────────────
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

private sealed interface Route {
    data object PosKey : Route
    data object Waiting : Route
    data object PaymentSelect : Route
    data object FacePay : Route
    data object Rba : Route
    data object PaymentDone : Route
}