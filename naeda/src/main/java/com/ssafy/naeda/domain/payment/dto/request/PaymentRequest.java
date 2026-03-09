package com.ssafy.naeda.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "결제 요청 — 단말기에서 전송 (multipart JSON 파트)")
public class PaymentRequest {

    @NotNull
    @Schema(description = "매장 ID (SSAFY merchantId)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long storeId;

    @NotNull
    @Positive
    @Max(100_000_000)
    @Schema(description = "결제 금액 (원)", example = "15000", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
    private Long amount;
}
