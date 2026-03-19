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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
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
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val Primary = Color(0xFF009688)
private val BgScan = Color(0xFFECF8F7)
private val TextPrimary = Color(0xFF0D3B35)

data class FaceSearchResponse(
    val matched: Boolean,
    val bestUserId: String?,
    val similarity: Double,
    val threshold: Double
)

@Composable
fun FacePayAuthScreen(
    amount: Long,
    merchant: String,
    apiBaseUrl: String,
    topK: Int = 3,
    onBack: () -> Unit,
    onAuthed: (bestUserId: String, similarity: Double) -> Unit,
    onNotMatched: () -> Unit,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    var statusText by remember { mutableStateOf("얼굴 스캔 중...") }
    var busy by remember { mutableStateOf(false) }
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

    // 2.5초 후 자동 캡처
    LaunchedEffect(hasPerm) {
        if (!hasPerm) return@LaunchedEffect
        delay(2500)
        if (busy) return@LaunchedEffect
        busy = true
        statusText = "서버 전송 중..."
        scope.launch(Dispatchers.IO) {
            runCatching {
                captureImage(imageCapture, photoFile, executor)
                postFaceSearchFile(client, apiBaseUrl, photoFile, topK)
            }.onSuccess { resp ->
                withContext(Dispatchers.Main) {
                    if (resp.matched && !resp.bestUserId.isNullOrBlank()) {
                        onAuthed(resp.bestUserId!!, resp.similarity)
                    } else {
                        onNotMatched()
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    busy = false
                    statusText = "인식 실패, 다시 시도합니다"
                }
                delay(1500)
                withContext(Dispatchers.Main) { statusText = "얼굴 스캔 중..." }
            }
        }
    }

    // 진행 애니메이션
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "progress"
    )

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

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
                    modifier = Modifier.fillMaxSize().background(Color(0xFF111111)),
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
                                color = Color(0xFF009688),
                                radius = r,
                                center = Offset(cx, cy),
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = Color(0xFF009688).copy(alpha = 0.15f),
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

            // 결제 정보 카드
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgScan)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "결제 예정 금액",
                        color = TextPrimary.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontFamily = NaedaFontFamily
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%,d".format(amount),
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = NaedaFontFamily
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "KRW",
                            color = TextPrimary.copy(alpha = 0.45f),
                            fontSize = 13.sp,
                            fontFamily = NaedaFontFamily,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) { Text("🏪", fontSize = 13.sp) }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = merchant,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = NaedaFontFamily
                            )
                            Text(
                                text = "GUMI Electronics Store Main",
                                color = TextPrimary.copy(alpha = 0.4f),
                                fontSize = 10.sp,
                                fontFamily = NaedaFontFamily
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
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
            override fun onImageSaved(output: ImageCapture.OutputFileResults) = cont.resume(Unit)
            override fun onError(exc: ImageCaptureException) = cont.resumeWithException(exc)
        }
    )
}

private fun postFaceSearchFile(
    client: OkHttpClient,
    apiBaseUrl: String,
    file: File,
    topK: Int
): FaceSearchResponse {
    val body = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("image", file.name, file.asRequestBody("image/jpeg".toMediaType()))
        .addFormDataPart("topK", topK.toString())
        .build()

    val req = Request.Builder()
        .url("${apiBaseUrl.trimEnd('/')}/api/v1/face/search")
        .post(body)
        .build()

    return client.newCall(req).execute().use { res ->
        val raw = res.body?.string().orEmpty()
        if (!res.isSuccessful) error("HTTP ${res.code}: $raw")
        val json = JSONObject(raw)
        FaceSearchResponse(
            matched = json.optBoolean("matched", false),
            bestUserId = json.optString("bestUserId").takeIf { it.isNotBlank() },
            similarity = json.optDouble("similarity", 0.0),
            threshold = json.optDouble("threshold", 0.0)
        )
    }
}

private fun fileUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)