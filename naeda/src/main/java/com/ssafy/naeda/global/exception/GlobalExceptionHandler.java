package com.ssafy.naeda.global.exception;

import com.ssafy.naeda.domain.face.exception.FaceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FaceException.class)
    public ResponseEntity<ErrorResponse> handleFaceException(FaceException e) {
        log.warn("FaceException: code={}, message={}", e.getCode(), e.getMessage());
        boolean retryable = "AI_TIMEOUT".equals(e.getCode()) || "AI_UNAVAILABLE".equals(e.getCode());
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(
                        e.getCode(),
                        e.getMessage(),
                        null,
                        retryable ? "FAILED_RETRYABLE" : "FAILED",
                        retryable ? Boolean.TRUE : null
                ));
    }

    @ExceptionHandler(SsafyApiException.class)
    public ResponseEntity<ErrorResponse> handleSsafyApiException(SsafyApiException e) {
        log.warn("SsafyApiException: code={}, message={}", e.getErrorCode(), e.getMessage());
        HttpStatus status = "NETWORK_ERROR".equals(e.getErrorCode())
                ? HttpStatus.SERVICE_UNAVAILABLE   // 503
                : HttpStatus.BAD_GATEWAY;          // 502
        return ResponseEntity.status(status)
                .body(new ErrorResponse(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolation: {}", e.getMostSpecificCause() != null
                ? e.getMostSpecificCause().getMessage()
                : e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_REFERENCE", "유효하지 않은 참조값입니다. userId를 확인해주세요."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "입력값이 올바르지 않습니다."));
    }

    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            NumberFormatException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", "요청 파라미터를 확인해주세요."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("Upload size exceeded: {}", e.getMessage());
        return ResponseEntity.status(413)
                .body(new ErrorResponse("FILE_TOO_LARGE", "업로드 파일 크기가 제한(10MB)을 초과했습니다."));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e) {
        log.warn("Not found: {}", e.getMessage());
        return ResponseEntity.status(404)
                .body(new ErrorResponse("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateException(DuplicateException e) {
        log.warn("Duplicate: {}", e.getMessage());
        return ResponseEntity.status(409)
                .body(new ErrorResponse("DUPLICATE", e.getMessage()));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalanceException(InsufficientBalanceException e) {
        log.warn("Insufficient balance: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INSUFFICIENT_BALANCE", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.status(500)
                .body(new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(AuthenticationFailedException e){
        log.warn("Authentication failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("AUTHENTICATION_FAILED",e.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
    }
}
