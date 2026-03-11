package com.ssafy.naeda.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^\\d{7}$", message = "주민등록번호는 앞 6자리와 뒤 1자리를 붙인 숫자 7자리여야 합니다.")
    @Schema(description = "주민등록번호 앞 6자리와 뒤 1자리(하이픈 제외)", example = "9901011")
    private String residentNo;

    @NotBlank
    @Size(max = 20)
    @Schema(description = "전화번호", example = "01012345678")
    private String phone;

    @NotBlank
    @Size(max = 50)
    @Schema(description = "기관코드", example = "001")
    private String institutionCode;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "PIN은 숫자 6자리여야 합니다.")
    @Schema(description = "페이스페이 PIN 6자리", example = "123456")
    private String pin;
}
