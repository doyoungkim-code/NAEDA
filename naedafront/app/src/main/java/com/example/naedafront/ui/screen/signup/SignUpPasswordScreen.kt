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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.ui.common.SignUpProgressBar
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Mint900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpPasswordScreen(
    signUpViewModel: SignUpViewModel,
    onBackClick: () -> Unit = {},
    onConfirmClick: () -> Unit = {},
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showInvalidPasswordDialog by remember { mutableStateOf(false) }
    var showPasswordMismatchDialog by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val trimmedPassword = password.trim()
    val trimmedConfirmPassword = confirmPassword.trim()

    val hasLetter = trimmedPassword.any { it.isLetter() }
    val hasDigit = trimmedPassword.any { it.isDigit() }
    val isLengthValid = trimmedPassword.length >= 8
    val isPasswordNotBlank = trimmedPassword.isNotBlank()
    val isConfirmPasswordNotBlank = trimmedConfirmPassword.isNotBlank()
    val isPasswordValid = isLengthValid && hasLetter && hasDigit
    val isPasswordMatched = trimmedPassword == trimmedConfirmPassword

    fun handleConfirm() {
        keyboardController?.hide()

        if (!isPasswordNotBlank || !isConfirmPasswordNotBlank) return

        if (!isPasswordValid) {
            showInvalidPasswordDialog = true
            return
        }

        if (!isPasswordMatched) {
            showPasswordMismatchDialog = true
            return
        }

        signUpViewModel.updatePassword(trimmedPassword)
        onConfirmClick()
    }

    if (showInvalidPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showInvalidPasswordDialog = false },
            confirmButton = {
                TextButton(onClick = { showInvalidPasswordDialog = false }) {
                    Text("확인")
                }
            },
            title = {
                Text("비밀번호 형식 오류")
            },
            text = {
                Text("비밀번호는 8자 이상이며 영문과 숫자를 모두 포함해야 합니다.")
            }
        )
    }

    if (showPasswordMismatchDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordMismatchDialog = false },
            confirmButton = {
                TextButton(onClick = { showPasswordMismatchDialog = false }) {
                    Text("확인")
                }
            },
            title = {
                Text("비밀번호 불일치")
            },
            text = {
                Text("비밀번호와 비밀번호 확인이 일치하지 않습니다.")
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
                    containerColor = Background
                ),
                windowInsets = WindowInsets(0)
            )
        },
        containerColor = Background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SignUpProgressBar(
                currentStep = 5,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "비밀번호를\n입력해주세요",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "영문과 숫자를 조합해 8자 이상 입력해 주세요",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("비밀번호") },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    supportingText = {
                        Text(
                            text = "영문 + 숫자 조합 / 대소문자 구분 없음",
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Mint500,
                        focusedLabelColor = Mint500,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = Mint500
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("비밀번호 확인") },
                    visualTransformation = if (confirmPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    supportingText = {
                        if (isConfirmPasswordNotBlank && isPasswordMatched) {
                            Text(
                                text = "비밀번호가 일치합니다.",
                                color = Mint900
                            )
                        } else if (isConfirmPasswordNotBlank && !isPasswordMatched) {
                            Text(
                                text = "비밀번호가 일치하지 않습니다.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { handleConfirm() }
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Mint500,
                        focusedLabelColor = Mint500,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = Mint500
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { handleConfirm() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isPasswordNotBlank && isConfirmPasswordNotBlank,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Mint900,
                        disabledContainerColor = Mint900.copy(alpha = 0.38f)
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
private fun SignUpPasswordScreenPreview() {
    MaterialTheme {
        SignUpPasswordScreen(
            signUpViewModel = SignUpViewModel()
        )
    }
}
