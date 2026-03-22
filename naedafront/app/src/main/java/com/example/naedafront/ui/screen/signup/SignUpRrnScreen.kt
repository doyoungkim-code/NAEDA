package com.example.naedafront.ui.screen.signup

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.naedafront.ui.common.SignUpProgressBar
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpRrnScreen(
    signUpViewModel: SignUpViewModel,
    onBackClick: () -> Unit = {},
    onConfirmClick: () -> Unit = {},

) {
    var frontNumber by remember { mutableStateOf("") }
    var backNumber by remember { mutableStateOf("") }
    var showInvalidRrnDialog by remember { mutableStateOf(false) }

    val isFrontComplete = frontNumber.length == 6
    val isValid = frontNumber.length == 6 && backNumber.length == 1

    fun isValidResidentFrontNumber(value: String): Boolean {
        if (value.length != 6) return false

        val yy = value.substring(0, 2).toIntOrNull() ?: return false
        val mm = value.substring(2, 4).toIntOrNull() ?: return false
        val dd = value.substring(4, 6).toIntOrNull() ?: return false

        val isYearValid = yy in 0..99
        val isMonthValid = mm in 1..12
        val isDayValid = dd in 1..31

        return isYearValid && isMonthValid && isDayValid
    }

    fun handleConfirm() {
        if (!isValid) return

        val residentNo = "$frontNumber-$backNumber"

        if (!isValidResidentFrontNumber(frontNumber)) {
            showInvalidRrnDialog = true
            return
        }

        signUpViewModel.updateResidentNo(residentNo)
        onConfirmClick()
    }

    if (showInvalidRrnDialog) {
        AlertDialog(
            onDismissRequest = { showInvalidRrnDialog = false },
            confirmButton = {
                TextButton(onClick = { showInvalidRrnDialog = false }) {
                    Text("확인")
                }
            },
            title = {
                Text("주민등록번호 형식 오류")
            },
            text = {
                Text("생년월일 6자리를 올바르게 입력해주세요.")
            }
        )
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
                currentStep = 2,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "주민등록번호를\n입력해주세요",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "주민등록번호",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = frontNumber.padEnd(6, ' ').let { padded ->
                            padded.mapIndexed { index, c ->
                                if (index < frontNumber.length) c.toString() else ""
                            }.joinToString(" ")
                        }.ifBlank { "" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 4.sp
                    )

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

                Text(
                    text = " - ",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (backNumber.isNotEmpty()) backNumber else "",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        repeat(6) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }

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
        SignUpRrnScreen(
            signUpViewModel = SignUpViewModel()
        )
    }
}
