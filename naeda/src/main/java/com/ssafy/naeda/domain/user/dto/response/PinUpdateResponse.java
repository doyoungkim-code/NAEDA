package com.ssafy.naeda.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "PIN 설정/변경 응답")
public class PinUpdateResponse {

    @Schema(description = "PIN 설정 여부", example = "true")
    private boolean pinSet;

    @Schema(description = "처리 결과 메시지", example = "PIN 변경이 완료되었습니다.")
    private String message;
}
