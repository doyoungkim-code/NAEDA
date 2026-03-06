package com.example.naedafront.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.ui.common.SignUpProgressBar
<<<<<<< HEAD
=======
import com.example.naedafront.ui.theme.Mint900
>>>>>>> origin/S14P21D103-22-fe-002-회원가입-본인인증-화면

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpPhoneScreen(
    onBackClick: () -> Unit = {},
    onConfirmClick: (String) -> Unit = {}
) {
    var phoneDigits by remember { mutableStateOf("") }

    // 010-1234-5678 포맷
    val formattedPhone = formatPhone(phoneDigits)
    val isValid = phoneDigits.length == 11

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
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            SignUpProgressBar(
                currentStep = 2,  // 각 화면마다 번호 다르게
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

            // 라벨
            Text(
                text = "휴대폰 번호",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 번호 표시
            Text(
                text = formattedPhone.ifEmpty { "010-0000-0000" },
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = if (phoneDigits.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 밑줄
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 안내 문구
            Text(
                text = "본인 명의의 휴대폰 번호를 입력해 주세요.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.weight(1f))

            // 확인 버튼
            Button(
                onClick = { onConfirmClick(formattedPhone) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isValid,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
<<<<<<< HEAD
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer
=======
                    containerColor = Mint900,
                    disabledContainerColor = Mint900.copy(alpha = 0.38f)
>>>>>>> origin/S14P21D103-22-fe-002-회원가입-본인인증-화면
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

            // 숫자 키패드
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
    onDeleteClick: () -> Unit
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
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key.isNotEmpty()) {
                            Text(
                                text = key,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
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
        SignUpPhoneScreen()
    }
}