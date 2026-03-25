package com.ssafy.naeda.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "현재 PIN 검증 응답")
public class PinVerifyResponse {

    @Schema(description = "현재 PIN 검증 성공 여부", example = "true")
    private boolean verified;

    @Schema(description = "검증 결과 메시지", example = "현재 PIN 확인이 완료되었습니다.")
    private String message;
}
