package com.ssafy.naeda.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final String detail;    // 선택 필드 — null이면 직렬화에서 제외
    private final String aiStatus;
    private final Boolean retryable;

    // GlobalExceptionHandler에서 new ErrorResponse("CODE", "message") 형태로 쓰는 곳 대응
    public ErrorResponse(String code, String message) {
        this(code, message, null, null, null);
    }

    // @Builder가 사용하는 전체 생성자
    public ErrorResponse(String code, String message, String detail) {
        this(code, message, detail, null, null);
    }

    public ErrorResponse(String code, String message, String detail, String aiStatus, Boolean retryable) {
        this.code    = code;
        this.message = message;
        this.detail  = detail;
        this.aiStatus = aiStatus;
        this.retryable = retryable;
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }

    public static ErrorResponse of(String code, String message, String detail) {
        return new ErrorResponse(code, message, detail);
    }
}
