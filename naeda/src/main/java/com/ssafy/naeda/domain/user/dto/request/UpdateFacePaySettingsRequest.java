package com.ssafy.naeda.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "페이스페이 설정 저장 요청")
public class UpdateFacePaySettingsRequest {

    @Schema(description = "PIN 2차 인증 사용 여부", example = "true")
    private Boolean enableSecondaryAuth;

    @Pattern(regexp = "^\\d{6}$", message = "PIN은 6자리 숫자여야 합니다.")
    @Schema(description = "현재 계정 PIN", example = "123456", nullable = true)
    private String currentPin;

    @Schema(description = "선택한 대표 결제수단 ID", example = "12", nullable = true)
    private Long paymentMethodId;

    @Schema(description = "1일 결제 한도", example = "300000", nullable = true)
    private Long dailyLimit;

    @Schema(description = "월 결제 한도", example = "3000000", nullable = true)
    private Long monthlyLimit;

    @Schema(description = "1회 결제 한도", example = "100000", nullable = true)
    private Long singleTransactionLimit;
}
