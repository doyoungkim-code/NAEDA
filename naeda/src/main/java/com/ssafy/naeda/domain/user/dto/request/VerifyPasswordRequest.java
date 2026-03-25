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
@Schema(description = "비밀번호 검증 요청")
public class VerifyPasswordRequest {

    @NotBlank
    @Schema(description = "로그인 비밀번호", example = "password1234!")
    private String password;
}
