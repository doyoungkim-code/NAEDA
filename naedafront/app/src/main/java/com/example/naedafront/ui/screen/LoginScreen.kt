package com.example.naedafront.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.AuthRepository
import com.example.naedafront.data.remote.LoginResult
import com.example.naedafront.data.remote.RetrofitClient
import com.example.naedafront.ui.theme.KronaOneFontFamily
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Mint900
import kotlinx.coroutines.launch

private val FieldBackground = Color(0xFFF5F5F5)
private val HintColor = Color(0xFFBDBDBD)

@Composable
fun LoginScreen(
    onBackClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        var valid = true

        emailError = when {
            email.isBlank() -> { valid = false; "이메일을 입력해주세요" }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                valid = false; "올바른 이메일 형식이 아니에요"
            }
            else -> null
        }

        passwordError = when {
            password.isBlank() -> { valid = false; "비밀번호를 입력해주세요" }
            else -> null
        }

        return valid
    }

    fun submit() {
        focusManager.clearFocus()
        if (!validate() || isLoading) return

        loginError = null
        isLoading = true

        coroutineScope.launch {
            when (val result = AuthRepository.login(email.trim(), password)) {
                is LoginResult.Success -> {
                    AuthPrefs.saveLoginSession(
                        context = context,
                        userNo = result.response.userNo,
                        userId = result.response.userId,
                        username = result.response.username,
                        userKey = result.response.userKey,
                        accessToken = result.response.accessToken,
                        refreshToken = result.response.refreshToken,
                        faceRegistered = result.response.faceRegistered,
                        secondaryAuthEnabled = result.response.secondaryAuthEnabled
                    )
                    RetrofitClient.setAccessToken(result.response.accessToken)

                    com.example.naedafront.fcm.NaedaFirebaseMessagingService
                        .registerCurrentToken(context)

                    isLoading = false
                    onLoginSuccess()
                }
                is LoginResult.Failure -> {
                    loginError = result.message
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NAEDA",
                    fontFamily = KronaOneFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    color = Mint900,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "이미 계정이 있으신가요?",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "이메일과 비밀번호로 로그인하세요",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 이메일 필드
            TextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                    loginError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                label = {
                    Text(
                        text = "이메일",
                        color = if (emailError != null) MaterialTheme.colorScheme.error else HintColor,
                        fontSize = 13.sp
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError = emailError != null,
                supportingText = if (emailError != null) {
                    { Text(emailError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else null,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FieldBackground,
                    unfocusedContainerColor = FieldBackground,
                    errorContainerColor = FieldBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedLabelColor = Mint500,
                    cursorColor = Mint500,
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 비밀번호 필드
            TextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                    loginError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                label = {
                    Text(
                        text = "비밀번호",
                        color = if (passwordError != null) MaterialTheme.colorScheme.error else HintColor,
                        fontSize = 13.sp
                    )
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    Box(modifier = Modifier.padding(end = 4.dp)) {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = null,
                                tint = HintColor
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submit() }
                ),
                isError = passwordError != null,
                supportingText = if (passwordError != null) {
                    { Text(passwordError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else null,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = FieldBackground,
                    unfocusedContainerColor = FieldBackground,
                    errorContainerColor = FieldBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedLabelColor = Mint500,
                    cursorColor = Mint500,
                )
            )

            if (loginError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = loginError!!,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { submit() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint900),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "로그인",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen()
    }
}