package com.example.naedafront.ui.screen.signup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.naedafront.ui.common.SignUpProgressBar
import kotlinx.coroutines.delay

private val DarkBg = Color(0xFF0D1A1A)
private val PinFilled = Color(0xFF009688)
private val PinEmpty = Color.White.copy(alpha = 0.25f)
private val PinError = Color(0xFFF2522E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpPinScreen(
    onBackClick: () -> Unit = {},
    onConfirmClick: () -> Unit = {},
    signUpViewModel: SignUpViewModel = viewModel()
) {
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    val currentPin = if (isConfirming) confirmPin else firstPin
    val currentStep = if (isConfirming) 8 else 7

    LaunchedEffect(hasError) {
        if (hasError) {
            delay(500L)
            confirmPin = ""
            hasError = false
        }
    }

    fun onNumberInput(digit: String) {
        if (hasError) return

        if (isConfirming) {
            if (confirmPin.length >= 6) return

            val newPin = confirmPin + digit
            confirmPin = newPin

            if (newPin.length == 6) {
                if (newPin == firstPin) {
                    signUpViewModel.updatePin(firstPin)
                    onConfirmClick()
                } else {
                    hasError = true
                }
            }
        } else {
            if (firstPin.length >= 6) return

            val newPin = firstPin + digit
            firstPin = newPin

            if (newPin.length == 6) {
                isConfirming = true
            }
        }
    }

    fun onDelete() {
        if (hasError) return

        if (isConfirming) {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            }
        } else {
            if (firstPin.isNotEmpty()) {
                firstPin = firstPin.dropLast(1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isConfirming) {
                                confirmPin = ""
                                hasError = false
                                isConfirming = false
                            } else {
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg
                )
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SignUpProgressBar(
                currentStep = currentStep,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isConfirming) {
                        "PIN 번호를 한 번 더\n입력해주세요"
                    } else {
                        "PIN 6자리를\n입력해주세요"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isConfirming) {
                        "확인을 위해 PIN 번호를 다시 입력해주세요"
                    } else {
                        "2차 인증 비밀번호로 사용할 예정입니다"
                    },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        hasError -> PinError
                                        index < currentPin.length -> PinFilled
                                        else -> PinEmpty
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                AnimatedVisibility(visible = hasError) {
                    Text(
                        text = "PIN 번호가 일치하지 않습니다. 다시 입력해주세요.",
                        fontSize = 13.sp,
                        color = PinError,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                NumberKeypad(
                    onNumberClick = { onNumberInput(it) },
                    onDeleteClick = { onDelete() },
                    textColor = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpPinScreenPreview() {
    MaterialTheme {
        SignUpPinScreen()
    }
}