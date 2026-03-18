//package com.ssafy.naeda.domain.payment.dto.response;
//
//import com.ssafy.naeda.domain.payment.dto.PaymentRequestData;
//import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.Builder;
//import lombok.Getter;
//
//@Getter
//@Builder
//@Schema(description = "결제 요청 응답 (Redis 기반)")
//public class PaymentRequestResponse {
//
//    @Schema(description = "결제 요청 ID (UUID)")
//    private String requestId;
//
//    @Schema(description = "매장 ID")
//    private Long storeId;
//
//    @Schema(description = "결제 금액")
//    private Long amount;
//
//    @Schema(description = "요청 상태", example = "PENDING")
//    private String status;
//
//    @Schema(description = "결제된 사용자 번호 (결제 처리 후)")
//    private Long userNo;
//
//    @Schema(description = "다음 액션 (PASS / BLOCK / REQUIRE_SECOND_FACTOR)")
//    private String nextAction;
//
//    @Schema(description = "결제 ID (결제 성공 시)")
//    private Long paymentId;
//
//    @Schema(description = "실패 사유")
//    private String failureReason;
//
//    @Schema(description = "생성 시각 (epoch ms)")
//    private long createdAt;
//
//    @Schema(description = "최종 수정 시각 (epoch ms)")
//    private long updatedAt;
//
//    public static PaymentRequestResponse from(PaymentRequestData data) {
//        return PaymentRequestResponse.builder()
//                .requestId(data.getRequestId())
//                .storeId(data.getStoreId())
//                .amount(data.getAmount())
//                .status(data.getStatus().name())
//                .userNo(data.getUserNo())
//                .nextAction(data.getNextAction())
//                .paymentId(data.getPaymentId())
//                .failureReason(data.getFailureReason())
//                .createdAt(data.getCreatedAt())
//                .updatedAt(data.getUpdatedAt())
//                .build();
//    }
//}
