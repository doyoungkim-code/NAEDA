package com.example.naedaterminal.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.StoreMallDirectory
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.core.content.FileProvider
import com.example.naedaterminal.ui.theme.NaedaFontFamily
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val Primary = Color(0xFF00635A)
private val BgScan = Color(0xFFECF8F7)
private val TextPrimary = Color(0xFF0D3B35)

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

@Composable
fun FacePayAuthScreen(
    amount: Long,
    merchant: String,
    apiBaseUrl: String,
    topK: Int = 3,
    onBack: () -> Unit,
    onAuthed: (result: FaceSearchResponse) -> Unit,
    onNotMatched: () -> Unit,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    var statusText by remember { mutableStateOf("얼굴 스캔 중...") }
    var busy by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<FaceSearchResponse?>(null) }
    var hasPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPerm = it }

    LaunchedEffect(Unit) {
        if (!hasPerm) permLauncher.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val photoFile = remember { File(ctx.cacheDir, "face_terminal_${System.currentTimeMillis()}.jpg") }

    // 2.5초 후 자동 캡처 → 서버 전송 (자동 재시도)
    LaunchedEffect(hasPerm) {
        if (!hasPerm) return@LaunchedEffect
        delay(2500)

        while (true) {
            withContext(Dispatchers.Main) { statusText = "서버 전송 중..." }

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    captureImage(imageCapture, photoFile, executor)
                    postFaceSearchFile(client, apiBaseUrl, photoFile, topK, amount)
                }
            }

            result.onSuccess { resp ->
                android.util.Log.d("FacePay", "응답: matched=${resp.matched}, bestUserId=${resp.bestUserId}, similarity=${resp.similarity}")
                if (resp.blocked) {
                    onNotMatched()
                    return@LaunchedEffect
                } else if (resp.matched && !resp.bestUserId.isNullOrBlank()) {
                    onAuthed(resp)
                    return@LaunchedEffect
                } else {
                    // 매칭 안 됨 → 재시도
                    statusText = "얼굴을 인식하지 못했습니다. 다시 시도합니다..."
                    android.util.Log.d("FacePay", "매칭 실패, 재시도...")
                }
            }

            result.onFailure { error ->
                android.util.Log.e("FacePay", "얼굴 인식 실패: ${error.message}", error)
                statusText = "인식 실패: ${error.message?.take(40)}"
            }

            delay(2000) // 2초 후 재시도
            withContext(Dispatchers.Main) { statusText = "얼굴 스캔 중..." }
            delay(1500)
        }
    }

    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "progress"
    )

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    FaceScanContent(
        hasPerm = hasPerm,
        lifecycleOwner = lifecycleOwner,
        imageCapture = imageCapture,
        statusText = statusText,
        progress = progress,
        amount = amount,
        merchant = merchant
    )
}

// ── 스캔 중 화면 ──
@Composable
private fun FaceScanContent(
    hasPerm: Boolean,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    imageCapture: ImageCapture,
    statusText: String,
    progress: Float,
    amount: Long,
    merchant: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── 카메라 프리뷰 (상단 58%) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f)
        ) {
            if (hasPerm) {
                AndroidView(
                    factory = { context ->
                        PreviewView(context).also { pv ->
                            val fut = ProcessCameraProvider.getInstance(context)
                            fut.addListener({
                                val provider = fut.get()
                                val preview = Preview.Builder().build()
                                    .also { it.setSurfaceProvider(pv.surfaceProvider) }
                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_FRONT_CAMERA,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) { e.printStackTrace() }
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

            // 원형 가이드 오버레이
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = size.width * 0.38f
                        onDrawWithContent {
                            drawContent()
                            drawCircle(
                                color = Primary,
                                radius = r,
                                center = Offset(cx, cy),
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = Primary.copy(alpha = 0.15f),
                                radius = r + 12.dp.toPx(),
                                center = Offset(cx, cy),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
            )
        }

        // ── 하단 정보 패널 ──
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
                text = statusText,
                color = Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NaedaFontFamily
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "정면을 바라보고 잠시만 기다려 주세요.",
                color = TextPrimary.copy(alpha = 0.45f),
                fontSize = 13.sp,
                fontFamily = NaedaFontFamily
            )
            Spacer(Modifier.height(14.dp))

            // ✅ progress Float로 수정
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.12f)
            )

            Spacer(Modifier.height(20.dp))

            // ── 결제 정보 카드 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgScan)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 가맹점 행
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
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

                // 금액 행
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

// ── 코루틴 친화적 캡처 helper ──
private suspend fun captureImage(
    imageCapture: ImageCapture,
    file: File,
    executor: java.util.concurrent.Executor
): Unit = suspendCoroutine { cont ->
    imageCapture.takePicture(
        ImageCapture.OutputFileOptions.Builder(file).build(),
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // 이미지 리사이즈 + 압축 (3MB 제한)
                compressImage(file, maxWidth = 720, quality = 85)
                cont.resume(Unit)
            }
            override fun onError(exc: ImageCaptureException) = cont.resumeWithException(exc)
        }
    )
}

private fun compressImage(file: File, maxWidth: Int, quality: Int) {
    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath) ?: return
    val scale = if (bitmap.width > maxWidth) maxWidth.toFloat() / bitmap.width else 1f
    val resized = if (scale < 1f) {
        android.graphics.Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )
    } else bitmap

    file.outputStream().use { out ->
        resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
    }
    if (resized !== bitmap) resized.recycle()
    bitmap.recycle()
}

private fun postFaceSearchFile(
    client: OkHttpClient,
    apiBaseUrl: String,
    file: File,
    topK: Int,
    amount: Long = 0L
): FaceSearchResponse {
    val body = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("image", file.name, file.asRequestBody("image/jpeg".toMediaType()))
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
            matchedUserNo = if (json.has("matchedUserNo") && !json.isNull("matchedUserNo"))
                json.optLong("matchedUserNo") else null,
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

private fun fileUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)