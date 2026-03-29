package com.ssafy.naeda.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "비밀번호 재설정 코드 검증 요청")
public class PasswordResetVerifyRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{11}$", message = "휴대폰번호는 11자리 숫자여야 합니다.")
    @Schema(description = "휴대폰번호", example = "01012345678")
    private String phone;

    @NotBlank
    @Schema(description = "인증 코드 (6자리)", example = "482917")
    private String code;
}
