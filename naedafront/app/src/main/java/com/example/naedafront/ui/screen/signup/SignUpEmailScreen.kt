package com.example.naedafront.ui.screen.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.naedafront.ui.common.SignUpProgressBar
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Mint900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpEmailScreen(
    signUpViewModel: SignUpViewModel,
    onBackClick: () -> Unit = {},
    onConfirmClick: () -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var showInvalidEmailDialog by remember { mutableStateOf(false) }
    var showDuplicateEmailDialog by remember { mutableStateOf(false) }
    var duplicateEmailMessage by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val trimmedEmail = email.trim()

    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    val isEmailNotBlank = trimmedEmail.isNotBlank()
    val isEmailValid = emailRegex.matches(trimmedEmail)

    fun handleConfirm() {
        keyboardController?.hide()

        if (!isEmailNotBlank || isChecking) return

        if (!isEmailValid) {
            showInvalidEmailDialog = true
            return
        }

        isChecking = true
        signUpViewModel.checkEmailDuplicate(trimmedEmail) { isDuplicate, message ->
            isChecking = false
            if (isDuplicate) {
                duplicateEmailMessage = message ?: "이미 사용 중인 이메일입니다."
                showDuplicateEmailDialog = true
            } else {
                signUpViewModel.updateUserId(trimmedEmail)
                onConfirmClick()
            }
        }
    }

    if (showInvalidEmailDialog) {
        AlertDialog(
            onDismissRequest = { showInvalidEmailDialog = false },
            confirmButton = {
                TextButton(onClick = { showInvalidEmailDialog = false }) {
                    Text("확인")
                }
            },
            title = {
                Text("이메일 형식 오류")
            },
            text = {
                Text("올바른 이메일 형식으로 입력해주세요.")
            }
        )
    }

    if (showDuplicateEmailDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateEmailDialog = false },
            confirmButton = {
                TextButton(onClick = { showDuplicateEmailDialog = false }) {
                    Text("확인")
                }
            },
            title = {
                Text("이메일 중복")
            },
            text = {
                Text(duplicateEmailMessage)
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0)
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
                currentStep = 4,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "이메일을\n입력해주세요",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "가입하실 이메일 주소를 입력해 주세요",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("이메일") },
                    placeholder = {
                        Text(
                            text = "example@email.com",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleConfirm() }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { handleConfirm() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isEmailNotBlank && !isChecking,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    )
                ) {
                    Text(
                        text = "확인",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpEmailScreenPreview() {
    MaterialTheme {
        SignUpEmailScreen(
            signUpViewModel = SignUpViewModel()
        )
    }
}
