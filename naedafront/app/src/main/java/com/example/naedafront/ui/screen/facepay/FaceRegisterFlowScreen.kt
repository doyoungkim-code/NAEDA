package com.example.naedafront.ui.screen.facepay

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.ApiRequestException
import com.example.naedafront.data.remote.FaceRegistrationRepository
import com.example.naedafront.data.remote.HeadPoseCheckResponseDto
import com.example.naedafront.data.remote.ResidentIdExtractResponseDto
import com.example.naedafront.data.remote.ResidentIdVerifyResponseDto
import com.example.naedafront.ui.screen.signup.NumberKeypad
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Error
import com.example.naedafront.ui.theme.Mint100
import com.example.naedafront.ui.theme.Mint50
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaFontFamily
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import com.example.naedafront.ui.theme.Outline
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt


private data class FaceCaptureSpec(
    val backendPose: String,
    val title: String,
    val instruction: String,
    val expectedDirection: String,
    val localDirection: FaceCaptureDirection
)

private val faceCaptureSequence = listOf(
    FaceCaptureSpec("front1", "정면 1", "정면을 바라봐 주세요", "front", FaceCaptureDirection.FRONT),
    FaceCaptureSpec("front2", "정면 2", "정면을 유지해 주세요", "front", FaceCaptureDirection.FRONT),
    FaceCaptureSpec("front3", "정면 3", "정면을 한 번 더 유지해 주세요", "front", FaceCaptureDirection.FRONT),
    FaceCaptureSpec("left", "오른쪽", "고개를 오른쪽으로 돌려주세요", "left", FaceCaptureDirection.LEFT),
    FaceCaptureSpec("right", "왼쪽", "고개를 왼쪽으로 돌려주세요", "right", FaceCaptureDirection.RIGHT),
    FaceCaptureSpec("up", "위", "고개를 위로 들어주세요", "up", FaceCaptureDirection.UP),
    FaceCaptureSpec("down", "아래", "고개를 아래로 내려주세요", "down", FaceCaptureDirection.DOWN)
)

private sealed class RegisterStage {
    object PermissionRequest : RegisterStage()
    object Intro : RegisterStage()
    object Guide : RegisterStage()
    data class FaceCapture(val index: Int) : RegisterStage()
    object IdGuide : RegisterStage()
    object IdScanning : RegisterStage()
    data class IdConfirm(val extracted: ResidentIdExtractResponseDto) : RegisterStage()
    object PinChoice : RegisterStage()
    data class PinCreate(val currentPin: String? = null) : RegisterStage()
    data class PinConfirm(val newPin: String, val currentPin: String? = null) : RegisterStage()
    data class CurrentPin(val newPin: String) : RegisterStage()
    object Success : RegisterStage()
}

private data class DevicePostureState(
    val isUpright: Boolean,
    val message: String
)

private data class RegistrationFaceFramePayload(
    val fullFrameJpeg: ByteArray,
    val croppedFaceJpeg: ByteArray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRegisterFlowScreen(
    onBack: () -> Unit,
    onRegisterComplete: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val token = remember(context) { AuthPrefs.getAccessToken(context).orEmpty() }
    val scope = rememberCoroutineScope()
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isPermanentlyDenied by remember { mutableStateOf(false) }
    var stage: RegisterStage by remember {
        mutableStateOf(if (hasCameraPermission) RegisterStage.Intro else RegisterStage.PermissionRequest)
    }
    var globalError by remember { mutableStateOf<String?>(null) }
    var pendingCurrentPin by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            stage = RegisterStage.Intro
        } else {
            val canAskAgain = activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ?: false
            isPermanentlyDenied = !canAskAgain
            stage = RegisterStage.PermissionRequest
        }
    }

    LaunchedEffect(stage) {
        globalError = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleForStage(stage),
                        fontFamily = NaedaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBackground
                    )
                },
                navigationIcon = {
                    if (stage !is RegisterStage.Success) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = OnBackground
                            )
                        }
                    }
                },
                actions = {
                    if (stage is RegisterStage.Success) {
                        IconButton(onClick = onRegisterComplete) {
                            Icon(Icons.Default.Close, contentDescription = "닫기", tint = OnBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (token.isBlank()) {
                RegistrationMessageScreen(
                    title = "로그인이 필요합니다",
                    description = "현재 로그인된 사용자 컨텍스트가 없어 페이스페이 등록을 시작할 수 없습니다.",
                    primaryButtonText = "뒤로가기",
                    onPrimaryClick = onBack
                )
            } else {
                when (val currentStage = stage) {
                    is RegisterStage.PermissionRequest -> PermissionRequestContentReal(
                        isPermanentlyDenied = isPermanentlyDenied,
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }
                    )

                    is RegisterStage.Intro -> RegistrationMessageScreen(
                        title = "이제 오프라인에서 얼굴 인증으로 결제하세요",
                        description = "얼굴 등록, 신분증 확인, PIN 설정까지 완료하면 페이스페이 사용 준비가 끝납니다.",
                        primaryButtonText = "페이스페이 시작하기",
                        onPrimaryClick = { stage = RegisterStage.Guide }
                    )

                    is RegisterStage.Guide -> RegistrationChecklistScreen(
                        title = "얼굴 등록을 시작할게요",
                        tips = listOf(
                            "휴대폰을 세로로 똑바로 세워주세요.",
                            "정면 3장과 좌우상하 4장을 포함해 총 7장을 촬영합니다.",
                            "실제 사람 판정과 자세 1초 유지가 모두 필요합니다."
                        ),
                        primaryButtonText = "등록 시작하기",
                        onPrimaryClick = { stage = RegisterStage.FaceCapture(0) }
                    )

                    is RegisterStage.FaceCapture -> FaceCaptureStageContent(
                        spec = faceCaptureSequence[currentStage.index],
                        currentIndex = currentStage.index,
                        totalCount = faceCaptureSequence.size,
                        onPoseSaved = {
                            stage = if (currentStage.index + 1 < faceCaptureSequence.size) {
                                RegisterStage.FaceCapture(currentStage.index + 1)
                            } else {
                                RegisterStage.IdGuide
                            }
                        },
                        onError = { globalError = it }
                    )

                    is RegisterStage.IdGuide -> RegistrationChecklistScreen(
                        title = "신분증을 준비해 주세요",
                        tips = listOf(
                            "주민등록증 또는 운전면허증을 준비해 주세요.",
                            "신분증이 프레임 안에서 또렷하게 보이도록 맞춰주세요.",
                            "인식 상태를 3초간 유지하면 자동 촬영됩니다."
                        ),
                        primaryButtonText = "신분증 촬영 시작",
                        onPrimaryClick = { stage = RegisterStage.IdScanning }
                    )

                    is RegisterStage.IdScanning -> IdCardScanningStageContent(
                        onExtracted = { extracted -> stage = RegisterStage.IdConfirm(extracted) },
                        onError = { globalError = it }
                    )

                    is RegisterStage.IdConfirm -> IdConfirmStageContent(
                        extracted = currentStage.extracted,
                        onConfirmComplete = { stage = RegisterStage.PinChoice },
                        onConfirmError = { globalError = it }
                    )

                    is RegisterStage.PinChoice -> PinChoiceStageContent(
                        onUsePin = { stage = RegisterStage.PinCreate(currentPin = pendingCurrentPin) },
                        onSkip = { stage = RegisterStage.Success }
                    )

                    is RegisterStage.PinCreate -> PinCreateStageContent(
                        title = if (currentStage.currentPin == null) "새 PIN 번호를 설정해 주세요" else "새 PIN 번호를 다시 설정해 주세요",
                        description = "6자리 숫자로 페이스페이 2차 인증 PIN을 설정합니다.",
                        onPinCreated = { newPin ->
                            stage = RegisterStage.PinConfirm(
                                newPin = newPin,
                                currentPin = currentStage.currentPin
                            )
                        }
                    )

                    is RegisterStage.PinConfirm -> PinConfirmStageContent(
                        newPin = currentStage.newPin,
                        onPinConfirmed = { confirmedPin ->
                            if (confirmedPin != currentStage.newPin) {
                                globalError = "PIN 번호가 일치하지 않습니다. 다시 입력해 주세요."
                                return@PinConfirmStageContent
                            }
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    FaceRegistrationRepository.updatePin(
                                        currentPin = currentStage.currentPin,
                                        newPin = currentStage.newPin
                                    )
                                }.onSuccess {
                                    stage = RegisterStage.Success
                                }.onFailure { throwable ->
                                    val message = throwable.message ?: "PIN 설정에 실패했습니다."
                                    if (message.contains("현재 PIN을 입력해주세요") || message.contains("현재 PIN")) {
                                        stage = RegisterStage.CurrentPin(currentStage.newPin)
                                    } else {
                                        globalError = message
                                    }
                                }
                            }
                        }
                    )

                    is RegisterStage.CurrentPin -> CurrentPinStageContent(
                        onCurrentPinEntered = { currentPin ->
                            pendingCurrentPin = currentPin
                            stage = RegisterStage.PinCreate(currentPin = currentPin)
                        }
                    )

                    is RegisterStage.Success -> SuccessStageContent(onComplete = onRegisterComplete)
                }
            }

            globalError?.let { message ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    ErrorBanner(message)
                }
            }
        }
    }
}

private fun titleForStage(stage: RegisterStage): String {
    return when (stage) {
        is RegisterStage.PermissionRequest -> "권한 요청"
        is RegisterStage.Intro -> "페이스페이"
        is RegisterStage.Guide -> "촬영 가이드"
        is RegisterStage.FaceCapture -> "얼굴 등록"
        is RegisterStage.IdGuide -> "신분증 준비"
        is RegisterStage.IdScanning -> "신분증 촬영"
        is RegisterStage.IdConfirm -> "신분증 정보 확인"
        is RegisterStage.PinChoice -> "PIN 설정"
        is RegisterStage.PinCreate -> "PIN 입력"
        is RegisterStage.PinConfirm -> "PIN 확인"
        is RegisterStage.CurrentPin -> "현재 PIN 입력"
        is RegisterStage.Success -> "등록 완료"
    }
}

@Composable
private fun PermissionRequestContentReal(
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
private fun RegistrationMessageScreen(
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
private fun RegistrationChecklistScreen(
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
private fun ErrorBanner(message: String) {
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
private fun PinDotsReal(pinLength: Int) {
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
private fun FaceCaptureStageContent(
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
                    createFaceFramePayload(imageProxy, face.boundingBox)
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
private fun FaceCaptureOverlay(
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
private fun IdCardScanningStageContent(
    onExtracted: (ResidentIdExtractResponseDto) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf("신분증을 가이드 안에 맞춰주세요.") }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var requestInFlight by remember { mutableStateOf(false) }
    val lastRequestAt = remember { AtomicLong(0L) }
    val validDetectedAt = remember { AtomicLong(0L) }
    var latestExtract by remember { mutableStateOf<ResidentIdExtractResponseDto?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1717))
    ) {
        Text(
            text = "신분증을 3초 동안 유지해 주세요",
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
                if (now - lastRequestAt.get() < 1000L) {
                    imageProxy.close()
                    return@DocumentCaptureCameraCard
                }
                lastRequestAt.set(now)

                val jpegBytes = runCatching { imageProxyToJpegBytes(imageProxy) }
                    .onFailure { throwable ->
                        onError("신분증 프레임 변환 실패: ${throwable.message}")
                    }
                    .getOrNull()
                imageProxy.close()

                if (jpegBytes == null) {
                    return@DocumentCaptureCameraCard
                }

                requestInFlight = true
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        FaceRegistrationRepository.extractResidentId(jpegBytes)
                    }.onSuccess { extracted ->
                        requestInFlight = false
                        val isValid = extracted.documentMatched &&
                            !extracted.name.isNullOrBlank() &&
                            extracted.residentFront6?.length == 6 &&
                            extracted.residentBackFirst1?.length == 1

                        if (!isValid) {
                            validDetectedAt.set(0L)
                            holdProgress = 0f
                            latestExtract = null
                            statusMessage = "신분증이 선명하게 보이도록 다시 맞춰주세요."
                            return@onSuccess
                        }

                        latestExtract = extracted
                        if (validDetectedAt.get() == 0L) {
                            validDetectedAt.set(System.currentTimeMillis())
                        }
                        val elapsed = System.currentTimeMillis() - validDetectedAt.get()
                        holdProgress = (elapsed / 3000f).coerceIn(0f, 1f)
                        statusMessage = if (elapsed < 3000L) {
                            "OCR 인식 완료. 3초 유지 중입니다."
                        } else {
                            "신분증 인식이 완료되었습니다."
                        }

                        if (elapsed >= 3000L) {
                            validDetectedAt.set(0L)
                            holdProgress = 0f
                            latestExtract?.let(onExtracted)
                        }
                    }.onFailure { throwable ->
                        requestInFlight = false
                        validDetectedAt.set(0L)
                        holdProgress = 0f
                        latestExtract = null
                        onError(throwable.message ?: "신분증 OCR 추출에 실패했습니다.")
                    }
                }
            },
            overlay = {
                IdCaptureOverlay(
                    holdProgress = holdProgress,
                    isExtracting = requestInFlight
                )
            }
        )

        latestExtract?.let { extracted ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("최근 OCR 결과", color = Color.White, fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("이름: ${extracted.name ?: "-"}", color = Color.White.copy(alpha = 0.8f), fontFamily = NaedaFontFamily)
                    Text(
                        "주민번호: ${extracted.residentFront6 ?: "-"}-${extracted.residentBackFirst1 ?: "-"}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = NaedaFontFamily
                    )
                }
            }
        }
    }
}

@Composable
private fun IdCaptureOverlay(
    holdProgress: Float,
    isExtracting: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.88f)
                .aspectRatio(1.586f)
                .border(
                    width = 2.dp,
                    color = if (isExtracting) Mint500 else Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(20.dp)
                )
        )

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
                CircularProgressIndicator(
                    progress = { holdProgress },
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Mint500,
                    trackColor = Color.White.copy(alpha = 0.18f)
                )
                Text(
                    text = if (isExtracting) "OCR 확인 중" else "${(holdProgress * 100).roundToInt()}% 유지",
                    color = Color.White,
                    fontFamily = NaedaFontFamily,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun IdConfirmStageContent(
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
private fun LabeledField(
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
private fun PinChoiceStageContent(
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
            "PIN을 사용하면 얼굴 인식 후 한 번 더 확인해 결제를 보호할 수 있습니다.",
            "원하지 않으면 지금은 건너뛰고 나중에 다시 설정할 수 있습니다."
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
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint900)
        ) {
            Text("PIN 설정하기", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("이번에는 건너뛰기", fontFamily = NaedaFontFamily, fontSize = 15.sp, color = OnSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PinCreateStageContent(
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
private fun PinConfirmStageContent(
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
private fun CurrentPinStageContent(
    onCurrentPinEntered: (String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }

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
                text = "이미 PIN이 설정되어 있어 새 PIN 저장 전에 현재 PIN 확인이 필요합니다.",
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
                    if (currentPin.length < 6) {
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
private fun SuccessStageContent(
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
            text = "얼굴 7장 저장, 신분증 OCR 확인, PIN 단계가 모두 완료되었습니다.",
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

@Composable
private fun FaceRegistrationCameraCard(
    modifier: Modifier,
    cameraSelector: CameraSelector,
    onFaceFrame: (ImageProxy, Face, Double) -> Unit,
    onNoFace: () -> Unit,
    overlay: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnFaceFrame by rememberUpdatedState(onFaceFrame)
    val latestOnNoFace by rememberUpdatedState(onNoFace)
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            this.cameraSelector = cameraSelector
            setEnabledUseCases(
                LifecycleCameraController.IMAGE_ANALYSIS or LifecycleCameraController.IMAGE_CAPTURE
            )
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }

    DisposableEffect(cameraController, lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        cameraController.setImageAnalysisAnalyzer(executor) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return@setImageAnalysisAnalyzer
            }

            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    val bestFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    if (bestFace == null) {
                        latestOnNoFace()
                        imageProxy.close()
                    } else {
                        latestOnFaceFrame(imageProxy, bestFace, estimateLuminance(imageProxy))
                    }
                }
                .addOnFailureListener {
                    imageProxy.close()
                }
        }

        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            detector.close()
            executor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }
        )
        overlay()
    }
}

@Composable
private fun DocumentCaptureCameraCard(
    modifier: Modifier,
    onFrame: (ImageProxy) -> Unit,
    overlay: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnFrame by rememberUpdatedState(onFrame)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(
                LifecycleCameraController.IMAGE_ANALYSIS or LifecycleCameraController.IMAGE_CAPTURE
            )
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }

    DisposableEffect(cameraController, lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        cameraController.setImageAnalysisAnalyzer(executor) { imageProxy ->
            latestOnFrame(imageProxy)
        }

        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            executor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }
        )
        overlay()
    }
}

@Composable
private fun rememberDevicePostureState(): DevicePostureState {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    var postureState by remember {
        mutableStateOf(
            DevicePostureState(
                isUpright = false,
                message = "휴대폰을 세로로 똑바로 세워주세요."
            )
        )
    }

    DisposableEffect(sensorManager) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val y = event.values[1]
                val z = event.values[2]
                val upright = abs(z) < 5.5f && abs(y) > 6.5f
                postureState = DevicePostureState(
                    isUpright = upright,
                    message = if (upright) "휴대폰 자세가 정상입니다." else "휴대폰을 똑바로 세워주세요."
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return postureState
}

private suspend fun enrollWithFallback(
    pose: String,
    payload: RegistrationFaceFramePayload
) {
    try {
        FaceRegistrationRepository.enrollFace(pose, payload.croppedFaceJpeg)
    } catch (exception: ApiRequestException) {
        if (exception.errorCode == "NO_FACE") {
            FaceRegistrationRepository.enrollFace(pose, payload.fullFrameJpeg)
        } else {
            throw exception
        }
    }
}

private fun ensureHeadPoseMatched(
    response: HeadPoseCheckResponseDto,
    spec: FaceCaptureSpec
) {
    if (!response.matched) {
        val detected = response.detectedDirection ?: "unknown"
        val yaw = String.format("%.2f", response.yaw)
        val pitch = String.format("%.2f", response.pitch)
        throw IllegalStateException(
            "${spec.title} 자세가 감지되지 않았습니다. AI 판정=$detected (yaw=$yaw, pitch=$pitch)"
        )
    }
}

private fun buildIdConfirmError(response: ResidentIdVerifyResponseDto): String {
    return when {
        !response.nameMatched && !response.residentNoMatched -> "이름과 주민등록번호 일부가 모두 일치하지 않습니다."
        !response.nameMatched -> "이름이 로그인된 사용자 정보와 일치하지 않습니다."
        !response.residentNoMatched -> "주민등록번호 일부가 로그인된 사용자 정보와 일치하지 않습니다."
        else -> "신분증 확인에 실패했습니다."
    }
}

private fun isFaceCentered(face: Face, frameWidth: Int, frameHeight: Int): Boolean {
    val box = face.boundingBox
    val centerX = box.centerX().toFloat() / frameWidth.toFloat()
    val centerY = box.centerY().toFloat() / frameHeight.toFloat()
    return centerX in 0.3f..0.7f && centerY in 0.25f..0.75f
}


private fun resetHold(holdStartedAt: AtomicLong, updateProgress: (Float) -> Unit) {
    holdStartedAt.set(0L)
    updateProgress(0f)
}

private class RegistrationPassiveLivenessEvaluator {
    private var frameCount = 0
    private var movementScore = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var yawMin = 0f
    private var yawMax = 0f
    private var pitchMin = 0f
    private var pitchMax = 0f
    private var blinkState = 0
    private var blinkDetected = false
    private var guide = "실제 얼굴 여부를 확인 중입니다."

    fun observe(face: Face): Boolean {
        frameCount += 1
        val box = face.boundingBox
        val centerX = (box.left + box.right) / 2f
        val centerY = (box.top + box.bottom) / 2f
        if (frameCount == 1) {
            lastX = centerX
            lastY = centerY
            yawMin = face.headEulerAngleY
            yawMax = face.headEulerAngleY
            pitchMin = face.headEulerAngleX
            pitchMax = face.headEulerAngleX
        } else {
            movementScore += abs(centerX - lastX) + abs(centerY - lastY)
            yawMin = minOf(yawMin, face.headEulerAngleY)
            yawMax = maxOf(yawMax, face.headEulerAngleY)
            pitchMin = minOf(pitchMin, face.headEulerAngleX)
            pitchMax = maxOf(pitchMax, face.headEulerAngleX)
            lastX = centerX
            lastY = centerY
        }

        val leftEye = face.leftEyeOpenProbability ?: 1f
        val rightEye = face.rightEyeOpenProbability ?: 1f
        if (leftEye < 0.35f || rightEye < 0.35f) {
            if (blinkState == 0) {
                blinkState = 1
            }
        } else if (blinkState == 1 && leftEye > 0.7f && rightEye > 0.7f) {
            blinkDetected = true
            blinkState = 2
        }

        guide = when {
            !blinkDetected && frameCount < 5 -> "눈을 한 번 깜빡이거나 얼굴을 조금 움직여주세요."
            !blinkDetected && movementScore < 20f -> "정면을 유지한 채 미세하게 움직여주세요."
            else -> "실제 얼굴로 판단되었습니다."
        }

        return blinkDetected || movementScore > 24f || (yawMax - yawMin) > 6f || (pitchMax - pitchMin) > 6f
    }

    fun guideText(): String = guide
}

private fun estimateLuminance(imageProxy: ImageProxy): Double {
    val buffer = imageProxy.planes.firstOrNull()?.buffer ?: return 0.0
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    if (bytes.isEmpty()) return 0.0

    var total = 0L
    var count = 0
    val step = maxOf(1, bytes.size / 1500)
    var index = 0
    while (index < bytes.size) {
        total += bytes[index].toInt() and 0xFF
        count += 1
        index += step
    }
    return if (count == 0) 0.0 else total.toDouble() / count
}

private fun imageProxyToJpegBytes(imageProxy: ImageProxy): ByteArray {
    val nv21 = yuv420888ToNv21(imageProxy)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
    val output = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 92, output)
    val jpegBytes = output.toByteArray()
    return rotateJpeg(jpegBytes, imageProxy.imageInfo.rotationDegrees)
}

private fun createFaceFramePayload(
    imageProxy: ImageProxy,
    faceBounds: Rect
): RegistrationFaceFramePayload {
    val fullJpeg = imageProxyToJpegBytes(imageProxy)
    return RegistrationFaceFramePayload(
        fullFrameJpeg = fullJpeg,
        croppedFaceJpeg = cropFaceJpeg(fullJpeg, faceBounds)
    )
}

private fun rotateJpeg(jpegBytes: ByteArray, rotationDegrees: Int): ByteArray {
    if (rotationDegrees == 0) return jpegBytes
    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return jpegBytes
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    val output = ByteArrayOutputStream()
    rotated.compress(Bitmap.CompressFormat.JPEG, 92, output)
    bitmap.recycle()
    rotated.recycle()
    return output.toByteArray()
}

private fun cropFaceJpeg(jpegBytes: ByteArray, faceBounds: Rect): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return jpegBytes
    val cropRect = expandedFaceRect(faceBounds, bitmap.width, bitmap.height)
    if (cropRect.width() <= 0 || cropRect.height() <= 0) {
        bitmap.recycle()
        return jpegBytes
    }

    val croppedBitmap = Bitmap.createBitmap(
        bitmap,
        cropRect.left,
        cropRect.top,
        cropRect.width(),
        cropRect.height()
    )
    bitmap.recycle()

    val resizedBitmap = resizeBitmapIfNeeded(croppedBitmap, 720)
    if (resizedBitmap !== croppedBitmap) {
        croppedBitmap.recycle()
    }

    val output = ByteArrayOutputStream()
    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
    resizedBitmap.recycle()
    return output.toByteArray()
}

private fun expandedFaceRect(faceBounds: Rect, imageWidth: Int, imageHeight: Int): Rect {
    val centerX = faceBounds.centerX().toFloat()
    val centerY = faceBounds.centerY().toFloat()
    val targetSize = (max(faceBounds.width(), faceBounds.height()) * 1.8f).toInt().coerceAtLeast(1)
    var left = (centerX - targetSize / 2f).toInt()
    var top = (centerY - targetSize / 2f).toInt()
    var right = left + targetSize
    var bottom = top + targetSize

    if (left < 0) {
        right = (right - left).coerceAtMost(imageWidth)
        left = 0
    }
    if (top < 0) {
        bottom = (bottom - top).coerceAtMost(imageHeight)
        top = 0
    }
    if (right > imageWidth) {
        val delta = right - imageWidth
        left = (left - delta).coerceAtLeast(0)
        right = imageWidth
    }
    if (bottom > imageHeight) {
        val delta = bottom - imageHeight
        top = (top - delta).coerceAtLeast(0)
        bottom = imageHeight
    }

    return Rect(left, top, right, bottom)
}

private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val currentMax = max(bitmap.width, bitmap.height)
    if (currentMax <= maxDimension) {
        return bitmap
    }

    val scale = maxDimension / currentMax.toFloat()
    val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val width = image.width
    val height = image.height
    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]
    val yBuffer = yPlane.buffer.duplicate()
    val uBuffer = uPlane.buffer.duplicate()
    val vBuffer = vPlane.buffer.duplicate()

    val nv21 = ByteArray(width * height * 3 / 2)
    var outputOffset = 0

    for (row in 0 until height) {
        val rowStart = row * yPlane.rowStride
        for (col in 0 until width) {
            val index = rowStart + col * yPlane.pixelStride
            nv21[outputOffset++] = yBuffer.get(index)
        }
    }

    val chromaWidth = width / 2
    val chromaHeight = height / 2
    for (row in 0 until chromaHeight) {
        val uRowStart = row * uPlane.rowStride
        val vRowStart = row * vPlane.rowStride
        for (col in 0 until chromaWidth) {
            nv21[outputOffset++] = vBuffer.get(vRowStart + col * vPlane.pixelStride)
            nv21[outputOffset++] = uBuffer.get(uRowStart + col * uPlane.pixelStride)
        }
    }

    return nv21
}
