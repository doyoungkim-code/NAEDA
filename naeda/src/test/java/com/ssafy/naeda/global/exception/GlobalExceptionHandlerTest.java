package com.ssafy.naeda.global.exception;

import com.ssafy.naeda.domain.face.exception.FaceErrorCode;
import com.ssafy.naeda.domain.face.exception.FaceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("AI timeout 예외는 FE가 바로 사용할 수 있는 retryable 오류로 내려간다")
    void handleFaceException_retryableAiError() {
        ResponseEntity<ErrorResponse> response = handler.handleFaceException(new FaceException(FaceErrorCode.AI_TIMEOUT));

        assertThat(response.getStatusCode().value()).isEqualTo(504);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("AI_TIMEOUT");
        assertThat(response.getBody().getAiStatus()).isEqualTo("FAILED_RETRYABLE");
        assertThat(response.getBody().getRetryable()).isTrue();
    }
}
