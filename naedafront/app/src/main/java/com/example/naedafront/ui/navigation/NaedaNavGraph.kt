package com.example.naedafront.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.naedafront.data.repository.OrderRepository
import com.example.naedafront.ui.screen.LoginScreen
import com.example.naedafront.ui.screen.NotificationScreen
import com.example.naedafront.ui.screen.WelcomeScreen
import com.example.naedafront.ui.screen.asset.AccountDetailRoute
import com.example.naedafront.ui.screen.asset.AccountListRoute
import com.example.naedafront.ui.screen.asset.CardDetailRoute
import com.example.naedafront.ui.screen.asset.RegisterAssetDialog
import com.example.naedafront.ui.screen.asset.RegisterAssetScreen
import com.example.naedafront.ui.screen.asset.TradeReportScreen
import com.example.naedafront.ui.screen.chat.ChatScreen
import com.example.naedafront.ui.screen.facepay.FaceMatchRecognizeScreen
import com.example.naedafront.ui.screen.facepay.FaceMatchResultScreen
import com.example.naedafront.ui.screen.facepay.FaceRegisterScreen
import com.example.naedafront.ui.screen.home.HomeScreen
import com.example.naedafront.ui.screen.home.HomeViewModel
import com.example.naedafront.ui.screen.home.NoticeDetailScreen
import com.example.naedafront.ui.screen.home.NoticeListScreen
import com.example.naedafront.ui.screen.map.MapRegion
import com.example.naedafront.ui.screen.map.MapSelectScreen
import com.example.naedafront.ui.screen.mypage.CustomerCenterScreen
import com.example.naedafront.ui.screen.mypage.MyPageScreen
import com.example.naedafront.ui.screen.mypage.MyPageViewModel
import com.example.naedafront.ui.screen.mypage.PinChangeScreen
import com.example.naedafront.ui.screen.setting.NotificationSettingsScreen
import com.example.naedafront.ui.screen.setting.PrivacyPolicyScreen
import com.example.naedafront.ui.screen.setting.SettingsScreen
import com.example.naedafront.ui.screen.setting.TermsOfServiceScreen
import com.example.naedafront.ui.screen.signup.SignUpEmailScreen
import com.example.naedafront.ui.screen.signup.SignUpNameScreen
import com.example.naedafront.ui.screen.signup.SignUpPasswordScreen
import com.example.naedafront.ui.screen.signup.SignUpPhoneScreen
import com.example.naedafront.ui.screen.signup.SignUpPinScreen
import com.example.naedafront.ui.screen.signup.SignUpRrnScreen
import com.example.naedafront.ui.screen.signup.SignUpViewModel
import com.example.naedafront.ui.screen.store.DeliveryAddressScreen
import com.example.naedafront.ui.screen.store.OrderCompleteScreen
import com.example.naedafront.ui.screen.store.OrderDetailScreen
import com.example.naedafront.ui.screen.store.OrderDetailViewModel
import com.example.naedafront.ui.screen.store.OrderHistoryScreen
import com.example.naedafront.ui.screen.store.OrderHistoryViewModel
import com.example.naedafront.ui.screen.store.PointHistoryScreen
import com.example.naedafront.ui.screen.store.PointStoreScreen
import com.example.naedafront.ui.screen.store.StoreOrderDraftStore
import com.example.naedafront.data.repository.AddressRepository
import com.example.naedafront.data.repository.ProductRepository
import com.example.naedafront.data.repository.UserRepository
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
                onConfirmClick = { navController.navigate(Screen.SignUpPhone.route) }
            )
        }

        composable(Screen.SignUpPhone.route) {
            SignUpPhoneScreen(
                signUpViewModel = signUpViewModel,
                onBackClick = { navController.popBackStack() },
                onConfirmClick = {
                    navController.navigate(Screen.SignUpEmail.route)
                }
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
                        popUpTo(Screen.Welcome.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeTabContent(navController)
        }

        composable(Screen.Store.route) {
            StoreTabContent(navController)
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
                onAddressSelected = {
                    navController.navigate(Screen.OrderComplete.route)
                }
            )
        }

        composable(Screen.OrderComplete.route) {
            val orderInfo = StoreOrderDraftStore.completedOrder

            if (orderInfo == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Home.route) {
                        launchSingleTop = true
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {}
            } else {
                OrderCompleteScreen(
                    orderInfo = orderInfo,
                    onCloseClick = {
                        navController.navigate(Screen.Home.route)
                    },
                    onOrderHistoryClick = {
                        navController.navigate(Screen.OrderHistory.route)
                    },
                    onHomeClick = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }
        }

        composable(Screen.OrderHistory.route) {
            val orderHistoryViewModel: OrderHistoryViewModel = viewModel(
                factory = OrderHistoryViewModel.factory(
                    authPrefs = AuthPrefs,
                    orderRepository = OrderRepository(),
                    context = context
                )
            )

            OrderHistoryScreen(
                viewModel = orderHistoryViewModel,
                onBackClick = { navController.popBackStack() },
                onOrderClick = { orderId ->
                    navController.navigate(Screen.OrderDetail.createRoute(orderId))
                }
            )
        }

        composable(
            route = Screen.OrderDetail.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L

            val orderDetailViewModel: OrderDetailViewModel = viewModel(
                factory = OrderDetailViewModel.factory(
                    orderId = orderId,
                    orderRepository = OrderRepository(),
                    productRepository = ProductRepository(),
                    addressRepository = AddressRepository(),
                    userRepository = UserRepository()
                )
            )

            OrderDetailScreen(
                viewModel = orderDetailViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Scan.route) {
            MapTabContent(navController)
        }

        composable(Screen.Asset.route) {
            AssetTabContent(navController)
        }

        composable(Screen.More.route) {
            SettingsTabContent(navController)
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

        composable(Screen.FaceIntro.route) { PlaceholderScreen("페이스페이 소개") }
        composable(Screen.FaceGuide.route) { PlaceholderScreen("촬영 가이드") }
        composable(Screen.FaceCapture.route) { PlaceholderScreen("카메라 촬영") }
        composable(Screen.FaceAnalyzing.route) { PlaceholderScreen("분석 중...") }
        composable(Screen.FaceComplete.route) { PlaceholderScreen("등록 완료!") }

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
                    selectedRegion.isBlank() && selectedRestaurant.isBlank() -> "🗺️ 구미 맛집 지도"
                    selectedRestaurant.isBlank() -> "🗺️ 구미 맛집 지도\n선택 지역: $selectedRegion"
                    else -> "🗺️ 구미 맛집 지도\n선택 지역: $selectedRegion\n맛집: $selectedRestaurant"
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
            route = Screen.AccountList.route,
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getInt("tab") ?: 0

            AccountListRoute(
                initialTab = tab,
                onBack = { navController.popBackStack() },
                onAccountClick = { account ->
                    navController.navigate(
                        Screen.Transaction.createRoute(
                            assetType = Screen.Transaction.ASSET_TYPE_ACCOUNT,
                            paymentMethodId = account.paymentMethodId
                        )
                    )
                },
                onCardClick = { card ->
                    navController.navigate(
                        Screen.Transaction.createRoute(
                            assetType = Screen.Transaction.ASSET_TYPE_CARD,
                            paymentMethodId = card.paymentMethodId
                        )
                    )
                },
                onDeleteAccount = { },
                onSetPrimary = { },
                onSetPrimaryCard = { },
                onDeleteCard = { },
                useRegisterDialog = true
            )
        }

        composable(
            route = Screen.RegisterAsset.route,
            arguments = listOf(navArgument("tab") { type = NavType.IntType })
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getInt("tab") ?: 0

            RegisterAssetScreen(
                initialTab = tab,
                onDismiss = { navController.popBackStack() },
                onRegisterComplete = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(
                navArgument("accountId") { type = NavType.StringType },
                navArgument("accountNo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
            val accountNo = backStackEntry.arguments?.getString("accountNo") ?: ""

            AccountDetailRoute(
                accountId = accountId,
                accountNo = accountNo,
                onBack = { navController.popBackStack() }
            )
        }

        composable("card_detail/{cardId}") { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId")?.toLongOrNull()
            CardDetailRoute(
                cardId = cardId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Transfer.route) {
            PlaceholderScreen("이체")
        }

        composable(
            route = Screen.Transaction.routeWithArgs,
            arguments = listOf(
                navArgument(Screen.Transaction.ASSET_TYPE_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(Screen.Transaction.PAYMENT_METHOD_ID_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val assetType = backStackEntry.arguments
                ?.getString(Screen.Transaction.ASSET_TYPE_ARG)
                .orEmpty()
                .ifBlank { null }
            val paymentMethodId = backStackEntry.arguments
                ?.getString(Screen.Transaction.PAYMENT_METHOD_ID_ARG)
                .orEmpty()
                .toLongOrNull()

            TradeReportScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Report.route) { PlaceholderScreen("📊 소비 리포트") }
        composable(Screen.Coupon.route) { PlaceholderScreen("할인권 교환") }
        composable(Screen.Donation.route) { PlaceholderScreen("후원하기") }

        composable(Screen.Chat.route) {
            val userNo = remember(context) { AuthPrefs.getUserNo(context) }
            ChatScreen(
                userNo = userNo,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.MyPage.route) {
            val myPageViewModel: MyPageViewModel = viewModel()

            MyPageScreen(
                viewModel = myPageViewModel,
                onBackClick = { navController.popBackStack() },
                onNotificationClick = { navController.navigate(Screen.Notification.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onFaceReRegisterClick = { navController.navigate(Screen.FaceRegister.route) },
                onPinChangeClick = { navController.navigate(Screen.Security.route) },
                onDeliveryAddressClick = { navController.navigate(Screen.DeliveryAddress.route) },
                onOrderHistoryClick = { navController.navigate(Screen.OrderHistory.route) },
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
                onNotificationClick = { navController.navigate(Screen.NotificationSettings.route) },
                onTermsClick = { navController.navigate(Screen.TermsOfService.route) },
                onPrivacyClick = { navController.navigate(Screen.PrivacyPolicy.route) },
                onSupportClick = { navController.navigate(Screen.CustomerCenter.route) },
                onLogoutClick = {
                    AuthPrefs.clearSession(context)
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Notification.route) {
            NotificationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Security.route) {
            PinChangeScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomerCenter.route) {
            CustomerCenterScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.TermsOfService.route) {
            TermsOfServiceScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.NoticeList.route) {
            NoticeListScreen(
                onBackClick = { navController.popBackStack() },
                onItemClick = { type, id ->
                    navController.navigate(Screen.NoticeDetail.createRoute(type, id))
                }
            )
        }

        composable(
            route = Screen.NoticeDetail.route,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val noticeType = backStackEntry.arguments?.getString("type") ?: "notice"
            val noticeId = backStackEntry.arguments?.getLong("id") ?: 0L

            NoticeDetailScreen(
                type = noticeType,
                id = noticeId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun HomeTabContent(
    navController: NavHostController
) {
    val context = LocalContext.current
    val displayName = AuthPrefs.getUsername(context)
        ?.takeUnless { it.isBlank() }
        ?: "사용자"

    var isFaceRegistered by remember {
        mutableStateOf(AuthPrefs.isFaceRegistered(context))
    }

    val homeViewModel: HomeViewModel = viewModel()
    val homeUiState by homeViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        runCatching { FaceRegistrationRepository.getFacePaySettings() }
            .onSuccess { settings ->
                AuthPrefs.saveFacePaySettings(
                    context = context,
                    faceRegistered = settings.faceRegistered,
                    secondaryAuthEnabled = settings.secondaryAuthEnabled
                )
                isFaceRegistered = settings.faceRegistered
                homeViewModel.loadHomeData(
                    context = context,
                    userName = displayName,
                    isFaceRegistered = settings.faceRegistered
                )
            }
            .onFailure {
                homeViewModel.loadHomeData(
                    context = context,
                    userName = displayName,
                    isFaceRegistered = isFaceRegistered
                )
            }
    }

    HomeScreen(
        uiState = homeUiState,
        onTransactionClick = { navController.navigate(Screen.Transaction.createRoute()) },
        onFacePaySettingClick = { navController.navigate(Screen.FaceRegister.route) },
        onLinkAccountClick = { navController.navigate(Screen.AccountList.createRoute(0)) },
        onViewAllTransactionsClick = { navController.navigate(Screen.Transaction.createRoute()) },
        onSearchClick = { },
        onAlarmClick = { navController.navigate(Screen.Notification.route) },
        onProfileClick = { navController.navigate(Screen.MyPage.route) },
        onSecretFaceMatchTestClick = { navController.navigate(Screen.FaceMatchRecognize.route) },
        onRegisterCardClick = { navController.navigate(Screen.Asset.route) },
        onNoticeItemClick = { item ->
            navController.navigate(Screen.NoticeDetail.createRoute(item.type, item.id))
        },
        onNoticeMoreClick = { navController.navigate(Screen.NoticeList.route) }
    )
}

@Composable
private fun StoreTabContent(
    navController: NavHostController
) {
    PointStoreScreen(
        onHistoryClick = { navController.navigate(Screen.PointHistory.route) },
        onPurchaseClick = {
            navController.navigate(Screen.DeliveryAddress.route)
        }
    )
}

@Composable
private fun MapTabContent(
    navController: NavHostController
) {
    MapSelectScreen(
        onBack = { },
        showBackButton = false,
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

@Composable
private fun AssetTabContent(
    navController: NavHostController
) {
    AccountListRoute(
        initialTab = 0,
        onBack = { },
        showBackButton = false,
        onRegisterNewAccount = { navController.navigate(Screen.RegisterAsset.createRoute(0)) },
        onRegisterNewCard = { navController.navigate(Screen.RegisterAsset.createRoute(1)) },
        onAccountClick = { account ->
            navController.navigate(
                Screen.Transaction.createRoute(
                    assetType = Screen.Transaction.ASSET_TYPE_ACCOUNT,
                    paymentMethodId = account.paymentMethodId
                )
            )
        },
        onCardClick = { card ->
            navController.navigate(
                Screen.Transaction.createRoute(
                    assetType = Screen.Transaction.ASSET_TYPE_CARD,
                    paymentMethodId = card.paymentMethodId
                )
            )
        },
        onDeleteAccount = { },
        onSetPrimary = { },
        onSetPrimaryCard = { },
        onDeleteCard = { },
        useRegisterDialog = false
    )
}

@Composable
private fun SettingsTabContent(
    navController: NavHostController
) {
    val context = LocalContext.current

    SettingsScreen(
        onNotificationClick = { navController.navigate(Screen.NotificationSettings.route) },
        onTermsClick = { navController.navigate(Screen.TermsOfService.route) },
        onPrivacyClick = { navController.navigate(Screen.PrivacyPolicy.route) },
        onSupportClick = { navController.navigate(Screen.CustomerCenter.route) },
        onLogoutClick = {
            AuthPrefs.clearSession(context)
            navController.navigate(Screen.Welcome.route) {
                popUpTo(Screen.Welcome.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    )
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
