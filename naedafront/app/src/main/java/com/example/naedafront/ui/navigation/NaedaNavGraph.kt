package com.example.naedafront.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.naedafront.AuthPrefs
import com.example.naedafront.ui.screen.LoginScreen
import com.example.naedafront.ui.screen.asset.AccountListScreen
import com.example.naedafront.ui.screen.asset.RegisterAssetScreen
import com.example.naedafront.ui.screen.WelcomeScreen
import com.example.naedafront.ui.screen.signup.SignUpNameScreen
import com.example.naedafront.ui.screen.signup.SignUpRrnScreen
import com.example.naedafront.ui.screen.signup.SignUpPhoneScreen
import com.example.naedafront.ui.screen.signup.SignUpVerifyScreen
import com.example.naedafront.ui.screen.signup.SignUpEmailScreen
import com.example.naedafront.ui.screen.signup.SignUpPasswordScreen
import com.example.naedafront.ui.screen.signup.SignUpPinScreen
import com.example.naedafront.ui.screen.home.HomeScreen
import com.example.naedafront.ui.screen.facepay.FaceRegisterScreen
import com.example.naedafront.ui.screen.facepay.FaceMatchRecognizeScreen
import com.example.naedafront.ui.screen.facepay.FaceMatchResultScreen
import com.example.naedafront.ui.screen.home.HomeUiState
import com.example.naedafront.ui.screen.asset.AccountDetailScreen
import com.example.naedafront.ui.screen.asset.sampleAccounts

/**
 * 내다(NAEDA) 전체 네비게이션 그래프
 */
@Composable
fun NaedaNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Welcome.route,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        // ═══ 인증 플로우 ═══

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onStartClick = { navController.navigate(Screen.SignUp.route) },
                onLoginClick = { navController.navigate(Screen.Login.route) }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onBackClick = { navController.popBackStack() },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpNameScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { navController.navigate(Screen.SignUpRrn.route) }
            )
        }

        composable(Screen.SignUpRrn.route) {
            SignUpRrnScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { navController.navigate(Screen.SignUpPhone.route) }
            )
        }

        composable(Screen.SignUpPhone.route) {
            SignUpPhoneScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { phone ->
                    navController.navigate(Screen.SignUpVerify.createRoute(phone))
                }
            )
        }

        composable(
            route = Screen.SignUpVerify.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            SignUpVerifyScreen(
                phoneNumber = phone,
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { navController.navigate(Screen.SignUpEmail.route) },
                onResendClick = { }
            )
        }

        composable(Screen.SignUpEmail.route) {
            SignUpEmailScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { navController.navigate(Screen.SignUpPassword.route) }
            )
        }

        composable(Screen.SignUpPassword.route) {
            SignUpPasswordScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { navController.navigate(Screen.SignUpPin.route) }
            )
        }

        composable(Screen.SignUpPin.route) {
            SignUpPinScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { _ ->
                    AuthPrefs.clearSession(context)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        // ═══════════════════════════════════════
        // 메인 5탭
        // ═══════════════════════════════════════

        composable(Screen.Home.route) {
            val displayName = AuthPrefs.getUsername(context)
                ?.takeUnless { it.isBlank() }
                ?: "사용자"
            HomeScreen(
                uiState = HomeUiState(
                    userName = displayName,
                    isFaceRegistered = AuthPrefs.isFaceRegistered(context)
                ),
                onTransferClick = { navController.navigate(Screen.Transfer.route) },
                onTransactionClick = { navController.navigate(Screen.Transaction.route) },
                onFacePaySettingClick = { navController.navigate(Screen.FaceRegister.route) },
                onLinkAccountClick = { navController.navigate(Screen.AccountList.route) },
                onViewAllTransactionsClick = { navController.navigate(Screen.Transaction.route) },
                onSearchClick = { },
                onAlarmClick = { navController.navigate(Screen.Notification.route) },
                onProfileClick = { navController.navigate(Screen.Settings.route) },
                onSecretFaceMatchTestClick = { navController.navigate(Screen.FaceMatchRecognize.route) }
            )
        }

        composable(Screen.Benefit.route) {
            PlaceholderScreen("🎁 혜택")
        }

        composable(Screen.Scan.route) {
            PlaceholderScreen("🗺️ 구미 맛집 지도")
        }

        // 자산 탭 → AccountListScreen 연결
        composable(Screen.Asset.route) {
            AccountListScreen(
                onBack = { navController.popBackStack() },
                onRegisterNew = { navController.navigate(Screen.RegisterAsset.route) },
                onAccountClick = { account ->
                    navController.navigate(Screen.AccountDetail.createRoute(account.id))
                },
                onDeleteAccount = { /* TODO: ViewModel 연결 후 처리 */ },
                onSetPrimary = { /* TODO: ViewModel 연결 후 처리 */ }
            )
        }

        composable(Screen.More.route) {
            PlaceholderScreen("⋯ 더보기")
        }

        // ═══════════════════════════════════════
        // 스캔 탭 하위 화면
        // ═══════════════════════════════════════

        composable(Screen.FaceRegister.route) {
            FaceRegisterScreen(
                onBack = { navController.popBackStack() },
                onRegisterComplete = {
                    AuthPrefs.setFaceRegistered(context, true)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.FaceMatchRecognize.route) {
            FaceMatchRecognizeScreen(
                onBack = { navController.popBackStack() },
                onShowResult = { navController.navigate(Screen.FaceMatchResult.route) }
            )
        }

        composable(Screen.FaceMatchResult.route) {
            FaceMatchResultScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FaceIntro.route) {
            PlaceholderScreen("페이스페이 소개")
        }

        composable(Screen.FaceGuide.route) {
            PlaceholderScreen("촬영 가이드")
        }

        composable(Screen.FaceCapture.route) {
            PlaceholderScreen("카메라 촬영")
        }

        composable(Screen.FaceAnalyzing.route) {
            PlaceholderScreen("분석 중...")
        }

        composable(Screen.FaceComplete.route) {
            PlaceholderScreen("등록 완료!")
        }

        composable(Screen.GumiMap.route) {
            PlaceholderScreen("🗺️ 구미 맛집 지도")
        }

        composable(
            route = Screen.StoreDetail.route,
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            PlaceholderScreen("매장 상세: $storeId")
        }

        // ═══════════════════════════════════════
        // 자산 탭 하위 화면
        // ═══════════════════════════════════════

        // 계좌 목록 (별도 라우트 — 홈에서도 진입 가능)
        composable(Screen.AccountList.route) {
            AccountListScreen(
                onBack = { navController.popBackStack() },
                onRegisterNew = { navController.navigate(Screen.RegisterAsset.route) },
                onAccountClick = { account ->
                    navController.navigate(Screen.AccountDetail.createRoute(account.id))
                },
                onDeleteAccount = { /* TODO: ViewModel 연결 후 처리 */ },
                onSetPrimary = { /* TODO: ViewModel 연결 후 처리 */ }
            )
        }

        // 새 계좌/카드 등록
        composable(Screen.RegisterAsset.route) {
            RegisterAssetScreen(
                onBack = { navController.popBackStack() },
                onRegisterComplete = { navController.popBackStack() }
            )
        }

        // 계좌 상세
        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
            val account = sampleAccounts.find { it.id == accountId } ?: sampleAccounts.first()
            AccountDetailScreen(
                account = account,
                onBack = { navController.popBackStack() },
                onTransferClick = { navController.navigate(Screen.Transfer.route) }
            )
        }
        composable(Screen.Transfer.route) { PlaceholderScreen("이체") }
        composable(Screen.Transaction.route) { PlaceholderScreen("거래내역") }
        composable(Screen.Report.route) { PlaceholderScreen("📊 소비 리포트") }

        // ═══════════════════════════════════════
        // 혜택 탭 하위 화면
        // ═══════════════════════════════════════

        composable(Screen.Coupon.route) { PlaceholderScreen("할인권 교환") }
        composable(Screen.Donation.route) { PlaceholderScreen("후원하기") }

        // ═══════════════════════════════════════
        // 더보기 탭 하위 화면
        // ═══════════════════════════════════════

        composable(Screen.Settings.route) { PlaceholderScreen("⚙️ 설정") }
        composable(Screen.Notification.route) { PlaceholderScreen("🔔 알림") }
        composable(Screen.Security.route) { PlaceholderScreen("🔒 보안 내역") }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name, fontSize = 24.sp)
    }
}
