package com.ssafy.naeda.domain.point.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "포인트 적립 요청")
public class PointEarnRequest {

    @NotNull
    @Positive
    @Schema(description = "적립할 포인트", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
    private Long amount;

    @Size(max = 255)
    @Schema(description = "적립 사유", example = "출석 보상")
    private String description;

    @Schema(description = "결제 식별자(결제 기반 적립 시 사용)", example = "202603040001")
    private Long paymentId;
}
