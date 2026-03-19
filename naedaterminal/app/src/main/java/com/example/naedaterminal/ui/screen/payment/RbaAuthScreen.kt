package com.example.naedaterminal.ui.screen.payment

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedaterminal.ui.theme.*

private val BgColor = Background
private val TextPrimary = Color(0xFF0D3B35)
private val TextSecondary = Color(0xFF0D3B35).copy(alpha = 0.5f)
private val AccentColor = Color(0xFF00635A)
private val KeypadBg = Color(0xFF00635A).copy(alpha = 0.08f)
private val KeypadDeleteBg = Color(0xFF00635A).copy(alpha = 0.05f)
private val ErrorColor = Color(0xFFD32F2F)

sealed class RbaAuthType {
    object Pin : RbaAuthType()
    object PhoneMiddleFour : RbaAuthType()
}

@Composable
fun RbaAuthContainer(
    authSteps: List<RbaAuthType>,
    paymentAmount: Long,
    merchantName: String,
    onAuthComplete: () -> Unit,
    onAuthCancel: () -> Unit
) {
    var currentStepIndex by remember { mutableStateOf(0) }

    if (currentStepIndex >= authSteps.size) {
        LaunchedEffect(Unit) { onAuthComplete() }
        return
    }

    val currentStep = authSteps[currentStepIndex]

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
        },
        label = "rba_step_transition"
    ) { step ->
        when (step) {
            is RbaAuthType.Pin -> RbaPinScreen(
                paymentAmount = paymentAmount,
                merchantName = merchantName,
                stepInfo = "${currentStepIndex + 1}/${authSteps.size}",
                onSuccess = { currentStepIndex++ },
                onCancel = onAuthCancel
            )
            is RbaAuthType.PhoneMiddleFour -> RbaPhoneScreen(
                paymentAmount = paymentAmount,
                merchantName = merchantName,
                stepInfo = "${currentStepIndex + 1}/${authSteps.size}",
                title = "전화번호 가운데\n4자리를 입력해주세요",
                subtitle = "본인 확인을 위해 휴대폰 번호 가운데 4자리를 입력하세요",
                onSuccess = { currentStepIndex++ },
                onCancel = onAuthCancel
            )
        }
    }
}

@Composable
private fun RbaAuthScaffold(
    title: String,
    subtitle: String,
    stepInfo: String,
    paymentAmount: Long,
    merchantName: String,
    onCancel: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val amountText = "%,d원".format(paymentAmount)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "취소", tint = AccentColor)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = stepInfo, color = TextSecondary, fontSize = 13.sp, fontFamily = NaedaFontFamily)
                Spacer(modifier = Modifier.width(16.dp))
            }

            PaymentInfoCard(
                merchantName = merchantName,
                amountText = amountText,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NaedaFontFamily,
                    lineHeight = 30.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = subtitle, color = TextSecondary, fontSize = 14.sp, fontFamily = NaedaFontFamily)
            }

            Spacer(modifier = Modifier.height(32.dp))

            content()
        }
    }
}

@Composable
private fun PaymentInfoCard(
    merchantName: String,
    amountText: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(AccentColor.copy(alpha = 0.15f), AccentColor.copy(alpha = 0.08f))
                )
            )
            .border(1.dp, AccentColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "추가 인증 필요", color = AccentColor, fontSize = 11.sp, fontFamily = NaedaFontFamily, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = merchantName, color = TextPrimary.copy(alpha = 0.8f), fontSize = 14.sp, fontFamily = NaedaFontFamily)
            }
            Text(text = amountText, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = NaedaFontFamily)
        }
    }
}

@Composable
fun RbaPinScreen(
    paymentAmount: Long,
    merchantName: String,
    stepInfo: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val maxLength = 6

    val shakeOffset by animateFloatAsState(
        targetValue = if (isError) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "shake"
    )

    LaunchedEffect(isError) {
        if (isError) {
            kotlinx.coroutines.delay(600)
            enteredPin = ""
            isError = false
        }
    }

    LaunchedEffect(enteredPin) {
        if (enteredPin.length == maxLength) {
            // TODO: 백엔드 PIN 검증 후 onSuccess() 호출
            onSuccess()
        }
    }

    RbaAuthScaffold(
        title = "PIN 번호를\n입력해주세요",
        subtitle = "등록하신 6자리 PIN을 입력하세요",
        stepInfo = stepInfo,
        paymentAmount = paymentAmount,
        merchantName = merchantName,
        onCancel = onCancel
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .offset(x = if (isError) (shakeOffset * 8).dp else 0.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(maxLength) { index ->
                val isFilled = index < enteredPin.length
                val dotColor = when {
                    isError -> ErrorColor
                    isFilled -> AccentColor
                    else -> AccentColor.copy(alpha = 0.2f)
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            animateColorAsState(
                                targetValue = dotColor,
                                animationSpec = tween(150),
                                label = "dot_color_$index"
                            ).value
                        )
                )
            }
        }

        if (isError) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PIN이 일치하지 않습니다",
                color = ErrorColor,
                fontSize = 13.sp,
                fontFamily = NaedaFontFamily,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        NaedaNumericKeypad(
            onNumberClick = { num -> if (enteredPin.length < maxLength) enteredPin += num },
            onDelete = { if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1) }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RbaPhoneScreen(
    paymentAmount: Long,
    merchantName: String,
    stepInfo: String,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var enteredDigits by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val maxLength = 4

    LaunchedEffect(isError) {
        if (isError) {
            kotlinx.coroutines.delay(600)
            enteredDigits = ""
            isError = false
        }
    }

    LaunchedEffect(enteredDigits) {
        if (enteredDigits.length == maxLength) {
            // TODO: 백엔드 전화번호 검증 후 onSuccess() 호출
            onSuccess()
        }
    }

    RbaAuthScaffold(
        title = title,
        subtitle = subtitle,
        stepInfo = stepInfo,
        paymentAmount = paymentAmount,
        merchantName = merchantName,
        onCancel = onCancel
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(maxLength) { index ->
                val char = enteredDigits.getOrNull(index)
                val isActive = index == enteredDigits.length
                val isFilled = char != null

                val borderColor by animateColorAsState(
                    targetValue = when {
                        isError -> ErrorColor
                        isActive -> AccentColor
                        isFilled -> AccentColor.copy(alpha = 0.6f)
                        else -> AccentColor.copy(alpha = 0.2f)
                    },
                    animationSpec = tween(200),
                    label = "border_$index"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(56.dp, 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isFilled) AccentColor.copy(alpha = 0.1f)
                            else AccentColor.copy(alpha = 0.05f)
                        )
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (char != null) {
                        Text(text = "•", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    if (isActive) {
                        val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                            label = "cursor_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(2.dp, 24.dp)
                                .background(AccentColor.copy(alpha = alpha))
                        )
                    }
                }
            }
        }

        if (isError) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "번호가 일치하지 않습니다",
                color = ErrorColor,
                fontSize = 13.sp,
                fontFamily = NaedaFontFamily,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        NaedaNumericKeypad(
            onNumberClick = { num -> if (enteredDigits.length < maxLength) enteredDigits += num },
            onDelete = { if (enteredDigits.isNotEmpty()) enteredDigits = enteredDigits.dropLast(1) }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NaedaNumericKeypad(onNumberClick: (String) -> Unit, onDelete: () -> Unit) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            key.isEmpty() -> Spacer(modifier = Modifier.fillMaxWidth())
                            key == "⌫" -> KeypadButton(label = key, isDelete = true, onClick = onDelete)
                            else -> KeypadButton(label = key, onClick = { onNumberClick(key) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(label: String, isDelete: Boolean = false, onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "key_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDelete) KeypadDeleteBg else KeypadBg)
            .clickable { isPressed = true; onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isDelete) TextSecondary else TextPrimary,
            fontSize = if (isDelete) 20.sp else 22.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = NaedaFontFamily
        )
    }

    LaunchedEffect(isPressed) {
        if (isPressed) { kotlinx.coroutines.delay(100); isPressed = false }
    }
}