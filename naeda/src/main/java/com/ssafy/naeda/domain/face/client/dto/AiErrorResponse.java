package com.ssafy.naeda.domain.face.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

/**
 * AI 서버 에러 응답
 * {"code": "NO_FACE", "message": "No face detected", "requestId": "..."}
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiErrorResponse {
    private String code;
    private String message;
}
