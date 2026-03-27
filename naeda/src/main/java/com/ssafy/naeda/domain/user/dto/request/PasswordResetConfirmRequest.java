package com.ssafy.naeda.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "비밀번호 재설정 확인 요청")
public class PasswordResetConfirmRequest {

    @NotBlank
    @Schema(description = "검증 완료 토큰")
    private String token;

    @NotBlank
    @Schema(description = "새 비밀번호", example = "newPassword123!")
    private String newPassword;
}
