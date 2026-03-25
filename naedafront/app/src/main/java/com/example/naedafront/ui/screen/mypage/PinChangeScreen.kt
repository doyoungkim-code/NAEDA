package com.example.naedafront.ui.screen.mypage

import android.widget.Toast
import com.example.naedafront.AuthPrefs
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.data.remote.FaceRegistrationRepository
import com.example.naedafront.ui.screen.signup.NumberKeypad
import com.example.naedafront.ui.theme.Background
import com.example.naedafront.ui.theme.Mint50
import com.example.naedafront.ui.theme.Mint500
import com.example.naedafront.ui.theme.Mint900
import com.example.naedafront.ui.theme.NaedaFontFamily
import com.example.naedafront.ui.theme.OnBackground
import com.example.naedafront.ui.theme.OnSurfaceVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private enum class PinChangeStep {
    CURRENT_PIN,
    NEW_PIN,
    CONFIRM_PIN,
    RESET_PHONE,
    RESET_VERIFY,
    RESET_NEW_PIN,
    RESET_CONFIRM_PIN
}

private const val MAX_PIN_ATTEMPTS = 5
private const val LOCK_DURATION_MS = 30L * 1000 // 30초
private const val PREFS_NAME = "pin_change_lock"
private const val KEY_FAIL_COUNT = "fail_count"
private const val KEY_LOCKED_UNTIL = "locked_until"

private fun formatPhone(digits: String): String {
    return when {
        digits.length <= 3 -> digits
        digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
        else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
    }
}

private fun formatLockDurationMessage(remainingSeconds: Long): String {
    val min = remainingSeconds / 60
    val sec = remainingSeconds % 60

    return if (min > 0) {
        "%d분 %02d초 후에 다시 시도해 주세요.".format(min, sec)
    } else {
        "${sec}초 후에 다시 시도해 주세요."
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinChangeScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    val pinLockScope = remember(context) {
        AuthPrefs.getUserNo(context)?.toString()
            ?: AuthPrefs.getUserId(context)
            ?: "anonymous"
    }
    val failCountKey = remember(pinLockScope) { "${KEY_FAIL_COUNT}_$pinLockScope" }
    val lockedUntilKey = remember(pinLockScope) { "${KEY_LOCKED_UNTIL}_$pinLockScope" }

    var step by remember { mutableStateOf(PinChangeStep.CURRENT_PIN) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var failCount by remember(pinLockScope) { mutableIntStateOf(prefs.getInt(failCountKey, 0)) }
    var lockedUntil by remember(pinLockScope) { mutableLongStateOf(prefs.getLong(lockedUntilKey, 0L)) }
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    val isLocked = remainingSeconds > 0

    // ── 비밀번호 찾기 관련 상태 ──
    var phoneDigits by remember { mutableStateOf("") }
    var verifyCode by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var verifyTimerSeconds by remember { mutableIntStateOf(180) }
    var showSmsBanner by remember { mutableStateOf(false) }

    // 잠금 카운트다운 타이머
    LaunchedEffect(lockedUntil) {
        if (lockedUntil <= 0L) {
            remainingSeconds = 0
            return@LaunchedEffect
        }
        while (true) {
            val now = System.currentTimeMillis()
            val diff = lockedUntil - now
            if (diff <= 0) {
                remainingSeconds = 0
                failCount = 0
                prefs.edit().putInt(failCountKey, 0).putLong(lockedUntilKey, 0L).apply()
                errorMessage = null
                break
            }
            remainingSeconds = diff / 1000
            delay(1000L)
        }
    }

    // 인증번호 타이머
    LaunchedEffect(step, verifyTimerSeconds) {
        if (step == PinChangeStep.RESET_VERIFY && verifyTimerSeconds > 0) {
            delay(1000L)
            verifyTimerSeconds--
        }
    }

    // SMS 배너 자동 숨기기
    LaunchedEffect(showSmsBanner) {
        if (showSmsBanner) {
            delay(3000L)
            showSmsBanner = false
        }
    }

    // step 변경 또는 리셋 키 변경 시 pin 초기화
    var pinResetKey by remember { mutableIntStateOf(0) }
    var pin by remember(step, pinResetKey) { mutableStateOf("") }

    // ── 일반 PIN 변경 단계인지 여부 ──
    val isNormalFlow = step in listOf(PinChangeStep.CURRENT_PIN, PinChangeStep.NEW_PIN, PinChangeStep.CONFIRM_PIN)
    val isResetPinFlow = step in listOf(PinChangeStep.RESET_NEW_PIN, PinChangeStep.RESET_CONFIRM_PIN)

    val topBarTitle = when (step) {
        PinChangeStep.CURRENT_PIN -> "현재 PIN 입력"
        PinChangeStep.NEW_PIN -> "새 PIN 입력"
        PinChangeStep.CONFIRM_PIN -> "PIN 확인"
        PinChangeStep.RESET_PHONE -> "본인 인증"
        PinChangeStep.RESET_VERIFY -> "인증번호 확인"
        PinChangeStep.RESET_NEW_PIN -> "새 PIN 입력"
        PinChangeStep.RESET_CONFIRM_PIN -> "PIN 확인"
    }

    // ── 뒤로가기 로직 ──
    val onBack: () -> Unit = {
        when (step) {
            PinChangeStep.CURRENT_PIN -> onBackClick()
            PinChangeStep.NEW_PIN -> {
                step = PinChangeStep.CURRENT_PIN
                currentPin = ""
                errorMessage = null
            }
            PinChangeStep.CONFIRM_PIN -> {
                step = PinChangeStep.NEW_PIN
                newPin = ""
                errorMessage = null
            }
            PinChangeStep.RESET_PHONE -> {
                step = PinChangeStep.CURRENT_PIN
                phoneDigits = ""
                errorMessage = null
            }
            PinChangeStep.RESET_VERIFY -> {
                step = PinChangeStep.RESET_PHONE
                verifyCode = ""
                errorMessage = null
            }
            PinChangeStep.RESET_NEW_PIN -> {
                step = PinChangeStep.RESET_VERIFY
                errorMessage = null
            }
            PinChangeStep.RESET_CONFIRM_PIN -> {
                step = PinChangeStep.RESET_NEW_PIN
                errorMessage = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = topBarTitle,
                            fontFamily = NaedaFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = OnBackground
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
                )
            },
            containerColor = Background,
            contentWindowInsets = WindowInsets(0)
        ) { innerPadding ->
            when (step) {
                // ══════════════════════════════════════
                // 전화번호 입력 (회원가입과 동일 디자인)
                // ══════════════════════════════════════
                PinChangeStep.RESET_PHONE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "휴대폰 번호를\n입력해주세요",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 34.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "휴대폰 번호",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = formatPhone(phoneDigits).ifEmpty { "010-0000-0000" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (phoneDigits.isNotEmpty()) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outlineVariant,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "가입 시 등록한 휴대폰 번호를 입력해 주세요.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                verificationCode = Random.nextInt(100000, 1000000).toString()
                                verifyCode = ""
                                verifyTimerSeconds = 180
                                showSmsBanner = true
                                step = PinChangeStep.RESET_VERIFY
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = phoneDigits.length == 11,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Mint900,
                                disabledContainerColor = Mint900.copy(alpha = 0.38f)
                            )
                        ) {
                            Text(
                                text = "인증번호 받기",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        NumberKeypad(
                            onNumberClick = { digit ->
                                if (phoneDigits.length < 11) phoneDigits += digit
                            },
                            onDeleteClick = {
                                if (phoneDigits.isNotEmpty()) phoneDigits = phoneDigits.dropLast(1)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // ══════════════════════════════════════
                // 인증번호 입력 (회원가입과 동일 디자인)
                // ══════════════════════════════════════
                PinChangeStep.RESET_VERIFY -> {
                    val timerText = String.format("%02d:%02d", verifyTimerSeconds / 60, verifyTimerSeconds % 60)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "인증번호를\n입력해주세요",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 34.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${formatPhone(phoneDigits)} 로 번호를 보냈어요",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = verifyCode.toList().joinToString("  ").ifEmpty { "" },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 4.sp
                            )
                            Text(
                                text = timerText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "인증번호 다시받기",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.clickable {
                                verifyTimerSeconds = 180
                                verifyCode = ""
                                verificationCode = Random.nextInt(100000, 1000000).toString()
                                showSmsBanner = true
                            }
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage!!,
                                fontSize = 13.sp,
                                color = Color(0xFFF2522E)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (verifyCode == verificationCode) {
                                    errorMessage = null
                                    step = PinChangeStep.RESET_NEW_PIN
                                } else {
                                    errorMessage = "인증번호가 일치하지 않습니다."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = verifyCode.length == 6 && verifyTimerSeconds > 0,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Mint900,
                                disabledContainerColor = Mint900.copy(alpha = 0.38f)
                            )
                        ) {
                            Text(
                                text = "확인",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { verifyCode = verificationCode },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = "테스트 인증번호: $verificationCode",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        NumberKeypad(
                            onNumberClick = { digit ->
                                if (verifyCode.length < 6) verifyCode += digit
                            },
                            onDeleteClick = {
                                if (verifyCode.isNotEmpty()) verifyCode = verifyCode.dropLast(1)
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // ══════════════════════════════════════
                // PIN 입력 화면들 (현재PIN / 새PIN / 확인 / 리셋새PIN / 리셋확인)
                // ══════════════════════════════════════
                else -> {
                    val chipText = when (step) {
                        PinChangeStep.CURRENT_PIN -> "PIN 인증"
                        PinChangeStep.NEW_PIN, PinChangeStep.RESET_NEW_PIN -> "PIN 변경"
                        PinChangeStep.CONFIRM_PIN, PinChangeStep.RESET_CONFIRM_PIN -> "PIN 확인"
                        else -> ""
                    }

                    val title = when {
                        isLocked && step == PinChangeStep.CURRENT_PIN -> "PIN 변경이\n일시 중지되었습니다"
                        else -> when (step) {
                            PinChangeStep.CURRENT_PIN -> "현재 PIN 번호를\n입력해 주세요"
                            PinChangeStep.NEW_PIN, PinChangeStep.RESET_NEW_PIN -> "새로운 PIN 번호를\n입력해 주세요"
                            PinChangeStep.CONFIRM_PIN, PinChangeStep.RESET_CONFIRM_PIN -> "PIN 번호를 한 번 더\n입력해 주세요"
                            else -> ""
                        }
                    }

                    val description = when {
                        isLocked && step == PinChangeStep.CURRENT_PIN -> {
                            "PIN 번호를 ${MAX_PIN_ATTEMPTS}회 잘못 입력하셨습니다.\n${formatLockDurationMessage(remainingSeconds)}"
                        }
                        else -> when (step) {
                            PinChangeStep.CURRENT_PIN -> "PIN 번호를 변경하려면\n현재 계정 PIN 확인이 필요합니다."
                            PinChangeStep.NEW_PIN, PinChangeStep.RESET_NEW_PIN -> "변경할 새로운\nPIN 번호 6자리를 입력합니다."
                            PinChangeStep.CONFIRM_PIN, PinChangeStep.RESET_CONFIRM_PIN -> "확인을 위해\n같은 PIN 번호를 다시 입력합니다."
                            else -> ""
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ── 상단 콘텐츠 (스크롤 가능) ──
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(56.dp))

                            // ── 상단 칩 ──
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isLocked && step == PinChangeStep.CURRENT_PIN) Color(0xFFFDECEA)
                                        else Mint50
                                    )
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isLocked && step == PinChangeStep.CURRENT_PIN) "잠금" else chipText,
                                    fontFamily = NaedaFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isLocked && step == PinChangeStep.CURRENT_PIN) Color(0xFFF2522E) else Mint500
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = title,
                                fontFamily = NaedaFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = OnBackground,
                                textAlign = TextAlign.Center,
                                lineHeight = 34.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = description,
                                fontFamily = NaedaFontFamily,
                                fontSize = 14.sp,
                                color = if (isLocked && step == PinChangeStep.CURRENT_PIN) Color(0xFFF2522E) else OnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )

                            // Error message
                            if (errorMessage != null && !(isLocked && step == PinChangeStep.CURRENT_PIN)) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = errorMessage!!,
                                    fontFamily = NaedaFontFamily,
                                    fontSize = 13.sp,
                                    color = Color(0xFFF2522E),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // ── 비밀번호를 잊으셨나요? ──
                            if (step == PinChangeStep.CURRENT_PIN && failCount >= 1) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "비밀번호를 잊으셨나요?",
                                    fontFamily = NaedaFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Mint500,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        errorMessage = null
                                        phoneDigits = ""
                                        step = PinChangeStep.RESET_PHONE
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(48.dp))

                            // ── PIN 도트 ──
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                repeat(6) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(
                                                color = if (index < pin.length) Mint500
                                                else Color(0xFFE0E0E0),
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }

                        // ── 키패드 (하단 고정) ──
                        NumberKeypad(
                            onNumberClick = {
                                if ((isLocked && step == PinChangeStep.CURRENT_PIN) || isSaving || pin.length >= 6) return@NumberKeypad
                                errorMessage = null
                                pin += it
                                if (pin.length == 6) {
                                    when (step) {
                                        PinChangeStep.CURRENT_PIN -> {
                                            isSaving = true
                                            scope.launch {
                                                try {
                                                    FaceRegistrationRepository.verifyPin(pin)
                                                    currentPin = pin
                                                    failCount = 0
                                                    lockedUntil = 0L
                                                    prefs.edit()
                                                        .putInt(failCountKey, 0)
                                                        .putLong(lockedUntilKey, 0L)
                                                        .apply()
                                                    errorMessage = null
                                                    pinResetKey++
                                                    step = PinChangeStep.NEW_PIN
                                                } catch (e: Exception) {
                                                    val nextFailCount = failCount + 1
                                                    failCount = nextFailCount
                                                    if (nextFailCount >= MAX_PIN_ATTEMPTS) {
                                                        val until = System.currentTimeMillis() + LOCK_DURATION_MS
                                                        lockedUntil = until
                                                        remainingSeconds = LOCK_DURATION_MS / 1000
                                                        prefs.edit()
                                                            .putInt(failCountKey, nextFailCount)
                                                            .putLong(lockedUntilKey, until)
                                                            .apply()
                                                    } else {
                                                        prefs.edit().putInt(failCountKey, nextFailCount).apply()
                                                        errorMessage = "현재 PIN 번호가 올바르지 않습니다.\n다시 입력해 주세요. (${nextFailCount}/${MAX_PIN_ATTEMPTS})"
                                                    }
                                                    currentPin = ""
                                                    newPin = ""
                                                    pinResetKey++
                                                    step = PinChangeStep.CURRENT_PIN
                                                } finally {
                                                    isSaving = false
                                                }
                                            }
                                        }
                                        PinChangeStep.NEW_PIN -> {
                                            if (pin == currentPin) {
                                                errorMessage = "새 PIN 번호는 현재 PIN 번호와 다르게 입력해 주세요."
                                                pinResetKey++
                                            } else {
                                                newPin = pin
                                                step = PinChangeStep.CONFIRM_PIN
                                            }
                                        }
                                        PinChangeStep.CONFIRM_PIN -> {
                                            if (pin == newPin) {
                                                isSaving = true
                                                scope.launch {
                                                    try {
                                                        FaceRegistrationRepository.updatePin(
                                                            currentPin = currentPin,
                                                            newPin = newPin
                                                        )
                                                        Toast.makeText(context, "PIN 번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                                                        onBackClick()
                                                    } catch (e: Exception) {
                                                        isSaving = false
                                                        failCount++
                                                        if (failCount >= MAX_PIN_ATTEMPTS) {
                                                            val until = System.currentTimeMillis() + LOCK_DURATION_MS
                                                            lockedUntil = until
                                                            remainingSeconds = LOCK_DURATION_MS / 1000
                                                            prefs.edit().putInt(failCountKey, failCount).putLong(lockedUntilKey, until).apply()
                                                        } else {
                                                            prefs.edit().putInt(failCountKey, failCount).apply()
                                                            errorMessage = "현재 PIN 번호가 올바르지 않습니다.\n다시 입력해 주세요. (${failCount}/${MAX_PIN_ATTEMPTS})"
                                                        }
                                                        currentPin = ""
                                                        newPin = ""
                                                        pinResetKey++
                                                        step = PinChangeStep.CURRENT_PIN
                                                    }
                                                }
                                            } else {
                                                errorMessage = "처음 입력한 PIN 번호와 다릅니다.\n다시 입력해 주세요."
                                                pinResetKey++
                                            }
                                        }
                                        PinChangeStep.RESET_NEW_PIN -> {
                                            newPin = pin
                                            step = PinChangeStep.RESET_CONFIRM_PIN
                                        }
                                        PinChangeStep.RESET_CONFIRM_PIN -> {
                                            if (pin == newPin) {
                                                isSaving = true
                                                scope.launch {
                                                    try {
                                                        FaceRegistrationRepository.updatePin(
                                                            currentPin = null,
                                                            newPin = newPin
                                                        )
                                                        // 잠금 및 실패 횟수 초기화
                                                        failCount = 0
                                                        lockedUntil = 0L
                                                        prefs.edit().putInt(failCountKey, 0).putLong(lockedUntilKey, 0L).apply()
                                                        Toast.makeText(context, "PIN 번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                                                        onBackClick()
                                                    } catch (e: Exception) {
                                                        isSaving = false
                                                        errorMessage = e.message ?: "PIN 변경에 실패했습니다."
                                                    }
                                                }
                                            } else {
                                                errorMessage = "처음 입력한 PIN 번호와 다릅니다.\n다시 입력해 주세요."
                                                pinResetKey++
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            },
                            onDeleteClick = {
                                if (!(isLocked && step == PinChangeStep.CURRENT_PIN) && pin.isNotEmpty()) {
                                    pin = pin.dropLast(1)
                                }
                            },
                            textColor = OnBackground
                        )

                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }
            }
        }

        // ── 가짜 SMS 배너 ──
        if (showSmsBanner && step == PinChangeStep.RESET_VERIFY) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "문자 알림",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "문자 메시지",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatPhone(phoneDigits)} 인증번호 [$verificationCode]",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
