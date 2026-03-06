package com.example.naedaterminal.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

data class FaceSearchResponse(
    val matched: Boolean,
    val bestUserId: String?,
    val similarity: Double,
    val threshold: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacePayAuthScreen(
    apiBaseUrl: String,
    topK: Int = 3,
    onBack: () -> Unit,
    onAuthed: (bestUserId: String, similarity: Double) -> Unit,
    onNotMatched: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    var status by remember { mutableStateOf("촬영 버튼을 누르세요.") }
    var busy by remember { mutableStateOf(false) }

    // 촬영 파일(캐시). filename으로 그대로 전송됨
    val photoFile = remember {
        File(ctx.cacheDir, "face_${System.currentTimeMillis()}.jpg")
    }
    val photoUri = remember(photoFile) { fileUri(ctx, photoFile) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        if (!ok) {
            status = "촬영이 취소되었습니다."
            return@rememberLauncherForActivityResult
        }

        busy = true
        status = "서버로 전송 중…"

        scope.launch(Dispatchers.IO) {
            runCatching {
                postFaceSearchFile(
                    client = client,
                    apiBaseUrl = apiBaseUrl,
                    file = photoFile,
                    topK = topK
                )
            }.onSuccess { resp ->
                busy = false
                if (resp.matched && !resp.bestUserId.isNullOrBlank()) {
                    status = "성공: ${resp.bestUserId} (sim=${"%.2f".format(resp.similarity)})"
                    onAuthed(resp.bestUserId!!, resp.similarity)
                } else {
                    status = "일치 사용자 없음"
                    onNotMatched()
                }
            }.onFailure { e ->
                busy = false
                status = "실패: ${e.message}"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) takePictureLauncher.launch(photoUri)
        else status = "카메라 권한이 필요합니다."
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FacePay 촬영", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(status)

            Button(
                enabled = !busy,
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        takePictureLauncher.launch(photoUri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            ) {
                Text(if (busy) "전송 중…" else "촬영하고 전송")
            }
        }
    }
}

private fun postFaceSearchFile(
    client: OkHttpClient,
    apiBaseUrl: String,
    file: File,
    topK: Int
): FaceSearchResponse {
    val body = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            name = "image",
            filename = file.name, // ✅ 파일명 전송
            body = file.asRequestBody("image/jpeg".toMediaType())
        )
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
            bestUserId = json.optString("bestUserId", null),
            similarity = json.optDouble("similarity", 0.0),
            threshold = json.optDouble("threshold", 0.0)
        )
    }
}

private fun fileUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )