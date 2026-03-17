package com.example.naedafront

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

        // FCM 토큰 확인용 (나중에 지워도 됨)
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