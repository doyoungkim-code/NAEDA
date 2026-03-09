package com.example.naedaterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.naedaterminal.ui.screen.*
import com.example.naedaterminal.ui.screen.payment.RbaAuthContainer
import com.example.naedaterminal.ui.screen.payment.RbaAuthType
import com.example.naedaterminal.ui.theme.NaedaTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NaedaTheme {
                var route by remember { mutableStateOf<Route>(Route.Start) }

                // 결제 결과 데이터
                var lastPaidMethod by remember { mutableStateOf("FACE PAY") }
                var lastAmount by remember { mutableStateOf(4500L) }
                var lastApprovalNo by remember { mutableStateOf("A-20260305-0001") }
                var lastApprovedAt by remember { mutableStateOf("2026-03-05 14:30") }

                // RBA 관련 상태
                var rbaUserId by remember { mutableStateOf("") }
                var rbaAmount by remember { mutableStateOf(0L) }
                var rbaAuthSteps by remember { mutableStateOf<List<RbaAuthType>>(emptyList()) }

                when (route) {
                    Route.Start -> NaedaStartScreen(
                        onStart = { route = Route.PaymentSelect },
                        onTerminalMode = { }
                    )

                    Route.PaymentSelect -> PaymentMethodSelectScreen(
                        onBack = { route = Route.Start },
                        onSelect = { method: PaymentMethod ->
                            when (method) {
                                PaymentMethod.FACE_PAY -> {
                                    lastPaidMethod = "FACE PAY"
                                    route = Route.FacePay
                                }
                                PaymentMethod.SAMSUNG_PAY -> {
                                    lastPaidMethod = "SAMSUNG PAY"
                                    route = Route.PaymentDone
                                }
                                PaymentMethod.CARD -> {
                                    lastPaidMethod = "CARD"
                                    route = Route.PaymentDone
                                }
                            }
                        }
                    )

                    Route.FacePay -> FacePayAuthScreen(
                        apiBaseUrl = "http://10.0.2.2:8080",
                        topK = 3,
                        onBack = { route = Route.PaymentSelect },
                        onAuthed = { userId, similarity ->
                            // RBA 필요 여부 판단
                            // 유사도 0.55 미만 or 5만원 이상이면 RBA 트리거
                            val steps = buildList {
                                if (similarity < 0.55) {
                                    // TODO: 실제로는 서버(BE-012)에서 hasPinRegistered 받아야 함
                                    // 임시로 전화번호 인증 사용
                                    add(RbaAuthType.PhoneLastFour)
                                }
                                if (lastAmount >= 50_000L) {
                                    add(RbaAuthType.Signature)
                                }
                            }

                            rbaUserId = userId
                            rbaAmount = lastAmount
                            rbaAuthSteps = steps

                            if (steps.isEmpty()) {
                                // RBA 불필요 → 바로 결제 완료
                                route = Route.PaymentDone
                            } else {
                                route = Route.Rba
                            }
                        },
                        onNotMatched = {
                            route = Route.PaymentSelect
                        }
                    )

                    Route.Rba -> RbaAuthContainer(
                        authSteps = rbaAuthSteps,
                        paymentAmount = rbaAmount,
                        merchantName = "SSAFY 편의점",
                        onAuthComplete = {
                            route = Route.PaymentDone
                        },
                        onAuthCancel = {
                            route = Route.FacePay
                        }
                    )

                    Route.PaymentDone -> PaymentDoneScreen(
                        onDone = { route = Route.Start },
                        onReceipt = { },
                        merchantName = "SSAFY 편의점",
                        orderName = "아메리카노 1잔",
                        amountWon = lastAmount,
                        paidMethodLabel = lastPaidMethod,
                        approvedAt = lastApprovedAt,
                        approvalNo = lastApprovalNo
                    )
                }
            }
        }
    }
}

private sealed interface Route {
    data object Start : Route
    data object PaymentSelect : Route
    data object FacePay : Route
    data object Rba : Route         // ← 추가
    data object PaymentDone : Route
}