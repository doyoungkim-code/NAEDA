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
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.naedafront.R
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.ApiRequestException
import com.example.naedafront.data.remote.AssetAccountResponse
import com.example.naedafront.data.remote.AssetCardResponse
import com.example.naedafront.data.remote.AssetPayMethodResponse
import com.example.naedafront.data.remote.AssetRepository
import com.example.naedafront.data.remote.FaceRegistrationRepository
import com.example.naedafront.data.remote.HeadPoseCheckResponseDto
import com.example.naedafront.data.remote.PayLimitResponseDto
import com.example.naedafront.data.remote.ResidentIdExtractResponseDto
import com.example.naedafront.data.remote.ResidentIdVerifyResponseDto
import com.example.naedafront.ui.screen.signup.NumberKeypad
import com.example.naedafront.ui.theme.Error
import com.example.naedafront.ui.theme.Mint100
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaFontFamily
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
    FaceCaptureSpec("left", "왼쪽", "고개를 왼쪽으로 돌려주세요", "left", FaceCaptureDirection.LEFT),
    FaceCaptureSpec("right", "오른쪽", "고개를 오른쪽으로 돌려주세요", "right", FaceCaptureDirection.RIGHT),
    FaceCaptureSpec("up", "위", "고개를 위로 들어주세요", "up", FaceCaptureDirection.UP),
    FaceCaptureSpec("down", "아래", "고개를 아래로 내려주세요", "down", FaceCaptureDirection.DOWN)
)

private const val ID_CARD_HOLD_DURATION_MS = 2000L
private const val ID_CARD_REQUEST_INTERVAL_MS = 650L
private const val ID_CARD_ALLOWED_MISSES = 1
private val REGISTER_OVERLAY_CONTENT_TOP_PADDING = 64.dp

private sealed class RegisterStage {
    object PermissionRequest : RegisterStage()
    object Intro : RegisterStage()
    object Guide : RegisterStage()
    data class FaceCapture(val index: Int) : RegisterStage()
    object IdGuide : RegisterStage()
    object IdScanning : RegisterStage()
    data class IdConfirm(val extracted: ResidentIdExtractResponseDto) : RegisterStage()
    object PaymentMethodSelect : RegisterStage()
    object PaymentLimitSetup : RegisterStage()
    object PinChoice : RegisterStage()
    data class CurrentPin(val resetKey: Int = 0) : RegisterStage()
    object Saving : RegisterStage()   // 등록 중 로딩 화면
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

private data class PendingPayLimit(
    val dailyLimit: Long,
    val monthlyLimit: Long,
    val singleTransactionLimit: Long
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
    val userNo = remember(context) { AuthPrefs.getUserNo(context) }
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
    var selectedFacePayPaymentMethodId by remember { mutableStateOf<Long?>(null) }
    var pendingPayLimit by remember { mutableStateOf<PendingPayLimit?>(null) }

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

        val paymentMethodId = selectedFacePayPaymentMethodId
        val payLimit = pendingPayLimit
        if (paymentMethodId == null) {
            globalError = "대표 결제수단을 선택해 주세요."
            return
        }
        if (payLimit == null) {
            globalError = "결제 한도를 먼저 설정해 주세요."
            return
        }

        isSavingFacePaySettings = true
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    FaceRegistrationRepository.updateFacePaySettings(
                        enableSecondaryAuth = enableSecondaryAuth,
                        currentPin = currentPin,
                        paymentMethodId = paymentMethodId,
                        dailyLimit = payLimit.dailyLimit,
                        monthlyLimit = payLimit.monthlyLimit,
                        singleTransactionLimit = payLimit.singleTransactionLimit
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
                stage = RegisterStage.Saving
                kotlinx.coroutines.delay(1500L)
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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (token.isBlank() || userNo == null) {
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

                    is RegisterStage.Intro -> IntroStageContent(
                        onStartClick = { stage = RegisterStage.Guide }
                    )

                    is RegisterStage.Guide -> FaceGuideStageContent(
                        onStartClick = { stage = RegisterStage.FaceCapture(0) }
                    )

                    is RegisterStage.FaceCapture -> key(currentStage.index) {
                        FaceCaptureStageContent(
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
                            onRestartRequested = {
                                stage = RegisterStage.FaceCapture(0)
                            },
                            onError = { globalError = it }
                        )
                    }

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
                        onError = { }
                    )

                    is RegisterStage.IdConfirm -> IdConfirmStageContent(
                        extracted = currentStage.extracted,
                        onConfirmComplete = { stage = RegisterStage.PaymentMethodSelect },
                        onConfirmError = { globalError = it }
                    )

                    is RegisterStage.PaymentMethodSelect -> PaymentMethodSelectStageContent(
                        userNo = userNo!!,
                        onSelectionComplete = { paymentMethodId ->
                            selectedFacePayPaymentMethodId = paymentMethodId
                            stage = RegisterStage.PaymentLimitSetup
                        },
                        onError = { globalError = it }
                    )

                    is RegisterStage.PaymentLimitSetup -> PaymentLimitSetupStageContent(
                        userNo = userNo!!,
                        onSaveComplete = {
                            pendingPayLimit = it
                            stage = RegisterStage.PinChoice
                        },
                        onError = { globalError = it }
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

                    is RegisterStage.Saving -> SavingStageContent()

                    is RegisterStage.Success -> SuccessStageContent(
                        secondaryAuthEnabled = completedSecondaryAuthEnabled,
                        onComplete = onRegisterComplete
                    )
                }
            }

            val useDarkOverlayAction = stage is RegisterStage.FaceCapture || stage is RegisterStage.IdScanning

            if (stage !is RegisterStage.Success) {
                RegistrationOverlayActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    onClick = onBack,
                    darkBackground = useDarkOverlayAction,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 12.dp, top = 12.dp)
                )
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

@Composable
private fun RegistrationOverlayActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    darkBackground: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = if (darkBackground) Color.Black.copy(alpha = 0.34f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = if (darkBackground) 0.dp else 6.dp
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (darkBackground) Color.White else MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun PermissionRequestContentReal(
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // ── 일러스트 카드 (Intro/Guide 동일 스타일) ──────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFFE0F5F3), Color(0xFFF0FAF9))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 바깥 연한 원
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(Color(0xFFCCEAE7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // 안쪽 민트 원
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = if (isPermanentlyDenied) "카메라 권한이\n차단되었습니다" else "카메라 권한이\n필요합니다",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isPermanentlyDenied)
                "얼굴 등록과 신분증 촬영을 위해\n설정에서 카메라 권한을 허용해 주세요."
            else
                "페이스페이 등록을 위해\n카메라 권한을 허용해 주세요.",
            fontFamily = NaedaFontFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = if (isPermanentlyDenied) onOpenSettings else onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = if (isPermanentlyDenied) "설정으로 이동" else "권한 허용하기",
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
private fun IntroStageContent(onStartClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 24.dp, top = REGISTER_OVERLAY_CONTENT_TOP_PADDING, end = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "이제 오프라인에서\n얼굴 인증으로 결제하세요",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(24.dp))

        // ── 얼굴 일러스트 카드 ─────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFFE0F5F3), Color(0xFFF0FAF9))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 2겹 원 + 얼굴 아이콘
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .background(Color(0xFFCCEAE7), CircleShape),  // 바깥 연한 원
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FaceRetouchingNatural,
                                contentDescription = null,
                                tint = Color(0xFF5B9E94),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                    Text(
                        text = "지갑, 휴대폰 두고 나와도\n결제할 수 있어요",
                        fontFamily = NaedaFontFamily,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "어디서, 어떻게 사용하나요?",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        listOf("매장 기기에", "얼굴을 인식하면 결제완료!").forEachIndexed { index, step ->
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 사진과 동일한 번호 뱃지
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        fontFamily = NaedaFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = step,
                    fontFamily = NaedaFontFamily,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "페이스페이 시작하기",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "NADA PAY  •  SECURE CORE",
            fontFamily = NaedaFontFamily,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
@Composable
private fun FaceGuideStageContent(onStartClick: () -> Unit) {
    val tipItems = listOf(
        Pair(Icons.Default.Face,              "마스크나 모자를 벗어주세요"),
        Pair(Icons.Default.WbSunny,           "밝은 곳에서 촬영해주세요"),
        Pair(Icons.Default.CenterFocusStrong, "카메라를 정면으로 응시하세요")
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 24.dp, top = REGISTER_OVERLAY_CONTENT_TOP_PADDING, end = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "페이스페이 사용을 위해\n얼굴을 등록할게요.",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 32.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        // ── 얼굴 일러스트 카드 (그라데이션 배경) ──────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color(0xFFE0F5F3), Color(0xFFF0FAF9))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 점선 원 + 2겹 원 + 얼굴 아이콘
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .drawBehind {
                                val radius = size.minDimension / 2f
                                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f), 0f)
                                drawCircle(
                                    color = Color(0xFF009688),
                                    radius = radius - 2.dp.toPx(),
                                    style = Stroke(width = 2.dp.toPx(), pathEffect = dashEffect)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // 바깥 연한 원
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(Color(0xFFCCEAE7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            // 안쪽 진한 원
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .background(Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FaceRetouchingNatural,
                                    contentDescription = null,
                                    tint = Color(0xFF5B9E94),
                                    modifier = Modifier.size(58.dp)
                                )
                            }
                        }
                    }

                    // 안내 칩
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "정면을 바라봐 주세요",
                                fontFamily = NaedaFontFamily,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 등록 팁 ────────────────────────────────────────────
        tipItems.forEachIndexed { index, (icon, text) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Color.kt 기반 아이콘 배경 (SurfaceVariant), 아이콘 색(OnSurfaceVariant)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFCCEAE7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF5B9E94),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = text,
                        fontFamily = NaedaFontFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 22.sp
                    )
                }
                // 체크 뱃지 (MaterialTheme.colorScheme.primary 배경)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (index < tipItems.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onStartClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "등록 시작하기",
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
                .background(Color(0xFFCCEAE7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.FaceRetouchingNatural,
                contentDescription = null,
                tint = Color(0xFF5B9E94),
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            fontFamily = NaedaFontFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ── 상단 칩 ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "준비하기",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 제목 ─────────────────────────────────────────────────
        Text(
            text = title,
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── 신분증 아이콘 (2겹 원) ───────────────────────────────
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color(0xFFCCEAE7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── 보안 안내 섹션 ────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "신분증 안내",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = tips.joinToString("\n") { "• $it" },
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 프로그레스 인디케이터 ─────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(if (i < 2) 32.dp else 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPrimaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                        color = if (index < pinLength) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.18f),
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
    onRestartRequested: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val postureState = rememberDevicePostureState()
    var statusMessage by remember { mutableStateOf("얼굴을 화면 중앙에 맞춰주세요.") }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isUploading by remember { mutableStateOf(false) }
    var poseCompleted by remember { mutableStateOf(false) }
    var headPoseRetryCount by remember { mutableStateOf(0) }  // AI matched=false 재시도 횟수
    // 실시간 yaw/pitch → 가이드라인 애니메이션용
    var currentYaw by remember { mutableFloatStateOf(0f) }
    var currentPitch by remember { mutableFloatStateOf(0f) }
    var isDirectionMatched by remember { mutableStateOf(false) }
    val requestInFlight = remember { AtomicBoolean(false) }
    val holdStartedAt = remember { AtomicLong(0L) }
    val livenessEvaluator = remember { RegistrationPassiveLivenessEvaluator() }
    val requiredHoldMillis = remember(spec.backendPose) {
        if (spec.backendPose == "front1") 2000L else 1000L
    }
    val captureFeedbackPlayer = remember(context) {
        MediaPlayer.create(context, R.raw.kevangc_pling_sound)
    }

    DisposableEffect(captureFeedbackPlayer) {
        onDispose {
            captureFeedbackPlayer?.release()
        }
    }

    // poseCompleted가 true로 바뀌는 순간 확실히 트리거
    val onPoseSavedUpdated by rememberUpdatedState(onPoseSaved)
    LaunchedEffect(poseCompleted) {
        if (poseCompleted) {
            triggerFaceCaptureFeedback(context, captureFeedbackPlayer)
            delay(600L)
            onPoseSavedUpdated()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FaceRegistrationCameraCard(
            modifier = Modifier.fillMaxSize(),
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,
            onFaceFrame = { imageProxy, face, luminance ->
                if (requestInFlight.get() || poseCompleted) {
                    imageProxy.close()
                    return@FaceRegistrationCameraCard
                }
                // 실시간 각도 업데이트 (가이드라인용)
                currentYaw = face.headEulerAngleY
                currentPitch = face.headEulerAngleX

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
                        isDirectionMatched = false
                        statusMessage = posture.message
                        imageProxy.close(); return@FaceRegistrationCameraCard
                    }
                    luminance < 35.0 -> {
                        resetHold(holdStartedAt) { holdProgress = it }
                        isDirectionMatched = false
                        statusMessage = "너무 어둡습니다. 밝은 곳으로 이동해 주세요."
                        imageProxy.close(); return@FaceRegistrationCameraCard
                    }
                    !isCentered -> {
                        resetHold(holdStartedAt) { holdProgress = it }
                        isDirectionMatched = false
                        statusMessage = "얼굴을 가이드 중앙에 맞춰주세요."
                        imageProxy.close(); return@FaceRegistrationCameraCard
                    }
                    !isLive -> {
                        resetHold(holdStartedAt) { holdProgress = it }
                        isDirectionMatched = false
                        statusMessage = livenessEvaluator.guideText()
                        imageProxy.close(); return@FaceRegistrationCameraCard
                    }
                    !locallyMatched -> {
                        resetHold(holdStartedAt) { holdProgress = it }
                        isDirectionMatched = false
                        statusMessage = spec.instruction
                        imageProxy.close(); return@FaceRegistrationCameraCard
                    }
                }
                // 모든 조건 통과 후에만 matched = true
                isDirectionMatched = true

                val now = System.currentTimeMillis()
                if (holdStartedAt.get() == 0L) holdStartedAt.set(now)
                val elapsed = now - holdStartedAt.get()
                holdProgress = (elapsed.toFloat() / requiredHoldMillis.toFloat()).coerceIn(0f, 1f)
                statusMessage = when {
                    elapsed < requiredHoldMillis && spec.backendPose == "front1" -> "기준 얼굴을 저장하는 중입니다. 2초간 정면을 유지해주세요."
                    elapsed < requiredHoldMillis -> "현재 자세를 유지해주세요."
                    else -> "자세 확인 완료. 저장 중입니다."
                }

                if (elapsed < requiredHoldMillis) {
                    imageProxy.close(); return@FaceRegistrationCameraCard
                }

                val payload = runCatching {
                    createFaceFramePayload(imageProxy, face.boundingBox)
                }.onFailure { onError("촬영 프레임 변환에 실패했습니다: ${it.message}") }.getOrNull()
                imageProxy.close()
                if (payload == null) {
                    resetHold(holdStartedAt) { holdProgress = it }; return@FaceRegistrationCameraCard
                }

                requestInFlight.set(true)
                isUploading = true
                scope.launch(Dispatchers.IO) {
                    // ── headpose 체크 ──────────────────────────────
                    val headPoseResult = runCatching {
                        FaceRegistrationRepository.checkHeadPose(
                            expectedDirection = spec.expectedDirection,
                            imageBytes = payload.fullFrameJpeg
                        )
                    }

                    // headpose API 자체 실패 (네트워크/서버 오류)
                    val headPoseException = headPoseResult.exceptionOrNull()
                    if (headPoseException != null) {
                        withContext(Dispatchers.Main) {
                            requestInFlight.set(false)
                            isUploading = false
                            resetHold(holdStartedAt) { holdProgress = it }
                            val msg = when {
                                headPoseException is ApiRequestException -> when (headPoseException.errorCode) {
                                    "NO_FACE"        -> "얼굴이 화면 안에 오도록 맞춰주세요."
                                    "MULTIPLE_FACES" -> "한 명만 화면에 나오게 해주세요."
                                    "AI_TIMEOUT",
                                    "AI_UNAVAILABLE" -> "서버 상태를 확인 후 다시 시도해주세요."
                                    else             -> "서버 오류가 발생했습니다. 다시 시도해주세요."
                                }
                                else -> "서버 오류가 발생했습니다. 다시 시도해주세요."
                            }
                            onError(msg)
                        }
                        return@launch
                    }

                    val headPose = headPoseResult.getOrNull()!!

                    // ── matched=false → Main 스레드에서 재시도 처리 ──
                    if (!headPose.matched) {
                        withContext(Dispatchers.Main) {
                            requestInFlight.set(false)
                            isUploading = false
                            resetHold(holdStartedAt) { holdProgress = it }
                            val retry = headPoseRetryCount + 1
                            headPoseRetryCount = retry
                            statusMessage = when {
                                retry >= 5 -> {
                                    headPoseRetryCount = 0
                                    // 배너 에러 대신 상태 메시지로만 표시 (UX 방해 최소화)
                                    "정면을 더 정확히 바라봐 주세요."
                                }
                                retry >= 3 -> "각도를 조금 더 맞춰주세요. (${retry}/5)"
                                else -> spec.instruction
                            }
                        }
                        return@launch
                    }

                    // ── matched=true → 등록 진행 ──────────────────
                    val enrollResult = runCatching {
                        enrollWithFallback(spec.backendPose, payload)
                    }

                    withContext(Dispatchers.Main) {
                        requestInFlight.set(false)
                        isUploading = false
                        headPoseRetryCount = 0  // Main 스레드에서 state 수정
                        if (enrollResult.isSuccess) {
                            holdStartedAt.set(0L)
                            holdProgress = 0f
                            poseCompleted = true  // → LaunchedEffect 트리거
                        } else {
                            val throwable = enrollResult.exceptionOrNull()!!
                            resetHold(holdStartedAt) { holdProgress = it }
                            val msg = when {
                                throwable is ApiRequestException -> when (throwable.errorCode) {
                                    "NO_FACE" -> "얼굴이 화면 안에 오도록 맞춰주세요."
                                    "MULTIPLE_FACES" -> "한 명만 화면에 나오게 해주세요."
                                    "AI_TIMEOUT",
                                    "AI_UNAVAILABLE" -> "서버 상태를 확인 후 다시 시도해주세요."
                                    "REGISTRATION_QUALITY_LOW" -> if (spec.backendPose == "front1") "정면 얼굴을 더 또렷하게 2초간 유지해주세요." else "현재 자세를 더 또렷하게 유지한 뒤 다시 촬영해주세요."
                                    "REGISTRATION_MISMATCH" -> when (spec.backendPose) {
                                        "front2", "front3" -> "기준 정면 얼굴과 일치하지 않습니다. 같은 사람이 다시 정면을 촬영해주세요."
                                        else -> "기준 얼굴과 차이가 큽니다. 같은 사람이 해당 자세를 다시 촬영해주세요."
                                    }
                                    "REGISTRATION_FRONT_REQUIRED" -> {
                                        onRestartRequested()
                                        "정면 기준 얼굴이 필요합니다. 처음부터 다시 촬영해주세요."
                                    }
                                    "REGISTRATION_SESSION_EXPIRED" -> {
                                        onRestartRequested()
                                        "얼굴 등록 세션이 만료되었습니다. 처음부터 다시 진행해주세요."
                                    }
                                    else -> throwable.message ?: "얼굴 등록에 실패했습니다."
                                }
                                else -> throwable.message ?: "얼굴 등록에 실패했습니다."
                            }
                            onError(msg)
                        }
                    }
                }
            },
            onNoFace = {
                resetHold(holdStartedAt) { holdProgress = it }
                isDirectionMatched = false
                headPoseRetryCount = 0
                statusMessage = "얼굴이 화면 안에 오도록 맞춰주세요."
            },
            overlay = {
                FaceCaptureOverlay(
                    spec = spec,
                    currentIndex = currentIndex,
                    totalCount = totalCount,
                    statusMessage = statusMessage,
                    holdProgress = holdProgress,
                    isUploading = isUploading,
                    poseCompleted = poseCompleted,
                    currentYaw = currentYaw,
                    currentPitch = currentPitch,
                    isDirectionMatched = isDirectionMatched
                )
            }
        )
    }
}

@Composable
private fun FaceCaptureOverlay(
    spec: FaceCaptureSpec,
    currentIndex: Int,
    totalCount: Int,
    statusMessage: String,
    holdProgress: Float,
    isUploading: Boolean,
    poseCompleted: Boolean,
    currentYaw: Float,
    currentPitch: Float,
    isDirectionMatched: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ovalPulse")
    val ovalAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            androidx.compose.animation.core.tween(900),
            RepeatMode.Reverse
        ),
        label = "ovalAlpha"
    )

    // 가이드라인 yaw/pitch 애니메이션 (부드럽게)
    val animYaw by animateFloatAsState(
        targetValue = currentYaw,
        animationSpec = androidx.compose.animation.core.tween(80),
        label = "yaw"
    )
    val animPitch by animateFloatAsState(
        targetValue = currentPitch,
        animationSpec = androidx.compose.animation.core.tween(80),
        label = "pitch"
    )

    // 타원 색: 기본=흰색, 방향맞음=민트, 완료=초록
    val ovalColor = when {
        poseCompleted    -> Color(0xFF4CAF50)
        isDirectionMatched && holdProgress > 0f -> Color(0xFF009688)
        isDirectionMatched -> Color(0xFF44E3D3)
        else             -> Color.White.copy(alpha = ovalAlpha)
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── 어두운 오버레이 + 타원 구멍 ──────────────────────────
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val ovalW = size.width * 0.72f
            val ovalH = ovalW * 1.28f
            val cx = size.width / 2f
            val cy = size.height * 0.44f
            drawRect(Color(0xFF0E1717).copy(alpha = 0.68f))
            drawOval(
                color = Color.Transparent,
                topLeft = androidx.compose.ui.geometry.Offset(cx - ovalW / 2f, cy - ovalH / 2f),
                size = androidx.compose.ui.geometry.Size(ovalW, ovalH),
                blendMode = BlendMode.Clear
            )
        }

        // ── 타원 + 코 가이드라인 (yaw/pitch 기반 원근 변형) ─────
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val ovalW = size.width * 0.72f
            val ovalH = ovalW * 1.28f
            val cx = size.width / 2f
            val cy = size.height * 0.44f

            // yaw/pitch 정규화 (-1 ~ 1)
            // 전면 카메라 거울 모드 → yaw 부호 반전
            val yawNorm   = (-animYaw  / 45f).coerceIn(-1f, 1f)
            val pitchNorm = (animPitch / 30f).coerceIn(-1f, 1f)

            // ── 원근감 적용된 타원 크기 계산 ──────────────────
            // 좌우 회전 시 가로가 줄어들고 (cos), 위아래 회전 시 세로가 줄어듦
            val perspW = ovalW * (1f - kotlin.math.abs(yawNorm) * 0.38f)
            val perspH = ovalH * (1f - kotlin.math.abs(pitchNorm) * 0.32f)

            // 타원 중심이 회전 방향으로 살짝 이동 (입체감)
            val shiftX = yawNorm   * ovalW * 0.06f
            val shiftY = -pitchNorm * ovalH * 0.05f
            val ocx = cx + shiftX
            val ocy = cy + shiftY

            // ── 배경 점선 타원 (고정 크기 - 원근 변형 없음) ──
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
            drawOval(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = androidx.compose.ui.geometry.Offset(cx - ovalW / 2f, cy - ovalH / 2f),
                size = androidx.compose.ui.geometry.Size(ovalW, ovalH),
                style = Stroke(width = 2.5.dp.toPx(), pathEffect = dashEffect)
            )

            // ── 진행도 Arc (hold 진행에 따라 타원 테두리 채움) ─
            if (holdProgress > 0f || poseCompleted) {
                val sweepAngle = if (poseCompleted) 360f else holdProgress * 360f
                val arcColor = if (poseCompleted) Color(0xFF4CAF50) else Color(0xFF009688)

                // 글로우 (두껍고 흐릿)
                drawArc(
                    color = arcColor.copy(alpha = 0.35f),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - ovalW / 2f, cy - ovalH / 2f),
                    size = androidx.compose.ui.geometry.Size(ovalW, ovalH),
                    style = Stroke(width = 10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                // 선명한 선
                drawArc(
                    color = arcColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - ovalW / 2f, cy - ovalH / 2f),
                    size = androidx.compose.ui.geometry.Size(ovalW, ovalH),
                    style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }

            // ── 코 중심 가이드라인 ─────────────────────────────
            // 코 끝 위치: yaw에 따라 가로로, pitch에 따라 세로로 이동
            val noseX = ocx + yawNorm  * perspW * 0.28f
            val noseY = ocy - pitchNorm * perspH * 0.20f

            val lineTop    = ocy - perspH * 0.38f
            val lineBottom = ocy + perspH * 0.38f

            val mintGlow  = Color(0xFF44E3D3)
            val lineColor = if (isDirectionMatched) mintGlow else mintGlow.copy(alpha = 0.65f)

            // 고개 방향 따라 휘는 베지어 곡선
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(ocx + yawNorm * perspW * 0.07f, lineTop)
                cubicTo(
                    ocx + yawNorm * perspW * 0.16f, lineTop + (noseY - lineTop) * 0.4f,
                    noseX - yawNorm * perspW * 0.04f, noseY - perspH * 0.05f,
                    noseX, noseY
                )
                cubicTo(
                    noseX + yawNorm * perspW * 0.04f, noseY + perspH * 0.05f,
                    ocx + yawNorm * perspW * 0.16f, noseY + (lineBottom - noseY) * 0.6f,
                    ocx + yawNorm * perspW * 0.07f, lineBottom
                )
            }

            // 글로우 - 방향 맞을 때 더 강하게 빛남
            val glowAlpha  = if (isDirectionMatched) 0.45f else 0.18f
            val glowWidth  = if (isDirectionMatched) 12.dp.toPx() else 7.dp.toPx()
            val lineWidth  = if (isDirectionMatched) 3.dp.toPx() else 2.dp.toPx()
            drawPath(path = path, color = lineColor.copy(alpha = glowAlpha),
                style = Stroke(width = glowWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            // 선명한 선
            drawPath(path = path, color = lineColor,
                style = Stroke(width = lineWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round))

            // 코 끝 포인트 - 방향 맞을 때 더 크고 밝게
            val dotGlowR = if (isDirectionMatched) 14.dp.toPx() else 9.dp.toPx()
            val dotR     = if (isDirectionMatched) 5.dp.toPx()  else 3.5.dp.toPx()
            drawCircle(color = lineColor.copy(alpha = if (isDirectionMatched) 0.5f else 0.3f),
                radius = dotGlowR, center = androidx.compose.ui.geometry.Offset(noseX, noseY))
            drawCircle(color = lineColor, radius = dotR,
                center = androidx.compose.ui.geometry.Offset(noseX, noseY))
        }

        // ── 상단: STEP + 제목 ─────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 64.dp, start = 84.dp, end = 84.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.35f)
            ) {
                Text(
                    text = "STEP ${currentIndex + 1} / $totalCount",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontFamily = NaedaFontFamily,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isDirectionMatched && !poseCompleted) "현재 자세를 유지해주세요"
                else spec.instruction,
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = if (isDirectionMatched) Color(0xFF44E3D3) else Color.White,
                textAlign = TextAlign.Center
            )
            // 방향 화살표 (정면 제외)
            if (spec.localDirection != FaceCaptureDirection.FRONT && !isDirectionMatched) {
                Spacer(modifier = Modifier.height(6.dp))
                val rotation = when (spec.localDirection) {
                    FaceCaptureDirection.RIGHT -> 180f
                    FaceCaptureDirection.UP    -> 90f
                    FaceCaptureDirection.DOWN  -> 270f
                    else -> 0f
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color(0xFF44E3D3),
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }

        // ── 완료 체크 ─────────────────────────────────────────────
        if (poseCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(88.dp)
                    .background(Color(0xFF4CAF50), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // ── 하단 ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상태 메시지 칩
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDirectionMatched) Color(0xFF009688).copy(alpha = 0.85f)
                else Color.Black.copy(alpha = 0.40f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        isUploading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Text("저장 중...", color = Color.White, fontFamily = NaedaFontFamily, fontSize = 13.sp)
                        }
                        holdProgress > 0f -> {
                            CircularProgressIndicator(
                                progress = { holdProgress },
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                            Text(
                                "${(holdProgress * 100).roundToInt()}% 유지 중",
                                color = Color.White, fontFamily = NaedaFontFamily, fontSize = 13.sp
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                statusMessage,
                                color = Color.White.copy(alpha = 0.9f),
                                fontFamily = NaedaFontFamily,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 진행 도트
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                faceCaptureSequence.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentIndex) 14.dp else 8.dp)
                            .background(
                                color = when {
                                    index < currentIndex  -> MaterialTheme.colorScheme.primary
                                    index == currentIndex -> Color.White
                                    else                  -> Color.White.copy(alpha = 0.25f)
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
@Composable
private fun IdCardScanningStageContent(
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
private fun IdCaptureOverlay(
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
                    color = if (isRecognizing || isExtracting) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
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
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            CircularProgressIndicator(
                                progress = { holdProgress },
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
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
                        color = MaterialTheme.colorScheme.primary,
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
    var isLoading by remember(extracted) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 24.dp, top = REGISTER_OVERLAY_CONTENT_TOP_PADDING, end = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ── 상단 칩 ──────────────────────────────────────────────

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "인식된 정보를\n확인해 주세요",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "이름과 주민등록번호 앞 6자리, 뒤 첫 1자리만 사용합니다.",
            fontFamily = NaedaFontFamily,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── 이름 필드 ─────────────────────────────────────────────
        Text(
            text = "이름",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.material3.OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── 주민번호 필드 ─────────────────────────────────────────
        Text(
            text = "주민등록번호",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.OutlinedTextField(
                value = residentFront6,
                onValueChange = { residentFront6 = it.filter(Char::isDigit).take(6) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = {
                    Text("앞 6자리", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NaedaFontFamily, fontSize = 14.sp)
                },
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "-",
                modifier = Modifier.align(Alignment.CenterVertically),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            androidx.compose.material3.OutlinedTextField(
                value = residentBackFirst1,
                onValueChange = { residentBackFirst1 = it.filter(Char::isDigit).take(1) },
                modifier = Modifier.weight(0.5f),
                singleLine = true,
                placeholder = {
                    Text("뒤 1자리", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NaedaFontFamily, fontSize = 14.sp)
                },
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── 안내 박스 ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "입력하신 정보는 본인 확인을 위해서만 사용되며 안전하게 암호화됩니다.",
                fontFamily = NaedaFontFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (name.isBlank() || residentFront6.length != 6 || residentBackFirst1.length != 1 || isLoading) {
                    onConfirmError("OCR 확인값을 다시 확인해 주세요.")
                    return@Button
                }
                scope.launch {
                    isLoading = true
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            FaceRegistrationRepository.confirmResidentId(
                                name = name,
                                residentFront6 = residentFront6,
                                residentBackFirst1 = residentBackFirst1
                            )
                        }
                    }
                    isLoading = false
                    result.onSuccess { response ->
                        if (response.verified) {
                            onConfirmComplete()
                        } else {
                            onConfirmError(buildIdConfirmError(response))
                        }
                    }.onFailure { throwable ->
                        onConfirmError(throwable.message ?: "신분증 확인에 실패했습니다.")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(
                    "확인 및 다음",
                    fontFamily = NaedaFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

private enum class FacePaySelectableType {
    ACCOUNT,
    CARD
}

private data class FacePaySelectableMethod(
    val paymentMethodId: Long?,
    val title: String,
    val subtitle: String,
    val typeLabel: String,
    val isDefault: Boolean,
    val selectable: Boolean,
    val type: FacePaySelectableType
)

@Composable
private fun PaymentMethodSelectStageContent(
    userNo: Long,
    onSelectionComplete: (Long) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var accountItems by remember { mutableStateOf<List<FacePaySelectableMethod>>(emptyList()) }
    var cardItems by remember { mutableStateOf<List<FacePaySelectableMethod>>(emptyList()) }
    var pendingSelection by remember { mutableStateOf<FacePaySelectableMethod?>(null) }

    fun loadAssets() {
        scope.launch {
            isLoading = true
            runCatching {
                withContext(Dispatchers.IO) {
                    AssetRepository.getWalletAssets(userNo)
                }
            }.onSuccess { assets ->
                accountItems = assets.accounts.mapIndexed { index, account ->
                    account.toFacePaySelectableMethod(assets.payMethods, index)
                }
                cardItems = assets.cards
                    .filter { it.isActive != false }
                    .mapIndexed { index, card ->
                        card.toFacePaySelectableMethod(assets.payMethods, index)
                    }
                isLoading = false
            }.onFailure { throwable ->
                isLoading = false
                onError(throwable.message ?: "결제수단 목록을 불러오지 못했습니다.")
            }
        }
    }

    LaunchedEffect(userNo) {
        loadAssets()
    }

    val currentItems = if (selectedTab == 0) accountItems else cardItems

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 24.dp, top = REGISTER_OVERLAY_CONTENT_TOP_PADDING, end = 24.dp)
    ) {
        Text(
            text = "대표 결제수단을\n선택해 주세요",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "계좌 또는 카드 중 하나를 대표 결제수단으로 등록합니다. 나중에 지갑 탭에서 다시 변경할 수 있어요.",
            fontFamily = NaedaFontFamily,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            listOf("계좌", "카드").forEachIndexed { index, label ->
                val selected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable(enabled = !isLoading && !isSubmitting) { selectedTab = index }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = NaedaFontFamily,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (currentItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "선택할 수 있는 ${if (selectedTab == 0) "계좌" else "카드"}가 없어요.",
                    fontFamily = NaedaFontFamily,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(currentItems, key = { "${it.type}-${it.paymentMethodId ?: it.subtitle}" }) { item ->
                    PaymentMethodSelectCard(
                        item = item,
                        enabled = !isSubmitting,
                        onClick = {
                            if (!item.selectable || item.paymentMethodId == null) {
                                onError("선택할 수 없는 결제수단입니다.")
                            } else {
                                pendingSelection = item
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "선택 후에는 결제 한도와 PIN 2차 인증 설정이 이어집니다.",
            fontFamily = NaedaFontFamily,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
    }

    pendingSelection?.let { item ->
        AlertDialog(
            onDismissRequest = {
                if (!isSubmitting) pendingSelection = null
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paymentMethodId = item.paymentMethodId ?: return@Button
                        pendingSelection = null
                        onSelectionComplete(paymentMethodId)
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "선택",
                        fontFamily = NaedaFontFamily,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingSelection = null },
                    enabled = !isSubmitting
                ) {
                    Text("취소", fontFamily = NaedaFontFamily, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            title = {
                Text(
                    text = "대표 결제수단으로 설정할까요?",
                    fontFamily = NaedaFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.title,
                        fontFamily = NaedaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = item.subtitle,
                        fontFamily = NaedaFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun PaymentMethodSelectCard(
    item: FacePaySelectableMethod,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val alpha = if (item.selectable) 1f else 0.55f
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = alpha)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled && item.selectable, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (item.isDefault) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (item.type == FacePaySelectableType.ACCOUNT) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFFF3D8)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.type == FacePaySelectableType.ACCOUNT) Icons.Default.SwapVert else Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = if (item.type == FacePaySelectableType.ACCOUNT) MaterialTheme.colorScheme.primary else Color(0xFFCC8B00),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        fontFamily = NaedaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (item.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "현재 대표",
                                fontFamily = NaedaFontFamily,
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.subtitle,
                    fontFamily = NaedaFontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.typeLabel,
                    fontFamily = NaedaFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!item.selectable) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "선택 불가",
                        fontFamily = NaedaFontFamily,
                        fontSize = 11.sp,
                        color = Error
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentLimitSetupStageContent(
    userNo: Long,
    onSaveComplete: (PendingPayLimit) -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var dailyLimitInput by remember { mutableStateOf("") }
    var singleLimitInput by remember { mutableStateOf("") }
    var monthlyLimit by remember { mutableStateOf(0L) }

    LaunchedEffect(userNo) {
        isLoading = true
        runCatching {
            withContext(Dispatchers.IO) {
                FaceRegistrationRepository.getPayLimit(userNo)
            }
        }.onSuccess { response ->
            dailyLimitInput = response.dailyLimit.toString()
            singleLimitInput = response.singleTransactionLimit.toString()
            monthlyLimit = response.monthlyLimit
            isLoading = false
        }.onFailure { throwable ->
            isLoading = false
            onError(throwable.message ?: "결제 한도를 불러오지 못했습니다.")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 24.dp, top = REGISTER_OVERLAY_CONTENT_TOP_PADDING, end = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "결제 한도를\n설정해 주세요",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "대표 결제수단으로 사용할 때 적용될 1일 한도와 1회 한도를 설정합니다.",
            fontFamily = NaedaFontFamily,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LimitInputField(
                label = "1일 한도",
                value = dailyLimitInput,
                placeholder = "예: 300000",
                onValueChange = { dailyLimitInput = it.filter(Char::isDigit).take(11) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            LimitInputField(
                label = "1회 한도",
                value = singleLimitInput,
                placeholder = "예: 100000",
                onValueChange = { singleLimitInput = it.filter(Char::isDigit).take(11) }
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "월 한도는 현재 설정값 ${formatWon(monthlyLimit)}을 유지합니다.",
                    fontFamily = NaedaFontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val dailyLimit = dailyLimitInput.toLongOrNull()
                    val singleLimit = singleLimitInput.toLongOrNull()

                    if (dailyLimit == null || singleLimit == null || dailyLimit <= 0L || singleLimit <= 0L) {
                        onError("1일 한도와 1회 한도를 모두 올바르게 입력해 주세요.")
                        return@Button
                    }
                    if (singleLimit > dailyLimit) {
                        onError("1회 한도는 1일 한도보다 클 수 없습니다.")
                        return@Button
                    }
                    if (isSaving) {
                        return@Button
                    }

                    isSaving = true
                    scope.launch {
                        isSaving = false
                        onSaveComplete(
                            PendingPayLimit(
                                dailyLimit = dailyLimit,
                                monthlyLimit = max(monthlyLimit, dailyLimit),
                                singleTransactionLimit = singleLimit
                            )
                        )
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (isSaving) "한도 저장 중..." else "한도 저장하고 다음",
                    fontFamily = NaedaFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LimitInputField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Text(
        text = label,
        fontFamily = NaedaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NaedaFontFamily,
                fontSize = 14.sp
            )
        },
        trailingIcon = {
            Text(
                text = "원",
                fontFamily = NaedaFontFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

private fun AssetAccountResponse.toFacePaySelectableMethod(
    payMethods: List<AssetPayMethodResponse>,
    index: Int
): FacePaySelectableMethod {
    val payMethod = payMethods.firstOrNull { method ->
        method.isActive != false &&
            method.methodType == "ACCOUNT" &&
            method.accountId != null &&
            method.accountId == accountId
    }
    val resolvedBankName = bankName.orEmpty().ifBlank { "내 계좌" }
    val resolvedAccountName = accountName.orEmpty().ifBlank { resolvedBankName }
    val resolvedAccountNo = accountNo.orEmpty().ifBlank { "계좌번호 없음" }

    return FacePaySelectableMethod(
        paymentMethodId = payMethod?.paymentMethodId,
        title = resolvedBankName,
        subtitle = "$resolvedAccountName · $resolvedAccountNo",
        typeLabel = "계좌",
        isDefault = payMethod?.isDefault == true,
        selectable = payMethod?.paymentMethodId != null,
        type = FacePaySelectableType.ACCOUNT
    )
}

private fun AssetCardResponse.toFacePaySelectableMethod(
    payMethods: List<AssetPayMethodResponse>,
    index: Int
): FacePaySelectableMethod {
    val resolvedType = cardType.orEmpty().uppercase().ifBlank { "DEBIT" }
    val payMethod = payMethods.firstOrNull { method ->
        method.isActive != false && when (resolvedType) {
            "CREDIT" -> method.methodType == "CREDIT_CARD" && method.creditCardId == cardId
            else -> method.methodType == "DEBIT_CARD" && method.debitCardId == cardId
        }
    }
    val resolvedIssuer = cardIssuerName.orEmpty().ifBlank { "등록 카드" }
    val resolvedCardName = cardName.orEmpty().ifBlank { resolvedIssuer }

    return FacePaySelectableMethod(
        paymentMethodId = payMethod?.paymentMethodId,
        title = resolvedIssuer,
        subtitle = "$resolvedCardName · ${maskFacePayCardNumber(cardNo)}",
        typeLabel = if (resolvedType == "CREDIT") "신용" else "체크",
        isDefault = payMethod?.isDefault == true,
        selectable = payMethod?.paymentMethodId != null,
        type = FacePaySelectableType.CARD
    )
}

private fun maskFacePayCardNumber(raw: String?): String {
    val value = raw.orEmpty().trim()
    if (value.isBlank()) return "카드번호 없음"

    val normalized = value.replace("-", "")
    return if (normalized.length == 12 && normalized.contains("****")) {
        val first = normalized.take(4)
        val last = normalized.takeLast(4)
        "$first-****-****-$last"
    } else if (normalized.length >= 16 && normalized.all { it.isDigit() || it == '*' }) {
        normalized.chunked(4).joinToString("-")
    } else {
        value
    }
}

private fun formatWon(amount: Long): String {
    return "%,d원".format(amount)
}
@Composable
private fun PinChoiceStageContent(
    isSaving: Boolean,
    onUsePin: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ── 상단 칩 ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "보안 설정",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "PIN 번호 2차 인증을\n사용할까요?",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── 자물쇠 아이콘 (2겹 원) ───────────────────────────────
        Box(
            modifier = Modifier
                .size(190.dp)
                .background(Color(0xFFCCEAE7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── 보안 안내 ─────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "보안 강화 안내",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "2차 인증을 사용하면 보안이 더욱 강력해집니다.\n결제 시 얼굴 인식 후 PIN 번호를 한 번 더 입력하여\n안전하게 보호하세요.",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onUsePin,
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                if (isSaving) "설정 저장 중..." else "2차 인증 사용하기",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "다음에 하기",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(enabled = !isSaving) { onSkip() }
                    .padding(vertical = 12.dp, horizontal = 24.dp)
            )
        }
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
    resetKey: Int,
    isSaving: Boolean,
    onCurrentPinEntered: (String) -> Unit
) {
    var currentPin by remember(resetKey) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        // ── 상단 칩 ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "PIN 인증",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "현재 PIN 번호를\n입력해 주세요",
            fontFamily = NaedaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "페이스페이 2차 인증을 사용하려면\n현재 계정 PIN 확인이 필요합니다.",
            fontFamily = NaedaFontFamily,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ── PIN 도트 ─────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(6) { index ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(
                            color = if (index < currentPin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                )
            }
        }

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
            textColor = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun SavingStageContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "saving")
    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "dot1"
    )
    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse),
        label = "dot2"
    )
    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse),
        label = "dot3"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(52.dp),
                        strokeWidth = 3.dp,
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "페이스페이 등록 중",
                    fontFamily = NaedaFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(".", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = dotAlpha1), fontWeight = FontWeight.Bold)
                    Text(".", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = dotAlpha2), fontWeight = FontWeight.Bold)
                    Text(".", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary.copy(alpha = dotAlpha3), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "잠시만 기다려 주세요",
                    fontFamily = NaedaFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SuccessStageContent(
    secondaryAuthEnabled: Boolean,
    onComplete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color(0xFFCCEAE7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FaceRetouchingNatural,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "페이스페이 등록이 완료되었습니다",
                fontFamily = NaedaFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (secondaryAuthEnabled) "얼굴 등록과 신분증 확인이 완료되었고 PIN 2차 인증 사용도 저장되었습니다." else "얼굴 등록과 신분증 확인이 완료되었습니다. PIN 2차 인증은 사용 안 함으로 저장되었습니다.",
                fontFamily = NaedaFontFamily,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    "홈으로 이동",
                    fontFamily = NaedaFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
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

private fun triggerFaceCaptureFeedback(
    context: Context,
    mediaPlayer: MediaPlayer?
) {
    mediaPlayer?.let { player ->
        runCatching {
            if (player.isPlaying) {
                player.pause()
            }
            player.seekTo(0)
            player.start()
        }
    }

    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    if (vibrator?.hasVibrator() != true) {
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(40L, 80))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(40L)
    }
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
