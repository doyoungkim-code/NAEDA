package com.ssafy.naeda.domain.rba.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "전화번호 가운데 4자리 검증 요청")
public class PhoneVerifyRequest {

    @NotNull
    @Schema(description = "검증 대상 사용자 번호", example = "18")
    private Long userNo;

    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "전화번호 가운데 4자리는 숫자 4자리여야 합니다.")
    @Schema(description = "입력한 전화번호 가운데 4자리", example = "1234")
    private String middleDigits;
}
