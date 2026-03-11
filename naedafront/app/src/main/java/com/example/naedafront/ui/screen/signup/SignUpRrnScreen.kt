package com.example.naedafront.ui.screen.signup

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.ui.common.SignUpProgressBar
import com.example.naedafront.ui.theme.Mint900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpRrnScreen(
    onBackClick: () -> Unit = {},
    onConfirmClick: (String) -> Unit = {}
) {
    // 앞자리 6자리 + 뒷자리 1자리만 입력
    var frontNumber by remember { mutableStateOf("") }
    var backNumber by remember { mutableStateOf("") }

    val isFrontComplete = frontNumber.length == 6
    val isValid = frontNumber.length == 6 && backNumber.length == 1

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

            // 타이틀
            Text(
                text = "주민등록번호를\n입력해주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 주민등록번호 라벨
            Text(
                text = "주민등록번호",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 주민등록번호 입력 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 앞자리 6자리
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = frontNumber.padEnd(6, ' ').let {
                            it.mapIndexed { index, c ->
                                if (index < frontNumber.length) c.toString()
                                else ""
                            }.joinToString(" ")
                        }.ifBlank { "" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 4.sp
                    )

                    // 밑줄
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (!isFrontComplete) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }

                // 구분자
                Text(
                    text = " - ",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // 뒷자리 (첫 자리 + ●●●●●●)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 뒷자리 첫번째 숫자 (보임)
                        Text(
                            text = if (backNumber.isNotEmpty()) backNumber else "",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 나머지 6자리 고정 마스킹 (입력 안 받음)
                        repeat(6) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }

                    // 밑줄
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (isFrontComplete) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 확인 버튼
            Button(
                onClick = { onConfirmClick("$frontNumber$backNumber") },
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

            // 커스텀 숫자 키패드
            NumberKeypad(
                onNumberClick = { digit ->
                    if (!isFrontComplete) {
                        if (frontNumber.length < 6) {
                            frontNumber += digit
                        }
                    } else {
                        if (backNumber.isEmpty()) {
                            backNumber = digit
                        }
                    }
                },
                onDeleteClick = {
                    if (isFrontComplete && backNumber.isNotEmpty()) {
                        backNumber = backNumber.dropLast(1)
                    } else if (frontNumber.isNotEmpty()) {
                        frontNumber = frontNumber.dropLast(1)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SignUpRrnScreenPreview() {
    MaterialTheme {
        SignUpRrnScreen()
    }
}
