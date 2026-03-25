package com.example.naedafront.ui.screen.mypage

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedafront.AuthPrefs
import com.example.naedafront.data.remote.ApiRequestException
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

private enum class PinChangeStep {
    CURRENT_PIN,
    NEW_PIN,
    CONFIRM_PIN,
    RESET_PASSWORD,
    RESET_NEW_PIN,
    RESET_CONFIRM_PIN
}

private const val MAX_PIN_ATTEMPTS = 5
private const val LOCK_DURATION_MS = 30L * 1000
private const val PREFS_NAME = "pin_change_lock"
private const val KEY_FAIL_COUNT = "fail_count"
private const val KEY_LOCKED_UNTIL = "locked_until"

private fun formatLockDurationMessage(remainingSeconds: Long): String {
    val min = remainingSeconds / 60
    val sec = remainingSeconds % 60
    return if (min > 0) {
        "%d분 %02d초 후에 다시 시도해 주세요.".format(min, sec)
    } else {
        "${sec}초 후에 다시 시도해 주세요."
    }
}

private fun readableMessage(throwable: Throwable, fallback: String): String {
    val raw = throwable.message
        ?.substringBefore(" [")
        ?.substringBefore(" (HTTP")
        ?.trim()
        .orEmpty()
    return raw.ifBlank { fallback }
}

private fun isAuthFailure(throwable: Throwable): Boolean {
    return throwable is ApiRequestException && throwable.statusCode == 401
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
    var verifiedPassword by remember { mutableStateOf<String?>(null) }
    var resetPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var failCount by remember(pinLockScope) { mutableIntStateOf(prefs.getInt(failCountKey, 0)) }
    var lockedUntil by remember(pinLockScope) { mutableLongStateOf(prefs.getLong(lockedUntilKey, 0L)) }
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    val isLocked = step == PinChangeStep.CURRENT_PIN && remainingSeconds > 0

    LaunchedEffect(lockedUntil) {
        if (lockedUntil <= 0L) {
            remainingSeconds = 0
            return@LaunchedEffect
        }

        while (true) {
            val diff = lockedUntil - System.currentTimeMillis()
            if (diff <= 0L) {
                remainingSeconds = 0
                failCount = 0
                prefs.edit()
                    .putInt(failCountKey, 0)
                    .putLong(lockedUntilKey, 0L)
                    .apply()
                errorMessage = null
                break
            }

            remainingSeconds = diff / 1000
            delay(1000L)
        }
    }

    var pinResetKey by remember { mutableIntStateOf(0) }
    var pin by remember(step, pinResetKey) { mutableStateOf("") }

    fun clearPinLockState() {
        failCount = 0
        lockedUntil = 0L
        remainingSeconds = 0L
        prefs.edit()
            .putInt(failCountKey, 0)
            .putLong(lockedUntilKey, 0L)
            .apply()
    }

    fun handleCurrentPinFailure() {
        val nextFailCount = failCount + 1
        failCount = nextFailCount
        currentPin = ""
        newPin = ""
        pinResetKey++
        step = PinChangeStep.CURRENT_PIN

        if (nextFailCount >= MAX_PIN_ATTEMPTS) {
            val until = System.currentTimeMillis() + LOCK_DURATION_MS
            lockedUntil = until
            remainingSeconds = LOCK_DURATION_MS / 1000
            errorMessage = null
            prefs.edit()
                .putInt(failCountKey, nextFailCount)
                .putLong(lockedUntilKey, until)
                .apply()
        } else {
            errorMessage = "현재 PIN 번호가 올바르지 않습니다.\n다시 입력해 주세요. (${nextFailCount}/${MAX_PIN_ATTEMPTS})"
            prefs.edit()
                .putInt(failCountKey, nextFailCount)
                .putLong(lockedUntilKey, 0L)
                .apply()
        }
    }

    val topBarTitle = when (step) {
        PinChangeStep.CURRENT_PIN -> "현재 PIN 입력"
        PinChangeStep.NEW_PIN -> "새 PIN 입력"
        PinChangeStep.CONFIRM_PIN -> "PIN 확인"
        PinChangeStep.RESET_PASSWORD -> "비밀번호 확인"
        PinChangeStep.RESET_NEW_PIN -> "새 PIN 입력"
        PinChangeStep.RESET_CONFIRM_PIN -> "PIN 확인"
    }

    val onBack: () -> Unit = {
        when (step) {
            PinChangeStep.CURRENT_PIN -> onBackClick()
            PinChangeStep.NEW_PIN -> {
                step = PinChangeStep.CURRENT_PIN
                currentPin = ""
                newPin = ""
                errorMessage = null
            }
            PinChangeStep.CONFIRM_PIN -> {
                step = PinChangeStep.NEW_PIN
                errorMessage = null
            }
            PinChangeStep.RESET_PASSWORD -> {
                step = PinChangeStep.CURRENT_PIN
                resetPassword = ""
                verifiedPassword = null
                passwordVisible = false
                errorMessage = null
            }
            PinChangeStep.RESET_NEW_PIN -> {
                step = PinChangeStep.RESET_PASSWORD
                verifiedPassword = null
                resetPassword = ""
                errorMessage = null
            }
            PinChangeStep.RESET_CONFIRM_PIN -> {
                step = PinChangeStep.RESET_NEW_PIN
                errorMessage = null
            }
        }
    }

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
            PinChangeStep.RESET_PASSWORD -> {
                PasswordVerifyContent(
                    modifier = Modifier.padding(innerPadding),
                    password = resetPassword,
                    onPasswordChange = {
                        resetPassword = it
                        errorMessage = null
                    },
                    passwordVisible = passwordVisible,
                    onToggleVisibility = { passwordVisible = !passwordVisible },
                    errorMessage = errorMessage,
                    isSaving = isSaving,
                    onConfirm = {
                        if (resetPassword.isNotBlank() && !isSaving) {
                            isSaving = true
                            scope.launch {
                                try {
                                    FaceRegistrationRepository.verifyPasswordForPinReset(resetPassword)
                                    verifiedPassword = resetPassword
                                    resetPassword = ""
                                    errorMessage = null
                                    pinResetKey++
                                    step = PinChangeStep.RESET_NEW_PIN
                                } catch (e: Exception) {
                                    errorMessage = if (isAuthFailure(e)) {
                                        "비밀번호가 일치하지 않습니다.\n다시 입력해 주세요."
                                    } else {
                                        readableMessage(e, "비밀번호 확인에 실패했습니다.")
                                    }
                                } finally {
                                    isSaving = false
                                }
                            }
                        }
                    }
                )
            }

            else -> {
                PinEntryContent(
                    modifier = Modifier.padding(innerPadding),
                    step = step,
                    pinLength = pin.length,
                    errorMessage = errorMessage,
                    isLocked = isLocked,
                    remainingSeconds = remainingSeconds,
                    failCount = failCount,
                    onForgotPinClick = if (step == PinChangeStep.CURRENT_PIN && failCount >= 1) {
                        {
                            errorMessage = null
                            resetPassword = ""
                            verifiedPassword = null
                            passwordVisible = false
                            step = PinChangeStep.RESET_PASSWORD
                        }
                    } else {
                        null
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NumberKeypad(
                            onNumberClick = { digit ->
                                if ((isLocked && step == PinChangeStep.CURRENT_PIN) || isSaving || pin.length >= 6) {
                                    return@NumberKeypad
                                }

                                errorMessage = null
                                pin += digit
                                if (pin.length != 6) return@NumberKeypad

                                when (step) {
                                    PinChangeStep.CURRENT_PIN -> {
                                        isSaving = true
                                        scope.launch {
                                            try {
                                                FaceRegistrationRepository.verifyPin(pin)
                                                currentPin = pin
                                                clearPinLockState()
                                                errorMessage = null
                                                pinResetKey++
                                                step = PinChangeStep.NEW_PIN
                                            } catch (e: Exception) {
                                                if (isAuthFailure(e)) {
                                                    handleCurrentPinFailure()
                                                } else {
                                                    errorMessage = readableMessage(e, "현재 PIN 확인에 실패했습니다.")
                                                    pinResetKey++
                                                }
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
                                        if (pin != newPin) {
                                            errorMessage = "처음 입력한 PIN 번호와 다릅니다.\n다시 입력해 주세요."
                                            pinResetKey++
                                            return@NumberKeypad
                                        }

                                        isSaving = true
                                        scope.launch {
                                            try {
                                                FaceRegistrationRepository.updatePin(
                                                    currentPin = currentPin,
                                                    newPin = newPin
                                                )
                                                clearPinLockState()
                                                Toast.makeText(context, "PIN 번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                                                onBackClick()
                                            } catch (e: Exception) {
                                                if (isAuthFailure(e)) {
                                                    handleCurrentPinFailure()
                                                } else {
                                                    errorMessage = readableMessage(e, "PIN 변경에 실패했습니다.")
                                                    pinResetKey++
                                                }
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    }

                                    PinChangeStep.RESET_NEW_PIN -> {
                                        if (verifiedPassword.isNullOrBlank()) {
                                            errorMessage = "비밀번호를 다시 확인해 주세요."
                                            pinResetKey++
                                            step = PinChangeStep.RESET_PASSWORD
                                        } else {
                                            newPin = pin
                                            step = PinChangeStep.RESET_CONFIRM_PIN
                                        }
                                    }

                                    PinChangeStep.RESET_CONFIRM_PIN -> {
                                        if (pin != newPin) {
                                            errorMessage = "처음 입력한 PIN 번호와 다릅니다.\n다시 입력해 주세요."
                                            pinResetKey++
                                            return@NumberKeypad
                                        }

                                        val password = verifiedPassword
                                        if (password.isNullOrBlank()) {
                                            errorMessage = "비밀번호를 다시 확인해 주세요."
                                            pinResetKey++
                                            step = PinChangeStep.RESET_PASSWORD
                                            return@NumberKeypad
                                        }

                                        isSaving = true
                                        scope.launch {
                                            try {
                                                FaceRegistrationRepository.resetPinWithPassword(
                                                    password = password,
                                                    newPin = newPin
                                                )
                                                clearPinLockState()
                                                Toast.makeText(context, "PIN 번호가 재설정되었습니다.", Toast.LENGTH_SHORT).show()
                                                onBackClick()
                                            } catch (e: Exception) {
                                                if (isAuthFailure(e)) {
                                                    verifiedPassword = null
                                                    resetPassword = ""
                                                    errorMessage = "비밀번호가 만료되었거나 일치하지 않습니다.\n다시 입력해 주세요."
                                                    pinResetKey++
                                                    step = PinChangeStep.RESET_PASSWORD
                                                } else {
                                                    val message = readableMessage(e, "PIN 재설정에 실패했습니다.")
                                                    errorMessage = message
                                                    pinResetKey++
                                                    if (message.contains("새 PIN은 현재 PIN과 달라야")) {
                                                        newPin = ""
                                                        step = PinChangeStep.RESET_NEW_PIN
                                                    }
                                                }
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    }

                                    PinChangeStep.RESET_PASSWORD -> Unit
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
    }
}

@Composable
private fun PasswordVerifyContent(
    modifier: Modifier = Modifier,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onToggleVisibility: () -> Unit,
    errorMessage: String?,
    isSaving: Boolean,
    onConfirm: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "비밀번호를\n입력해 주세요",
            fontFamily = NaedaFontFamily,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "PIN 번호를 잊은 경우\n로그인 비밀번호로 본인 확인을 진행합니다.",
            fontFamily = NaedaFontFamily,
            fontSize = 14.sp,
            color = OnSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("비밀번호") },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Mint500,
                focusedLabelColor = Mint500,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = Mint500
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                fontFamily = NaedaFontFamily,
                fontSize = 13.sp,
                color = Color(0xFFF2522E)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = password.isNotBlank() && !isSaving,
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PinEntryContent(
    modifier: Modifier = Modifier,
    step: PinChangeStep,
    pinLength: Int,
    errorMessage: String?,
    isLocked: Boolean,
    remainingSeconds: Long,
    failCount: Int,
    onForgotPinClick: (() -> Unit)?
) {
    val chipText = when (step) {
        PinChangeStep.CURRENT_PIN -> "PIN 인증"
        PinChangeStep.NEW_PIN, PinChangeStep.RESET_NEW_PIN -> "PIN 변경"
        PinChangeStep.CONFIRM_PIN, PinChangeStep.RESET_CONFIRM_PIN -> "PIN 확인"
        PinChangeStep.RESET_PASSWORD -> ""
    }

    val title = when {
        isLocked && step == PinChangeStep.CURRENT_PIN -> "PIN 변경이\n일시 중지되었습니다"
        step == PinChangeStep.CURRENT_PIN -> "현재 PIN 번호를\n입력해 주세요"
        step == PinChangeStep.NEW_PIN || step == PinChangeStep.RESET_NEW_PIN -> "새로운 PIN 번호를\n입력해 주세요"
        else -> "PIN 번호를 한 번 더\n입력해 주세요"
    }

    val description = when {
        isLocked && step == PinChangeStep.CURRENT_PIN -> {
            "PIN 번호를 ${MAX_PIN_ATTEMPTS}회 잘못 입력하셨습니다.\n${formatLockDurationMessage(remainingSeconds)}"
        }
        step == PinChangeStep.CURRENT_PIN -> "PIN 번호를 변경하려면\n현재 계정 PIN 확인이 필요합니다."
        step == PinChangeStep.NEW_PIN || step == PinChangeStep.RESET_NEW_PIN -> "변경할 새로운\nPIN 번호 6자리를 입력합니다."
        else -> "확인을 위해\n같은 PIN 번호를 다시 입력합니다."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

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

            if (errorMessage != null && !isLocked) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    fontFamily = NaedaFontFamily,
                    fontSize = 13.sp,
                    color = Color(0xFFF2522E),
                    textAlign = TextAlign.Center
                )
            }

            if (onForgotPinClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PIN 번호를 잊으셨나요?",
                    fontFamily = NaedaFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Mint500,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(onClick = onForgotPinClick)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(6) { index ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                color = if (index < pinLength) Mint500 else Color(0xFFE0E0E0),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (step == PinChangeStep.CURRENT_PIN && failCount > 0 && !isLocked) {
                Text(
                    text = "현재 실패 횟수: $failCount/$MAX_PIN_ATTEMPTS",
                    fontFamily = NaedaFontFamily,
                    fontSize = 12.sp,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}
