package com.ssafy.naeda.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 요청")
public class SignupRequest {

    @NotBlank
    @Email
    @Size(max = 100)
    @Schema(description = "로그인 아이디 (이메일 형식)", example = "hong123@ssafy.co.kr")
    private String userId;

    @NotBlank
    @Schema(description = "비밀번호", example = "password123!")
    private String password;

    @NotBlank
    @Size(max = 50)
    @Schema(description = "사용자 이름", example = "홍길동")
    private String username;

    @NotBlank
    @Size(max = 7)
    @Schema(description = "주민등록번호 앞 7자리", example = "990101-")
    private String residentNo;

    @NotBlank
    @Size(max = 20)
    @Schema(description = "전화번호", example = "01012345678")
    private String phone;

    @NotBlank
    @Size(max = 50)
    @Schema(description = "기관코드", example = "001")
    private String institutionCode;
}
