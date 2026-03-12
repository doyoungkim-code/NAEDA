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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt


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
    var isSavingFacePaySettings by remember { mutableStateOf(false) }
    var currentPinResetKey by remember { mutableStateOf(0) }
    var completedSecondaryAuthEnabled by remember { mutableStateOf(false) }

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

    fun saveFacePaySettings(enableSecondaryAuth: Boolean, currentPin: String?) {
        if (isSavingFacePaySettings) {
            return
        }

        isSavingFacePaySettings = true
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    FaceRegistrationRepository.updateFacePaySettings(
                        enableSecondaryAuth = enableSecondaryAuth,
                        currentPin = currentPin
                    )
                }
            }

            isSavingFacePaySettings = false
            result.onSuccess { response ->
                completedSecondaryAuthEnabled = response.secondaryAuthEnabled
                AuthPrefs.saveFacePaySettings(
                    context = context,
                    faceRegistered = response.faceRegistered,
                    secondaryAuthEnabled = response.secondaryAuthEnabled
                )
                currentPinResetKey = 0
                stage = RegisterStage.Success
            }.onFailure { throwable ->
                if (enableSecondaryAuth) {
                    currentPinResetKey += 1
                }
                globalError = throwable.message ?: "페이스페이 설정 저장에 실패했습니다."
            }
        }
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
                        isSaving = isSavingFacePaySettings,
                        onUsePin = {
                            currentPinResetKey = 0
                            stage = RegisterStage.CurrentPin(resetKey = currentPinResetKey)
                        },
                        onSkip = { saveFacePaySettings(enableSecondaryAuth = false, currentPin = null) }
                    )

                    is RegisterStage.CurrentPin -> CurrentPinStageContent(
                        resetKey = currentPinResetKey,
                        isSaving = isSavingFacePaySettings,
                        onCurrentPinEntered = { currentPin ->
                            saveFacePaySettings(enableSecondaryAuth = true, currentPin = currentPin)
                        }
                    )

                    is RegisterStage.Success -> SuccessStageContent(
                        secondaryAuthEnabled = completedSecondaryAuthEnabled,
                        onComplete = onRegisterComplete
                    )
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



