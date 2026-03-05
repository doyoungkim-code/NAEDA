package com.ssafy.naeda.global.exception;

import lombok.Getter;

/**
 * SSAFY 금융 API 호출 실패 시 던지는 예외.
 *
 * errorCode: SSAFY 응답의 responseCode (예: E1001, A1080)
 *            또는 내부 코드 (NETWORK_ERROR, NULL_RESPONSE 등)
 * message:   SSAFY 응답의 responseMessage
 */
@Getter
public class SsafyApiException extends RuntimeException {

    private final String errorCode;

    public SsafyApiException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}