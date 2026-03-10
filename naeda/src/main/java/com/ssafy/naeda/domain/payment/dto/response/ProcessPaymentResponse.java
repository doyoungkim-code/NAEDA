package com.ssafy.naeda.domain.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "결제 처리 결과 응답")
public class ProcessPaymentResponse {

    @Schema(description = "결제 요청 ID")
    private String requestId;

    @Schema(description = "결제 ID (성공 시)")
    private Long paymentId;

    @Schema(description = "처리 결과 상태", example = "SUCCESS")
    private String status;

    @Schema(description = "다음 액션", example = "PASS")
    private String nextAction;

    @Schema(description = "매장 ID")
    private Long storeId;

    @Schema(description = "결제 금액")
    private Long amount;

    @Schema(description = "적립 포인트")
    private Integer earnedPoints;

    @Schema(description = "SSAFY 거래 고유번호")
    private String ssafyTransactionId;

    @Schema(description = "얼굴 유사도")
    private Double similarity;

    @Schema(description = "실패 사유")
    private String failureReason;
}
