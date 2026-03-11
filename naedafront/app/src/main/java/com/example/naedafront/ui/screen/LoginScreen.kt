package com.example.naedafront.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.ui.theme.*

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun validate(): Boolean {
        var valid = true
        emailError = when {
            email.isBlank() -> { valid = false; "이메일을 입력해주세요" }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { valid = false; "올바른 이메일 형식이 아니에요" }
            else -> null
        }
        passwordError = when {
            password.isBlank() -> { valid = false; "비밀번호를 입력해주세요" }
            else -> null
        }
        return valid
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

            // ── 뒤로가기 ──
            IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                Text(text = "←", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── NAEDA 로고 ──
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "NAEDA",
                    fontFamily = KronaOneFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 28.sp,
                    color = Mint500,
                    letterSpacing = 3.sp
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // ── 타이틀 ──
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

            Spacer(modifier = Modifier.height(36.dp))

            // ── 이메일 입력 ──
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null; loginError = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("이메일") },
                placeholder = {
                    Text("이메일 주소를 입력해주세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    { Text(emailError!!, color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Mint500,
                    focusedLabelColor = Mint500,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = Mint500
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 비밀번호 입력 ──
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = null; loginError = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("비밀번호") },
                placeholder = {
                    Text("비밀번호를 입력해주세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (validate()) onLoginClick(email, password)
                    }
                ),
                isError = passwordError != null,
                supportingText = if (passwordError != null) {
                    { Text(passwordError!!, color = MaterialTheme.colorScheme.error) }
                } else null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Mint500,
                    focusedLabelColor = Mint500,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = Mint500
                )
            )

            // ── 전체 로그인 에러 (서버 응답: 이메일/비밀번호 불일치 등) ──
            if (loginError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = loginError!!,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── 로그인 버튼 ──
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (validate()) onLoginClick(email, password)
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
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