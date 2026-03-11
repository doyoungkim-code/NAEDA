package com.example.naedafront.ui.screen.facepay

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.naedafront.DebugAuthPrefs
import com.example.naedafront.data.remote.AccountResponse
import com.example.naedafront.data.remote.CandidateDto
import com.example.naedafront.data.remote.FaceMatchRepository
import com.example.naedafront.data.remote.SearchResponse
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceMatchRecognizeScreen(
    onBack: () -> Unit,
    onShowResult: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var userId by remember { mutableStateOf(DebugAuthPrefs.getUserId(context) ?: "") }
    var password by remember { mutableStateOf("") }
    var manualToken by remember { mutableStateOf(DebugAuthPrefs.getAccessToken(context) ?: "") }
    var activeToken by remember { mutableStateOf(DebugAuthPrefs.getAccessToken(context) ?: "") }
    var authMessage by remember { mutableStateOf("테스트 화면은 서버 토큰이 필요합니다.") }
    var statusMessage by remember { mutableStateOf("얼굴을 정면으로 비춰주세요.") }
    var lastResponse by remember { mutableStateOf<SearchResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("페이스 매칭 테스트") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AuthPanel(
                userId = userId,
                password = password,
                manualToken = manualToken,
                authMessage = authMessage,
                onUserIdChange = { userId = it },
                onPasswordChange = { password = it },
                onManualTokenChange = { manualToken = it },
                onApplyTokenClick = {
                    if (manualToken.isBlank()) {
                        authMessage = "토큰이 비어 있습니다."
                    } else {
                        activeToken = manualToken.trim()
                        authMessage = "입력한 토큰을 사용합니다."
                    }
                },
                onLoginClick = {
                    scope.launch {
                        errorMessage = null
                        authMessage = "로그인 중..."
                        runCatching {
                            FaceMatchRepository.login(userId.trim(), password)
                        }.onSuccess { response ->
                            val accessToken = response.accessToken.orEmpty()
                            if (accessToken.isBlank()) {
                                authMessage = "토큰이 응답에 없습니다."
                                return@onSuccess
                            }
                            activeToken = accessToken
                            manualToken = accessToken
                            DebugAuthPrefs.saveSession(
                                context = context,
                                accessToken = accessToken,
                                refreshToken = response.refreshToken,
                                userNo = response.userNo,
                                userId = response.userId,
                                username = response.username
                            )
                            authMessage = "로그인 성공. 카메라 인식을 시작하세요."
                        }.onFailure { throwable ->
                            authMessage = "로그인 실패"
                            errorMessage = throwable.message
                        }
                    }
                },
                onClearSessionClick = {
                    DebugAuthPrefs.clear(context)
                    activeToken = ""
                    manualToken = ""
                    authMessage = "저장된 테스트 세션을 지웠습니다."
                }
            )

            if (!hasCameraPermission) {
                PermissionPanel(
                    onRequestPermissionClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            } else {
                RecognizeCameraCard(
                    activeToken = activeToken,
                    onStatusChange = { statusMessage = it },
                    onError = { errorMessage = it },
                    onResponse = { response ->
                        lastResponse = response
                    }
                )
            }

            StatusPanel(
                statusMessage = statusMessage,
                lastResponse = lastResponse,
                onShowResult = onShowResult
            )

            errorMessage?.let { message ->
                ErrorPanel(message)
            }
        }
    }
}

@Composable
private fun AuthPanel(
    userId: String,
    password: String,
    manualToken: String,
    authMessage: String,
    onUserIdChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onManualTokenChange: (String) -> Unit,
    onApplyTokenClick: () -> Unit,
    onLoginClick: () -> Unit,
    onClearSessionClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("서버 세션", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = userId,
                onValueChange = onUserIdChange,
                label = { Text("userId") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onLoginClick) {
                    Text("로그인")
                }
                Button(onClick = onApplyTokenClick) {
                    Text("토큰 적용")
                }
                Button(onClick = onClearSessionClick) {
                    Text("세션 삭제")
                }
            }
            OutlinedTextField(
                value = manualToken,
                onValueChange = onManualTokenChange,
                label = { Text("Access Token") },
                modifier = Modifier.fillMaxWidth()
            )
            Text(authMessage, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PermissionPanel(
    onRequestPermissionClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("카메라 권한이 필요합니다.", fontWeight = FontWeight.Bold)
            Button(onClick = onRequestPermissionClick) {
                Text("권한 요청")
            }
        }
    }
}

@Composable
private fun StatusPanel(
    statusMessage: String,
    lastResponse: SearchResponse?,
    onShowResult: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("인식 상태", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(statusMessage)
            lastResponse?.let { response ->
                HorizontalDivider()
                Text("최근 응답 상태: ${response.status ?: "-"}")
                Text("bestUserId: ${response.bestUserId ?: "-"}")
                Text("matchedUserNo: ${response.matchedUserNo ?: "-"}")
                Text("similarity: ${"%.4f".format(response.similarity)}")
                Text("matchThreshold: ${"%.4f".format(response.matchThreshold)}")
                Text("ambiguousThreshold: ${"%.4f".format(response.ambiguousThreshold)}")
                Button(
                    onClick = onShowResult,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("결과 화면 보기")
                }
            }
        }
    }
}

@Composable
private fun ErrorPanel(message: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(18.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun RecognizeCameraCard(
    activeToken: String,
    onStatusChange: (String) -> Unit,
    onError: (String) -> Unit,
    onResponse: (SearchResponse) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val livenessEvaluator = remember { PassiveLivenessEvaluator() }
    val requestInFlight = remember { AtomicBoolean(false) }
    val lastRequestAt = remember { AtomicLong(0L) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("전면 카메라", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FaceCameraView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                onFaceFrame = { imageProxy, face, luminance ->
                    val token = activeToken.trim()
                    if (token.isBlank()) {
                        onStatusChange("로그인 또는 토큰 입력 후 인식을 시작하세요.")
                        imageProxy.close()
                        return@FaceCameraView
                    }

                    val isFrontFace = abs(face.headEulerAngleY) < 16f &&
                        abs(face.headEulerAngleX) < 16f &&
                        abs(face.headEulerAngleZ) < 12f
                    val isLive = livenessEvaluator.observe(face)

                    when {
                        luminance < 35.0 -> {
                            onStatusChange("화면이 어두워서 인식이 어렵습니다.")
                            imageProxy.close()
                            return@FaceCameraView
                        }

                        !isFrontFace -> {
                            onStatusChange("정면으로 얼굴을 맞춰주세요.")
                            imageProxy.close()
                            return@FaceCameraView
                        }

                        !isLive -> {
                            onStatusChange(livenessEvaluator.guideText())
                            imageProxy.close()
                            return@FaceCameraView
                        }
                    }

                    val now = System.currentTimeMillis()
                    if (requestInFlight.get() || now - lastRequestAt.get() < 900L) {
                        onStatusChange("실제 얼굴 확인 중입니다.")
                        imageProxy.close()
                        return@FaceCameraView
                    }

                    val jpegBytes = runCatching { imageProxyToJpegBytes(imageProxy) }
                        .onFailure { throwable ->
                            onError("프레임 변환 실패: ${throwable.message}")
                        }
                        .getOrNull()
                    imageProxy.close()

                    if (jpegBytes == null) {
                        return@FaceCameraView
                    }

                    requestInFlight.set(true)
                    lastRequestAt.set(now)
                    onStatusChange("배포 서버에 얼굴 검색 요청 중입니다.")

                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            val response = FaceMatchRepository.searchFace(token, jpegBytes)
                            FaceMatchSessionStore.latestResult = buildSnapshot(token, response)
                            response
                        }.onSuccess { response ->
                            onResponse(response)
                            onStatusChange(
                                when (response.status) {
                                    "MATCH" -> "threshold를 넘는 최고 후보를 찾았습니다."
                                    "AMBIGUOUS" -> "애매한 후보가 있어 결과 화면에서 비교할 수 있습니다."
                                    else -> "threshold 아래 후보들만 있어 결과 화면에서 참고 후보를 확인할 수 있습니다."
                                }
                            )
                        }.onFailure { throwable ->
                            onError("얼굴 검색 실패: ${throwable.message}")
                        }
                        requestInFlight.set(false)
                    }
                },
                onStatusChange = onStatusChange
            )
        }
    }
}

@Composable
private fun FaceCameraView(
    modifier: Modifier,
    onFaceFrame: (ImageProxy, Face, Double) -> Unit,
    onStatusChange: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnFaceFrame by rememberUpdatedState(onFaceFrame)
    val latestOnStatusChange by rememberUpdatedState(onStatusChange)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            setEnabledUseCases(
                LifecycleCameraController.IMAGE_CAPTURE or
                    LifecycleCameraController.IMAGE_ANALYSIS or
                    LifecycleCameraController.VIDEO_CAPTURE
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
                        latestOnStatusChange("얼굴을 화면 중앙에 맞춰주세요.")
                        imageProxy.close()
                    } else {
                        latestOnFaceFrame(imageProxy, bestFace, estimateLuminance(imageProxy))
                    }
                }
                .addOnFailureListener { throwable ->
                    latestOnStatusChange("얼굴 분석 실패: ${throwable.message ?: "unknown"}")
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

    AndroidView(
        modifier = modifier.background(Color.Black, RoundedCornerShape(18.dp)),
        factory = { ctx ->
            PreviewView(ctx).apply {
                this.controller = cameraController
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }
    )
}

private class PassiveLivenessEvaluator {
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
            !blinkDetected && frameCount < 5 -> "눈을 한 번 깜빡이거나 얼굴을 조금만 움직여주세요."
            !blinkDetected && movementScore < 20f -> "정면을 유지한 채 미세하게 움직여주세요."
            else -> "실제 얼굴로 판단되었습니다."
        }

        return blinkDetected || movementScore > 24f || (yawMax - yawMin) > 6f || (pitchMax - pitchMin) > 6f
    }

    fun guideText(): String = guide
}

private suspend fun buildSnapshot(
    token: String,
    response: SearchResponse
): FaceMatchResultSnapshot {
    val accountMap = linkedMapOf<Long, List<AccountResponse>>()
    response.candidates.mapNotNull { it.userNo }.distinct().forEach { userNo ->
        val accounts = runCatching {
            FaceMatchRepository.getAccounts(token, userNo)
        }.getOrDefault(emptyList())
        accountMap[userNo] = accounts
    }

    val normalizedCandidates = response.candidates
        .groupBy { candidate -> "${candidate.userId}:${candidate.userNo}" }
        .values
        .mapNotNull { duplicates ->
            duplicates.maxByOrNull { it.similarity ?: 0f }?.toResultCandidate(accountMap)
        }
        .sortedByDescending { it.similarity }

    val matchedCandidates = normalizedCandidates.filter {
        it.similarity >= response.matchThreshold
    }
    val ambiguousCandidates = normalizedCandidates.filter {
        it.similarity >= response.ambiguousThreshold && it.similarity < response.matchThreshold
    }
    val belowThresholdCandidates = normalizedCandidates.filter {
        it.similarity < response.ambiguousThreshold
    }

    return FaceMatchResultSnapshot(
        searchResponse = response,
        topCandidate = normalizedCandidates.firstOrNull(),
        matchedCandidates = matchedCandidates,
        ambiguousCandidates = ambiguousCandidates,
        belowThresholdCandidates = belowThresholdCandidates
    )
}

private fun CandidateDto.toResultCandidate(
    accountMap: Map<Long, List<AccountResponse>>
): FaceMatchCandidateResult? {
    val id = userId ?: return null
    return FaceMatchCandidateResult(
        userId = id,
        userNo = userNo,
        pose = pose ?: "-",
        similarity = similarity ?: 0f,
        accounts = userNo?.let { accountMap[it] }.orEmpty()
    )
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
    yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 90, output)
    val jpegBytes = output.toByteArray()
    return rotateJpeg(jpegBytes, imageProxy.imageInfo.rotationDegrees)
}

private fun rotateJpeg(jpegBytes: ByteArray, rotationDegrees: Int): ByteArray {
    if (rotationDegrees == 0) return jpegBytes
    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return jpegBytes
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    val output = ByteArrayOutputStream()
    rotated.compress(Bitmap.CompressFormat.JPEG, 90, output)
    bitmap.recycle()
    rotated.recycle()
    return output.toByteArray()
}

private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)

    val chromaRowStride = image.planes[1].rowStride
    val chromaPixelStride = image.planes[1].pixelStride
    val width = image.width
    val height = image.height
    val vBytes = ByteArray(vSize)
    val uBytes = ByteArray(uSize)
    vBuffer.get(vBytes)
    uBuffer.get(uBytes)

    var outputOffset = ySize
    for (row in 0 until height / 2) {
        for (col in 0 until width / 2) {
            val index = row * chromaRowStride + col * chromaPixelStride
            nv21[outputOffset++] = vBytes[index]
            nv21[outputOffset++] = uBytes[index]
        }
    }

    return nv21
}
