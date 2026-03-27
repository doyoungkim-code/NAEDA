package com.ssafy.naeda.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "비밀번호 재설정 코드 검증 응답")
public class PasswordResetVerifyResponse {

    @Schema(description = "비밀번호 변경용 검증 토큰")
    private String token;

    public static PasswordResetVerifyResponse of(String token) {
        return PasswordResetVerifyResponse.builder()
                .token(token)
                .build();
    }
}
