package com.example.naedafront.ui.navigation

/**
 * 앱 전체 화면 Route 정의
 * 네비게이션 그래프에서 각 화면을 식별하는 데 사용
 */
sealed class Screen(val route: String) {

    // ── 인증 플로우 ──
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")

    object SignUpRrn : Screen("signup_rrn")
    object SignUpPhone : Screen("signup_phone")
    object SignUpVerify : Screen("signup_verify/{phone}") {
        fun createRoute(phone: String) = "signup_verify/$phone"
    }
    object SignUpEmail : Screen("signup_email")
    object SignUpPassword : Screen("signup_password")
    object SignUpPin : Screen("signup_pin")
    object SignUp : Screen("signup")

    // ── 메인 5탭 ──
    object Home : Screen("home")
    object Benefit : Screen("benefit")
    object Scan : Screen("scan")
    object Asset : Screen("asset")
    object More : Screen("more")

    // ── 스캔 탭 하위 ──
    object FaceRegister : Screen("face_register")
    object FaceMatchRecognize : Screen("face_match_recognize")
    object FaceMatchResult : Screen("face_match_result")
    object FaceIntro : Screen("face_intro")
    object FaceGuide : Screen("face_guide")
    object FaceCapture : Screen("face_capture")
    object FaceAnalyzing : Screen("face_analyzing")
    object FaceComplete : Screen("face_complete")
    object GumiMap : Screen("gumi_map")
    object StoreDetail : Screen("store_detail/{storeId}") {
        fun createRoute(storeId: String) = "store_detail/$storeId"
    }

    // ── 자산 탭 하위 ──
    object AccountList : Screen("account_list")          // 계좌 목록 (계좌 및 카드 관리)
    object RegisterAsset : Screen("register_asset")      // 새 계좌/카드 등록
    object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: String) = "account_detail/$accountId"
    }
    object Transfer : Screen("transfer")
    object Transaction : Screen("transaction")
    object Report : Screen("report")

    // ── 혜택 탭 하위 ──
    object Coupon : Screen("coupon")
    object Donation : Screen("donation")

    // ── 더보기 탭 하위 ──
    object Settings : Screen("settings")
    object Notification : Screen("notification")
    object Security : Screen("security")
}
