package com.ssafy.naeda.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 요청")
public class LoginRequest {

    @NotBlank
    @Email
    @Schema(description = "로그인 아이디(이메일 형식)", example = "hong123@ssafy.co.kr")
    private String userId;

    @NotBlank
    @Schema(description = "비밀번호", example = "password1234!")
    private String password;
}
