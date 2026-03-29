package com.example.naedaterminal.ui.screen.payment

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedaterminal.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

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
    object Signature : RbaAuthType()
}

@Composable
fun RbaAuthContainer(
    authSteps: List<RbaAuthType>,
    paymentAmount: Long,
    merchantName: String,
    apiBaseUrl: String,
    userNo: Long?,
    onAuthComplete: () -> Unit,
    onAuthCancel: () -> Unit,
    onPinEntered: ((pin: String) -> Unit)? = null,
    onPhoneEntered: ((digits: String) -> Unit)? = null,
    onSignatureConfirmed: (() -> Unit)? = null
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
                onSuccess = { pin ->
                    onPinEntered?.invoke(pin)
                    currentStepIndex++
                },
                onCancel = onAuthCancel
            )
            is RbaAuthType.PhoneMiddleFour -> RbaPhoneScreen(
                paymentAmount = paymentAmount,
                merchantName = merchantName,
                stepInfo = "${currentStepIndex + 1}/${authSteps.size}",
                apiBaseUrl = apiBaseUrl,
                userNo = userNo,
                title = "전화번호 가운데\n4자리를 입력해주세요",
                subtitle = "본인 확인을 위해 휴대폰 번호 가운데 4자리를 입력하세요",
                onSuccess = { digits ->
                    onPhoneEntered?.invoke(digits)
                    currentStepIndex++
                },
                onCancel = onAuthCancel
            )
            is RbaAuthType.Signature -> SignatureCaptureScreen(
                paymentAmount = paymentAmount,
                merchantName = merchantName,
                stepInfo = "${currentStepIndex + 1}/${authSteps.size}",
                onSuccess = {
                    onSignatureConfirmed?.invoke()
                    currentStepIndex++
                },
                onCancel = onAuthCancel
            )
        }
    }
}

@Composable
fun AmbiguousAuthChoiceScreen(
    paymentAmount: Long,
    merchantName: String,
    onChoosePin: () -> Unit,
    onChoosePhone: () -> Unit,
    onCancel: () -> Unit
) {
    RbaAuthScaffold(
        title = "추가 인증 방식을\n선택해주세요",
        subtitle = "애매한 얼굴 매칭은 PIN 번호 또는 전화번호 4자리 중 하나가 필요합니다",
        stepInfo = "1/1",
        paymentAmount = paymentAmount,
        merchantName = merchantName,
        onCancel = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AuthChoiceButton(
                title = "PIN 번호 입력",
                subtitle = "등록한 6자리 PIN 번호로 인증합니다",
                onClick = onChoosePin
            )
            AuthChoiceButton(
                title = "전화번호 4자리 입력",
                subtitle = "휴대폰 번호 가운데 4자리로 인증합니다",
                onClick = onChoosePhone
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "둘 중 한 가지 인증만 통과하면 결제를 진행합니다.",
            color = TextSecondary,
            fontSize = 13.sp,
            fontFamily = NaedaFontFamily,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
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
    onSuccess: (pin: String) -> Unit,
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
            onSuccess(enteredPin)
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
    apiBaseUrl: String,
    userNo: Long?,
    title: String,
    subtitle: String,
    onSuccess: (digits: String) -> Unit,
    onCancel: () -> Unit
) {
    var enteredDigits by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("번호가 일치하지 않습니다") }
    val maxLength = 4
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    LaunchedEffect(isError) {
        if (isError) {
            kotlinx.coroutines.delay(600)
            enteredDigits = ""
            isError = false
        }
    }

    LaunchedEffect(enteredDigits) {
        if (enteredDigits.length == maxLength && !isSubmitting) {
            if (userNo == null) {
                errorMessage = "사용자 정보를 확인할 수 없습니다"
                isError = true
                return@LaunchedEffect
            }

            isSubmitting = true
            scope.launch {
                val verified = withContext(Dispatchers.IO) {
                    runCatching {
                        verifyPhoneMiddleDigits(
                            client = client,
                            apiBaseUrl = apiBaseUrl,
                            userNo = userNo,
                            middleDigits = enteredDigits
                        )
                    }.getOrElse { error ->
                        PhoneVerifyResult(false, error.message ?: "전화번호 검증에 실패했습니다.")
                    }
                }

                isSubmitting = false
                if (verified.verified) {
                    onSuccess(enteredDigits)
                } else {
                    errorMessage = verified.message ?: "번호가 일치하지 않습니다"
                    isError = true
                }
            }
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
                text = errorMessage,
                color = ErrorColor,
                fontSize = 13.sp,
                fontFamily = NaedaFontFamily,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        if (isSubmitting) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "전화번호를 확인하는 중입니다...",
                color = AccentColor,
                fontSize = 13.sp,
                fontFamily = NaedaFontFamily,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        NaedaNumericKeypad(
            onNumberClick = {
                num -> if (enteredDigits.length < maxLength && !isSubmitting) enteredDigits += num
            },
            onDelete = { if (enteredDigits.isNotEmpty() && !isSubmitting) enteredDigits = enteredDigits.dropLast(1) }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private data class PhoneVerifyResult(
    val verified: Boolean,
    val message: String?
)

@Composable
private fun AuthChoiceButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentColor.copy(alpha = 0.1f),
            contentColor = TextPrimary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NaedaFontFamily
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextSecondary,
                fontFamily = NaedaFontFamily
            )
        }
    }
}

private fun verifyPhoneMiddleDigits(
    client: OkHttpClient,
    apiBaseUrl: String,
    userNo: Long,
    middleDigits: String
): PhoneVerifyResult {
    val payload = JSONObject()
        .put("userNo", userNo)
        .put("middleDigits", middleDigits)

    val req = Request.Builder()
        .url("${apiBaseUrl.trimEnd('/')}/api/rba/phone/verify")
        .post(payload.toString().toRequestBody("application/json".toMediaType()))
        .build()

    return client.newCall(req).execute().use { res ->
        val raw = res.body?.string().orEmpty()
        if (!res.isSuccessful) {
            val json = runCatching { JSONObject(raw) }.getOrNull()
            return@use PhoneVerifyResult(
                verified = false,
                message = json?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: "전화번호 검증에 실패했습니다."
            )
        }

        val json = JSONObject(raw)
        PhoneVerifyResult(
            verified = json.optBoolean("verified", false),
            message = json.optString("message").takeIf { it.isNotBlank() }
        )
    }
}

@Composable
private fun SignatureCaptureScreen(
    paymentAmount: Long,
    merchantName: String,
    stepInfo: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val hasSignature = strokes.any { it.size > 1 } || currentStroke.size > 1

    RbaAuthScaffold(
        title = "서명을 입력해주세요",
        subtitle = "5만원 이상 결제는 화면에 서명 후 확인이 필요합니다",
        stepInfo = stepInfo,
        paymentAmount = paymentAmount,
        merchantName = merchantName,
        onCancel = onCancel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, AccentColor.copy(alpha = 0.24f), RoundedCornerShape(20.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentStroke = listOf(offset)
                            },
                            onDragEnd = {
                                if (currentStroke.size > 1) {
                                    strokes.add(currentStroke)
                                }
                                currentStroke = emptyList()
                            },
                            onDragCancel = {
                                currentStroke = emptyList()
                            }
                        ) { change, _ ->
                            currentStroke = currentStroke + change.position
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    strokes.forEach { stroke ->
                        drawStroke(stroke)
                    }
                    drawStroke(currentStroke)
                }

                if (!hasSignature) {
                    Text(
                        text = "손가락으로 서명해주세요",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontFamily = NaedaFontFamily,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        strokes.clear()
                        currentStroke = emptyList()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentColor)
                ) {
                    Text("다시 쓰기", fontFamily = NaedaFontFamily)
                }
                Button(
                    onClick = onSuccess,
                    enabled = hasSignature,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentColor,
                        contentColor = Color.White
                    )
                ) {
                    Text("확인", fontFamily = NaedaFontFamily)
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun DrawScope.drawStroke(points: List<Offset>) {
    if (points.isEmpty()) {
        return
    }
    if (points.size == 1) {
        drawCircle(
            color = AccentColor,
            radius = 4.dp.toPx(),
            center = points.first()
        )
        return
    }

    for (index in 0 until points.lastIndex) {
        drawLine(
            color = AccentColor,
            start = points[index],
            end = points[index + 1],
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )
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
