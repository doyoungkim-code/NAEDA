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
import androidx.compose.material3.Icon
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt


@Composable
internal fun PermissionRequestContentReal(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    RegistrationMessageScreen(
        title = if (isPermanentlyDenied) "카메라 권한이 차단되었습니다" else "카메라 권한이 필요합니다",
        description = if (isPermanentlyDenied) {
            "얼굴 등록과 신분증 촬영을 위해 설정에서 카메라 권한을 허용해 주세요."
        } else {
            "페이스페이 등록을 위해 카메라 권한을 허용해 주세요."
        },
        primaryButtonText = if (isPermanentlyDenied) "설정으로 이동" else "권한 허용하기",
        onPrimaryClick = if (isPermanentlyDenied) onOpenSettings else onRequestPermission
    )
}

@Composable
internal fun RegistrationMessageScreen(
    title: String,
    description: String,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(Mint100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("😊", fontSize = 36.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = OnBackground,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            fontFamily = NaedaFontFamily,
            fontSize = 15.sp,
            color = OnSurfaceVariant,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint900)
        ) {
            Text(
                text = primaryButtonText,
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
internal fun RegistrationChecklistScreen(
    title: String,
    tips: List<String>,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = OnBackground,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        tips.forEach { tip ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Mint50, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Mint500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = tip,
                        fontFamily = NaedaFontFamily,
                        fontSize = 14.sp,
                        color = OnBackground,
                        lineHeight = 22.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint900)
        ) {
            Text(
                text = primaryButtonText,
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
internal fun ErrorBanner(message: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.14f))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            color = Error,
            fontFamily = NaedaFontFamily,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
internal fun PinDotsReal(pinLength: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(6) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = if (index < pinLength) Mint500 else Color.White.copy(alpha = 0.18f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
internal fun FaceCaptureStageContent(
    spec: FaceCaptureSpec,
    currentIndex: Int,
    totalCount: Int,
    onPoseSaved: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val postureState = rememberDevicePostureState()
    var statusMessage by remember { mutableStateOf("휴대폰을 세로로 똑바로 세워주세요.") }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isUploading by remember { mutableStateOf(false) }
    val requestInFlight = remember { AtomicBoolean(false) }
    val holdStartedAt = remember { AtomicLong(0L) }
    val livenessEvaluator = remember { RegistrationPassiveLivenessEvaluator() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1717))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "STEP ${currentIndex + 1} / $totalCount",
                fontFamily = NaedaFontFamily,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Text(
            text = spec.instruction,
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = statusMessage,
            fontFamily = NaedaFontFamily,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        FaceRegistrationCameraCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
            onFaceFrame = { imageProxy, face, luminance ->
                if (requestInFlight.get()) {
                    imageProxy.close()
                    return@FaceRegistrationCameraCard
                }

                val posture = postureState
                val isCentered = isFaceCentered(face, imageProxy.width, imageProxy.height)
                val locallyMatched = matchesFaceCaptureDirection(
                    yaw = face.headEulerAngleY,
                    pitch = face.headEulerAngleX,
                    roll = face.headEulerAngleZ,
                    direction = spec.localDirection
                )
                val isLive = livenessEvaluator.observe(face)

                when {
                    !posture.isUpright -> {
                        resetHold(holdStartedAt) { holdProgress = it }
                        statusMessage = posture.message
                        imageProxy.close()
                        return@FaceRegistrationCameraCard
                    }

                    luminance < 35.0 -> {
                        resetHold(holdStartedAt) { holdProgress = it }
                        statusMessage = "화면이 어두워서 얼굴 인식이 어렵습니다."
                        imageProxy.close()
                        return@FaceRegistrationCameraCard
                    }

                    !isCentered -> {
                        resetHold(holdStartedAt) { holdProgress = it }
                        statusMessage = "얼굴을 가이드 중앙에 맞춰주세요."
                        imageProxy.close()
                        return@FaceRegistrationCameraCard
                    }

                    !isLive -> {
                        resetHold(holdStartedAt) { holdProgress = it }
                        statusMessage = livenessEvaluator.guideText()
                        imageProxy.close()
                        return@FaceRegistrationCameraCard
                    }

                    !locallyMatched -> {
                        resetHold(holdStartedAt) { holdProgress = it }
                        statusMessage = spec.instruction
                        imageProxy.close()
                        return@FaceRegistrationCameraCard
                    }
                }

                val now = System.currentTimeMillis()
                if (holdStartedAt.get() == 0L) {
                    holdStartedAt.set(now)
                }
                val elapsed = now - holdStartedAt.get()
                holdProgress = (elapsed / 1000f).coerceIn(0f, 1f)
                statusMessage = if (elapsed < 1000L) {
                    "현재 자세를 1초 유지해주세요."
                } else {
                    "자세 확인 완료. 서버에 저장 중입니다."
                }

                if (elapsed < 1000L) {
                    imageProxy.close()
                    return@FaceRegistrationCameraCard
                }

                val payload = runCatching {
                    createRegistrationFaceFramePayload(imageProxy, face.boundingBox)
                }.onFailure { throwable ->
                    onError("촬영 프레임 변환에 실패했습니다: ${throwable.message}")
                }.getOrNull()
                imageProxy.close()

                if (payload == null) {
                    resetHold(holdStartedAt) { holdProgress = it }
                    return@FaceRegistrationCameraCard
                }

                requestInFlight.set(true)
                isUploading = true
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val headPose = FaceRegistrationRepository.checkHeadPose(
                            expectedDirection = spec.expectedDirection,
                            imageBytes = payload.fullFrameJpeg
                        )
                        ensureHeadPoseMatched(headPose, spec)
                        enrollWithFallback(spec.backendPose, payload)
                    }.onSuccess {
                        requestInFlight.set(false)
                        isUploading = false
                        holdStartedAt.set(0L)
                        holdProgress = 0f
                        onPoseSaved()
                    }.onFailure { throwable ->
                        requestInFlight.set(false)
                        isUploading = false
                        resetHold(holdStartedAt) { holdProgress = it }
                        onError(throwable.message ?: "얼굴 등록에 실패했습니다.")
                    }
                }
            },
            onNoFace = {
                resetHold(holdStartedAt) { holdProgress = it }
                statusMessage = "얼굴을 화면 중앙에 맞춰주세요."
            },
            overlay = {
                FaceCaptureOverlay(
                    poseLabel = spec.title,
                    holdProgress = holdProgress,
                    isUploading = isUploading
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { (currentIndex + holdProgress) / totalCount.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            color = Mint500,
            trackColor = Color.White.copy(alpha = 0.16f)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            faceCaptureSequence.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentIndex) 14.dp else 10.dp)
                        .background(
                            color = when {
                                index < currentIndex -> Mint500
                                index == currentIndex -> Color.White
                                else -> Color.White.copy(alpha = 0.18f)
                            },
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
internal fun FaceCaptureOverlay(
    poseLabel: String,
    holdProgress: Float,
    isUploading: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.7f)
                .aspectRatio(0.78f)
                .border(
                    width = 2.dp,
                    color = if (isUploading) Mint500 else Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(180.dp)
                )
        )

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.36f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = poseLabel,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.White
            )
        }

        if (holdProgress > 0f || isUploading) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.36f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Mint500
                        )
                        Text("서버 저장 중", color = Color.White, fontFamily = NaedaFontFamily, fontSize = 13.sp)
                    } else {
                        CircularProgressIndicator(
                            progress = { holdProgress },
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Mint500,
                            trackColor = Color.White.copy(alpha = 0.18f)
                        )
                        Text("${(holdProgress * 100).roundToInt()}% 유지", color = Color.White, fontFamily = NaedaFontFamily, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun IdCardScanningStageContent(
    onExtracted: (ResidentIdExtractResponseDto) -> Unit,
    onError: (String?) -> Unit
) {
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("신분증을 가이드 안에 맞춰주세요.") }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var requestInFlight by remember { mutableStateOf(false) }
    val lastRequestAt = remember { AtomicLong(0L) }
    var holdStartedAt by remember { mutableStateOf(0L) }
    var consecutiveRecoverableMisses by remember { mutableStateOf(0) }
    var latestExtract by remember { mutableStateOf<ResidentIdExtractResponseDto?>(null) }

    fun resetRecognition(message: String = "신분증을 가이드 안에 맞춰주세요.") {
        holdStartedAt = 0L
        holdProgress = 0f
        consecutiveRecoverableMisses = 0
        latestExtract = null
        statusMessage = message
    }

    LaunchedEffect(holdStartedAt, latestExtract) {
        if (holdStartedAt == 0L || latestExtract == null) {
            holdProgress = 0f
            return@LaunchedEffect
        }

        while (holdStartedAt != 0L && latestExtract != null) {
            val progress = ((System.currentTimeMillis() - holdStartedAt).toFloat() / ID_CARD_HOLD_DURATION_MS)
                .coerceIn(0f, 1f)
            holdProgress = progress
            statusMessage = if (progress < 1f) {
                "신분증 정보를 읽는 중입니다. 흔들리지 않게 유지해 주세요."
            } else {
                "신분증 인식이 완료되었습니다."
            }
            onError(null)

            if (progress >= 1f) {
                val extracted = latestExtract
                resetRecognition("신분증 인식이 완료되었습니다.")
                extracted?.let(onExtracted)
                break
            }

            delay(50L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1717))
    ) {
        Text(
            text = "신분증을 2초 동안 유지해 주세요",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )

        Text(
            text = statusMessage,
            fontFamily = NaedaFontFamily,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        DocumentCaptureCameraCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            onFrame = { imageProxy ->
                if (requestInFlight) {
                    imageProxy.close()
                    return@DocumentCaptureCameraCard
                }

                val now = System.currentTimeMillis()
                if (now - lastRequestAt.get() < ID_CARD_REQUEST_INTERVAL_MS) {
                    imageProxy.close()
                    return@DocumentCaptureCameraCard
                }
                lastRequestAt.set(now)

                val jpegBytes = runCatching { imageProxyToRegistrationJpegBytes(imageProxy) }
                    .onFailure { throwable ->
                        onError("신분증 프레임 변환 실패: ${throwable.message}")
                    }
                    .getOrNull()
                imageProxy.close()

                if (jpegBytes == null) {
                    return@DocumentCaptureCameraCard
                }

                requestInFlight = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching { FaceRegistrationRepository.extractResidentId(jpegBytes) }
                    }
                    requestInFlight = false

                    result.onSuccess { extracted ->
                        val provider = extracted.provider?.trim()?.lowercase()
                        if (provider == "mock") {
                            resetRecognition("실제 OCR 서버가 아니라 mock 응답을 받았습니다.")
                            onError("서버 OCR이 mock 모드입니다. AI 설정을 확인해 주세요.")
                            return@onSuccess
                        }

                        val isValid = extracted.documentMatched &&
                            !extracted.name.isNullOrBlank() &&
                            extracted.residentFront6?.length == 6 &&
                            extracted.residentBackFirst1?.length == 1

                        if (!isValid) {
                            if (holdStartedAt != 0L && consecutiveRecoverableMisses < ID_CARD_ALLOWED_MISSES) {
                                consecutiveRecoverableMisses += 1
                                statusMessage = "신분증 정보를 다시 맞추는 중입니다. 그대로 유지해 주세요."
                                onError(null)
                            } else {
                                resetRecognition()
                                onError(null)
                            }
                            return@onSuccess
                        }

                        latestExtract = extracted
                        consecutiveRecoverableMisses = 0
                        if (holdStartedAt == 0L) {
                            holdStartedAt = System.currentTimeMillis()
                        }
                        statusMessage = "신분증 정보를 읽는 중입니다. 흔들리지 않게 유지해 주세요."
                        onError(null)
                    }.onFailure { throwable ->
                        if (throwable is ApiRequestException && throwable.statusCode == 400) {
                            if (holdStartedAt != 0L && consecutiveRecoverableMisses < ID_CARD_ALLOWED_MISSES) {
                                consecutiveRecoverableMisses += 1
                                statusMessage = "신분증 정보를 다시 맞추는 중입니다. 그대로 유지해 주세요."
                            } else {
                                resetRecognition()
                            }
                            onError(null)
                            return@onFailure
                        }

                        resetRecognition()
                        onError(throwable.message ?: "신분증 OCR 추출에 실패했습니다.")
                    }
                }
            },
            overlay = {
                IdCaptureOverlay(
                    holdProgress = holdProgress,
                    isExtracting = requestInFlight,
                    isRecognizing = requestInFlight || holdStartedAt != 0L || holdProgress > 0f
                )
            }
        )
    }
}

@Composable
internal fun IdCaptureOverlay(
    holdProgress: Float,
    isExtracting: Boolean,
    isRecognizing: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.88f)
                .aspectRatio(1.586f)
                .border(
                    width = 2.dp,
                    color = if (isRecognizing || isExtracting) Mint500 else Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(20.dp)
                )
        )
        if (isRecognizing) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.42f)
            ) {
                Column(
                    modifier = Modifier
                        .width(260.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isExtracting && holdProgress <= 0f) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Mint500
                            )
                        } else {
                            CircularProgressIndicator(
                                progress = { holdProgress },
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Mint500,
                                trackColor = Color.White.copy(alpha = 0.18f)
                            )
                        }
                        Text(
                            text = if (holdProgress > 0f) {
                                "신분증 인식 중 ${(holdProgress * 100).roundToInt()}%"
                            } else {
                                "신분증 정보를 읽는 중입니다..."
                            },
                            color = Color.White,
                            fontFamily = NaedaFontFamily,
                            fontSize = 13.sp
                        )
                    }
                    LinearProgressIndicator(
                        progress = { holdProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp),
                        color = Mint500,
                        trackColor = Color.White.copy(alpha = 0.18f)
                    )
                    Text(
                        text = "흔들리지 않게 유지하면 자동 촬영됩니다.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = NaedaFontFamily,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}


