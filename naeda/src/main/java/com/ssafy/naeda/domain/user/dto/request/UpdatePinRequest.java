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
@Schema(description = "PIN 설정/변경 요청")
public class UpdatePinRequest {

    @Pattern(regexp = "\\d{6}", message = "현재 PIN은 숫자 6자리여야 합니다.")
    @Schema(description = "현재 PIN 6자리. 기존 PIN이 설정된 경우 필수", example = "123456", nullable = true)
    private String currentPin;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "새 PIN은 숫자 6자리여야 합니다.")
    @Schema(description = "새 PIN 6자리", example = "654321")
    private String newPin;
}
