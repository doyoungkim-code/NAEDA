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
import com.example.naedafront.ui.screen.WelcomeScreen
import com.example.naedafront.ui.screen.SignUpEmailScreen
import com.example.naedafront.ui.screen.SignUpNameScreen
import com.example.naedafront.ui.screen.SignUpPasswordScreen
import com.example.naedafront.ui.screen.SignUpPhoneScreen
import com.example.naedafront.ui.screen.SignUpPinScreen
import com.example.naedafront.ui.screen.SignUpRrnScreen
import com.example.naedafront.ui.screen.SignUpVerifyScreen
import com.example.naedafront.ui.screen.facepay.FaceRegisterScreen
import com.example.naedafront.ui.screen.home.HomeScreen
import com.example.naedafront.ui.screen.home.HomeUiState
import com.example.naedafront.ui.screen.map.MapSelectScreen

@Composable
fun NaedaNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Scan.route,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onStartClick = { navController.navigate(Screen.SignUp.route) },
                onLoginClick = { navController.navigate(Screen.Login.route) }
            )
        }

        composable(Screen.Login.route) {
            PlaceholderScreen("로그인")
        }

        composable(Screen.SignUp.route) {
            SignUpNameScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmClick = {
                    navController.navigate(Screen.SignUpRrn.route)
                }
            )
        }

        composable(Screen.SignUpRrn.route) {
            SignUpRrnScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmClick = {
                    navController.navigate(Screen.SignUpPhone.route)
                }
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
                onConfirmClick = {
                    navController.navigate(Screen.SignUpEmail.route)
                },
                onResendClick = { }
            )
        }

        composable(Screen.SignUpEmail.route) {
            SignUpEmailScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmClick = {
                    navController.navigate(Screen.SignUpPassword.route)
                }
            )
        }

        composable(Screen.SignUpPassword.route) {
            SignUpPasswordScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmClick = {
                    navController.navigate(Screen.SignUpPin.route)
                }
            )
        }

        composable(Screen.SignUpPin.route) {
            SignUpPinScreen(
                onBackClick = { navController.popBackStack() },
                onConfirmClick = {
                    AuthPrefs.setLoggedIn(context, true)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                uiState = HomeUiState(isFaceRegistered = AuthPrefs.isFaceRegistered(context)),
                onTransferClick = { navController.navigate(Screen.Transfer.route) },
                onTransactionClick = { navController.navigate(Screen.Transaction.route) },
                onFacePaySettingClick = { navController.navigate(Screen.FaceRegister.route) },
                onLinkAccountClick = { },
                onViewAllTransactionsClick = { navController.navigate(Screen.Transaction.route) },
                onSearchClick = { },
                onAlarmClick = { navController.navigate(Screen.Notification.route) },
                onProfileClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Benefit.route) {
            PlaceholderScreen("🎁 혜택")
        }

        composable(Screen.Scan.route) {
            MapSelectScreen(
                onBack = { navController.popBackStack() },
                onRestaurantClick = { region, restaurantName ->
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedRegion", region.label)

                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedRestaurant", restaurantName)

                    navController.navigate(Screen.GumiMap.route)
                }
            )
        }

        composable(Screen.Asset.route) {
            PlaceholderScreen("💰 자산")
        }

        composable(Screen.More.route) {
            PlaceholderScreen("⋯ 더보기")
        }

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
            val selectedRegion = navController
                .previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("selectedRegion")
                .orEmpty()

            val selectedRestaurant = navController
                .previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("selectedRestaurant")
                .orEmpty()

            PlaceholderScreen(
                when {
                    selectedRegion.isBlank() && selectedRestaurant.isBlank() -> {
                        "🗺️ 구미 맛집 지도"
                    }
                    selectedRestaurant.isBlank() -> {
                        "🗺️ 구미 맛집 지도\n선택 지역: $selectedRegion"
                    }
                    else -> {
                        "🗺️ 구미 맛집 지도\n선택 지역: $selectedRegion\n맛집: $selectedRestaurant"
                    }
                }
            )
        }

        composable(
            route = Screen.StoreDetail.route,
            arguments = listOf(navArgument("storeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            PlaceholderScreen("매장 상세: $storeId")
        }

        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
            PlaceholderScreen("계좌 상세: $accountId")
        }

        composable(Screen.Transfer.route) {
            PlaceholderScreen("이체")
        }

        composable(Screen.Transaction.route) {
            PlaceholderScreen("거래내역")
        }

        composable(Screen.Report.route) {
            PlaceholderScreen("📊 소비 리포트")
        }

        composable(Screen.Coupon.route) {
            PlaceholderScreen("할인권 교환")
        }

        composable(Screen.Donation.route) {
            PlaceholderScreen("후원하기")
        }

        composable(Screen.Settings.route) {
            PlaceholderScreen("⚙️ 설정")
        }

        composable(Screen.Notification.route) {
            PlaceholderScreen("🔔 알림")
        }

        composable(Screen.Security.route) {
            PlaceholderScreen("🔒 보안 내역")
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontSize = 24.sp
        )
    }
}