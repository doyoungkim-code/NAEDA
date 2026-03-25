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
@Schema(description = "비밀번호 기반 PIN 재설정 요청")
public class ResetPinWithPasswordRequest {

    @NotBlank
    @Schema(description = "로그인 비밀번호", example = "password1234!")
    private String password;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "새 PIN은 숫자 6자리여야 합니다.")
    @Schema(description = "새 PIN 6자리", example = "654321")
    private String newPin;
}
