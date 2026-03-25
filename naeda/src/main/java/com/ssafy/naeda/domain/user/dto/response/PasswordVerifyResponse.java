package com.ssafy.naeda.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "비밀번호 검증 응답")
public class PasswordVerifyResponse {

    @Schema(description = "비밀번호 검증 여부", example = "true")
    private boolean verified;

    @Schema(description = "처리 결과 메시지", example = "비밀번호 확인이 완료되었습니다.")
    private String message;
}
