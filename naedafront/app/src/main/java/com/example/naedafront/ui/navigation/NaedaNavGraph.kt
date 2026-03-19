package com.example.naedafront.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.FaceRegistrationRepository
import com.example.naedafront.ui.screen.LoginScreen
import com.example.naedafront.ui.screen.WelcomeScreen
import com.example.naedafront.ui.screen.asset.AccountDetailScreen
import com.example.naedafront.ui.screen.asset.AccountListScreen
import com.example.naedafront.ui.screen.asset.CardDetailScreen
import com.example.naedafront.ui.screen.asset.RegisterAssetScreen
import com.example.naedafront.ui.screen.asset.TradeReportScreen
import com.example.naedafront.ui.screen.facepay.FaceMatchRecognizeScreen
import com.example.naedafront.ui.screen.facepay.FaceMatchResultScreen
import com.example.naedafront.ui.screen.facepay.FaceRegisterScreen
import com.example.naedafront.ui.screen.home.HomeScreen
import com.example.naedafront.ui.screen.home.HomeUiState
import com.example.naedafront.ui.screen.map.MapRegion
import com.example.naedafront.ui.screen.map.MapSelectScreen
import com.example.naedafront.ui.screen.mypage.MyPageScreen
import com.example.naedafront.ui.screen.mypage.MyPageViewModel
import com.example.naedafront.ui.screen.setting.SettingsScreen
import com.example.naedafront.ui.screen.signup.SignUpEmailScreen
import com.example.naedafront.ui.screen.signup.SignUpNameScreen
import com.example.naedafront.ui.screen.signup.SignUpPasswordScreen
import com.example.naedafront.ui.screen.signup.SignUpPhoneScreen
import com.example.naedafront.ui.screen.signup.SignUpPinScreen
import com.example.naedafront.ui.screen.signup.SignUpRrnScreen
import com.example.naedafront.ui.screen.signup.SignUpVerifyScreen
import com.example.naedafront.ui.screen.signup.SignUpViewModel
import com.example.naedafront.ui.screen.store.DeliveryAddressScreen
import com.example.naedafront.ui.screen.store.OrderCompleteScreen
import com.example.naedafront.ui.screen.store.PointHistoryScreen
import com.example.naedafront.ui.screen.store.PointStoreScreen

@Composable
fun NaedaNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Scan.route,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val signUpViewModel: SignUpViewModel = viewModel()

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
                signUpViewModel = signUpViewModel,
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { navController.navigate(Screen.SignUpRrn.route) }
            )
        }

        composable(Screen.SignUpRrn.route) {
            SignUpRrnScreen(
                signUpViewModel = signUpViewModel,
                onBackClick = { navController.popBackStack() },
                onConfirmClick = {
                    navController.navigate(Screen.SignUpPhone.route)
                }
            )
        }

        composable(Screen.SignUpPhone.route) {
            SignUpPhoneScreen(
                signUpViewModel = signUpViewModel,
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { phone ->
                    navController.navigate(Screen.SignUpVerify.createRoute(phone))
                }
            )
        }

        composable(
            route = Screen.SignUpVerify.route,
            arguments = listOf(
                navArgument("phone") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""

            SignUpVerifyScreen(
                signUpViewModel = signUpViewModel,
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
                signUpViewModel = signUpViewModel,
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { navController.navigate(Screen.SignUpPassword.route) }
            )
        }

        composable(Screen.SignUpPassword.route) {
            SignUpPasswordScreen(
                signUpViewModel = signUpViewModel,
                onBackClick = { navController.popBackStack() },
                onConfirmClick = { navController.navigate(Screen.SignUpPin.route) }
            )
        }

        composable(Screen.SignUpPin.route) {
            SignUpPinScreen(
                signUpViewModel = signUpViewModel,
                onBackClick = { navController.popBackStack() },
                onConfirmClick = {
                    AuthPrefs.setLoggedIn(context, false)
                    AuthPrefs.setFaceRegistered(context, false)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val displayName = AuthPrefs.getUsername(context)
                ?.takeUnless { it.isBlank() }
                ?: "사용자"

            var isFaceRegistered by remember {
                mutableStateOf(AuthPrefs.isFaceRegistered(context))
            }

            LaunchedEffect(Unit) {
                runCatching { FaceRegistrationRepository.getFacePaySettings() }
                    .onSuccess { settings ->
                        AuthPrefs.saveFacePaySettings(
                            context = context,
                            faceRegistered = settings.faceRegistered,
                            secondaryAuthEnabled = settings.secondaryAuthEnabled
                        )
                        isFaceRegistered = settings.faceRegistered
                    }
            }

            HomeScreen(
                uiState = HomeUiState(
                    userName = displayName,
                    isFaceRegistered = isFaceRegistered
                ),
                onTransactionClick = { navController.navigate(Screen.Transaction.route) },
                onFacePaySettingClick = { navController.navigate(Screen.FaceRegister.route) },
                onLinkAccountClick = { navController.navigate(Screen.AccountList.createRoute(0)) },
                onViewAllTransactionsClick = { navController.navigate(Screen.Transaction.route) },
                onSearchClick = { },
                onAlarmClick = { navController.navigate(Screen.Notification.route) },
                onProfileClick = { navController.navigate(Screen.MyPage.route) },
                onSecretFaceMatchTestClick = { navController.navigate(Screen.FaceMatchRecognize.route) }
            )
        }

        composable(Screen.Store.route) {
            PointStoreScreen(
                onCartClick = { },
                onGiftClick = { },
                onHistoryClick = {
                    navController.navigate(Screen.PointHistory.route)
                },
                onPurchaseClick = { _, _ ->
                    navController.navigate(Screen.DeliveryAddress.route)
                }
            )
        }

        composable(Screen.PointHistory.route) {
            PointHistoryScreen(
                onBackClick = { navController.popBackStack() },
                onGiftClick = { }
            )
        }

        composable(Screen.DeliveryAddress.route) {
            DeliveryAddressScreen(
                onBackClick = { navController.popBackStack() },
                onSearchPostCodeClick = { },
                onRequestClick = { },
                onSaveAndPayClick = { _, _, _, _, _, _, _ ->
                    navController.navigate(Screen.OrderComplete.route)
                }
            )
        }

        composable(Screen.OrderComplete.route) {
            OrderCompleteScreen(
                onCloseClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOrderHistoryClick = {
                    navController.navigate(Screen.PointHistory.route)
                },
                onHomeClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Scan.route) {
            MapSelectScreen(
                onBack = { navController.popBackStack() },
                onRestaurantClick = { region: MapRegion, restaurantName: String ->
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
            AccountListScreen(
                initialTab = 0,
                onBack = { navController.popBackStack() },
                onRegisterNewAccount = {
                    navController.navigate(Screen.RegisterAsset.createRoute(0))
                },
                onRegisterNewCard = {
                    navController.navigate(Screen.RegisterAsset.createRoute(1))
                },
                onAccountClick = { account ->
                    navController.navigate(Screen.AccountDetail.createRoute(account.id))
                },
                onCardClick = { card ->
                    navController.navigate(Screen.CardDetail.createRoute(card.id))
                },
                onDeleteAccount = { },
                onSetPrimary = { },
                onSetPrimaryCard = { },
                onDeleteCard = { }
            )
        }

        composable(Screen.More.route) {
            SettingsScreen(
                onNotificationClick = {
                    navController.navigate(Screen.Notification.route)
                },
                onPinChangeClick = {
                    navController.navigate(Screen.Security.route)
                },
                onTermsClick = { },
                onPrivacyClick = { },
                onSupportClick = { },
                onLogoutClick = {
                    AuthPrefs.clearSession(context)
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onWithdrawClick = { }
            )
        }

        composable(Screen.FaceRegister.route) {
            FaceRegisterScreen(
                onBack = { navController.popBackStack() },
                onRegisterComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
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
            arguments = listOf(
                navArgument("storeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val storeId = backStackEntry.arguments?.getString("storeId") ?: ""
            PlaceholderScreen("매장 상세: $storeId")
        }

        composable(
            route = Screen.AccountList.route,
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getInt("tab") ?: 0

            AccountListScreen(
                initialTab = tab,
                onBack = { navController.popBackStack() },
                onRegisterNewAccount = {
                    navController.navigate(Screen.RegisterAsset.createRoute(0))
                },
                onRegisterNewCard = {
                    navController.navigate(Screen.RegisterAsset.createRoute(1))
                },
                onAccountClick = { account ->
                    navController.navigate(Screen.AccountDetail.createRoute(account.id))
                },
                onCardClick = { card ->
                    navController.navigate(Screen.CardDetail.createRoute(card.id))
                },
                onDeleteAccount = { },
                onSetPrimary = { },
                onSetPrimaryCard = { },
                onDeleteCard = { }
            )
        }

        composable(
            route = Screen.RegisterAsset.route,
            arguments = listOf(
                navArgument("tab") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getInt("tab") ?: 0

            RegisterAssetScreen(
                initialTab = tab,
                onBack = {
                    navController.navigate(Screen.AccountList.createRoute(tab)) {
                        popUpTo(Screen.AccountList.route) { inclusive = true }
                    }
                },
                onRegisterComplete = {
                    navController.navigate(Screen.AccountList.createRoute(tab)) {
                        popUpTo(Screen.AccountList.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(
                navArgument("accountId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: ""

            AccountDetailScreen(
                accountId = accountId,
                onBack = { navController.popBackStack() },
                onTransferClick = { navController.navigate(Screen.Transfer.route) }
            )
        }

        composable(
            route = Screen.CardDetail.route,
            arguments = listOf(
                navArgument("cardId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId") ?: ""

            CardDetailScreen(
                cardId = cardId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Transfer.route) {
            PlaceholderScreen("이체")
        }

        composable(Screen.Transaction.route) {
            TradeReportScreen(
                onBackClick = { navController.popBackStack() }
            )
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

        composable(Screen.MyPage.route) {
            val myPageViewModel: MyPageViewModel = viewModel()

            MyPageScreen(
                viewModel = myPageViewModel,
                onBackClick = { navController.popBackStack() },
                onNotificationClick = {
                    navController.navigate(Screen.Notification.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onFaceReRegisterClick = {
                    navController.navigate(Screen.FaceRegister.route)
                },
                onPinChangeClick = {
                    navController.navigate(Screen.Security.route)
                },
                onEditProfileClick = { },
                onContactManageClick = { },
                onCustomerCenterClick = { },
                onLogoutClick = {
                    AuthPrefs.clearSession(context)
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onFabClick = { }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNotificationClick = {
                    navController.navigate(Screen.Notification.route)
                },
                onPinChangeClick = {
                    navController.navigate(Screen.Security.route)
                },
                onTermsClick = { },
                onPrivacyClick = { },
                onSupportClick = { },
                onLogoutClick = {
                    AuthPrefs.clearSession(context)
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onWithdrawClick = { }
            )
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