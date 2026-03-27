package com.example.naedafront.ui.screen.signup

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.AuthPrefs
import com.example.naedafront.ui.common.SignUpProgressBar
import com.example.naedafront.data.remote.AuthRepository
import com.example.naedafront.data.remote.LoginResult
import com.example.naedafront.data.repository.PointRepository
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.NaedaFontFamily
import kotlinx.coroutines.delay

private val PinError = Color(0xFFF2522E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpPinScreen(
    signUpViewModel: SignUpViewModel,
    onBackClick: () -> Unit = {},
    onConfirmClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val uiState by signUpViewModel.uiState.collectAsState()

    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var hasPinMismatchError by remember { mutableStateOf(false) }

    val currentPin = if (isConfirming) confirmPin else firstPin
    val currentStep = if (isConfirming) 7 else 6

    suspend fun createPointWalletAfterSignUp() {
        when (val loginResult = AuthRepository.login(uiState.userId, uiState.password)) {
            is LoginResult.Success -> {
                val response = loginResult.response

                AuthPrefs.saveLoginSession(
                    context = context,
                    userNo = response.userNo,
                    userId = response.userId,
                    username = response.username,
                    userKey = response.userKey,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                    faceRegistered = response.faceRegistered,
                    secondaryAuthEnabled = response.secondaryAuthEnabled
                )

                PointRepository().createPointWallet(response.userNo)
                    .onFailure { throwable ->
                        val message = throwable.message.orEmpty()
                        if (!message.contains("409")) {
                            Log.w("SignUpPinScreen", "createPointWalletAfterSignUp failed: $message")
                        }
                    }

                AuthPrefs.clearSession(context)
            }

            is LoginResult.Failure -> {
                Log.w("SignUpPinScreen", "auto login after sign up failed: ${loginResult.message}")
            }
        }
    }

    LaunchedEffect(hasPinMismatchError) {
        if (hasPinMismatchError) {
            delay(500L)
            confirmPin = ""
            hasPinMismatchError = false
        }
    }

    LaunchedEffect(uiState.isSignUpSuccess) {
        if (uiState.isSignUpSuccess) {
            createPointWalletAfterSignUp()
            signUpViewModel.resetSignUpSuccess()
            onConfirmClick()
        }
    }

    fun onNumberInput(digit: String) {
        if (hasPinMismatchError || uiState.isLoading) return

        if (isConfirming) {
            if (confirmPin.length >= 6) return

            val newPin = confirmPin + digit
            confirmPin = newPin

            if (newPin.length == 6) {
                if (newPin == firstPin) {
                    signUpViewModel.clearError()
                    signUpViewModel.updatePin(firstPin)
                    signUpViewModel.submitSignUp()
                } else {
                    hasPinMismatchError = true
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
        if (hasPinMismatchError || uiState.isLoading) return

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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isConfirming) "PIN 확인" else "PIN 설정",
                        fontFamily = NaedaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.isLoading) return@IconButton

                            if (isConfirming) {
                                confirmPin = ""
                                hasPinMismatchError = false
                                signUpViewModel.clearError()
                                isConfirming = false
                            } else {
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                windowInsets = WindowInsets(0),
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
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
                // ── 상단 콘텐츠 (스크롤 가능) ──
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(56.dp))

                    // ── 상단 칩 ──
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isConfirming) "PIN 확인" else "PIN 설정",
                            fontFamily = NaedaFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isConfirming) {
                            "PIN 번호를 한 번 더\n입력해주세요"
                        } else {
                            "PIN 6자리를\n입력해주세요"
                        },
                        fontFamily = NaedaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isConfirming) {
                            "확인을 위해\nPIN 번호를 다시 입력해주세요"
                        } else {
                            "2차 인증 비밀번호로\n사용할 예정입니다"
                        },
                        fontFamily = NaedaFontFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    AnimatedVisibility(visible = hasPinMismatchError) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "PIN 번호가 일치하지 않습니다. 다시 입력해주세요.",
                                fontFamily = NaedaFontFamily,
                                fontSize = 13.sp,
                                color = PinError,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (!uiState.errorMessage.isNullOrBlank()) {
                        AlertDialog(
                            onDismissRequest = {
                                signUpViewModel.clearError()
                                confirmPin = ""
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    signUpViewModel.clearError()
                                    confirmPin = ""
                                }) {
                                    Text("확인")
                                }
                            },
                            title = {
                                Text("회원가입 실패")
                            },
                            text = {
                                Text(uiState.errorMessage.orEmpty())
                            }
                        )
                    }

                    AnimatedVisibility(visible = uiState.isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "회원가입 처리 중입니다...",
                                fontFamily = NaedaFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // ── PIN 도트 ──
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(6) { index ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        color = when {
                                            hasPinMismatchError -> PinError
                                            index < currentPin.length -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.outline
                                        },
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                // ── 키패드 (하단 고정) ──
                NumberKeypad(
                    onNumberClick = { onNumberInput(it) },
                    onDeleteClick = { onDelete() },
                    textColor = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpPinScreenPreview() {
    MaterialTheme {
        SignUpPinScreen(
            signUpViewModel = SignUpViewModel()
        )
    }
}
