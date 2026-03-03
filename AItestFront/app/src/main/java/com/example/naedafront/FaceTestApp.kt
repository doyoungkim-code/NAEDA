package com.example.naedafront

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.max

private const val AI_BASE_URL = "http://10.180.31.87:8000"

private const val DARKNESS_THRESHOLD = 65.0

private val JPEG_MEDIA_TYPE = "image/jpeg".toMediaType()

private enum class CapturePose(val apiValue: String, val label: String) {
    FRONT("front", "정면"),
    LEFT("left", "좌"),
    RIGHT("right", "우"),
    UP("up", "상"),
    DOWN("down", "하"),
}

private data class RegisterStep(
    val pose: CapturePose,
    val progressLabel: String,
    val guideMessage: String,
)

private sealed interface AppScreen {
    data object Home : AppScreen
    data class Register(val userId: String) : AppScreen
    data object Recognize : AppScreen
    data class Result(
        val userId: String,
        val similarity: Double,
        val threshold: Double,
        val poseScores: List<PoseScore>,
    ) : AppScreen
}

private data class PoseScore(
    val pose: String,
    val similarity: Double?,
)

private data class RecognizeCandidate(
    val userId: String,
    val pose: String,
    val similarity: Double,
)

private data class RecognizeApiResponse(
    val matched: Boolean,
    val bestUserId: String?,
    val similarity: Double?,
    val threshold: Double,
    val topCandidates: List<RecognizeCandidate>,
)

@Composable
fun FaceTestApp() {
    val apiClient = remember { FaceApiClient(AI_BASE_URL) }
    var inputName by rememberSaveable { mutableStateOf("") }
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val current = screen) {
            AppScreen.Home -> HomeScreen(
                inputName = inputName,
                onNameChange = { inputName = it },
                onRegisterClick = {
                    val normalized = inputName.trim()
                    if (normalized.isNotEmpty()) {
                        screen = AppScreen.Register(normalized)
                    }
                },
                onRecognizeClick = { screen = AppScreen.Recognize },
            )

            is AppScreen.Register -> RegisterScreen(
                userId = current.userId,
                apiClient = apiClient,
                onBack = { screen = AppScreen.Home },
                onComplete = { screen = AppScreen.Home },
            )

            AppScreen.Recognize -> RecognizeScreen(
                apiClient = apiClient,
                onBack = { screen = AppScreen.Home },
                onRecognized = { userId, similarity, threshold, poseScores ->
                    screen = AppScreen.Result(
                        userId = userId,
                        similarity = similarity,
                        threshold = threshold,
                        poseScores = poseScores,
                    )
                },
            )

            is AppScreen.Result -> ResultScreen(
                userId = current.userId,
                similarity = current.similarity,
                threshold = current.threshold,
                poseScores = current.poseScores,
                onBack = { screen = AppScreen.Home },
            )
        }
    }
}

@Composable
private fun HomeScreen(
    inputName: String,
    onNameChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onRecognizeClick: () -> Unit,
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "AI 얼굴 테스트",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = inputName,
                onValueChange = {
                    onNameChange(it)
                    errorMessage = null
                },
                label = { Text("이름") },
                singleLine = true,
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (inputName.trim().isBlank()) {
                        errorMessage = "이름을 입력해주세요."
                        return@Button
                    }
                    onRegisterClick()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("얼굴 등록하기")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRecognizeClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("얼굴 찾기")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "서버: $AI_BASE_URL",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RegisterScreen(
    userId: String,
    apiClient: FaceApiClient,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val captureSteps = remember {
        listOf(
            RegisterStep(CapturePose.FRONT, "정면1", "정면을 바라봐주세요. (1/3)"),
            RegisterStep(CapturePose.FRONT, "정면2", "정면을 바라봐주세요. (2/3)"),
            RegisterStep(CapturePose.FRONT, "정면3", "정면을 바라봐주세요. (3/3)"),
            RegisterStep(CapturePose.LEFT, "좌", "얼굴을 좌측으로 돌려주세요."),
            RegisterStep(CapturePose.RIGHT, "우", "얼굴을 우측으로 돌려주세요."),
            RegisterStep(CapturePose.UP, "상", "얼굴을 위로 들어주세요."),
            RegisterStep(CapturePose.DOWN, "하", "얼굴을 아래로 내려주세요."),
        )
    }
    val completed = remember {
        mutableStateListOf<Boolean>().apply {
            repeat(captureSteps.size) { add(false) }
        }
    }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentIndexRef = remember { AtomicInteger(0) }
    val sideSignRef = remember { AtomicReference<Int?>(null) }
    val verticalSignRef = remember { AtomicReference<Int?>(null) }
    val finishedRef = remember { AtomicBoolean(false) }
    val savingRef = remember { AtomicBoolean(false) }
    val poseHoldStepRef = remember { AtomicInteger(-1) }
    val poseHoldStartMsRef = remember { AtomicLong(0L) }

    var statusMessage by remember { mutableStateOf(captureSteps[0].guideMessage) }
    val lastStatusRef = remember { AtomicReference(statusMessage) }
    val scope = rememberCoroutineScope()

    fun postStatus(message: String) {
        if (lastStatusRef.getAndSet(message) == message) return
        scope.launch { statusMessage = message }
    }

    fun resetPoseHold() {
        poseHoldStepRef.set(-1)
        poseHoldStartMsRef.set(0L)
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "얼굴 등록",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Button(onClick = onBack) {
                    Text("뒤로가기")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDF3D8), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "안경/마스크 등 악세사리를 제거하고 밝은 환경에서 촬영해주세요.",
                    color = Color(0xFF5A4200),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("등록 대상: $userId")
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "진행: " + captureSteps.mapIndexed { index, step ->
                    if (completed[index]) "${step.progressLabel}✓" else step.progressLabel
                }.joinToString("  ")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black, RoundedCornerShape(12.dp)),
            ) {
                CameraPermissionGate {
                    FaceCameraView(
                        modifier = Modifier.fillMaxSize(),
                        onFrame = { imageProxy, face, luminance ->
                            if (finishedRef.get()) return@FaceCameraView

                            val index = currentIndexRef.get()
                            if (index !in captureSteps.indices) return@FaceCameraView
                            if (savingRef.get()) return@FaceCameraView

                            val expectedStep = captureSteps[index]
                            val expectedPose = expectedStep.pose
                            if (face == null) {
                                resetPoseHold()
                                postStatus("얼굴이 프레임 안에 오도록 맞춰주세요.")
                                return@FaceCameraView
                            }

                            if (luminance < DARKNESS_THRESHOLD) {
                                resetPoseHold()
                                postStatus("밝은곳에서 촬영해주세요.")
                                return@FaceCameraView
                            }

                            if (isMaskSuspected(face)) {
                                resetPoseHold()
                                postStatus("마스크를 제거해주세요.")
                                return@FaceCameraView
                            }
                            if (isGlassesSuspected(face)) {
                                resetPoseHold()
                                postStatus("안경을 벗어주세요.")
                                return@FaceCameraView
                            }

                            val matched = isExpectedPose(
                                pose = expectedPose,
                                face = face,
                                sideSign = sideSignRef.get(),
                                verticalSign = verticalSignRef.get(),
                            )
                            if (!matched) {
                                resetPoseHold()
                                postStatus(expectedStep.guideMessage)
                                return@FaceCameraView
                            }

                            val nowMs = SystemClock.elapsedRealtime()
                            if (poseHoldStepRef.get() != index) {
                                poseHoldStepRef.set(index)
                                poseHoldStartMsRef.set(nowMs)
                                postStatus("${expectedStep.progressLabel} 자세를 0.5초 유지해주세요.")
                                return@FaceCameraView
                            }
                            if (nowMs - poseHoldStartMsRef.get() < 500L) {
                                postStatus("${expectedStep.progressLabel} 자세를 0.5초 유지해주세요.")
                                return@FaceCameraView
                            }

                            if (!savingRef.compareAndSet(false, true)) return@FaceCameraView
                            resetPoseHold()
                            val jpegBytes = runCatching { imageProxyToJpegBytes(imageProxy) }
                                .getOrElse {
                                    savingRef.set(false)
                                    resetPoseHold()
                                    postStatus("촬영 실패. 자세를 유지하고 다시 시도해주세요.")
                                    return@FaceCameraView
                                }
                            postStatus("${expectedStep.progressLabel} 촬영/저장 중...")

                            val yaw = face.headEulerAngleY
                            val pitch = face.headEulerAngleX
                            scope.launch {
                                val registerResult = apiClient.registerPose(
                                    userId = userId,
                                    pose = expectedPose.apiValue,
                                    jpegBytes = jpegBytes,
                                )
                                savingRef.set(false)

                                registerResult.onSuccess {
                                    completed[index] = true
                                    if (expectedPose == CapturePose.LEFT) {
                                        sideSignRef.set(signFor(yaw))
                                    }
                                    if (expectedPose == CapturePose.UP) {
                                        verticalSignRef.set(signFor(pitch))
                                    }

                                    if (index == captureSteps.lastIndex) {
                                        finishedRef.set(true)
                                        postStatus("얼굴 데이터가 저장되었습니다.")
                                        delay(900)
                                        onComplete()
                                    } else {
                                        val nextIndex = index + 1
                                        currentIndex = nextIndex
                                        currentIndexRef.set(nextIndex)
                                        postStatus(captureSteps[nextIndex].guideMessage)
                                    }
                                }.onFailure { error ->
                                    resetPoseHold()
                                    postStatus(error.message ?: "저장 실패. 자세를 유지하고 다시 시도해주세요.")
                                }
                            }
                        },
                        onError = { postStatus(it) },
                    )
                }
                RegisterFaceGuideOverlay(
                    pose = captureSteps.getOrElse(currentIndex) { captureSteps.last() }.pose,
                    modifier = Modifier.fillMaxSize(),
                )

                Text(
                    text = statusMessage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0xB3000000))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun RegisterFaceGuideOverlay(
    pose: CapturePose,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.padding(16.dp)) {
        val baseDiameter = min(size.width, size.height) * 0.88f
        val faceOvalWidth = baseDiameter * 0.86f
        val faceOvalHeight = baseDiameter * 1.24f
        val centerX = size.width * 0.5f
        val centerY = size.height * 0.5f
        val radiusX = faceOvalWidth * 0.5f
        val radiusY = faceOvalHeight * 0.5f
        val ovalTopLeft = Offset(centerX - radiusX, centerY - radiusY)
        val ovalSize = Size(faceOvalWidth, faceOvalHeight)

        val stroke = Stroke(
            width = 4f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 12f)),
        )
        drawOval(
            color = Color.White.copy(alpha = 0.95f),
            topLeft = ovalTopLeft,
            size = ovalSize,
            style = stroke,
        )

        val horizontalStart = Offset(centerX - radiusX, centerY)
        val horizontalEnd = Offset(centerX + radiusX, centerY)
        val verticalStart = Offset(centerX, centerY - radiusY)
        val verticalEnd = Offset(centerX, centerY + radiusY)

        val (curveX, curveY) = crossCurveControlOffset(pose, radiusX, radiusY)
        val control = Offset(centerX + curveX, centerY + curveY)

        drawPath(
            path = Path().apply {
                moveTo(horizontalStart.x, horizontalStart.y)
                quadraticTo(control.x, control.y, horizontalEnd.x, horizontalEnd.y)
            },
            color = Color(0xFF80D8FF),
            style = Stroke(width = 5f),
        )
        drawPath(
            path = Path().apply {
                moveTo(verticalStart.x, verticalStart.y)
                quadraticTo(control.x, control.y, verticalEnd.x, verticalEnd.y)
            },
            color = Color(0xFF80D8FF),
            style = Stroke(width = 5f),
        )
    }
}

private fun crossCurveControlOffset(
    pose: CapturePose,
    radiusX: Float,
    radiusY: Float,
): Pair<Float, Float> = when (pose) {
    CapturePose.FRONT -> 0f to 0f
    CapturePose.LEFT -> -(radiusX * 0.28f) to 0f
    CapturePose.RIGHT -> (radiusX * 0.28f) to 0f
    CapturePose.UP -> 0f to -(radiusY * 0.24f)
    CapturePose.DOWN -> 0f to (radiusY * 0.24f)
}

@Composable
private fun RecognizeScreen(
    apiClient: FaceApiClient,
    onBack: () -> Unit,
    onRecognized: (
        userId: String,
        similarity: Double,
        threshold: Double,
        poseScores: List<PoseScore>,
    ) -> Unit,
) {
    val evaluator = remember { PassiveLivenessEvaluator() }
    val recognizedRef = remember { AtomicBoolean(false) }
    val livenessPassedRef = remember { AtomicBoolean(false) }
    val inFlightRequests = remember { AtomicInteger(0) }
    val lastRequestAtRef = remember { AtomicLong(0L) }
    val bestSimilarityRef = remember { AtomicReference<Double?>(null) }

    var statusMessage by remember {
        mutableStateOf("실제 인물 확인 중입니다. 정면에서 자연스럽게 눈을 깜빡여주세요.")
    }
    val lastStatusRef = remember { AtomicReference(statusMessage) }
    var livenessPassed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun postStatus(message: String) {
        if (lastStatusRef.getAndSet(message) == message) return
        scope.launch { statusMessage = message }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "얼굴 찾기",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Button(onClick = onBack) {
                    Text("뒤로가기")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black, RoundedCornerShape(12.dp)),
            ) {
                CameraPermissionGate {
                    FaceCameraView(
                        modifier = Modifier.fillMaxSize(),
                        onFrame = { imageProxy, face, luminance ->
                            if (recognizedRef.get()) return@FaceCameraView

                            if (face == null) {
                                if (!livenessPassedRef.get()) evaluator.reset()
                                postStatus("얼굴이 프레임 안에 오도록 맞춰주세요.")
                                return@FaceCameraView
                            }

                            if (luminance < DARKNESS_THRESHOLD) {
                                postStatus("밝은곳에서 촬영해주세요.")
                                return@FaceCameraView
                            }

                            val frontLike = abs(face.headEulerAngleX) < 20f && abs(face.headEulerAngleY) < 20f
                            if (frontLike && isMaskSuspected(face)) {
                                postStatus("마스크를 제거해주세요.")
                                return@FaceCameraView
                            }

                            if (!livenessPassedRef.get()) {
                                val passed = evaluator.observe(face)
                                postStatus(evaluator.guideText())
                                if (passed) {
                                    livenessPassedRef.set(true)
                                    scope.launch { livenessPassed = true }
                                    postStatus("실제 인물 확인 완료. 얼굴 비교 중...")
                                }
                                return@FaceCameraView
                            }

                            val now = SystemClock.elapsedRealtime()
                            if (now - lastRequestAtRef.get() < 100L) return@FaceCameraView
                            if (inFlightRequests.get() >= 2) return@FaceCameraView

                            val jpegBytes = runCatching { imageProxyToJpegBytes(imageProxy) }
                                .getOrElse {
                                    postStatus("촬영 실패. 얼굴을 정면으로 유지해주세요.")
                                    return@FaceCameraView
                                }
                            lastRequestAtRef.set(now)
                            inFlightRequests.incrementAndGet()

                            scope.launch {
                                val recognizeResult = apiClient.recognize(jpegBytes, topK = 20)
                                inFlightRequests.decrementAndGet()

                                recognizeResult.onSuccess { response ->
                                    if (response.matched && response.bestUserId != null && response.similarity != null) {
                                        if (recognizedRef.compareAndSet(false, true)) {
                                            val poseScores = buildPoseScoresForUser(
                                                userId = response.bestUserId,
                                                candidates = response.topCandidates,
                                            )
                                            onRecognized(
                                                response.bestUserId,
                                                response.similarity,
                                                response.threshold,
                                                poseScores,
                                            )
                                        }
                                    } else {
                                        response.similarity?.let { similarity ->
                                            val previous = bestSimilarityRef.get()
                                            if (previous == null || similarity > previous) {
                                                bestSimilarityRef.set(similarity)
                                            }
                                        }
                                        val best = bestSimilarityRef.get()
                                        val bestText = best?.let { formatScore(it) } ?: "-"
                                        postStatus(
                                            "실시간 비교 중... (최고 유사도 $bestText / 기준 ${formatScore(response.threshold)})"
                                        )
                                    }
                                }.onFailure { error ->
                                    postStatus(error.message ?: "인식 요청 실패. 다시 시도해주세요.")
                                }
                            }
                        },
                        onError = { postStatus(it) },
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0xB3000000))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = statusMessage,
                        color = Color.White,
                    )
                    if (livenessPassed) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Passive Liveness: 통과 / 비교 속도: 10fps(최대)",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(
    userId: String,
    similarity: Double,
    threshold: Double,
    poseScores: List<PoseScore>,
    onBack: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "인식 결과",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(text = "이름: $userId")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "유사도: ${formatScore(similarity)}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "기준값(threshold): ${formatScore(threshold)}")
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "보조 벡터 유사도",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            poseScores.forEach { poseScore ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "${poseLabel(poseScore.pose)} (${poseScore.pose})")
                    Text(text = formatNullableScore(poseScore.similarity))
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("뒤로가기")
            }
        }
    }
}

@Composable
private fun CameraPermissionGate(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        content()
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("카메라 권한이 필요합니다.")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("권한 허용")
            }
        }
    }
}

@Composable
private fun FaceCameraView(
    modifier: Modifier = Modifier,
    onFrame: (imageProxy: ImageProxy, face: Face?, luminance: Double) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val updatedOnFrame by rememberUpdatedState(onFrame)
    val updatedOnError by rememberUpdatedState(onError)
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .enableTracking()
                .build()
        )
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzerBusy = remember { AtomicBoolean(false) }
    val controller = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        }
    }

    LaunchedEffect(controller, detector) {
        controller.setImageAnalysisAnalyzer(analysisExecutor) { imageProxy ->
            if (!analyzerBusy.compareAndSet(false, true)) {
                imageProxy.close()
                return@setImageAnalysisAnalyzer
            }

            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                analyzerBusy.set(false)
                return@setImageAnalysisAnalyzer
            }

            val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val luminance = estimateLuminance(imageProxy)
            try {
                val faces = Tasks.await(detector.process(input), 2, TimeUnit.SECONDS)
                val largestFace = faces.maxByOrNull { face ->
                    face.boundingBox.width() * face.boundingBox.height()
                }
                updatedOnFrame(imageProxy, largestFace, luminance)
            } catch (_: TimeoutException) {
                updatedOnError("얼굴 분석 지연. 다시 시도해주세요.")
            } catch (error: Exception) {
                updatedOnError(error.message ?: "얼굴 분석 실패")
            } finally {
                imageProxy.close()
                analyzerBusy.set(false)
            }
        }
    }

    DisposableEffect(lifecycleOwner, controller) {
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            detector.close()
            analysisExecutor.shutdownNow()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { previewContext ->
            PreviewView(previewContext).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
    )
}

private class PassiveLivenessEvaluator {
    private var frameCount = 0
    private var movementScore = 0f
    private var lastX = Float.NaN
    private var lastY = Float.NaN
    private var yawMin = Float.POSITIVE_INFINITY
    private var yawMax = Float.NEGATIVE_INFINITY
    private var pitchMin = Float.POSITIVE_INFINITY
    private var pitchMax = Float.NEGATIVE_INFINITY
    private var blinkState = 0
    private var blinkDetected = false
    private var guide = "실제 인물 확인 중입니다. 정면에서 자연스럽게 눈을 깜빡여주세요."

    fun reset() {
        frameCount = 0
        movementScore = 0f
        lastX = Float.NaN
        lastY = Float.NaN
        yawMin = Float.POSITIVE_INFINITY
        yawMax = Float.NEGATIVE_INFINITY
        pitchMin = Float.POSITIVE_INFINITY
        pitchMax = Float.NEGATIVE_INFINITY
        blinkState = 0
        blinkDetected = false
        guide = "실제 인물 확인 중입니다. 정면에서 자연스럽게 눈을 깜빡여주세요."
    }

    fun observe(face: Face): Boolean {
        frameCount += 1

        val centerX = face.boundingBox.exactCenterX()
        val centerY = face.boundingBox.exactCenterY()
        if (!lastX.isNaN() && !lastY.isNaN()) {
            movementScore += hypot(centerX - lastX, centerY - lastY)
        }
        lastX = centerX
        lastY = centerY

        val yaw = face.headEulerAngleY
        val pitch = face.headEulerAngleX
        yawMin = min(yawMin, yaw)
        yawMax = max(yawMax, yaw)
        pitchMin = min(pitchMin, pitch)
        pitchMax = max(pitchMax, pitch)

        val leftEye = face.leftEyeOpenProbability ?: -1f
        val rightEye = face.rightEyeOpenProbability ?: -1f
        val eyeOpen = if (leftEye >= 0f && rightEye >= 0f) {
            (leftEye + rightEye) / 2f
        } else {
            -1f
        }

        if (eyeOpen >= 0f) {
            when (blinkState) {
                0 -> if (eyeOpen > 0.65f) blinkState = 1
                1 -> if (eyeOpen < 0.30f) blinkState = 2
                2 -> if (eyeOpen > 0.65f) {
                    blinkDetected = true
                    blinkState = 3
                }
            }
        }

        val frameEnough = frameCount >= 15
        val movementEnough = movementScore >= 40f
        val angleEnough = (yawMax - yawMin) >= 8f || (pitchMax - pitchMin) >= 8f
        val passed = frameEnough && movementEnough && (blinkDetected || angleEnough)

        guide = when {
            !frameEnough -> "실제 인물 확인 중입니다. 카메라를 정면으로 봐주세요."
            !movementEnough -> "실제 인물 확인 중입니다. 얼굴을 자연스럽게 움직여주세요."
            !blinkDetected && !angleEnough -> "실제 인물 확인 중입니다. 눈을 깜빡이거나 고개를 살짝 움직여주세요."
            else -> "실제 인물 확인 완료. 얼굴 비교 중..."
        }

        return passed
    }

    fun guideText(): String = guide
}

private class FaceApiClient(baseUrl: String) {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun registerPose(
        userId: String,
        pose: String,
        jpegBytes: ByteArray,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("user_id", userId)
                .addFormDataPart("pose", pose)
                .addFormDataPart(
                    "image",
                    "${userId}_${pose}.jpg",
                    jpegBytes.toRequestBody(JPEG_MEDIA_TYPE),
                )
                .build()

            val request = Request.Builder()
                .url("$normalizedBaseUrl/faces/register")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val fallback = "등록 실패(${response.code})"
                    val errorText = parseApiError(response.body?.string().orEmpty(), fallback)
                    throw IOException(errorText)
                }
            }
        }
    }

    suspend fun recognize(
        jpegBytes: ByteArray,
        topK: Int,
    ): Result<RecognizeApiResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    "probe.jpg",
                    jpegBytes.toRequestBody(JPEG_MEDIA_TYPE),
                )
                .addFormDataPart("top_k", topK.toString())
                .build()

            val request = Request.Builder()
                .url("$normalizedBaseUrl/faces/recognize")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val fallback = "인식 실패(${response.code})"
                    val errorText = parseApiError(bodyText, fallback)
                    throw IOException(errorText)
                }

                val json = JSONObject(bodyText)
                val similarity = if (json.isNull("similarity")) null else json.optDouble("similarity")
                val bestUserId = if (json.isNull("best_user_id")) null else json.optString("best_user_id")
                val topCandidates = parseTopCandidates(json)
                RecognizeApiResponse(
                    matched = json.optBoolean("matched", false),
                    bestUserId = bestUserId,
                    similarity = similarity,
                    threshold = json.optDouble("threshold", 0.0),
                    topCandidates = topCandidates,
                )
            }
        }
    }

    private fun parseApiError(body: String, fallback: String): String {
        if (body.isBlank()) return fallback
        return runCatching {
            val json = JSONObject(body)
            val detail = json.opt("detail")
            when (detail) {
                is String -> detail
                else -> fallback
            }
        }.getOrDefault(fallback)
    }

    private fun parseTopCandidates(json: JSONObject): List<RecognizeCandidate> {
        val array = json.optJSONArray("top_k") ?: return emptyList()
        val results = mutableListOf<RecognizeCandidate>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val userId = item.optString("user_id", "")
            val pose = item.optString("pose", "")
            if (userId.isBlank() || pose.isBlank()) continue
            val similarity = item.optDouble("similarity", Double.NaN)
            if (similarity.isNaN()) continue
            results += RecognizeCandidate(
                userId = userId,
                pose = pose,
                similarity = similarity,
            )
        }
        return results
    }
}

private fun isExpectedPose(
    pose: CapturePose,
    face: Face,
    sideSign: Int?,
    verticalSign: Int?,
): Boolean {
    val yaw = face.headEulerAngleY
    val pitch = face.headEulerAngleX
    return when (pose) {
        CapturePose.FRONT -> abs(yaw) <= 10f && abs(pitch) <= 10f
        CapturePose.LEFT -> abs(yaw) >= 18f
        CapturePose.RIGHT -> {
            if (sideSign == null) {
                abs(yaw) >= 18f
            } else {
                yaw * sideSign <= -18f
            }
        }
        CapturePose.UP -> abs(pitch) >= 12f
        CapturePose.DOWN -> {
            if (verticalSign == null) {
                abs(pitch) >= 12f
            } else {
                pitch * verticalSign <= -12f
            }
        }
    }
}

private fun buildPoseScoresForUser(
    userId: String,
    candidates: List<RecognizeCandidate>,
): List<PoseScore> {
    val targetCandidates = candidates.filter { it.userId == userId }
    val poseOrder = listOf("front", "left", "right", "up", "down")
    val bestByPose = targetCandidates.groupBy { normalizePoseKey(it.pose) }
        .mapValues { (_, items) -> items.maxOfOrNull { it.similarity } }

    return poseOrder.map { pose ->
        PoseScore(
            pose = pose,
            similarity = bestByPose[pose],
        )
    }
}

private fun normalizePoseKey(pose: String): String {
    val key = pose.lowercase(Locale.US)
    return if (key.startsWith("front")) "front" else key
}

private fun poseLabel(pose: String): String = when (normalizePoseKey(pose)) {
    "front" -> "정면 벡터"
    "left" -> "좌 보조 벡터"
    "right" -> "우 보조 벡터"
    "up" -> "상 보조 벡터"
    "down" -> "하 보조 벡터"
    else -> pose
}

private fun isMaskSuspected(face: Face): Boolean {
    if (abs(face.headEulerAngleY) > 18f || abs(face.headEulerAngleX) > 18f) return false

    val upperLip = face.getContour(FaceContour.UPPER_LIP_TOP)?.points?.size ?: 0
    val lowerLip = face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points?.size ?: 0
    val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
    val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)
    val noseBottom = face.getContour(FaceContour.NOSE_BOTTOM)?.points?.size ?: 0

    val lipsWeak = upperLip < 4 || lowerLip < 4
    val mouthLandmarkMissing = mouthLeft == null || mouthRight == null
    val noseWeak = noseBottom < 3

    return (lipsWeak && mouthLandmarkMissing) || (lipsWeak && noseWeak)
}

private fun isGlassesSuspected(face: Face): Boolean {
    if (abs(face.headEulerAngleY) > 18f || abs(face.headEulerAngleX) > 18f) return false

    val leftEyeContourSize = face.getContour(FaceContour.LEFT_EYE)?.points?.size ?: 0
    val rightEyeContourSize = face.getContour(FaceContour.RIGHT_EYE)?.points?.size ?: 0
    val leftEyeLandmark = face.getLandmark(FaceLandmark.LEFT_EYE)
    val rightEyeLandmark = face.getLandmark(FaceLandmark.RIGHT_EYE)
    val bothEyeProbabilityMissing = face.leftEyeOpenProbability == null && face.rightEyeOpenProbability == null
    val oneEyeProbabilityMissing = face.leftEyeOpenProbability == null || face.rightEyeOpenProbability == null
    val eyeContourWeak = leftEyeContourSize < 8 || rightEyeContourSize < 8
    val eyeLandmarkWeak = leftEyeLandmark == null || rightEyeLandmark == null

    return (bothEyeProbabilityMissing && eyeContourWeak) ||
        (oneEyeProbabilityMissing && eyeLandmarkWeak && eyeContourWeak)
}

private fun signFor(value: Float): Int = if (value >= 0f) 1 else -1

private fun formatScore(value: Double): String = String.format(Locale.US, "%.3f", value)

private fun formatNullableScore(value: Double?): String = value?.let { formatScore(it) } ?: "-"

private fun estimateLuminance(imageProxy: ImageProxy): Double {
    val plane = imageProxy.planes.first()
    val width = imageProxy.width
    val height = imageProxy.height
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val buffer = plane.buffer

    var sum = 0L
    var count = 0
    var row = 0
    while (row < height) {
        var col = 0
        while (col < width) {
            val index = row * rowStride + col * pixelStride
            if (index < buffer.limit()) {
                sum += buffer.get(index).toInt() and 0xFF
                count += 1
            }
            col += 16
        }
        row += 16
    }

    return if (count == 0) 0.0 else sum.toDouble() / count.toDouble()
}

private fun imageProxyToJpegBytes(imageProxy: ImageProxy): ByteArray {
    val nv21 = yuv420888ToNv21(imageProxy)
    val yuvImage = YuvImage(
        nv21,
        ImageFormat.NV21,
        imageProxy.width,
        imageProxy.height,
        null,
    )
    val output = ByteArrayOutputStream()
    yuvImage.compressToJpeg(
        Rect(0, 0, imageProxy.width, imageProxy.height),
        90,
        output,
    )
    val rawJpeg = output.toByteArray()
    return rotateJpeg(rawJpeg, imageProxy.imageInfo.rotationDegrees)
}

private fun rotateJpeg(jpegBytes: ByteArray, rotationDegrees: Int): ByteArray {
    if (rotationDegrees == 0) return jpegBytes

    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return jpegBytes
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    val output = ByteArrayOutputStream()
    rotated.compress(Bitmap.CompressFormat.JPEG, 90, output)
    bitmap.recycle()
    if (rotated != bitmap) rotated.recycle()
    return output.toByteArray()
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val width = image.width
    val height = image.height
    val ySize = width * height
    val uvSize = width * height / 4
    val nv21 = ByteArray(ySize + uvSize * 2)

    val yPlane = image.planes[0]
    val uPlane = image.planes[1]
    val vPlane = image.planes[2]

    val yBuffer = yPlane.buffer
    val uBuffer = uPlane.buffer
    val vBuffer = vPlane.buffer
    yBuffer.rewind()
    uBuffer.rewind()
    vBuffer.rewind()

    val yBytes = ByteArray(yBuffer.remaining())
    yBuffer.get(yBytes)
    val uBytes = ByteArray(uBuffer.remaining())
    uBuffer.get(uBytes)
    val vBytes = ByteArray(vBuffer.remaining())
    vBuffer.get(vBytes)

    var outputPos = 0

    for (row in 0 until height) {
        val rowOffset = row * yPlane.rowStride
        for (col in 0 until width) {
            val index = rowOffset + col * yPlane.pixelStride
            nv21[outputPos++] = yBytes[index]
        }
    }

    for (row in 0 until height / 2) {
        val uRowOffset = row * uPlane.rowStride
        val vRowOffset = row * vPlane.rowStride
        for (col in 0 until width / 2) {
            val uIndex = uRowOffset + col * uPlane.pixelStride
            val vIndex = vRowOffset + col * vPlane.pixelStride
            nv21[outputPos++] = vBytes[vIndex]
            nv21[outputPos++] = uBytes[uIndex]
        }
    }

    return nv21
}
