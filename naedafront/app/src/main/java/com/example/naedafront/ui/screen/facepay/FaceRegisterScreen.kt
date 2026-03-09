package com.example.naedafront.ui.screen.facepay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.naedafront.ui.theme.*
import kotlinx.coroutines.delay

// ─── 등록 방향 ────────────────────────────────────────────────────
enum class FaceDirection(val label: String, val instruction: String) {
    FRONT("정면", "카메라를 정면으로 바라봐 주세요"),
    UP("위", "고개를 위로 들어주세요"),
    DOWN("아래", "고개를 아래로 내려주세요"),
    LEFT("왼쪽", "고개를 왼쪽으로 돌려주세요"),
    RIGHT("오른쪽", "고개를 오른쪽으로 돌려주세요")
}

val ALL_DIRECTIONS = listOf(
    FaceDirection.FRONT, FaceDirection.UP, FaceDirection.DOWN,
    FaceDirection.LEFT, FaceDirection.RIGHT
)

// ─── 화면 단계 ────────────────────────────────────────────────────
sealed class FaceRegisterStep {
    object PermissionRequest : FaceRegisterStep()
    object Intro : FaceRegisterStep()
    object Guide : FaceRegisterStep()
    data class Scanning(val directionIndex: Int) : FaceRegisterStep()
    object Analyzing : FaceRegisterStep()
    object IdGuide : FaceRegisterStep()
    object IdScanning : FaceRegisterStep()
    data class IdConfirm(
        val name: String = "김철수",
        val idNumber: String = "900101-1023456",
        val issueDate: String = "2023.10.15"
    ) : FaceRegisterStep()
    object PinSetup : FaceRegisterStep()
    object Success : FaceRegisterStep()
}

// ─── 메인 화면 ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRegisterScreen(
    onBack: () -> Unit,
    onRegisterComplete: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var step: FaceRegisterStep by remember {
        mutableStateOf(
            if (hasCameraPermission) FaceRegisterStep.Intro else FaceRegisterStep.PermissionRequest
        )
    }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        step = if (granted) FaceRegisterStep.Intro
        else {
            val canAsk = activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ?: false
            isPermanentlyDenied = !canAsk
            FaceRegisterStep.PermissionRequest
        }
    }

    // 자동 진행 타이머
    LaunchedEffect(step) {
        when (val s = step) {
            is FaceRegisterStep.Scanning -> {
                delay(2500L)
                val next = s.directionIndex + 1
                step = if (next < ALL_DIRECTIONS.size) FaceRegisterStep.Scanning(next)
                else FaceRegisterStep.Analyzing
            }
            is FaceRegisterStep.Analyzing -> {
                delay(3000L)
                step = FaceRegisterStep.IdGuide
            }
            is FaceRegisterStep.IdScanning -> {
                delay(3000L)
                step = FaceRegisterStep.IdConfirm()
            }
            else -> Unit
        }
    }

    val isDark = step is FaceRegisterStep.Scanning || step is FaceRegisterStep.IdScanning
    val isFullscreen = isDark || step is FaceRegisterStep.Intro || step is FaceRegisterStep.Analyzing

    val showTopBar = !isFullscreen
    val showBack = showTopBar && step !is FaceRegisterStep.Success
    val showClose = step is FaceRegisterStep.Success

    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (step is FaceRegisterStep.IdConfirm) "신분증 정보 확인" else "NAEDA",
                            fontFamily = NaedaFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = OnBackground
                        )
                    },
                    navigationIcon = {
                        if (showBack) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = OnBackground)
                            }
                        }
                    },
                    actions = {
                        if (showClose) {
                            IconButton(onClick = onRegisterComplete) {
                                Icon(Icons.Default.Close, contentDescription = "닫기", tint = OnBackground)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
                )
            }
        },
        containerColor = if (isDark) Color(0xFF1A1E1E) else Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isDark) PaddingValues(0.dp) else padding)
        ) {
            when (val s = step) {
                is FaceRegisterStep.PermissionRequest -> PermissionRequestContent(
                    isPermanentlyDenied = isPermanentlyDenied,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    }
                )
                is FaceRegisterStep.Intro -> IntroContent(onBack = onBack, onStart = { step = FaceRegisterStep.Guide })
                is FaceRegisterStep.Guide -> GuideContent(onStart = { step = FaceRegisterStep.Scanning(0) })
                is FaceRegisterStep.Scanning -> ScanningContent(s.directionIndex, ALL_DIRECTIONS.size)
                is FaceRegisterStep.Analyzing -> AnalyzingContent()
                is FaceRegisterStep.IdGuide -> IdGuideContent(onStart = { step = FaceRegisterStep.IdScanning })
                is FaceRegisterStep.IdScanning -> IdScanningContent()
                is FaceRegisterStep.IdConfirm -> IdConfirmContent(
                    name = s.name, idNumber = s.idNumber, issueDate = s.issueDate,
                    onConfirm = { step = FaceRegisterStep.PinSetup }
                )
                is FaceRegisterStep.PinSetup -> PinSetupContent(
                    onUsePin = { step = FaceRegisterStep.Success },
                    onSkip = { step = FaceRegisterStep.Success }
                )
                is FaceRegisterStep.Success -> SuccessContent(onComplete = onRegisterComplete)
            }
        }
    }
}

// ─── 권한 요청 ────────────────────────────────────────────────────
@Composable
private fun PermissionRequestContent(
    isPermanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(Mint100.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Text("📷", fontSize = 40.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (isPermanentlyDenied) "카메라 권한이 차단됐어요" else "카메라 권한이 필요해요",
            fontFamily = NaedaFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
            color = OnBackground, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (isPermanentlyDenied) "권한이 영구적으로 거부됐어요.\n설정에서 직접 허용해주세요."
            else "얼굴 등록을 위해\n카메라 접근 권한을 허용해주세요",
            fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp,
            color = OnSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 22.sp
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = if (isPermanentlyDenied) onSettings else onRequest,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint500)
        ) {
            Text(
                text = if (isPermanentlyDenied) "설정으로 이동" else "권한 허용하기",
                fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White
            )
        }
    }
}

// ─── 소개 화면 ────────────────────────────────────────────────────
@Composable
private fun IntroContent(onBack: () -> Unit, onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 48.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = OnBackground)
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(100.dp))
            Text(
                text = "이제 오프라인에서\n얼굴 인증으로 결제하세요",
                fontFamily = NaedaFontFamily, fontWeight = FontWeight.Bold,
                fontSize = 24.sp, color = OnBackground, lineHeight = 34.sp
            )
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Mint50),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(130.dp).clip(CircleShape).background(Mint100.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) { Text("😊", fontSize = 56.sp) }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "지갑, 휴대폰 두고 나와도\n결제할 수 있어요",
                        fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal,
                        fontSize = 14.sp, color = Mint700, textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Text("어디서, 어떻게 사용하나요?", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = OnBackground)
            Spacer(Modifier.height(16.dp))
            listOf("매장 기기에", "얼굴을 인식하면 결제완료!").forEachIndexed { index, text ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Mint500), contentAlignment = Alignment.Center) {
                        Text("${index + 1}", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                    Text(text, fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, color = OnBackground)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint900)
            ) {
                Text("페이스페이 시작하기", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "NADA PAY  ·  SECURE CORE",
                fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal,
                fontSize = 12.sp, color = OnSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── 가이드 화면 ─────────────────────────────────────────────────
@Composable
private fun GuideContent(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            "페이스페이 사용을 위해\n얼굴을 등록할게요",
            fontFamily = NaedaFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 24.sp, color = OnBackground, lineHeight = 34.sp
        )
        Spacer(Modifier.height(6.dp))
        Text("빠르고 안전한 결제를 시작해 보세요.", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = OnSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp)).background(Mint50),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(120.dp).border(2.dp, Mint500, CircleShape), contentAlignment = Alignment.Center) {
                    Text("😊", fontSize = 52.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Surface).padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("📷", fontSize = 14.sp)
                    Text("정면을 바라봐 주세요", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = OnBackground)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ℹ️", fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text("등록 팁", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = OnBackground)
        }
        Spacer(Modifier.height(16.dp))
        listOf("😷" to "마스크나 모자를 벗어주세요", "☀️" to "밝은 곳에서 촬영해주세요", "🎯" to "카메라를 정면으로 응시하세요").forEach { (emoji, tip) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceVariant), contentAlignment = Alignment.Center) {
                        Text(emoji, fontSize = 18.sp)
                    }
                    Text(tip, fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = OnBackground)
                }
                Icon(Icons.Default.Check, contentDescription = null, tint = Mint500, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint900)
        ) {
            Text("등록 시작하기", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ─── 얼굴 스캔 화면 (Canvas 구멍) ────────────────────────────────
@Composable
private fun ScanningContent(directionIndex: Int, totalCount: Int) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val direction = ALL_DIRECTIONS[directionIndex]

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // 카메라 풀스크린
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cp = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    try { cp.unbindAll(); cp.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview) }
                    catch (e: Exception) { e.printStackTrace() }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        // 어두운 오버레이 + 원형 구멍
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val r = 160.dp.toPx()
            val cx = size.width / 2f
            val cy = size.height / 2f - 40.dp.toPx()
            drawRect(color = Color(0xFF1A1A1A).copy(alpha = 0.72f))
            drawCircle(color = Color.Transparent, radius = r, center = androidx.compose.ui.geometry.Offset(cx, cy), blendMode = androidx.compose.ui.graphics.BlendMode.Clear)
        }
        // 원형 테두리
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.size(320.dp).align(Alignment.Center).offset(y = (-40).dp)
                    .border(2.5.dp, SolidColor(Mint500.copy(alpha = pulseAlpha)), CircleShape)
            )
        }
        // 상단 스텝
        Box(modifier = Modifier.fillMaxWidth().padding(top = 52.dp).align(Alignment.TopCenter), contentAlignment = Alignment.Center) {
            Text("STEP ${directionIndex + 1} OF $totalCount", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
        }
        // 방향 안내
        Column(modifier = Modifier.align(Alignment.Center).offset(y = (-290).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(direction.instruction, fontFamily = NaedaFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("카메라를 바라보며 천천히 ${direction.label} 방향으로 움직이세요", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        }
        // 하단
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.width(200.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.2f))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((directionIndex + 1).toFloat() / totalCount.toFloat()).clip(RoundedCornerShape(2.dp)).background(Mint500))
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ALL_DIRECTIONS.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(
                            if (index < directionIndex) Mint500
                            else if (index == directionIndex) Mint500.copy(alpha = pulseAlpha)
                            else Color.White.copy(alpha = 0.15f)
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < directionIndex) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("😊", fontSize = 14.sp)
                Text("NAEDA 엔진이 분석 중입니다...", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f))
            }
        }
    }
}

// ─── 분석 중 화면 ─────────────────────────────────────────────────
@Composable
private fun AnalyzingContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "analyzing")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Mint50).padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text("분석 중", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Mint500)
        }
        Spacer(Modifier.height(16.dp))
        Text("모든 방향 인식 완료!", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = OnBackground)
        Spacer(Modifier.height(56.dp))
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(230.dp).clip(CircleShape).background(Mint100.copy(alpha = glowAlpha * 0.4f)))
            Box(modifier = Modifier.size(180.dp).clip(CircleShape).background(Mint500), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(80.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🛡️", fontSize = 18.sp)
            Text("내다(NAEDA) 엔진", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = OnBackground)
        }
        Spacer(Modifier.height(8.dp))
        Text("정보를 안전하게 분석하고 있습니다.", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = OnSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth(0.65f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(SurfaceVariant)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(2.dp)).background(Mint500))
        }
        Spacer(Modifier.height(52.dp))
    }
}

// ─── 신분증 안내 화면 (Image 1) ───────────────────────────────────
@Composable
private fun IdGuideContent(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        // 준비하기 칩
        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Mint50).padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text("준비하기", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Mint500)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "신분증을 준비해 주세요",
            fontFamily = NaedaFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 26.sp, color = OnBackground, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        // 신분증 아이콘 카드
        Box(contentAlignment = Alignment.Center) {
            // 글로우 배경
            Box(modifier = Modifier.size(220.dp).clip(CircleShape).background(Mint100.copy(alpha = 0.25f)))
            Box(
                modifier = Modifier.size(160.dp).clip(CircleShape).background(Mint500),
                contentAlignment = Alignment.Center
            ) {
                Text("🪪", fontSize = 64.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        // 내다 엔진 안내
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🛡️", fontSize = 16.sp)
            Text("내다(NAEDA) 엔진", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = OnBackground)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "본인 확인을 위해 주민등록증 또는\n운전면허증이 필요합니다.",
            fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal,
            fontSize = 14.sp, color = OnSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 22.sp
        )
        Spacer(Modifier.height(20.dp))
        // 프로그레스 인디케이터 (2/4 위치)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier.height(4.dp).width(if (i < 2) 32.dp else 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < 2) Mint500 else SurfaceVariant)
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint900)
        ) {
            Text("촬영 시작하기", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ─── 신분증 촬영 화면 (Image 2) ───────────────────────────────────
@Composable
private fun IdScanningContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val infiniteTransition = rememberInfiniteTransition(label = "idScan")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "border"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1E1E))) {
        // 카메라 배경
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cp = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    try { cp.unbindAll(); cp.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview) }
                    catch (e: Exception) { e.printStackTrace() }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        // 어두운 오버레이
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A).copy(alpha = 0.55f)))

        // 안내 텍스트
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "신분증을 가이드라인 안에\n맞춰 주세요",
                fontFamily = NaedaFontFamily, fontWeight = FontWeight.Bold,
                fontSize = 22.sp, color = Color.White, textAlign = TextAlign.Center, lineHeight = 32.sp
            )
        }

        // 신분증 가이드 박스
        Box(
            modifier = Modifier.fillMaxWidth(0.88f).aspectRatio(1.586f) // 신분증 비율
                .align(Alignment.Center).offset(y = (-20).dp)
                .border(2.dp, SolidColor(Mint500.copy(alpha = borderAlpha)), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "빛 반사가 없도록 주의해 주세요",
                fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal,
                fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f)
            )
        }

        // 인식 중 칩
        Box(
            modifier = Modifier.align(Alignment.Center).offset(y = 130.dp)
                .clip(RoundedCornerShape(20.dp)).background(Mint500.copy(alpha = 0.15f))
                .border(1.dp, Mint500, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("👁️", fontSize = 13.sp)
                Text("인식 중...", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Mint500)
            }
        }

        // 셔터 버튼
        Box(
            modifier = Modifier.size(64.dp).align(Alignment.Center).offset(y = 190.dp)
                .clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
        }

        // 하단 영역
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth(0.85f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.15f))) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(2.dp)).background(Mint500))
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.9f).clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.07f)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("😊", fontSize = 16.sp)
                Text("NAEDA 엔진이 인식 중입니다...", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

// ─── 신분증 정보 확인 화면 (Image 3) ─────────────────────────────
@Composable
private fun IdConfirmContent(
    name: String,
    idNumber: String,
    issueDate: String,
    onConfirm: () -> Unit
) {
    var nameState by remember { mutableStateOf(name) }
    var idNumberState by remember { mutableStateOf(idNumber) }
    var issueDateState by remember { mutableStateOf(issueDate) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("인식된 정보를 확인해주세요", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OnBackground)
        Spacer(Modifier.height(4.dp))
        Text("촬영된 신분증의 정보와 다른 부분이 있다면 수정해 주세요.", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, color = OnSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        // 신분증 이미지 영역
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp)
                .clip(RoundedCornerShape(16.dp)).background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🔍", fontSize = 14.sp)
                    Text("원본 보기", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = OnBackground)
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // 성명
        Text("성명", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = OnSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = nameState,
            onValueChange = { nameState = it },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Mint500,
                unfocusedBorderColor = Outline,
                focusedTextColor = OnBackground,
                unfocusedTextColor = OnBackground
            ),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        // 주민등록번호
        Text("주민등록번호", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = OnSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = idNumberState,
            onValueChange = { idNumberState = it },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Mint500,
                unfocusedBorderColor = Outline,
                focusedTextColor = OnBackground,
                unfocusedTextColor = OnBackground
            ),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        // 발급일자
        Text("발급일자", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = OnSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = issueDateState,
            onValueChange = { issueDateState = it },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Mint500,
                unfocusedBorderColor = Outline,
                focusedTextColor = OnBackground,
                unfocusedTextColor = OnBackground
            ),
            singleLine = true
        )

        Spacer(Modifier.height(20.dp))

        // 안내 박스
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(SurfaceVariant).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("ℹ️", fontSize = 14.sp)
            Text(
                "입력하신 정보는 본인 확인을 위해서만 사용되며, 안전하게 암호화되어 관리됩니다. 정보가 명확하지 않을 경우 서비스 이용이 제한될 수 있습니다.",
                fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal,
                fontSize = 12.sp, color = OnSurfaceVariant, lineHeight = 18.sp
            )
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint900)
        ) {
            Text("확인 및 다음", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ─── PIN 설정 여부 화면 (Image 4) ─────────────────────────────────
@Composable
private fun PinSetupContent(onUsePin: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Mint50).padding(horizontal = 16.dp, vertical = 6.dp)) {
            Text("보안 설정", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Mint500)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "PIN 번호 2차 인증을\n사용할까요?",
            fontFamily = NaedaFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 26.sp, color = OnBackground, textAlign = TextAlign.Center, lineHeight = 36.sp
        )
        Spacer(Modifier.height(48.dp))
        // 자물쇠 아이콘
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(220.dp).clip(CircleShape).background(Mint100.copy(alpha = 0.2f)))
            Box(modifier = Modifier.size(160.dp).clip(CircleShape).background(Mint500), contentAlignment = Alignment.Center) {
                Text("🔐", fontSize = 64.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        // 보안 안내 섹션
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🛡️", fontSize = 16.sp)
            Text("보안 강화 안내", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = OnBackground)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "2차 인증을 사용하면 보안이 더욱 강력해집니다.\n결제 시 얼굴 인식 후 PIN 번호를 한 번 더 입력하여 안전하게 보호하세요.",
            fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal,
            fontSize = 14.sp, color = OnSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 22.sp
        )
        Spacer(Modifier.height(20.dp))
        // 프로그레스 인디케이터 (3/4 위치)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier.height(4.dp).width(if (i < 3) 32.dp else 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < 3) Mint500 else SurfaceVariant)
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onUsePin,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint900)
        ) {
            Text("2차 인증 사용하기", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("다음에 하기", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, color = OnSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ─── 완료 화면 (Image 5) ──────────────────────────────────────────
@Composable
private fun SuccessContent(onComplete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))
        // 얼굴 아이콘 + 체크
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(180.dp).clip(CircleShape).background(Mint500),
                contentAlignment = Alignment.Center
            ) {
                Text("😊", fontSize = 72.sp)
            }
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White).border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Success, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("얼굴 등록이 완료되었습니다!", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = OnBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "이제 내다 매장에서 얼굴만으로\n간편하게 결제해 보세요",
            fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal,
            fontSize = 15.sp, color = OnSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 22.sp
        )
        Spacer(Modifier.height(32.dp))
        // 결제 준비 완료 카드
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(SurfaceVariant).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Mint100), contentAlignment = Alignment.Center) {
                Text("🏪", fontSize = 22.sp)
            }
            Column {
                Text("결제 준비 완료", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = OnBackground)
                Text("내다 가맹점에서 사용 가능합니다", fontFamily = NaedaFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = OnSurfaceVariant)
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint900)
        ) {
            Text("홈으로 이동", fontFamily = NaedaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
        }
        Spacer(Modifier.height(24.dp))
    }
}