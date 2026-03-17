package com.ssafy.naeda.domain.pay.dto.response;

import com.ssafy.naeda.domain.pay.entity.PayTransaction;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PayTransactionResponse {

    private Long paymentId;
    private Long userNo;
    private Long storeId;
    private Long paymentMethodId;
    private Long amount;
    private String status;
    private String authMethod;
    private String authLevel;
    private Double faceDistance;
    private Boolean livenessPass;
    private Boolean pinVerified;
    private Integer fdsScore;
    private String fdsAction;
    private Long earnedPoints;
    private String ssafyTransactionId;
    private String failureReason;
    private LocalDateTime createdAt;

    public static PayTransactionResponse from(PayTransaction tx) {
        return PayTransactionResponse.builder()
                .paymentId(tx.getId())
                .userNo(tx.getUserNo())
                .storeId(tx.getStoreId())
                .paymentMethodId(tx.getPaymentMethodId())
                .amount(tx.getAmount())
                .status(tx.getStatus().name())
                .authMethod(tx.getAuthMethod())
                .authLevel(tx.getAuthLevel())
                .faceDistance(tx.getFaceDistance())
                .livenessPass(tx.getLivenessPass())
                .pinVerified(tx.getPinVerified())
                .fdsScore(tx.getFdsScore())
                .fdsAction(tx.getFdsAction())
                .earnedPoints(tx.getEarnedPoints())
                .ssafyTransactionId(tx.getSsafyTransactionId())
                .failureReason(tx.getFailureReason())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
