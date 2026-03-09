package com.ssafy.naeda.domain.face.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FaceErrorCode {

    EMPTY_IMAGE(400, "EMPTY_IMAGE", "이미지가 비어있습니다."),
    INVALID_IMAGE(400, "INVALID_IMAGE", "올바르지 않은 이미지 형식입니다."),
    UNSUPPORTED_IMAGE_TYPE(400, "UNSUPPORTED_IMAGE_TYPE", "지원하지 않는 이미지 MIME 타입입니다."),
    NO_FACE(400, "NO_FACE", "얼굴을 찾을 수 없습니다."),
    MULTIPLE_FACES(400, "MULTIPLE_FACES", "이미지에 얼굴이 여러 개 감지되었습니다."),
    INVALID_POSE(400, "INVALID_POSE", "올바르지 않은 포즈입니다. (front|left|right|up|down)"),
    IMAGE_TOO_LARGE(413, "IMAGE_TOO_LARGE", "이미지 크기가 허용된 최대 크기를 초과했습니다."),
    AI_TIMEOUT(504, "AI_TIMEOUT", "AI 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요."),
    AI_UNAVAILABLE(503, "AI_UNAVAILABLE", "AI 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.");

    private final int status;
    private final String code;
    private final String message;
}
