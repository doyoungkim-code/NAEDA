package com.example.naedafront.ui.screen.facepay

import androidx.compose.runtime.Composable

@Composable
fun FaceRegisterScreen(
    onBack: () -> Unit,
    onRegisterComplete: () -> Unit
) {
    FaceRegisterFlowScreen(
        onBack = onBack,
        onRegisterComplete = onRegisterComplete
    )
}
