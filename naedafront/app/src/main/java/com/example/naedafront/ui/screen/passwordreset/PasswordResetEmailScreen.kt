package com.example.naedafront.ui.screen.passwordreset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.data.remote.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun PasswordResetEmailScreen(
    onBackClick: () -> Unit = {},
    onCodeReceived: (phone: String, code: String) -> Unit = { _, _ -> }
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        phoneError = when {
            phone.isBlank() -> "휴대폰번호를 입력해주세요"
            !phone.trim().matches(Regex("^01[016789]\\d{7,8}$")) ->
                "올바른 휴대폰번호 형식이 아니에요"
            else -> null
        }
        return phoneError == null
    }

    fun submit() {
        focusManager.clearFocus()
        if (!validate() || isLoading) return

        isLoading = true
        coroutineScope.launch {
            AuthRepository.requestPasswordReset(phone.trim())
                .onSuccess { response ->
                    isLoading = false
                    onCodeReceived(phone.trim(), response.code)
                }
                .onFailure { error ->
                    phoneError = error.message ?: "요청에 실패했습니다."
                    isLoading = false
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

            Text(
                text = "비밀번호 찾기",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "가입한 휴대폰번호를 입력하면\n인증코드를 보내드립니다.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextField(
                value = phone,
                onValueChange = {
                    phone = it
                    phoneError = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                label = {
                    Text(
                        text = "휴대폰번호",
                        color = if (phoneError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                },
                placeholder = {
                    Text(
                        text = "01012345678",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                isError = phoneError != null,
                supportingText = if (phoneError != null) {
                    { Text(phoneError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                } else null,
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { submit() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = Color.White
                ),
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
                        text = "인증코드 받기",
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
