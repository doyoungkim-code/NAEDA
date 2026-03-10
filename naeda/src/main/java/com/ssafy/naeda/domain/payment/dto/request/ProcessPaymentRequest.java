package com.ssafy.naeda.domain.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "결제 처리 요청 — 사용자 앱에서 전송 (multipart JSON 파트)")
public class ProcessPaymentRequest {

    @NotNull
    @Schema(description = "사용자 번호", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userNo;
}
