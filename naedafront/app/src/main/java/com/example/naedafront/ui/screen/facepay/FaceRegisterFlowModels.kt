package com.example.naedafront.ui.screen.facepay

import com.example.naedafront.data.remote.ResidentIdExtractResponseDto
import com.example.naedafront.data.remote.ResidentIdVerifyResponseDto

internal data class FaceCaptureSpec(
    val backendPose: String,
    val title: String,
    val instruction: String,
    val expectedDirection: String,
    val localDirection: FaceCaptureDirection
)

internal val faceCaptureSequence = listOf(
    FaceCaptureSpec("front1", "정면 1", "정면을 바라봐 주세요", "front", FaceCaptureDirection.FRONT),
    FaceCaptureSpec("front2", "정면 2", "정면을 유지해 주세요", "front", FaceCaptureDirection.FRONT),
    FaceCaptureSpec("front3", "정면 3", "정면을 한 번 더 유지해 주세요", "front", FaceCaptureDirection.FRONT),
    FaceCaptureSpec("left", "왼쪽", "고개를 왼쪽으로 돌려주세요", "left", FaceCaptureDirection.LEFT),
    FaceCaptureSpec("right", "오른쪽", "고개를 오른쪽으로 돌려주세요", "right", FaceCaptureDirection.RIGHT),
    FaceCaptureSpec("up", "위", "고개를 위로 들어주세요", "up", FaceCaptureDirection.UP),
    FaceCaptureSpec("down", "아래", "고개를 아래로 내려주세요", "down", FaceCaptureDirection.DOWN),
)

internal const val ID_CARD_HOLD_DURATION_MS = 2000L
internal const val ID_CARD_REQUEST_INTERVAL_MS = 650L
internal const val ID_CARD_ALLOWED_MISSES = 1

internal sealed class RegisterStage {
    data object PermissionRequest : RegisterStage()
    data object Intro : RegisterStage()
    data object Guide : RegisterStage()
    data class FaceCapture(val index: Int) : RegisterStage()
    data object IdGuide : RegisterStage()
    data object IdScanning : RegisterStage()
    data class IdConfirm(val extracted: ResidentIdExtractResponseDto) : RegisterStage()
    data object PinChoice : RegisterStage()
    data class CurrentPin(val resetKey: Int = 0) : RegisterStage()
    data object Success : RegisterStage()
}

internal data class DevicePostureState(
    val isUpright: Boolean,
    val message: String,
)

internal data class RegistrationFaceFramePayload(
    val fullFrameJpeg: ByteArray,
    val croppedFaceJpeg: ByteArray,
)

internal fun titleForStage(stage: RegisterStage): String {
    return when (stage) {
        is RegisterStage.PermissionRequest -> "권한 요청"
        is RegisterStage.Intro -> "페이스페이"
        is RegisterStage.Guide -> "촬영 가이드"
        is RegisterStage.FaceCapture -> "얼굴 등록"
        is RegisterStage.IdGuide -> "신분증 준비"
        is RegisterStage.IdScanning -> "신분증 촬영"
        is RegisterStage.IdConfirm -> "신분증 정보 확인"
        is RegisterStage.PinChoice -> "PIN 설정"
        is RegisterStage.CurrentPin -> "현재 PIN 입력"
        is RegisterStage.Success -> "등록 완료"
    }
}

internal fun buildIdConfirmError(response: ResidentIdVerifyResponseDto): String {
    return when {
        !response.nameMatched && !response.residentNoMatched -> "이름과 주민등록번호 일부가 모두 일치하지 않습니다."
        !response.nameMatched -> "이름이 로그인된 사용자 정보와 일치하지 않습니다."
        !response.residentNoMatched -> "주민등록번호 일부가 로그인된 사용자 정보와 일치하지 않습니다."
        else -> "신분증 확인에 실패했습니다."
    }
}
