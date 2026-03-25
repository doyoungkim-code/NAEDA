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
@Schema(description = "현재 PIN 검증 요청")
public class VerifyPinRequest {

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "PIN은 숫자 6자리여야 합니다.")
    @Schema(description = "검증할 현재 PIN 6자리", example = "123456")
    private String pin;
}
