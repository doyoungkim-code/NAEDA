package com.example.naedaterminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.naedaterminal.ui.screen.*
import com.example.naedaterminal.ui.theme.NaedaterminalTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NaedaterminalTheme {
                var route by remember { mutableStateOf<Route>(Route.Start) }

                // 결제 결과 더미 데이터(폰 없어도 UI 확인용)
                var lastPaidMethod by remember { mutableStateOf("FACE PAY") }
                var lastAmount by remember { mutableStateOf(4500L) }
                var lastApprovalNo by remember { mutableStateOf("A-20260305-0001") }
                var lastApprovedAt by remember { mutableStateOf("2026-03-05 14:30") }

                when (route) {
                    Route.Start -> NaedaStartScreen(
                        onStart = { route = Route.PaymentSelect },
                        onTerminalMode = { /* TODO */ }
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
                                    // TODO: 삼성페이 결제 흐름
                                    route = Route.PaymentDone
                                }
                                PaymentMethod.CARD -> {
                                    lastPaidMethod = "CARD"
                                    // TODO: 카드 결제 흐름
                                    route = Route.PaymentDone
                                }
                            }
                        }
                    )

                    Route.FacePay -> FacePayAuthScreen(
                        apiBaseUrl = "http://10.0.2.2:8080",
                        topK = 3,
                        onBack = { route = Route.PaymentSelect },
                        onAuthed = { _, _ ->
                            // ✅ 인증 성공 = 결제 성공으로 가정하고 완료 화면 이동(지금은 폰 없으니 더미)
                            route = Route.PaymentDone
                        },
                        onNotMatched = {
                            // TODO: 불일치 시 2차 인증 화면
                            route = Route.PaymentSelect
                        }
                    )

                    Route.PaymentDone -> PaymentDoneScreen(
                        onDone = { route = Route.Start },
                        onReceipt = { /* TODO: 영수증 화면 */ },
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
    data object PaymentDone : Route
}