package com.example.naedafront.ui.screen.facepay

import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.data.remote.ApiRequestException
import com.example.naedafront.data.remote.FaceRegistrationRepository
import com.example.naedafront.data.remote.ResidentIdExtractResponseDto
import com.example.naedafront.ui.screen.signup.NumberKeypad
import com.example.naedafront.ui.theme.Error
import com.example.naedafront.ui.theme.Mint50
import com.example.naedafront.ui.theme.Mint100
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaFontFamily
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import com.example.naedafront.ui.theme.Outline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt


@Composable
internal fun IdConfirmStageContent(
    extracted: ResidentIdExtractResponseDto,
    onConfirmComplete: () -> Unit,
    onConfirmError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember(extracted) { mutableStateOf(extracted.name.orEmpty()) }
    var residentFront6 by remember(extracted) { mutableStateOf(extracted.residentFront6.orEmpty()) }
    var residentBackFirst1 by remember(extracted) { mutableStateOf(extracted.residentBackFirst1.orEmpty()) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "인식된 정보를 확인해 주세요",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "이름과 주민등록번호 앞 6자리, 뒤 첫 1자리만 사용합니다.",
            fontFamily = NaedaFontFamily,
            fontSize = 14.sp,
            color = OnSurfaceVariant,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        LabeledField(label = "이름", value = name, onValueChange = { name = it })
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledField(
                label = "주민번호 앞 6자리",
                value = residentFront6,
                onValueChange = { residentFront6 = it.filter(Char::isDigit).take(6) },
                modifier = Modifier.weight(1f)
            )
            LabeledField(
                label = "뒤 첫 1자리",
                value = residentBackFirst1,
                onValueChange = { residentBackFirst1 = it.filter(Char::isDigit).take(1) },
                modifier = Modifier.weight(0.5f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Mint50)
        ) {
            Text(
                text = "표시 값: ${residentFront6.ifBlank { "------" }}-${residentBackFirst1.ifBlank { "-" }}",
                modifier = Modifier.padding(16.dp),
                fontFamily = NaedaFontFamily,
                fontSize = 14.sp,
                color = OnBackground
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (name.isBlank() || residentFront6.length != 6 || residentBackFirst1.length != 1 || isLoading) {
                    onConfirmError("OCR 확인값을 다시 확인해 주세요.")
                    return@Button
                }
                isLoading = true
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        FaceRegistrationRepository.confirmResidentId(
                            name = name,
                            residentFront6 = residentFront6,
                            residentBackFirst1 = residentBackFirst1
                        )
                    }.onSuccess { response ->
                        isLoading = false
                        if (response.verified) {
                            onConfirmComplete()
                        } else {
                            onConfirmError(buildIdConfirmError(response))
                        }
                    }.onFailure { throwable ->
                        isLoading = false
                        onConfirmError(throwable.message ?: "신분증 확인에 실패했습니다.")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint900)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("확인 및 다음", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
internal fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (label.contains("주민번호")) KeyboardType.Number else KeyboardType.Text
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Mint500,
                unfocusedBorderColor = Outline
            )
        )
    }
}

@Composable
internal fun PinChoiceStageContent(
    isSaving: Boolean,
    onUsePin: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "PIN 2차 인증을 사용할까요?",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = OnBackground,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        listOf(
            "현재 계정 PIN을 한 번 더 확인해 결제를 보호할 수 있습니다.",
            "원하지 않으면 이번에는 건너뛰고 얼굴 등록만 완료할 수 있습니다."
        ).forEach { tip ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = tip,
                    modifier = Modifier.padding(16.dp),
                    fontFamily = NaedaFontFamily,
                    fontSize = 14.sp,
                    color = OnBackground,
                    lineHeight = 22.sp
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onUsePin,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint900)
        ) {
            Text(if (isSaving) "설정 저장 중..." else "현재 PIN으로 사용하기", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onSkip,
            enabled = !isSaving,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("이번에는 건너뛰기", fontFamily = NaedaFontFamily, fontSize = 15.sp, color = OnSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
internal fun PinCreateStageContent(
    title: String,
    description: String,
    onPinCreated: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1717))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = title,
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontFamily = NaedaFontFamily,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            PinDotsReal(pinLength = pin.length)
            Spacer(modifier = Modifier.weight(1f))
            NumberKeypad(
                onNumberClick = {
                    if (pin.length < 6) {
                        pin += it
                        if (pin.length == 6) {
                            onPinCreated(pin)
                        }
                    }
                },
                onDeleteClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
                textColor = Color.White
            )
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
internal fun PinConfirmStageContent(
    newPin: String,
    onPinConfirmed: (String) -> Unit
) {
    var confirmedPin by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1717))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "PIN 번호를 한 번 더 입력해 주세요",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "확인을 위해 같은 PIN 번호를 다시 입력합니다.",
                fontFamily = NaedaFontFamily,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            PinDotsReal(pinLength = confirmedPin.length)
            Spacer(modifier = Modifier.weight(1f))
            NumberKeypad(
                onNumberClick = {
                    if (confirmedPin.length < 6) {
                        confirmedPin += it
                        if (confirmedPin.length == 6) {
                            onPinConfirmed(confirmedPin)
                        }
                    }
                },
                onDeleteClick = { if (confirmedPin.isNotEmpty()) confirmedPin = confirmedPin.dropLast(1) },
                textColor = Color.White
            )
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
internal fun CurrentPinStageContent(
    resetKey: Int,
    isSaving: Boolean,
    onCurrentPinEntered: (String) -> Unit
) {
    var currentPin by remember(resetKey) { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1717))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "현재 PIN 번호를 입력해 주세요",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "페이스페이에서 PIN 2차 인증을 사용하려면 현재 계정 PIN 확인이 필요합니다.",
                fontFamily = NaedaFontFamily,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            PinDotsReal(pinLength = currentPin.length)
            Spacer(modifier = Modifier.weight(1f))
            NumberKeypad(
                onNumberClick = {
                    if (!isSaving && currentPin.length < 6) {
                        currentPin += it
                        if (currentPin.length == 6) {
                            onCurrentPinEntered(currentPin)
                        }
                    }
                },
                onDeleteClick = { if (currentPin.isNotEmpty()) currentPin = currentPin.dropLast(1) },
                textColor = Color.White
            )
            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
internal fun SuccessStageContent(
    secondaryAuthEnabled: Boolean,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(Mint500, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("😊", fontSize = 70.sp)
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "페이스페이 등록이 완료되었습니다",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = OnBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (secondaryAuthEnabled) "얼굴 등록과 신분증 확인이 완료되었고 PIN 2차 인증 사용도 저장되었습니다." else "얼굴 등록과 신분증 확인이 완료되었습니다. PIN 2차 인증은 사용 안 함으로 저장되었습니다.",
            fontFamily = NaedaFontFamily,
            fontSize = 15.sp,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint900)
        ) {
            Text("홈으로 이동", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

