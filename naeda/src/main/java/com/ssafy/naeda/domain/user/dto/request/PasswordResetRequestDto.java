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
@Schema(description = "비밀번호 재설정 요청")
public class PasswordResetRequestDto {

    @NotBlank
    @Pattern(regexp = "^01[016789]\\d{7,8}$", message = "올바른 휴대폰번호 형식이 아닙니다.")
    @Schema(description = "휴대폰번호", example = "01012345678")
    private String phone;
}
