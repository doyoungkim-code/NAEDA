//package com.ssafy.naeda.domain.payment.dto.response;
//
//import com.ssafy.naeda.domain.face.dto.response.SearchResponse;
//import com.ssafy.naeda.domain.payment.entity.Payment;
//import com.ssafy.naeda.domain.payment.entity.PaymentStatus;
//import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.Builder;
//import lombok.Getter;
//
//import java.time.LocalDateTime;
//
//@Getter
//@Builder
//@Schema(description = "결제 응답")
//public class PaymentResponse {
//
//    @Schema(description = "결제 ID")
//    private Long paymentId;
//
//    @Schema(description = "사용자 번호")
//    private Long userNo;
//
//    @Schema(description = "매장 ID")
//    private Long storeId;
//
//    @Schema(description = "결제 금액")
//    private Long amount;
//
//    @Schema(description = "인증 방식", example = "FACE_PAY")
//    private String authMethod;
//
//    @Schema(description = "인증 레벨", example = "FACE_ONLY")
//    private String authLevel;
//
//    @Schema(description = "결제 상태", example = "SUCCESS")
//    private String status;
//
//    @Schema(description = "적립 포인트")
//    private Integer earnedPoints;
//
//    @Schema(description = "SSAFY 거래 고유번호")
//    private String ssafyTransactionId;
//
//    @Schema(description = "결제 일시")
//    private LocalDateTime paid;
//
//    // 결제 실행 시에만 포함되는 필드
//    @Schema(description = "다음 액션 (PASS / BLOCK / REQUIRE_SECOND_FACTOR)", example = "PASS")
//    private String nextAction;
//
//    @Schema(description = "얼굴 유사도 (0~1)", example = "0.92")
//    private Double similarity;
//
//    @Schema(description = "RBA 판정 사유")
//    private String rbaReason;
//
//    /** BLOCKED — 얼굴 불일치로 사용자 특정 불가, Payment 미저장 */
//    public static PaymentResponse blocked(SearchResponse face) {
//        return PaymentResponse.builder()
//                .status(PaymentStatus.BLOCKED.name())
//                .nextAction("BLOCK")
//                .similarity((double) face.getSimilarity())
//                .rbaReason(face.getRbaReason())
//                .build();
//    }
//
//    /** GET 조회용 — face 정보 없음 */
//    public static PaymentResponse from(Payment payment) {
//        return PaymentResponse.builder()
//                .paymentId(payment.getPaymentId())
//                .userNo(payment.getUserNo())
//                .storeId(payment.getStoreId())
//                .amount(payment.getAmount())
//                .authMethod(payment.getAuthMethod().name())
//                .authLevel(payment.getAuthLevel().name())
//                .status(payment.getStatus().name())
//                .earnedPoints(payment.getEarnedPoints())
//                .ssafyTransactionId(payment.getSsafyTransactionId())
//                .paid(payment.getPaid())
//                .build();
//    }
//
//    /** POST 결제 실행 결과 — face 정보 포함 */
//    public static PaymentResponse from(Payment payment, String nextAction, SearchResponse face) {
//        return PaymentResponse.builder()
//                .paymentId(payment.getPaymentId())
//                .userNo(payment.getUserNo())
//                .storeId(payment.getStoreId())
//                .amount(payment.getAmount())
//                .authMethod(payment.getAuthMethod().name())
//                .authLevel(payment.getAuthLevel().name())
//                .status(payment.getStatus().name())
//                .earnedPoints(payment.getEarnedPoints())
//                .ssafyTransactionId(payment.getSsafyTransactionId())
//                .paid(payment.getPaid())
//                .nextAction(nextAction)
//                .similarity((double) face.getSimilarity())
//                .rbaReason(face.getRbaReason())
//                .build();
//    }
//}
