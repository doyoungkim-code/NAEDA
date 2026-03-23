package com.example.naedafront.ui.navigation

sealed class Screen(val route: String) {

    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")

    object SignUpGraph : Screen("signup_graph")

    object OrderComplete : Screen("order_complete")
    object OrderHistory : Screen("order_history")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: Long) = "order_detail/$orderId"
    }

    object DeliveryAddress : Screen("delivery_address")
    object SignUp : Screen("signup")
    object SignUpRrn : Screen("signup_rrn")
    object SignUpPhone : Screen("signup_phone")

    object SignUpVerify : Screen("signup_verify/{phone}") {
        fun createRoute(phone: String) = "signup_verify/$phone"
    }

    object SignUpEmail : Screen("signup_email")
    object SignUpPassword : Screen("signup_password")
    object SignUpPin : Screen("signup_pin")

    object Home : Screen("home")
    object Store : Screen("store")
    object PointHistory : Screen("point_history")
    object Scan : Screen("scan")
    object Asset : Screen("asset")
    object More : Screen("more")

    object FaceRegister : Screen("face_register")
    object FaceIntro : Screen("face_intro")
    object FaceGuide : Screen("face_guide")
    object FaceMatchRecognize : Screen("face_match_recognize")
    object FaceMatchResult : Screen("face_match_result")
    object FaceCapture : Screen("face_capture")
    object FaceAnalyzing : Screen("face_analyzing")
    object FaceComplete : Screen("face_complete")
    object GumiMap : Screen("gumi_map")

    object StoreDetail : Screen("store_detail/{storeId}") {
        fun createRoute(storeId: String) = "store_detail/$storeId"
    }

    object AccountList : Screen("account_list/{tab}") {
        fun createRoute(tab: Int = 0) = "account_list/$tab"
    }

    object RegisterAsset : Screen("register_asset/{tab}") {
        fun createRoute(tab: Int) = "register_asset/$tab"
    }

    object AccountDetail : Screen("account_detail/{accountId}/{accountNo}") {
        fun createRoute(accountId: Long?, accountNo: String) =
            "account_detail/${accountId ?: 0}/$accountNo"
    }

    object CardDetail : Screen("card_detail/{cardId}") {
        fun createRoute(cardId: String) = "card_detail/$cardId"
    }

    object Transfer : Screen("transfer")
    object Transaction : Screen("transaction")
    object Report : Screen("report")

    object Coupon : Screen("coupon")
    object Donation : Screen("donation")

    object Chat : Screen("chat")

    object MyPage : Screen("mypage")
    object Settings : Screen("settings")
    object Notification : Screen("notification")
    object NotificationSettings : Screen("notification_settings")
    object TermsOfService : Screen("terms_of_service")
    object PrivacyPolicy : Screen("privacy_policy")
    object Security : Screen("security")
    object CustomerCenter : Screen("customer_center")

    object NoticeList : Screen("notice_list")

    object NoticeDetail : Screen("notice_detail/{type}/{id}") {
        fun createRoute(type: String, id: Long) = "notice_detail/$type/$id"
    }
}