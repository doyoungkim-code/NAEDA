package com.example.naedafront.ui.screen.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.ui.common.SignUpProgressBar
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpPhoneScreen(
    signUpViewModel: SignUpViewModel,
    onBackClick: () -> Unit = {},
    onConfirmClick: () -> Unit = {}
) {
    var phoneDigits by remember { mutableStateOf("") }

    val formattedPhone = formatPhone(phoneDigits)
    val isValid = phoneDigits.length == 11

    fun handleConfirm() {
        if (!isValid) return

        signUpViewModel.updatePhone(phoneDigits)
        onConfirmClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
            )
        },
        containerColor = Background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            SignUpProgressBar(
                currentStep = 3,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "휴대폰 번호를\n입력해주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "휴대폰 번호",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formattedPhone.ifEmpty { "010-0000-0000" },
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = if (phoneDigits.isNotEmpty()) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "본인 명의의 휴대폰 번호를 입력해 주세요.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { handleConfirm() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isValid,
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

            Spacer(modifier = Modifier.height(16.dp))

            NumberKeypad(
                onNumberClick = { digit ->
                    if (phoneDigits.length < 11) {
                        phoneDigits += digit
                    }
                },
                onDeleteClick = {
                    if (phoneDigits.isNotEmpty()) {
                        phoneDigits = phoneDigits.dropLast(1)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 숫자를 010-1234-5678 형태로 포맷
 */
private fun formatPhone(digits: String): String {
    return when {
        digits.length <= 3 -> digits
        digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
        else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
    }
}

/**
 * 공용 숫자 키패드
 */
@Composable
fun NumberKeypad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )

    Column {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .then(
                                if (key.isNotEmpty()) {
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (key == "⌫") onDeleteClick()
                                            else onNumberClick(key)
                                        }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key.isNotEmpty()) {
                            Text(
                                text = key,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpPhoneScreenPreview() {
    MaterialTheme {
        SignUpPhoneScreen(
            signUpViewModel = SignUpViewModel()
        )
    }
}