package com.ssafy.naeda.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "비밀번호 재설정 코드 응답")
public class PasswordResetCodeResponse {

    @Schema(description = "인증 코드 (6자리)", example = "482917")
    private String code;

    @Schema(description = "코드 만료시간(초)", example = "300")
    private int expiresInSeconds;

    public static PasswordResetCodeResponse of(String code, int expiresInSeconds) {
        return PasswordResetCodeResponse.builder()
                .code(code)
                .expiresInSeconds(expiresInSeconds)
                .build();
    }
}
