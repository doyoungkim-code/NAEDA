package com.example.naedafront

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.naedafront.data.remote.ApiConfig
import com.example.naedafront.ui.common.NaedaBottomNavBar
import com.example.naedafront.ui.navigation.NaedaNavGraph
import com.example.naedafront.ui.navigation.Screen
import com.example.naedafront.ui.theme.NaedaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiConfig.initialize(applicationContext)
        enableEdgeToEdge()

        setContent {
            NaedaTheme {
                NaedaApp()
            }
        }

        // Android 13+ 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // 로그인 상태면 FCM 토큰을 서버에 등록
        if (AuthPrefs.hasSession(this)) {
            com.example.naedafront.fcm.NaedaFirebaseMessagingService.registerCurrentToken(this)
        }

        // FCM 토큰 확인용 (디버그)
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                android.util.Log.d("FCM_TOKEN", "토큰: $token")
            }
    }
}

@Composable
fun NaedaApp() {
    val context = LocalContext.current
    val startDestination =
        if (AuthPrefs.hasSession(context)) Screen.Home.route else Screen.Welcome.route

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Store.route,
        Screen.Scan.route,
        Screen.PointHistory.route,
        Screen.Asset.route,
        Screen.More.route
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NaedaBottomNavBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { innerPadding ->
        NaedaNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        )
    }
}