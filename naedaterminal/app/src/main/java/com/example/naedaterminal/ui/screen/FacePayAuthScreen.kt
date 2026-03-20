package com.example.naedaterminal.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StoreMallDirectory
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.naedaterminal.ui.theme.NaedaFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.sqrt

private val Primary = Color(0xFF00635A)
private val BgScan = Color(0xFFECF8F7)
private val TextPrimary = Color(0xFF0D3B35)
private val GuideReady = Color(0xFF20D5BE)

private const val MAX_SCAN_DURATION_MS = 30_000L
private const val ANALYSIS_INTERVAL_MS = 100L
private const val SERVER_REQUEST_INTERVAL_MS = 700L
private const val CANDIDATE_STALE_MS = 1_500L
private const val MIN_BRIGHTNESS = 55f

data class CandidateResult(
    val userId: String,
    val userNo: Long?,
    val pose: String,
    val similarity: Double
)

data class FaceSearchResponse(
    val matched: Boolean,
    val status: String?,
    val nextAction: String?,
    val bestUserId: String?,
    val username: String?,
    val userNo: Long?,
    val matchedUserNo: Long?,
    val similarity: Double,
    val matchThreshold: Double,
    val ambiguousThreshold: Double,
    val authLevel: String?,
    val requiredMethods: Set<String>,
    val blocked: Boolean,
    val rbaReason: String?,
    val candidates: List<CandidateResult>
)

private data class GuideFrameState(
    val faceDetected: Boolean = false,
    val aligned: Boolean = false,
    val centered: Boolean = false,
    val sizeOk: Boolean = false,
    val brightnessOk: Boolean = true,
    val score: Float = 0f,
    val message: String = "얼굴을 원형 가이드 안에 맞춰주세요.",
    val uploadBytes: ByteArray? = null
)

private data class UploadCandidate(
    val jpegBytes: ByteArray,
    val score: Float,
    val capturedAt: Long
)

@Composable
fun FacePayAuthScreen(
    amount: Long,
    merchant: String,
    apiBaseUrl: String,
    topK: Int = 3,
    onBack: () -> Unit,
    onResolved: (FaceSearchResponse) -> Unit,
    onNotMatched: (String?) -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    var statusText by remember { mutableStateOf("얼굴을 원형 가이드 안에 맞춰주세요.") }
    var guideState by remember { mutableStateOf(GuideFrameState()) }
    var bestCandidate by remember { mutableStateOf<UploadCandidate?>(null) }
    var hasPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    var scanResolved by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf((MAX_SCAN_DURATION_MS / 1_000L).toInt()) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPerm = it }

    LaunchedEffect(Unit) {
        if (!hasPerm) permLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(imageAnalysis) {
        val analyzer = FaceGuideAnalyzer { frameState ->
            if (scanResolved) return@FaceGuideAnalyzer
            guideState = frameState.copy(uploadBytes = null)
            val uploadBytes = frameState.uploadBytes ?: return@FaceGuideAnalyzer
            val candidate = UploadCandidate(
                jpegBytes = uploadBytes,
                score = frameState.score,
                capturedAt = SystemClock.elapsedRealtime()
            )
            val current = bestCandidate
            if (current == null ||
                candidate.score >= current.score ||
                candidate.capturedAt - current.capturedAt > 1_000L
            ) {
                bestCandidate = candidate
            }
        }
        imageAnalysis.setAnalyzer(analysisExecutor, analyzer)
        onDispose {
            imageAnalysis.clearAnalyzer()
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(hasPerm) {
        if (!hasPerm) return@LaunchedEffect

        val startedAt = SystemClock.elapsedRealtime()
        while (!scanResolved) {
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            val remaining = (MAX_SCAN_DURATION_MS - elapsed).coerceAtLeast(0L)
            remainingSeconds = kotlin.math.ceil(remaining / 1_000.0).toInt().coerceAtLeast(0)

            if (remaining <= 0L) {
                scanResolved = true
                onNotMatched("30초 동안 얼굴을 인식하지 못했습니다.")
                return@LaunchedEffect
            }

            val candidate = bestCandidate
            if (!isSending &&
                candidate != null &&
                SystemClock.elapsedRealtime() - candidate.capturedAt <= CANDIDATE_STALE_MS
            ) {
                isSending = true
                bestCandidate = null
                statusText = "얼굴 확인 중..."

                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        postFaceSearchBytes(
                            client = client,
                            apiBaseUrl = apiBaseUrl,
                            imageBytes = candidate.jpegBytes,
                            topK = topK,
                            amount = amount
                        )
                    }
                }

                result.onSuccess { response ->
                    val isAmbiguous = response.status.equals("AMBIGUOUS", ignoreCase = true) ||
                            response.nextAction == "REQUIRE_SECOND_FACTOR"
                    val hasBestUser = !response.bestUserId.isNullOrBlank()
                    when {
                        response.blocked -> {
                            scanResolved = true
                            onNotMatched(response.rbaReason ?: "결제가 차단되었습니다.")
                            return@LaunchedEffect
                        }

                        hasBestUser && (response.matched || isAmbiguous) -> {
                            scanResolved = true
                            onResolved(response)
                            return@LaunchedEffect
                        }

                        else -> {
                            statusText = guideState.messageWithCountdown(remainingSeconds)
                        }
                    }
                }

                result.onFailure { error ->
                    statusText = "네트워크 오류가 발생했습니다. 다시 시도합니다."
                    android.util.Log.e("FacePay", "얼굴 검색 실패: ${error.message}", error)
                }

                isSending = false
                delay(SERVER_REQUEST_INTERVAL_MS)
            } else {
                statusText = when {
                    !guideState.brightnessOk -> guideState.message
                    guideState.aligned -> "좋아요. 얼굴을 그대로 유지해주세요."
                    else -> guideState.messageWithCountdown(remainingSeconds)
                }
                delay(120)
            }
        }
    }

    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    FaceScanContent(
        hasPerm = hasPerm,
        lifecycleOwner = lifecycleOwner,
        imageAnalysis = imageAnalysis,
        guideAligned = guideState.aligned,
        lifecycleStatusText = statusText,
        progress = progress,
        amount = amount,
        merchant = merchant,
        remainingSeconds = remainingSeconds
    )
}

private fun GuideFrameState.messageWithCountdown(remainingSeconds: Int): String {
    if (remainingSeconds <= 0) return message
    return "$message (${remainingSeconds}초 남음)"
}

@Composable
private fun FaceScanContent(
    hasPerm: Boolean,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    imageAnalysis: ImageAnalysis,
    guideAligned: Boolean,
    lifecycleStatusText: String,
    progress: Float,
    amount: Long,
    merchant: String,
    remainingSeconds: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f)
        ) {
            if (hasPerm) {
                AndroidView(
                    factory = { context ->
                        PreviewView(context).also { previewView ->
                            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                            val providerFuture = ProcessCameraProvider.getInstance(context)
                            providerFuture.addListener({
                                val provider = providerFuture.get()
                                val preview = Preview.Builder()
                                    .build()
                                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_FRONT_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (error: Exception) {
                                    error.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF111111)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("카메라 권한 필요", color = Color.White, fontFamily = NaedaFontFamily)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val radius = size.width * 0.38f
                        val guideColor = if (guideAligned) GuideReady else Primary
                        onDrawWithContent {
                            drawContent()
                            drawCircle(
                                color = guideColor,
                                radius = radius,
                                center = Offset(cx, cy),
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = guideColor.copy(alpha = 0.15f),
                                radius = radius + 12.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f)
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = lifecycleStatusText,
                color = if (guideAligned) GuideReady else Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NaedaFontFamily
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "얼굴이 원형 가이드 안에 정확히 들어와야 인식됩니다. 남은 시간 ${remainingSeconds}초",
                color = TextPrimary.copy(alpha = 0.45f),
                fontSize = 13.sp,
                fontFamily = NaedaFontFamily
            )
            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.12f)
            )

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgScan)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.StoreMallDirectory,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = merchant,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = NaedaFontFamily
                        )
                    }
                    Text(
                        text = "FACE PAY",
                        color = Primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = NaedaFontFamily,
                        letterSpacing = 1.sp
                    )
                }

                HorizontalDivider(color = Primary.copy(alpha = 0.12f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "결제 예정 금액",
                        color = TextPrimary.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontFamily = NaedaFontFamily
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%,d".format(amount),
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = NaedaFontFamily
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "KRW",
                            color = TextPrimary.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            fontFamily = NaedaFontFamily,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private class FaceGuideAnalyzer(
    private val onResult: (GuideFrameState) -> Unit
) : ImageAnalysis.Analyzer {
    private val handler = Handler(Looper.getMainLooper())
    private var lastAnalyzedAt = 0L

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAnalyzedAt < ANALYSIS_INTERVAL_MS) {
            image.close()
            return
        }
        lastAnalyzedAt = now

        val result = runCatching { evaluateGuideFrame(image) }
            .getOrElse {
                GuideFrameState(message = "얼굴을 다시 맞춰주세요.")
            }
        image.close()

        handler.post { onResult(result) }
    }
}

private fun evaluateGuideFrame(image: ImageProxy): GuideFrameState {
    val brightness = sampleLuminance(image)
    if (brightness < MIN_BRIGHTNESS) {
        return GuideFrameState(
            brightnessOk = false,
            message = "조명을 더 밝게 해주세요."
        )
    }

    val uprightBitmap = imageProxyToBitmap(image)
        ?: return GuideFrameState(message = "카메라 프레임을 읽는 중입니다.")
    val previewBitmap = scaleDownBitmap(uprightBitmap, maxWidth = 640)
    if (previewBitmap !== uprightBitmap) {
        uprightBitmap.recycle()
    }

    val rgb565 = previewBitmap
        .let(::ensureEvenBitmapWidth)
        .copy(Bitmap.Config.RGB_565, false)
    val face = detectPrimaryFace(rgb565)

    if (face == null) {
        if (rgb565 !== previewBitmap) rgb565.recycle()
        previewBitmap.recycle()
        return GuideFrameState(
            faceDetected = false,
            brightnessOk = true,
            message = "얼굴을 원형 가이드 안에 맞춰주세요."
        )
    }

    val midpoint = PointF().also(face::getMidPoint)
    val faceRadius = face.eyesDistance() * 1.75f
    val guideRadius = previewBitmap.width * 0.38f
    val guideCenterX = previewBitmap.width / 2f
    val guideCenterY = previewBitmap.height / 2f
    val dx = midpoint.x - guideCenterX
    val dy = midpoint.y - guideCenterY
    val centerDistance = sqrt(dx * dx + dy * dy)

    val centered = centerDistance <= guideRadius * 0.18f
    val sizeOk = faceRadius in (guideRadius * 0.40f)..(guideRadius * 0.78f)
    val aligned = centerDistance + faceRadius <= guideRadius * 0.98f && centered && sizeOk
    val alignmentScore = (1f - (centerDistance / guideRadius).coerceIn(0f, 1f))
    val sizeScore = (1f - abs(faceRadius - guideRadius * 0.58f) / guideRadius).coerceIn(0f, 1f)
    val brightnessScore = ((brightness - MIN_BRIGHTNESS) / 60f).coerceIn(0f, 1f)
    val score = (alignmentScore * 0.55f) + (sizeScore * 0.30f) + (brightnessScore * 0.15f)

    val message = when {
        !centered -> "얼굴을 원형 중앙으로 맞춰주세요."
        !sizeOk && faceRadius < guideRadius * 0.40f -> "얼굴을 조금 더 가까이 보여주세요."
        !sizeOk -> "얼굴을 조금 더 뒤로 이동해주세요."
        !aligned -> "얼굴이 가이드를 벗어났습니다."
        else -> "좋아요. 얼굴을 그대로 유지해주세요."
    }

    val bytes = if (aligned) bitmapToJpeg(previewBitmap) else null

    if (rgb565 !== previewBitmap) rgb565.recycle()
    previewBitmap.recycle()

    return GuideFrameState(
        faceDetected = true,
        aligned = aligned,
        centered = centered,
        sizeOk = sizeOk,
        brightnessOk = true,
        score = score,
        message = message,
        uploadBytes = bytes
    )
}

private fun detectPrimaryFace(bitmap: Bitmap): android.media.FaceDetector.Face? {
    val faces = arrayOfNulls<android.media.FaceDetector.Face>(1)
    val detector = android.media.FaceDetector(bitmap.width, bitmap.height, 1)
    val found = detector.findFaces(bitmap, faces)
    return if (found > 0) faces[0] else null
}

private fun sampleLuminance(image: ImageProxy): Float {
    val buffer = image.planes.firstOrNull()?.buffer ?: return 0f
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    var sum = 0L
    val step = maxOf(1, data.size / 1_000)
    for (index in data.indices step step) {
        sum += data[index].toInt() and 0xFF
    }
    val sampleCount = maxOf(1, data.size / step)
    return sum.toFloat() / sampleCount
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val nv21 = imageProxyToNv21(image)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 85, out)
    val jpegBytes = out.toByteArray()
    val rawBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return null
    return if (image.imageInfo.rotationDegrees != 0) {
        rotateBitmap(rawBitmap, image.imageInfo.rotationDegrees.toFloat())
    } else {
        rawBitmap
    }
}

private fun imageProxyToNv21(image: ImageProxy): ByteArray {
    val yBuffer = image.planes[0].buffer
    val uBuffer = image.planes[1].buffer
    val vBuffer = image.planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    return nv21
}

private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(rotationDegrees) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    bitmap.recycle()
    return rotated
}

private fun scaleDownBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
    if (bitmap.width <= maxWidth) return bitmap
    val scale = maxWidth.toFloat() / bitmap.width
    return Bitmap.createScaledBitmap(
        bitmap,
        maxWidth,
        (bitmap.height * scale).toInt(),
        true
    )
}

private fun ensureEvenBitmapWidth(bitmap: Bitmap): Bitmap {
    if (bitmap.width % 2 == 0) return bitmap
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width - 1, bitmap.height)
}

private fun bitmapToJpeg(bitmap: Bitmap): ByteArray {
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
    return out.toByteArray()
}

private fun postFaceSearchBytes(
    client: OkHttpClient,
    apiBaseUrl: String,
    imageBytes: ByteArray,
    topK: Int,
    amount: Long = 0L
): FaceSearchResponse {
    val body = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "image",
            "face_terminal.jpg",
            imageBytes.toRequestBody("image/jpeg".toMediaType())
        )
        .addFormDataPart("topK", topK.toString())
        .addFormDataPart("amount", amount.toString())
        .build()

    val req = Request.Builder()
        .url("${apiBaseUrl.trimEnd('/')}/api/v1/face/search")
        .post(body)
        .build()

    return client.newCall(req).execute().use { res ->
        val raw = res.body?.string().orEmpty()
        if (!res.isSuccessful) error("HTTP ${res.code}: $raw")
        val json = JSONObject(raw)

        val candidatesArray = json.optJSONArray("candidates") ?: JSONArray()
        val candidates = (0 until candidatesArray.length()).map { i ->
            val c = candidatesArray.getJSONObject(i)
            CandidateResult(
                userId = c.optString("userId", "-"),
                userNo = if (c.has("userNo") && !c.isNull("userNo")) c.optLong("userNo") else null,
                pose = c.optString("pose", "-"),
                similarity = c.optDouble("similarity", 0.0)
            )
        }

        val methodsArray = json.optJSONArray("requiredMethods")
        val methods = buildSet {
            if (methodsArray != null) {
                for (i in 0 until methodsArray.length()) {
                    add(methodsArray.getString(i))
                }
            }
        }

        FaceSearchResponse(
            matched = json.optBoolean("matched", false),
            status = json.optString("status").takeIf { it.isNotBlank() },
            nextAction = json.optString("nextAction").takeIf { it.isNotBlank() },
            bestUserId = json.optString("bestUserId").takeIf { it.isNotBlank() },
            username = json.optString("username").takeIf { it.isNotBlank() },
            userNo = if (json.has("userNo") && !json.isNull("userNo")) json.optLong("userNo") else null,
            matchedUserNo = if (json.has("matchedUserNo") && !json.isNull("matchedUserNo")) {
                json.optLong("matchedUserNo")
            } else {
                null
            },
            similarity = json.optDouble("similarity", 0.0),
            matchThreshold = json.optDouble("matchThreshold", 0.7),
            ambiguousThreshold = json.optDouble("ambiguousThreshold", 0.65),
            authLevel = json.optString("authLevel").takeIf { it.isNotBlank() },
            requiredMethods = methods,
            blocked = json.optBoolean("blocked", false),
            rbaReason = json.optString("rbaReason").takeIf { it.isNotBlank() },
            candidates = candidates
        )
    }
}
