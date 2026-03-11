package com.example.naedaterminal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.naedaterminal.ui.screen.FacePayAuthScreen
import com.example.naedaterminal.ui.screen.NaedaStartScreen
import com.example.naedaterminal.ui.screen.PaymentDoneScreen
import com.example.naedaterminal.ui.screen.PaymentMethod
import com.example.naedaterminal.ui.screen.PaymentMethodSelectScreen
import com.example.naedaterminal.ui.screen.payment.RbaAuthContainer
import com.example.naedaterminal.ui.screen.payment.RbaAuthType

// ─────────────────────────────────────────────
// 라우트 정의
// ─────────────────────────────────────────────
sealed class Screen(val route: String) {

    // 시작
    object Start : Screen("start")

    // 결제 수단 선택
    object PaymentMethodSelect : Screen("payment_method_select")

    // 얼굴 인증
    // amount: 결제 금액 (Long)
    object FacePayAuth : Screen("face_pay_auth/{amount}") {
        fun createRoute(amount: Long) = "face_pay_auth/$amount"
    }

    // RBA 추가 인증
    // amount       : 결제 금액 (Long)
    // userId       : 얼굴 인식으로 확인된 사용자 ID
    // similarity   : 얼굴 유사도 (Double → String 변환)
    // hasPinRegistered : PIN 등록 여부 (Boolean)
    object RbaAuth : Screen("rba_auth/{amount}/{userId}/{similarity}/{hasPinRegistered}") {
        fun createRoute(
            amount: Long,
            userId: String,
            similarity: Double,
            hasPinRegistered: Boolean
        ) = "rba_auth/$amount/$userId/$similarity/$hasPinRegistered"
    }

    // 결제 완료
    // amount       : 결제 금액 (Long)
    // userId       : 사용자 ID
    object PaymentDone : Screen("payment_done/{amount}/{userId}") {
        fun createRoute(amount: Long, userId: String) = "payment_done/$amount/$userId"
    }
}

// ─────────────────────────────────────────────
// NavGraph
// ─────────────────────────────────────────────

/** 단말기 앱 전체 네비게이션 그래프 */
@Composable
fun NaedaTerminalNavGraph(
    navController: NavHostController,
    apiBaseUrl: String,                     // 서버 주소 (예: "http://192.168.0.10:8080")
    rbaThresholdSimilarity: Double = 0.55,  // 유사도 이 값 미만이면 RBA 트리거
    rbaThresholdAmount: Long = 50_000L,     // 이 금액 이상이면 전자서명 추가
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Start.route,
        modifier = modifier
    ) {

        // ═══ 시작 화면 ═══
        composable(Screen.Start.route) {
            NaedaStartScreen(
                onStart = {
                    navController.navigate(Screen.PaymentMethodSelect.route)
                },
                onTerminalMode = {
                    navController.navigate(Screen.PaymentMethodSelect.route)
                }
            )
        }

        // ═══ 결제 수단 선택 ═══
        composable(Screen.PaymentMethodSelect.route) {
            PaymentMethodSelectScreen(
                onBack = { navController.popBackStack() },
                onSelect = { method ->
                    when (method) {
                        PaymentMethod.FACE_PAY -> {
                            // TODO: 금액 입력 화면 추가 시 amount 받아오기
                            // 임시로 금액 0으로 진입 (금액 입력 화면 생기면 수정)
                            navController.navigate(Screen.FacePayAuth.createRoute(amount = 0L))
                        }
                        PaymentMethod.SAMSUNG_PAY -> { /* TODO */ }
                        PaymentMethod.CARD -> { /* TODO */ }
                    }
                }
            )
        }

        // ═══ 얼굴 인증 ═══
        composable(
            route = Screen.FacePayAuth.route,
            arguments = listOf(
                navArgument("amount") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val amount = backStackEntry.arguments?.getLong("amount") ?: 0L

            FacePayAuthScreen(
                apiBaseUrl = apiBaseUrl,
                onBack = { navController.popBackStack() },
                onAuthed = { userId, similarity ->
                    // 얼굴 인식 성공 → RBA 필요 여부 판단
                    val needsRba = similarity < rbaThresholdSimilarity || amount >= rbaThresholdAmount

                    if (needsRba) {
                        // RBA 화면으로 이동
                        // hasPinRegistered: TODO - 실제로는 서버 응답(BE-012)에서 받아야 함
                        // 임시로 false (전화번호 인증)
                        navController.navigate(
                            Screen.RbaAuth.createRoute(
                                amount = amount,
                                userId = userId,
                                similarity = similarity,
                                hasPinRegistered = false
                            )
                        )
                    } else {
                        // RBA 불필요 → 바로 결제 완료
                        navController.navigate(
                            Screen.PaymentDone.createRoute(amount = amount, userId = userId)
                        ) {
                            popUpTo(Screen.FacePayAuth.route) { inclusive = true }
                        }
                    }
                },
                onNotMatched = {
                    // 얼굴 인식 실패 → 다시 시도 or 다른 결제 수단 선택
                    navController.popBackStack()
                }
            )
        }

        // ═══ RBA 추가 인증 ═══
        composable(
            route = Screen.RbaAuth.route,
            arguments = listOf(
                navArgument("amount") { type = NavType.LongType },
                navArgument("userId") { type = NavType.StringType },
                navArgument("similarity") { type = NavType.StringType },
                navArgument("hasPinRegistered") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val amount = backStackEntry.arguments?.getLong("amount") ?: 0L
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val similarity = backStackEntry.arguments?.getString("similarity")?.toDoubleOrNull() ?: 0.0
            val hasPinRegistered = backStackEntry.arguments?.getBoolean("hasPinRegistered") ?: false

            // 인증 단계 결정
            // - 유사도 낮음 → PIN(등록 시) or 전화번호 4자리
            // - 금액 5만원 초과 → 전자서명 추가
            val authSteps = buildList {
                if (similarity < rbaThresholdSimilarity) {
                    if (hasPinRegistered) add(RbaAuthType.Pin)
                    else add(RbaAuthType.PhoneLastFour)
                }
                if (amount >= rbaThresholdAmount) {
                    add(RbaAuthType.Signature)
                }
            }

            RbaAuthContainer(
                authSteps = authSteps,
                paymentAmount = amount,
                // TODO: 가맹점명 실제 데이터로 교체
                merchantName = "내다 가맹점",
                onAuthComplete = {
                    // 모든 추가 인증 통과 → 결제 완료
                    navController.navigate(
                        Screen.PaymentDone.createRoute(amount = amount, userId = userId)
                    ) {
                        popUpTo(Screen.FacePayAuth.route) { inclusive = true }
                    }
                },
                onAuthCancel = {
                    // 취소 → 얼굴 인증 화면으로 돌아가기
                    navController.popBackStack(Screen.FacePayAuth.route, inclusive = false)
                }
            )
        }

        // ═══ 결제 완료 ═══
        composable(
            route = Screen.PaymentDone.route,
            arguments = listOf(
                navArgument("amount") { type = NavType.LongType },
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val amount = backStackEntry.arguments?.getLong("amount") ?: 0L
            val userId = backStackEntry.arguments?.getString("userId") ?: ""

            PaymentDoneScreen(
                amountWon = amount,
                onDone = {
                    // 처음(Start)으로 돌아가기
                    navController.navigate(Screen.Start.route) {
                        popUpTo(Screen.Start.route) { inclusive = true }
                    }
                },
                onReceipt = {
                    // TODO: 영수증 화면
                }
            )
        }
    }
}