package com.example.naedafront.ui.screen.mypage

import android.widget.Toast
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import com.example.naedafront.ui.common.NaedaTopBar
import com.example.naedafront.ui.screen.signup.NumberKeypad
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.NaedaFontFamily

@Composable
fun SecondaryAuthPinScreen(
    viewModel: MyPageViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isSaving = uiState.isUpdatingSecondaryAuth

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NaedaTopBar(
                title = "현재 PIN 입력",
                showBackButton = true,
                onBackClick = onBackClick
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(44.dp))

                Text(
                    text = "2차 인증을 사용하려면\n현재 PIN 번호를 입력해주세요",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NaedaFontFamily,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "등록된 6자리 PIN이 확인되면\n2차 인증이 활성화됩니다",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontFamily = NaedaFontFamily,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        val isFilled = index < pin.length
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        errorMessage != null -> Color(0xFFE53935)
                                        isFilled -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        isSaving -> "PIN 번호를 확인하는 중입니다..."
                        errorMessage != null -> errorMessage.orEmpty()
                        else -> "현재 사용 중인 PIN 번호를 입력하세요"
                    },
                    color = if (errorMessage != null) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontFamily = NaedaFontFamily,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))

                NumberKeypad(
                    onNumberClick = { digit ->
                        if (isSaving || pin.length >= 6) {
                            return@NumberKeypad
                        }

                        errorMessage = null
                        pin += digit
                        if (pin.length != 6) {
                            return@NumberKeypad
                        }

                        viewModel.updateSecondaryAuth(
                            context = context,
                            enable = true,
                            currentPin = pin,
                            onSuccess = {
                                Toast.makeText(context, "2차 인증이 활성화되었습니다.", Toast.LENGTH_SHORT).show()
                                onBackClick()
                            },
                            onFailure = { message ->
                                errorMessage = if (message.contains("PIN")) {
                                    "현재 PIN 번호가 올바르지 않습니다.\n다시 입력해 주세요."
                                } else {
                                    message
                                }
                                pin = ""
                            }
                        )
                    },
                    onDeleteClick = {
                        if (!isSaving && pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                            errorMessage = null
                        }
                    },
                    textColor = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
